package com.xyp.gtneverreset.utils.text.effect;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Serializable visual parameters independent of client rendering classes. */
public record TextEffectStyle(String rendererId, List<Integer> colors, float speed) {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public TextEffectStyle {
        Objects.requireNonNull(rendererId, "rendererId");
        if (rendererId.length() > 128 || !IDENTIFIER.matcher(rendererId)
            .matches()) throw new IllegalArgumentException("Invalid effect identifier");
        colors = List.copyOf(colors);
        if (colors.size() > 8) throw new IllegalArgumentException("At most eight palette colors are supported");
        for (int color : colors) {
            if ((color & 0xFF000000) != 0) throw new IllegalArgumentException("Palette colors must be 24-bit RGB");
        }
        if (!Float.isFinite(speed) || speed < 0)
            throw new IllegalArgumentException("Speed must be finite and nonnegative");
    }

}
