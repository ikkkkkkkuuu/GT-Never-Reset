package com.xyp.gtnrlib.client.animation;

import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.BLEND;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.BLOCK_INSPECT;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.CARRY;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.CROSSBOW_INSPECT;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.CUTTER_INSPECT;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.HINGE_POSITION;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.HINGE_ROTATION;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.JAW_ROTATION;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.PICKAXE_INSPECT;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.SCREWDRIVER_INSPECT;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.STOCK_POSITION;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.STOCK_ROTATION;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.SWORD_INSPECT;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.TRANSFER;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.WAND_INSPECT;
import static com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.WRENCH_INSPECT;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Matrix4f;

import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;
import com.xyp.gtnrlib.api.animation.AnimationPlayer;
import com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.Binding;
import com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.Inspection;
import com.xyp.gtnrlib.client.animation.FirstPersonActionBindings.Key;
import com.xyp.gtnrlib.client.render.animation.HmiPoseRenderer;
import com.xyp.gtnrlib.compat.backhand.BackhandAnimationCompat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Resource-bound actions in the held item's local coordinate space, on top of the live first-person pose. */
@SideOnly(Side.CLIENT)
public final class FirstPersonItemActions {

    public static final FirstPersonItemActions INSTANCE = new FirstPersonItemActions(false);
    public static final FirstPersonItemActions OFFHAND = new FirstPersonItemActions(true);
    private static final List<String> ITEM_INSPECTIONS = List
        .of(WRENCH_INSPECT, CUTTER_INSPECT, SCREWDRIVER_INSPECT, CROSSBOW_INSPECT, WAND_INSPECT);
    private static final Map<String, Key> INSPECTION_DEFAULTS = Map.of(
        BLOCK_INSPECT,
        new Key("#block", -1, BLOCK_INSPECT),
        SWORD_INSPECT,
        new Key("#sword", -1, SWORD_INSPECT),
        PICKAXE_INSPECT,
        new Key("#pickaxe", -1, PICKAXE_INSPECT));
    private static final Vec NO_TRANSFER = new Vec(0, 0, 0);
    private static final Bone WRIST = new Bone("wrist"), ITEM = new Bone("item"), MAIN = new Bone("right_arm"),
        SUPPORT = new Bone("left_arm");
    private static FirstPersonActionBindings rules = FirstPersonActionBindings.EMPTY;
    private final boolean offhand;
    private final Matrix4f local = new Matrix4f(), changeOfBasis = new Matrix4f();
    private final Matrix4f leftGrip = new Matrix4f(), rightGrip = new Matrix4f();
    private final AnimationPlayer player = AnimationPlayer.withSources(this::playMarker);
    private AnimationPlayer.Playback action;
    private Key selected;
    private Binding activeBinding;
    private ItemStack inspectedStack;
    private HmiItemBindings.TwoHandedModel inspectedRig;
    private EntityPlayer owner, useOwner;
    private World world;
    private ItemStack useStack;
    private int useSlot, useDamage;
    private boolean overridesUse, transferAllowed, inspectionLeftArm;
    private int slot, dimension, startedTick;
    private double tick;

    private FirstPersonItemActions(boolean offhand) {
        this.offhand = offhand;
    }

    ItemStack held(EntityPlayer p) {
        return offhand ? BackhandAnimationCompat.heldItem(p) : p.getHeldItem();
    }

    private int currentSlot(EntityPlayer p) {
        return offhand ? BackhandAnimationCompat.slot(p) : p.inventory.currentItem;
    }

    private boolean enabled() {
        return (offhand ? HmiFirstPerson.OFFHAND : HmiFirstPerson.INSTANCE).enabled();
    }

    void reload(IResourceManager resources) throws IOException {
        rules = FirstPersonActionBindings.load(resources);
        reset();
        OFFHAND.reset();
    }

    void inspectHeld() {
        var p = Minecraft.getMinecraft().thePlayer;
        var stack = p == null ? null : held(p);
        if (stack == null) return;
        String custom = rules.inspection(id(stack), stack.getItemDamage());
        if (custom != null) {
            trigger(custom, true);
            return;
        }
        if (stack.getItem() instanceof ItemBlock) {
            trigger(BLOCK_INSPECT);
            return;
        }
        for (String inspection : ITEM_INSPECTIONS) {
            if (trigger(inspection)) return;
        }
        var grip = ToolGrip.resolve(stack);
        if (grip == ToolGrip.SWORD) trigger(SWORD_INSPECT);
        else if (grip == ToolGrip.PICKAXE) trigger(PICKAXE_INSPECT);
    }

