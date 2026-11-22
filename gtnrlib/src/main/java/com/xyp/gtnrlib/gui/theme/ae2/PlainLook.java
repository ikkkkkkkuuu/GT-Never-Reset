package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.BIG_SCROLLER_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.DISABLED_TEXT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_BUTTON_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.INACTIVE_ALPHA;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.LABEL_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.LABEL_PADDING;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SCROLLER_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SHEET;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SMALL_SCROLLER_WIDTH;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.xyp.gtnrlib.gui.theme.AE2Theme.ButtonText;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

/**
 * The plain look of GTNRLib's AE2-styled widgets, used under every theme that is not an AE2 theme: Minecraft's sunken
 * slot box and creative-inventory scroll bar, MUI2's button, check box and icons, and the theme's own text style.
 * <p>
 * Every color and drawing rule of the plain look is written here once; the AE2 painters only draw the AE2 skins. An
 * {@link AE2Look} that is not an AE2 skin hands out its plain look with {@link AE2Look#plainLook()}, for the few places
 * where the plain layout differs from AE2's. Colors are constants because they do not depend on the theme; text and
 * buttons read the theme the look was resolved from.
 * <p>
 * The class loads on both sides; its drawing methods are client only.
 */
public class PlainLook {

    /** ARGB fill, top-left shadow and bottom-right light of Minecraft's sunken slot box. */
    public static final int WELL_FILL = 0xFF8B8B8B, WELL_SHADOW = 0xFF373737, WELL_LIGHT = 0xFFFFFFFF;
    /**
     * ARGB colors of list rows: white row text, Minecraft's dark grey label color for headers and unusable rows, the
     * selected row's bar, the shade over a hovered row and the shade under nested rows.
     */
    public static final int LIST_TEXT = 0xFFFFFFFF, MINECRAFT_TEXT = 0xFF404040, SELECTED_ROW = 0xFF5A5A5A,
        ROW_HOVER = 0x30FFFFFF, NESTED_ROWS = 0x20000000;
    /**
     * ARGB colors of something marked good, such as a filled progress bar, and of something marked bad, AE2 1.21's
     * {@code ERROR} palette color. Other mods read them through {@link AE2Look#good()} and {@link AE2Look#bad()}: these
     * constants are copied into a mod when it is compiled, so a later GTNRLib that changes them would not reach it.
     */
    public static final int GOOD = 0xFF60E060, BAD = 0xFFCE2401;
    /** ARGB color of invalid text in an input field, AE2 1.21's {@code TEXTFIELD_ERROR} palette color. */
    public static final int INPUT_ERROR = 0xFFFF1900;
    /** ARGB color of the thin scroll bar of drawn lists, the face color of Minecraft's scroll handle. */
    public static final int THIN_SCROLL_BAR = 0xFFC6C6C6;
    /** Width of the creative-inventory scroll bar: its 12-pixel handle and a one-pixel track border on each side. */
    public static final int SCROLLBAR_WIDTH = BIG_SCROLLER_WIDTH + 2;
    /**
     * Border width of MUI2's vanilla panel background on each side. Parts attached to a panel's edge, such as the
     * toolbar and the upgrade panel, leave out their border on that side and cover the panel's border this far.
     */
    public static final int PANEL_BORDER = 4;
    /** Width and height of a toolbar icon button: a square, as Minecraft's button has no shadow below its face. */
    public static final int ICON_BUTTON_SIZE = ICON_BUTTON_WIDTH;
    /** Size of MUI2's check box as the plain look draws it for an on/off value. */
    public static final int CHECKBOX_SIZE = CHECKBOX_HEIGHT;
    /** Pixels from the left edge of a labelled check box to its label. */
    public static final int CHECKBOX_LABEL_X = CHECKBOX_SIZE + LABEL_PADDING * 2;
    private static final ResourceLocation CREATIVE_TABS = new ResourceLocation(
        "textures/gui/container/creative_inventory/tabs.png");
    /**
     * Width of the small scroll bar: Minecraft's handle narrowed to five pixels in a sunken track with a one-pixel
     * border, as wide as AE2's small scroll bar.
     */
    public static final int SMALL_SCROLLBAR_WIDTH = SMALL_SCROLLER_WIDTH;
    private static final int SCROLLER_U = 232, SCROLLER_DISABLED_U = 244, SLIM_EDGE = 2, SLIM_GRIP = 1,
        SLIM_RIGHT = BIG_SCROLLER_WIDTH - SLIM_EDGE;
    private static final UITexture SCROLLER = scroller(SCROLLER_U, 0, BIG_SCROLLER_WIDTH),
        SCROLLER_DISABLED = scroller(SCROLLER_DISABLED_U, 0, BIG_SCROLLER_WIDTH);
    private static final UITexture[] SLIM = slim(SCROLLER_U), SLIM_DISABLED = slim(SCROLLER_DISABLED_U);

