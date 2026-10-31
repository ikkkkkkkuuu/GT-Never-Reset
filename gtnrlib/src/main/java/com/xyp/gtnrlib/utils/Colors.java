package com.xyp.gtnrlib.utils;

public final class Colors {

    private static final double RED_WEIGHT = 0.2126, GREEN_WEIGHT = 0.7152, BLUE_WEIGHT = 0.0722, LINEAR_KNEE = 0.04045,
        LINEAR_SLOPE = 12.92, GAMMA_OFFSET = 0.055, GAMMA = 2.4, CONTRAST_FLARE = 0.05;

    public static float red(int rgb) {
        return (rgb >> 16 & 255) / 255F;
    }

    public static float green(int rgb) {
        return (rgb >> 8 & 255) / 255F;
    }

    public static float blue(int rgb) {
        return (rgb & 255) / 255F;
    }

    public static int mixWhite(int rgb, float k) {
        return lerpRgb(rgb, 0xFFFFFF, k);
    }

    /** Multiplies RGB channels, rounding and capping only the upper bound, as in the original effects API. */
    public static int scaleRgb(int rgb, float k) {
        int r = Math.min(255, Math.round((rgb >> 16 & 255) * k));
        int g = Math.min(255, Math.round((rgb >> 8 & 255) * k));
        int b = Math.min(255, Math.round((rgb & 255) * k));
        return r << 16 | g << 8 | b;
    }

    /** RGB-only interpolation using the original delta formula; blendRgb preserves alpha and weighted rounding. */
    public static int lerpRgb(int from, int to, float t) {
        int r = Math.round((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * t);
        int g = Math.round((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return r << 16 | g << 8 | b;
    }

    public static int compositeRgb(int argb, int rgb) {
        return lerpRgb(rgb, argb & 0xFFFFFF, (argb >>> 24) / 255F);
    }

    public static double[] hsl(int rgb) {
        double r = (rgb >> 16 & 255) / 255D, g = (rgb >> 8 & 255) / 255D, b = (rgb & 255) / 255D;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), lightness = (max + min) / 2;
        if (max == min) return new double[] { 0, 0, lightness };
        double delta = max - min, saturation = delta / (1 - Math.abs(2 * lightness - 1));
        double sector = max == r ? (g - b) / delta % 6 : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4;
        return new double[] { (sector * 60 + 360) % 360, saturation, lightness };
    }

    public static int hsl(double hue, double saturation, double lightness) {
        double h = (hue % 360 + 360) % 360, s = MathUtils.clampUnit(saturation), l = MathUtils.clampUnit(lightness);
        double chroma = (1 - Math.abs(2 * l - 1)) * s, x = chroma * (1 - Math.abs(h / 60 % 2 - 1)), m = l - chroma / 2;
        int sector = (int) (h / 60);
        double r = sector == 0 || sector == 5 ? chroma : sector == 1 || sector == 4 ? x : 0;
        double g = sector == 1 || sector == 2 ? chroma : sector == 0 || sector == 3 ? x : 0;
        double b = sector == 3 || sector == 4 ? chroma : sector == 2 || sector == 5 ? x : 0;
        return channel(r + m) << 16 | channel(g + m) << 8 | channel(b + m);
    }

    public static double luminance(int rgb) {
        return RED_WEIGHT * linear(rgb >> 16 & 255) + GREEN_WEIGHT * linear(rgb >> 8 & 255)
            + BLUE_WEIGHT * linear(rgb & 255);
    }

    public static double contrast(int rgb, int other) {
        double first = luminance(rgb) + CONTRAST_FLARE, second = luminance(other) + CONTRAST_FLARE;
        return Math.max(first, second) / Math.min(first, second);
    }

    private static double linear(int channel) {
        double value = channel / 255D;
        return value <= LINEAR_KNEE ? value / LINEAR_SLOPE
            : Math.pow((value + GAMMA_OFFSET) / (1 + GAMMA_OFFSET), GAMMA);
    }

    private static int channel(double value) {
        return (int) Math.round(MathUtils.clampUnit(value) * 255);
    }

    public static boolean hasAlpha(String value) {
        return digits(value).length() == 8;
    }

    public static int parseArgb(String value, int fallback) {
        String digits = digits(value);
        if (digits.length() != 6 && digits.length() != 8) {
            return fallback;
        }
        try {
            return (int) Long.parseLong(digits, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static int resolve(String override, String inherited, int fallback) {
        return over(override, parseArgb(inherited, fallback));
    }

    public static int over(String override, int base) {
        if (override == null || override.trim()
            .isEmpty()) {
            return base;
        }
        int picked = parseArgb(override, base);
        if (hasAlpha(override)) {
            return picked;
        }
        return (base & 0xFF000000) | (picked & 0x00FFFFFF);
    }

    /** Percentage brightness: below 100 scales RGB; above 100 blends towards white. */
    public static int scale(int rgb, int percent) {
        if (percent == 100) return rgb & 0x00FFFFFF;
        return percent < 100 ? scaleRgb(rgb, Math.max(0, percent) / 100f)
            : mixWhite(rgb, Math.min(100, percent - 100) / 100f);
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((Math.max(0, Math.min(255, alpha))) << 24) | (rgb & 0x00FFFFFF);
    }

    /** Interpolates RGB while preserving the source alpha. */
    public static int blendRgb(int argb, int rgb, float amount) {
        int alpha = argb >>> 24;
        int r = Math.round(((argb >> 16) & 0xFF) * (1 - amount) + ((rgb >> 16) & 0xFF) * amount);
        int g = Math.round(((argb >> 8) & 0xFF) * (1 - amount) + ((rgb >> 8) & 0xFF) * amount);
        int b = Math.round((argb & 0xFF) * (1 - amount) + (rgb & 0xFF) * amount);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /** Scales alpha using the wheel's integer truncation, leaving RGB unchanged. */
    public static int fadeAlpha(int argb, float factor) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * factor);
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** Scales alpha by rounding to the nearest integer; fadeAlpha retains the older truncation rule. */
    public static int scaleAlpha(int argb, float factor) {
        return Math.round((argb >>> 24) * factor) << 24 | argb & 0xFFFFFF;
    }

    public static String toHex8(int argb) {
        return String.format("0x%08X", argb);
    }

    private static String digits(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            return cleaned.substring(2);
        }
        if (cleaned.startsWith("#")) {
            return cleaned.substring(1);
        }
        return cleaned;
    }
}
