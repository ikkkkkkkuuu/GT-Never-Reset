package com.xyp.gtnrlib.client.text;

import com.xyp.gtnrlib.text.ShakeMode;
import com.xyp.gtnrlib.text.ShakeMotion;
import com.xyp.gtnrlib.text.TextEffectStyle.Shake;

/**
 * Where trembling text sits at one moment. Every glyph (or the whole span) heads for a new random target
 * {@link #STEPS_PER_SECOND} times a second: a smooth shake glides there and eases in and out, a jumping shake holds a
 * whole-pixel position for each step.
 */
public class TextShake {

    public static final double STEPS_PER_SECOND = 12;

    private TextShake() {}

    /** Offset in pixels of glyph {@code index} along {@code axis} (0 across, 1 down), within {@code amplitude}. */
    public static float offset(int index, int axis, double seconds, float amplitude, boolean jump) {
        double time = seconds * STEPS_PER_SECOND;
        long step = (long) Math.floor(time);
        if (jump) return Math.round(target(index, axis, step) * amplitude);
        double blend = time - step;
        double eased = blend * blend * (3 - 2 * blend);
        double from = target(index, axis, step);
        return (float) ((from + (target(index, axis, step + 1) - from) * eased) * amplitude);
    }

    /**
     * Offset of glyph {@code glyph} in a span shaking as {@code shake}; glyphs shaking together all move as glyph 0.
     */
    public static float offset(Shake shake, int glyph, int axis, double seconds) {
        if (shake.pixels() == 0) return 0;
        boolean together = shake.mode() == ShakeMode.SYNC;
        int amplitude = axis == 0 && !together ? sideways(shake.pixels()) : shake.pixels();
        return offset(together ? 0 : glyph, axis, seconds, amplitude, shake.motion() == ShakeMotion.JUMP);
    }

    // Glyphs moving on their own go half as far sideways, since narrow punctuation would otherwise hide in a neighbor.
    public static int sideways(int pixels) {
        return pixels / 2;
    }

    public static float toGrid(float value, int resolution) {
        return Math.round(value * resolution) / (float) resolution;
    }

    private static double target(int index, int axis, long step) {
        double random = Math.sin(index * 127.1 + axis * 311.7 + step * 74.7) * 43758.5453;
        return (random - Math.floor(random)) * 2 - 1;
    }
}
