package com.xyp.gtnrlib;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

final class HmiPlaybackContracts {

    static void verify(Path release) throws Exception {
        var scripts = HmiPoseProgram.Scripts.read(release);
        check(scripts, "walk", p -> p.frame.set("speed", .22));
        check(scripts, "fall", p -> {
            p.frame.set("ground", LuaValue.FALSE);
            p.frame.set("vertical", -.65);
        });
        check(scripts, "look", p -> p.frame.set("pitch", 28));
        check(scripts, "inspect", p -> p.frame.set("inspect", LuaValue.TRUE));
        check(scripts, "ladder", p -> p.frame.set("climb", LuaValue.TRUE));
        check(scripts, "swing", p -> {});
        check(scripts, "equip", p -> {});
        var program = create(scripts);
        program.frame.set("mainItem", "minecraft:water_bucket");
        program.frame.set("pitch", 40);
        for (int i = 0; i < 24; i++) sample(program, i, 0, false);
        if (program.part(0) == null || program.part(0)
            .equals(new Matrix4f(), 1e-4f)) throw new AssertionError("liquid quad did not tilt");
    }

    private static void check(HmiPoseProgram.Scripts source, String action, Consumer<HmiPoseProgram> change)
        throws Exception {
        var baseline = create(source);
        var test = create(source);
        for (int age = 0; age < 40; age++) {
            sample(baseline, age, 0, false);
            sample(test, age, 0, false);
        }
        change.accept(test);
        boolean moved = false;
        for (int age = 40; age < 64; age++) {
            var expected = sample(baseline, age, 0, false);
            var actual = sample(
                test,
                age,
                action.equals("swing") ? (age - 40) / 24f : 0,
                action.equals("equip") && age == 40);
            moved |= !expected.equals(actual, .0001f) || !baseline.armMatrix.equals(test.armMatrix, .0001f);
        }
        if (!moved) throw new AssertionError("HMI input did not affect either hand: " + action);
    }

    private static Matrix4f sample(HmiPoseProgram program, int age, float progress, boolean changed) {
        var f = program.frame;
        f.set("age", age);
        program.pose(
            true,
            f.get("mainItem")
                .tojstring(),
            progress,
            0,
            changed,
            false,
            false,
            1f / 30);
        var right = new Matrix4f(program.armMatrix);
        program.pose(false, HmiPoseProgram.EMPTY, progress, 0, changed, false, false, 1f / 30);
        return right;
    }

    private static HmiPoseProgram create(HmiPoseProgram.Scripts scripts) throws Exception {
        var program = new HmiPoseProgram(scripts);
        var f = program.frame;
        f.set("mainItem", "minecraft:iron_sword");
        f.set("action", "block");
        var tags = new LuaTable();
        tags.set("swords", LuaValue.TRUE);
        f.set("tags", tags);
        for (String key : new String[] { "pitch", "yaw", "speed", "vertical", "age", "swings" }) f.set(key, 0);
        for (String key : new String[] { "water", "sneak", "climb", "inspect", "block", "using", "throwable",
            "translate" }) f.set(key, LuaValue.FALSE);
        f.set("ground", LuaValue.TRUE);
        return program;
    }
}
