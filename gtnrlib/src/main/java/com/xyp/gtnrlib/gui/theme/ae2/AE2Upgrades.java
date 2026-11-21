package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.PANEL_BOTTOM_EDGE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.PANEL_EDGE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SLOT_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.UPGRADE_PADDING;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.UPGRADE_ROWS;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * Layout and drawing of AE2's upgrade panel, the strip of upgrade slots on the right of a screen.
 */
public class AE2Upgrades {

    private static final int UPGRADE_SHADOW = 2;

    private AE2Upgrades() {}

    /**
     * Calculates an upgrade panel's width for columns of at most {@link AE2Theme#UPGRADE_ROWS} slots. AE2 attaches the
     * panel
     * to the right of the panel it belongs to, starting {@link AE2Theme#UPGRADE_OVERLAP} pixels inside that panel's
     * right edge,
     * so that many of these pixels cover the other panel's border.
     *
     * @param slots nonnegative slot count; zero still reserves one column
     * @return panel width in pixels, 23 for one column
     */
    public static int upgradePanelWidth(int slots) {
        return upgradePanelWidth(slots, UPGRADE_ROWS);
    }

    /**
     * Calculates an upgrade panel's width like {@link #upgradePanelWidth(int)} for columns of a chosen length.
     *
     * @param slots     nonnegative slot count; zero still reserves one column
     * @param perColumn slots in a full column, at least one; AE2 uses {@link AE2Theme#UPGRADE_ROWS}
     * @return panel width in pixels
     */
    public static int upgradePanelWidth(int slots, int perColumn) {
        return upgradeColumns(slots, perColumn) * SLOT_SIZE + UPGRADE_PADDING;
    }

    private static int upgradeColumns(int slots, int perColumn) {
        int rows = Math.max(1, perColumn);
        return Math.max(1, (slots + rows - 1) / rows);
    }

    /**
     * Returns the left edge of an 18-by-18 item slot widget in an upgrade panel, relative to the panel; the item sits
     * one pixel inside, on the slot well the panel draws. Turn off the slot widget's theme background
     * ({@code disableThemeBackground(true)}), or its own frame covers AE2's wells.
     *
     * @param index zero-based slot index; slots fill columns of {@link AE2Theme#UPGRADE_ROWS} from the top
     * @return x offset in pixels
     */
    public static int upgradeSlotX(int index) {
        return upgradeSlotX(index, UPGRADE_ROWS);
    }

    /**
     * Returns the left edge of an item slot widget in an upgrade panel with columns of a chosen length.
     *
     * @param index     zero-based slot index; slots fill each column from the top
     * @param perColumn slots in a full column, at least one
     * @return x offset in pixels
     * @see #upgradeSlotX(int)
     */
    public static int upgradeSlotX(int index, int perColumn) {
        return index / Math.max(1, perColumn) * SLOT_SIZE;
    }

    /**
     * Returns the top edge of an 18-by-18 item slot widget in an upgrade panel, relative to the panel.
     *
     * @param index zero-based slot index
     * @return y offset in pixels
     * @see #upgradeSlotX(int)
     */
    public static int upgradeSlotY(int index) {
        return upgradeSlotY(index, UPGRADE_ROWS);
    }

    /**
     * Returns the top edge of an item slot widget in an upgrade panel with columns of a chosen length.
     *
     * @param index     zero-based slot index
     * @param perColumn slots in a full column, at least one
     * @return y offset in pixels
     */
    public static int upgradeSlotY(int index, int perColumn) {
        return UPGRADE_PADDING + index % Math.max(1, perColumn) * SLOT_SIZE;
    }

    /**
     * Calculates an upgrade panel's height, including padding and its bottom shadow.
     *
     * @param slots nonnegative slot count; zero still reserves one row
     * @return panel height in pixels
     */
    public static int upgradePanelHeight(int slots) {
        return upgradePanelHeight(slots, UPGRADE_ROWS);
    }

    /**
     * Calculates an upgrade panel's height for columns of a chosen length.
     *
     * @param slots     nonnegative slot count; zero still reserves one row
     * @param perColumn slots in a full column, at least one
     * @return panel height in pixels
     */
    public static int upgradePanelHeight(int slots, int perColumn) {
        int rows = Math.max(1, Math.min(slots, Math.max(1, perColumn)));
        return UPGRADE_PADDING * 2 + UPGRADE_SHADOW + rows * SLOT_SIZE;
    }

    /**
     * Draws an upgrade panel and its empty slot wells as AE2 does. The panel has no left border: its first column
     * covers the right border of the panel it belongs to, so draw it after that panel at
     * {@code panelRight - UPGRADE_OVERLAP}. A light frame surrounds the block of 16-by-16 wells, which sit 18 pixels
     * apart in columns of {@link AE2Theme#UPGRADE_ROWS}. With no slots nothing is drawn, as in AE2.
     *
     * @param variant non-null AE2 variant
     * @param x       panel left edge
     * @param y       panel top edge
     * @param slots   nonnegative number of slots to draw
     * @see #upgradePanelWidth(int)
     * @see #upgradePanelHeight(int)
     * @see #upgradeSlotX(int)
     */
    public static void drawUpgradePanel(Variant variant, int x, int y, int slots) {
        drawUpgradePanel(variant, x, y, slots, UPGRADE_ROWS);
    }

    /**
     * Draws an upgrade panel like {@link #drawUpgradePanel(Variant, int, int, int)} with columns of a chosen length,
     * such as a single row of slots with {@code perColumn} 1.
     *
     * @param variant   non-null AE2 variant
     * @param x         panel left edge
     * @param y         panel top edge
     * @param slots     nonnegative number of slots to draw
     * @param perColumn slots in a full column, at least one
     * @see #upgradePanelWidth(int, int)
     * @see #upgradePanelHeight(int, int)
     * @see #upgradeSlotX(int, int)
     */
    public static void drawUpgradePanel(Variant variant, int x, int y, int slots, int perColumn) {
        if (slots <= 0) return;
        int columnSlots = Math.max(1, perColumn), columns = upgradeColumns(slots, columnSlots),
            rows = Math.min(slots, columnSlots), width = upgradePanelWidth(slots, columnSlots),
            height = upgradePanelHeight(slots, columnSlots);
        GlStateManager.color(1, 1, 1, 1);
        variant.upgradeFrame.draw(x + 1, y, width - 1, height);
        GuiDraw.drawRect(x, y + PANEL_EDGE, 1, height - PANEL_EDGE - PANEL_BOTTOM_EDGE, variant.colors.panel());
        int blockY = y + UPGRADE_PADDING, blockWidth = columns * SLOT_SIZE, blockHeight = rows * SLOT_SIZE;
        GuiDraw.drawRect(x, blockY, blockWidth, blockHeight, variant.colors.edge());
        GuiDraw.drawRect(x + 1, blockY + 1, blockWidth - 2, blockHeight - 2, variant.colors.panel());
        for (int i = 0; i < slots; i++) {
            int wellX = x + upgradeSlotX(i, columnSlots) + 1, wellY = y + upgradeSlotY(i, columnSlots) + 1;
            GuiDraw.drawRect(wellX, wellY, ICON_SIZE, 1, variant.colors.well());
            GuiDraw.drawRect(wellX, wellY + 1, ICON_SIZE, ICON_SIZE - 1, variant.colors.raised());
        }
        GlStateManager.color(1, 1, 1, 1);
    }
}
