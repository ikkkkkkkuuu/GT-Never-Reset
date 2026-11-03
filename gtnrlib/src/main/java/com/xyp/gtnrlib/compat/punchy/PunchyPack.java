package com.xyp.gtnrlib.compat.punchy;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.xyp.gtnrlib.animation.resource.AnimationPackFiles;
import com.xyp.gtnrlib.api.animation.AnimationClip;

/** Resolved, read-only subset of the Punchy first-person format; file order is low to high priority. */
public final class PunchyPack {

    private static final List<String> ROOTS = List.of(
        "assets/minecraft/punchy/animations/",
        "assets/minecraft/punchy/compat/",
        "assets/punchy/animations/",
        "assets/punchy/compat/");

    public record Motion(AnimationClip clip, double speed, boolean dualHanded, boolean v2) {}

    private record Rule(String target, String action) {}

    private final Map<String, Motion> clips;
    private final Map<Rule, List<String>> bindings;

    private PunchyPack(Map<String, Motion> clips, Map<Rule, List<String>> bindings) {
        this.clips = Map.copyOf(clips);
        this.bindings = Map.copyOf(bindings);
    }

    public static PunchyPack empty() {
        return new PunchyPack(Map.of(), Map.of());
    }

    public boolean hasMotions() {
        return !clips.isEmpty();
    }

    public Motion clip(String name) {
        return clips.get(name);
    }

    public Motion select(String item, String kind, String action, int sequence) {
        for (String target : List.of(item, kind, "*")) {
            List<String> names = bindings.get(new Rule(target, action));
            if (names != null) return clips.get(names.get(Math.floorMod(sequence, names.size())));
        }
        return clips.get(action);
    }

    public static PunchyPack load(List<Path> sources, Consumer<String> diagnostics) throws IOException {
        Map<String, Motion> clips = new LinkedHashMap<>();
        Map<Rule, List<String>> rules = new LinkedHashMap<>();
        for (Path source : sources) AnimationPackFiles.read(source, ROOTS, (path, json) -> {
            String origin = source.getFileName() + "/" + path;
            try {
                JsonObject root;
                try (JsonReader reader = new JsonReader(new StringReader(json))) {
                    root = read(reader, 0).getAsJsonObject();
                    if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("trailing JSON");
                }
                if (path.contains("/animations/")) {
                    for (var entry : PunchyClips.object(root, "animations")
                        .entrySet()) {
                        try {
                            clips.put(
                                entry.getKey(),
                                PunchyClips.read(
                                    entry.getValue()
                                        .getAsJsonObject()));
                        } catch (RuntimeException e) {
                            diagnostics.accept(origin + " animations." + entry.getKey() + ": " + e.getMessage());
                        }
                    }
                } else {
                    Map<Rule, List<String>> file = new LinkedHashMap<>();
                    for (String section : List.of("item", "itemSpecific")) {
                        for (var entry : PunchyClips.object(root, section)
                            .entrySet()) {
                            rules(
                                file,
                                entry.getKey(),
                                entry.getValue()
                                    .getAsJsonObject()
                                    .get("customAnimation"),
                                diagnostics,
                                origin);
                        }
                    }
                    rules(
                        file,
                        "emptyHand",
                        PunchyClips.object(root, "emptyHand")
                            .get("customAnimation"),
                        diagnostics,
                        origin);
                    rules(file, "*", root.get("default_animations"), diagnostics, origin);
                    rules.putAll(file);
                }
            } catch (Exception e) {
                diagnostics.accept(origin + ": " + e.getMessage());
            }
        });
        rules.forEach((key, names) -> {
            for (String name : names) if (!clips.containsKey(name))
                diagnostics.accept(key.target + "/" + key.action + ": missing clip " + name);
        });
        return new PunchyPack(clips, rules);
    }

    private static void rules(Map<Rule, List<String>> rules, String target, JsonElement list,
        Consumer<String> diagnostics, String origin) {
        if (list == null) return;
        for (var element : list.getAsJsonArray()) {
            var entry = element.getAsJsonObject();
            String action = PunchyClips.string(entry, "type", "")
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
            if (action.equals("miningv2")) action = "mining";
            if (!List.of("attack", "mining", "idle", "useitem", "walk", "sprint", "interaction")
                .contains(action)) continue;
            if (entry.entrySet()
                .stream()
                .anyMatch(
                    e -> !List.of("type", "name", "animation", "var_1", "var_2", "var_3")
                        .contains(e.getKey()))) {
                diagnostics.accept(origin + " " + target + "/" + action + ": unsupported conditional or phased rule");
                continue;
            }
            List<String> names = new ArrayList<>();
            for (String k : List.of("var_1", "var_2", "var_3")) if (entry.has(k)) names.add(
                entry.get(k)
                    .getAsString());
            if (names.isEmpty())
                names.add(PunchyClips.string(entry, "name", PunchyClips.string(entry, "animation", "")));
            if (!names.get(0)
                .isEmpty()) rules.put(new Rule(target, action), List.copyOf(names));
        }
    }

    private static JsonElement read(JsonReader reader, int depth) throws IOException {
        if (depth > 48) throw new IOException("JSON nesting limit");
        switch (reader.peek()) {
            case BEGIN_OBJECT: {
                reader.beginObject();
                JsonObject result = new JsonObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (result.has(name)) throw new IOException("duplicate key: " + name);
                    result.add(name, read(reader, depth + 1));
                }
                reader.endObject();
                return result;
            }
            case BEGIN_ARRAY: {
                reader.beginArray();
                JsonArray result = new JsonArray();
                while (reader.hasNext()) result.add(read(reader, depth + 1));
                reader.endArray();
                return result;
            }
            case STRING:
                return new JsonPrimitive(reader.nextString());
            case NUMBER:
                return new JsonPrimitive(reader.nextDouble());
            case BOOLEAN:
                return new JsonPrimitive(reader.nextBoolean());
            case NULL:
                reader.nextNull();
                return JsonNull.INSTANCE;
            default:
                throw new IOException("unexpected JSON token " + reader.peek());
        }
    }
}
