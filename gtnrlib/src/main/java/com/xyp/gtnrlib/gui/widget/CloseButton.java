package com.xyp.gtnrlib.gui.widget;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.widget.sizer.Unit.Measure;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

/**
 * A small button that closes its panel, drawn with the theme's close-button style ({@code IThemeApi.CLOSE_BUTTON}) and
 * AE2's 4-by-4 cross, the one on AE2's small clear buttons, in the style's text color. It is {@link #SIZE} pixels
 * square, and {@link #AE2_HEIGHT} pixels high under AE2 themes, whose buttons keep a one-pixel shadow below the face,
 * so the cross sits in the middle of the face in every look; under AE2 themes it sinks with the face on hover. AE2 1.21
 * itself has no close button: its sub-screens return with {@link TabButton#back(Runnable)}.
 */
public class CloseButton extends ButtonWidget<CloseButton> {

    /** Width of the button, and its height outside AE2 themes. */
    public static final int SIZE = 12;
    /** Height of the button under AE2 themes. */
    public static final int AE2_HEIGHT = 13;
    private static final int CROSS_INSET = 4;
    private static final int[][] CROSS = { { 0, 0 }, { 3, 0 }, { 1, 1 }, { 2, 1 }, { 1, 2 }, { 2, 2 }, { 0, 3 },
        { 3, 3 } };

    private Runnable action = () -> getPanel().closeIfOpen();

    /**
     * Creates a button that closes the panel it is in; {@link #action(Runnable)} changes what it does.
     */
    public CloseButton() {
        widgetTheme(IThemeApi.CLOSE_BUTTON);
        overlay((context, x, y, width, height, theme) -> {
            int sink = AE2Look.of(this)
                .isAE2() && isHovering()
                    ? AE2ButtonDrawable.shadow(State.NORMAL, height) - AE2ButtonDrawable.shadow(State.HOVER, height)
                    : 0;
            int color = theme.getTextColor();
            for (int[] pixel : CROSS)
                GuiDraw.drawRect(x + CROSS_INSET + pixel[0], y + CROSS_INSET + sink + pixel[1], 1, 1, color);
        });
        width(SIZE).height(
            () -> AE2Look.of(this)
                .isAE2() ? AE2_HEIGHT : SIZE,
            Measure.PIXEL);
        onMousePressed(mouse -> {
            if (mouse != 0) return false;
            action.run();
            return true;
        });
    }

    public static CloseButton panelCorner() {
        CloseButton button = new CloseButton();
        return button.top(4)
            .right(4)
            .onMousePressed(mouse -> {
                if (mouse != 0 && mouse != 1) return false;
                button.action.run();
                return true;
            });
    }

    /**
     * Sets what a left click does instead of closing the panel.
     *
     * @param action action to run
     * @return this button
     */
    public CloseButton action(Runnable action) {
        this.action = action;
        return this;
    }

    /**
     * Places the button in the top-right corner of a panel laid out with the given padding.
     *
     * @param padding panel padding in pixels
     * @param style   layout theme giving the small spacing
     * @return this button
     */
    public CloseButton corner(int padding, LayoutTheme style) {
        int inset = inset(padding, style);
        return top(inset).right(inset);
    }

    /**
     * Returns how far from the panel's top and right edges {@link #corner} places the button.
     *
     * @param padding panel padding in pixels
     * @param style   layout theme giving the small spacing
     * @return inset in pixels
     */
    public static int inset(int padding, LayoutTheme style) {
        return Math.max(0, padding - style.spacingSmall);
    }

    /**
     * Returns how much of a row beside the button, inside the padding, the button and its gap take.
     *
     * @param padding panel padding in pixels
     * @param style   layout theme giving the small spacing
     * @return width in pixels
     */
    public static int reserve(int padding, LayoutTheme style) {
        return inset(padding, style) + SIZE + style.spacingSmall - padding;
    }
}
