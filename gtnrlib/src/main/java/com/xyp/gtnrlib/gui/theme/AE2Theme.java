package com.xyp.gtnrlib.gui.theme;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.ReloadThemeEvent;
import com.cleanroommc.modularui.theme.SelectableTheme;
import com.cleanroommc.modularui.theme.ThemeAPI;
import com.cleanroommc.modularui.theme.ThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.JsonBuilder;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.gui.theme.ThemeSkin.Part;
import com.xyp.gtnrlib.gui.wheel.WheelStyle;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;
import com.xyp.gtnrlib.gui.widget.LayoutTheme;
import com.xyp.gtnrlib.gui.widget.SkinDrawable;
import com.xyp.gtnrlib.gui.widget.SlideSwitch;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AE2Theme {

    public static final String LIGHT_ID = "gtnrlib:ae2_light";
    public static final String LIGHT_MENU_ID = "gtnrlib:ae2_light_menu";
    public static final String DARK_ID = "gtnrlib:ae2_dark";
    public static final String DARK_MENU_ID = "gtnrlib:ae2_dark_menu";
    public static final String DARK_PURPLE_ID = "gtnrlib:ae2_dark_purple";
    public static final String DARK_PURPLE_MENU_ID = "gtnrlib:ae2_dark_purple_menu";
    public static final String CONTEXT_MENU_ID = "modularui.context_menu";
    public static final int HIGHLIGHT = 0xFF9CD3FF, HIGHLIGHT_TOP = 0xFFDAFFFF, HIGHLIGHT_SHADOW = 0xFF708CBA,
        FIELD_SELECTION = 0xFF0000FF;
    public static final int PURPLE_HIGHLIGHT = 0xFF644595, PURPLE_HIGHLIGHT_TOP = 0xFFE2A3E3,
        PURPLE_HIGHLIGHT_SHADOW = 0xFF212040;
    public static final int BUTTON_HEIGHT = 20, TEXT_FIELD_HEIGHT = 12, ICON_SIZE = 16, ICON_BUTTON_WIDTH = 18,
        ICON_BUTTON_HEIGHT = 20, TAB_BUTTON_SIZE = 20, SWITCH_WIDTH = 22, SWITCH_HEIGHT = 12, SWITCH_LABEL_X = 26,
        SWITCH_LABEL_Y = 2, SCROLLBAR_WIDTH = 7, INSET_TOP = 3, INSET_EDGE = 1, BIG_SCROLLER_WIDTH = 12,
        SCROLLER_HEIGHT = 15, RADIO_SIZE = 14, RADIO_LABEL_X = 16, RADIO_LABEL_Y = 3, LIST_ROW_HEIGHT = 22,
        PROGRESS_WIDTH = 6, PROGRESS_HEIGHT = 18, BUTTON_BAR_MARGIN = 2, BUTTON_BAR_SPACING = 6, UPGRADE_PADDING = 5,
        UPGRADE_ROWS = 8, VALIDATION_ICON_SIZE = 8, PANEL_EDGE = 2, PANEL_BOTTOM_EDGE = 4;
    private static final String ROOT = "textures/gui/skin/";
    private static final ResourceLocation AE2_LEGACY_STATES = new ResourceLocation(
        "appliedenergistics2",
        "textures/guis/states.png"),
        AE2_LEGACY_TERMINAL = new ResourceLocation("appliedenergistics2", "textures/guis/terminal.png"),
        CREATIVE_TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static final int PANEL_SIZE = 256, INSET_WIDTH = 8, INSET_HEIGHT = 9, SLOT_SIZE = 18, FIELD_WIDTH = 128,
        SHEET = 256, SWITCH_SHEET = 64, TAB_HOVER_SIZE = 22, LABEL_PADDING = 2, LABEL_HEIGHT = 8, TRACK_WIDTH = 8,
        TRACK_HEIGHT = 6, FILL_SIZE = 4, TAG_WIDTH = 6, TAG_HEIGHT = 9, DIVIDER_WIDTH = 4, DIVIDER_HEIGHT = 2,
        LEGACY_CELLS = 16, DISABLED_TEXT = 0xFFA0A0A0, VANILLA_TEXT = 0xFF404040, WHEEL_DISC_ALPHA = 0xE6000000,
        GROOVE_X = 3, GROOVE_WIDTH = 6, SMALL_GROOVE_X = 1, SMALL_GROOVE_WIDTH = 5, LIST_ROW_WIDTH = 67,
        BUTTON_BAR_WIDTH = 21, BUTTON_BAR_HEIGHT = 26, RADIO_X = 28, INVALID_Y = 128, UPGRADE_SHADOW = 2,
        VANILLA_SCROLLER_U = 232, VANILLA_SCROLLER_DISABLED_U = 244, VANILLA_TRACK = 0xFF8B8B8B,
        VANILLA_TRACK_SHADOW = 0xFF373737, VANILLA_TRACK_LIGHT = 0xFFFFFFFF;
    private static final UITexture VANILLA_SCROLLER = vanillaScroller(VANILLA_SCROLLER_U),
        VANILLA_SCROLLER_DISABLED = vanillaScroller(VANILLA_SCROLLER_DISABLED_U);
    // IDrawable.EMPTY is saved as JSON null, which the theme loader reads as unset and replaces with the parent
    // theme's framed button; a transparent rectangle keeps menu options flat.
    private static final IDrawable FLAT_OPTION = new Rectangle().color(0);
    private static final float INACTIVE_ALPHA = 0.5F;
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

    public static ResourceLocation texture(String folder, String name) {
        return new ResourceLocation(GTNRLib.MODID, ROOT + folder + "/" + name + ".png");
    }

    public static @Nullable Variant variant(@Nullable String themeId) {
        for (Variant variant : Variant.values())
            if (variant.id.equals(themeId) || variant.menuId.equals(themeId)) return variant;
        return null;
    }

    public static boolean isAE2(@Nullable String themeId) {
        return variant(themeId) != null;
    }

    public static @Nullable Variant active(GuiContext context) {
        ITheme theme = screenTheme(context);
        return theme == null ? null : variant(theme.getId());
    }

    public static void useMenuTheme(@Nullable String screenTheme, String owner, String screen, String... menus) {
        Variant variant = variant(screenTheme);
        String menuTheme = variant == null ? CONTEXT_MENU_ID : variant.menuId;
        for (String menu : menus) IThemeApi.get()
            .registerThemeForScreen(owner + ":" + screen + ":" + menu, menuTheme);
    }

    public static ResourceLocation legacyStates(@Nullable Variant variant) {
        return variant == null ? AE2_LEGACY_STATES : variant.legacyStates;
    }

    public static ResourceLocation legacyTerminal(@Nullable Variant variant) {
        return variant == null ? AE2_LEGACY_TERMINAL : variant.legacyTerminal;
    }

    public static int scrollbarWidth(@Nullable Variant variant) {
        return variant == null ? BIG_SCROLLER_WIDTH + 2 : BIG_SCROLLER_WIDTH;
    }

    public static void drawScrollbar(@Nullable Variant variant, int x, int y, int height, int handleY,
        boolean enabled) {
        GlStateManager.color(1, 1, 1, 1);
        int handleTop = y + 1 + handleY;
        if (variant == null) {
            int width = scrollbarWidth(null);
            GuiDraw.drawRect(x, y, width, height, VANILLA_TRACK);
            GuiDraw.drawRect(x, y, width - 1, 1, VANILLA_TRACK_SHADOW);
            GuiDraw.drawRect(x, y, 1, height - 1, VANILLA_TRACK_SHADOW);
            GuiDraw.drawRect(x + width - 1, y + 1, 1, height - 1, VANILLA_TRACK_LIGHT);
            GuiDraw.drawRect(x + 1, y + height - 1, width - 1, 1, VANILLA_TRACK_LIGHT);
            GlStateManager.color(1, 1, 1, 1);
            (enabled ? VANILLA_SCROLLER : VANILLA_SCROLLER_DISABLED)
                .draw(x + 1, handleTop, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
            return;
        }
        variant.inset.draw(x + GROOVE_X, y, GROOVE_WIDTH, height);
        (enabled ? variant.bigScroller : variant.bigScrollerDisabled)
            .draw(x, handleTop, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }

    public static void drawSmallScrollbar(Variant variant, int x, int y, int height, int handleY, boolean enabled) {
        GlStateManager.color(1, 1, 1, 1);
        variant.inset.draw(x + SMALL_GROOVE_X, y, SMALL_GROOVE_WIDTH, height);
        (enabled ? variant.scroller : variant.scrollerDisabled)
            .draw(x, y + 1 + handleY, SCROLLBAR_WIDTH, SCROLLER_HEIGHT);
    }

    public static void drawButtonBar(Variant variant, int x, int y, int width, int height) {
        GlStateManager.color(1, 1, 1, 1);
        variant.buttonBar.draw(x, y, width, height);
    }

    public static int upgradePanelWidth(int slots) {
        return UPGRADE_PADDING * 2 + Math.max(1, (slots + UPGRADE_ROWS - 1) / UPGRADE_ROWS) * SLOT_SIZE;
    }

    public static int upgradePanelHeight(int slots) {
        return UPGRADE_PADDING * 2 + UPGRADE_SHADOW + Math.max(1, Math.min(slots, UPGRADE_ROWS)) * SLOT_SIZE;
    }

    public static void drawUpgradePanel(Variant variant, int x, int y, int slots) {
        GlStateManager.color(1, 1, 1, 1);
        variant.panel.draw(x, y, upgradePanelWidth(slots), upgradePanelHeight(slots));
        for (int i = 0; i < slots; i++) variant.slotItem.draw(
            x + UPGRADE_PADDING + i / UPGRADE_ROWS * SLOT_SIZE,
            y + UPGRADE_PADDING + i % UPGRADE_ROWS * SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE);
    }

    public static int tableSize(int cells, int cellSize) {
        return cells * cellSize + cells - 1 + INSET_EDGE * 2;
    }

    public static void drawTable(Variant variant, int x, int y, int columns, int rows, int cellWidth, int cellHeight) {
        int width = tableSize(columns, cellWidth), height = tableSize(rows, cellHeight);
        GlStateManager.color(1, 1, 1, 1);
        variant.inset.draw(x, y, width, height);
        for (int column = 1; column < columns; column++) GuiDraw.drawRect(
            x + INSET_EDGE + column * (cellWidth + 1) - 1,
            y + INSET_EDGE,
            1,
            height - INSET_EDGE * 2,
            variant.colors.line);
        for (int row = 1; row < rows; row++) GuiDraw.drawRect(
            x + INSET_EDGE,
            y + INSET_EDGE + row * (cellHeight + 1) - 1,
            width - INSET_EDGE * 2,
            1,
            variant.colors.line);
        GlStateManager.color(1, 1, 1, 1);
    }

    public static IDrawable listRow(BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> {
            boolean lit = selected.getAsBoolean();
            Variant variant = active(context);
            if (variant == null) {
                themeButton(context, lit ? State.HOVER : State.NORMAL, x, y, width, height);
                return;
            }
            GlStateManager.color(1, 1, 1, 1);
            (lit ? variant.listRowSelected : variant.listRow).draw(x, y, width, height);
        };
    }

    public static IDrawable radio(Widget<?> widget, BooleanSupplier selected, Supplier<String> label,
        BooleanSupplier available) {
        return (context, x, y, width, height, theme) -> {
            boolean usable = available.getAsBoolean(), lit = usable && widget.isHovering(),
                checked = selected.getAsBoolean();
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            String fitted = font.trimStringToWidth(label.get(), Math.max(0, width - RADIO_LABEL_X));
            Variant variant = active(context);
            ITheme screen = screenTheme(context);
            WidgetTheme look = screen == null ? null
                : screen.getFallback()
                    .getTheme();
            int textColor = look == null ? VANILLA_TEXT : look.getTextColor();
            int ring = VANILLA_TEXT, center = lit ? Color.WHITE.main : 0xFFDDDDDD, dot = VANILLA_TEXT;
            if (variant != null) {
                Colors colors = variant.colors;
                textColor = colors.text;
                ring = lit ? variant.highlight : colors.text;
                center = colors.well;
                dot = variant == Variant.LIGHT ? colors.info : variant.highlight;
            }
            if (!usable) ring = dot = textColor = variant == null ? DISABLED_TEXT : variant.colors.muted;
            GuiDraw.drawCircle(x + 1, y + 1, 12, ring, 20);
            GuiDraw.drawCircle(x + 2, y + 2, 10, center, 20);
            if (checked) GuiDraw.drawCircle(x + 4, y + 4, 6, dot, 20);
            text(
                fitted,
                x + RADIO_LABEL_X,
                y + RADIO_LABEL_Y,
                textColor,
                variant == null && look != null && look.getTextShadow());
        };
    }

    public static int radioWidth(String label) {
        return RADIO_LABEL_X + Minecraft.getMinecraft().fontRenderer.getStringWidth(label);
    }

    public static void drawLegacyState(@Nullable Variant variant, int index, float x, float y, float size) {
        float u = index % LEGACY_CELLS * ICON_SIZE / (float) SHEET,
            v = index / LEGACY_CELLS * ICON_SIZE / (float) SHEET, span = ICON_SIZE / (float) SHEET;
        GuiDraw.drawTexture(legacyStates(variant), x, y, x + size, y + size, u, v, u + span, v + span, true);
    }

    public static void sliderHandles(IWidget root, @Nullable String themeId) {
        Variant variant = variant(themeId);
        if (variant != null) sliderHandles(root, new AE2ButtonDrawable(State.NORMAL, variant.folder));
    }

    private static void sliderHandles(IWidget widget, IDrawable handle) {
        if (widget instanceof SliderWidget slider) slider.sliderTexture(handle);
        for (IWidget child : widget.getChildren()) sliderHandles(child, handle);
    }

    public static IDrawable button(Widget<?> widget, BooleanSupplier available, BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> {
            State state = State.of(widget, available.getAsBoolean(), selected.getAsBoolean());
            Variant variant = active(context);
            if (variant != null) AE2ButtonDrawable.draw(variant.folder, state, x, y, width, height);
            else themeButton(context, state, x, y, width, height);
        };
    }

    public static ButtonText buttonText(GuiContext context, State state, int height) {
        Variant variant = active(context);
        if (variant != null)
            return new ButtonText(variant.colors.button(state), AE2ButtonDrawable.textY(state, height), false);
        WidgetTheme look = buttonLook(context, state);
        int color = state == State.DISABLED ? DISABLED_TEXT : look == null ? Color.WHITE.main : look.getTextColor();
        return new ButtonText(color, (height - LABEL_HEIGHT) / 2, look == null || look.getTextShadow());
    }

    public static IDrawable label(Widget<?> widget, Supplier<String> text, BooleanSupplier available,
        BooleanSupplier selected) {
        return (context, x, y, width, height, theme) -> {
            State state = State.of(widget, available.getAsBoolean(), selected.getAsBoolean());
            ButtonText style = buttonText(context, state, height);
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            String fitted = font.trimStringToWidth(text.get(), Math.max(0, width - LABEL_PADDING * 2));
            text(fitted, x + (width - font.getStringWidth(fitted)) / 2, y + style.y(), style.color(), style.shadow());
        };
    }

    public static IDrawable iconButton(Widget<?> widget, Supplier<Icon> icon, BooleanSupplier available) {
        return (context, x, y, width, height, theme) -> {
            boolean on = available.getAsBoolean(), lit = on && widget.isHovering();
            Variant variant = active(context);
            UITexture sprite = icon.get()
                .texture(variant);
            if (variant == null) {
                themeButton(context, lit ? State.HOVER : State.NORMAL, x, y, width, height);
                drawIcon(sprite, x + (width - ICON_SIZE) / 2, y + (height - ICON_SIZE) / 2, on);
                return;
            }
            int sink = lit ? 1 : 0;
            GlStateManager.color(1, 1, 1, 1);
            (lit ? variant.iconButtonHover : variant.iconButton).draw(x, y + sink, width, height);
            drawIcon(sprite, x + (width - ICON_SIZE) / 2, y + Math.max(0, (height - ICON_SIZE) / 2 - 1) + sink, on);
        };
    }

    public static IDrawable tabButton(Widget<?> widget, Supplier<Icon> icon, BooleanSupplier available) {
        return (context, x, y, width, height, theme) -> {
            boolean on = available.getAsBoolean(), lit = on && widget.isHovering();
            Variant variant = active(context);
            UITexture sprite = icon.get()
                .texture(variant);
            if (variant == null) {
                themeButton(context, lit ? State.HOVER : State.NORMAL, x, y, width, height);
                drawIcon(sprite, x + (width - ICON_SIZE) / 2, y + (height - ICON_SIZE) / 2, on);
                return;
            }
            GlStateManager.color(1, 1, 1, 1);
            if (lit) variant.tabButtonHover.draw(x, y, TAB_HOVER_SIZE, TAB_HOVER_SIZE);
            else variant.tabButton.draw(x, y, TAB_BUTTON_SIZE, TAB_BUTTON_SIZE);
            drawIcon(sprite, x + 2, y + 1, on);
        };
    }

    public static IDrawable toggleSwitch(Widget<?> widget, BooleanSupplier on, Supplier<String> label,
        BooleanSupplier available) {
        return (context, x, y, width, height, theme) -> {
            boolean usable = available.getAsBoolean(), lit = usable && widget.isHovering(), checked = on.getAsBoolean();
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            String fitted = font.trimStringToWidth(label.get(), Math.max(0, width - SWITCH_LABEL_X));
            Variant variant = active(context);
            GlStateManager.color(1, 1, 1, usable ? 1 : INACTIVE_ALPHA);
            if (variant == null) {
                (checked ? GuiTextures.CHECK_BOX_FULL : GuiTextures.CHECK_BOX_EMPTY)
                    .draw(x, y, SWITCH_HEIGHT, SWITCH_HEIGHT);
                GlStateManager.color(1, 1, 1, 1);
                ITheme screen = screenTheme(context);
                WidgetTheme look = screen == null ? null
                    : screen.getFallback()
                        .getTheme();
                text(
                    fitted,
                    x + SWITCH_HEIGHT + LABEL_PADDING * 2,
                    y + (SWITCH_HEIGHT - LABEL_HEIGHT) / 2,
                    look == null ? VANILLA_TEXT : look.getTextColor(),
                    look != null && look.getTextShadow());
                return;
            }
            variant.switchSprite(checked, lit)
                .draw(x, y, SWITCH_WIDTH, SWITCH_HEIGHT);
            GlStateManager.color(1, 1, 1, 1);
            text(
                fitted,
                x + SWITCH_LABEL_X,
                y + SWITCH_LABEL_Y,
                usable ? variant.colors.text : variant.colors.muted,
                false);
        };
    }

    public static int switchWidth(String label) {
        return SWITCH_LABEL_X + Minecraft.getMinecraft().fontRenderer.getStringWidth(label);
    }

    public static void text(String text, int x, int y, int color, boolean shadow) {
        Platform.setupDrawFont();
        Minecraft.getMinecraft().fontRenderer.drawString(text, x, y, color, shadow);
        GlStateManager.color(1, 1, 1, 1);
    }

    private static @Nullable ITheme screenTheme(GuiContext context) {
        return context instanceof ModularGuiContext modular ? modular.getTheme() : null;
    }

    private static @Nullable WidgetTheme buttonLook(GuiContext context, State state) {
        ITheme theme = screenTheme(context);
        if (theme == null) return null;
        WidgetThemeEntry<?> entry = theme.getButtonTheme();
        return state == State.HOVER ? entry.getHoverTheme() : entry.getTheme();
    }

    private static void themeButton(GuiContext context, State state, int x, int y, int width, int height) {
        GlStateManager.color(1, 1, 1, 1);
        WidgetTheme look = buttonLook(context, state);
        IDrawable background = look == null ? null : look.getBackground();
        if (state == State.HOVER && (background == null || background == IDrawable.NONE)) {
            look = buttonLook(context, State.NORMAL);
            background = look == null ? null : look.getBackground();
        }
        if (state == State.DISABLED || background == null) {
            UITexture vanilla = state == State.DISABLED ? GuiTextures.MC_BUTTON_DISABLED
                : state == State.HOVER ? GuiTextures.MC_BUTTON_HOVERED : GuiTextures.MC_BUTTON;
            vanilla.draw(x, y, width, height);
        } else background.draw(context, x, y, width, height, look);
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void drawIcon(UITexture icon, int x, int y, boolean active) {
        GlStateManager.color(1, 1, 1, active ? 1 : INACTIVE_ALPHA);
        icon.draw(x, y, ICON_SIZE, ICON_SIZE);
        GlStateManager.color(1, 1, 1, 1);
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

    private static UITexture vanillaScroller(int u) {
        return UITexture.builder()
            .location(CREATIVE_TABS)
            .imageSize(SHEET, SHEET)
            .subAreaXYWH(u, 0, BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT)
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

    public record ButtonText(int color, int y, boolean shadow) {}

    public record Colors(int text, int muted, int secondary, int buttonText, int hoverText, int disabledText,
        int fieldText, int placeholder, int line, int edge, int panel, int well, int outline, int raised, int good,
        int bad, int info, int warning, int error) {

        public int button(State state) {
            return switch (state) {
                case NORMAL -> buttonText;
                case HOVER -> hoverText;
                case DISABLED -> disabledText;
            };
        }
    }

    public enum Variant {

        LIGHT(LIGHT_ID, LIGHT_MENU_ID, "ae2_light",
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
                0xFFCE2401),
            HIGHLIGHT, HIGHLIGHT_TOP, HIGHLIGHT_SHADOW),
        DARK(DARK_ID, DARK_MENU_ID, "ae2_dark", darkColors(0xFF517497), HIGHLIGHT, HIGHLIGHT_TOP, HIGHLIGHT_SHADOW),
        DARK_PURPLE(DARK_PURPLE_ID, DARK_PURPLE_MENU_ID, "ae2_dark_purple", darkColors(0xFFFFFFFF), PURPLE_HIGHLIGHT,
            PURPLE_HIGHLIGHT_TOP, PURPLE_HIGHLIGHT_SHADOW, 0xFFFFFFFF);

        public final String id, menuId, folder;
        public final Colors colors;
        public final int highlight, highlightTop, highlightShadow, highlightText;
        public final ThemePalette palette;
        public final ThemeSkin skin;
        public final ResourceLocation legacyStates, legacyTerminal;
        public final UITexture panel, inset, slotItem, slotFluid, scroller, scrollerDisabled, bigScroller,
            bigScrollerDisabled, textField, iconButton, iconButtonHover, tabButton, tabButtonHover, switchOff,
            switchOffHover, switchOn, switchOnHover, radioOff, radioOffHover, radioOn, radioOnHover, listRow,
            listRowSelected, buttonBar, progressEmpty, progressFull, invalid;

        Variant(String id, String menuId, String folder, Colors colors, int highlight, int highlightTop,
            int highlightShadow) {
            this(id, menuId, folder, colors, highlight, highlightTop, highlightShadow, colors.outline);
        }

        Variant(String id, String menuId, String folder, Colors colors, int highlight, int highlightTop,
            int highlightShadow, int highlightText) {
            this.id = id;
            this.menuId = menuId;
            this.folder = folder;
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
            scroller = adaptable(folder, "scroller", SCROLLBAR_WIDTH, SCROLLER_HEIGHT, 2, 2, 2, 3);
            scrollerDisabled = image(folder, "scroller_disabled", SCROLLBAR_WIDTH, SCROLLER_HEIGHT);
            bigScroller = image(folder, "big_scroller", BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
            bigScrollerDisabled = image(folder, "big_scroller_disabled", BIG_SCROLLER_WIDTH, SCROLLER_HEIGHT);
            textField = UITexture.builder()
                .location(texture(folder, "text_field"))
                .imageSize(FIELD_WIDTH, FIELD_WIDTH)
                .subAreaXYWH(0, 0, FIELD_WIDTH, TEXT_FIELD_HEIGHT)
                .adaptable(1, 3, 1, 1)
                .name("gtnrlib_" + folder + "_text_field")
                .build();
            iconButton = frame(folder, "icon_button", 176, 128);
            iconButtonHover = frame(folder, "icon_button_hover", 212, 128);
            tabButton = sprite(folder, "states", SHEET, 160, 192, TAB_BUTTON_SIZE, TAB_BUTTON_SIZE);
            tabButtonHover = sprite(folder, "states", SHEET, 160, 224, TAB_HOVER_SIZE, TAB_HOVER_SIZE);
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
            buttonBar = adaptable(folder, "button_bar", BUTTON_BAR_WIDTH, BUTTON_BAR_HEIGHT, 0, 2, 0, 4);
            progressEmpty = progressSprite(folder, "progress_empty", 0);
            progressFull = progressSprite(folder, "progress_full", PROGRESS_WIDTH);
            invalid = sprite(folder, "states", SHEET, 0, INVALID_Y, ICON_SIZE, ICON_SIZE);
        }

        public UITexture switchSprite(boolean on, boolean hovered) {
            return on ? hovered ? switchOnHover : switchOn : hovered ? switchOffHover : switchOff;
        }

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

        private static UITexture progressSprite(String folder, String name, int x) {
            return UITexture.builder()
                .location(texture(folder, "progress"))
                .imageSize(PROGRESS_WIDTH * 2, PROGRESS_HEIGHT)
                .subAreaXYWH(x, 0, PROGRESS_WIDTH, PROGRESS_HEIGHT)
                .name("gtnrlib_" + folder + "_" + name)
                .build();
        }

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

    public enum Icon {

        COPY(192, 0, GuiTextures.COPY),
        MORE(16, 0, GuiTextures.MORE),
        REFRESH(240, 0, GuiTextures.REFRESH),
        VISIBLE(16, 32, GuiTextures.VISIBLE),
        PLAY(48, 32, GuiTextures.PLAY),
        PAUSE(64, 32, GuiTextures.PAUSE),
        MOVE_TO(224, 32, GuiTextures.MOVE_TO),
        SEARCH(160, 48, GuiTextures.SEARCH),
        CHECKMARK(176, 48, GuiTextures.CHECKMARK),
        GEAR(0, 0, GuiTextures.GEAR),
        ADD(64, 0, GuiTextures.ADD),
        REMOVE(96, 0, GuiTextures.REMOVE),
        CLOSE(112, 16, GuiTextures.CLOSE),
        EDIT(80, 16, GuiTextures.EDIT),
        FILTER(128, 0, GuiTextures.FILTER);

        public final int x, y;
        public final UITexture vanilla;
        private final UITexture[] skins = new UITexture[Variant.values().length];

        Icon(int x, int y, UITexture vanilla) {
            this.x = x;
            this.y = y;
            this.vanilla = vanilla;
        }

        public UITexture texture(@Nullable Variant variant) {
            if (variant == null) return vanilla;
            UITexture skin = skins[variant.ordinal()];
            if (skin == null)
                skin = skins[variant.ordinal()] = sprite(variant.folder, "icons", SHEET, x, y, ICON_SIZE, ICON_SIZE);
            return skin;
        }
    }

    public static class Reloader {

        @SubscribeEvent
        public void onThemeReload(ReloadThemeEvent.Pre event) {
            register();
        }
    }
}
