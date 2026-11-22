package com.xyp.gtnrlib.gui.theme.ae2;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/** @deprecated Use {@link AE2PanelPainter} and {@link AE2Look} for new screens. */
@Deprecated
public class AE2Panels {

    private AE2Panels() {}

    public static void drawButtonBar(Variant variant, int x, int y, int width, int height) {
        AE2PanelPainter.drawButtonBar(variant, x, y, width, height);
    }

    public static void drawCaret(int x, int y, boolean open, int color) {
        AE2PanelPainter.drawCaret(x, y, open, color);
    }

    public static int tableSize(int cells, int cellSize) {
        return AE2PanelPainter.tableSize(cells, cellSize);
    }

    public static void drawTable(Variant variant, int x, int y, int columns, int rows, int cellWidth, int cellHeight) {
        AE2PanelPainter.drawTable(variant, x, y, columns, rows, cellWidth, cellHeight);
    }

    public static IDrawable listRow(BooleanSupplier selected) {
        return AE2Theme.listRow(selected);
    }

    public static IDrawable panelBackground(int left, int right) {
        return AE2PanelPainter.panelBackground(null, left, right);
    }

    public static IDrawable panelBackground(IWidget owner, int left, int right) {
        return AE2PanelPainter.panelBackground(owner, left, right);
    }

    public static IDrawable well(int width, int plainColor) {
        return (context, x, y, ignoredWidth, height, theme) -> AE2Look.of(context)
            .drawWell((int) x, (int) y, width, (int) height);
    }

    public static void drawWell(@Nullable Variant variant, int x, int y, int width, int height, int plainColor) {
        AE2Look.of(variant)
            .drawWell(x, y, width, height);
    }
}
