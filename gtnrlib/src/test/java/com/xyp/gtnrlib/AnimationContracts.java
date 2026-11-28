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
        verifyCrossFade();
        verifyMarkerSources();
        verifyVelocityTransition();
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

    private static void verifyCrossFade() {
        Channel wrist = new Channel("wrist", Property.ROTATION);
        Channel left = new Channel("left", Property.POSITION);
        Channel right = new Channel("right", Property.POSITION);
        List<Key> fixed = List.of(new Key(0, new Vec(40, 0, 0)));
        List<Key> moving = List.of(new Key(0, new Vec(0, 0, 0)), new Key(1, new Vec(20, 0, 0)));
        var walk = new AnimationClip(1, End.HOLD, Map.of(left, moving, right, moving), List.of());
        var before = new AnimationClip(
            .25,
            End.ONCE,
            Map.of(wrist, fixed, left, fixed),
            List.of(new Marker(.25, "end")));
        var after = new AnimationClip(1, End.HOLD, Map.of(wrist, fixed, right, fixed), List.of());
        var events = new ArrayList<String>();
        var player = new AnimationPlayer(m -> events.add(m.name()));
        player.play(walk, 0);
        var old = player.play(before, 0, 1);
        var next = player.crossFade(old, after, 1, AnimationPlayer.RotationBlend.EULER);
        for (int i = 0; i <= 10; i++) near(
            player.sample(wrist, i / 10.0)
                .x(),
            40);
        near(
            player.sample(left, .5)
                .x(),
            25);
        near(
            player.sample(right, .5)
                .x(),
            25);
        check(events.isEmpty());
        player.advance(.5);
        near(
            player.sample(wrist, 0)
                .x(),
            40);
        near(
            player.sample(left, 0)
                .x(),
            25);
        check(events.equals(List.of("end")));
        var interrupted = player.crossFade(next, before, .5, AnimationPlayer.RotationBlend.EULER);
        near(
            player.sample(right, 0)
                .x(),
            25);
        var last = player.crossFade(interrupted, after, .5, AnimationPlayer.RotationBlend.EULER);
        near(
            player.sample(right, 0)
                .x(),
            25);
        player.advance(.5);
        near(
            player.sample(left, 0)
                .x(),
            20);
        near(
            player.sample(right, 0)
                .x(),
            40);
        check(!old.active() && !next.active() && !interrupted.active() && last.active());
        last.stop(.25);
        near(
            player.sample(right, .125)
                .x(),
            30);
        player.advance(.25);
        near(
            player.sample(right, 0)
                .x(),
            20);
        check(events.equals(List.of("end", "end")));

        // A short replacement can finish while its incoming crossfade is still in progress.
        var shortOverlay = player.crossFade(player.play(after, 0, 1), before, 1, AnimationPlayer.RotationBlend.EULER);
        shortOverlay.fadeOutAtEnd(.125);
        near(
            player.sample(wrist, .25)
                .x(),
            0);
        near(
            player.sample(right, .25)
                .x(),
            20);
        player.advance(.25);
        near(
            player.sample(right, 0)
                .x(),
            20);

        for (var mode : AnimationPlayer.RotationBlend.values()) {
            player.clear();
            var a = new AnimationClip(1, End.HOLD, Map.of(wrist, List.of(new Key(0, new Vec(0, 0, 179)))), List.of());
            var b = new AnimationClip(1, End.HOLD, Map.of(wrist, List.of(new Key(0, new Vec(0, 0, -179)))), List.of());
            player.crossFade(player.play(a, 0), b, 1, mode);
            if (mode != AnimationPlayer.RotationBlend.EULER) {
                nearTurn(
                    player.sample(wrist, .25)
                        .z(),
                    179.5);
                nearTurn(
                    player.sample(wrist, .75)
                        .z(),
                    -179.5);
            }
            near(
                Math.abs(
                    player.sample(wrist, .5)
                        .z()),
                mode == AnimationPlayer.RotationBlend.EULER ? 0 : 180);
        }
        var spin = new AnimationClip(
            1,
            End.HOLD,
            Map.of(wrist, List.of(new Key(0, new Vec(0, 0, 0)), new Key(1, new Vec(0, 0, 360)))),
            List.of());
        player.clear();
        player.play(spin, 0)
            .rotationBlend(AnimationPlayer.RotationBlend.SLERP_XYZ);
        near(
            player.sample(wrist, .75)
                .z(),
            270);
    }

    private static void verifyMarkerSources() {
        var oldMarker = new Marker(.125, "impact");
        var newMarker = new Marker(.25, "impact");
        var before = new AnimationClip(.5, End.ONCE, Map.of(), List.of(oldMarker));
        var after = new AnimationClip(.25, End.ONCE, Map.of(), List.of(newMarker));
        var sources = new ArrayList<AnimationPlayer.Playback>();
        var markers = new ArrayList<Marker>();
        var player = AnimationPlayer.withSources((source, marker) -> {
            sources.add(source);
            markers.add(marker);
            check(!source.active());
        });
        var old = player.play(before, 0);
        var next = player.crossFade(old, after, .125, AnimationPlayer.RotationBlend.EULER);
        player.advance(.25);
        player.advance(.25);
        check(sources.equals(List.of(old, next)) && markers.equals(List.of(oldMarker, newMarker)));
        near(next.timeAfter(1), next.time());

        var fading = player.play(new AnimationClip(2, End.HOLD, Map.of(), List.of()), 0);
        fading.speed(2);
        player.advance(.1);
        near(fading.timeAfter(.1), .4);
        near(fading.time(), .2);
        fading.stop(.1);
        double limit = fading.timeAfter(1);
        player.advance(1);
        near(limit, .4);
        near(fading.timeAfter(1), limit);
    }

    private static void verifyVelocityTransition() {
        Channel x = new Channel("wrist", Property.POSITION);
        Channel left = new Channel("left", Property.POSITION);
        Channel masked = new Channel("masked", Property.POSITION);
        List<Key> rising = List.of(new Key(0, new Vec(0, 0, 0)), new Key(1, new Vec(10, 0, 0)));
        List<Key> falling = List.of(new Key(0, new Vec(8, 0, 0)), new Key(1, new Vec(-2, 0, 0)));
        var walk = new AnimationClip(1, End.HOLD, Map.of(left, rising, masked, rising), List.of());
        var old = new AnimationClip(1, End.HOLD, Map.of(x, rising, left, falling), List.of(new Marker(.4, "old")));
        var next = new AnimationClip(1, End.HOLD, Map.of(x, falling, masked, falling), List.of(new Marker(0, "new")));
        var events = new ArrayList<String>();
        var p = new AnimationPlayer(m -> events.add(m.name()));
        p.play(walk, 0);
        var handle = p.play(old, 0, 1, AnimationPlayer.Blend.OVERRIDE, c -> !c.equals(masked));
        p.play(walk, 0, 2, AnimationPlayer.Blend.ADDITIVE, c -> c.equals(left));
        handle.speed(2);
        p.advance(.1);
        double h = 1e-6;
        var mode = AnimationPlayer.Transition.MATCH_VELOCITY;
        var rotation = AnimationPlayer.RotationBlend.EULER;
        var action = p.transition(handle, next, .25, rotation, mode);
        check(!handle.active() && events.isEmpty());
        near(
            p.sample(x, 0)
                .x(),
            2);
        near(
            p.sample(left, 0)
                .x(),
            7);
        check(
            Math.abs(
                (p.sample(x, h)
                    .x() - 2) / h - 20)
                < .01);
        check(
            Math.abs(
                (p.sample(left, h)
                    .x() - 7) / h + 10)
                < .01);
        near(
            p.sample(masked, .1)
                .x(),
            2);
        action.speed(.5);
        near(
            p.sample(x, .25)
                .x(),
            6.75);
        check(
            Math.abs(
                (6.75 - p.sample(x, .25 - h)
                    .x()) / h + 5)
                < .01);
        near(
            p.sample(left, .25)
                .x(),
            7);
        p.advance(.1);
        double mid = p.sample(x, 0)
            .x();
        double tangent = (p.sample(x, .001)
            .x() - mid) / .001;
        var again = p.transition(action, old, .1, rotation, mode);
        near(
            p.sample(x, 0)
                .x(),
            mid);
        check(
            Math.abs(
                (p.sample(x, h)
                    .x() - mid) / h - tangent)
                < .01);
        p.advance(.1);
        near(
            p.sample(x, 0)
                .x(),
            1);
        check(events.equals(List.of("new")));
        p.transition(again, next, 0, rotation, mode);
        near(
            p.sample(x, 0)
                .x(),
            8);
        p.clear();

        var states = new com.xyp.gtnrlib.api.animation.AnimationStates<Boolean>(
            p,
            1,
            List.of(
                new com.xyp.gtnrlib.api.animation.AnimationStates.State<>("second", Boolean::booleanValue, .25, mode),
                new com.xyp.gtnrlib.api.animation.AnimationStates.State<>("first", active -> true, 0)),
            id -> id.equals("first") ? old : next);
        states.update(false);
        p.advance(.1);
        states.update(true);
        near(
            p.sample(x, 0)
                .x(),
            1);
        check(
            Math.abs(
                (p.sample(x, h)
                    .x() - 1) / h - 10)
                < .01);
        states.clear();

        Channel wrist = new Channel("wrist", Property.ROTATION);
        AnimationClip.Value wrapping = t -> new Vec(0, 0, Math.IEEEremainder(179.999 + 10 * t, 360));
        var spin = new AnimationClip(
            1,
            End.HOLD,
            Map.of(wrist, List.of(new Key(0, wrapping, wrapping, Curve.LINEAR))),
            List.of());
        var target = new AnimationClip(1, End.HOLD, Map.of(wrist, List.of(new Key(0, new Vec(0, 0, -170)))), List.of());
        for (var order : List.of(AnimationPlayer.RotationBlend.SLERP_XYZ, AnimationPlayer.RotationBlend.SLERP_ZYX)) {
            var turn = p.transition(p.play(spin, 0), target, .1, order, mode);
            check(
                Math.abs(
                    Math.IEEEremainder(
                        p.sample(wrist, h)
                            .z() - 179.999,
                        360) / h - 10)
                    < .01);
            near(
                p.sample(wrist, .1)
                    .z(),
                -170);
            turn.stop(0);
            p.clear();
        }
    }

    private static void nearTurn(double actual, double expected) {
        // SLERP may take its numerically stable linear approximation for very close orientations.
        if (!(Math.abs(actual - expected) < 1e-4)) throw new AssertionError("unexpected rotation " + actual);
    }

    private static void near(double actual, double expected) {
        if (!(Math.abs(actual - expected) < 1e-6)) throw new AssertionError("expected " + expected + ", got " + actual);
    }

    private static void check(boolean passed) {
        if (!passed) throw new AssertionError("animation contract");
    }
}
