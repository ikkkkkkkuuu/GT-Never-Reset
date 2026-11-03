package com.xyp.gtnrlib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.xyp.gtnrlib.api.animation.*;
import com.xyp.gtnrlib.api.animation.AnimationClip.*;

final class AnimationContracts {

    static void verify() {
        Channel arm = new Channel("arm", Property.ROTATION);
        AnimationClip clip = new AnimationClip(
            1,
            End.LOOP,
            Map.of(arm, List.of(new Key(1, new Vec(360, 0, 0)), new Key(0, new Vec(0, 0, 0)))),
            List.of(new Marker(0, "start"), new Marker(.5, "hit")));
        List<String> events = new ArrayList<>();
        AnimationPlayer player = new AnimationPlayer(m -> events.add(m.name()));
        AnimationPlayer other = new AnimationPlayer(m -> {});
        var handle = player.play(clip, 0);
        other.play(clip, 0);
        player.advance(1.25);
        check(events.equals(List.of("start", "hit", "start")));
        check(
            player.sample(arm, .25)
                .x() == 180
                && player.sample(arm, .25)
                    .x() == 180
                && events.size() == 3);
        check(
            other.sample(arm, 0)
                .x() == 0);
        handle.speed(0);
        handle.stop(.5);
        player.advance(.25);
        check(
            player.sample(arm, 0)
                .x() == 45);
        player.advance(.25);
        check(
            !handle.active() && player.sample(arm, 0)
                .x() == 0);
        AnimationLibrary library = new AnimationLibrary();
        library.registerProvider("test:base", out -> out.register("test:spin", clip));
        check(library.reload(events::add));
        var previous = library.snapshot();
        library.registerProvider("test:bad", out -> { throw new IllegalStateException("broken pack"); });
        check(!library.reload(events::add) && library.snapshot() == previous && library.find("test:spin") == clip);
    }

    private static void check(boolean passed) {
        if (!passed) throw new AssertionError("animation contract");
    }
}
