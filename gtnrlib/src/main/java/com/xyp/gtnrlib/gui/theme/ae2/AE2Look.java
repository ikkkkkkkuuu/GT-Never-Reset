package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.BIG_SCROLLER_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_WIDTH;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.JsonHelper;
import com.google.gson.JsonObject;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.ButtonText;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

/**
 * The look GTNRLib's AE2-styled widgets take in one panel: one of the three AE2 skins, or the plain look every other
 * theme gets. Widgets ask {@link #of(IWidget)} while they draw, so a panel with a theme of its own, such as a dialog
 * over an AE2 screen, gets the look of its own theme rather than the screen's.
 * <p>
 * A look is the one way to say which look to draw. Everything that exists in every look is asked of the look itself:
 * colors, wells, list rows, buttons, check boxes and both scroll bars. Parts only AE2 has, such as the toolbar strip or
 * tables, take the skin from {@link #variant()}, and the few layouts that differ outside AE2 themes take the
 * plain rules from {@link #plainLook()}.
 * <p>
 * A theme names its AE2 skin in the {@link #KEY} entry of its theme data. The AE2 themes write that entry when they are
 * registered, and themes whose parent is an AE2 theme inherit it. A theme without the entry is recognised by its
 * identifier, so the AE2 themes keep their look even when the entry is missing.
 * <p>
 * The class loads on both sides. Resolving a look from a widget, a context or a theme, and every drawing method, is
 * meant for the client, while the widget is part of an open panel.
 */
public class AE2Look {

    private static final Variant[] VARIANTS = Variant.values();
    /** Theme data entry that names the AE2 skin of a theme; themes without it get the plain look. */
    public static final WidgetThemeKey<Marker> KEY = Marker.register();
    /** Pixels a list well's border takes on each side, under every look. */
    public static final int WELL_EDGE = 1;
    private static final int LIGHT_SHADE = 0x18000000, DARK_SHADE = 0x18FFFFFF;
    private static final AE2Look PLAIN = new AE2Look(null, new PlainLook(null));
    private static final AE2Look[] SKINNED = skinned();
    private static AE2Look lastPlain = PLAIN;

    private final @Nullable Variant variant;
    private final @Nullable PlainLook plain;

    private AE2Look(@Nullable Variant variant, @Nullable PlainLook plain) {
        this.variant = variant;
        this.plain = plain;
    }

    /**
     * Returns the plain look in Minecraft's own text and button styles, for drawing without a theme.
     *
     * @return the shared plain look
     */
    public static AE2Look plain() {
        return PLAIN;
    }

    /**
     * Returns the look of an AE2 skin.
     *
     * @param variant AE2 skin, or {@code null} for the plain look without a theme
     * @return the shared look of that skin
     */
    public static AE2Look of(@Nullable Variant variant) {
        return variant == null ? PLAIN : SKINNED[variant.ordinal()];
    }

    /**
     * Resolves the look of the panel a widget belongs to, from that panel's theme. This is how widgets and drawables
     * find their look.
     *
     * @param widget widget that is part of an open panel
     * @return the panel's look, or the plain look for a widget that is not in a panel yet
     */
    public static AE2Look of(IWidget widget) {
        return widget.isValid() ? of(
            widget.getPanel()
                .getTheme())
            : PLAIN;
    }

    /**
     * Resolves the look of the whole screen being drawn. Only for drawing code that has no widget at hand: a panel can
     * use a theme other than its screen's, which {@link #of(IWidget)} follows and this does not.
     *
     * @param context current drawing context
     * @return the screen's look, or the plain look outside modular screens
     */
    public static AE2Look of(GuiContext context) {
        return context instanceof ModularGuiContext modular ? of(modular.getTheme()) : PLAIN;
    }

    /**
     * Resolves the look a theme gives: the skin named in its {@link #KEY} entry, else the AE2 skin registered under its
     * identifier, else the plain look in that theme's text and button styles.
     *
     * @param theme theme to read, possibly {@code null}
     * @return the theme's look
     */
    public static AE2Look of(@Nullable ITheme theme) {
        if (theme == null) return PLAIN;
        WidgetThemeEntry<Marker> entry = theme.getWidgetTheme(KEY);
        Variant marked = entry == null ? null
            : entry.getTheme()
                .variant();
        Variant skin = marked != null ? marked : AE2Theme.variant(theme.getId());
        if (skin != null) return SKINNED[skin.ordinal()];
        AE2Look cached = lastPlain;
        if (!cached.plain.reads(theme)) lastPlain = cached = new AE2Look(null, new PlainLook(theme));
        return cached;
    }

