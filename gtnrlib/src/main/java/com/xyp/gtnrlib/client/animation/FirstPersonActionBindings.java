package com.xyp.gtnrlib.client.animation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.animation.resource.NativeAnimationResource;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class FirstPersonActionBindings {

    static final String BLOCK_INSPECT = "gtnrlib:block_inspect";
    static final String WRENCH_INSPECT = "gtnrlib:wrench_inspect";
    static final String CUTTER_INSPECT = "gtnrlib:wire_cutter_inspect";
    static final String SCREWDRIVER_INSPECT = "gtnrlib:screwdriver_inspect";
    static final String CROSSBOW_INSPECT = "gtnrlib:crossbow_inspect";
    static final String WAND_INSPECT = "gtnrlib:wand_inspect";
    static final String SWORD_INSPECT = "gtnrlib:sword_inspect";
    static final String PICKAXE_INSPECT = "gtnrlib:pickaxe_inspect";
    static final Channel TRANSFER = new Channel("transfer", Property.POSITION);
    static final Channel CARRY = new Channel("carry", Property.POSITION);
    static final Channel HINGE_POSITION = new Channel("hinge", Property.POSITION);
    static final Channel HINGE_ROTATION = new Channel("hinge", Property.ROTATION);
    static final Channel JAW_ROTATION = new Channel("jaw", Property.ROTATION);
    static final Channel STOCK_POSITION = new Channel("stock", Property.POSITION);
    static final Channel STOCK_ROTATION = new Channel("stock", Property.ROTATION);
    static final double BLEND = .08;
    static final FirstPersonActionBindings EMPTY = new FirstPersonActionBindings(Map.of(), Map.of());
    private static final Set<String> BONES = Set.of("wrist", "item", "left_arm", "right_arm");

    private final Map<Key, Binding> actions;
    private final Map<ItemKey, String> inspections;

    private FirstPersonActionBindings(Map<Key, Binding> actions, Map<ItemKey, String> inspections) {
        this.actions = Map.copyOf(actions);
        this.inspections = Map.copyOf(inspections);
    }

    static FirstPersonActionBindings load(IResourceManager resources) throws IOException {
        var actions = new LinkedHashMap<Key, Binding>();
        var clips = new HashMap<String, List<AnimationClip>>();
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
                            var json = entry.getAsJsonObject();
                            var key = new Key(string(json, "item"), damage(json), string(json, "trigger"));
                            var binding = loadBinding(key, json, resources, clips);
                            actions.remove(key);
                            actions.put(key, binding);
                        } catch (IOException | RuntimeException ex) {
                            GTNRLib.LOG.warn("Skipping first-person binding {} in {}", entry, index, ex);
                        }
                    }
                } catch (RuntimeException | IOException e) {
                    GTNRLib.LOG.warn("Invalid first-person action binding in {}", index, e);
                }
            }
        }
        var inspections = new HashMap<ItemKey, String>();
        actions.forEach(
            (key, binding) -> {
                if (binding.inspection.custom()) inspections.put(new ItemKey(key.item, key.damage), key.trigger);
            });
        return new FirstPersonActionBindings(actions, inspections);
    }

    Binding get(Key key) {
        return actions.get(key);
    }

    String inspection(String item, int damage) {
        String exact = inspections.get(new ItemKey(item, damage));
        return exact == null ? inspections.get(new ItemKey(item, -1)) : exact;
    }

    private static Binding loadBinding(Key key, JsonObject json, IResourceManager resources,
        Map<String, List<AnimationClip>> clips) throws IOException {
        String clipId = string(json, "clip");
        var inspection = Inspection.of(key, json);
        var sounds = sounds(json);
        var candidates = clips.get(clipId);
        if (candidates == null) {
            candidates = loadClips(resources, clipId);
            clips.put(clipId, candidates);
        }
        boolean repeat = key.trigger.equals("use") && inspection == Inspection.NONE;
        var clip = candidates.stream()
            .filter(
                candidate -> (repeat || candidate.end() == AnimationClip.End.ONCE) && !candidate.tracks()
                    .isEmpty()
                    && candidate.tracks()
                        .keySet()
                        .stream()
                        .allMatch(inspection::accepts)
                    && sounds.keySet()
                        .stream()
                        .allMatch(
                            name -> candidate.markers()
                                .stream()
                                .anyMatch(
                                    marker -> marker.name()
                                        .equals(name))))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No compatible first-person clip " + clipId + " for " + key));
        double fadeIn = number(json, "fadeIn", .025, 0, Double.MAX_VALUE);
        double fadeOut = number(json, "fadeOut", BLEND, 0, Double.MAX_VALUE);
        var rotation = json.has("rotationBlend") ? switch (string(json, "rotationBlend")) {
            case "shortest" -> AnimationPlayer.RotationBlend.SLERP_XYZ;
            case "euler" -> AnimationPlayer.RotationBlend.EULER;
            default -> throw new IllegalArgumentException("Unknown rotationBlend for " + key);
        } : AnimationPlayer.RotationBlend.EULER;
        var transition = json.has("transition") ? switch (string(json, "transition")) {
            case "match_velocity" -> AnimationPlayer.Transition.MATCH_VELOCITY;
            case "cross_fade" -> AnimationPlayer.Transition.CROSS_FADE;
            default -> throw new IllegalArgumentException("Unknown transition for " + key);
        } : AnimationPlayer.Transition.CROSS_FADE;
        ResourceLocation effect = json.has("effect") ? new ResourceLocation(string(json, "effect")) : null;
        if (effect != null && !inspection.custom())
            throw new IllegalArgumentException("Render effects require an explicit inspection mode");
        return new Binding(clip, fadeIn, fadeOut, rotation, transition, inspection, effect, sounds);
    }

    private static List<AnimationClip> loadClips(IResourceManager resources, String clipId) throws IOException {
        var id = new ResourceLocation(clipId);
        var path = new ResourceLocation(id.getResourceDomain(), "animations/" + id.getResourcePath() + ".json");
        var layers = resources.getAllResources(path);
        var found = new ArrayList<AnimationClip>();
        for (int i = layers.size() - 1; i >= 0; i--) {
            try (var reader = new InputStreamReader(
                layers.get(i)
                    .getInputStream(),
                StandardCharsets.UTF_8)) {
                NativeAnimationResource.load(reader, (name, clip) -> { if (name.equals(clipId)) found.add(clip); });
            } catch (RuntimeException | IOException ex) {
                GTNRLib.LOG.warn("Skipping invalid first-person clip layer {} at {}", i, path, ex);
            }
        }
        if (!found.isEmpty()) return List.copyOf(found);
        throw new IOException("No valid definition for first-person clip " + clipId);
    }

    private static Map<String, SoundCue> sounds(JsonObject json) {
        if (!json.has("sounds")) return Map.of();
        var result = new HashMap<String, SoundCue>();
        for (var entry : json.getAsJsonObject("sounds")
            .entrySet()) {
            var cue = entry.getValue()
                .getAsJsonObject();
            var id = new ResourceLocation(string(cue, "id"));
            float pitch = (float) number(cue, "pitch", 1, .5, 2);
            float volume = (float) number(cue, "volume", .25, 0, 1);
            result.put(entry.getKey(), new SoundCue(id, volume, pitch));
        }
        return Map.copyOf(result);
    }

    private static String string(JsonObject json, String field) {
        var value = json.getAsJsonPrimitive(field);
        if (value == null || !value.isString()
            || value.getAsString()
                .isBlank())
            throw new IllegalArgumentException("Expected nonempty " + field);
        return value.getAsString();
    }

    private static int damage(JsonObject json) {
        double value = number(json, "damage", -1, -1, Integer.MAX_VALUE);
        if (value != (int) value) throw new IllegalArgumentException("Item damage must be an integer");
        return (int) value;
    }

    private static double number(JsonObject json, String field, double fallback, double minimum, double maximum) {
        if (!json.has(field)) return fallback;
        var value = json.getAsJsonPrimitive(field);
        if (!value.isNumber()) throw new IllegalArgumentException("Expected numeric " + field);
        double result = value.getAsDouble();
        if (!Double.isFinite(result) || result < minimum || result > maximum)
            throw new IllegalArgumentException("Out-of-range " + field);
        return result;
    }

    enum Inspection {

        NONE,
        ONE_HANDED,
        TWO_HANDED,
        PALM,
        BLOCK,
        WRENCH,
        CUTTER,
        CROSSBOW;

        static Inspection of(Key key, JsonObject json) {
            var legacy = switch (key.trigger) {
                case BLOCK_INSPECT -> BLOCK;
                case WRENCH_INSPECT -> WRENCH;
                case CUTTER_INSPECT -> CUTTER;
                case CROSSBOW_INSPECT -> CROSSBOW;
                case SCREWDRIVER_INSPECT, WAND_INSPECT, SWORD_INSPECT, PICKAXE_INSPECT -> PALM;
                default -> NONE;
            };
            if (!json.has("inspection")) return legacy;
            if (legacy != NONE || key.item.startsWith("#")
                || key.trigger.equals("use")
                || key.trigger.equals("attack")
                || key.trigger.equals("mining")
                || key.trigger.equals("place")
                || key.trigger.equals("interact"))
                throw new IllegalArgumentException("Custom inspections require an item id and a distinct trigger");
            return switch (string(json, "inspection")) {
                case "one_handed" -> ONE_HANDED;
                case "two_handed" -> TWO_HANDED;
                default -> throw new IllegalArgumentException("Unknown inspection mode for " + key);
            };
        }

        boolean custom() {
            return this == ONE_HANDED || this == TWO_HANDED;
        }

        boolean rig() {
            return this == CUTTER || this == CROSSBOW;
        }

        boolean transfer() {
            return this == BLOCK || this == WRENCH || this == TWO_HANDED;
        }

        boolean accepts(Channel channel) {
            if (BONES.contains(channel.bone()) || transfer() && TRANSFER.equals(channel)) return true;
            return switch (this) {
                case TWO_HANDED -> CARRY.equals(channel);
                case CUTTER -> HINGE_POSITION.equals(channel) || HINGE_ROTATION.equals(channel)
                    || JAW_ROTATION.equals(channel);
                case CROSSBOW -> STOCK_POSITION.equals(channel) || STOCK_ROTATION.equals(channel);
                default -> false;
            };
        }
    }

    private record ItemKey(String item, int damage) {}

    record Key(String item, int damage, String trigger) {}

    record Binding(AnimationClip clip, double fadeIn, double fadeOut, AnimationPlayer.RotationBlend rotation,
        AnimationPlayer.Transition transition, Inspection inspection, ResourceLocation effect,
        Map<String, SoundCue> sounds) {}

    record SoundCue(ResourceLocation id, float volume, float pitch) {

        ISound sound() {
            return new LocalCue(this);
        }
    }

    private static final class LocalCue extends PositionedSound {

        private LocalCue(SoundCue cue) {
            super(cue.id);
            volume = cue.volume;
            field_147663_c = cue.pitch;
            field_147666_i = AttenuationType.NONE;
        }
    }
}
