package com.xyp.gtnrlib.api.animation;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** A target-local state layer. Predicates inspect an owner-supplied snapshot in declared priority order. */
public final class AnimationStates<T> {

    public record State<T> (String clipId, Predicate<T> when, double fade) {

        public State {
            Objects.requireNonNull(clipId);
            Objects.requireNonNull(when);
            AnimationClip.validTime(fade);
        }
    }

    private final List<State<T>> states;
    private final int priority;
    private final AnimationPlayer player;
    private final Function<String, AnimationClip> lookup;
    private State<T> current;
    private AnimationClip clip;
    private AnimationPlayer.Playback handle;

    public AnimationStates(AnimationPlayer player, int priority, List<State<T>> states,
        Function<String, AnimationClip> lookup) {
        this.player = Objects.requireNonNull(player);
        this.priority = priority;
        this.states = List.copyOf(states);
        this.lookup = Objects.requireNonNull(lookup);
    }

    /** Call once per tick before advancing the shared player. A state change crossfades the channel layer. */
    public void update(T context) {
        State<T> next = null;
        AnimationClip asset = null;
        for (State<T> state : states) if (state.when.test(context)) {
            asset = lookup.apply(state.clipId);
            if (asset != null) {
                next = state;
                break;
            }
        }
        if (current == next && asset == clip) return;
        double position = handle != null && current == next && handle.active() ? handle.time() : 0;
        if (handle != null) handle.stop(current.fade);
        clip = asset;
        current = next;
        handle = next == null ? null : player.play(clip, next.fade, priority);
        if (position > 0) handle.seek(position);
    }

    public String state() {
        return current == null ? null : current.clipId;
    }

    public void clear() {
        if (handle != null) handle.stop(0);
        current = null;
        clip = null;
        handle = null;
    }
}
