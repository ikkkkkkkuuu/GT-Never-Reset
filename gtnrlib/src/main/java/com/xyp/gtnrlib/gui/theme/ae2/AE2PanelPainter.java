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
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * AE2 panel pieces: panel backgrounds, list wells and rows, the vertical toolbar strip, tables and the dropdown caret.
 */
public class AE2PanelPainter {

    private AE2PanelPainter() {}

    /**
     * Draws the background of a vertical icon-button bar.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the background
     * @param y       top edge of the background
     * @param width   background width, including button margins
     * @param height  background height, including button margins and spacing
     * @see AE2Theme#BUTTON_BAR_MARGIN
     * @see AE2Theme#BUTTON_BAR_SPACING
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
     * Cell {@code (column, row)} begins at {@code (x + 1 + column * (cellWidth + 1), y + 1 + row * (cellHeight + 1))}.
     * Cell contents are drawn separately by the caller.
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
     * Creates a selectable list-row background in the look of a widget's panel, resolved at draw time; see
     * {@link #drawListRow}.
     *
     * @param owner    widget whose panel gives the look, usually the row widget itself
     * @param selected selection supplier; a hover supplier can be used for hover highlighting
     * @return the row background drawable
     */
    public static IDrawable listRow(IWidget owner, BooleanSupplier selected) {
        return (context, x, y, width, height,
            theme) -> drawListRow(AE2Look.of(owner), selected.getAsBoolean(), x, y, width, height);
    }

    /**
     * Draws a selectable list-row background. A selected row looks like a selected row of {@code IconRowList} ({@link
     * AE2Look#drawSelectedRow}); an unselected row is AE2's framed row under an AE2 skin and left clear in the plain
     * look.
     *
     * @param look     look to draw in
     * @param selected whether the row is selected
     * @param x        left edge
     * @param y        top edge
     * @param width    row width in pixels
     * @param height   row height in pixels
     */
    public static void drawListRow(AE2Look look, boolean selected, int x, int y, int width, int height) {
        GlStateManager.color(1, 1, 1, 1);
        if (selected) look.drawSelectedRow(x, y, width, height);
        else if (look.variant() != null) look.variant().listRow.draw(x, y, width, height);
    }

    /**
     * Creates the panel background for a panel whose edges are covered by AE2 toolbar strips or page tabs: the panel
     * texture of the theme of the panel a widget belongs to, drawn between the given insets. Use it with
     * {@code disableThemeBackground(true)}.
     *
     * @param owner widget whose panel's theme supplies the texture, usually the panel itself
     * @param left  pixels from the drawn area's left edge to the background, such as
     *              {@code VerticalButtonBar.WIDTH - VerticalButtonBar.OVERLAP}
     * @param right pixels from the drawn area's right edge to the background, such as
     *              {@code PAGE_TAB_SIZE - PAGE_TAB_INSET}
     * @return background drawable
     */
    public static IDrawable panelBackground(IWidget owner, int left, int right) {
        return (context, x, y, width, height, widgetTheme) -> {
            ITheme theme = panelTheme(owner, context);
            if (theme == null) return;
            WidgetTheme style = theme.getPanelTheme()
                .getTheme();
            IDrawable panel = style.getBackground();
            if (panel != null) panel.draw(context, x + left, y, width - left - right, height, style);
        };
    }

    private static @Nullable ITheme panelTheme(IWidget owner, GuiContext context) {
        if (owner != null && owner.isValid()) return owner.getPanel()
            .getTheme();
        return context instanceof ModularGuiContext modular ? modular.getTheme() : null;
    }

    /**
     * Creates a list well in the look of a widget's panel, drawn by {@link AE2Look#drawWell}: AE2's inset under AE2
     * themes, Minecraft's sunken slot box otherwise. The well starts at the drawn area's left edge and has a fixed
     * width, so a widget can be wider than its well, for example to leave room for the scroll bar that AE2 places
     * beside it.
     *
     * @param owner widget whose panel gives the look, usually the widget the well is the background of
     * @param width well width in pixels
     * @return background drawable
     */
    public static IDrawable well(IWidget owner, int width) {
        return (context, x, y, w, h, widgetTheme) -> AE2Look.of(owner)
            .drawWell(x, y, width, h);
    }
}
