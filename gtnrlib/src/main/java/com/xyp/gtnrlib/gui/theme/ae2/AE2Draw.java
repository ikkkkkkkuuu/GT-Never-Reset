package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.INACTIVE_ALPHA;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.Platform;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * Drawing helpers that do not depend on the look, shared by the AE2 painters and {@link PlainLook}: text, and icons
 * drawn tinted, faded or blended. Everything that depends on the look is asked of an {@link AE2Look}. Client only.
 */
public class AE2Draw {

    private AE2Draw() {}

    /**
     * Draws text and resets the OpenGL color to opaque white.
     *
     * @param text   display text
     * @param x      text left edge
     * @param y      text top edge
     * @param color  ARGB text color
     * @param shadow whether to draw a drop shadow
     */
    public static void text(String text, int x, int y, int color, boolean shadow) {
        Platform.setupDrawFont();
        Minecraft.getMinecraft().fontRenderer.drawString(text, x, y, color, shadow);
        GlStateManager.color(1, 1, 1, 1);
    }

    /**
     * Draws an icon tinted with a color, such as a green check or a red warning. The light sprite is used under every
     * theme so the tint keeps its brightness.
     *
     * @param icon  icon to draw
     * @param x     left edge
     * @param y     top edge
     * @param color RGB tint; the alpha byte is ignored
     */
    public static void drawTintedIcon(Icon icon, int x, int y, int color) {
        GlStateManager.color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
        icon.texture(Variant.LIGHT)
            .draw(x, y, ICON_SIZE, ICON_SIZE);
        GlStateManager.color(1, 1, 1, 1);
    }

    /**
     * Draws a 16-by-16 icon sprite, faded while the control it belongs to cannot be used.
     *
     * @param icon   sprite to draw
     * @param x      left edge
     * @param y      top edge
     * @param active whether the control can be used
     */
    public static void drawSprite(UITexture icon, int x, int y, boolean active) {
        GlStateManager.color(1, 1, 1, active ? 1 : INACTIVE_ALPHA);
        drawBlended(icon, x, y, ICON_SIZE, ICON_SIZE);
        GlStateManager.color(1, 1, 1, 1);
    }

    /**
     * Draws a texture with blending on, in the current color. MUI2 draws its own icons and check box without blending,
     * which drops the alpha of a faded color; this keeps it.
     *
     * @param texture texture to draw
     * @param x       left edge
     * @param y       top edge
     * @param width   width in pixels
     * @param height  height in pixels
     */
    public static void drawBlended(UITexture texture, int x, int y, int width, int height) {
        GuiDraw.drawTexture(
            texture.location,
            x,
            y,
            x + width,
            y + height,
            texture.u0,
            texture.v0,
            texture.u1,
            texture.v1,
            true);
    }
}
