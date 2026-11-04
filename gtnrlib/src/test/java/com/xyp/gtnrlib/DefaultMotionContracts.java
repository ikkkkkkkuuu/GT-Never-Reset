package com.xyp.gtnrlib;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

/** Compare independent players at every sampled frame, including their recovery after input ends. */
public final class DefaultMotionContracts {

    public static void main(String[] args) throws Exception {
        verify(HmiPoseProgram.Scripts.read(Path.of(args[0])));
    }

    static void verify(HmiPoseProgram.Scripts source) throws Exception {
        for (String test : List.of(
            "empty punch",
            "empty walk+punch",
            "empty jumps+punch",
            "jumps+mine",
            "rapid sword combo",
            "sword combo",
            "pickaxe mine",
            "look",
            "walk",
            "two jumps",
            "walk+strike",
            "walk+mine",
            "bow release",
            "short bow",
            "walk+bow",
            "look+strike",
            "jumps+strike",
            "jumps+bow",
            "walk+look+bow",
            "walk+look+strike",
            "sword guard",
            "eat",
            "drink",
            "equip",
            "lower")) {
            compare(source, test, 60);
        }
        for (int rate : new int[] { 30, 45, 75, 90, 120, 144, 240 }) {
            for (String test : List.of("look", "walk", "two jumps", "walk+strike", "walk+bow"))
                compare(source, test, rate);
        }
        var models = source.assets()
            .keySet()
            .stream()
            .filter(p -> p.startsWith("items/") && p.endsWith(".json"))
            .map(p -> p.substring(6, p.length() - 5))
            .sorted()
            .toList();
        if (models.size() != 35) throw new AssertionError("Reference model inventory changed");
        for (String id : models) compare(source, "model " + id + " strike", 60, id);
    }

    private static void compare(HmiPoseProgram.Scripts source, String test, int hz) throws Exception {
        String id = test.startsWith("empty") ? "air"
            : test.contains("mine") ? "iron_pickaxe"
                : test.contains("bow") ? "bow"
                    : test.equals("eat") ? "apple" : test.equals("drink") ? "potion" : "iron_sword";
        compare(source, test, hz, id);
    }

