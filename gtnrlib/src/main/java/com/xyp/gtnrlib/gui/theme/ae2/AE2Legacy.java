package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SHEET;

import net.minecraft.util.ResourceLocation;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * The state and terminal sprite sheets of GTNH's AE2 (rv3), as AE2 ships them or recolored for each AE2 skin.
 */
public class AE2Legacy {

    private static final ResourceLocation AE2_LEGACY_STATES = new ResourceLocation(
        "appliedenergistics2",
        "textures/guis/states.png"),
        AE2_LEGACY_TERMINAL = new ResourceLocation("appliedenergistics2", "textures/guis/terminal.png");
    private static final int LEGACY_CELLS = 16;

    private AE2Legacy() {}

    /**
     * Resolves the legacy AE2 rv3 state sprite sheet.
     *
     * @param look look to draw in: an AE2 skin gives its recolored sheet, the plain look the sheet AE2 ships
     * @return state sheet resource location
     */
    public static ResourceLocation legacyStates(AE2Look look) {
        Variant variant = look.variant();
        return variant == null ? AE2_LEGACY_STATES : variant.legacyStates;
    }

    /**
     * Resolves the legacy AE2 rv3 terminal sprite sheet.
     *
     * @param look look to draw in: an AE2 skin gives its recolored sheet, the plain look the sheet AE2 ships
     * @return terminal sheet resource location
     */
    public static ResourceLocation legacyTerminal(AE2Look look) {
        Variant variant = look.variant();
        return variant == null ? AE2_LEGACY_TERMINAL : variant.legacyTerminal;
    }

    /**
     * Draws one cell from the 16-by-16 grid of the legacy state sheet.
     *
     * @param look  look to draw in: an AE2 skin gives its recolored sheet, the plain look the sheet AE2 ships
     * @param index zero-based cell index, from zero through 255
     * @param x     destination left edge
     * @param y     destination top edge
     * @param size  destination width and height in pixels
     */
    public static void drawLegacyState(AE2Look look, int index, float x, float y, float size) {
        float u = index % LEGACY_CELLS * ICON_SIZE / (float) SHEET,
            v = index / LEGACY_CELLS * ICON_SIZE / (float) SHEET, span = ICON_SIZE / (float) SHEET;
        GuiDraw.drawTexture(legacyStates(look), x, y, x + size, y + size, u, v, u + span, v + span, true);
    }
}