    private final @Nullable ITheme theme;

    /**
     * Creates the plain look of a theme.
     *
     * @param theme theme whose text and button styles are used, or {@code null} for Minecraft's own
     */
    public PlainLook(@Nullable ITheme theme) {
        this.theme = theme;
    }

    /**
     * Reports whether this look reads the given theme, so a cached look can be reused.
     *
     * @param other theme to compare, possibly {@code null}
     * @return {@code true} if this look was created for that theme
     */
    public boolean reads(@Nullable ITheme other) {
        return theme == other;
    }

    /**
     * Draws Minecraft's sunken slot box: grey fill, dark top and left edges, white bottom and right edges.
     *
     * @param x      left edge
     * @param y      top edge
     * @param width  box width in pixels
     * @param height box height in pixels
     */
    public static void drawWell(int x, int y, int width, int height) {
        GuiDraw.drawRect(x, y, width, height, WELL_FILL);
        GuiDraw.drawRect(x, y, width - 1, 1, WELL_SHADOW);
        GuiDraw.drawRect(x, y, 1, height - 1, WELL_SHADOW);
        GuiDraw.drawRect(x + width - 1, y + 1, 1, height - 1, WELL_LIGHT);
        GuiDraw.drawRect(x + 1, y + height - 1, width - 1, 1, WELL_LIGHT);
    }

    /**
     * Returns the color of text on the panel: the theme's text color, or MUI2's disabled grey for something that cannot
     * be used.
     *
     * @param available whether the text belongs to something that can be used
     * @return ARGB text color
     */
    public int textColor(boolean available) {
        if (!available) return DISABLED_TEXT;
        return theme == null ? MINECRAFT_TEXT
            : theme.getFallback()
                .getTheme()
                .getTextColor();
    }

    /**
     * Draws a label: in the theme's own text color and shadow while available, in MUI2's disabled grey without a shadow
     * otherwise.
     *
     * @param text      text to draw
     * @param x         left edge
     * @param y         top edge
     * @param available whether the control the label belongs to can be used
     */
    public void drawLabel(String text, int x, int y, boolean available) {
        WidgetTheme style = theme == null ? null
            : theme.getFallback()
                .getTheme();
        if (available) AE2Draw.text(
            text,
            x,
            y,
            style == null ? MINECRAFT_TEXT : style.getTextColor(),
            style != null && style.getTextShadow());
        else AE2Draw.text(text, x, y, DISABLED_TEXT, false);
    }

    /**
     * Resolves the text style of a button: the theme's button text color and shadow, centred in the button, or MUI2's
     * disabled grey for a disabled button.
     *
     * @param state  visual button state
     * @param height button height in pixels
     * @return text color, vertical offset and shadow setting
     */
    public ButtonText buttonText(State state, int height) {
        WidgetTheme style = buttonStyle(state);
        int color = state == State.DISABLED ? DISABLED_TEXT : style == null ? Color.WHITE.main : style.getTextColor();
        return new ButtonText(color, (height - LABEL_HEIGHT) / 2, style == null || style.getTextShadow());
    }

    /**
     * Draws the theme's MUI2 button background. A disabled button, and a theme without a button background, use
     * Minecraft's button.
     *
     * @param context current drawing context
     * @param state   button state
     * @param x       left edge
     * @param y       top edge
     * @param width   width in pixels
     * @param height  height in pixels
     */
    public void drawButton(GuiContext context, State state, int x, int y, int width, int height) {
        GlStateManager.color(1, 1, 1, 1);
        WidgetTheme style = buttonStyle(state);
        IDrawable background = style == null ? null : style.getBackground();
        if (state == State.HOVER && (background == null || background == IDrawable.NONE)) {
            style = buttonStyle(State.NORMAL);
            background = style == null ? null : style.getBackground();
        }
        if (state == State.DISABLED || background == null) {
            UITexture vanilla = state == State.DISABLED ? GuiTextures.MC_BUTTON_DISABLED
                : state == State.HOVER ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON;
            vanilla.draw(x, y, width, height);
        } else background.draw(context, x, y, width, height, style);
        GlStateManager.color(1, 1, 1, 1);
    }

