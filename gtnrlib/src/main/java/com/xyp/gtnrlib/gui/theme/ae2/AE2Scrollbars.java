package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.BIG_SCROLLER_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SCROLLBAR_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SCROLLER_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SHEET;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * AE2 scroll-bar grooves and handles, and the vanilla creative-inventory scroll bar used outside AE2 themes.
 */
public class AE2Scrollbars {

    private static final ResourceLocation CREATIVE_TABS = new ResourceLocation(
        "textures/gui/container/creative_inventory/tabs.png");
    private static final int GROOVE_X = 3, GROOVE_WIDTH = 6, SMALL_GROOVE_X = 1, SMALL_GROOVE_WIDTH = 5,
        VANILLA_SCROLLER_U = 232, VANILLA_SCROLLER_DISABLED_U = 244, VANILLA_TRACK = 0xFF8B8B8B,
        VANILLA_TRACK_SHADOW = 0xFF373737, VANILLA_TRACK_LIGHT = 0xFFFFFFFF;
    private static final UITexture VANILLA_SCROLLER = vanillaScroller(VANILLA_SCROLLER_U),
        VANILLA_SCROLLER_DISABLED = vanillaScroller(VANILLA_SCROLLER_DISABLED_U);

    private AE2Scrollbars() {}

    /**
     * Returns the width occupied by a large scroll bar, including its track border.
     *
     * @param variant AE2 variant, or {@code null} for the vanilla creative-inventory style
     * @return 12 pixels for AE2 or 14 pixels for vanilla
     */
    public static int scrollbarWidth(@Nullable Variant variant) {
        return variant == null ? BIG_SCROLLER_WIDTH + 2 : BIG_SCROLLER_WIDTH;
    }

    /**
     * Draws a large scroll-bar track and a fixed 12-by-15-pixel handle.
     *
     * @param variant AE2 variant, or {@code null} for the vanilla creative-inventory style
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner track top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     */
    public static void drawScrollbar(@Nullable Variant variant, int x, int y, int height, int handleY,
        boolean enabled) {
        GlStateManager.color(1, 1, 1, 1);
        int handleTop = y + 1 + handleY;
        if (variant == null) {
            int width = scrollbarWidth(null);
            GuiDraw.drawRect(x, y, width, height, VANILLA_TRACK);
            GuiDraw.drawRect(x, y, width - 1, 1, VANILLA_TRACK_SHADOW);
            GuiDraw.drawRect(x, y, 1, height - 1, VANILLA_TRACK_SHADOW);
            GuiDraw.drawRect(x + width - 1, y + 1, 1, height - 1, VANILLA_TRACK_LIGHT);
            GuiDraw.drawRect(x + 1, y + height - 1, width - 1, 1, VANILLA_TRACK_LIGHT);
            GlStateManager.color(1, 1, 1, 1);
            (enabled ? VANILLA_SCROLLER : VANILLA_SCROLLER_DISABLED)
                .draw(x + 1, handleTop, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
            return;
        }
        variant.inset.draw(x + GROOVE_X, y, GROOVE_WIDTH, height);
        (enabled ? variant.bigScroller : variant.bigScrollerDisabled)
            .draw(x, handleTop, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }

    /**
     * Draws a five-pixel groove and a fixed 7-by-15-pixel AE2 handle. The groove looks like the one AE2 draws for its
     * small scroll bar in the processing pattern panel: a one-pixel line-colored outline around a raised fill whose
     * top row is shaded.
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
            .draw(x, y + 1 + handleY, SCROLLBAR_WIDTH, SCROLLER_HEIGHT);
    }

    private static UITexture vanillaScroller(int u) {
        return UITexture.builder()
            .location(CREATIVE_TABS)
            .imageSize(SHEET, SHEET)
            .subAreaXYWH(u, 0, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT)
            .build();
    }
}
