package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.CARET_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.CARET_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.INSET_EDGE;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

/**
 * AE2 panel pieces: panel backgrounds, list wells and rows, the vertical toolbar strip, tables and the dropdown caret.
 */
public class AE2Panels {

    private AE2Panels() {}

    /**
     * Draws the background of a vertical icon-button bar.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the background
     * @param y       top edge of the background
     * @param width   background width, including button margins
     * @param height  background height, including button margins and spacing
     * @see #BUTTON_BAR_MARGIN
     * @see #BUTTON_BAR_SPACING
     */
    public static void drawButtonBar(Variant variant, int x, int y, int width, int height) {
        GlStateManager.color(1, 1, 1, 1);
        variant.buttonBar.draw(x, y, width, height);
    }

    /**
     * Draws the five-by-three-pixel triangle that marks a dropdown.
     *
     * @param x     left edge of the triangle
     * @param y     top edge of the triangle
     * @param open  whether the dropdown is open, which points the triangle up
     * @param color ARGB color
     */
    public static void drawCaret(int x, int y, boolean open, int color) {
        for (int line = 0; line < CARET_HEIGHT; line++) {
            int row = open ? CARET_HEIGHT - 1 - line : line;
            GuiDraw.drawRect(x + line, y + row, CARET_WIDTH - line * 2, 1, color);
        }
    }

    /**
     * Calculates a table dimension with one-pixel separators and outer borders.
     *
     * @param cells    positive number of cells along the axis
     * @param cellSize positive inner size of each cell in pixels
     * @return total dimension in pixels
     */
    public static int tableSize(int cells, int cellSize) {
        return cells * cellSize + cells - 1 + INSET_EDGE * 2;
    }

    /**
     * Draws an inset table background with one-pixel grid lines.
     * <p>
     * Cell {@code (column, row)} begins at {@code (x + 1 + column * (cellWidth + 1),
     * y + 1 + row * (cellHeight + 1))}. Cell contents are drawn separately by the caller.
     *
     * @param variant    non-null AE2 variant
     * @param x          table left edge
     * @param y          table top edge
     * @param columns    positive column count
     * @param rows       positive row count
     * @param cellWidth  positive inner cell width
     * @param cellHeight positive inner cell height
     */
    public static void drawTable(Variant variant, int x, int y, int columns, int rows, int cellWidth, int cellHeight) {
        int width = tableSize(columns, cellWidth), height = tableSize(rows, cellHeight);
        GlStateManager.color(1, 1, 1, 1);
        variant.inset.draw(x, y, width, height);
        for (int column = 1; column < columns; column++) GuiDraw.drawRect(
            x + INSET_EDGE + column * (cellWidth + 1) - 1,
            y + INSET_EDGE,
            1,
            height - INSET_EDGE * 2,
            variant.colors.line());
        for (int row = 1; row < rows; row++) GuiDraw.drawRect(
            x + INSET_EDGE,
            y + INSET_EDGE + row * (cellHeight + 1) - 1,
            width - INSET_EDGE * 2,
            1,
            variant.colors.line());
        GlStateManager.color(1, 1, 1, 1);
    }

    /**
     * Creates a selectable list-row background resolved against the screen's theme at draw time.
     *
     * @param selected selection supplier; a hover supplier can be used for hover highlighting
     * @return an AE2 nine-slice row or the current MUI2 button background
     */
    public static IDrawable listRow(BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> {
            boolean lit = selected.getAsBoolean();
            Variant variant = AE2Theme.active(context);
            if (variant == null) {
                AE2Draw.themeButton(
                    context,
                    AE2Draw.screenTheme(context),
                    lit ? State.HOVER : State.NORMAL,
                    x,
                    y,
                    width,
                    height);
                return;
            }
            GlStateManager.color(1, 1, 1, 1);
            (lit ? variant.listRowSelected : variant.listRow).draw(x, y, width, height);
        };
    }

    /**
     * Creates the panel background for a panel whose edges are covered by AE2 toolbar strips or page tabs: the screen
     * theme's panel texture drawn between the given insets. Use it with {@code disableThemeBackground(true)}.
     *
     * @param left  pixels from the panel's left edge to the background, such as
     *              {@code VerticalButtonBar.WIDTH - VerticalButtonBar.OVERLAP}
     * @param right pixels from the panel's right edge to the background, such as
     *              {@code PAGE_TAB_SIZE - PAGE_TAB_INSET}
     * @return background drawable for the panel
     * @see #panelBackground(IWidget, int, int)
     */
    public static IDrawable panelBackground(int left, int right) {
        return (context, x, y, width, height, widgetTheme) -> drawPanelBackground(
            AE2Draw.screenTheme(context),
            context,
            x + left,
            y,
            width - left - right,
            height);
    }

    /**
     * Creates a panel background like {@link #panelBackground(int, int)} that uses the theme of the panel a widget
     * belongs to, so it also follows a panel with a theme of its own.
     *
     * @param owner widget whose panel's theme supplies the texture, usually the panel itself
     * @param left  pixels from the drawn area's left edge to the background
     * @param right pixels from the drawn area's right edge to the background
     * @return background drawable
     */
    public static IDrawable panelBackground(IWidget owner, int left, int right) {
        return (context, x, y, width, height, widgetTheme) -> drawPanelBackground(
            AE2Draw.themeOf(owner, context),
            context,
            x + left,
            y,
            width - left - right,
            height);
    }

    private static void drawPanelBackground(@Nullable ITheme theme, GuiContext context, int x, int y, int width,
        int height) {
        if (theme == null) return;
        WidgetTheme look = theme.getPanelTheme()
            .getTheme();
        IDrawable panel = look.getBackground();
        if (panel != null) panel.draw(context, x, y, width, height, look);
    }

    /**
     * Creates a list well: AE2's inset under AE2 themes, a flat rectangle otherwise. The well starts at the drawn
     * area's left edge and has a fixed width, so a widget can be wider than its well, for example to leave room for
     * the scroll bar that AE2 places beside it.
     *
     * @param width      well width in pixels
     * @param plainColor ARGB fill used outside the AE2 themes
     * @return background drawable
     */
    public static IDrawable well(int width, int plainColor) {
        return (context, x, y, w, h, widgetTheme) -> drawWell(AE2Theme.active(context), x, y, width, h, plainColor);
    }

    /**
     * Draws a list well directly, for widgets that draw their own content.
     *
     * @param variant    active AE2 variant, or {@code null} outside the AE2 themes
     * @param x          left edge
     * @param y          top edge
     * @param width      well width in pixels
     * @param height     well height in pixels
     * @param plainColor ARGB fill used outside the AE2 themes
     * @see #well(int, int)
     */
    public static void drawWell(@Nullable Variant variant, int x, int y, int width, int height, int plainColor) {
        if (variant == null) GuiDraw.drawRect(x, y, width, height, plainColor);
        else variant.inset.draw(x, y, width, height);
    }
}
