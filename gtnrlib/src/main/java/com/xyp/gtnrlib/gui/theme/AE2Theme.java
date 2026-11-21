package com.xyp.gtnrlib.gui.theme;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.ReloadThemeEvent;
import com.cleanroommc.modularui.theme.SelectableTheme;
import com.cleanroommc.modularui.theme.ThemeAPI;
import com.cleanroommc.modularui.theme.ThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.JsonBuilder;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.gui.theme.ThemeSkin.Part;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Buttons;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Draw;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Legacy;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Panels;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Scrollbars;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Switches;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Upgrades;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;
import com.xyp.gtnrlib.gui.wheel.WheelStyle;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;
import com.xyp.gtnrlib.gui.widget.LayoutTheme;
import com.xyp.gtnrlib.gui.widget.SkinDrawable;
import com.xyp.gtnrlib.gui.widget.SlideSwitch;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The AE2 light, dark and purple dark themes for ModularUI 2: their identifiers, AE2's widget dimensions, the three
 * skins with their colors and textures ({@link Variant}), the built-in toolbar icons ({@link Icon}) and the theme
 * registration.
 * <p>
 * Select a panel theme through {@link ScreenTheme}. Panel backgrounds, fields, slots, switches and button styles
 * are supplied by the registered theme. Use {@link #useMenuTheme(String, String, String, String...)} to map child
 * menus and {@link #sliderHandles(IWidget, String)} to style slider handles.
 * <p>
 * The drawing helpers live in the {@code ae2} package, one class per part: {@link AE2Buttons}, {@link AE2Switches},
 * {@link AE2Scrollbars}, {@link AE2Panels}, {@link AE2Upgrades}, {@link AE2Legacy} and {@link AE2Draw}. Widgets ask
 * {@link AE2Look#of(IWidget)} for the skin of the panel they are in. The drawing methods still declared here forward
 * to those classes and are deprecated.
 * <p>
 * The class loads on both sides: constants, {@link Variant} lookups and drawable factories may be used while a GUI is
 * built on the server, as ModularUI 2 builds GUIs opened by the server on both sides. Drawing, measuring text and
 * theme registration run on the client only.
 *
 * @see Variant
 * @see HandleScrollData
 */
public class AE2Theme {

    /** Light panel theme identifier. */
    public static final String LIGHT_ID = "gtnrlib:ae2_light";
    /** Light context-menu theme identifier. */
    public static final String LIGHT_MENU_ID = "gtnrlib:ae2_light_menu";
    /** Dark panel theme identifier. */
    public static final String DARK_ID = "gtnrlib:ae2_dark";
    /** Dark context-menu theme identifier. */
    public static final String DARK_MENU_ID = "gtnrlib:ae2_dark_menu";
    /** Dark panel theme identifier with purple selection highlights. */
    public static final String DARK_PURPLE_ID = "gtnrlib:ae2_dark_purple";
    /** Dark context-menu theme identifier with purple selection highlights. */
    public static final String DARK_PURPLE_MENU_ID = "gtnrlib:ae2_dark_purple_menu";
    /** MUI2 context-menu theme used outside AE2 panels. */
    public static final String CONTEXT_MENU_ID = "modularui.context_menu";
    /** ARGB light-blue selection fill, top edge and shadow of the light and dark variants. */
    public static final int HIGHLIGHT = 0xFF9CD3FF, HIGHLIGHT_TOP = 0xFFDAFFFF, HIGHLIGHT_SHADOW = 0xFF708CBA,
        FIELD_SELECTION = 0xFF0000FF;
    /** ARGB fluix-purple selection fill, top edge and shadow of the purple dark variant. */
    public static final int PURPLE_HIGHLIGHT = 0xFF644595, PURPLE_HIGHLIGHT_TOP = 0xFFE2A3E3,
        PURPLE_HIGHLIGHT_SHADOW = 0xFF212040;
    /** ARGB text color of a text field that cannot be edited, as Minecraft draws uneditable fields. */
    public static final int FIELD_DISABLED_TEXT = 0xFF707070;
    /**
     * Widget dimensions, label offsets, border widths and layout spacing in GUI pixels; {@code UPGRADE_ROWS} is a
     * count.
     */
    public static final int BUTTON_HEIGHT = 20, TEXT_FIELD_HEIGHT = 12, ICON_SIZE = 16, ICON_BUTTON_WIDTH = 18,
        ICON_BUTTON_HEIGHT = 20, TAB_BUTTON_SIZE = 20, SWITCH_WIDTH = 22, SWITCH_HEIGHT = 12, SWITCH_LABEL_X = 26,
        SWITCH_LABEL_Y = 4, SCROLLBAR_WIDTH = 7, INSET_TOP = 3, INSET_EDGE = 1, BIG_SCROLLER_WIDTH = 12,
        SCROLLER_HEIGHT = 15, RADIO_SIZE = 14, RADIO_LABEL_X = 16, RADIO_LABEL_Y = 4, LIST_ROW_HEIGHT = 22,
        PROGRESS_WIDTH = 6, PROGRESS_HEIGHT = 18, BUTTON_BAR_MARGIN = 2, BUTTON_BAR_SPACING = 6, UPGRADE_PADDING = 5,
        UPGRADE_ROWS = 8, VALIDATION_ICON_SIZE = 8, PANEL_EDGE = 2, PANEL_BOTTOM_EDGE = 4;
    /**
     * AE2 page tabs: size, distance between tab tops (neighbours share an outline row) and how far a tab starts inside
     * the panel's right edge, which its left columns cover so the selected tab joins the panel.
     */
    public static final int PAGE_TAB_SIZE = 22, PAGE_TAB_PITCH = 21, PAGE_TAB_INSET = 3;
    /** How far an upgrade panel starts inside the right edge of the panel it belongs to, as AE2 places it. */
    public static final int UPGRADE_OVERLAP = 2;
    /** Size of the dropdown triangle drawn by {@link AE2Panels#drawCaret}. */
    public static final int CARET_WIDTH = 5, CARET_HEIGHT = 3;
    /**
     * Size of AE2's square sprite sheets, the 18-pixel item slot, the space kept left and right of a button label and
     * the height of one line of label text, in GUI pixels.
     */
    public static final int SHEET = 256, SLOT_SIZE = 18, LABEL_PADDING = 2, LABEL_HEIGHT = 8;
    /** ARGB text color of disabled buttons and unavailable text outside the AE2 themes, MUI2's disabled grey. */
    public static final int DISABLED_TEXT = 0xFFA0A0A0;
    /** Alpha of sprites and icons drawn for something that cannot be used. */
    public static final float INACTIVE_ALPHA = 0.5F;
    private static final String ROOT = "textures/gui/skin/";
    private static final int PAGE_TAB_U = 128, PAGE_TAB_V = 128;
    private static final int PANEL_SIZE = 256, INSET_WIDTH = 8, INSET_HEIGHT = 9, FIELD_WIDTH = 128, SWITCH_SHEET = 64,
        TAB_BUTTON_U = 160, TAB_BUTTON_V = 192, TAB_HOVER_V = 224, TAB_BORDER_LEFT = 2, PAGE_TAB_BORDER_LEFT = 3,
        TRACK_WIDTH = 8, TRACK_HEIGHT = 6, FILL_SIZE = 4, TAG_WIDTH = 6, TAG_HEIGHT = 9, DIVIDER_WIDTH = 4,
        DIVIDER_HEIGHT = 2, WHEEL_DISC_ALPHA = 0xE6000000, LIST_ROW_WIDTH = 67, BUTTON_BAR_WIDTH = 21,
        BUTTON_BAR_HEIGHT = 26, RADIO_X = 28, INVALID_Y = 128;
    // IDrawable.EMPTY is saved as JSON null, which the theme loader reads as unset and replaces with the parent
    // theme's framed button; a transparent rectangle keeps menu options flat.
    private static final IDrawable FLAT_OPTION = new Rectangle().color(0);
    /** Shared sizing and spacing used by both AE2 variants. */
    public static final ThemeStyle STYLE = new ThemeStyle(
        ThemeStyle.DEFAULT.cornerLength(),
        ThemeStyle.DEFAULT.cornerThickness(),
        ThemeStyle.DEFAULT.cardAccentTop(),
        ThemeStyle.DEFAULT.cardAccentWidth(),
        ThemeStyle.DEFAULT.cardAccentHeight(),
        2,
        4,
        8,
        8,
        BUTTON_HEIGHT,
        SLOT_SIZE,
        1F);

    /**
     * Registers fresh definitions for both panel themes and their menu themes.
     * <p>
     * GTNRLib installs the themes during client initialization and invokes this method on theme reload. Consumers
     * select a theme identifier without registering these definitions themselves.
     */
    @SideOnly(Side.CLIENT)
    public static void register() {
        for (Variant variant : Variant.values()) {
            replace(variant.id, variant.theme());
            replace(variant.menuId, variant.menu());
        }
    }

    private static void replace(String id, ThemeBuilder<?> theme) {
        // ThemeManager writes "background": "none" into the registered json on every reload, so register a fresh copy.
        IThemeApi.get()
            .getJavaDefaultThemes(id)
            .clear();
        IThemeApi.get()
            .registerTheme(theme);
    }

    /**
     * Resolves a texture below GTNRLib's GUI skin directory.
     *
     * @param folder skin folder, such as {@code ae2_light}
     * @param name   texture filename without the {@code .png} extension
     * @return texture resource location
     */
    public static ResourceLocation texture(String folder, String name) {
        return new ResourceLocation(GTNRLib.MODID, ROOT + folder + "/" + name + ".png");
    }

    /**
     * Finds the variant associated with a panel or menu theme identifier.
     *
     * @param themeId theme identifier, or {@code null} for the default theme
     * @return the matching variant, or {@code null} for an unrecognized identifier
     */
    public static @Nullable Variant variant(@Nullable String themeId) {
        for (Variant variant : Variant.values())
            if (variant.id.equals(themeId) || variant.menuId.equals(themeId)) return variant;
        return null;
    }

    /**
     * Reports whether an identifier names one of the AE2 panel or menu themes.
     *
     * @param themeId theme identifier, possibly {@code null}
     * @return {@code true} for an AE2 theme identifier
     */
    public static boolean isAE2(@Nullable String themeId) {
        return variant(themeId) != null;
    }

    /**
     * Resolves the variant currently used by a modular GUI context, as {@link AE2Look#of(GuiContext)} does. A panel can
     * use a theme other than its screen's; {@link AE2Look#of(IWidget)} resolves the panel's.
     *
     * @param context current drawing context
     * @return the active variant, or {@code null} for another theme or a non-modular context
     */
    public static @Nullable Variant active(GuiContext context) {
        return AE2Look.of(context)
            .variant();
    }

    /**
     * Maps named child menu panels to the menu theme matching their parent screen.
     *
     * @param screenTheme parent theme identifier, or {@code null} for the default theme
     * @param owner       owning mod identifier
     * @param screen      registered screen name
     * @param menus       child panel names within the screen
     */
    public static void useMenuTheme(@Nullable String screenTheme, String owner, String screen, String... menus) {
        Variant variant = variant(screenTheme);
        String menuTheme = variant == null ? CONTEXT_MENU_ID : variant.menuId;
        for (String menu : menus) IThemeApi.get()
            .registerThemeForScreen(owner + ":" + screen + ":" + menu, menuTheme);
    }

    /**
     * Applies an AE2 handle texture to sliders in a widget tree.
     * <p>
     * Only children already present in the tree are visited. An unrecognized theme identifier leaves handles
     * unchanged.
     *
     * @param root    root widget to visit, including its descendants
     * @param themeId panel or menu theme identifier, possibly {@code null}
     */
    public static void sliderHandles(IWidget root, @Nullable String themeId) {
        Variant variant = variant(themeId);
        if (variant != null) sliderHandles(root, new AE2ButtonDrawable(State.NORMAL, variant.folder));
    }

    private static void sliderHandles(IWidget widget, IDrawable handle) {
        if (widget instanceof SliderWidget slider) slider.sliderTexture(handle);
        for (IWidget child : widget.getChildren()) sliderHandles(child, handle);
    }

    private static SkinDrawable skin(String folder, String name, int width, int height) {
        return new SkinDrawable().texture(texture(folder, name), width, height);
    }

    private static UITexture adaptable(String folder, String name, int width, int height, int left, int top, int right,
        int bottom) {
        return UITexture.builder()
            .location(texture(folder, name))
            .imageSize(width, height)
            .adaptable(left, top, right, bottom)
            .name("gtnrlib_" + folder + "_" + name)
            .build();
    }

    private static Colors darkColors(int hoverText) {
        return new Colors(
            0xFFAAAAAA,
            0xFF7E808E,
            0xFF9496A4,
            0xFFE0E0E0,
            hoverText,
            0xFF5E6070,
            0xFFE0E0E0,
            0xFF6B6E7E,
            0xFF14151D,
            0xFF8E8E8E,
            0xFF42434B,
            0xFF242631,
            0xFF0A090E,
            0xFF2D2F3C,
            0xFF5FCB79,
            0xFFE06A60,
            0xFF6CB4F0,
            0xFFE0A54A,
            0xFFFF5A3C);
    }

    private static UITexture frame(String folder, String name, int x, int y) {
        return UITexture.builder()
            .location(texture(folder, "states"))
            .imageSize(SHEET, SHEET)
            .subAreaXYWH(x, y, ICON_BUTTON_WIDTH, ICON_BUTTON_HEIGHT)
            .adaptable(2, 2, 2, 1)
            .name("gtnrlib_" + folder + "_" + name)
            .build();
    }

    private static UITexture image(String folder, String name, int width, int height) {
        return UITexture.builder()
            .location(texture(folder, name))
            .imageSize(width, height)
            .nonOpaque()
            .name("gtnrlib_" + folder + "_" + name)
            .build();
    }

    // Why: tab sprites are a frame around a flat fill, two pixels at the top and four at the bottom with the shadow, so
    // they stretch to any size; the page tab's three left columns are the panel border it covers.
    private static UITexture tabSprite(String folder, String name, int x, int y, int size, int left) {
        return UITexture.builder()
            .location(texture(folder, "states"))
            .imageSize(SHEET, SHEET)
            .subAreaXYWH(x, y, size, size)
            .adaptable(left, 2, 2, 4)
            .nonOpaque()
            .name("gtnrlib_" + folder + "_" + name)
            .build();
    }

    private static UITexture sprite(String folder, String sheet, int size, int x, int y, int width, int height) {
        return UITexture.builder()
            .location(texture(folder, sheet))
            .imageSize(size, size)
            .subAreaXYWH(x, y, width, height)
            .nonOpaque()
            .build();
    }

    /**
     * Same as {@link AE2Legacy#legacyStates}.
     *
     * @param variant recolored variant, or {@code null} for the resource supplied by AE2
     * @return state sheet resource location
     * @deprecated use {@link AE2Legacy#legacyStates}
     */
    @Deprecated
    public static ResourceLocation legacyStates(@Nullable Variant variant) {
        return AE2Legacy.legacyStates(variant);
    }

    /**
     * Same as {@link AE2Legacy#legacyTerminal}.
     *
     * @param variant recolored variant, or {@code null} for the resource supplied by AE2
     * @return terminal sheet resource location
     * @deprecated use {@link AE2Legacy#legacyTerminal}
     */
    @Deprecated
    public static ResourceLocation legacyTerminal(@Nullable Variant variant) {
        return AE2Legacy.legacyTerminal(variant);
    }

    /**
     * Same as {@link AE2Scrollbars#scrollbarWidth}.
     *
     * @param variant AE2 variant, or {@code null} for the vanilla creative-inventory style
     * @return 12 pixels for AE2 or 14 pixels for vanilla
     * @deprecated use {@link AE2Scrollbars#scrollbarWidth}
     */
    @Deprecated
    public static int scrollbarWidth(@Nullable Variant variant) {
        return AE2Scrollbars.scrollbarWidth(variant);
    }

    /**
     * Same as {@link AE2Scrollbars#drawScrollbar}.
     *
     * @param variant AE2 variant, or {@code null} for the vanilla creative-inventory style
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner track top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     * @deprecated use {@link AE2Scrollbars#drawScrollbar}
     */
    @Deprecated
    public static void drawScrollbar(@Nullable Variant variant, int x, int y, int height, int handleY,
        boolean enabled) {
        AE2Scrollbars.drawScrollbar(variant, x, y, height, handleY, enabled);
    }

    /**
     * Same as {@link AE2Scrollbars#drawSmallScrollbar}.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the handle; the groove starts one pixel to its right
     * @param y       top edge of the groove, including its one-pixel border
     * @param height  total groove height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner groove top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     * @deprecated use {@link AE2Scrollbars#drawSmallScrollbar}
     */
    @Deprecated
    public static void drawSmallScrollbar(Variant variant, int x, int y, int height, int handleY, boolean enabled) {
        AE2Scrollbars.drawSmallScrollbar(variant, x, y, height, handleY, enabled);
    }

    /**
     * Same as {@link AE2Panels#drawButtonBar}.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the background
     * @param y       top edge of the background
     * @param width   background width, including button margins
     * @param height  background height, including button margins and spacing
     * @deprecated use {@link AE2Panels#drawButtonBar}
     */
    @Deprecated
    public static void drawButtonBar(Variant variant, int x, int y, int width, int height) {
        AE2Panels.drawButtonBar(variant, x, y, width, height);
    }

    /**
     * Same as {@link AE2Upgrades#upgradePanelWidth}.
     *
     * @param slots nonnegative slot count; zero still reserves one column
     * @return panel width in pixels, 23 for one column
     * @deprecated use {@link AE2Upgrades#upgradePanelWidth}
     */
    @Deprecated
    public static int upgradePanelWidth(int slots) {
        return AE2Upgrades.upgradePanelWidth(slots);
    }

    /**
     * Same as {@link AE2Upgrades#upgradePanelHeight}.
     *
     * @param slots nonnegative slot count; zero still reserves one row
     * @return panel height in pixels
     * @deprecated use {@link AE2Upgrades#upgradePanelHeight}
     */
    @Deprecated
    public static int upgradePanelHeight(int slots) {
        return AE2Upgrades.upgradePanelHeight(slots);
    }

    /**
     * Same as {@link AE2Upgrades#drawUpgradePanel}.
     *
     * @param variant non-null AE2 variant
     * @param x       panel left edge
     * @param y       panel top edge
     * @param slots   nonnegative number of slots to draw
     * @deprecated use {@link AE2Upgrades#drawUpgradePanel}
     */
    @Deprecated
    public static void drawUpgradePanel(Variant variant, int x, int y, int slots) {
        AE2Upgrades.drawUpgradePanel(variant, x, y, slots);
    }

    /**
     * Same as {@link AE2Panels#tableSize}.
     *
     * @param cells    positive number of cells along the axis
     * @param cellSize positive inner size of each cell in pixels
     * @return total dimension in pixels
     * @deprecated use {@link AE2Panels#tableSize}
     */
    @Deprecated
    public static int tableSize(int cells, int cellSize) {
        return AE2Panels.tableSize(cells, cellSize);
    }

    /**
     * Same as {@link AE2Panels#drawTable}.
     *
     * @param variant    non-null AE2 variant
     * @param x          table left edge
     * @param y          table top edge
     * @param columns    positive column count
     * @param rows       positive row count
     * @param cellWidth  positive inner cell width
     * @param cellHeight positive inner cell height
     * @deprecated use {@link AE2Panels#drawTable}
     */
    @Deprecated
    public static void drawTable(Variant variant, int x, int y, int columns, int rows, int cellWidth, int cellHeight) {
        AE2Panels.drawTable(variant, x, y, columns, rows, cellWidth, cellHeight);
    }

    /**
     * Same as {@link AE2Panels#listRow}.
     *
     * @param selected selection supplier; a hover supplier can be used for hover highlighting
     * @return an AE2 nine-slice row or the current MUI2 button background
     * @deprecated use {@link AE2Panels#listRow}
     */
    @Deprecated
    public static IDrawable listRow(BooleanSupplier selected) {
        return AE2Panels.listRow(selected);
    }

    /**
     * Same as {@link AE2Switches#radio}.
     *
     * @param widget    widget supplying hover state
     * @param selected  supplier of the selected state
     * @param label     supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @return the indicator and label drawable
     * @deprecated use {@link AE2Switches#radio}
     */
    @Deprecated
    public static IDrawable radio(Widget<?> widget, BooleanSupplier selected, Supplier<String> label,
        BooleanSupplier available) {
        return AE2Switches.radio(widget, selected, label, available);
    }

    /**
     * Same as {@link AE2Switches#radioWidth}.
     *
     * @param label display text
     * @return required width in pixels
     * @deprecated use {@link AE2Switches#radioWidth}
     */
    @Deprecated
    public static int radioWidth(String label) {
        return AE2Switches.radioWidth(label);
    }

    /**
     * Same as {@link AE2Legacy#drawLegacyState}.
     *
     * @param variant recolored variant, or {@code null} for the sheet supplied by AE2
     * @param index   zero-based cell index, from zero through 255
     * @param x       destination left edge
     * @param y       destination top edge
     * @param size    destination width and height in pixels
     * @deprecated use {@link AE2Legacy#drawLegacyState}
     */
    @Deprecated
    public static void drawLegacyState(@Nullable Variant variant, int index, float x, float y, float size) {
        AE2Legacy.drawLegacyState(variant, index, x, y, size);
    }

    /**
     * Same as {@link AE2Buttons#button}.
     *
     * @param widget    widget supplying hover state
     * @param available supplier of the visual enabled state; input handling remains the caller's responsibility
     * @param selected  supplier of the selected state, which uses the highlighted background
     * @return background drawable suitable for both normal and hover backgrounds
     * @deprecated use {@link AE2Buttons#button}
     */
    @Deprecated
    public static IDrawable button(Widget<?> widget, BooleanSupplier available, BooleanSupplier selected) {
        return AE2Buttons.button(widget, available, selected);
    }

    /**
     * Same as {@link AE2Buttons#buttonText}.
     *
     * @param context current drawing context
     * @param state   visual button state
     * @param height  button height in pixels
     * @return text color, vertical offset and shadow setting
     * @deprecated use {@link AE2Buttons#buttonText}
     */
    @Deprecated
    public static ButtonText buttonText(GuiContext context, State state, int height) {
        return AE2Buttons.buttonText(context, state, height);
    }

    /**
     * Same as {@link AE2Buttons#label}.
     *
     * @param widget    widget supplying hover state
     * @param text      supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @param selected  supplier of the selected state
     * @return label drawable suitable for a button overlay
     * @deprecated use {@link AE2Buttons#label}
     */
    @Deprecated
    public static IDrawable label(Widget<?> widget, Supplier<String> text, BooleanSupplier available,
        BooleanSupplier selected) {
        return AE2Buttons.label(widget, text, available, selected);
    }

    /**
     * Same as {@link AE2Buttons#iconButton}.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param available supplier of the visual enabled state
     * @return combined background and icon drawable; the standard size is 18 by 20 pixels
     * @deprecated use {@link AE2Buttons#iconButton}
     */
    @Deprecated
    public static IDrawable iconButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return AE2Buttons.iconButton(widget, icon, available);
    }

    /**
     * Same as {@link AE2Buttons#tabButton}.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param available supplier of the visual enabled state
     * @return combined tab background and icon drawable
     * @deprecated use {@link AE2Buttons#tabButton}
     */
    @Deprecated
    public static IDrawable tabButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return AE2Buttons.tabButton(widget, icon, available);
    }

    /**
     * Same as {@link AE2Switches#toggleSwitch}.
     *
     * @param widget    widget supplying hover state
     * @param on        supplier of the switch value
     * @param label     supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @return combined switch and label drawable
     * @deprecated use {@link AE2Switches#toggleSwitch}
     */
    @Deprecated
    public static IDrawable toggleSwitch(Widget<?> widget, BooleanSupplier on, Supplier<String> label,
        BooleanSupplier available) {
        return AE2Switches.toggleSwitch(widget, on, label, available);
    }

    /**
     * Same as {@link AE2Switches#switchWidth}.
     *
     * @param label display text
     * @return required width in pixels
     * @deprecated use {@link AE2Switches#switchWidth}
     */
    @Deprecated
    public static int switchWidth(String label) {
        return AE2Switches.switchWidth(label);
    }

    /**
     * Same as {@link AE2Draw#text}.
     *
     * @param text   display text
     * @param x      text left edge
     * @param y      text top edge
     * @param color  ARGB text color
     * @param shadow whether to draw a drop shadow
     * @deprecated use {@link AE2Draw#text}
     */
    @Deprecated
    public static void text(String text, int x, int y, int color, boolean shadow) {
        AE2Draw.text(text, x, y, color, shadow);
    }

    /**
     * Text style resolved for a button state.
     *
     * @param color  ARGB text color
     * @param y      vertical offset from the button's top edge in pixels
     * @param shadow whether to draw a drop shadow
     */
    public record ButtonText(int color, int y, boolean shadow) {}

    /**
     * Semantic ARGB colors for one AE2 variant.
     *
     * @param text         primary text color
     * @param muted        muted text color
     * @param secondary    secondary text color
     * @param buttonText   normal button text color
     * @param hoverText    highlighted button text color
     * @param disabledText disabled button text color
     * @param fieldText    text-field content color
     * @param placeholder  text-field hint color
     * @param line         separator and grid-line color
     * @param edge         panel edge color
     * @param panel        panel fill color
     * @param well         inset fill color
     * @param outline      outline color
     * @param raised       raised surface color
     * @param good         positive status color
     * @param bad          negative status color
     * @param info         informational status color
     * @param warning      warning status color
     * @param error        input validation error color
     */
    public record Colors(int text, int muted, int secondary, int buttonText, int hoverText, int disabledText,
        int fieldText, int placeholder, int line, int edge, int panel, int well, int outline, int raised, int good,
        int bad, int info, int warning, int error) {

        /**
         * Resolves button text color for a visual state.
         *
         * @param state button state
         * @return ARGB text color
         */
        public int button(State state) {
            return switch (state) {
                case NORMAL -> buttonText;
                case HOVER -> hoverText;
                case DISABLED -> disabledText;
            };
        }
    }

    /** @deprecated Use {@link AE2Panels#panelBackground}. */
    @Deprecated
    public static IDrawable panelBackground(int left, int right) {
        return AE2Panels.panelBackground(left, right);
    }

    /** @deprecated Use {@link AE2Panels#well}. */
    @Deprecated
    public static IDrawable well(int width, int plainColor) {
        return AE2Panels.well(width, plainColor);
    }

    /** @deprecated Use {@link AE2Switches#drawSwitch}. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static int drawSwitch(@Nullable Variant variant, int x, int y, boolean on, boolean hovered) {
        return AE2Switches.drawSwitch(variant, x, y, on, hovered);
    }

    /** @deprecated Use {@link AE2Panels#drawCaret}. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static void drawCaret(int x, int y, boolean open, int color) {
        AE2Panels.drawCaret(x, y, open, color);
    }

    /** @deprecated Use {@link AE2Draw#drawTintedIcon}. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static void drawTintedIcon(Icon icon, int x, int y, int color) {
        AE2Draw.drawTintedIcon(icon, x, y, color);
    }

    /** @deprecated Use {@link AE2Panels#drawWell}. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static void drawWell(@Nullable Variant variant, int x, int y, int width, int height, int plainColor) {
        AE2Panels.drawWell(variant, x, y, width, height, plainColor);
    }

    /**
     * AE2 panel and menu variants with their palettes and textures.
     * <p>
     * Colors and resource names are shared by the registered themes and direct drawing helpers. Raw textures do
     * not select a variant from the drawing context. Each variant's selection colors match the selected sprites of
     * its skin.
     */
    public enum Variant {

        /** Light AE2 palette. */
        LIGHT(LIGHT_ID, LIGHT_MENU_ID, "ae2_light", false,
            new Colors(
                0xFF413F54,
                0xFF878FA5,
                0xFF4A5068,
                0xFFF2F2F2,
                0xFF517497,
                0xFF413F54,
                0xFFF2F2F2,
                0xFFDEDFE3,
                0xFF696D88,
                0xFFF2F2F2,
                0xFFCBCCD4,
                0xFF9A9FB4,
                0xFF413F54,
                0xFFADB0C4,
                0xFF1C6B34,
                0xFF8F2B23,
                0xFF144673,
                0xFF7A4A00,
                0xFFFF1900),
            HIGHLIGHT, HIGHLIGHT_TOP, HIGHLIGHT_SHADOW),
        /** Dark AE2 palette. */
        DARK(DARK_ID, DARK_MENU_ID, "ae2_dark", true, darkColors(0xFF517497), HIGHLIGHT, HIGHLIGHT_TOP,
            HIGHLIGHT_SHADOW),
        /** Dark AE2 palette with fluix-purple selection highlights. */
        DARK_PURPLE(DARK_PURPLE_ID, DARK_PURPLE_MENU_ID, "ae2_dark_purple", true, darkColors(0xFFFFFFFF),
            PURPLE_HIGHLIGHT, PURPLE_HIGHLIGHT_TOP, PURPLE_HIGHLIGHT_SHADOW, 0xFFFFFFFF);

        /** Registered panel theme identifier. */
        public final String id;
        /** Registered context-menu theme identifier. */
        public final String menuId;
        /** Folder name below {@code textures/gui/skin/}. */
        public final String folder;
        /** Whether the panel is dark, so overlays such as a hovered row lighten it rather than darken it. */
        public final boolean dark;
        /** Semantic ARGB colors. */
        public final Colors colors;
        /** ARGB selection fill, top edge and bottom shadow matching this skin's selected sprites. */
        public final int highlight, highlightTop, highlightShadow;
        /** ARGB text color drawn on the selection fill, such as slider value tags. */
        public final int highlightText;
        /** Palette used by GTNRLib's general theme helpers. */
        public final ThemePalette palette;
        /** Skin used by GTNRLib's general theme helpers. */
        public final ThemeSkin skin;
        /** Recolored AE2 rv3 state and terminal sheets. */
        public final ResourceLocation legacyStates, legacyTerminal;
        /** Resizable panel background and inset list well. */
        public final UITexture panel, inset;
        /** The panel texture without its left border, for upgrade panels; see {@link AE2Upgrades#drawUpgradePanel}. */
        public final UITexture upgradeFrame;
        /** Empty item and fluid slot backgrounds, natively 18 by 18 pixels. */
        public final UITexture slotItem, slotFluid;
        /** Small scroll-bar handle, natively 7 by 15 pixels and resizable by MUI2. */
        public final UITexture scroller;
        /** Disabled small scroll-bar handle, 7 by 15 pixels. */
        public final UITexture scrollerDisabled;
        /** Enabled and disabled large scroll-bar handles, 12 by 15 pixels. */
        public final UITexture bigScroller, bigScrollerDisabled;
        /** Resizable text-field background with a native height of 12 pixels. */
        public final UITexture textField;
        /** Dark background of a text field that cannot be edited, with the same borders as {@link #textField}. */
        public final UITexture textFieldDisabled;
        /** Normal, hovered and selected toolbar-button frames, natively 18 by 20 pixels. */
        public final UITexture iconButton, iconButtonHover, iconButtonSelected;
        /** Normal 20-by-20 and hovered 22-by-22 tab-button backgrounds. */
        public final UITexture tabButton, tabButtonHover;
        /** Normal, selected and hovered 22-by-22 page tabs that stick out of a panel's right edge. */
        public final UITexture pageTab, pageTabSelected, pageTabHover;
        /** Unchecked switch sprites in normal and hovered states, 22 by 12 pixels. */
        public final UITexture switchOff, switchOffHover;
        /** Checked switch sprites in normal and hovered states, 22 by 12 pixels. */
        public final UITexture switchOn, switchOnHover;
        /** Unselected radio sprites in normal and hovered states, 14 by 14 pixels. */
        public final UITexture radioOff, radioOffHover;
        /** Selected radio sprites in normal and hovered states, 14 by 14 pixels. */
        public final UITexture radioOn, radioOnHover;
        /** Resizable normal and selected list rows with a native height of 22 pixels. */
        public final UITexture listRow, listRowSelected;
        /** Resizable vertical button-bar background. */
        public final UITexture buttonBar;
        /** Empty and filled inscriber progress textures, 6 by 18 pixels, intended for upward filling. */
        public final UITexture progressEmpty, progressFull;
        /**
         * Validation warning sprite, 16 by 16 pixels and drawn at that size like AE2's; number entries use
         * {@link AE2Theme#VALIDATION_ICON_SIZE} only for its hover area.
         */
        public final UITexture invalid;

        Variant(String id, String menuId, String folder, boolean dark, Colors colors, int highlight, int highlightTop,
            int highlightShadow) {
            this(id, menuId, folder, dark, colors, highlight, highlightTop, highlightShadow, colors.outline);
        }

        Variant(String id, String menuId, String folder, boolean dark, Colors colors, int highlight, int highlightTop,
            int highlightShadow, int highlightText) {
            this.id = id;
            this.menuId = menuId;
            this.folder = folder;
            this.dark = dark;
            this.colors = colors;
            this.highlight = highlight;
            this.highlightTop = highlightTop;
            this.highlightShadow = highlightShadow;
            this.highlightText = highlightText;
            palette = palette(colors, highlight, highlightShadow, highlightText);
            legacyStates = texture(folder, "legacy_states");
            legacyTerminal = texture(folder, "legacy_terminal");
            skin = ThemeSkin.NONE
                .with(
                    Part.PANEL,
                    skin(folder, "panel", PANEL_SIZE, PANEL_SIZE)
                        .border(PANEL_EDGE, PANEL_EDGE, PANEL_EDGE, PANEL_BOTTOM_EDGE))
                .with(
                    Part.CARD,
                    skin(folder, "inset", INSET_WIDTH, INSET_HEIGHT)
                        .border(INSET_EDGE, INSET_TOP, INSET_EDGE, INSET_EDGE))
                .with(Part.SLOT, skin(folder, "slot_item", SLOT_SIZE, SLOT_SIZE).border(1, 1, 1, 1))
                .with(Part.SLIDER_TRACK, skin(folder, "slider_track", TRACK_WIDTH, TRACK_HEIGHT).border(1, 2, 1, 1))
                .with(Part.SLIDER_FILL, skin(folder, "slider_fill", FILL_SIZE, FILL_SIZE).border(0, 1, 0, 0))
                .with(Part.SLIDER_TAG, skin(folder, "slider_tag", TAG_WIDTH, TAG_HEIGHT).border(1, 2, 1, 2))
                .with(Part.DIVIDER, skin(folder, "divider", DIVIDER_WIDTH, DIVIDER_HEIGHT));
            panel = adaptable(
                folder,
                "panel",
                PANEL_SIZE,
                PANEL_SIZE,
                PANEL_EDGE,
                PANEL_EDGE,
                PANEL_EDGE,
                PANEL_BOTTOM_EDGE);
            upgradeFrame = UITexture.builder()
                .location(texture(folder, "panel"))
                .imageSize(PANEL_SIZE, PANEL_SIZE)
                .subAreaXYWH(PANEL_EDGE, 0, PANEL_SIZE - PANEL_EDGE, PANEL_SIZE)
                .adaptable(0, PANEL_EDGE, PANEL_EDGE, PANEL_BOTTOM_EDGE)
                .name("gtnrlib_" + folder + "_upgrade_frame")
                .build();
            inset = adaptable(
                folder,
                "inset",
                INSET_WIDTH,
                INSET_HEIGHT,
                INSET_EDGE,
                INSET_TOP,
                INSET_EDGE,
                INSET_EDGE);
            slotItem = adaptable(folder, "slot_item", SLOT_SIZE, SLOT_SIZE, 1, 1, 1, 1);
            slotFluid = adaptable(folder, "slot_fluid", SLOT_SIZE, SLOT_SIZE, 1, 1, 1, 1);
            scroller = adaptable(folder, "scroller", SCROLLBAR_WIDTH, SCROLLER_HEIGHT, 2, 2, 2, 4);
            scrollerDisabled = image(folder, "scroller_disabled", SCROLLBAR_WIDTH, SCROLLER_HEIGHT);
            bigScroller = image(folder, "big_scroller", BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
            bigScrollerDisabled = image(folder, "big_scroller_disabled", BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
            textField = fieldSprite(folder, "text_field", 0);
            textFieldDisabled = fieldSprite(folder, "text_field_disabled", TEXT_FIELD_HEIGHT);
            iconButton = frame(folder, "icon_button", 176, 128);
            iconButtonHover = frame(folder, "icon_button_hover", 212, 128);
            iconButtonSelected = frame(folder, "icon_button_selected", 194, 128);
            tabButton = tabSprite(folder, "tab_button", TAB_BUTTON_U, TAB_BUTTON_V, TAB_BUTTON_SIZE, TAB_BORDER_LEFT);
            tabButtonHover = tabSprite(
                folder,
                "tab_button_hover",
                TAB_BUTTON_U,
                TAB_HOVER_V,
                TAB_BUTTON_SIZE,
                TAB_BORDER_LEFT);
            pageTab = tabSprite(folder, "page_tab", PAGE_TAB_U, PAGE_TAB_V, PAGE_TAB_SIZE, PAGE_TAB_BORDER_LEFT);
            pageTabSelected = tabSprite(
                folder,
                "page_tab_selected",
                PAGE_TAB_U,
                PAGE_TAB_V + PAGE_TAB_SIZE,
                PAGE_TAB_SIZE,
                PAGE_TAB_BORDER_LEFT);
            pageTabHover = tabSprite(
                folder,
                "page_tab_hover",
                PAGE_TAB_U + PAGE_TAB_SIZE,
                PAGE_TAB_V,
                PAGE_TAB_SIZE,
                PAGE_TAB_BORDER_LEFT);
            switchOff = checkbox(folder, "switch_off", 0, 28);
            switchOffHover = checkbox(folder, "switch_off_hover", 22, 28);
            switchOn = checkbox(folder, "switch_on", 0, 40);
            switchOnHover = checkbox(folder, "switch_on_hover", 22, 40);
            radioOff = sprite(folder, "checkbox", SWITCH_SHEET, RADIO_X, 0, RADIO_SIZE, RADIO_SIZE);
            radioOffHover = sprite(folder, "checkbox", SWITCH_SHEET, RADIO_X + RADIO_SIZE, 0, RADIO_SIZE, RADIO_SIZE);
            radioOn = sprite(folder, "checkbox", SWITCH_SHEET, RADIO_X, RADIO_SIZE, RADIO_SIZE, RADIO_SIZE);
            radioOnHover = sprite(
                folder,
                "checkbox",
                SWITCH_SHEET,
                RADIO_X + RADIO_SIZE,
                RADIO_SIZE,
                RADIO_SIZE,
                RADIO_SIZE);
            listRow = rowSprite(folder, "list_row", 0);
            listRowSelected = rowSprite(folder, "list_row_selected", LIST_ROW_HEIGHT);
            buttonBar = adaptable(folder, "button_bar", BUTTON_BAR_WIDTH, BUTTON_BAR_HEIGHT, 2, 2, 0, 4);
            progressEmpty = progressSprite(folder, "progress_empty", 0);
            progressFull = progressSprite(folder, "progress_full", PROGRESS_WIDTH);
            invalid = sprite(folder, "states", SHEET, 0, INVALID_Y, ICON_SIZE, ICON_SIZE);
        }

        /**
         * Selects the switch sprite for the supplied state.
         *
         * @param on      switch value
         * @param hovered whether the switch is hovered and available
         * @return a 22-by-12-pixel switch sprite
         */
        public UITexture switchSprite(boolean on, boolean hovered) {
            return on ? hovered ? switchOnHover : switchOn : hovered ? switchOffHover : switchOff;
        }

        /**
         * Selects the radio sprite for the supplied state.
         *
         * @param on      whether the radio option is selected
         * @param hovered whether the option is hovered and available
         * @return a 14-by-14-pixel radio sprite
         */
        public UITexture radioSprite(boolean on, boolean hovered) {
            return on ? hovered ? radioOnHover : radioOn : hovered ? radioOffHover : radioOff;
        }

        private static UITexture rowSprite(String folder, String name, int y) {
            return UITexture.builder()
                .location(texture(folder, "list_row"))
                .imageSize(LIST_ROW_WIDTH, LIST_ROW_HEIGHT * 2)
                .subAreaXYWH(0, y, LIST_ROW_WIDTH, LIST_ROW_HEIGHT)
                .adaptable(1, 1, 1, 2)
                .name("gtnrlib_" + folder + "_" + name)
                .build();
        }

        private static UITexture fieldSprite(String folder, String name, int y) {
            return UITexture.builder()
                .location(texture(folder, "text_field"))
                .imageSize(FIELD_WIDTH, FIELD_WIDTH)
                .subAreaXYWH(0, y, FIELD_WIDTH, TEXT_FIELD_HEIGHT)
                .adaptable(1, 3, 1, 1)
                .name("gtnrlib_" + folder + "_" + name)
                .build();
        }

        private static UITexture progressSprite(String folder, String name, int x) {
            return UITexture.builder()
                .location(texture(folder, "progress"))
                .imageSize(PROGRESS_WIDTH * 2, PROGRESS_HEIGHT)
                .subAreaXYWH(x, 0, PROGRESS_WIDTH, PROGRESS_HEIGHT)
                .name("gtnrlib_" + folder + "_" + name)
                .build();
        }

        /**
         * Applies this variant's disc, line, text and highlight colors to a wheel style.
         *
         * @param style builder to modify
         * @return the supplied builder
         */
        public WheelStyle.Builder wheel(WheelStyle.Builder style) {
            return style.discColor(WHEEL_DISC_ALPHA | colors.panel & 0xFFFFFF)
                .lineColor(colors.line)
                .textColor(colors.text)
                .textShadow(false)
                .highlightColor(highlight);
        }

        private static UITexture checkbox(String folder, String name, int x, int y) {
            return UITexture.builder()
                .location(texture(folder, "checkbox"))
                .imageSize(SWITCH_SHEET, SWITCH_SHEET)
                .subAreaXYWH(x, y, SWITCH_WIDTH, SWITCH_HEIGHT)
                .nonOpaque()
                .name("gtnrlib_" + folder + "_" + name)
                .build();
        }

        private static ThemePalette palette(Colors colors, int highlight, int highlightShadow, int highlightText) {
            return new ThemePalette(
                highlight,
                highlightShadow,
                0x40FFFFFF,
                colors.text,
                colors.muted,
                colors.panel,
                colors.panel,
                colors.well,
                colors.edge,
                colors.well,
                highlight,
                0x80000000 | highlight & 0xFFFFFF,
                colors.well,
                colors.edge,
                0x80FFFFFF,
                colors.line,
                colors.muted,
                colors.edge,
                colors.raised,
                colors.text,
                colors.muted,
                colors.secondary,
                colors.hoverText,
                highlightText,
                0x40000000 | colors.text & 0xFFFFFF,
                0x99000000 | colors.text & 0xFFFFFF,
                colors.error);
        }

        private ThemeBuilder<?> menu() {
            return new ThemeBuilder<>(menuId).parent(id)
                .background(IThemeApi.PANEL, panel)
                .background(IThemeApi.BUTTON, FLAT_OPTION)
                .hoverBackground(IThemeApi.BUTTON, listRowSelected)
                .textColor(IThemeApi.BUTTON, colors.text)
                .textHoverColor(IThemeApi.BUTTON, colors.hoverText)
                .widgetTheme(
                    IThemeApi.TOGGLE_BUTTON,
                    new SelectableTheme.Builder<>().background(FLAT_OPTION)
                        .textColor(colors.text)
                        .selectedBackground(listRowSelected)
                        .selectedTextColor(colors.hoverText))
                .widgetThemeHover(
                    IThemeApi.TOGGLE_BUTTON,
                    new SelectableTheme.Builder<>().background(listRowSelected)
                        .textColor(colors.hoverText)
                        .selectedBackground(listRowSelected)
                        .selectedTextColor(colors.hoverText));
        }

        private ThemeBuilder<?> theme() {
            IDrawable button = new AE2ButtonDrawable(State.NORMAL, folder),
                lit = new AE2ButtonDrawable(State.HOVER, folder);
            int normal = colors.buttonText, hover = colors.hoverText;
            ThemeBuilder<?> theme = PaletteThemes.build(id, ThemeAPI.DEFAULT_ID, palette, STYLE, skin)
                .defaultTextShadow(false)
                .background(IThemeApi.BUTTON, button)
                .hoverBackground(IThemeApi.BUTTON, lit)
                .textColor(IThemeApi.BUTTON, normal)
                .textHoverColor(IThemeApi.BUTTON, hover)
                .textShadow(IThemeApi.BUTTON, false)
                .textHoverShadow(IThemeApi.BUTTON, false)
                .widgetTheme(
                    IThemeApi.TOGGLE_BUTTON,
                    new SelectableTheme.Builder<>().background(button)
                        .textColor(normal)
                        .selectedBackground(lit)
                        .selectedTextColor(hover))
                .widgetThemeHover(
                    IThemeApi.TOGGLE_BUTTON,
                    new SelectableTheme.Builder<>().background(lit)
                        .textColor(hover)
                        .selectedBackground(lit)
                        .selectedTextColor(hover))
                .background(IThemeApi.CLOSE_BUTTON, button)
                .hoverBackground(IThemeApi.CLOSE_BUTTON, lit)
                .textColor(IThemeApi.CLOSE_BUTTON, normal)
                .textHoverColor(IThemeApi.CLOSE_BUTTON, hover)
                .textShadow(IThemeApi.CLOSE_BUTTON, false)
                .textHoverShadow(IThemeApi.CLOSE_BUTTON, false)
                .background(IThemeApi.FLUID_SLOT, slotFluid)
                .background(IThemeApi.TEXT_FIELD, textField)
                .textColor(IThemeApi.TEXT_FIELD, colors.fieldText)
                .textFieldHintColor(colors.placeholder)
                .textFieldMarkedColor(FIELD_SELECTION)
                .background(IThemeApi.SCROLLBAR, scroller)
                .widgetTheme(
                    SlideSwitch.Theme.KEY,
                    new SlideSwitch.Theme.Builder().text(colors.text, colors.muted, colors.text)
                        .sprites(switchOff, switchOffHover, switchOn, switchOnHover))
                .hoverBackground(SlideSwitch.Theme.KEY, IDrawable.EMPTY)
                .widgetTheme(AE2Look.KEY, new AE2Look.Marker.Builder().skin(folder))
                .widgetTheme(
                    LayoutTheme.KEY,
                    new LayoutTheme.Builder().values(STYLE.layout())
                        .switches(SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_LABEL_X - SWITCH_WIDTH))
                .background(ButtonPurpose.PRIMARY.key, lit)
                .hoverBackground(ButtonPurpose.PRIMARY.key, lit)
                .textColor(ButtonPurpose.PRIMARY.key, hover)
                .textHoverColor(ButtonPurpose.PRIMARY.key, hover)
                .textShadow(ButtonPurpose.PRIMARY.key, false)
                .textHoverShadow(ButtonPurpose.PRIMARY.key, false)
                .background(ButtonPurpose.DANGER.key, button)
                .hoverBackground(ButtonPurpose.DANGER.key, lit)
                .textColor(ButtonPurpose.DANGER.key, colors.bad)
                .textHoverColor(ButtonPurpose.DANGER.key, colors.bad)
                .background(ButtonPurpose.ICON.key, iconButton)
                .hoverBackground(ButtonPurpose.ICON.key, iconButtonHover);
            theme.mergeAdd(
                IThemeApi.TOGGLE_BUTTON.getFullName(),
                new JsonBuilder().add(IThemeApi.SELECTED_TEXT_SHADOW, false));
            PaletteThemes.decorate(theme, palette);
            return theme;
        }
    }

    /** Shared toolbar icons with AE2 sprites and MUI2 fallback textures. */
    public enum Icon implements IconSource {

        /** Copy action. */
        COPY(192, 0, GuiTextures.COPY),
        /** Additional actions. */
        MORE(16, 0, GuiTextures.MORE),
        /** Refresh action. */
        REFRESH(240, 0, GuiTextures.REFRESH),
        /** Visibility control. */
        VISIBLE(16, 32, GuiTextures.VISIBLE),
        /** Start or resume action. */
        PLAY(48, 32, GuiTextures.PLAY),
        /** Pause action. */
        PAUSE(64, 32, GuiTextures.PAUSE),
        /** Move-to action. */
        MOVE_TO(224, 32, GuiTextures.MOVE_TO),
        /** Search action. */
        SEARCH(160, 48, GuiTextures.SEARCH),
        /** Confirmation action. */
        CHECKMARK(176, 48, GuiTextures.CHECKMARK),
        /** Warning, such as a problem to look at. */
        EXCLAMATION(16, 48, GuiTextures.EXCLAMATION),
        /** Settings action. */
        GEAR(0, 0, GuiTextures.GEAR),
        /** Add action. */
        ADD(64, 0, GuiTextures.ADD),
        /** Remove action. */
        REMOVE(96, 0, GuiTextures.REMOVE),
        /** Close action. */
        CLOSE(112, 16, GuiTextures.CLOSE),
        /** Edit action. */
        EDIT(80, 16, GuiTextures.EDIT),
        /** Filter control. */
        FILTER(128, 0, GuiTextures.FILTER),
        /** Hidden state of a visibility control. */
        INVISIBLE(32, 32, GuiTextures.INVISIBLE),
        /** Upward arrow from a bar, such as raising something. */
        UPLOAD(16, 16, GuiTextures.UPLOAD),
        /** Downward arrow onto a bar, such as lowering something. */
        DOWNLOAD(0, 16, GuiTextures.DOWNLOAD),
        /** Two opposite horizontal arrows, such as mirroring. */
        REVERSE(224, 16, GuiTextures.REVERSE),
        /** A block or cube. */
        BLOCK(240, 16, GuiTextures.BLOCK),
        /** Turning arrow, such as a quarter turn. */
        SHIFT_BACKWARD(208, 32, GuiTextures.SHIFT_BACKWARD),
        /** A page of text, such as a list. */
        FILE(80, 48, GuiTextures.FILE),
        /** A chip, such as a machine part. */
        PROCESSOR(96, 48, GuiTextures.PROCESSOR),
        /** Arrows pointing outwards, such as fitting a view to its content. */
        MAXIMIZE(80, 32, GuiTextures.MAXIMIZE),
        /** Arrows pointing inwards, such as turning around a centre. */
        MINIMIZE(96, 32, GuiTextures.MINIMIZE),
        /** Plain upward arrow, such as raising by one step. */
        ARROW_UP(0, 64, GuiTextures.ARROW_UP),
        /** Plain downward arrow, such as lowering by one step. */
        ARROW_DOWN(16, 64, GuiTextures.ARROW_DOWN),
        /** Two opposite vertical arrows, such as flipping upside down. */
        FLIP_VERTICAL(32, 64, null),
        /** {@link #MOVE_TO} pointing up, such as a face turned upwards. */
        MOVE_TO_UP(48, 64, null),
        /** {@link #MOVE_TO} pointing down, such as a face turned downwards. */
        MOVE_TO_DOWN(64, 64, null);

        /** Source coordinates within the AE2 icon sheet, in pixels. */
        public final int x, y;
        /** MUI2 fallback texture used outside AE2 themes, or {@code null} to use the dark AE2 sprite there. */
        public final @Nullable UITexture vanilla;
        private final UITexture[] skins = new UITexture[Variant.values().length];

        Icon(int x, int y, @Nullable UITexture vanilla) {
            this.x = x;
            this.y = y;
            this.vanilla = vanilla;
        }

        /**
         * Resolves this icon's texture, caching the AE2 sprite for each variant.
         *
         * @param variant AE2 variant, or {@code null} for the MUI2 fallback; icons MUI2 has no equivalent for use the
         *                dark AE2 sprite there
         * @return icon texture
         */
        public UITexture texture(@Nullable Variant variant) {
            if (variant == null) {
                if (vanilla != null) return vanilla;
                variant = Variant.DARK;
            }
            UITexture skin = skins[variant.ordinal()];
            if (skin == null)
                skin = skins[variant.ordinal()] = sprite(variant.folder, "icons", SHEET, x, y, ICON_SIZE, ICON_SIZE);
            return skin;
        }

        @Override
        public void drawIcon(GuiContext context, WidgetTheme theme, @Nullable Variant variant, int x, int y,
            boolean enabled) {
            AE2Draw.drawSprite(texture(variant), x, y, enabled);
        }
    }

    /** Client event listener that reinstalls the theme definitions before a MUI2 theme reload. */
    @SideOnly(Side.CLIENT)
    public static class Reloader {

        /**
         * Refreshes the registered theme definitions.
         *
         * @param event theme reload event
         */
        @SubscribeEvent
        public void onThemeReload(ReloadThemeEvent.Pre event) {
            register();
        }
    }
}
