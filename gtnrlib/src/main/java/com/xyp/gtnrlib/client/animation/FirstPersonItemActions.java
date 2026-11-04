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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.joml.Matrix4f;

import com.google.gson.JsonParser;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.animation.resource.NativeAnimationResource;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Resource-bound actions in the held item's local coordinate space, on top of the live first-person pose. */
@SideOnly(Side.CLIENT)
public final class FirstPersonItemActions {

    public static final FirstPersonItemActions INSTANCE = new FirstPersonItemActions();
    private static final double BLEND = .08;
    private static final Set<String> BONES = Set.of("wrist", "item", "left_arm", "right_arm");
    private static final Bone WRIST = new Bone("wrist"), ITEM = new Bone("item"), MAIN = new Bone("right_arm"),
        SUPPORT = new Bone("left_arm");
    private final Matrix4f local = new Matrix4f(), changeOfBasis = new Matrix4f();
    private final Matrix4f leftGrip = new Matrix4f(), rightGrip = new Matrix4f();
    private final AnimationPlayer player = new AnimationPlayer(marker -> {});
    private Map<Key, Binding> rules = Map.of();
    private AnimationPlayer.Playback action;
    private Key selected;
    private EntityPlayer owner, useOwner;
    private ItemStack useStack;
    private int useSlot, useDamage;
    private boolean overridesUse;
    private int slot, dimension, startedTick;
    private double tick;

    private FirstPersonItemActions() {}

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
        useOwner = null;
        useStack = null;
        overridesUse = false;
        rules = Map.copyOf(next);
        clear();
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
                .anyMatch(channel -> !BONES.contains(channel.bone())))
            throw new IllegalArgumentException("Invalid first-person clip " + clipId + " for " + key);
        double fadeIn = json.has("fadeIn") ? json.get("fadeIn")
            .getAsDouble() : .025;
        double fadeOut = json.has("fadeOut") ? json.get("fadeOut")
            .getAsDouble() : BLEND;
        if (!Double.isFinite(fadeIn) || fadeIn < 0 || !Double.isFinite(fadeOut) || fadeOut < 0)
            throw new IllegalArgumentException("Invalid first-person fade for " + key);
        rules.put(key, new Binding(clip, fadeIn, fadeOut));
    }

    /** Play a named accepted action on the currently held stack. Unknown item/action pairs fall through. */
    public boolean trigger(String trigger) {
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        if (!HmiFirstPerson.INSTANCE.enabled() || p == null || mc.gameSettings.thirdPersonView != 0 || rules.isEmpty())
            return false;
        var stack = p.getHeldItem();
        String id = id(stack);
        Key key = new Key(id, stack == null ? -1 : stack.getItemDamage(), trigger);
        var binding = rules.get(key);
        if (binding == null) {
            key = new Key(id, -1, trigger);
            binding = rules.get(key);
        }
        if (binding == null) return false;
        if (!matches(p, stack)) clear();
        advance(p.ticksExisted);
        boolean chaining = action != null && action.active();
        if (chaining) action.stop(BLEND);
        owner = p;
        dimension = p.dimension;
        slot = p.inventory.currentItem;
        selected = key;
        startedTick = p.ticksExisted;
        tick = Math.max(tick, p.ticksExisted);
        action = player.play(binding.clip, chaining ? Math.max(BLEND, binding.fadeIn) : binding.fadeIn);
        action.fadeOutAtEnd(binding.fadeOut);
        return true;
    }

    void apply(ItemStack stack, float partial, Matrix4f main, Matrix4f support, Matrix4f item) {
        if (action == null) return;
        var p = Minecraft.getMinecraft().thePlayer;
        if (!matches(p, p.getHeldItem())) {
            clear();
            return;
        }
        // The outgoing stack may still be lowering while the server has already acknowledged the incoming tool.
        if (!matches(p, stack)) return;
        advance(p.ticksExisted);
        if (action == null) return;
        double ahead = Math.max(0, (p.ticksExisted + (double) partial - tick) / 20);
        WRIST.sample(player, ahead, local);
        item.invert(changeOfBasis)
            .mulLocal(local)
            .mulLocal(item);
        main.mulLocal(changeOfBasis);
        item.mul(local);
        MAIN.sample(player, ahead, rightGrip);
        SUPPORT.sample(player, ahead, leftGrip);
        main.mul(rightGrip);
        support.mul(leftGrip);
        item.mul(ITEM.sample(player, ahead, local));
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
        var active = p == null ? null : p.getItemInUse();
        int damage = active == null ? 0 : active.getItemDamage();
        int currentSlot = p == null ? -1 : p.inventory.currentItem;
        if (active != useStack || p != useOwner || currentSlot != useSlot || damage != useDamage) {
            if (playing("use")) action.stop(rules.get(selected).fadeOut);
            useOwner = p;
            useSlot = currentSlot;
            useStack = active;
            useDamage = damage;
            overridesUse = active != null && active.isItemEqual(p.getHeldItem()) && trigger("use");
        }
        return overridesUse;
    }

    boolean startedThisTick(String name, EntityPlayer p) {
        return playing(name) && startedTick == p.ticksExisted && matches(p, p.getHeldItem());
    }

    void tick() {
        var mc = Minecraft.getMinecraft();
        var p = mc.gameSettings.thirdPersonView == 0 && HmiFirstPerson.INSTANCE.enabled() ? mc.thePlayer : null;
        observeUse(p);
        if (action == null) return;
        if (p == null || !matches(p, p.getHeldItem())) clear();
        else advance(p.ticksExisted);
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
            && p.inventory.currentItem == slot
            && selected.item.equals(id(stack))
            && (selected.damage < 0 || stack != null && stack.getItemDamage() == selected.damage);
    }

    private static String id(ItemStack stack) {
        return stack == null ? "minecraft:air" : String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
    }

    void clear() {
        player.clear();
        action = null;
        selected = null;
        owner = null;
        tick = 0;
        rightGrip.identity();
        leftGrip.identity();
    }

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

    private record Binding(AnimationClip clip, double fadeIn, double fadeOut) {}
}
