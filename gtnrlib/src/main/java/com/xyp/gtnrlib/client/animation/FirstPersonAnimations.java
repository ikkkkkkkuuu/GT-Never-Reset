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
    private static final Marker HAND_MOTION = new Marker(0, "gtnrlib:hand_motion");
    private final FirstPersonHandMotion handMotion = new FirstPersonHandMotion();
    private final List<Consumer<Marker>> listeners = new ArrayList<>();
    private final AnimationPlayer player = new AnimationPlayer(
        m -> { for (var listener : List.copyOf(listeners)) listener.accept(m); });
    private final Map<String, Binding> bindings = new LinkedHashMap<>();
    private PunchyPack packs = PunchyPack.empty();
    private Object world;
    private Item item;
    private int slot = -1, combo;
    private Motion baseMotion, actionMotion;
    private PendingAction pending;
    private PunchyPack.Pose rightItem, rightArm, leftArm;
    private AnimationPlayer.Playback base, action;
    private String activeAction = "";
    private boolean mining, replacesSwing, usesHandMotion;
    private double sway, previousSway, pitch, previousPitch;

    /** Implemented by the 1.7.10 renderer while its outgoing stack lowers on a slot change. */
    public interface EquippedStack {

        ItemStack gtnrlib$equippedStack();

        float gtnrlib$equipDip(float partial);
    }

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
        if (pending != null && (pending.slot != mc.thePlayer.inventory.currentItem || pending.stack != held))
            pending = null;
        if (isLowering(mc)) {
            player.advance(.05);
            advanceMovement(mc);
            return;
        }
        var incoming = pending;
        pending = null;
        synchronizeTarget(mc, held);
        boolean hitting = mc.currentScreen == null && mc.gameSettings.keyBindAttack.getIsKeyPressed()
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
        player.advance(.05);
        if (replacesSwing && (action == null || !action.active())) {
            // Vanilla interpolates the final tick from the previous progress through 1, even after the swing stops.
            var local = mc.thePlayer;
            if (local.swingProgress == 0 && local.prevSwingProgress == 0
                || local.isSwingInProgress && local.swingProgress < local.prevSwingProgress) replacesSwing = false;
        }
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
            usesHandMotion = next != null && next.clip()
                .markers()
                .contains(HAND_MOTION);
            base = next == null ? null : play(next, .12, 0);
        }
        if (incoming != null) trigger(incoming.name);
        advanceMovement(mc);
    }

    private void advanceMovement(Minecraft mc) {
        previousSway = sway;
        previousPitch = pitch;
        double yaw = net.minecraft.util.MathHelper
            .wrapAngleTo180_float(mc.thePlayer.rotationYaw - mc.thePlayer.prevRotationYaw);
        sway += (clamp(yaw, 12) - sway) * .28;
        var local = mc.thePlayer;
        double look = local.rotationPitch - local.prevRotationPitch;
        pitch += (clamp(look, 8) - pitch) * .28;
        if (usesHandMotion) {
            handMotion.advance(
                local.posX - local.prevPosX,
                local.posY - local.prevPosY,
                local.posZ - local.prevPosZ,
                local.rotationYaw,
                local.rotationPitch,
                local.onGround && mc.gameSettings.viewBobbing);
        } else handMotion.clear();
    }

    public void trigger(String name) {
        if (!enabled()) return;
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (mc.theWorld == world && isLowering(mc)) {
            pending = new PendingAction(mc.thePlayer.inventory.currentItem, held, name);
            return;
        }
        synchronizeTarget(mc, held);
        Motion motion = resolve(held, name, name.equals("attack") ? combo : 0);
        if (motion == null) {
            if (action == null || !action.active()) replacesSwing = false;
            return;
        }
        if (name.equals("mining")) mining = true;
        if (name.equals("attack")) combo++;
        boolean replacingActiveSwing = replacesSwing && action != null && action.active();
        if (action != null) action.stop(replacingActiveSwing ? .10 : .05);
        activeAction = name;
        actionMotion = motion;
        replacesSwing = motion.clip()
            .markers()
            .contains(REPLACE_SWING);
        action = play(motion, replacingActiveSwing && replacesSwing ? .10 : .025, 1);
        if (replacesSwing) action.fadeOutAtEnd(
            Math.min(
                .08,
                motion.clip()
                    .duration() * .25));
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
        String group = group(held);
        rightItem = packs.pose(id, group, "item_right");
        rightArm = packs.pose(id, group, "arm_right");
        leftArm = packs.pose(id, group, "arm_left");
        return packs.select(id, group, actionName, index);
    }

    private static String group(ItemStack stack) {
        if (stack == null) return "ITEM";
        Item item = stack.getItem();
        if (item instanceof ItemSword) return "SWORD";
        if (item instanceof ItemPickaxe) return "PICKAXE";
        if (item instanceof ItemAxe) return "AXE";
        if (item instanceof ItemSpade) return "SHOVEL";
        if (item instanceof ItemHoe) return "HOE";
        if (item instanceof ItemBlock) return "BLOCK";
        if (item instanceof ItemBow) return "BOW";
        return !item.isFull3D() && stack.getMaxDamage() == 0 ? "GTNRLIB_FLAT_ITEM" : "ITEM";
    }

    public boolean enabled() {
        return packs.hasMotions() || !bindings.isEmpty();
    }

    public boolean applies(ItemStack rendered) {
        if (!enabled() || rightItem == null && rightArm == null
            && leftArm == null
            && (base == null || !base.active())
            && (action == null || !action.active())) return false;
        return matchesTarget(rendered);
    }

    private boolean matchesTarget(ItemStack rendered) {
        var mc = Minecraft.getMinecraft();
        if (mc.theWorld != world || mc.thePlayer == null
            || mc.gameSettings.thirdPersonView != 0
            || mc.thePlayer.isInvisible()
            || item != (rendered == null ? null : rendered.getItem())) return false;
        if ((slot != mc.thePlayer.inventory.currentItem || rendered != mc.thePlayer.getHeldItem()) && !(isLowering(mc)
            && ((EquippedStack) mc.entityRenderer.itemRenderer).gtnrlib$equippedStack() == rendered)) return false;
        return rendered == null || !(rendered.getItem() instanceof ItemMap)
            && MinecraftForgeClient.getItemRenderer(rendered, ItemRenderType.EQUIPPED_FIRST_PERSON) == null;
    }

    private boolean isLowering(Minecraft mc) {
        if (!usesHandMotion || !(mc.entityRenderer.itemRenderer instanceof EquippedStack renderer)) return false;
        ItemStack rendered = renderer.gtnrlib$equippedStack();
        return rendered != mc.thePlayer.getHeldItem() && item == (rendered == null ? null : rendered.getItem());
    }

    public Vec handOffset(float partial) {
        return usesHandMotion ? handMotion.sample(partial, Minecraft.getMinecraft().thePlayer.ticksExisted + partial)
            : null;
    }

    public PunchyPack.Pose tunedArm(boolean left) {
        return left ? leftArm : rightArm;
    }

    public PunchyPack.Pose tunedItem() {
        return rightItem;
    }

    public boolean replacesSwing(ItemStack rendered) {
        return replacesSwing && matchesTarget(rendered);
    }

    public boolean replacesPlacementDip(ItemStack rendered) {
        return activeAction.equals("useitem") && action != null && action.active() && replacesSwing(rendered);
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
            if (world != mc.theWorld) handMotion.clear();
            resetPlayback();
            world = mc.theWorld;
            item = current;
            slot = mc.thePlayer.inventory.currentItem;
        }
    }

    private void reset() {
        pending = null;
        handMotion.clear();
        resetPlayback();
    }

    private void resetPlayback() {
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
        usesHandMotion = false;
        actionMotion = null;
        replacesSwing = false;
        mining = false;
        combo = 0;
        activeAction = "";
        previousSway = sway = previousPitch = pitch = 0;
    }

    private record PendingAction(int slot, ItemStack stack, String name) {}
}
