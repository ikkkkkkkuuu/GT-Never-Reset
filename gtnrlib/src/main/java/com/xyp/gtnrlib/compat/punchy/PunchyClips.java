package com.xyp.gtnrlib.compat.punchy;

import java.util.*;
import java.util.function.DoubleUnaryOperator;

import com.google.gson.*;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.*;

final class PunchyClips {

    static PunchyPack.Motion read(JsonObject obj) {
        Map<Channel, List<Key>> tracks = new LinkedHashMap<>();
        List<Marker> markers = new ArrayList<>();
        boolean v2 = false, dual = false;
        double speed = 5.5;
        double last = 0;
        for (var point : object(obj, "timeline").entrySet()) {
            double time = Double.parseDouble(point.getKey());
            JsonElement val = point.getValue();
            List<String> tokens = new ArrayList<>();
            if (val.isJsonArray()) for (var value : val.getAsJsonArray()) tokens.add(value.getAsString());
            else tokens.add(val.getAsString());
            for (String line : tokens) for (String raw : line.split(";")) {
                String token = raw.trim();
                if (token.isEmpty()) continue;
                if (token.equals("use_keyframes_v2")) v2 = true;
                if (time == 0 && token.equals("dual_handed")) dual = true;
                if (time == 0 && token.startsWith("anim_speed_")) speed = Double.parseDouble(token.substring(11));
                markers.add(new Marker(time, token));
                last = Math.max(time, last);
            }
        }
        if (!Double.isFinite(speed) || speed <= 0 || speed > 128)
            throw new IllegalArgumentException("invalid animation speed");
        for (var bone : object(obj, "bones").entrySet()) {
            for (Property property : Property.values()) {
                var channel = bone.getValue()
                    .getAsJsonObject()
                    .get(
                        property.name()
                            .toLowerCase(Locale.ROOT));
                if (channel == null) continue;
                var keys = new ArrayList<Key>();
                if (channel.isJsonArray()) keys.add(key(0, channel, property, v2));
                else for (var point : channel.getAsJsonObject()
                    .entrySet()) {
                        double t = Double.parseDouble(point.getKey());
                        last = Math.max(last, t);
                        keys.add(key(t, point.getValue(), property, v2));
                    }
                tracks.put(new Channel(bone.getKey(), property), keys);
            }
        }
        String loop = string(obj, "loop", "false").toLowerCase(Locale.ROOT);
        End end = switch (loop) {
            case "true", "loop" -> End.LOOP;
            case "hold_on_last_frame" -> End.HOLD;
            default -> End.ONCE;
        };
        double duration = obj.has("animation_length") ? obj.get("animation_length")
            .getAsDouble() : Math.max(last, .05);
        return new PunchyPack.Motion(new AnimationClip(duration, end, tracks, markers), speed, dual, v2);
    }

    private static Key key(double time, JsonElement element, Property property, boolean v2) {
        if (element.isJsonArray()) {
            Value v = value(element);
            return new Key(time, v, v, Curve.LINEAR);
        }
        var obj = element.getAsJsonObject();
        JsonElement post = obj.has("post") ? obj.get("post") : obj.has("vector") ? obj.get("vector") : obj.get("pre");
        if (post == null) throw new IllegalArgumentException("missing key value at " + time);
        Value after = value(post);
        Value before = v2 && property == Property.ROTATION && obj.has("pre") ? value(obj.get("pre")) : after;
        Curve mode = switch (string(obj, "lerp_mode", "linear").toLowerCase(Locale.ROOT)) {
            case "linear" -> Curve.LINEAR;
            case "catmullrom" -> Curve.CATMULL_ROM;
            case "step" -> Curve.STEP;
            default -> throw new IllegalArgumentException("unsupported interpolation: " + obj.get("lerp_mode"));
        };
        return new Key(time, before, after, mode, easing(string(obj, "easing", "linear")));
    }

    private static DoubleUnaryOperator easing(String name) {
        if (name.equals("linear")) return t -> t;
        String family = name.replaceFirst("^ease(InOut|In|Out)", "");
        DoubleUnaryOperator in = switch (family) {
            case "Sine" -> t -> 1 - Math.cos(t * Math.PI / 2);
            case "Quad" -> t -> t * t;
            case "Cubic" -> t -> t * t * t;
            case "Quart" -> t -> Math.pow(t, 4);
            case "Quint" -> t -> Math.pow(t, 5);
            case "Expo" -> t -> t == 0 ? 0 : Math.pow(2, 10 * t - 10);
            case "Circ" -> t -> 1 - Math.sqrt(1 - t * t);
            case "Back" -> t -> 2.70158 * t * t * t - 1.70158 * t * t;
            case "Elastic" -> t -> t == 0 || t == 1 ? t
                : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * 2 * Math.PI / 3);
            case "Bounce" -> t -> 1 - bounce(1 - t);
            default -> throw new IllegalArgumentException("unsupported easing: " + name);
        };
        if (name.startsWith("easeInOut")) {
            if (family.equals("Back")) return t -> {
                double s = 1.70158 * 1.525, x = t < .5 ? 2 * t : 2 * t - 2;
                return t < .5 ? x * x * ((s + 1) * x - s) / 2 : (x * x * ((s + 1) * x + s) + 2) / 2;
            };
            if (family.equals("Elastic")) return t -> {
                if (t == 0 || t == 1) return t;
                double wave = Math.sin((20 * t - 11.125) * 2 * Math.PI / 4.5);
                return t < .5 ? -Math.pow(2, 20 * t - 10) * wave / 2 : 1 + Math.pow(2, -20 * t + 10) * wave / 2;
            };
            return t -> t < .5 ? in.applyAsDouble(2 * t) / 2 : 1 - in.applyAsDouble(2 - 2 * t) / 2;
        }
        if (name.startsWith("easeOut")) return t -> 1 - in.applyAsDouble(1 - t);
        return in;
    }

    private static double bounce(double t) {
        if (t < 1 / 2.75) return 7.5625 * t * t;
        if (t < 2 / 2.75) {
            t -= 1.5 / 2.75;
            return 7.5625 * t * t + .75;
        }
        if (t < 2.5 / 2.75) {
            t -= 2.25 / 2.75;
            return 7.5625 * t * t + .9375;
        }
        t -= 2.625 / 2.75;
        return 7.5625 * t * t + .984375;
    }

    private static Value value(JsonElement element) {
        var a = element.getAsJsonArray();
        if (a.size() != 3) throw new IllegalArgumentException("expected three axes");
        if (a.get(0)
            .getAsJsonPrimitive()
            .isNumber()
            && a.get(1)
                .getAsJsonPrimitive()
                .isNumber()
            && a.get(2)
                .getAsJsonPrimitive()
                .isNumber())
            return new Vec(
                a.get(0)
                    .getAsDouble(),
                a.get(1)
                    .getAsDouble(),
                a.get(2)
                    .getAsDouble());
        var x = ClipExpression.compile(
            a.get(0)
                .getAsString());
        var y = ClipExpression.compile(
            a.get(1)
                .getAsString());
        var z = ClipExpression.compile(
            a.get(2)
                .getAsString());
        return t -> new Vec(x.applyAsDouble(t), y.applyAsDouble(t), z.applyAsDouble(t));
    }

    static JsonObject object(JsonObject obj, String key) {
        return obj.has(key) ? obj.getAsJsonObject(key) : new JsonObject();
    }

    static String string(JsonObject obj, String key, String fallback) {
        return obj.has(key) ? obj.get(key)
            .getAsString() : fallback;
    }
}
