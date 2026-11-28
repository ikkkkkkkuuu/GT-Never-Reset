package com.xyp.gtnrlib.api.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.xyp.gtnrlib.animation.RotationCurves;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.End;
import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;

/** One target on one owner thread. Later layers override only channels authored by that clip. */
public final class AnimationPlayer {

    public enum Blend {
        OVERRIDE,
        ADDITIVE
    }

    /** Match the rotation order used by the renderer consuming the sampled Euler angles. */
    public enum RotationBlend {

        EULER,
        SLERP_XYZ,
        SLERP_ZYX;

        private Vec mix(Channel channel, Vec from, Vec to, double weight) {
            if (weight == 0 || from == to) return from;
            if (weight == 1) return to;
            if (this == EULER || channel.property() != AnimationClip.Property.ROTATION) return from.mix(to, weight);
            return this == SLERP_XYZ ? RotationCurves.slerpXYZ(from, to, weight)
                : RotationCurves.slerp(from, to, weight);
        }
    }

    public enum Transition {
        CROSS_FADE,
        MATCH_VELOCITY
    }

    private static final double VELOCITY_SAMPLE_SECONDS = .001;

    private record VelocitySample(Vec pose, Vec neighbor) {

        Vec project(Channel channel, RotationBlend rotation, double intervals) {
            if (intervals == 0 || pose.equals(neighbor)) return pose;
            if (channel.property() == AnimationClip.Property.ROTATION && rotation != RotationBlend.EULER)
                return RotationCurves.extrapolate(pose, neighbor, intervals, rotation == RotationBlend.SLERP_XYZ);
            return pose.mix(neighbor, intervals);
        }
    }

    private final List<Playback> layers = new ArrayList<>();
    private final BiConsumer<Playback, Marker> listener;

    public AnimationPlayer(Consumer<Marker> listener) {
        Objects.requireNonNull(listener);
        this.listener = (source, marker) -> listener.accept(marker);
    }

