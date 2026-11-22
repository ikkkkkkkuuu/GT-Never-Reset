package com.xyp.gtnrlib.gui.theme;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.ReloadThemeEvent;
import com.cleanroommc.modularui.theme.ThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.AbstractScrollWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.gui.theme.ThemeSkin.Part;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ButtonPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2CheckboxPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Draw;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Legacy;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2PanelPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Panels;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ScrollbarPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Switches;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ThemeDefinitions;
import com.xyp.gtnrlib.gui.theme.ae2.AE2UpgradePainter;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;
import com.xyp.gtnrlib.gui.wheel.WheelStyle;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;
import com.xyp.gtnrlib.gui.widget.SkinDrawable;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The AE2 light, dark and purple dark themes for ModularUI 2: their identifiers, AE2's widget dimensions, the three
 * skins with their colors and textures ({@link Variant}), the built-in toolbar icons ({@link Icon}) and the theme
 * registration.
 * <p>
 * Select a panel theme through {@link ScreenTheme}. Panel backgrounds, fields, slots, switches and button styles are
 * supplied by the registered theme. {@code CycleDropdown} menus pick their menu theme themselves; use
 * {@link #useMenuTheme(String, String, String, String...)} for other named child menus, such as MUI2's context menus,
 * and {@link #sliderHandles(IWidget, String)} to style slider handles.
 * <p>
 * Drawing lives in the {@code ae2} package. Widgets ask {@link AE2Look#of(IWidget)} for the look of the panel they are
 * in, and the look answers everything that exists in every look. The painters draw AE2's parts, one class per part:
 * {@link AE2ButtonPainter}, {@link AE2CheckboxPainter}, {@link AE2ScrollbarPainter}, {@link AE2PanelPainter},
 * {@link AE2UpgradePainter}, {@code AE2ListRowPainter}, {@link AE2Legacy} and {@link AE2Draw}; {@code PlainLook} holds
 * the look outside AE2 themes. The drawing methods still declared here forward to those classes and are deprecated.
 * <p>
 * The class loads on both sides: constants, {@link Variant} lookups and drawable factories may be used while a GUI
 * is built on the server, as ModularUI 2 builds GUIs opened by the server on both sides. Drawing, measuring text and
 * theme registration run on the client only.
 *
 * @see Variant
 * @see AE2Look
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
     * Widget dimensions, label offsets, border widths and layout spacing in GUI pixels. {@code SMALL_SCROLLER_WIDTH}
     * and {@code BIG_SCROLLER_WIDTH} are AE2's two scroll-bar handles; {@code LIST_ROW_HEIGHT} is the height of AE2's
     * row sprite, not of GTNRLib's list rows ({@code IconRowList.ROW}); {@code UPGRADE_SLOTS_PER_COLUMN} is a count.
     * The {@code CHECKBOX} sizes are those of AE2's check box, a 22-by-12 on/off sprite, and of its label.
     */
    public static final int BUTTON_HEIGHT = 20, TEXT_FIELD_HEIGHT = 12, ICON_SIZE = 16, ICON_BUTTON_WIDTH = 18,
        ICON_BUTTON_HEIGHT = 20, TAB_BUTTON_SIZE = 20, CHECKBOX_WIDTH = 22, CHECKBOX_HEIGHT = 12, CHECKBOX_LABEL_X = 26,
        CHECKBOX_LABEL_Y = 4, SMALL_SCROLLER_WIDTH = 7, INSET_TOP = 3, INSET_EDGE = 1, BIG_SCROLLER_WIDTH = 12,
        SCROLLER_HEIGHT = 15, RADIO_SIZE = 14, RADIO_LABEL_X = 16, RADIO_LABEL_Y = 4, LIST_ROW_HEIGHT = 22,
        PROGRESS_WIDTH = 6, PROGRESS_HEIGHT = 18, BUTTON_BAR_MARGIN = 2, BUTTON_BAR_SPACING = 6, UPGRADE_PADDING = 5,
        UPGRADE_SLOTS_PER_COLUMN = 8, VALIDATION_ICON_SIZE = 8, PANEL_EDGE = 2, PANEL_BOTTOM_EDGE = 4;
    /**
     * Size and label offsets of AE2's check box under their older names.
     *
     * @deprecated use {@link #CHECKBOX_WIDTH}, {@link #CHECKBOX_HEIGHT}, {@link #CHECKBOX_LABEL_X} and
     *             {@link #CHECKBOX_LABEL_Y}; the on/off box is called a check box everywhere, after AE2's
     *             {@code AECheckbox}
     */
    @Deprecated
    public static final int SWITCH_WIDTH = CHECKBOX_WIDTH, SWITCH_HEIGHT = CHECKBOX_HEIGHT,
        SWITCH_LABEL_X = CHECKBOX_LABEL_X, SWITCH_LABEL_Y = CHECKBOX_LABEL_Y;
    /**
     * Width of the small scroll-bar handle.
     *
     * @deprecated use {@link #SMALL_SCROLLER_WIDTH}; this name reads like the width of a whole scroll bar
     */
    @Deprecated
    public static final int SCROLLBAR_WIDTH = SMALL_SCROLLER_WIDTH;
    /**
     * Upgrade slots in a full column.
     *
     * @deprecated use {@link #UPGRADE_SLOTS_PER_COLUMN}; the slots run down a column, not across a row
     */
    @Deprecated
    public static final int UPGRADE_ROWS = UPGRADE_SLOTS_PER_COLUMN;
    /**
     * AE2 page tabs: size, distance between tab tops (neighbours share an outline row) and how far a tab starts inside
     * the panel's right edge, which its left columns cover so the selected tab joins the panel.
     */
    public static final int PAGE_TAB_SIZE = 22, PAGE_TAB_PITCH = 21, PAGE_TAB_INSET = 3;
    /** How far an upgrade panel starts inside the right edge of the panel it belongs to, as AE2 places it. */
    public static final int UPGRADE_OVERLAP = 2;
    /** Size of the dropdown triangle drawn by {@link AE2PanelPainter#drawCaret}. */
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
    /** Folder names of the three AE2 skins below {@code textures/gui/skin/}. */
    public static final String LIGHT_FOLDER = "ae2_light", DARK_FOLDER = "ae2_dark", PURPLE_FOLDER = "ae2_dark_purple";
    /** Textures the purple dark skin recolours; it takes the others from {@link #DARK_FOLDER}. */
    public static final Set<String> PURPLE_TEXTURES = Set
        .of("button_highlighted", "checkbox", "legacy_states", "list_row", "slider_fill", "slider_tag", "states");
    private static final String ROOT = "textures/gui/skin/";
    /** Shared sizing and spacing used by all three AE2 skins. */
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
     * Registers fresh definitions for the three panel themes and their menu themes.
     * <p>
     * GTNRLib installs the themes during client initialization and invokes this method on theme reload. Consumers
     * select a theme identifier without registering these definitions themselves.
     */
    @SideOnly(Side.CLIENT)
    public static void register() {
        for (Variant variant : Variant.values()) {
            replace(variant.id, AE2ThemeDefinitions.panelTheme(variant));
            replace(variant.menuId, AE2ThemeDefinitions.menuTheme(variant));
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
     * Resolves a texture below GTNRLib's GUI skin directory. The purple dark skin ships only the
     * {@link #PURPLE_TEXTURES} it recolours and takes every other texture from the dark skin's folder.
     *
     * @param folder skin folder, such as {@code ae2_light}
     * @param name   texture filename without the {@code .png} extension
     * @return texture resource location
     */
    public static ResourceLocation texture(String folder, String name) {
        String source = PURPLE_FOLDER.equals(folder) && !PURPLE_TEXTURES.contains(name) ? DARK_FOLDER : folder;
        return new ResourceLocation(GTNRLib.MODID, ROOT + source + "/" + name + ".png");
    }

    /**
     * Finds the variant associated with a panel or menu theme identifier.
     *
     * @param themeId theme identifier, or {@code null} for the default theme
     * @return the matching variant, or {@code null} for an unrecognized identifier
     */
    public static @Nullable Variant variant(@Nullable String themeId) {
        for (Variant variant : Variants.ALL)
            if (variant.id.equals(themeId) || variant.menuId.equals(themeId)) return variant;
        return null;
    }

    // Widgets in plain panels look this up every frame; a holder keeps the array from being copied each time and is
    // only read once the skins exist, since the skins themselves load this class.
    private static class Variants {

        private static final Variant[] ALL = Variant.values();
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

    public static void polish(IWidget root, @Nullable String themeId) {
        Variant variant = variant(themeId);
        if (variant != null) polish(root, variant, new AE2ButtonDrawable(State.NORMAL, variant.folder));
    }

    private static void polish(IWidget widget, Variant variant, IDrawable handle) {
        if (widget instanceof SliderWidget slider) slider.sliderTexture(handle);
        if (widget instanceof AbstractScrollWidget<?, ?>scroll) scroll.getScrollArea()
            .setScrollBarBackgroundColor(variant.colors.outline());
        for (IWidget child : widget.getChildren()) polish(child, variant, handle);
    }

    /**
     * Applies an AE2 handle texture to sliders in a widget tree.
     * <p>
     * Only children already present in the tree are visited. An unrecognized theme identifier leaves handles unchanged.
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

    /**
     * Same as {@link AE2Legacy#legacyStates}.
     *
     * @param variant recolored variant, or {@code null} for the resource supplied by AE2
     * @return state sheet resource location
     * @deprecated use {@link AE2Legacy#legacyStates}
     */
    @Deprecated
    public static ResourceLocation legacyStates(@Nullable Variant variant) {
        return AE2Legacy.legacyStates(AE2Look.of(variant));
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
        return AE2Legacy.legacyTerminal(AE2Look.of(variant));
    }

    /**
     * Same as {@link AE2Look#scrollbarWidth}.
     *
     * @param variant AE2 variant, or {@code null} for the vanilla creative-inventory style
     * @return 12 pixels for AE2 or 14 pixels for vanilla
     * @deprecated use {@link AE2Look#scrollbarWidth} of the panel's look
     */
    @Deprecated
    public static int scrollbarWidth(@Nullable Variant variant) {
        return AE2Look.of(variant)
            .scrollbarWidth();
    }

    /**
     * Same as {@link AE2Look#drawScrollbar}.
     *
     * @param variant AE2 variant, or {@code null} for the vanilla creative-inventory style
     * @param x       left edge of the track
     * @param y       top edge of the track, including its one-pixel border
     * @param height  total track height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner track top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     * @deprecated use {@link AE2Look#drawScrollbar} of the panel's look
     */
    @Deprecated
    public static void drawScrollbar(@Nullable Variant variant, int x, int y, int height, int handleY,
        boolean enabled) {
        AE2Look.of(variant)
            .drawScrollbar(x, y, height, handleY, enabled);
    }

    /**
     * Same as {@link AE2ScrollbarPainter#drawSmallScrollbar}.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the handle; the groove starts one pixel to its right
     * @param y       top edge of the groove, including its one-pixel border
     * @param height  total groove height, at least {@code SCROLLER_HEIGHT + 2}
     * @param handleY handle offset from the inner groove top, from zero to {@code height - SCROLLER_HEIGHT - 2}
     * @param enabled whether to draw the enabled handle
     * @deprecated use {@link AE2ScrollbarPainter#drawSmallScrollbar}
     */
    @Deprecated
    public static void drawSmallScrollbar(Variant variant, int x, int y, int height, int handleY, boolean enabled) {
        AE2ScrollbarPainter.drawSmallScrollbar(variant, x, y, height, handleY, enabled);
    }

    /**
     * Same as {@link AE2PanelPainter#drawButtonBar}.
     *
     * @param variant non-null AE2 variant
     * @param x       left edge of the background
     * @param y       top edge of the background
     * @param width   background width, including button margins
     * @param height  background height, including button margins and spacing
     * @deprecated use {@link AE2PanelPainter#drawButtonBar}
     */
    @Deprecated
    public static void drawButtonBar(Variant variant, int x, int y, int width, int height) {
        AE2PanelPainter.drawButtonBar(variant, x, y, width, height);
    }

    /**
     * Same as {@link AE2UpgradePainter#upgradePanelWidth}.
     *
     * @param slots nonnegative slot count; zero still reserves one column
     * @return panel width in pixels, 23 for one column
     * @deprecated use {@link AE2UpgradePainter#upgradePanelWidth}
     */
    @Deprecated
    public static int upgradePanelWidth(int slots) {
        return AE2UpgradePainter.upgradePanelWidth(slots);
    }

    /**
     * Same as {@link AE2UpgradePainter#upgradePanelHeight}.
     *
     * @param slots nonnegative slot count; zero still reserves one row
     * @return panel height in pixels
     * @deprecated use {@link AE2UpgradePainter#upgradePanelHeight}
     */
    @Deprecated
    public static int upgradePanelHeight(int slots) {
        return AE2UpgradePainter.upgradePanelHeight(slots);
    }

    /**
     * Same as {@link AE2UpgradePainter#drawUpgradePanel}.
     *
     * @param variant non-null AE2 variant
     * @param x       panel left edge
     * @param y       panel top edge
     * @param slots   nonnegative number of slots to draw
     * @deprecated use {@link AE2UpgradePainter#drawUpgradePanel}
     */
    @Deprecated
    public static void drawUpgradePanel(Variant variant, int x, int y, int slots) {
        AE2UpgradePainter.drawUpgradePanel(variant, x, y, slots);
    }

    /**
     * Same as {@link AE2PanelPainter#tableSize}.
     *
     * @param cells    positive number of cells along the axis
     * @param cellSize positive inner size of each cell in pixels
     * @return total dimension in pixels
     * @deprecated use {@link AE2PanelPainter#tableSize}
     */
    @Deprecated
    public static int tableSize(int cells, int cellSize) {
        return AE2PanelPainter.tableSize(cells, cellSize);
    }

    /**
     * Same as {@link AE2PanelPainter#drawTable}.
     *
     * @param variant    non-null AE2 variant
     * @param x          table left edge
     * @param y          table top edge
     * @param columns    positive column count
     * @param rows       positive row count
     * @param cellWidth  positive inner cell width
     * @param cellHeight positive inner cell height
     * @deprecated use {@link AE2PanelPainter#drawTable}
     */
    @Deprecated
    public static void drawTable(Variant variant, int x, int y, int columns, int rows, int cellWidth, int cellHeight) {
        AE2PanelPainter.drawTable(variant, x, y, columns, rows, cellWidth, cellHeight);
    }

    /**
     * Creates a selectable list-row background in the look of the whole screen.
     *
     * @param selected selection supplier; a hover supplier can be used for hover highlighting
     * @return the row background drawable
     * @deprecated use {@link AE2PanelPainter#listRow}, which follows the theme of the row's own panel
     */
    @Deprecated
    public static IDrawable listRow(BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> AE2PanelPainter
            .drawListRow(AE2Look.of(context), selected.getAsBoolean(), x, y, width, height);
    }

    /**
     * Same as {@link AE2CheckboxPainter#radio}.
     *
     * @param widget    widget supplying hover state
     * @param selected  supplier of the selected state
     * @param label     supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @return the indicator and label drawable
     * @deprecated use {@link AE2CheckboxPainter#radio}
     */
    @Deprecated
    public static IDrawable radio(Widget<?> widget, BooleanSupplier selected, Supplier<String> label,
        BooleanSupplier available) {
        return AE2CheckboxPainter.radio(widget, selected, label, available);
    }

    /**
     * Same as {@link AE2CheckboxPainter#radioWidth}.
     *
     * @param label display text
     * @return required width in pixels
     * @deprecated use {@link AE2CheckboxPainter#radioWidth}
     */
    @Deprecated
    public static int radioWidth(String label) {
        return AE2CheckboxPainter.radioWidth(label);
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
        AE2Legacy.drawLegacyState(AE2Look.of(variant), index, x, y, size);
    }

    /**
     * Same as {@link AE2ButtonPainter#button}.
     *
     * @param widget    widget supplying hover state
     * @param available supplier of the visual enabled state; input handling remains the caller's responsibility
     * @param selected  supplier of the selected state, which uses the highlighted background
     * @return background drawable suitable for both normal and hover backgrounds
     * @deprecated use {@link AE2ButtonPainter#button}
     */
    @Deprecated
    public static IDrawable button(Widget<?> widget, BooleanSupplier available, BooleanSupplier selected) {
        return AE2ButtonPainter.button(widget, available, selected);
    }

    /**
     * Resolves the text style for a button in the look of the whole screen.
     *
     * @param context current drawing context
     * @param state   visual button state
     * @param height  button height in pixels
     * @return text color, vertical offset and shadow setting
     * @deprecated use {@link AE2Look#buttonText} of the look of the button's own panel
     */
    @Deprecated
    public static ButtonText buttonText(GuiContext context, State state, int height) {
        return AE2Look.of(context)
            .buttonText(state, height);
    }

    /**
     * Same as {@link AE2ButtonPainter#label}.
     *
     * @param widget    widget supplying hover state
     * @param text      supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @param selected  supplier of the selected state
     * @return label drawable suitable for a button overlay
     * @deprecated use {@link AE2ButtonPainter#label}
     */
    @Deprecated
    public static IDrawable label(Widget<?> widget, Supplier<String> text, BooleanSupplier available,
        BooleanSupplier selected) {
        return AE2ButtonPainter.label(widget, text, available, selected);
    }

    /**
     * Same as {@link AE2ButtonPainter#iconButton}.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param available supplier of the visual enabled state
     * @return combined background and icon drawable; the standard size is 18 by 20 pixels
     * @deprecated use {@link AE2ButtonPainter#iconButton}
     */
    @Deprecated
    public static IDrawable iconButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return AE2ButtonPainter.iconButton(widget, icon, available);
    }

    /**
     * Same as {@link AE2ButtonPainter#tabButton}.
     *
     * @param widget    widget supplying hover state
     * @param icon      supplier of the displayed icon
     * @param available supplier of the visual enabled state
     * @return combined tab background and icon drawable
     * @deprecated use {@link AE2ButtonPainter#tabButton}
     */
    @Deprecated
    public static IDrawable tabButton(Widget<?> widget, Supplier<? extends IconSource> icon,
        BooleanSupplier available) {
        return AE2ButtonPainter.tabButton(widget, icon, available);
    }

    /**
     * Same as {@link AE2CheckboxPainter#checkbox}.
     *
     * @param widget    widget supplying hover state
     * @param on        supplier of the switch value
     * @param label     supplier of non-null display text
     * @param available supplier of the visual enabled state
     * @return combined switch and label drawable
     * @deprecated use {@link AE2CheckboxPainter#checkbox}
     */
    @Deprecated
    public static IDrawable toggleSwitch(Widget<?> widget, BooleanSupplier on, Supplier<String> label,
        BooleanSupplier available) {
        return AE2CheckboxPainter.checkbox(widget, on, label, available);
    }

    /**
     * Same as {@link AE2CheckboxPainter#checkboxWidth}.
     *
     * @param label display text
     * @return required width in pixels
     * @deprecated use {@link AE2CheckboxPainter#checkboxWidth}
     */
    @Deprecated
    public static int switchWidth(String label) {
        return AE2CheckboxPainter.checkboxWidth(label);
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

    /** Client event listener that reinstalls the theme definitions before a MUI2 theme reload. */
    @SideOnly(Side.CLIENT)
    /** @deprecated Use the panel look or its painter. */
    @Deprecated
    public static IDrawable panelBackground(int left, int right) {
        return AE2Panels.panelBackground(left, right);
    }

    /** @deprecated Use the panel look or its painter. */
    @Deprecated
    public static IDrawable well(int width, int plainColor) {
        return AE2Panels.well(width, plainColor);
    }

    /** @deprecated Use the panel look or its painter. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static int drawSwitch(@Nullable Variant variant, int x, int y, boolean on, boolean hovered) {
        return AE2Switches.drawSwitch(variant, x, y, on, hovered);
    }

    /** @deprecated Use the panel look or its painter. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static void drawCaret(int x, int y, boolean open, int color) {
        AE2PanelPainter.drawCaret(x, y, open, color);
    }

    /** @deprecated Use the panel look or its painter. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static void drawTintedIcon(Icon icon, int x, int y, int color) {
        AE2Draw.drawTintedIcon(icon, x, y, color);
    }

    /** @deprecated Use the panel look or its painter. */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static void drawWell(@Nullable Variant variant, int x, int y, int width, int height, int plainColor) {
        AE2Panels.drawWell(variant, x, y, width, height, plainColor);
    }

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

    /**
     * The three AE2 skins: light, dark and purple dark. A skin is data only: its theme identifiers, its palette, its
     * selection colors, which match the selected sprites of the skin, and its textures.
     * <p>
     * The registered themes ({@link AE2ThemeDefinitions}) and the AE2 painters both read it. The enum loads on both
     * sides
     * and builds no theme itself. Drawing code does not pick a skin: it asks {@link AE2Look#of} for the look of its
     * panel
     * and takes the skin from {@link AE2Look#variant()}.
     */
    public enum Variant {

        /** Light AE2 palette. */
        LIGHT(LIGHT_ID, LIGHT_MENU_ID, LIGHT_FOLDER, false, Colors.builder()
            .text(0xFF413F54)
            .muted(0xFF878FA5)
            .secondary(0xFF4A5068)
            .buttonText(0xFFF2F2F2)
            .hoverText(0xFF517497)
            .disabledText(0xFF413F54)
            .fieldText(0xFFF2F2F2)
            .placeholder(0xFFDEDFE3)
            .line(0xFF696D88)
            .edge(0xFFF2F2F2)
            .panel(0xFFCBCCD4)
            .well(0xFF9A9FB4)
            .outline(0xFF413F54)
            .raised(0xFFADB0C4)
            .good(0xFF1C6B34)
            .bad(0xFF8F2B23)
            .info(0xFF144673)
            .warning(0xFF7A4A00)
            .error(0xFFFF1900)
            .build(), HIGHLIGHT, HIGHLIGHT_TOP, HIGHLIGHT_SHADOW),
        /** Dark AE2 palette. */
        DARK(DARK_ID, DARK_MENU_ID, DARK_FOLDER, true, darkColors(0xFF517497), HIGHLIGHT, HIGHLIGHT_TOP,
            HIGHLIGHT_SHADOW),
        /** Dark AE2 palette with fluix-purple selection highlights. */
        DARK_PURPLE(DARK_PURPLE_ID, DARK_PURPLE_MENU_ID, PURPLE_FOLDER, true, darkColors(0xFFFFFFFF), PURPLE_HIGHLIGHT,
            PURPLE_HIGHLIGHT_TOP, PURPLE_HIGHLIGHT_SHADOW, 0xFFFFFFFF);

        private static final int PAGE_TAB_U = 128, PAGE_TAB_V = 128;
        private static final int PANEL_SIZE = 256, INSET_WIDTH = 8, INSET_HEIGHT = 9, FIELD_WIDTH = 128,
            CHECKBOX_SHEET = 64, TAB_BUTTON_U = 160, TAB_BUTTON_V = 192, TAB_HOVER_V = 224, TAB_BORDER_LEFT = 2,
            PAGE_TAB_BORDER_LEFT = 3, TRACK_WIDTH = 8, TRACK_HEIGHT = 6, FILL_SIZE = 4, TAG_WIDTH = 6, TAG_HEIGHT = 9,
            DIVIDER_WIDTH = 4, DIVIDER_HEIGHT = 2, LIST_ROW_WIDTH = 67, BUTTON_BAR_WIDTH = 21, BUTTON_BAR_HEIGHT = 26,
            RADIO_X = 28, INVALID_Y = 128;

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
        /**
         * The panel texture without its left border, for upgrade panels; see
         * {@link AE2UpgradePainter#drawUpgradePanel}.
         */
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
        /** Unchecked check box sprites in normal and hovered states, 22 by 12 pixels. */
        public final UITexture switchOff, switchOffHover, switchOn, switchOnHover;
        public final UITexture checkboxOff, checkboxOffHover;
        /** Checked check box sprites in normal and hovered states, 22 by 12 pixels. */
        public final UITexture checkboxOn, checkboxOnHover;
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
            this(id, menuId, folder, dark, colors, highlight, highlightTop, highlightShadow, colors.outline());
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
            skin = ThemeSkin.NONE.with(Part.PANEL, NineSlice.PANEL.skin(folder))
                .with(Part.CARD, NineSlice.INSET.skin(folder))
                .with(Part.SLOT, NineSlice.SLOT.skin(folder))
                .with(Part.SLIDER_TRACK, skin(folder, "slider_track", TRACK_WIDTH, TRACK_HEIGHT).border(1, 2, 1, 1))
                .with(Part.SLIDER_FILL, skin(folder, "slider_fill", FILL_SIZE, FILL_SIZE).border(0, 1, 0, 0))
                .with(Part.SLIDER_TAG, skin(folder, "slider_tag", TAG_WIDTH, TAG_HEIGHT).border(1, 2, 1, 2))
                .with(Part.DIVIDER, skin(folder, "divider", DIVIDER_WIDTH, DIVIDER_HEIGHT));
            panel = NineSlice.PANEL.adaptable(folder);
            upgradeFrame = UITexture.builder()
                .location(texture(folder, "panel"))
                .imageSize(PANEL_SIZE, PANEL_SIZE)
                .subAreaXYWH(PANEL_EDGE, 0, PANEL_SIZE - PANEL_EDGE, PANEL_SIZE)
                .adaptable(0, PANEL_EDGE, PANEL_EDGE, PANEL_BOTTOM_EDGE)
                .name("gtnrlib_" + folder + "_upgrade_frame")
                .build();
            inset = NineSlice.INSET.adaptable(folder);
            slotItem = NineSlice.SLOT.adaptable(folder);
            slotFluid = adaptable(folder, "slot_fluid", SLOT_SIZE, SLOT_SIZE, 1, 1, 1, 1);
            scroller = adaptable(folder, "scroller", SMALL_SCROLLER_WIDTH, SCROLLER_HEIGHT, 2, 2, 2, 4);
            scrollerDisabled = image(folder, "scroller_disabled", SMALL_SCROLLER_WIDTH, SCROLLER_HEIGHT);
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
            checkboxOff = checkbox(folder, "switch_off", 0, 28);
            checkboxOffHover = checkbox(folder, "switch_off_hover", 22, 28);
            checkboxOn = checkbox(folder, "switch_on", 0, 40);
            checkboxOnHover = checkbox(folder, "switch_on_hover", 22, 40);
            switchOff = checkboxOff;
            switchOffHover = checkboxOffHover;
            switchOn = checkboxOn;
            switchOnHover = checkboxOnHover;
            radioOff = sprite(folder, "checkbox", CHECKBOX_SHEET, RADIO_X, 0, RADIO_SIZE, RADIO_SIZE);
            radioOffHover = sprite(folder, "checkbox", CHECKBOX_SHEET, RADIO_X + RADIO_SIZE, 0, RADIO_SIZE, RADIO_SIZE);
            radioOn = sprite(folder, "checkbox", CHECKBOX_SHEET, RADIO_X, RADIO_SIZE, RADIO_SIZE, RADIO_SIZE);
            radioOnHover = sprite(
                folder,
                "checkbox",
                CHECKBOX_SHEET,
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
         * Selects the check box sprite for the supplied state.
         *
         * @param on      whether the box is checked
         * @param hovered whether the check box is hovered and available
         * @return a 22-by-12-pixel check box sprite
         */
        public UITexture switchSprite(boolean on, boolean hovered) {
            return checkboxSprite(on, hovered);
        }

        public Colors rowColors(boolean selected) {
            return selected && this != DARK_PURPLE ? LIGHT.colors : colors;
        }

        public WheelStyle.Builder wheel(WheelStyle.Builder builder) {
            return builder.skin(this);
        }

        public UITexture checkboxSprite(boolean on, boolean hovered) {
            return on ? hovered ? checkboxOnHover : checkboxOn : hovered ? checkboxOffHover : checkboxOff;
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

        private static UITexture checkbox(String folder, String name, int x, int y) {
            return UITexture.builder()
                .location(texture(folder, "checkbox"))
                .imageSize(CHECKBOX_SHEET, CHECKBOX_SHEET)
                .subAreaXYWH(x, y, CHECKBOX_WIDTH, CHECKBOX_HEIGHT)
                .nonOpaque()
                .name("gtnrlib_" + folder + "_" + name)
                .build();
        }

        private static ThemePalette palette(Colors colors, int highlight, int highlightShadow, int highlightText) {
            return new ThemePalette(
                highlight,
                highlightShadow,
                0x40FFFFFF,
                colors.text(),
                colors.muted(),
                colors.panel(),
                colors.panel(),
                colors.well(),
                colors.edge(),
                colors.well(),
                highlight,
                0x80000000 | highlight & 0xFFFFFF,
                colors.well(),
                colors.edge(),
                0x80FFFFFF,
                colors.line(),
                colors.muted(),
                colors.edge(),
                colors.raised(),
                colors.text(),
                colors.muted(),
                colors.secondary(),
                colors.hoverText(),
                highlightText,
                0x40000000 | colors.text() & 0xFFFFFF,
                0x99000000 | colors.text() & 0xFFFFFF,
                colors.error());
        }

        private static SkinDrawable skin(String folder, String name, int width, int height) {
            return new SkinDrawable().texture(texture(folder, name), width, height);
        }

        /**
         * A texture used both by GTNRLib's skin parts and by MUI2 directly, so its size and borders are written once.
         *
         * @param name   texture filename without the {@code .png} extension
         * @param width  texture width
         * @param height texture height
         * @param left   left border kept unstretched
         * @param top    top border kept unstretched
         * @param right  right border kept unstretched
         * @param bottom bottom border kept unstretched
         */
        private record NineSlice(String name, int width, int height, int left, int top, int right, int bottom) {

            private static final NineSlice PANEL = new NineSlice(
                "panel",
                PANEL_SIZE,
                PANEL_SIZE,
                PANEL_EDGE,
                PANEL_EDGE,
                PANEL_EDGE,
                PANEL_BOTTOM_EDGE),
                INSET = new NineSlice(
                    "inset",
                    INSET_WIDTH,
                    INSET_HEIGHT,
                    INSET_EDGE,
                    INSET_TOP,
                    INSET_EDGE,
                    INSET_EDGE),
                SLOT = new NineSlice("slot_item", SLOT_SIZE, SLOT_SIZE, 1, 1, 1, 1);

            private SkinDrawable skin(String folder) {
                return Variant.skin(folder, name, width, height)
                    .border(left, top, right, bottom);
            }

            private UITexture adaptable(String folder) {
                return Variant.adaptable(folder, name, width, height, left, top, right, bottom);
            }
        }

        private static UITexture adaptable(String folder, String name, int width, int height, int left, int top,
            int right, int bottom) {
            return UITexture.builder()
                .location(texture(folder, name))
                .imageSize(width, height)
                .adaptable(left, top, right, bottom)
                .name("gtnrlib_" + folder + "_" + name)
                .build();
        }

        private static Colors darkColors(int hoverText) {
            return Colors.builder()
                .text(0xFFAAAAAA)
                .muted(0xFF7E808E)
                .secondary(0xFF9496A4)
                .buttonText(0xFFE0E0E0)
                .hoverText(hoverText)
                .disabledText(0xFF5E6070)
                .fieldText(0xFFE0E0E0)
                .placeholder(0xFF6B6E7E)
                .line(0xFF14151D)
                .edge(0xFF8E8E8E)
                .panel(0xFF42434B)
                .well(0xFF242631)
                .outline(0xFF0A090E)
                .raised(0xFF2D2F3C)
                .good(0xFF5FCB79)
                .bad(0xFFE06A60)
                .info(0xFF6CB4F0)
                .warning(0xFFE0A54A)
                .error(0xFFFF5A3C)
                .build();
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

        // Tab sprites are a frame around a flat fill, two pixels at the top and four at the bottom with the shadow, so
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

        /**
         * Cuts a sprite out of one of a skin's square sheets.
         *
         * @param folder skin folder, such as {@code ae2_light}
         * @param sheet  sheet filename without the {@code .png} extension
         * @param size   width and height of the sheet in pixels
         * @param x      left edge of the sprite in the sheet
         * @param y      top edge of the sprite in the sheet
         * @param width  sprite width
         * @param height sprite height
         * @return the sprite, drawn with transparency
         */
        public static UITexture sprite(String folder, String sheet, int size, int x, int y, int width, int height) {
            return UITexture.builder()
                .location(texture(folder, sheet))
                .imageSize(size, size)
                .subAreaXYWH(x, y, width, height)
                .nonOpaque()
                .build();
        }
    }

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

        /**
         * Starts a palette filled in by name, so it does not depend on the order of the nineteen components. A color
         * left
         * unset is 0, fully transparent.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /** Collects the colors of an {@link Colors} by name; each setter takes an ARGB color. */
        public static class Builder {

            private int text, muted, secondary, buttonText, hoverText, disabledText, fieldText, placeholder, line, edge,
                panel, well, outline, raised, good, bad, info, warning, error;

            /**
             * @param text primary text color
             * @return this builder
             */
            public Builder text(int text) {
                this.text = text;
                return this;
            }

            /**
             * @param muted muted text color
             * @return this builder
             */
            public Builder muted(int muted) {
                this.muted = muted;
                return this;
            }

            /**
             * @param secondary secondary text color
             * @return this builder
             */
            public Builder secondary(int secondary) {
                this.secondary = secondary;
                return this;
            }

            /**
             * @param buttonText normal button text color
             * @return this builder
             */
            public Builder buttonText(int buttonText) {
                this.buttonText = buttonText;
                return this;
            }

            /**
             * @param hoverText highlighted button text color
             * @return this builder
             */
            public Builder hoverText(int hoverText) {
                this.hoverText = hoverText;
                return this;
            }

            /**
             * @param disabledText disabled button text color
             * @return this builder
             */
            public Builder disabledText(int disabledText) {
                this.disabledText = disabledText;
                return this;
            }

            /**
             * @param fieldText text-field content color
             * @return this builder
             */
            public Builder fieldText(int fieldText) {
                this.fieldText = fieldText;
                return this;
            }

            /**
             * @param placeholder text-field hint color
             * @return this builder
             */
            public Builder placeholder(int placeholder) {
                this.placeholder = placeholder;
                return this;
            }

            /**
             * @param line separator and grid-line color
             * @return this builder
             */
            public Builder line(int line) {
                this.line = line;
                return this;
            }

            /**
             * @param edge panel edge color
             * @return this builder
             */
            public Builder edge(int edge) {
                this.edge = edge;
                return this;
            }

            /**
             * @param panel panel fill color
             * @return this builder
             */
            public Builder panel(int panel) {
                this.panel = panel;
                return this;
            }

            /**
             * @param well inset fill color
             * @return this builder
             */
            public Builder well(int well) {
                this.well = well;
                return this;
            }

            /**
             * @param outline outline color
             * @return this builder
             */
            public Builder outline(int outline) {
                this.outline = outline;
                return this;
            }

            /**
             * @param raised raised surface color
             * @return this builder
             */
            public Builder raised(int raised) {
                this.raised = raised;
                return this;
            }

            /**
             * @param good positive status color
             * @return this builder
             */
            public Builder good(int good) {
                this.good = good;
                return this;
            }

            /**
             * @param bad negative status color
             * @return this builder
             */
            public Builder bad(int bad) {
                this.bad = bad;
                return this;
            }

            /**
             * @param info informational status color
             * @return this builder
             */
            public Builder info(int info) {
                this.info = info;
                return this;
            }

            /**
             * @param warning warning status color
             * @return this builder
             */
            public Builder warning(int warning) {
                this.warning = warning;
                return this;
            }

            /**
             * @param error input validation error color
             * @return this builder
             */
            public Builder error(int error) {
                this.error = error;
                return this;
            }

            /**
             * Creates the colors.
             *
             * @return the filled-in colors
             */
            public Colors build() {
                return new Colors(
                    text,
                    muted,
                    secondary,
                    buttonText,
                    hoverText,
                    disabledText,
                    fieldText,
                    placeholder,
                    line,
                    edge,
                    panel,
                    well,
                    outline,
                    raised,
                    good,
                    bad,
                    info,
                    warning,
                    error);
            }
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
        /** MUI2 fallback texture used outside AE2 themes, or {@code null} to use the light AE2 sprite there. */
        public final @Nullable UITexture vanilla;
        private final UITexture[] skins = new UITexture[Variant.values().length];

        Icon(int x, int y, @Nullable UITexture vanilla) {
            this.x = x;
            this.y = y;
            this.vanilla = vanilla;
        }

        /**
         * Resolves this icon's texture in a look, caching the AE2 sprite for each skin.
         *
         * @param look look to draw in; the plain look gives the MUI2 fallback, or for an icon MUI2 has no equivalent
         *             for
         *             the light AE2 sprite, which is white like MUI2's icons
         * @return icon texture
         */
        public UITexture texture(@Nullable Variant variant) {
            return textureForLook(AE2Look.of(variant));
        }

        public UITexture textureForLook(AE2Look look) {
            Variant variant = look.variant();
            if (variant == null) {
                if (vanilla != null) return vanilla;
                variant = Variant.LIGHT;
            }
            UITexture skin = skins[variant.ordinal()];
            if (skin == null) skin = skins[variant.ordinal()] = Variant
                .sprite(variant.folder, "icons", SHEET, x, y, ICON_SIZE, ICON_SIZE);
            return skin;
        }

        @Override
        public void drawIcon(GuiContext context, WidgetTheme theme, Variant variant, int x, int y, boolean enabled) {
            drawInLook(context, theme, AE2Look.of(variant), x, y, enabled);
        }

        @Override
        public void drawInLook(GuiContext context, WidgetTheme theme, AE2Look look, int x, int y, boolean enabled) {
            AE2Draw.drawSprite(textureForLook(look), x, y, enabled);
        }
    }

    /**
     * Text style resolved for a button state.
     *
     * @param color  ARGB text color
     * @param y      vertical offset from the button's top edge in pixels
     * @param shadow whether to draw a drop shadow
     */
    public record ButtonText(int color, int y, boolean shadow) {}
}
