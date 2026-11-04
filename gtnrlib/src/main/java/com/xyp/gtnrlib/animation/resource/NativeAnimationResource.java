package com.xyp.gtnrlib.animation.resource;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Curve;
import com.xyp.gtnrlib.api.animation.AnimationClip.End;
import com.xyp.gtnrlib.api.animation.AnimationClip.Key;
import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;
import com.xyp.gtnrlib.api.animation.AnimationLibrary;

/** Versioned interchange format for mod providers. Readers are owned and closed by the caller. */
public final class NativeAnimationResource {

    public static void load(Reader source, AnimationLibrary.Registrar registrar) {
        JsonObject root = new JsonParser().parse(source)
            .getAsJsonObject();
        if (!"1".equals(
            root.get("version")
                .getAsString()))
            throw new IllegalArgumentException("unknown animation resource version");
        Map<String, AnimationClip> result = new LinkedHashMap<>();
        for (var entry : root.getAsJsonObject("clips")
            .entrySet()) {
            try {
                JsonObject clip = entry.getValue()
                    .getAsJsonObject();
                End end = clip.has("end") ? End.valueOf(
                    clip.get("end")
                        .getAsString()
                        .toUpperCase(Locale.ROOT))
                    : End.ONCE;
                Map<Channel, List<Key>> tracks = new LinkedHashMap<>();
                for (var node : clip.getAsJsonArray("tracks")) {
                    var track = node.getAsJsonObject();
                    Channel channel = new Channel(
                        track.get("bone")
                            .getAsString(),
                        Property.valueOf(
                            track.get("property")
                                .getAsString()
                                .toUpperCase(Locale.ROOT)));
                    List<Key> keys = new ArrayList<>();
                    for (var point : track.getAsJsonArray("keys")) {
                        var key = point.getAsJsonObject();
                        Vec value = vector(key.getAsJsonArray("value"));
                        keys.add(
                            new Key(
                                key.get("time")
                                    .getAsDouble(),
                                key.has("before") ? vector(key.getAsJsonArray("before")) : value,
                                value,
                                key.has("curve") ? Curve.valueOf(
                                    key.get("curve")
                                        .getAsString()
                                        .toUpperCase(Locale.ROOT))
                                    : Curve.LINEAR));
                    }
                    if (tracks.put(channel, keys) != null)
                        throw new IllegalArgumentException("duplicate channel " + channel);
                }
                List<Marker> markers = new ArrayList<>();
                if (clip.has("markers")) for (var point : clip.getAsJsonArray("markers")) {
                    var marker = point.getAsJsonObject();
                    markers.add(
                        new Marker(
                            marker.get("time")
                                .getAsDouble(),
                            marker.get("name")
                                .getAsString()));
                }
                result.put(
                    entry.getKey(),
                    new AnimationClip(
                        clip.get("duration")
                            .getAsDouble(),
                        end,
                        tracks,
                        markers));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("clip " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }
        result.forEach(registrar::register);
    }

    private static Vec vector(JsonArray values) {
        if (values.size() != 3) throw new IllegalArgumentException("expected three axes");
        return new Vec(
            values.get(0)
                .getAsDouble(),
            values.get(1)
                .getAsDouble(),
            values.get(2)
                .getAsDouble());
    }
}
