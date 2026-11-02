package com.xyp.gtnrlib.gui.widget;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.JsonHelper;
import com.google.gson.JsonObject;

public class LayoutTheme extends WidgetTheme {

    public static final String SPACING_SMALL = "spacingSmall", SPACING_MEDIUM = "spacingMedium",
        SPACING_LARGE = "spacingLarge", PADDING = "padding", BUTTON_HEIGHT = "buttonHeight", ROW_HEIGHT = "rowHeight",
        TEXT_SCALE = "textScale", SWITCH_WIDTH = "switchWidth", SWITCH_HEIGHT = "switchHeight",
        SWITCH_GAP = "switchGap";
    private static final int DEFAULT_SPACING_SMALL = 2, DEFAULT_SPACING_MEDIUM = 4, DEFAULT_SPACING_LARGE = 10,
        DEFAULT_PADDING = 10, DEFAULT_BUTTON_HEIGHT = 14, DEFAULT_ROW_HEIGHT = 16, DEFAULT_SWITCH_WIDTH = 12,
        DEFAULT_SWITCH_HEIGHT = 6, DEFAULT_SWITCH_GAP = 3;
    private static final float DEFAULT_TEXT_SCALE = 1F;
    public static final LayoutTheme DEFAULT = new LayoutTheme(
        DEFAULT_SPACING_SMALL,
        DEFAULT_SPACING_MEDIUM,
        DEFAULT_SPACING_LARGE,
        DEFAULT_PADDING,
        DEFAULT_BUTTON_HEIGHT,
        DEFAULT_ROW_HEIGHT,
        DEFAULT_TEXT_SCALE);
    public static final WidgetThemeKey<LayoutTheme> KEY = IThemeApi.get()
        .widgetThemeKeyBuilder("gtnrlibLayout", LayoutTheme.class)
        .defaultTheme(DEFAULT)
        .defaultHoverTheme(DEFAULT)
        .parser(LayoutTheme::new)
        .register();

    public final int spacingSmall, spacingMedium, spacingLarge, padding, buttonHeight, rowHeight, switchWidth,
        switchHeight, switchGap;
    public final float textScale;

    public LayoutTheme(int spacingSmall, int spacingMedium, int spacingLarge, int padding, int buttonHeight,
        int rowHeight, float textScale) {
        this(
            spacingSmall,
            spacingMedium,
            spacingLarge,
            padding,
            buttonHeight,
            rowHeight,
            textScale,
            DEFAULT_SWITCH_WIDTH,
            DEFAULT_SWITCH_HEIGHT,
            DEFAULT_SWITCH_GAP);
    }

    public LayoutTheme(int spacingSmall, int spacingMedium, int spacingLarge, int padding, int buttonHeight,
        int rowHeight, float textScale, int switchWidth, int switchHeight, int switchGap) {
        super(0, 0, null, Color.WHITE.main, Color.WHITE.main, false, Color.WHITE.main);
        this.spacingSmall = spacingSmall;
        this.spacingMedium = spacingMedium;
        this.spacingLarge = spacingLarge;
        this.padding = padding;
        this.buttonHeight = buttonHeight;
        this.rowHeight = rowHeight;
        this.textScale = textScale;
        this.switchWidth = switchWidth;
        this.switchHeight = switchHeight;
        this.switchGap = switchGap;
    }

    public LayoutTheme(LayoutTheme parent, JsonObject json, JsonObject fallback) {
        super(parent, json, fallback);
        spacingSmall = Math.max(0, JsonHelper.getInt(json, parent.spacingSmall, SPACING_SMALL));
        spacingMedium = Math.max(0, JsonHelper.getInt(json, parent.spacingMedium, SPACING_MEDIUM));
        spacingLarge = Math.max(0, JsonHelper.getInt(json, parent.spacingLarge, SPACING_LARGE));
        padding = Math.max(0, JsonHelper.getInt(json, parent.padding, PADDING));
        buttonHeight = Math.max(1, JsonHelper.getInt(json, parent.buttonHeight, BUTTON_HEIGHT));
        rowHeight = Math.max(buttonHeight, JsonHelper.getInt(json, parent.rowHeight, ROW_HEIGHT));
        float scale = JsonHelper.getFloat(json, parent.textScale, TEXT_SCALE);
        textScale = scale > 0 ? scale : parent.textScale;
        switchWidth = Math.max(1, JsonHelper.getInt(json, parent.switchWidth, SWITCH_WIDTH));
        switchHeight = Math.max(1, JsonHelper.getInt(json, parent.switchHeight, SWITCH_HEIGHT));
        switchGap = Math.max(0, JsonHelper.getInt(json, parent.switchGap, SWITCH_GAP));
    }

    public static LayoutTheme of(ITheme theme) {
        WidgetThemeEntry<LayoutTheme> entry = theme == null ? null : theme.getWidgetTheme(KEY);
        return entry == null ? DEFAULT : entry.getTheme();
    }

    public int scaled(int textWidth) {
        return (int) Math.ceil(textWidth * textScale);
    }

    public static class Builder extends WidgetThemeBuilder<LayoutTheme, Builder> {

        public Builder values(LayoutTheme values) {
            add(SPACING_SMALL, values.spacingSmall);
            add(SPACING_MEDIUM, values.spacingMedium);
            add(SPACING_LARGE, values.spacingLarge);
            add(PADDING, values.padding);
            add(BUTTON_HEIGHT, values.buttonHeight);
            add(ROW_HEIGHT, values.rowHeight);
            add(TEXT_SCALE, values.textScale);
            if (values.switchWidth != DEFAULT_SWITCH_WIDTH || values.switchHeight != DEFAULT_SWITCH_HEIGHT
                || values.switchGap != DEFAULT_SWITCH_GAP)
                switches(values.switchWidth, values.switchHeight, values.switchGap);
            return this;
        }

        public Builder switches(int width, int height, int gap) {
            add(SWITCH_WIDTH, width);
            add(SWITCH_HEIGHT, height);
            add(SWITCH_GAP, gap);
            return this;
        }
    }
}
