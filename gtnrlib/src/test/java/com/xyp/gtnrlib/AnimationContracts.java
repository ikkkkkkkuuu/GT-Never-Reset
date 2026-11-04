package com.xyp.gtnrlib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Curve;
import com.xyp.gtnrlib.api.animation.AnimationClip.End;
import com.xyp.gtnrlib.api.animation.AnimationClip.Key;
import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;
import com.xyp.gtnrlib.api.animation.AnimationLibrary;
import com.xyp.gtnrlib.api.animation.AnimationPlayer;
import com.xyp.gtnrlib.api.animation.AnimationRig;

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
        var recover = new AnimationClip(1, End.ONCE, Map.of(arm, List.of(new Key(0, new Vec(120, 0, 0)))), List.of());
        var overlay = other.play(recover, 0, 1);
        overlay.fadeOutAtEnd(.25);
        // The walking layer keeps moving during the action's last quarter, including across the final tick.
        check(
            other.sample(arm, .875)
                .x() == (315 + 120) / 2.0);
        other.advance(.95);
        double beforeEnd = other.sample(arm, .05)
            .x();
        other.advance(.05);
        check(
            !overlay.active() && Math.abs(
                beforeEnd - other.sample(arm, 0)
                    .x())
                < 1e-7);
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
        for (Curve curve : List.of(Curve.SHORTEST_ANGLE, Curve.SLERP, Curve.SQUAD)) {
            Vec a = new Vec(0, 0, 170), b = new Vec(0, 0, -170);
            var rotate = new AnimationClip(
                1,
                End.ONCE,
                Map.of(arm, List.of(new Key(0, a, a, curve), new Key(1, b, b, curve))),
                List.of());
            check(
                Math.abs(
                    Math.abs(
                        rotate.sample(arm, .5)
                            .z())
                        - 180)
                    < 1e-6);
        }
        Vec zero = new Vec(0, 0, 0), one = new Vec(1, 1, 1);
        var rig = new AnimationRig(
            List.of(
                new AnimationRig.Bone("root", null, new Vec(1, 0, 0), new Vec(0, 0, 90), one),
                new AnimationRig.Bone("tip", "root", new Vec(2, 0, 0), zero, one)));
        var joint = new Channel("tip", Property.POSITION);
        var translate = new AnimationClip(1, End.HOLD, Map.of(joint, List.of(new Key(0, new Vec(1, 0, 0)))), List.of());
        player.clear();
        player.play(translate, 0);
        player.play(translate, 0, 1, AnimationPlayer.Blend.ADDITIVE, c -> c.property() == Property.POSITION);
        double[] pose = new double[32];
        rig.sample(player, 0, pose);
        check(Math.abs(pose[28] - 1) < 1e-7 && Math.abs(pose[29] - 4) < 1e-7);
        player.clear();
        var tempo = new AnimationClip(1, End.LOOP, Map.of(), List.of(new Marker(.75, "fast")), Map.of(.5, 2.0));
        var timed = player.play(tempo, 0);
        player.advance(.625);
        check(
            Math.abs(timed.time() - .75) < 1e-8 && events.get(events.size() - 1)
                .equals("fast"));
        player.advance(.25);
        check(Math.abs(timed.time() - .125) < 1e-8);
        AnimationLibrary library = new AnimationLibrary();
        library.registerProvider("test:base", out -> out.register("test:spin", clip));
        check(library.reload(events::add));
        var states = new com.xyp.gtnrlib.api.animation.AnimationStates<Boolean>(
            player,
            4,
            List.of(new com.xyp.gtnrlib.api.animation.AnimationStates.State<>("test:spin", Boolean::booleanValue, .1)),
            library::find);
        states.update(true);
        check("test:spin".equals(states.state()));
        states.update(false);
        check(states.state() == null);
        library.registerProvider(
            "test:native",
            out -> com.xyp.gtnrlib.animation.resource.NativeAnimationResource.load(
                new java.io.StringReader(
                    "{\"version\":1,\"clips\":{\"test:door\":{\"duration\":1,\"tracks\":[{\"bone\":\"arm\",\"property\":\"rotation\",\"keys\":[{\"time\":0,\"value\":[0,0,42]}]}]}}}"),
                out));
        check(
            library.reload(events::add) && library.find("test:door")
                .sample(arm, 0)
                .z() == 42);
        var previous = library.snapshot();
        library.registerProvider("test:bad", out -> { throw new IllegalStateException("broken pack"); });
        check(!library.reload(events::add) && library.snapshot() == previous && library.find("test:spin") == clip);
    }

    private static void check(boolean passed) {
        if (!passed) throw new AssertionError("animation contract");
    }
}
