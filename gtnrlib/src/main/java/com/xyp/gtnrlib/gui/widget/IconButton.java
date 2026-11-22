package com.xyp.gtnrlib.gui.widget;

import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.widget.sizer.Unit.Measure;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ButtonPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;

/**
 * AE2's toolbar icon button ({@code IconButton}): {@link AE2ButtonPainter#iconButton}, drawn by the theme of its panel;
 * a selected one shows AE2's selected frame. Unless the caller sets a size it is {@link AE2Theme#ICON_BUTTON_WIDTH} by
 * {@link AE2Look#iconButtonHeight()} pixels: AE2's 18 by 20 under AE2 themes, an 18-pixel square in others. It reacts
 * to the left mouse button on its whole frame, where AE2's react only on the 16-by-16 icon square; see
 * {@link ThemedButton} for the available and selected states.
 */
public class IconButton extends ThemedButton<IconButton> {

    /**
     * Creates a toolbar icon button.
     *
     * @param icon   supplier of the icon, read every frame: a built-in {@code Icon}, or any texture or item through
     *               {@link IconSource#of}
     * @param action run on a left click while the button is available
     */
    public IconButton(Supplier<? extends IconSource> icon, Runnable action) {
        IDrawable look = AE2ButtonPainter.iconButton(this, icon, this::isAvailable, this::isSelected);
        background(look).hoverBackground(look)
            .width(AE2Theme.ICON_BUTTON_WIDTH)
            .height(
                () -> AE2Look.of(this)
                    .iconButtonHeight(),
                Measure.PIXEL)
            .action(action);
    }
}