    private AnimationPlayer(BiConsumer<Playback, Marker> listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    /** Report the playback that emitted each marker. It may already be inactive when the event is delivered. */
    public static AnimationPlayer withSources(BiConsumer<Playback, Marker> listener) {
        return new AnimationPlayer(listener);
    }

    public Playback play(AnimationClip clip, double fadeIn) {
        return play(clip, fadeIn, 0);
    }

    /** Higher priorities are applied last, independent of when another state starts. */
    public Playback play(AnimationClip clip, double fadeIn, int priority) {
        return play(clip, fadeIn, priority, Blend.OVERRIDE, channel -> true);
    }

    public Playback play(AnimationClip clip, double fadeIn, int priority, Blend blend,
        java.util.function.Predicate<Channel> mask) {
        AnimationClip.validTime(fadeIn);
        Playback handle = new Playback(
            Objects.requireNonNull(clip),
            fadeIn,
            priority,
            Objects.requireNonNull(blend),
            Objects.requireNonNull(mask));
        int index = layers.size();
        while (index > 0 && layers.get(index - 1).priority > priority) index--;
        layers.add(index, handle);
        return handle;
    }

    /** Replace an override in its existing slot, mixing both live poses over the same lower layers. */
    public Playback crossFade(Playback outgoing, AnimationClip clip, double seconds, RotationBlend rotation) {
        return transition(outgoing, clip, seconds, rotation, Transition.CROSS_FADE);
    }

    /** MATCH_VELOCITY takes over the outgoing pose and velocity in this layer's slot. Time is in wall seconds. */
    public Playback transition(Playback outgoing, AnimationClip clip, double seconds, RotationBlend rotation,
        Transition mode) {
        AnimationClip.validTime(seconds);
        Objects.requireNonNull(rotation);
        Objects.requireNonNull(mode);
        int index = layers.indexOf(outgoing);
        if (index < 0 || !outgoing.active || outgoing.blend != Blend.OVERRIDE)
            throw new IllegalArgumentException("expected an active override layer on this player");
        var next = new Playback(
            Objects.requireNonNull(clip),
            seconds,
            outgoing.priority,
            Blend.OVERRIDE,
            outgoing.mask);
        next.rotation = rotation;
        if (mode == Transition.MATCH_VELOCITY && seconds > 0) {
            var channels = new HashSet<>(
                clip.tracks()
                    .keySet());
            for (Playback source = outgoing; source != null; source = source.previous) {
                channels.addAll(
                    source.clip.tracks()
                        .keySet());
                channels.addAll(source.departures.keySet());
            }
            var departures = new HashMap<Channel, VelocitySample>();
            for (var channel : channels) {
                if (outgoing.mask.test(channel)) departures.put(
                    channel,
                    new VelocitySample(
                        sample(channel, 0, index + 1),
                        sample(channel, VELOCITY_SAMPLE_SECONDS, index + 1)));
            }
            next.departures = departures;
        }
        layers.set(index, next);
        if (seconds == 0 || mode == Transition.MATCH_VELOCITY) outgoing.stop(0);
        else if (outgoing.previous != null && outgoing.age == 0
            && outgoing.fadeIn > 0
            && outgoing.clip.markers()
                .isEmpty()) {
                    // A same-tick retrigger can skip an incoming clip that has contributed no pose yet.
                    next.previous = outgoing.previous;
                    outgoing.previous = null;
                    outgoing.stop(0);
                } else
            next.previous = outgoing;
        return next;
    }

    public void clear() {
        for (Playback layer : layers) layer.stop(0);
        layers.clear();
    }

    /** Advance simulation once, then deliver events. Listener callbacks may start and stop clips. */
    public void advance(double seconds) {
        AnimationClip.validTime(seconds);
        List<FiredMarker> fired = new ArrayList<>();
        for (Playback layer : layers) layer.advance(seconds, fired, false);
        layers.removeIf(layer -> !layer.active);
        for (var event : fired) listener.accept(event.source, event.marker);
    }

    /** Read-only prediction relative to the last advance, for sub-tick rendering and previews. */
    public Vec sample(Channel channel, double ahead) {
        AnimationClip.validTime(ahead);
        return sample(channel, ahead, layers.size());
    }

    private Vec sample(Channel channel, double ahead, int layerCount) {
        Vec result = channel.property().rest;
        for (int i = 0; i < layerCount; i++) result = layers.get(i)
            .sample(channel, ahead, result);
        return result;
    }

    public static final class Playback {

        private final AnimationClip clip;
        private final double fadeIn;
        private final int priority;
        private double position, age, fadeAge, fadeLength, endFade;
        private double speed = 1;
        private boolean started, stopping;
        private boolean active = true;

        private Map<Channel, VelocitySample> departures = Map.of();
        private Playback previous;
        private RotationBlend rotation = RotationBlend.EULER;
        private final Blend blend;
        private final java.util.function.Predicate<Channel> mask;

        private Playback(AnimationClip clip, double fadeIn, int priority, Blend blend,
            java.util.function.Predicate<Channel> mask) {
            this.blend = blend;
            this.mask = mask;
            this.priority = priority;
            this.clip = clip;
            this.fadeIn = fadeIn;
        }

        public void rotationBlend(RotationBlend mode) {
            rotation = Objects.requireNonNull(mode);
        }

        private Vec sample(Channel channel, double ahead, Vec base) {
            if (!active) return base;
            boolean authored = clip.tracks()
                .containsKey(channel) && mask.test(channel);
            var departure = departures.get(channel);
            if (!authored && previous == null && (departure == null || !mask.test(channel))) return base;
            double progress = fadeIn == 0 ? 1 : Math.min(1, (age + ahead) / fadeIn);
            Vec from = previous != null && progress < 1 ? previous.sample(channel, ahead, base) : base;
            Vec arrival = null;
            if (departure != null && progress < 1) {
                // Project both endpoint tangents, then mix with smoothstep. For vector channels
                // these weights give the cubic Hermite curve through the two poses and velocities.
                double elapsed = age + ahead;
                from = departure.project(channel, rotation, elapsed / ((1 + 2 * progress) * VELOCITY_SAMPLE_SECONDS));
                if (authored) {
                    double join = fadeIn - age;
                    double step = Math.min(VELOCITY_SAMPLE_SECONDS, join);
                    var destination = new VelocitySample(
                        clip.sample(channel, localTime(join)),
                        clip.sample(channel, localTime(join - step)));
                    arrival = destination.project(channel, rotation, (fadeIn - elapsed) / ((3 - 2 * progress) * step));
                }
                progress = progress * progress * (3 - 2 * progress);
            }
            double gain = stopping ? Math.max(0, 1 - (fadeAge + ahead) / fadeLength) : 1;
            if (!authored && from == base) return base;

            double local = localTime(ahead);

            if (endFade > 0 && clip.end() == End.ONCE) {
                double remaining = Math.min(1, (clip.duration() - local) / endFade);
                gain *= remaining * remaining * (3 - 2 * remaining);
            }
            if (!authored) return rotation.mix(channel, base, from, (1 - progress) * gain);
            Vec value = arrival != null ? arrival : clip.sample(channel, local);
            double weight = progress * gain;
            if (blend == Blend.ADDITIVE) {
                if (channel.property() == AnimationClip.Property.SCALE) {
                    Vec factor = channel.property().rest.mix(value, weight);
                    return new Vec(base.x() * factor.x(), base.y() * factor.y(), base.z() * factor.z());
                }
                return new Vec(
                    base.x() + value.x() * weight,
                    base.y() + value.y() * weight,
                    base.z() + value.z() * weight);
            }
            if (from == base) return rotation.mix(channel, base, value, weight);
            return rotation.mix(channel, base, rotation.mix(channel, from, value, progress), gain);
        }

        private double localTime(double ahead) {
            double t = clip.advanceTime(position, ahead * speed);
            if (!Double.isFinite(t)) throw new IllegalArgumentException("playback overflow");
            return clip.end() == End.LOOP ? t % clip.duration() : Math.min(t, clip.duration());
        }

        public boolean active() {
            return active;
        }

        public double time() {
            return position;
        }

        /** Predict the clip-local time after wall seconds without advancing or emitting markers. */
        public double timeAfter(double ahead) {
            AnimationClip.validTime(ahead);
            if (!active) return position;
            return localTime(stopping ? Math.min(ahead, fadeLength - fadeAge) : ahead);
        }

        /** Seek without replaying skipped markers. A looping target wraps, other targets clamp. */
        public void seek(double seconds) {
            AnimationClip.validTime(seconds);
            position = clip.end() == End.LOOP ? seconds % clip.duration() : Math.min(seconds, clip.duration());
            started = true;
        }

        public void speed(double value) {
            AnimationClip.validTime(value);
            speed = value;
        }

        /** Blend a once-playing layer into the underlying pose over its final clip-local seconds. */
        public void fadeOutAtEnd(double clipSeconds) {
            AnimationClip.validTime(clipSeconds);
            endFade = Math.min(clipSeconds, clip.duration());
        }

        public void stop(double fadeOut) {
            AnimationClip.validTime(fadeOut);
            if (fadeOut == 0) {
                active = false;
                departures = Map.of();
                discardPrevious();
                return;
            }
            if (!stopping) {
                stopping = true;
                fadeLength = fadeOut;
            }
        }

        private void discardPrevious() {
            if (previous != null) {
                previous.stop(0);
                previous = null;
            }
        }

        private void advance(double dt, List<FiredMarker> fired, boolean holdEndpoint) {
            if (!active) return;
            if (stopping) dt = Math.min(dt, fadeLength - fadeAge);
            if (previous != null) {
                double remaining = fadeIn - age;
                previous.advance(Math.min(dt, remaining), fired, true);
                if (dt >= remaining || !previous.active) discardPrevious();
            }
            double old = position, length = clip.duration();
            double next = clip.advanceTime(old, dt * speed);
            if (!Double.isFinite(next)) throw new IllegalArgumentException("playback overflow");
            if (clip.end() != End.LOOP) next = Math.min(next, length);
            if (!started) {
                collect(-1, 0, fired);
                started = true;
            }
            if (clip.end() == End.LOOP && next >= length) {
                if (!clip.markers()
                    .isEmpty()) {
                    collect(old, length, fired);
                    long loops = (long) (next / length);
                    // Bound event catch-up after a long suspension. Keep the most recent partial loop.
                    for (long i = 1; i < Math.min(loops, 64); i++) collect(-1, length, fired);
                    collect(-1, next % length, fired);
                }
                next %= length;
            } else collect(old, next, fired);
            position = next;
            age += dt;
            if (age >= fadeIn && !departures.isEmpty()) departures = Map.of();
            if (stopping) fadeAge += dt;
            if ((stopping && fadeAge >= fadeLength) || (!holdEndpoint && clip.end() == End.ONCE && position >= length))
                stop(0);
        }

        private void collect(double after, double through, List<FiredMarker> fired) {
            for (Marker marker : clip.markers()) {
                if (marker.time() > after && marker.time() <= through) fired.add(new FiredMarker(this, marker));
            }
        }
    }

    private record FiredMarker(Playback source, Marker marker) {}
}
