package com.xyp.gtnrlib;

import com.xyp.gtnrlib.client.animation.ToolboxMotion;

final class ToolboxMotionContracts {

    static void verify() {
        for (int fps : new int[] { 30, 60, 144 }) {
            var motion = new ToolboxMotion();
            motion.sample(0, true);
            for (int frame = 1; frame <= fps * 3; frame++) {
                motion.sample(frame / (double) fps, true);
                check(motion.lid() == 0 || motion.support() == 1, "support the box before lifting the lid");
                check(motion.reach() == 0 || motion.lid() == 1, "open the lid before reaching into the box");
                float x = motion.searchX(), y = motion.searchY();
                motion.sample(frame / (double) fps, true);
                check(x == motion.searchX() && y == motion.searchY(), "repeated rendering must not advance time");
            }
            var reference = new ToolboxMotion();
            reference.sample(0, true);
            for (int frame = 1; frame <= 360; frame++) reference.sample(frame / 120d, true);
            check(Math.abs(reference.searchX() - motion.searchX()) < .0001, "searching must not depend on frame rate");
            float previousReach = motion.reach();
            for (int frame = 1; frame <= fps * 2; frame++) {
                motion.sample(3 + frame / (double) fps, false);
                check(motion.reach() <= previousReach, "closing withdraws the hand continuously");
                check(motion.lid() == 1 || motion.reach() == 0, "withdraw before shutting the lid");
                previousReach = motion.reach();
            }
            check(motion.support() == 0 && motion.lid() == 0, "return to a closed carrying pose");
            motion.reset();
            motion.sample(100, true);
            check(motion.support() == 0, "world or slot changes do not catch up stale time");
            for (int frame = 1; frame <= fps / 2; frame++) motion.sample(100 + frame / (double) fps, true);
            float angle = motion.lid();
            motion.sample(100.5, false);
            check(Math.abs(angle - motion.lid()) < .03, "quick close starts at the current lid angle");
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
