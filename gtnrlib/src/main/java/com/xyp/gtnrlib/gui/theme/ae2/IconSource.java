package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.INACTIVE_ALPHA;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * What a toolbar button, a tab or a page tab shows in its 16-by-16 icon square: one of the built-in {@link Icon}s,
 * which follow the AE2 skin, or any texture or item through {@link #of(IDrawable)} and {@link #of(ItemStack)}.
 */
public interface IconSource {

    /**
     * Draws the icon at 16 by 16 pixels. Client only.
     *
     * @param context drawing context
     * @param theme   widget theme the control is drawn with
     * @param variant active AE2 variant, or {@code null} outside the AE2 themes
     * @param x       left edge
     * @param y       top edge
     * @param enabled whether the control is usable; built-in icons and textures fade when it is not
     */
    void drawIcon(GuiContext context, WidgetTheme theme, @Nullable Variant variant, int x, int y, boolean enabled);

    /**
     * Wraps a MUI2 drawable, such as a {@link UITexture} of a mod's own icon sheet, drawn at 16 by 16 pixels in
     * every theme. A {@link UITexture} fades when the control is unavailable, like the built-in icons; other
     * drawables are drawn unchanged.
     *
     * @param drawable drawable to show
     * @return the icon
     */
    static IconSource of(IDrawable drawable) {
        return (context, theme, variant, x, y, enabled) -> {
            if (!(drawable instanceof UITexture texture)) {
                drawable.draw(context, x, y, ICON_SIZE, ICON_SIZE, theme);
                return;
            }
            // Why: UITexture.draw sets an opaque color first and turns blending off for opaque textures, which
            // would undo the fade.
            GlStateManager.color(1, 1, 1, enabled ? 1 : INACTIVE_ALPHA);
            GuiDraw.drawTexture(
                texture.location,
                x,
                y,
                x + ICON_SIZE,
                y + ICON_SIZE,
                texture.u0,
                texture.v0,
                texture.u1,
                texture.v1,
                true);
            GlStateManager.color(1, 1, 1, 1);
        };
    }

    /**
     * Shows an item, drawn like an item in a slot. Items do not fade when the control is unavailable.
     *
     * @param stack item to show; it is not copied, so do not change it afterwards
     * @return the icon
     */
    static IconSource of(ItemStack stack) {
        return of(new ItemDrawable(stack));
    }
}
