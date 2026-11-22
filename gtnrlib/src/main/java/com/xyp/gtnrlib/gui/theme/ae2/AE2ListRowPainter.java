package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.INACTIVE_ALPHA;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.LABEL_HEIGHT;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * The parts of a list row that {@code IconRowList}'s drawn rows and {@code ListRow} widgets share, so rows of either
 * kind look the same in one list: where the text sits, the check box against the row's right end and the section
 * header. Colors and the well come from the {@link AE2Look}. Client only.
 */
public class AE2ListRowPainter {

    /** Standard height of a list row in pixels. */
    public static final int ROW_HEIGHT = 18;
    private static final int HEADER_LINE_ALPHA = 0x40000000, HEADER_LINE_GAP = 4;

    private AE2ListRowPainter() {}

    /**
     * Returns how far below a row's top its one-line text starts, so the text is centred in the row; 5 for the standard
     * {@link #ROW_HEIGHT}.
     *
     * @param rowHeight row height in pixels
     * @return text offset in pixels
     */
    public static int textTop(int rowHeight) {
        return (rowHeight - LABEL_HEIGHT) / 2;
    }

    /**
     * Draws a row's check box against the row's right end, centred in the row and faded while the row cannot be used;
     * the box itself is {@link AE2Look#drawCheckbox}.
     *
     * @param look      look of the panel the row is in
     * @param right     pixel the check box ends at
     * @param top       top of the row
     * @param rowHeight row height in pixels
     * @param on        whether the box is checked
     * @param hovered   whether the row is under the mouse
     * @param available whether the row can be used
     * @return width of the drawn check box
     */
    public static int drawCheckbox(AE2Look look, int right, int top, int rowHeight, boolean on, boolean hovered,
        boolean available) {
        GlStateManager.color(1, 1, 1, available ? 1 : INACTIVE_ALPHA);
        int width = look
            .drawCheckbox(right - look.checkboxWidth(), top + (rowHeight - CHECKBOX_HEIGHT) / 2, on, hovered);
        GlStateManager.color(1, 1, 1, 1);
        return width;
    }

    /**
     * Draws a section header: text in the look's muted color, followed under AE2 skins by a divider line up to the
     * row's end.
     *
     * @param look      look of the panel the row is in
     * @param text      header text, already cut to the available width
     * @param left      left edge of the text
     * @param top       top of the row
     * @param rowHeight row height in pixels
     * @param right     right end of the row
     */
    public static void drawHeader(AE2Look look, String text, int left, int top, int rowHeight, int right) {
        int textY = top + textTop(rowHeight);
        AE2Draw.text(text, left, textY, look.listMuted(), false);
        Variant variant = look.variant();
        if (variant == null) return;
        int start = left + Minecraft.getMinecraft().fontRenderer.getStringWidth(text) + HEADER_LINE_GAP,
            width = right - HEADER_LINE_GAP - start;
        if (width <= 0) return;
        GuiDraw.drawRect(start, textY + 4, width, 1, variant.colors.line());
        GuiDraw.drawRect(start, textY + 5, width, 1, HEADER_LINE_ALPHA | variant.colors.edge() & 0xFFFFFF);
    }
}
