package com.xyp.gtnrlib.client.animation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;
import com.xyp.gtnrlib.api.animation.AnimationPlayer;
import com.xyp.gtnrlib.compat.punchy.PunchyPack;
import com.xyp.gtnrlib.compat.punchy.PunchyPack.Motion;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Local first-person target. All calls, including provider and listener registration, belong on the client thread. */
@SideOnly(Side.CLIENT)
public final class FirstPersonAnimations implements IResourceManagerReloadListener {

    public static final FirstPersonAnimations INSTANCE = new FirstPersonAnimations();
    private static final Marker REPLACE_SWING = new Marker(0, "gtnrlib:replace_swing");
    private final List<Consumer<Marker>> listeners = new ArrayList<>();
    private final AnimationPlayer player = new AnimationPlayer(
        m -> { for (var listener : List.copyOf(listeners)) listener.accept(m); });
    private final Map<String, Binding> bindings = new LinkedHashMap<>();
    private PunchyPack packs = PunchyPack.empty();
    private Object world;
    private Item item;
    private int slot = -1, combo;
    private Motion baseMotion, actionMotion;
    private PunchyPack.Pose rightItem, rightArm, leftArm;
    private AnimationPlayer.Playback base, action;
    private String activeAction = "";
    private boolean mining, replacesSwing;
    private double sway, previousSway, pitch, previousPitch;

    @FunctionalInterface
    public interface Binding {

        AnimationClip resolve(ItemStack held, String action);
    }

    private FirstPersonAnimations() {}

