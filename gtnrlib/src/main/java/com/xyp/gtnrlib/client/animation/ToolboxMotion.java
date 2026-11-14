package com.xyp.gtnrlib.client.animation;

/** Reversible support, lid and searching phases. Time is in seconds and advances once per sampled frame. */
public final class ToolboxMotion {

    private static final float OPEN_SECONDS = 1.25f;
    private float phase, searching;
    private double previous = Double.NaN;

    public void reset() {
        phase = searching = 0;
        previous = Double.NaN;
    }

    public void sample(double seconds, boolean open) {
        if (!Double.isNaN(previous) && seconds <= previous) return;
        float elapsed = Double.isNaN(previous) ? 0 : (float) Math.max(0, Math.min(.1, seconds - previous));
        previous = seconds;
        float before = phase;
        phase = Math.max(0, Math.min(1, phase + (open ? elapsed : -elapsed) / OPEN_SECONDS));
        if (open) searching += Math.max(0, elapsed - Math.max(0, .85f - before) * OPEN_SECONDS);
        if (phase == 0) searching = 0;
    }

    public float support() {
        return smooth(phase / .25f);
    }

    public float lid() {
        return smooth((phase - .25f) / .35f);
    }

    public float reach() {
        return smooth((phase - .60f) / .25f);
    }

    public float searchX() {
        return (float) Math.sin(searching * 3.4) * .16f * smooth((phase - .85f) / .15f);
    }

    public float searchY() {
        return (float) (1 - Math.cos(searching * 6.8)) * .035f * smooth((phase - .85f) / .15f);
    }

    private static float smooth(float value) {
        value = Math.max(0, Math.min(1, value));
        return value * value * (3 - 2 * value);
    }
}
