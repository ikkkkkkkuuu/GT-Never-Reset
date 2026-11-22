package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;

public class ThemedButton extends ButtonWidget<ThemedButton> {

    public ThemedButton label(Supplier<String> label, BooleanSupplier available) {
        IDrawable look = AE2Theme.button(this, available, () -> false);
        return background(look).hoverBackground(look)
            .overlay(AE2Theme.label(this, label, available, () -> false));
    }

    @Override
    public boolean canBeSeen(IViewportStack stack) {
        // MUI2 can draw disabled children while dragging their panel.
        return isEnabled() && super.canBeSeen(stack);
    }
}