    /**
     * Finds the AE2 skin stored under a texture folder name.
     *
     * @param folder skin folder name below {@code textures/gui/skin/}, such as {@code ae2_dark}
     * @return the matching skin, or {@code null} for an empty or unknown name
     */
    public static @Nullable Variant skin(@Nullable String folder) {
        for (Variant variant : VARIANTS) if (variant.folder.equals(folder)) return variant;
        return null;
    }

    /**
     * Returns the AE2 skin of this look, for the parts only AE2 has.
     *
     * @return the skin, or {@code null} for the plain look
     */
    public @Nullable Variant variant() {
        return variant;
    }

    /**
     * Returns the plain rules of this look, for layouts that differ outside AE2 themes.
     *
     * @return the plain look, or {@code null} under an AE2 skin
     */
    public @Nullable PlainLook plainLook() {
        return plain;
    }

    /**
     * Reports whether this is one of the AE2 skins rather than the plain look.
     *
     * @return {@code true} under an AE2 skin
     */
    public boolean isAE2() {
        return variant != null;
    }

    /**
     * Draws a list well: AE2's inset under an AE2 skin, Minecraft's sunken slot box (grey fill, dark top and left
     * edges, white bottom and right edges) in the plain look. Rows inside start {@link #WELL_EDGE} pixels in. Client
     * only.
     *
     * @param x      left edge
     * @param y      top edge
     * @param width  well width in pixels
     * @param height well height in pixels
     */
    public void drawWell(int x, int y, int width, int height) {
        if (variant == null) {
            PlainLook.drawWell(x, y, width, height);
            return;
        }
        GlStateManager.color(1, 1, 1, 1);
        variant.inset.draw(x, y, width, height);
    }

    /**
     * Returns the color of text in a list row: the skin's text color, or white in the plain look.
     *
     * @return ARGB color
     */
    public int listText() {
        return variant == null ? PlainLook.LIST_TEXT : variant.colors.text();
    }

    /**
     * Returns the color of a list row's detail on its right: the skin's secondary text color, or white in the plain
     * look.
     *
     * @return ARGB color
     */
    public int listDetail() {
        return variant == null ? PlainLook.LIST_TEXT : variant.colors.secondary();
    }

    /**
     * Returns the color of section headers and of rows that cannot be used, drawn without a shadow: the skin's muted
     * color, or Minecraft's dark grey in the plain look.
     *
     * @return ARGB color
     */
    public int listMuted() {
        return variant == null ? PlainLook.MINECRAFT_TEXT : variant.colors.muted();
    }

    /**
     * Reports whether list text other than headers and unavailable rows is drawn with a shadow, as Minecraft draws
     * light text on its grey slots; only the plain look does.
     *
     * @return {@code true} in the plain look
     */
    public boolean listShadow() {
        return variant == null;
    }

    /**
     * Draws the background of a selected list row: AE2's selected row under an AE2 skin, a dark grey bar in the plain
     * look. Client only.
     *
     * @param x      left edge
     * @param y      top edge
     * @param width  row width in pixels
     * @param height row height in pixels
     */
    public void drawSelectedRow(int x, int y, int width, int height) {
        if (variant == null) {
            GuiDraw.drawRect(x, y, width, height, PlainLook.SELECTED_ROW);
            return;
        }
        // Text drawn before, such as the row above, leaves its color set, which would tint the sprite.
        GlStateManager.color(1, 1, 1, 1);
        variant.listRowSelected.draw(x, y, width, height);
    }

    /**
     * Returns the color of text on a selected row: the skin's text-on-selection color, or white in the plain look.
     *
     * @return ARGB color
     */
    public int selectedText() {
        return variant == null ? PlainLook.LIST_TEXT : variant.highlightText;
    }

    /**
     * Returns the color laid over a list row under the mouse: a faint shade that darkens a light skin and lightens a
     * dark one, or a faint white in the plain look.
     *
     * @return ARGB color
     */
    public int hover() {
        if (variant == null) return PlainLook.ROW_HOVER;
        return variant.dark ? DARK_SHADE : LIGHT_SHADE;
    }

