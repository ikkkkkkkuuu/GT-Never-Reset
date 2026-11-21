package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.DISABLED_TEXT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.INACTIVE_ALPHA;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.Platform;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

/**
 * Drawing helpers shared by the AE2 painters: text, the theme a widget is drawn with, and the plain MUI2 button and
 * text used outside AE2 themes.
 */
public class AE2Draw {

    private static final int VANILLA_TEXT = 0xFF404040;

    private AE2Draw() {}

    /**
     * Draws text and resets the OpenGL color to opaque white. Client only, like every direct drawing method here.
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
     * Returns the color of text drawn on the panel: the AE2 text color, or the muted one for something unavailable,
     * under AE2 themes, and the screen theme's text color, or MUI2's disabled grey, otherwise.
     *
     * @param context   current drawing context
     * @param available whether the text belongs to something that can be used
     * @return ARGB text color
     * @see #panelTextColor(IWidget, boolean)
     */
    public static int panelTextColor(GuiContext context, boolean available) {
        return panelTextColor(screenTheme(context), available);
    }

    /**
     * Returns the color of text drawn on a panel like {@link #panelTextColor(GuiContext, boolean)}, from the theme of
     * the panel a widget belongs to.
     *
     * @param widget    widget in an open panel
     * @param available whether the text belongs to something that can be used
     * @return ARGB text color
     */
    public static int panelTextColor(IWidget widget, boolean available) {
        return panelTextColor(themeOf(widget), available);
    }

    private static int panelTextColor(@Nullable ITheme theme, boolean available) {
        Variant variant = AE2Look.of(theme)
            .variant();
        if (variant != null) return available ? variant.colors.text() : variant.colors.muted();
        if (!available) return DISABLED_TEXT;
        return theme == null ? VANILLA_TEXT
            : theme.getFallback()
                .getTheme()
                .getTextColor();
    }

    /**
     * Draws text in a theme's own text color and shadow, the plain look of labels outside AE2 themes.
     *
     * @param theme theme to read, possibly {@code null} for Minecraft's dark grey
     * @param text  text to draw
     * @param x     left edge
     * @param y     top edge
     */
    public static void plainText(@Nullable ITheme theme, String text, int x, int y) {
        WidgetTheme look = theme == null ? null
            : theme.getFallback()
                .getTheme();
        text(text, x, y, look == null ? VANILLA_TEXT : look.getTextColor(), look != null && look.getTextShadow());
    }

    /**
     * Returns the theme of the screen being drawn.
     *
     * @param context current drawing context
     * @return the screen's theme, or {@code null} outside modular screens
     */
    public static @Nullable ITheme screenTheme(GuiContext context) {
        return context instanceof ModularGuiContext modular ? modular.getTheme() : null;
    }

    /**
     * Returns the theme a widget is drawn with, like {@link #themeOf(IWidget)}, falling back to the screen's theme for
     * a widget that is not in a panel yet.
     *
     * @param widget  widget to look up
     * @param context current drawing context
     * @return the panel's or else the screen's theme, or {@code null} outside modular screens
     */
    public static @Nullable ITheme themeOf(IWidget widget, GuiContext context) {
        return widget.isValid() ? themeOf(widget) : screenTheme(context);
    }

    /**
     * Returns the theme a widget is drawn with: its panel's theme while it is in an open panel.
     *
     * @param widget widget to look up
     * @return the panel's theme, or {@code null} for a widget that is not in a panel yet
     */
    public static @Nullable ITheme themeOf(IWidget widget) {
        return widget.isValid() ? widget.getPanel()
            .getTheme() : null;
    }

    /**
     * Returns MUI2's button style of a theme for a button state.
     *
     * @param theme theme to read, possibly {@code null}
     * @param state button state; only the hovered state uses the hover style
     * @return the button style, or {@code null} without a theme
     */
    public static @Nullable WidgetTheme buttonLook(@Nullable ITheme theme, State state) {
        if (theme == null) return null;
        WidgetThemeEntry<?> entry = theme.getButtonTheme();
        return state == State.HOVER ? entry.getHoverTheme() : entry.getTheme();
    }

    /**
     * Draws a MUI2 button background of a theme, the plain look of AE2 buttons outside AE2 themes. A
     * disabled button, and a theme without a button background, use Minecraft's button.
     *
     * @param context current drawing context
     * @param theme   theme to read, possibly {@code null}
     * @param state   button state
     * @param x       left edge
     * @param y       top edge
     * @param width   width in pixels
     * @param height  height in pixels
     */
    public static void themeButton(GuiContext context, @Nullable ITheme theme, State state, int x, int y, int width,
        int height) {
        GlStateManager.color(1, 1, 1, 1);
        WidgetTheme look = buttonLook(theme, state);
        IDrawable background = look == null ? null : look.getBackground();
        if (state == State.HOVER && (background == null || background == IDrawable.NONE)) {
            look = buttonLook(theme, State.NORMAL);
            background = look == null ? null : look.getBackground();
        }
        if (state == State.DISABLED || background == null) {
            UITexture vanilla = state == State.DISABLED ? GuiTextures.MC_BUTTON_DISABLED
                : state == State.HOVER ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON;
            vanilla.draw(x, y, width, height);
        } else background.draw(context, x, y, width, height, look);
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
        icon.draw(x, y, ICON_SIZE, ICON_SIZE);
        GlStateManager.color(1, 1, 1, 1);
    }
}