    public void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(this);
    }

    public void bind(String id, Binding binding) {
        if (bindings.putIfAbsent(id, Objects.requireNonNull(binding)) != null)
            throw new IllegalArgumentException("duplicate binding: " + id);
    }

    public void unbind(String id) {
        bindings.remove(id);
        reset();
    }

    public void addListener(Consumer<Marker> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(Consumer<Marker> listener) {
        listeners.remove(listener);
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        var repo = Minecraft.getMinecraft()
            .getResourcePackRepository();
        if (repo == null) return;
        Path dir = repo.getDirResourcepacks()
            .toPath()
            .toAbsolutePath()
            .normalize();
        List<Path> sources = new ArrayList<>();
        for (Object entry : repo.getRepositoryEntries()) {
            String name = ((ResourcePackRepository.Entry) entry).getResourcePackName();
            Path path = dir.resolve(name)
                .normalize();
            if (dir.equals(path.getParent())) sources.add(path);
        }
        try {
            if (!GTNRLibConfig.animationPack.isEmpty()) {
                Path imported = dir.resolve(GTNRLibConfig.animationPack)
                    .normalize();
                if (!dir.equals(imported.getParent()))
                    throw new IllegalArgumentException("expected a name inside resourcepacks");
                sources.remove(imported);
                sources.add(imported);
            }
            PunchyPack next = PunchyPack.load(sources, message -> GTNRLib.LOG.warn("animation pack: {}", message));
            packs = next;
            reset();
        } catch (Exception e) {
            GTNRLib.LOG.warn("animation reload retained previous snapshot", e);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled()) return;
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld != world || mc.gameSettings.thirdPersonView != 0) {
            reset();
            world = mc.theWorld;
            if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return;
        }
        if (mc.isGamePaused()) return;
        var held = mc.thePlayer.getHeldItem();
        synchronizeTarget(mc, held);
        boolean hitting = mc.currentScreen == null && mc.gameSettings.keyBindAttack.getIsKeyPressed()
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
        player.advance(.05);
        if (hitting && (!mining || action == null || !action.active())) trigger("mining");
        else if (!hitting && mining && activeAction.equals("mining")) {
            if (action != null) action.stop(.08);
            activeAction = "";
        }
        mining = hitting;
        String state = mc.thePlayer.isSprinting() ? "sprint"
            : mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0 ? "walk" : "idle";
        Motion next = resolve(held, state, 0);
        if (next == null && !state.equals("idle")) next = resolve(held, "idle", 0);
        if (!Objects.equals(baseMotion, next) || (next != null && (base == null || !base.active()))) {
            if (base != null) base.stop(.12);
            baseMotion = next;
            base = next == null ? null : play(next, .12, 0);
        }
        previousSway = sway;
        previousPitch = pitch;
        double yaw = net.minecraft.util.MathHelper
            .wrapAngleTo180_float(mc.thePlayer.rotationYaw - mc.thePlayer.prevRotationYaw);
        sway += (clamp(yaw, 12) - sway) * .28;
        pitch += (clamp(mc.thePlayer.rotationPitch - mc.thePlayer.prevRotationPitch, 8) - pitch) * .28;
    }

    public void trigger(String name) {
        if (!enabled()) return;
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        synchronizeTarget(mc, held);
        Motion motion = resolve(held, name, name.equals("attack") ? combo : 0);
        if (motion == null) return;
        if (name.equals("attack")) combo++;
        if (action != null) action.stop(.05);
        activeAction = name;
        actionMotion = motion;
        replacesSwing = motion.clip()
            .markers()
            .contains(REPLACE_SWING);
        action = play(motion, .025, 1);
    }

    private AnimationPlayer.Playback play(Motion motion, double fade, int priority) {
        var handle = player.play(motion.clip(), fade, priority);
        handle.speed(motion.speed());
        return handle;
    }

    private Motion resolve(ItemStack held, String actionName, int index) {
        for (Binding binding : bindings.values()) {
            var clip = binding.resolve(held, actionName);
            if (clip != null) return new Motion(clip, 1, false, true);
        }
        Item current = held == null ? null : held.getItem();
        String id = current == null ? "emptyHand" : String.valueOf(Item.itemRegistry.getNameForObject(current));
        String group = current instanceof ItemSword ? "SWORD"
            : current instanceof ItemPickaxe ? "PICKAXE"
                : current instanceof ItemAxe ? "AXE"
                    : current instanceof ItemSpade ? "SHOVEL"
                        : current instanceof ItemHoe ? "HOE"
                            : current instanceof ItemBlock ? "BLOCK" : current instanceof ItemBow ? "BOW" : "ITEM";
        rightItem = packs.pose(id, group, "item_right");
        rightArm = packs.pose(id, group, "arm_right");
        leftArm = packs.pose(id, group, "arm_left");
        return packs.select(id, group, actionName, index);
    }

    public boolean enabled() {
        return packs.hasMotions() || !bindings.isEmpty();
    }

    public boolean applies(ItemStack rendered) {
        if (!enabled() || rightItem == null && rightArm == null
            && leftArm == null
            && (base == null || !base.active())
            && (action == null || !action.active())) return false;
        var mc = Minecraft.getMinecraft();
        if (mc.theWorld != world || mc.thePlayer == null
            || mc.gameSettings.thirdPersonView != 0
            || mc.thePlayer.isInvisible()
            || rendered != mc.thePlayer.getHeldItem()) return false;
        return rendered == null || !(rendered.getItem() instanceof ItemMap)
            && MinecraftForgeClient.getItemRenderer(rendered, ItemRenderType.EQUIPPED_FIRST_PERSON) == null;
    }

    public PunchyPack.Pose tunedArm(boolean left) {
        return left ? leftArm : rightArm;
    }

    public PunchyPack.Pose tunedItem() {
        return rightItem;
    }

    public boolean replacesSwing(ItemStack rendered) {
        return replacesSwing && action != null && action.active() && applies(rendered);
    }

    public boolean replacesPlacementDip(ItemStack rendered) {
        return activeAction.equals("useitem") && replacesSwing(rendered);
    }

    public boolean dualHanded() {
        return (action != null && action.active() && actionMotion.dualHanded())
            || (base != null && base.active() && baseMotion.dualHanded());
    }

    public Vec sample(Channel channel, float partial) {
        return player.sample(channel, partial * .05);
    }

    public double yaw(float partial) {
        return previousSway + (sway - previousSway) * partial;
    }

    public double pitch(float partial) {
        return previousPitch + (pitch - previousPitch) * partial;
    }

    private static double clamp(double n, double max) {
        return Math.max(-max, Math.min(max, n));
    }

    private void synchronizeTarget(Minecraft mc, ItemStack held) {
        Item current = held == null ? null : held.getItem();
        if (mc.theWorld != world || current != item || mc.thePlayer.inventory.currentItem != slot) {
            reset();
            world = mc.theWorld;
            item = current;
            slot = mc.thePlayer.inventory.currentItem;
        }
    }

    private void reset() {
        rightItem = null;
        rightArm = null;
        leftArm = null;
        player.clear();
        slot = -1;
        item = null;
        world = null;
        action = null;
        base = null;
        baseMotion = null;
        actionMotion = null;
        replacesSwing = false;
        mining = false;
        combo = 0;
        activeAction = "";
        previousSway = sway = previousPitch = pitch = 0;
    }
}