    private static void compare(HmiPoseProgram.Scripts source, String test, int hz, String id) throws Exception {
        var reference = create(source, id);
        var nativePose = create(HmiPoseProgram.Scripts.BUILT_IN, id);
        float maxArm = 0, maxItem = 0, maxLeft = 0, maxTurn = 0, maxHandTurn = 0, sum = 0;
        int samples = 0;
        for (int frame = -hz * 2; frame < hz * 2.5; frame++) {
            float i = frame * 60f / hz;
            float progress = 0;
            int repeat = test.equals("rapid sword combo") ? 18 : 38;
            int strike = test.contains("combo") && i >= repeat ? repeat : 0;
            if ((test.contains("punch") || test.contains("combo") || test.contains("mine") || test.contains("strike"))
                && i >= strike) {
                float time = (i - strike) / (float) (3 * reference.swingDuration("minecraft:" + id));
                if (time < 1) progress = time;
            }
            boolean equip = test.equals("equip") && i == 0;
            float dip = test.equals("lower") ? Math.max(0, 1 - Math.abs(i - 24) / 24) : 0;
            boolean ground = true;
            float vertical = 0;
            if (test.contains("jumps") && i >= 0) {
                int t = (int) i % 54 / 3;
                if (t < 11) {
                    ground = false;
                    vertical = .42f;
                    for (int j = 0; j < t; j++) vertical = (vertical - .08f) * .98f;
                }
            }
            for (var p : List.of(reference, nativePose)) {
                var f = p.frame;
                f.set("age", (int) ((i + 120) / 3));
                boolean walk = i >= 0 && i < 115 && test.contains("walk");
                f.set("speed", Math.hypot(walk ? .22 : 0, vertical));
                f.set("vertical", vertical);
                f.set("ground", LuaValue.valueOf(ground));
                f.set("pitch", test.contains("look") ? Math.max(0, Math.min(i, 18)) * -1.3 : 0);
                f.set("yaw", test.contains("look") ? Math.max(0, Math.min(i, 18)) * 3 : 0);
                f.set(
                    "using",
                    LuaValue.valueOf(
                        (test.contains("bow") || test.equals("sword guard")
                            || test.equals("eat")
                            || test.equals("drink")) && i >= 0 && i < (test.equals("short bow") ? 10 : 93)));
                f.set("swings", i < 0 ? 0 : strike == 0 ? 1 : 2);
                p.pose(
                    true,
                    "minecraft:" + id,
                    progress,
                    dip,
                    equip,
                    i < repeat,
                    test.contains("mine") && progress > 0,
                    1f / hz);
            }
            if (i >= 0) {
                float a = distance(reference.armMatrix, nativePose.armMatrix);
                float b = distance(reference.itemMatrix, nativePose.itemMatrix);
                maxArm = Math.max(maxArm, a);
                maxItem = Math.max(maxItem, b);
                maxHandTurn = Math.max(maxHandTurn, angle(reference.armMatrix, nativePose.armMatrix));
                maxTurn = Math.max(maxTurn, angle(reference.itemMatrix, nativePose.itemMatrix));
                sum += a + b;
                samples++;
            }
            for (var p : List.of(reference, nativePose)) p.pose(
                false,
                HmiPoseProgram.EMPTY,
                progress,
                0,
                equip,
                i < repeat,
                test.contains("mine") && progress > 0,
                1f / hz);
            if (i >= 0) {
                maxLeft = Math.max(maxLeft, distance(reference.armMatrix, nativePose.armMatrix));
                maxHandTurn = Math.max(maxHandTurn, angle(reference.armMatrix, nativePose.armMatrix));
            }
        }
        System.out.printf(
            Locale.ROOT,
            "%s @%dHz | n=%d arm=%.4f item=%.4f off=%.4f rot=%.2f armRot=%.2f mean=%.4f%n",
            test,
            hz,
            samples,
            maxArm,
            maxItem,
            maxLeft,
            maxTurn,
            maxHandTurn,
            sum / (2 * samples));
        float positionLimit = switch (test) {
            case "walk", "look", "two jumps", "equip", "lower" -> .0002f;
            case "walk+bow", "walk+look+bow", "jumps+bow", "bow release", "short bow" -> .0015f;
            case "drink" -> .020f;
            case "model end_crystal strike" -> .018f;
            default -> .006f;
        };
        float rotationLimit = switch (test) {
            case "walk", "look", "two jumps", "equip", "lower", "walk+bow", "bow release", "short bow" -> .07f;
            case "drink" -> 1.5f;
            case "model leaf_litter strike", "model pink_petals strike", "model wildflowers strike" -> 1.15f;
            default -> .6f;
        };
        if (!Float.isFinite(sum) || !(Math.max(maxArm, Math.max(maxItem, maxLeft)) <= positionLimit)
            || !(Math.max(maxTurn, maxHandTurn) <= rotationLimit))
            throw new AssertionError(test + " @" + hz + " Hz exceeds pose tolerance");
    }

    private static float distance(Matrix4f a, Matrix4f b) {
        return a.getTranslation(new Vector3f())
            .distance(b.getTranslation(new Vector3f()));
    }

    private static float angle(Matrix4f a, Matrix4f b) {
        float max = 0;
        for (int i = 0; i < 3; i++) {
            var v = new Vector3f();
            v.setComponent(i, 1);
            float radians = a.transformDirection(new Vector3f(v))
                .angle(b.transformDirection(v));
            max = Math.max(max, (float) Math.toDegrees(radians));
        }
        return max;
    }

    private static HmiPoseProgram create(HmiPoseProgram.Scripts scripts, String name) throws Exception {
        var p = new HmiPoseProgram(scripts);
        var f = p.frame;
        f.set("mainItem", "minecraft:" + name);
        f.set(
            "action",
            name.equals("bow") ? "bow"
                : name.endsWith("sword") ? "block"
                    : name.equals("apple") ? "eat" : name.equals("potion") ? "drink" : "none");
        var tags = new LuaTable();
        boolean boat = name.endsWith("_boat") || name.endsWith("_raft");
        boolean leaves = List.of("wildflowers", "pink_petals", "leaf_litter")
            .contains(name);
        if (boat) tags.set("boats", LuaValue.TRUE);
        if (name.contains("_chest_")) tags.set("chest_boats", LuaValue.TRUE);
        if (name.endsWith("sword")) tags.set("swords", LuaValue.TRUE);
        if (name.endsWith("pickaxe")) tags.set("pickaxes", LuaValue.TRUE);
        f.set("tags", tags);
        for (String key : List.of("climb", "water", "sneak", "inspect", "block", "lantern", "throwable", "translate"))
            f.set(key, LuaValue.FALSE);
        f.set("block", LuaValue.valueOf(leaves));
        if (name.equals("milk_bucket")) f.set("action", "drink");
        f.set(
            "translate",
            LuaValue.valueOf(
                name.equals("apple") || name
                    .equals("potion") || leaves || boat || name.endsWith("bucket") || name.equals("end_crystal")));
        return p;
    }
}