    /** Play a named accepted action on the currently held stack. Unknown item/action pairs fall through. */
    public boolean trigger(String trigger) {
        return trigger(trigger, false);
    }

    private boolean trigger(String trigger, boolean requireCustom) {
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        if (trigger == null || !enabled() || p == null || mc.gameSettings.thirdPersonView != 0) return false;
        var stack = held(p);
        if (BLOCK_INSPECT.equals(trigger) && (stack == null || !(stack.getItem() instanceof ItemBlock))) return false;
        ToolGrip toolGrip = switch (trigger) {
            case SWORD_INSPECT -> ToolGrip.SWORD;
            case PICKAXE_INSPECT -> ToolGrip.PICKAXE;
            default -> null;
        };
        if (toolGrip != null && ToolGrip.resolve(stack) != toolGrip) return false;
        String id = id(stack);
        int damage = stack == null ? -1 : stack.getItemDamage();
        Key key = new Key(id, damage, trigger);
        var binding = rules.get(key);
        if (binding == null || requireCustom && !binding.inspection()
            .custom()) {
            key = new Key(id, -1, trigger);
            binding = rules.get(key);
        }
        if (requireCustom && (binding == null || !binding.inspection()
            .custom())) return false;
        var defaultKey = INSPECTION_DEFAULTS.get(trigger);
        if (binding == null && defaultKey != null
            && (toolGrip == null
                || MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON) == null))
            binding = rules.get(defaultKey);
        if (binding == null) {
            if (inspecting() && switch (trigger) {
                case "attack", "mining", "interact", "place", "use" -> true;
                default -> false;
            }) clear();
            return false;
        }

