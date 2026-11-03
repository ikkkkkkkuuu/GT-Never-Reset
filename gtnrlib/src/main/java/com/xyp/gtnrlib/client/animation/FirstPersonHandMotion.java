package com.xyp.gtnrlib.client.animation;

import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;

/** Camera-space lag for the optional first-person hand profile. One step per client tick. */
final class FirstPersonHandMotion {

    private final Axis side = new Axis(), lift = new Axis(), depth = new Axis();

    private double stride, previousStride, gait, previousGait;
    private float previousYaw = Float.NaN, previousLook;

    void advance(double dx, double dy, double dz, float yaw, float pitch, boolean grounded) {
        // Discontinuous position updates are teleports, not impulses to the wrist.
        if (dx * dx + dy * dy + dz * dz > 4) {
            clear();
            return;
        }
        // Mouse input changes between client ticks. Vanilla can copy the new angle into prevRotationYaw before END.
        double turn = Float.isNaN(previousYaw) ? 0
            : net.minecraft.util.MathHelper.wrapAngleTo180_float(yaw - previousYaw);
        double look = Float.isNaN(previousYaw) ? 0 : pitch - previousLook;
        previousYaw = yaw;
        previousLook = pitch;
        double angle = Math.toRadians(yaw), cos = Math.cos(angle), sin = Math.sin(angle);
        double strafe = dx * cos + dz * sin, forward = dz * cos - dx * sin;
        // Positive local strafe moves left, while increasing view yaw turns right.
        side.step(limit(strafe * .20 - limit(turn, 30) * .0022, .10));
        lift.step(limit(-dy * .065 + limit(look, 20) * .0016, .07));
        depth.step(limit(forward * .22, .09));
        double distance = Math.hypot(dx, dz);
        previousStride = stride;
        previousGait = gait;
        boolean walking = grounded && distance > .04;
        if (walking) stride += distance * 3;
        double target = walking ? Math.min(1.35, distance / .20) : 0;
        gait += limit(target - gait, .15);
    }

    Vec sample(float partial, double age) {
        // Use the player's clock so rendering an extra pass cannot advance the breathing cycle.
        double phase = previousStride + (stride - previousStride) * partial;
        double weight = previousGait + (gait - previousGait) * partial;
        return new Vec(
            side.sample(partial) + .012 * weight * Math.sin(phase),
            lift.sample(partial) + .007 * Math.sin(age * .10) + .009 * weight * Math.cos(2 * phase),
            depth.sample(partial));
    }

    void clear() {
        side.clear();
        lift.clear();
        depth.clear();
        stride = previousStride = gait = previousGait = 0;
        previousYaw = Float.NaN;
    }

    private static double limit(double value, double max) {
        return Math.max(-max, Math.min(max, value));
    }

    private static final class Axis {

        // Analytic critically damped response with time measured in 20 Hz simulation ticks.
        private static final double RATE = .85, DECAY = Math.exp(-RATE);
        private double previous, value, velocity;

        void step(double target) {
            previous = value;
            double error = value - target, slope = velocity + RATE * error;
            value = target + (error + slope) * DECAY;
            velocity = (velocity - RATE * slope) * DECAY;
        }

        double sample(float partial) {
            return previous + (value - previous) * partial;
        }

        void clear() {
            previous = value = velocity = 0;
        }
    }
}
