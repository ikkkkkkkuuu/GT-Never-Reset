package com.xyp.gtnrlib;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Random;

import com.xyp.gtnrlib.client.fx.FxEasing;
import com.xyp.gtnrlib.client.hud.HudElement;
import com.xyp.gtnrlib.client.hud.HudOption;
import com.xyp.gtnrlib.gui.wheel.RadialGeometry;
import com.xyp.gtnrlib.gui.wheel.RevealTiming;
import com.xyp.gtnrlib.utils.ColorMath;
import com.xyp.gtnrlib.utils.Colors;
import com.xyp.gtnrlib.world.Facing;

/** Output fingerprints captured before consolidating the local and upstream implementations. */
public final class SharedImplementationContracts {

    public static void verify() throws Exception {
        Fingerprint local = new Fingerprint(), upstream = new Fingerprint(), angles = new Fingerprint();
        Random random = new Random(663031);
        int[] percentages = { Integer.MIN_VALUE, -1, 0, 1, 49, 50, 99, 100, 101, 150, 199, 200, 201,
            Integer.MAX_VALUE };
        float[] factors = { Float.NEGATIVE_INFINITY, -2F, -1F, -Float.MIN_VALUE, -0F, 0F, Float.MIN_VALUE, 0.1F, 0.3F,
            0.5F, 0.7F, 0.99F, 1F, 1.01F, 2F, Float.POSITIVE_INFINITY, Float.NaN };
        for (int i = 0; i < 10000; i++) {
            int from = random.nextInt(), to = random.nextInt();
            for (int percent : percentages) local.add(Colors.scale(from, percent));
            for (float factor : factors) {
                local.add(Colors.blendRgb(from, to, factor));
                local.add(Colors.fadeAlpha(from, factor));
                upstream.add(ColorMath.mixWhite(from, factor));
                upstream.add(ColorMath.scale(from, factor));
                upstream.add(ColorMath.lerp(from, to, factor));
            }
            upstream.add(ColorMath.over(from, to));
            upstream.add(Float.floatToIntBits(ColorMath.red(from)));
            upstream.add(Float.floatToIntBits(ColorMath.green(from)));
            upstream.add(Float.floatToIntBits(ColorMath.blue(from)));
            double[] hsl = ColorMath.hsl(from);
            upstream.add(ColorMath.hsl(hsl[0], hsl[1], hsl[2]));
            local.add(Colors.withAlpha(from, random.nextInt()));
        }
        for (double value : new double[] { Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -720, -360, -1,
            -Double.MIN_VALUE, -0D, 0D, Double.MIN_VALUE, 1, 359.9, 360, 720, Double.MAX_VALUE,
            Double.POSITIVE_INFINITY, Double.NaN }) {
            angles.add(Double.doubleToLongBits(RadialGeometry.normalizeDegrees(value)));
            angles.add(Double.doubleToLongBits(Facing.normalizeYaw(value)));
            angles.add(Double.doubleToLongBits(RevealTiming.easeOutQuint(value)));
        }
        for (float value : factors) angles.add(Float.floatToIntBits(FxEasing.smoothStep(value)));
        check("local colors", "f6c3d3ecf31b048f06b4feaafc4c0f0e796cb2d11974dfd7a7e5223651b53e72", local.finish());
        check("upstream colors", "ade70a0c70f39bdca1be13cf667ed65eb9f8c8a6a9f074ad6115669d623cef20", upstream.finish());
        check("angles and easing", "82bac50fc66d44f6d74dde13a3af4b1f164930e95bc5887b396d5ef7c734b073", angles.finish());
        roundingContracts();
        hudSnapshots();
        System.out.println("Shared implementation behavior fingerprints passed");
    }

    private static void check(String label, String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(label + " behavior changed: " + actual);
    }

    private static void roundingContracts() {
        if (Colors.blendRgb(0x80010101, 0x060606, 0.9F) != 0x80050505
            || Colors.lerpRgb(0x010101, 0x060606, 0.9F) != 0x060606)
            throw new AssertionError("Weighted and delta interpolation must retain distinct rounding");
        if (Colors.fadeAlpha(0x81010203, 0.5F) != 0x40010203 || Colors.scaleAlpha(0x81010203, 0.5F) != 0x41010203)
            throw new AssertionError("Truncated and rounded alpha must remain distinct");
        if (Colors.scale(0x4080C0, 150) != 0xA0C0E0 || Colors.scaleRgb(0x4080C0, 1.5F) != 0x60C0FF)
            throw new AssertionError("Percentage brightness is not a multiplicative factor");
    }

    private static void hudSnapshots() {
        var original = new HudElement.Choice("first", "first");
        var choices = new HudElement.Choice[] { original };
        var option = HudOption.choice("choice", "choice", "first", choices);
        choices[0] = new HudElement.Choice("changed", "changed");
        if (option.choices()
            .get(0) != original) throw new AssertionError("HUD choices alias the source array");
        var builder = HudElement.builder("gtnrlib:snapshot_test")
            .bounds(0, 0, 20, 10)
            .style(original)
            .option(option);
        var element = builder.build();
        builder.style("second", "second")
            .option(HudOption.toggle("toggle", "toggle", true));
        if (element.styles()
            .size() != 1
            || element.options()
                .size() != 1)
            throw new AssertionError("HUD snapshots changed after builder reuse");
        for (Runnable mutation : new Runnable[] { () -> option.choices()
            .clear(),
            () -> element.styles()
                .clear(),
            () -> element.options()
                .clear() }) {
            try {
                mutation.run();
                throw new AssertionError("HUD snapshots must be unmodifiable");
            } catch (UnsupportedOperationException expected) {}
        }
    }

    private static final class Fingerprint {

        private final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

        private Fingerprint() throws Exception {}

        private void add(long value) {
            buffer.clear();
            buffer.putLong(value);
            digest.update(buffer.array());
        }

        private String finish() {
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) result.append(String.format("%02x", value & 255));
            return result.toString();
        }
    }
}