    /**
     * Returns the color laid under nested rows, such as the options a list dropdown opens.
     *
     * @return ARGB color
     */
    public int nested() {
        return variant == null ? PlainLook.NESTED_ROWS : hover();
    }

    /**
     * Returns the color of a row marked good, such as enough of a material.
     *
     * @return ARGB color
     */
    public int good() {
        return variant == null ? PlainLook.GOOD : variant.colors.good();
    }

    /**
     * Returns the color of a row marked bad, such as too little of a material.
     *
     * @return ARGB color
     */
    public int bad() {
        return variant == null ? PlainLook.BAD : variant.colors.bad();
    }

    /**
     * Returns the color of invalid text in an input field: AE2's input error color, or Minecraft's red in the plain
     * look.
     *
     * @return ARGB color
     */
    public int error() {
        return variant == null ? PlainLook.INPUT_ERROR : variant.colors.error();
    }

    /**
     * Returns the 16-by-16 warning sprite shown beside invalid input: the skin's, or the light skin's in the plain
     * look.
     *
     * @return warning sprite
     */
    public UITexture invalidIcon() {
        return (variant == null ? Variant.LIGHT : variant).invalid;
    }

    /**
     * Returns the color of text drawn on the panel: the skin's text color, or its muted color for something
     * unavailable; in the plain look the theme's text color, or MUI2's disabled grey.
     *
     * @param available whether the text belongs to something that can be used
     * @return ARGB text color
     */
    public int panelText(boolean available) {
        if (variant == null) return plain.textColor(available);
        return available ? variant.colors.text() : variant.colors.muted();
    }

    /**
     * Resolves the text style of a button: AE2's button text color at the height AE2 draws it, without a shadow, or the
     * theme's button text style, centred, in the plain look.
     *
     * @param state  visual button state
     * @param height button height in pixels
     * @return text color, vertical offset and shadow setting
     */
    public ButtonText buttonText(State state, int height) {
        if (variant == null) return plain.buttonText(state, height);
        return new ButtonText(variant.colors.button(state), AE2ButtonDrawable.textY(state, height), false);
    }

    /**
     * Draws a button background: AE2's button under an AE2 skin, the theme's MUI2 button in the plain look. Client
     * only.
     *
     * @param context current drawing context
     * @param state   visual button state
     * @param x       left edge
     * @param y       top edge
     * @param width   width in pixels
     * @param height  height in pixels
     */
    public void drawButton(GuiContext context, State state, int x, int y, int width, int height) {
        if (variant != null) AE2ButtonDrawable.draw(variant.folder, state, x, y, width, height);
        else plain.drawButton(context, state, x, y, width, height);
    }

    /**
     * Returns the height of a toolbar icon button: AE2's {@link AE2Theme#ICON_BUTTON_HEIGHT} under an AE2 skin, whose
     * bottom pixels are the button's shadow, and a square {@link PlainLook#ICON_BUTTON_SIZE} in the plain look, as
     * Minecraft's button has no such shadow. It is {@link AE2Theme#ICON_BUTTON_WIDTH} wide in both.
     *
     * @return height in pixels
     */
    public int iconButtonHeight() {
        return variant == null ? PlainLook.ICON_BUTTON_SIZE : AE2Theme.ICON_BUTTON_HEIGHT;
    }

    /**
     * Returns the width of the check box {@link #drawCheckbox} draws.
     *
     * @return {@link AE2Theme#CHECKBOX_WIDTH} under an AE2 skin, {@link PlainLook#CHECKBOX_SIZE} in the plain look
     */
    public int checkboxWidth() {
        return variant == null ? PlainLook.CHECKBOX_SIZE : CHECKBOX_WIDTH;
    }

    /**
     * Draws the check box of an on/off value in the current color: AE2's 22-by-12 check box sprite under an AE2 skin,
     * MUI2's check box in the plain look. List rows and check box drawables both use it, so an on/off value looks the
     * same everywhere in a look. Client only.
     *
     * @param x       left edge
     * @param y       top edge
     * @param on      whether the box is checked
     * @param hovered whether the mouse is over it; only AE2's sprites show it
     * @return width of the drawn check box, as {@link #checkboxWidth()}
     */
    public int drawCheckbox(int x, int y, boolean on, boolean hovered) {
        if (variant == null) PlainLook.drawCheckbox(x, y, PlainLook.CHECKBOX_SIZE, on);
        else variant.checkboxSprite(on, hovered)
            .draw(x, y, CHECKBOX_WIDTH, CHECKBOX_HEIGHT);
        return checkboxWidth();
    }

