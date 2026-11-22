package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.ICON_SIZE;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.SHEET;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * AE2 1.21's own 16-by-16 setting icons, such as the redstone modes, fuzzy modes, access modes and power units, with
 * the names and sheet positions of AE2's {@code Icon} enum. Use them on a {@code SettingToggleButton}, an
 * {@code IconButton} or a toolbar button for settings that AE2 has too, so they look like AE2's.
 * <p>
 * They are drawn from the {@code states.png} sheet of the panel's AE2 skin, which a resource pack can repaint per skin,
 * and from the light skin's sheet outside AE2 themes, where they are white like MUI2's icons. AE2 1.21 leaves twenty of
 * its icon cells as placeholders in that sheet (the search, type filter, substitution, level and white/black list
 * icons); those have no constant here. {@link Icon} holds the general actions such as add, refresh and close.
 */
public enum AE2SettingIcon implements IconSource {

    REDSTONE_LOW(0, 0),
    REDSTONE_HIGH(16, 0),
    REDSTONE_PULSE(32, 0),
    REDSTONE_IGNORE(48, 0),
    REDSTONE_OFF(64, 0),
    REDSTONE_ON(80, 0),
    REDSTONE_ABOVE_EQUAL(192, 0),
    REDSTONE_BELOW(208, 0),
    CLEAR(96, 0),
    ENTER(112, 0),
    WHITE_ARROW_DOWN(128, 0),
    LOCKED(144, 0),
    UNLOCKED(160, 0),
    HELP(176, 0),
    VIEW_MODE_STORED(0, 16),
    VIEW_MODE_ALL(32, 16),
    VIEW_MODE_CRAFTING(48, 16),
    BLOCKING_MODE_NO(64, 16),
    BLOCKING_MODE_YES(80, 16),
    BACK(96, 16),
    TRANSPARENT_FACADES_OFF(96, 16),
    TYPE_FILTER_ALL(160, 16),
    SEARCH_AUTO_FOCUS(48, 32),
    TAB_CRAFTING(0, 32),
    TAB_PROCESSING(16, 32),
    TAB_SMITHING(32, 32),
    TAB_STONECUTTING(48, 32),
    ARROW_UP(0, 48),
    ARROW_DOWN(16, 48),
    ARROW_RIGHT(32, 48),
    ARROW_LEFT(48, 48),
    STORAGE_FILTER_EXTRACTABLE_ONLY(80, 48),
    STORAGE_FILTER_EXTRACTABLE_NONE(96, 48),
    FILTER_ON_EXTRACT_ENABLED(160, 48),
    FILTER_ON_EXTRACT_DISABLED(176, 48),
    SORT_BY_NAME(0, 64),
    SORT_BY_AMOUNT(16, 64),
    COG(32, 64),
    COG_DISABLED(48, 64),
    SORT_BY_MOD(96, 64),
    PRIORITY(144, 64),
    FULLNESS_EMPTY(0, 80),
    FULLNESS_HALF(16, 80),
    FULLNESS_FULL(32, 80),
    PATTERN_ACCESS_SHOW(64, 80),
    PATTERN_ACCESS_HIDE(80, 80),
    PATTERN_TERMINAL_VISIBLE(96, 80),
    PATTERN_TERMINAL_ALL(112, 80),
    PATTERN_TERMINAL_NOT_FULL(128, 80),
    FUZZY_PERCENT_25(0, 96),
    FUZZY_PERCENT_50(16, 96),
    FUZZY_PERCENT_75(32, 96),
    FUZZY_PERCENT_99(48, 96),
    FUZZY_IGNORE(64, 96),
    INSCRIBER_SEPARATE_SIDES(80, 96),
    INSCRIBER_COMBINED_SIDES(96, 96),
    AUTO_EXPORT_OFF(112, 96),
    AUTO_EXPORT_ON(128, 96),
    INSCRIBER_BUFFER_4(144, 96),
    INSCRIBER_BUFFER_64(160, 96),
    INSCRIBER_BUFFER_1(176, 96),
    CONDENSER_OUTPUT_TRASH(0, 112),
    CONDENSER_OUTPUT_MATTER_BALL(16, 112),
    CONDENSER_OUTPUT_SINGULARITY(32, 112),
    INVALID(0, 128),
    ACCESS_WRITE(0, 144),
    ACCESS_READ(16, 144),
    ACCESS_READ_WRITE(32, 144),
    CRAFT_HAMMER(48, 144),
    POWER_UNIT_AE(0, 160),
    POWER_UNIT_EU(16, 160),
    POWER_UNIT_J(32, 160),
    POWER_UNIT_W(48, 160),
    POWER_UNIT_RF(64, 160),
    POWER_UNIT_TR(80, 160),
    COPY_MODE_ON(80, 176),
    COPY_MODE_OFF(80, 192),
    TERMINAL_STYLE_SMALL(0, 208),
    TERMINAL_STYLE_MEDIUM(16, 208),
    TERMINAL_STYLE_TALL(32, 208),
    TERMINAL_STYLE_FULL(48, 208),
    PLACEMENT_BLOCK(0, 224),
    PLACEMENT_ITEM(16, 224),
    SCHEDULING_DEFAULT(0, 240),
    SCHEDULING_ROUND_ROBIN(16, 240),
    SCHEDULING_RANDOM(32, 240),
    OVERLAY_OFF(48, 240),
    OVERLAY_ON(64, 240);

    private static final String SHEET_NAME = "states";
    /** Position of the icon in AE2's {@code states.png}, in pixels. */
    public final int x, y;
    private final UITexture[] skins = new UITexture[Variant.values().length];

    AE2SettingIcon(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Resolves this icon's texture in a look, caching it for each skin.
     *
     * @param look look to draw in; the plain look uses the light skin's sheet
     * @return icon texture
     */
    public UITexture texture(AE2Look look) {
        Variant variant = look.isAE2() ? look.variant() : Variant.LIGHT;
        UITexture skin = skins[variant.ordinal()];
        if (skin == null) skin = skins[variant.ordinal()] = Variant
            .sprite(variant.folder, SHEET_NAME, SHEET, x, y, ICON_SIZE, ICON_SIZE);
        return skin;
    }

    @Override
    public void drawIcon(GuiContext context, WidgetTheme theme, Variant variant, int x, int y, boolean enabled) {
        drawInLook(context, theme, AE2Look.of(variant), x, y, enabled);
    }

    @Override
    public void drawInLook(GuiContext context, WidgetTheme theme, AE2Look look, int x, int y, boolean enabled) {
        AE2Draw.drawSprite(texture(look), x, y, enabled);
    }
}
