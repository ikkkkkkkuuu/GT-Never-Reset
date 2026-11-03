package com.xyp.gtnrlib.api.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.End;
import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;

/** One target on one owner thread. Later layers override only channels authored by that clip. */
public final class AnimationPlayer {

    private final List<Playback> layers = new ArrayList<>();
    private final Consumer<Marker> listener;

    public AnimationPlayer(Consumer<Marker> listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    public Playback play(AnimationClip clip, double fadeIn) {
        return play(clip, fadeIn, 0);
    }

    /** Higher priorities are applied last, independent of when another state starts. */
    public Playback play(AnimationClip clip, double fadeIn, int priority) {
        AnimationClip.validTime(fadeIn);
        Playback handle = new Playback(Objects.requireNonNull(clip), fadeIn, priority);
        int index = layers.size();
        while (index > 0 && layers.get(index - 1).priority > priority) index--;
        layers.add(index, handle);
        return handle;
    }

    public void clear() {
        for (Playback layer : layers) layer.active = false;
        layers.clear();
    }

    /** Advance simulation once, then deliver events. Listener callbacks may start and stop clips. */
    public void advance(double seconds) {
        AnimationClip.validTime(seconds);
        List<Marker> fired = new ArrayList<>();
        for (Playback layer : layers) layer.advance(seconds, fired);
        layers.removeIf(layer -> !layer.active);
        fired.forEach(listener);
    }

    /** Read-only prediction relative to the last advance, for sub-tick rendering and previews. */
    public Vec sample(Channel channel, double ahead) {
        AnimationClip.validTime(ahead);
        Vec result = channel.property().rest;
        for (Playback layer : layers) {
            if (!layer.active || !layer.clip.tracks()
                .containsKey(channel)) continue;
            double t = layer.position + ahead * layer.speed;
            double local = layer.clip.end() == End.LOOP ? t % layer.clip.duration()
                : Math.min(t, layer.clip.duration());
            result = result.mix(layer.clip.sample(channel, local), layer.weight(ahead));
        }
        return result;
    }

    public static final class Playback {

        private final AnimationClip clip;
        private final double fadeIn;
        private final int priority;
        private double position, age, fadeAge, fadeLength;
        private double speed = 1;
        private boolean started, stopping;
        private boolean active = true;

        private Playback(AnimationClip clip, double fadeIn, int priority) {
            this.priority = priority;
            this.clip = clip;
            this.fadeIn = fadeIn;
        }

        public boolean active() {
            return active;
        }

        public double time() {
            return position;
        }

        public void speed(double value) {
            AnimationClip.validTime(value);
            speed = value;
        }

        public void stop(double fadeOut) {
            AnimationClip.validTime(fadeOut);
            if (fadeOut == 0) {
                active = false;
                return;
            }
            if (!stopping) {
                stopping = true;
                fadeLength = fadeOut;
            }
        }

        private double weight(double ahead) {
            double gain = fadeIn == 0 ? 1 : Math.min(1, (age + ahead) / fadeIn);
            return stopping ? gain * Math.max(0, 1 - (fadeAge + ahead) / fadeLength) : gain;
        }

        private void advance(double dt, List<Marker> fired) {
            if (!active) return;
            if (stopping) dt = Math.min(dt, fadeLength - fadeAge);
            double old = position, length = clip.duration();
            double next = old + dt * speed;
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
            if (stopping) fadeAge += dt;
            if ((stopping && fadeAge >= fadeLength) || (clip.end() == End.ONCE && position >= length)) active = false;
        }

        private void collect(double after, double through, List<Marker> fired) {
            for (Marker m : clip.markers()) if (m.time() > after && m.time() <= through) fired.add(m);
        }
    }
}