    /**
     * Returns the width a large scroll bar takes, including its track border.
     *
     * @return 12 pixels under an AE2 skin, 14 for the creative-inventory scroll bar of the plain look
     */
    public int scrollbarWidth() {
        return variant == null ? PlainLook.SCROLLBAR_WIDTH : BIG_SCROLLER_WIDTH;
    }

    /**
     * Draws a large scroll bar: AE2's groove and fixed 12-by-15 handle under an AE2 skin, the creative-inventory scroll
     * bar in the plain look. Client only.
     *
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner track top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     */
    public void drawScrollbar(int x, int y, int height, int handleY, boolean enabled) {
        if (variant != null) AE2ScrollbarPainter.drawScrollbar(variant, x, y, height, handleY, enabled);
        else PlainLook.drawScrollbar(x, y, height, handleY, enabled);
    }

    /**
     * Draws a small scroll bar on the client, {@link AE2Theme#SMALL_SCROLLER_WIDTH} pixels wide: AE2's small groove and
     * 7-by-15 handle under an AE2 skin, a sunken track with Minecraft's handle narrowed to five pixels in the plain
     * look.
     *
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner track top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     */
    public void drawSmallScrollbar(int x, int y, int height, int handleY, boolean enabled) {
        if (variant != null) AE2ScrollbarPainter.drawSmallScrollbar(variant, x, y, height, handleY, enabled);
        else PlainLook.drawSmallScrollbar(x, y, height, handleY, enabled);
    }

    private static AE2Look[] skinned() {
        AE2Look[] looks = new AE2Look[VARIANTS.length];
        for (Variant variant : VARIANTS) looks[variant.ordinal()] = new AE2Look(variant, null);
        return looks;
    }

    /** Theme data naming the AE2 skin a theme uses. */
    public static class Marker extends WidgetTheme {

        /** JSON key holding the skin folder name. */
        public static final String SKIN = "skin";
        private static final Logger LOG = LogManager.getLogger(GTNRLib.MODID);
        private final String skin;
        private final @Nullable Variant variant;

        /**
         * Creates the entry of a theme.
         *
         * @param skin skin folder name, such as {@code ae2_dark}, or an empty string for the plain look
         */
        public Marker(String skin) {
            super(0, 0, null, Color.WHITE.main, Color.WHITE.main, false, Color.WHITE.main);
            this.skin = skin;
            variant = AE2Look.skin(skin);
        }

        /**
         * Reads the entry of a theme from its theme data, keeping the parent's skin when the data names none.
         *
         * @param parent   entry of the parent theme
         * @param json     this entry's theme data
         * @param fallback the theme's top-level data, or {@code null}
         */
        public Marker(Marker parent, JsonObject json, JsonObject fallback) {
            super(parent, json, fallback);
            skin = JsonHelper.getString(json, parent.skin, SKIN);
            variant = AE2Look.skin(skin);
            if (variant == null && !skin.isEmpty())
                LOG.warn("Theme data names unknown AE2 skin \"{}\"; its widgets get the plain look", skin);
        }

        /**
         * Returns the skin folder name.
         *
         * @return skin folder name, or an empty string for the plain look
         */
        public String skin() {
            return skin;
        }

        /**
         * Returns the AE2 skin the folder name stands for, found once when the theme is read.
         *
         * @return the skin, or {@code null} for the plain look or an unknown name
         */
        public @Nullable Variant variant() {
            return variant;
        }

        private static WidgetThemeKey<Marker> register() {
            Marker plain = new Marker("");
            return IThemeApi.get()
                .widgetThemeKeyBuilder("gtnrlibAE2Look", Marker.class)
                .defaultTheme(plain)
                .defaultHoverTheme(plain)
                .parser(Marker::new)
                .register();
        }

        /** Writes the entry into a theme definition. */
        public static class Builder extends WidgetThemeBuilder<Marker, Builder> {

            /**
             * Names the skin.
             *
             * @param folder skin folder name, such as {@code ae2_dark}
             * @return this builder
             */
            public Builder skin(String folder) {
                add(SKIN, folder);
                return this;
            }
        }
    }
}
