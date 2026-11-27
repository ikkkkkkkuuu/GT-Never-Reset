package com.xyp.gtnrlib.client.animation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Matrix4f;

import com.google.gson.JsonParser;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.animation.resource.NativeAnimationResource;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;
import com.xyp.gtnrlib.api.animation.AnimationPlayer;
import com.xyp.gtnrlib.client.render.animation.HmiPoseRenderer;
import com.xyp.gtnrlib.compat.backhand.BackhandAnimationCompat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Resource-bound actions in the held item's local coordinate space, on top of the live first-person pose. */
@SideOnly(Side.CLIENT)
public final class FirstPersonItemActions {

    public static final FirstPersonItemActions INSTANCE = new FirstPersonItemActions(false);
    public static final FirstPersonItemActions OFFHAND = new FirstPersonItemActions(true);
    private static final String BLOCK_INSPECT = "gtnrlib:block_inspect";
    private static final String WRENCH_INSPECT = "gtnrlib:wrench_inspect";
    private static final String CUTTER_INSPECT = "gtnrlib:wire_cutter_inspect";
    private static final Key BLOCK_INSPECT_DEFAULT = new Key("#block", -1, BLOCK_INSPECT);
    private static final double BLEND = .08;
    private static final Set<String> BONES = Set.of("wrist", "item", "left_arm", "right_arm");
    private static final Channel INSPECTION_TRANSFER = new Channel("transfer", Property.POSITION);
    private static final Channel HINGE_POSITION = new Channel("hinge", Property.POSITION);
    private static final Channel HINGE_ROTATION = new Channel("hinge", Property.ROTATION);
    private static final Channel JAW_ROTATION = new Channel("jaw", Property.ROTATION);
    private static final Set<Channel> RIG_INSPECTION_CHANNELS = Set.of(HINGE_POSITION, HINGE_ROTATION, JAW_ROTATION);
    private static final Vec NO_TRANSFER = new Vec(0, 0, 0);
    private static final Bone WRIST = new Bone("wrist"), ITEM = new Bone("item"), MAIN = new Bone("right_arm"),
        SUPPORT = new Bone("left_arm");
    private static Map<Key, Binding> rules = Map.of();
    private final boolean offhand;
    private final Matrix4f local = new Matrix4f(), changeOfBasis = new Matrix4f();
    private final Matrix4f leftGrip = new Matrix4f(), rightGrip = new Matrix4f();
    private final AnimationPlayer player = new AnimationPlayer(marker -> {});
    private AnimationPlayer.Playback action;
    private Key selected;
    private ItemStack inspectedStack;
    private EntityPlayer owner, useOwner;
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
        var next = new HashMap<Key, Binding>();
        var clips = new HashMap<String, AnimationClip>();
        for (String domain : new TreeSet<>(resources.getResourceDomains())) {
            var index = new ResourceLocation(domain, "animations/first_person.json");
            List<IResource> layers;
            try {
                layers = resources.getAllResources(index);
            } catch (FileNotFoundException absent) {
                continue;
            }
            for (var layer : layers) {
                try (var reader = new InputStreamReader(layer.getInputStream(), StandardCharsets.UTF_8)) {
                    var entries = new JsonParser().parse(reader)
                        .getAsJsonObject()
                        .getAsJsonArray("bindings");
                    for (var entry : entries) {
                        try {
                            loadBinding(entry.getAsJsonObject(), resources, clips, next);
                        } catch (IOException | RuntimeException ex) {
                            GTNRLib.LOG.warn("Skipping first-person binding {} in {}", entry, index, ex);
                        }
                    }
                } catch (RuntimeException | IOException e) {
                    GTNRLib.LOG.warn("Invalid first-person action binding in {}", index, e);
                }
            }
        }
        rules = Map.copyOf(next);
        reset();
        OFFHAND.reset();
    }

    private static void loadBinding(com.google.gson.JsonObject json, IResourceManager resources,
        Map<String, AnimationClip> clips, Map<Key, Binding> rules) throws IOException {
        String clipId = json.get("clip")
            .getAsString();
        if (!clips.containsKey(clipId)) {
            var id = new ResourceLocation(clipId);
            var path = new ResourceLocation(id.getResourceDomain(), "animations/" + id.getResourcePath() + ".json");
            try (var source = new InputStreamReader(
                resources.getResource(path)
                    .getInputStream(),
                StandardCharsets.UTF_8)) {
                NativeAnimationResource
                    .load(source, (name, clip) -> { if (name.equals(clipId)) clips.put(name, clip); });
            }
        }
        var clip = clips.get(clipId);
        var key = new Key(
            json.get("item")
                .getAsString(),
            json.has("damage") ? json.get("damage")
                .getAsInt() : -1,
            json.get("trigger")
                .getAsString());
        if (clip == null || clip.end() != AnimationClip.End.ONCE && !key.trigger.equals("use")
            || clip.tracks()
                .isEmpty()
            || clip.tracks()
                .keySet()
                .stream()
                .anyMatch(
                    channel -> !BONES.contains(channel.bone())
                        && !(isPalmInspection(key.trigger) && INSPECTION_TRANSFER.equals(channel))
                        && !(CUTTER_INSPECT.equals(key.trigger) && RIG_INSPECTION_CHANNELS.contains(channel))))
            throw new IllegalArgumentException("Invalid first-person clip " + clipId + " for " + key);
        double fadeIn = json.has("fadeIn") ? json.get("fadeIn")
            .getAsDouble() : .025;
        double fadeOut = json.has("fadeOut") ? json.get("fadeOut")
            .getAsDouble() : BLEND;
        if (!Double.isFinite(fadeIn) || fadeIn < 0 || !Double.isFinite(fadeOut) || fadeOut < 0)
            throw new IllegalArgumentException("Invalid first-person fade for " + key);
        var rotation = json.has("rotationBlend") ? switch (json.get("rotationBlend")
            .getAsString()) {
            case "shortest" -> AnimationPlayer.RotationBlend.SLERP_XYZ;
            case "euler" -> AnimationPlayer.RotationBlend.EULER;
            default -> throw new IllegalArgumentException("Unknown rotationBlend for " + key);
        } : AnimationPlayer.RotationBlend.EULER;
        var transition = json.has("transition") ? switch (json.get("transition")
            .getAsString()) {
            case "match_velocity" -> AnimationPlayer.Transition.MATCH_VELOCITY;
            case "cross_fade" -> AnimationPlayer.Transition.CROSS_FADE;
            default -> throw new IllegalArgumentException("Unknown transition for " + key);
        } : AnimationPlayer.Transition.CROSS_FADE;
        rules.put(key, new Binding(clip, fadeIn, fadeOut, rotation, transition));
    }

    void inspectHeld() {
        var p = Minecraft.getMinecraft().thePlayer;
        var stack = p == null ? null : held(p);
        if (stack == null) return;
        if (stack.getItem() instanceof ItemBlock) trigger(BLOCK_INSPECT);
        else if (!trigger(WRENCH_INSPECT)) trigger(CUTTER_INSPECT);
    }

    private static boolean isPalmInspection(String trigger) {
        return BLOCK_INSPECT.equals(trigger) || WRENCH_INSPECT.equals(trigger);
    }

    /** Play a named accepted action on the currently held stack. Unknown item/action pairs fall through. */
    public boolean trigger(String trigger) {
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        if (trigger == null || !enabled() || p == null || mc.gameSettings.thirdPersonView != 0 || rules.isEmpty())
            return false;
        var stack = held(p);
        boolean blockInspection = BLOCK_INSPECT.equals(trigger);
        boolean cutterInspection = CUTTER_INSPECT.equals(trigger);
        boolean inspecting = isPalmInspection(trigger) || cutterInspection;
        if (inspecting && (offhand || stack == null
            || blockInspection && !(stack.getItem() instanceof ItemBlock)
            || !canInspect(mc, p))) return false;
        String id = id(stack);
        Key key = new Key(id, stack == null ? -1 : stack.getItemDamage(), trigger);
        var binding = rules.get(key);
        if (binding == null) {
            key = new Key(id, -1, trigger);
            binding = rules.get(key);
        }
        if (binding == null && blockInspection) binding = rules.get(BLOCK_INSPECT_DEFAULT);
        if (binding == null) {
            if (inspectedStack != null && switch (trigger) {
                case "attack", "mining", "interact", "place", "use" -> true;
                default -> false;
            }) clear();
            return false;
        }
        if (cutterInspection && !canInspectCutter(stack)) return false;
        if (!matches(p, stack) || !inspecting && inspectedStack != null) clear();
        advance(p.ticksExisted);
        boolean chaining = action != null && action.active();
        if (inspecting && chaining) return false;
        owner = p;
        dimension = p.dimension;
        slot = currentSlot(p);
        selected = inspecting ? new Key(id, stack.getItemDamage(), trigger) : key;
        inspectedStack = blockInspection ? stack.copy() : inspecting ? stack : null;
        transferAllowed = isPalmInspection(trigger) && !FirstPersonAnimations.offhandOwnsLeftArm(1);
        startedTick = p.ticksExisted;
        tick = Math.max(tick, p.ticksExisted);
        if (chaining) action = player
            .transition(action, binding.clip, Math.max(BLEND, binding.fadeIn), binding.rotation, binding.transition);
        else {
            action = player.play(binding.clip, binding.fadeIn);
            action.rotationBlend(binding.rotation);
        }
        action.fadeOutAtEnd(binding.fadeOut);
        return true;
    }

    void apply(ItemStack stack, float partial, Matrix4f main, Matrix4f support, Matrix4f item) {
        inspectionLeftArm = false;
        if (action == null) return;
        var p = Minecraft.getMinecraft().thePlayer;
        if (!matches(p, held(p))) {
            clear();
            return;
        }
        // The outgoing stack may still be lowering while the server has already acknowledged the incoming tool.
        if (!matches(p, stack)) return;
        advance(p.ticksExisted);
        if (action == null || playing(CUTTER_INSPECT)) return;
        double ahead = Math.max(0, (p.ticksExisted + (double) partial - tick) / 20);
        WRIST.sample(player, ahead, local);
        item.invert(changeOfBasis)
            .mulLocal(local)
            .mulLocal(item);
        item.mul(local);
        item.mul(ITEM.sample(player, ahead, local));
        if (isPalmInspection(selected.trigger)) {
            if (transferAllowed && FirstPersonAnimations.offhandOwnsLeftArm(partial)) transferAllowed = false;
            var transfer = transferAllowed ? player.sample(INSPECTION_TRANSFER, ahead) : NO_TRANSFER;
            inspectionLeftArm = transfer.z() > 0;
            HmiPoseRenderer
                .poseInspection(main, support, item, changeOfBasis, transfer, BLOCK_INSPECT.equals(selected.trigger));
        } else main.mulLocal(changeOfBasis);
        MAIN.sample(player, ahead, rightGrip);
        SUPPORT.sample(player, ahead, leftGrip);
        main.mul(rightGrip);
        support.mul(leftGrip);
    }

    public boolean inspectionUsesLeftArm() {
        return inspectionLeftArm;
    }

    public RigInspection sampleRigInspection(ItemStack rendered, float partial) {
        if (!rigInspectionActive(rendered, partial)) return null;
        var p = Minecraft.getMinecraft().thePlayer;
        double ahead = Math.max(0, (p.ticksExisted + (double) partial - tick) / 20);
        return new RigInspection(
            player.sample(HINGE_POSITION, ahead),
            player.sample(HINGE_ROTATION, ahead),
            player.sample(JAW_ROTATION, ahead));
    }

    private boolean rigInspectionActive(ItemStack rendered, float partial) {
        if (!playing(CUTTER_INSPECT)) return false;
        var p = Minecraft.getMinecraft().thePlayer;
        if (p == null || !matches(p, rendered)) return false;
        if (!FirstPersonAnimations.offhandOwnsLeftArm(partial)) return true;
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
        return selected != null && selected.trigger.equals(trigger) && action.active();
    }

    boolean controls(EntityPlayer p, ItemStack rendered) {
        return action != null && action.active() && matches(p, rendered);
    }

    boolean observeUse(EntityPlayer p) {
        var active = p == null || FirstPersonAnimations.usingOffhandItem() != offhand ? null : p.getItemInUse();
        int damage = active == null ? 0 : active.getItemDamage();
        int currentSlot = p == null ? -1 : currentSlot(p);
        if (active != useStack || p != useOwner || currentSlot != useSlot || damage != useDamage) {
            if (playing("use")) action.stop(rules.get(selected).fadeOut);
            useOwner = p;
            useSlot = currentSlot;
            useStack = active;
            useDamage = damage;
            overridesUse = active != null && active.isItemEqual(held(p)) && trigger("use");
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
        if (p == null || !matches(p, held(p)) || inspectedStack != null && !canInspect(mc, p)) clear();
        else advance(p.ticksExisted);
    }

    private static boolean canInspect(Minecraft mc, EntityPlayer p) {
        return mc.currentScreen == null && !p.isInvisible() && !p.isUsingItem() && !p.isSwingInProgress;
    }

    private static boolean canInspectCutter(ItemStack stack) {
        if (HmiFirstPerson.INSTANCE.strokeActive() || FirstPersonAnimations.offhandOwnsLeftArm(1)) return false;
        var renderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
        return HmiItemBindings.tool(stack, renderer) instanceof HmiItemBindings.TwoHandedTool;
    }

    private void advance(double now) {
        if (action == null || now <= tick) return;
        player.advance((now - tick) / 20);
        tick = now;
        if (!action.active()) clear();
    }

    private boolean matches(EntityPlayer p, ItemStack stack) {
        return selected != null && owner == p
            && p.dimension == dimension
            && currentSlot(p) == slot
            && selected.item.equals(id(stack))
            && (selected.damage < 0 || stack != null && stack.getItemDamage() == selected.damage)
            && (inspectedStack == null
                || (BLOCK_INSPECT.equals(selected.trigger) ? ItemStack.areItemStackTagsEqual(inspectedStack, stack)
                    : inspectedStack == stack));
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
        inspectedStack = null;
        transferAllowed = inspectionLeftArm = false;
        owner = null;
        tick = 0;
        rightGrip.identity();
        leftGrip.identity();
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

    private record Key(String item, int damage, String trigger) {}

    private record Binding(AnimationClip clip, double fadeIn, double fadeOut, AnimationPlayer.RotationBlend rotation,
        AnimationPlayer.Transition transition) {}
}