    /**
     * Draws a button label in the style of {@link #buttonText}: one line cut to the button's width and centred in it.
     *
     * @param text   label text
     * @param x      left edge of the button
     * @param y      top edge of the button
     * @param width  button width in pixels
     * @param height button height in pixels
     * @param state  visual button state
     */
    public void drawButtonLabel(String text, int x, int y, int width, int height, State state) {
        ButtonText style = buttonText(state, height);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String fitted = font.trimStringToWidth(text, Math.max(0, width - LABEL_PADDING * 2));
        int left = x + (width - font.getStringWidth(fitted)) / 2;
        AE2Draw.text(fitted, left, y + style.y(), style.color(), style.shadow());
    }

    private @Nullable WidgetTheme buttonStyle(State state) {
        if (theme == null) return null;
        WidgetThemeEntry<?> entry = theme.getButtonTheme();
        return state == State.HOVER ? entry.getHoverTheme() : entry.getTheme();
    }

    /**
     * Draws MUI2's check box for an on/off value in the current color, so a faded color fades it.
     *
     * @param x    left edge
     * @param y    top edge
     * @param size width and height in pixels, {@link #CHECKBOX_SIZE} for a check box
     * @param on   whether the box is checked
     */
    public static void drawCheckbox(int x, int y, int size, boolean on) {
        AE2Draw.drawBlended(on ? GuiTextures.CHECK_BOX_FULL : GuiTextures.CHECK_BOX_EMPTY, x, y, size, size);
    }

    /**
     * Draws a check box followed by a one-line label cut to the width, the plain layout of check boxes and radio
     * buttons; the box fades and the label turns grey while unavailable.
     *
     * @param label     label text
     * @param x         left edge of the box
     * @param y         top edge of the box
     * @param width     width the box and label may use, in pixels
     * @param size      width and height of the box in pixels
     * @param labelX    pixels from the box's left edge to the label
     * @param on        whether the box is checked
     * @param available whether the control can be used
     */
    public void drawCheckbox(String label, int x, int y, int width, int size, int labelX, boolean on,
        boolean available) {
        GlStateManager.color(1, 1, 1, available ? 1 : INACTIVE_ALPHA);
        drawCheckbox(x, y, size, on);
        GlStateManager.color(1, 1, 1, 1);
        String fitted = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, Math.max(0, width - labelX));
        drawLabel(fitted, x + labelX, y + (size - LABEL_HEIGHT) / 2, available);
    }

    /**
     * Draws the creative-inventory scroll bar: a sunken track {@link #SCROLLBAR_WIDTH} pixels wide and Minecraft's
     * 12-by-15 handle.
     *
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height
     * @param handleY handle offset from the inner track top
     * @param enabled whether to draw the enabled handle
     */
    public static void drawScrollbar(int x, int y, int height, int handleY, boolean enabled) {
        GlStateManager.color(1, 1, 1, 1);
        drawWell(x, y, SCROLLBAR_WIDTH, height);
        GlStateManager.color(1, 1, 1, 1);
        (enabled ? SCROLLER : SCROLLER_DISABLED).draw(x + 1, y + 1 + handleY, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }

    /**
     * Draws the small scroll bar: a sunken track {@link #SMALL_SCROLLBAR_WIDTH} pixels wide and Minecraft's handle
     * narrowed to five pixels, keeping its edges and one column of its grip.
     *
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height
     * @param handleY handle offset from the inner track top
     * @param enabled whether to draw the enabled handle
     */
    public static void drawSmallScrollbar(int x, int y, int height, int handleY, boolean enabled) {
        GlStateManager.color(1, 1, 1, 1);
        drawWell(x, y, SMALL_SCROLLBAR_WIDTH, height);
        GlStateManager.color(1, 1, 1, 1);
        UITexture[] handle = enabled ? SLIM : SLIM_DISABLED;
        int left = x + 1, top = y + 1 + handleY;
        handle[0].draw(left, top, SLIM_EDGE, SCROLLER_HEIGHT);
        handle[1].draw(left + SLIM_EDGE, top, SLIM_GRIP, SCROLLER_HEIGHT);
        handle[2].draw(left + SLIM_EDGE + SLIM_GRIP, top, SLIM_EDGE, SCROLLER_HEIGHT);
    }

    private static UITexture[] slim(int u) {
        return new UITexture[] { scroller(u, 0, SLIM_EDGE), scroller(u, SLIM_EDGE, SLIM_GRIP),
            scroller(u, SLIM_RIGHT, SLIM_EDGE) };
    }

    private static UITexture scroller(int u, int offset, int width) {
        return UITexture.builder()
            .location(CREATIVE_TABS)
            .imageSize(SHEET, SHEET)
            .subAreaXYWH(u + offset, 0, width, SCROLLER_HEIGHT)
            .build();
    }
}
