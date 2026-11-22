package com.xyp.gtnrlib.gui.theme.ae2;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtnrlib.gui.theme.AE2Theme.ButtonText;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

/** @deprecated Use {@link AE2ButtonPainter} and {@link AE2Look} for new screens. */
@Deprecated
public class AE2Buttons {

    private AE2Buttons() {}

    public static IDrawable button(Widget<?> widget, BooleanSupplier available, BooleanSupplier selected) {
        return AE2ButtonPainter.button(widget, available, selected);
    }

    public static ButtonText buttonText(GuiContext context, State state, int height) {
        return AE2Look.of(context)
            .buttonText(state, height);
    }

    public static ButtonText buttonText(IWidget widget, State state, int height) {
        return AE2Look.of(widget)
            .buttonText(state, height);
    }

    public static IDrawable label(Widget<?> widget, Supplier<String> text, BooleanSupplier available,
        BooleanSupplier selected) {
        return AE2ButtonPainter.label(widget, text, available, selected);
    }

    public static IDrawable iconButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return AE2ButtonPainter.iconButton(widget, icon, available);
    }

    public static IDrawable iconButton(Widget<?> widget, Supplier<? extends IconSource> icon, BooleanSupplier available,
        BooleanSupplier selected) {
        return AE2ButtonPainter.iconButton(widget, icon, available, selected);
    }

    public static IDrawable tabButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return AE2ButtonPainter.tabButton(widget, icon, available);
    }

    public static IDrawable pageTab(Widget<?> widget, Supplier<? extends IconSource> icon, BooleanSupplier selected) {
        return AE2ButtonPainter.pageTab(widget, icon, selected);
    }

    public static IDrawable pageTab(Widget<?> widget, Supplier<? extends IconSource> icon, BooleanSupplier selected,
        BooleanSupplier available) {
        return AE2ButtonPainter.pageTab(widget, icon, selected, available);
    }
}
