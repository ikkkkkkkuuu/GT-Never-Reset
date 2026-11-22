package com.xyp.gtnrlib.gui.theme.ae2;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/** @deprecated Use {@link AE2ScrollbarPainter} and {@link AE2Look} for new screens. */
@Deprecated
public class AE2Scrollbars {

    private AE2Scrollbars() {}

    public static int scrollbarWidth(@Nullable Variant variant) {
        return AE2Look.of(variant)
            .scrollbarWidth();
    }

    public static void drawScrollbar(@Nullable Variant variant, int x, int y, int height, int handleY,
        boolean enabled) {
        AE2Look.of(variant)
            .drawScrollbar(x, y, height, handleY, enabled);
    }

    public static void drawSmallScrollbar(Variant variant, int x, int y, int height, int handleY, boolean enabled) {
        AE2Look.of(variant)
            .drawSmallScrollbar(x, y, height, handleY, enabled);
    }
}