        var inspection = binding.inspection();
        boolean inspecting = inspection != Inspection.NONE;
        if (inspecting && (offhand || stack == null || !canInspect(mc, p, inspection, 1))) return false;
        var rig = inspection.rig() ? inspectionRig(stack, inspection == Inspection.CUTTER) : null;
        if (inspection.rig() && rig == null) return false;
        if (!matches(p, stack) || !inspecting && inspectedStack != null) clear();
        advance(p.ticksExisted);
        boolean chaining = action != null && action.active();
        if (inspecting && chaining) return false;
        owner = p;
        world = p.worldObj;
        dimension = p.dimension;
        slot = currentSlot(p);
        selected = inspecting ? new Key(id, damage, trigger) : key;
        activeBinding = binding;
        inspectedStack = inspection == Inspection.BLOCK || inspection.custom() ? stack.copy()
            : inspecting ? stack : null;
        inspectedRig = rig;
        transferAllowed = inspection.transfer() && !FirstPersonAnimations.offhandOwnsLeftArm(1);
        startedTick = p.ticksExisted;
        tick = Math.max(tick, p.ticksExisted);
        if (chaining) action = player.transition(
            action,
            binding.clip(),
            Math.max(BLEND, binding.fadeIn()),
            binding.rotation(),
            binding.transition());
        else {
            action = player.play(binding.clip(), binding.fadeIn());
            action.rotationBlend(binding.rotation());
        }
        action.fadeOutAtEnd(binding.fadeOut());
        return true;
    }

    void apply(ItemStack stack, float partial, Matrix4f main, Matrix4f support, Matrix4f item) {
        inspectionLeftArm = false;
        if (action == null) return;
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        if (p == null || !matches(p, held(p)) || !inspectionValid(mc, p, partial)) {
            clear();
            return;
        }
        // The outgoing stack may still be lowering while the server has already acknowledged the incoming tool.
        if (!matches(p, stack)) return;
        advance(p.ticksExisted);
        if (action == null || inspectedRig != null) return;
        double ahead = Math.max(0, (p.ticksExisted + (double) partial - tick) / 20);
        WRIST.sample(player, ahead, local);
        item.invert(changeOfBasis)
            .mulLocal(local)
            .mulLocal(item);
        item.mul(local);
        item.mul(ITEM.sample(player, ahead, local));
        var inspection = activeBinding.inspection();
        if (inspection != Inspection.NONE) {
            if (transferAllowed && FirstPersonAnimations.offhandOwnsLeftArm(partial)) transferAllowed = false;
            var transfer = transferAllowed ? player.sample(TRANSFER, ahead) : NO_TRANSFER;
            inspectionLeftArm = transfer.z() > 0;
            if (inspection == Inspection.TWO_HANDED) HmiPoseRenderer
                .poseHeldInspection(main, support, item, changeOfBasis, transfer, player.sample(CARRY, ahead));
            else HmiPoseRenderer
                .poseInspection(main, support, item, changeOfBasis, transfer, inspection == Inspection.BLOCK);
        } else main.mulLocal(changeOfBasis);
        MAIN.sample(player, ahead, rightGrip);
        SUPPORT.sample(player, ahead, leftGrip);
        main.mul(rightGrip);
        support.mul(leftGrip);
    }

    public boolean inspectionUsesLeftArm() {
        return inspectionLeftArm;
    }

    public boolean customInspection(ItemStack rendered, float partial) {
        if (activeBinding == null || !activeBinding.inspection()
            .custom() || !action.active()) return false;
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        return p != null && matches(p, rendered) && matches(p, held(p)) && inspectionValid(mc, p, partial);
    }

    public Scene scene(ItemStack rendered, float partial) {
        if (!customInspection(rendered, partial) || activeBinding.effect() == null) return null;
        var p = Minecraft.getMinecraft().thePlayer;
        double ahead = Math.max(0, (p.ticksExisted + (double) partial - tick) / 20);
        double time = action.timeAfter(ahead);
        return time >= activeBinding.clip()
            .duration() ? null : new Scene(activeBinding.effect(), time, activeBinding.clip());
    }

    private void playMarker(AnimationPlayer.Playback source, Marker marker) {
        if (source != action || activeBinding == null) return;
        var sound = activeBinding.sounds()
            .get(marker.name());
        if (sound != null && sound.volume() > 0) Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(sound.sound());
    }

    public RigInspection sampleRigInspection(ItemStack rendered, float partial) {
        if (!rigInspectionActive(rendered, partial)) return null;
        var p = Minecraft.getMinecraft().thePlayer;
        double ahead = Math.max(0, (p.ticksExisted + (double) partial - tick) / 20);
        boolean crossbow = activeBinding.inspection() == Inspection.CROSSBOW;
        return new RigInspection(
            player.sample(crossbow ? STOCK_POSITION : HINGE_POSITION, ahead),
            player.sample(crossbow ? STOCK_ROTATION : HINGE_ROTATION, ahead),
            player.sample(JAW_ROTATION, ahead));
    }

    private boolean rigInspectionActive(ItemStack rendered, float partial) {
        if (inspectedRig == null) return false;
        var p = Minecraft.getMinecraft().thePlayer;
        if (p == null || !matches(p, rendered)) return false;
        if (!FirstPersonAnimations.offhandOwnsLeftArm(partial) && inspectedRig.canInspect(rendered)) return true;
        clear();
        return false;
    }

    boolean inspecting() {
        return inspectedStack != null;
    }

    void interruptInspection() {
        if (inspecting()) clear();
    }

    public org.joml.Matrix4fc grip(boolean left) {
        return left ? leftGrip : rightGrip;
    }

    public boolean playing(String trigger) {
        return selected != null && selected.trigger()
            .equals(trigger) && action.active();
    }

    boolean controls(EntityPlayer p, ItemStack rendered) {
        return action != null && action.active() && matches(p, rendered);
    }

    boolean observeUse(EntityPlayer p) {
        var active = p == null || FirstPersonAnimations.usingOffhandItem() != offhand ? null : p.getItemInUse();
        int damage = active == null ? 0 : active.getItemDamage();
        int currentSlot = p == null ? -1 : currentSlot(p);
        if (active != useStack || p != useOwner || currentSlot != useSlot || damage != useDamage) {
            if (playing("use")) action.stop(activeBinding.fadeOut());
            useOwner = p;
            useSlot = currentSlot;
            useStack = active;
            useDamage = damage;
            overridesUse = active != null && held(p) != null && active.isItemEqual(held(p)) && trigger("use");
        }
        return overridesUse;
    }

    boolean startedThisTick(String name, EntityPlayer p) {
        return playing(name) && startedTick == p.ticksExisted && matches(p, held(p));
    }

    void tick() {
        var mc = Minecraft.getMinecraft();
        var p = mc.gameSettings.thirdPersonView == 0 && enabled() ? mc.thePlayer : null;
        observeUse(p);
        if (action == null) return;
        var stack = p == null ? null : held(p);
        if (p == null || !matches(p, stack)
            || !inspectionValid(mc, p, 1)
            || inspectedRig != null && !inspectedRig.canInspect(stack)) clear();
        else advance(p.ticksExisted);
    }

    private boolean inspectionValid(Minecraft mc, EntityPlayer p, float partial) {
        return !inspecting() || enabled() && mc.gameSettings.thirdPersonView == 0
            && canInspect(mc, p, activeBinding.inspection(), partial);
    }

    private static boolean canInspect(Minecraft mc, EntityPlayer p, Inspection inspection, float partial) {
        return mc.currentScreen == null && !p.isInvisible()
            && !p.isUsingItem()
            && !p.isSwingInProgress
            && (inspection != Inspection.TWO_HANDED || !FirstPersonAnimations.offhandOwnsLeftArm(partial));
    }

    private static HmiItemBindings.TwoHandedModel inspectionRig(ItemStack stack, boolean cutter) {
        if (FirstPersonAnimations.offhandOwnsLeftArm(1) || cutter && HmiFirstPerson.INSTANCE.strokeActive())
            return null;
        var renderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
        if (!(HmiItemBindings.tool(stack, renderer) instanceof HmiItemBindings.TwoHandedModel rig)
            || cutter && !(rig instanceof HmiItemBindings.TwoHandedTool)
            || !rig.canInspect(stack)) return null;
        return rig;
    }

    private void advance(double now) {
        if (action == null || now <= tick) return;
        var advancing = action;
        double elapsed = (now - tick) / 20;
        tick = now;
        player.advance(elapsed);
        if (action == advancing && !advancing.active()) clear();
    }

    private boolean matches(EntityPlayer p, ItemStack stack) {
        return selected != null && p != null
            && owner == p
            && world == p.worldObj
            && p.dimension == dimension
            && currentSlot(p) == slot
            && selected.item()
                .equals(id(stack))
            && (selected.damage() < 0 || stack != null && stack.getItemDamage() == selected.damage())
            && (inspectedStack == null || matchesInspectedStack(stack));
    }

    private boolean matchesInspectedStack(ItemStack stack) {
        var inspection = activeBinding.inspection();
        return inspection == Inspection.BLOCK || inspection.custom()
            ? ItemStack.areItemStackTagsEqual(inspectedStack, stack)
            : inspectedStack == stack;
    }

    private static String id(ItemStack stack) {
        return stack == null ? "minecraft:air" : String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
    }

    private void reset() {
        useOwner = null;
        useStack = null;
        overridesUse = false;
        clear();
    }

    void clear() {
        player.clear();
        action = null;
        selected = null;
        activeBinding = null;
        inspectedStack = null;
        inspectedRig = null;
        transferAllowed = inspectionLeftArm = false;
        owner = null;
        world = null;
        tick = 0;
        rightGrip.identity();
        leftGrip.identity();
    }

    public record Scene(ResourceLocation effect, double seconds, AnimationClip clip) {

        /** The first matching marker time in clip seconds, with a fallback for optional markers. */
        public double markerTime(String name, double fallback) {
            for (var marker : clip.markers()) if (marker.name()
                .equals(name)) return marker.time();
            return fallback;
        }
    }

    public record RigInspection(Vec position, Vec rotation, Vec jawRotation) {}

    private record Bone(Channel position, Channel rotation, Channel scale) {

        Bone(String name) {
            this(
                new Channel(name, Property.POSITION),
                new Channel(name, Property.ROTATION),
                new Channel(name, Property.SCALE));
        }

        Matrix4f sample(AnimationPlayer player, double ahead, Matrix4f out) {
            var pos = player.sample(position, ahead);
            var rot = player.sample(rotation, ahead);
            var size = player.sample(scale, ahead);
            return out.translation((float) pos.x(), (float) pos.y(), (float) pos.z())
                .rotateXYZ(
                    (float) Math.toRadians(rot.x()),
                    (float) Math.toRadians(rot.y()),
                    (float) Math.toRadians(rot.z()))
                .scale((float) size.x(), (float) size.y(), (float) size.z());
        }
    }
}
