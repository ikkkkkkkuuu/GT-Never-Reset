package com.xyp.gtnrlib.gui.theme.ae2;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/** @deprecated Use {@link AE2CheckboxPainter} and {@link AE2Look} for new screens. */
@Deprecated
public class AE2Switches {

    private AE2Switches() {}

    public static IDrawable radio(Widget<?> widget, BooleanSupplier selected, Supplier<String> label,
        BooleanSupplier available) {
        return AE2CheckboxPainter.radio(widget, selected, label, available);
    }

    public static void checkboxLabel(String label, int x, int top, int width, int color, boolean shadow) {
        AE2CheckboxPainter.checkboxLabel(label, x, top, width, color, shadow);
    }

    public static int radioWidth(String label) {
        return AE2CheckboxPainter.radioWidth(label);
    }

    public static int drawSwitch(@Nullable Variant variant, int x, int y, boolean on, boolean hovered) {
        return AE2Look.of(variant)
            .drawCheckbox(x, y, on, hovered);
    }

    public static IDrawable toggleSwitch(Widget<?> widget, BooleanSupplier on, Supplier<String> label,
        BooleanSupplier available) {
        return AE2CheckboxPainter.checkbox(widget, on, label, available);
    }

    public static int switchWidth(String label) {
        return AE2CheckboxPainter.checkboxWidth(label);
    }

    /** @deprecated Use {@link AE2CheckboxPainter.CheckboxHover}. */
    @Deprecated
    public static class SwitchHover extends AE2CheckboxPainter.CheckboxHover {
    }
}
