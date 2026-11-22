package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.BIG_SCROLLER_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SCROLLER_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SMALL_SCROLLER_WIDTH;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * AE2 scroll-bar grooves and handles. A scroll bar that follows the look, with the creative-inventory scroll bar
 * outside AE2 themes, is drawn by {@link AE2Look#drawScrollbar}.
 */
public class AE2ScrollbarPainter {

    private static final int GROOVE_X = 3, GROOVE_WIDTH = 6, SMALL_GROOVE_X = 1, SMALL_GROOVE_WIDTH = 5;

    private AE2ScrollbarPainter() {}

    /**
     * Draws AE2's large scroll bar: the inset groove and a fixed 12-by-15-pixel handle. Widgets that follow the look
     * call {@link AE2Look#drawScrollbar} instead.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner track top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     */
    public static void drawScrollbar(Variant variant, int x, int y, int height, int handleY, boolean enabled) {
        GlStateManager.color(1, 1, 1, 1);
        variant.inset.draw(x + GROOVE_X, y, GROOVE_WIDTH, height);
        (enabled ? variant.bigScroller : variant.bigScrollerDisabled)
            .draw(x, y + 1 + handleY, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }

    /**
     * Draws a five-pixel groove and a fixed 7-by-15-pixel AE2 handle. The groove looks like the one AE2 draws for its
     * small scroll bar in the processing pattern panel: a one-pixel line-colored outline around a raised fill whose top
     * row is shaded.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the handle; the groove starts one pixel to its right
     * @param y       top edge of the groove, including its one-pixel border
     * @param height  total groove height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner groove top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     */
    public static void drawSmallScrollbar(Variant variant, int x, int y, int height, int handleY, boolean enabled) {
        int grooveX = x + SMALL_GROOVE_X, inner = SMALL_GROOVE_WIDTH - 2;
        GuiDraw.drawRect(grooveX, y, SMALL_GROOVE_WIDTH, height, variant.colors.line());
        GuiDraw.drawRect(grooveX + 1, y + 1, inner, 1, variant.colors.well());
        GuiDraw.drawRect(grooveX + 1, y + 2, inner, height - 3, variant.colors.raised());
        GlStateManager.color(1, 1, 1, 1);
        (enabled ? variant.scroller : variant.scrollerDisabled)
            .draw(x, y + 1 + handleY, SMALL_SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }
}
