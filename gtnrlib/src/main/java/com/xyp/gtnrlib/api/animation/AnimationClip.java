package com.xyp.gtnrlib.api.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** An immutable timeline. Time is in seconds, rotations are in degrees and scale is relative to one. */
public final class AnimationClip {

    public enum End {
        ONCE,
        LOOP,
        HOLD
    }

    public enum Curve {
        LINEAR,
        STEP,
        CATMULL_ROM
    }

    public enum Property {

        POSITION(0),
        ROTATION(0),
        SCALE(1);

        public final Vec rest;

        Property(double value) {
            rest = new Vec(value, value, value);
        }
    }

    @FunctionalInterface
    public interface Value {

        Vec at(double seconds);
    }

    public record Vec(double x, double y, double z) implements Value {

        @Override
        public Vec at(double seconds) {
            return this;
        }

        public Vec {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("non-finite vector");
        }

        public Vec mix(Vec other, double weight) {
            return new Vec(x + (other.x - x) * weight, y + (other.y - y) * weight, z + (other.z - z) * weight);
        }
    }

    public record Channel(String bone, Property property) {

        public Channel {
            if (bone.isEmpty()) throw new IllegalArgumentException("empty bone");
            Objects.requireNonNull(property);
        }
    }

    public record Key(double time, Value before, Value after, Curve curve,
        java.util.function.DoubleUnaryOperator easing) {

        public Key(double time, Value before, Value after, Curve curve) {
            this(time, before, after, curve, t -> t);
        }

        public Key {
            validTime(time);
            Objects.requireNonNull(before);
            Objects.requireNonNull(after);
            Objects.requireNonNull(curve);
            Objects.requireNonNull(easing);
        }

        public Key(double time, Vec value) {
            this(time, value, value, Curve.LINEAR);
        }
    }

    public record Marker(double time, String name) {

        public Marker {
            validTime(time);
            Objects.requireNonNull(name);
        }
    }

    private final double duration;
    private final End end;
    private final Map<Channel, List<Key>> tracks;
    private final List<Marker> markers;

    public AnimationClip(double duration, End end, Map<Channel, List<Key>> tracks, List<Marker> markers) {
        validTime(duration);
        if (duration == 0) throw new IllegalArgumentException("zero length clip");
        this.duration = duration;
        this.end = Objects.requireNonNull(end);
        Map<Channel, List<Key>> ordered = new LinkedHashMap<>();
        tracks.forEach((channel, values) -> {
            List<Key> keys = new ArrayList<>(values);
            keys.sort(Comparator.comparingDouble(Key::time));
            double last = -1;
            for (Key key : keys) {
                if (key.time <= last || key.time > duration)
                    throw new IllegalArgumentException("invalid key time: " + key.time);
                last = key.time;
            }
            if (!keys.isEmpty()) ordered.put(Objects.requireNonNull(channel), List.copyOf(keys));
        });
        this.tracks = Collections.unmodifiableMap(ordered);
        List<Marker> sorted = new ArrayList<>(markers);
        sorted.sort(Comparator.comparingDouble(Marker::time));
        if (!sorted.isEmpty() && sorted.get(sorted.size() - 1).time > duration)
            throw new IllegalArgumentException("marker past end");
        this.markers = List.copyOf(sorted);
    }

    public double duration() {
        return duration;
    }

    public End end() {
        return end;
    }

    public Map<Channel, List<Key>> tracks() {
        return tracks;
    }

    public List<Marker> markers() {
        return markers;
    }

    /** Samples a local time without firing events. Endpoints clamp, even for looping clips. */
    public Vec sample(Channel channel, double seconds) {
        validTime(seconds);
        List<Key> keys = tracks.get(channel);
        if (keys == null) return channel.property.rest;
        int lo = 0, hi = keys.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (keys.get(mid).time <= seconds) lo = mid + 1;
            else hi = mid;
        }
        if (lo == 0) return keys.get(0).before.at(seconds);
        Key left = keys.get(lo - 1);
        Vec l = left.after.at(seconds);
        if (lo == keys.size() || left.time == seconds || left.curve == Curve.STEP) return l;
        Key right = keys.get(lo);
        double t = right.easing.applyAsDouble((seconds - left.time) / (right.time - left.time));
        Vec r = right.before.at(seconds);
        if (left.curve != Curve.CATMULL_ROM && right.curve != Curve.CATMULL_ROM) return l.mix(r, t);
        Vec p = keys.get(Math.max(0, lo - 2)).after.at(seconds);
        Vec q = keys.get(Math.min(keys.size() - 1, lo + 1)).before.at(seconds);
        return new Vec(cubic(p.x, l.x, r.x, q.x, t), cubic(p.y, l.y, r.y, q.y, t), cubic(p.z, l.z, r.z, q.z, t));
    }

    private static double cubic(double a, double b, double c, double d, double t) {
        return b + 0.5 * t * ((c - a) + t * ((2 * a - 5 * b + 4 * c - d) + t * (3 * b - a - 3 * c + d)));
    }

    static void validTime(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("invalid time: " + value);
    }
}
