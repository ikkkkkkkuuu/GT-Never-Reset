package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DrawableSerialization;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.utils.JsonHelper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.google.gson.JsonObject;
import com.xyp.gtnrlib.utils.TextRenderUtils;

public class SlideSwitch extends ButtonWidget<SlideSwitch> {

    private static final int LABEL_Y = 1, LABEL_END = 2;
    private static final float INACTIVE_ALPHA = 0.5F;

    private BooleanSupplier on = () -> false, live = () -> true, available = () -> true;
    private IKey label = IKey.EMPTY;

    public SlideSwitch value(BooleanSupplier on) {
        this.on = on;
        return this;
    }

    public SlideSwitch liveWhen(BooleanSupplier live) {
        this.live = live;
        return this;
    }

    public SlideSwitch availableWhen(BooleanSupplier available) {
        this.available = available;
        return this;
    }

    public SlideSwitch label(IKey label) {
        this.label = label;
        return this;
    }

    @Override
    public WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        WidgetThemeEntry<Theme> entry = theme.getWidgetTheme(Theme.KEY);
        return entry != null ? entry : theme.getFallback();
    }

    public static Theme colors(WidgetTheme theme) {
        return theme instanceof Theme colors ? colors : Theme.KEY.getDefaultValue();
    }

    public static int widthFor(int textWidth) {
        return widthFor(LayoutTheme.DEFAULT, textWidth);
    }

    public static int widthFor(LayoutTheme style, int textWidth) {
        return labelX(style) + textWidth + LABEL_END;
    }

    private static int labelX(LayoutTheme style) {
        return style.switchWidth + style.switchGap;
    }

    public static int trackColor(Theme colors, boolean on, boolean lit) {
        return !on ? colors.track : lit ? colors.trackOn : colors.trackDim;
    }

    public static int textColor(Theme colors, boolean on, boolean lit, boolean available) {
        if (!on) return available ? colors.textOff : colors.textDim;
        return lit ? colors.textOn : colors.textDim;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        Theme colors = colors(widgetTheme.getTheme());
        LayoutTheme style = LayoutTheme.of(context.getTheme());
        boolean on = this.on.getAsBoolean(), open = available.getAsBoolean(), lit = on && open && live.getAsBoolean();
        int width = getArea().w(), height = getArea().h(), switchWidth = style.switchWidth,
            switchHeight = style.switchHeight, top = (height - switchHeight) / 2, labelX = labelX(style);
        UITexture sprite = colors.sprite(on, open && isHovering());
        if (sprite != null) {
            GlStateManager.color(1, 1, 1, open ? 1 : INACTIVE_ALPHA);
            sprite.draw(0, top, switchWidth, switchHeight);
            GlStateManager.color(1, 1, 1, 1);
        } else {
            int knobX = on ? switchWidth - switchHeight : 0, knob = on ? colors.knob : colors.knobOff;
            if (colors.trackSkin != null)
                colors.trackSkin.draw(0, top, switchWidth, switchHeight, on ? trackColor(colors, true, lit) : 0);
            else GuiDraw.drawRect(0, top, switchWidth, switchHeight, trackColor(colors, on, lit));
            if (colors.knobSkin != null) colors.knobSkin.draw(knobX, top, switchHeight, switchHeight, knob);
            else GuiDraw.drawRect(knobX, top, switchHeight, switchHeight, knob);
        }
        TextRenderUtils.drawTextFitted(
            label.get(),
            labelX,
            LABEL_Y,
            width - labelX,
            height,
            Alignment.CenterLeft,
            textColor(colors, on, lit, open),
            lit && colors.getTextShadow(),
            style.textScale);
    }

    public static class Theme extends WidgetTheme {

        public static final String TRACK = "trackColor", TRACK_ON = "trackOnColor", TRACK_DIM = "trackDimColor",
            KNOB = "knobColor", KNOB_OFF = "knobOffColor", TEXT_ON = "textOnColor", TEXT_DIM = "textDimColor",
            TEXT_OFF = "textOffColor", TRACK_SKIN = "trackSkin", KNOB_SKIN = "knobSkin", OFF_SPRITE = "offSprite",
            OFF_HOVER_SPRITE = "offHoverSprite", ON_SPRITE = "onSprite", ON_HOVER_SPRITE = "onHoverSprite";
        private static final int DEFAULT_SIZE = 18;
        public static final WidgetThemeKey<Theme> KEY = register();

        public final int track, trackOn, trackDim, knob, knobOff, textOn, textDim, textOff;
        public final SkinDrawable trackSkin, knobSkin;
        public final @Nullable UITexture offSprite, offHoverSprite, onSprite, onHoverSprite;

        public Theme(@Nullable IDrawable background, int track, int trackOn, int trackDim, int knob, int knobOff,
            int textOn, int textDim, int textOff) {
            super(DEFAULT_SIZE, DEFAULT_SIZE, background, Color.WHITE.main, textOn, true, Color.WHITE.main);
            this.track = track;
            this.trackOn = trackOn;
            this.trackDim = trackDim;
            this.knob = knob;
            this.knobOff = knobOff;
            this.textOn = textOn;
            this.textDim = textDim;
            this.textOff = textOff;
            this.trackSkin = null;
            this.knobSkin = null;
            this.offSprite = null;
            this.offHoverSprite = null;
            this.onSprite = null;
            this.onHoverSprite = null;
        }

        public Theme(Theme parent, JsonObject json, JsonObject fallback) {
            super(parent, json, fallback);
            track = JsonHelper.getColor(json, parent.track, TRACK);
            trackOn = JsonHelper.getColor(json, parent.trackOn, TRACK_ON);
            trackDim = JsonHelper.getColor(json, parent.trackDim, TRACK_DIM);
            knob = JsonHelper.getColor(json, parent.knob, KNOB);
            knobOff = JsonHelper.getColor(json, parent.knobOff, KNOB_OFF);
            textOn = JsonHelper.getColor(json, parent.textOn, TEXT_ON);
            textDim = JsonHelper.getColor(json, parent.textDim, TEXT_DIM);
            textOff = JsonHelper.getColor(json, parent.textOff, TEXT_OFF);
            trackSkin = SkinDrawable.read(json, TRACK_SKIN, parent.trackSkin);
            knobSkin = SkinDrawable.read(json, KNOB_SKIN, parent.knobSkin);
            offSprite = sprite(json, OFF_SPRITE, parent.offSprite);
            offHoverSprite = sprite(json, OFF_HOVER_SPRITE, parent.offHoverSprite);
            onSprite = sprite(json, ON_SPRITE, parent.onSprite);
            onHoverSprite = sprite(json, ON_HOVER_SPRITE, parent.onHoverSprite);
        }

        public @Nullable UITexture sprite(boolean on, boolean hovered) {
            UITexture sprite = on ? hovered ? onHoverSprite : onSprite : hovered ? offHoverSprite : offSprite;
            return sprite != null ? sprite : on ? onSprite : offSprite;
        }

        private static @Nullable UITexture sprite(JsonObject json, String key, @Nullable UITexture fallback) {
            if (!json.has(key)) return fallback;
            return DrawableSerialization.deserialize(json.get(key)) instanceof UITexture sprite ? sprite : fallback;
        }

        private static WidgetThemeKey<Theme> register() {
            int[] colors = { 0xFF3A3A3A, 0xFFDDDDDD, 0x80DDDDDD, 0xFFF2F2F2, 0xFF8C8C8C, 0xFFF4F4F4, 0xFF6E6E6E,
                0xFF9A9A9A };
            return IThemeApi.get()
                .widgetThemeKeyBuilder("gtnrlibSlideSwitch", Theme.class)
                .defaultTheme(of(null, colors))
                .defaultHoverTheme(of(new Rectangle().color(0x26FFFFFF), colors))
                .parser(Theme::new)
                .register();
        }

        private static Theme of(@Nullable IDrawable background, int[] colors) {
            return new Theme(
                background,
                colors[0],
                colors[1],
                colors[2],
                colors[3],
                colors[4],
                colors[5],
                colors[6],
                colors[7]);
        }

        public static class Builder extends WidgetThemeBuilder<Theme, Builder> {

            public Builder track(int off, int on, int dim) {
                add(TRACK, off);
                add(TRACK_ON, on);
                add(TRACK_DIM, dim);
                return this;
            }

            public Builder knob(int on, int off) {
                add(KNOB, on);
                add(KNOB_OFF, off);
                return this;
            }

            public Builder skins(SkinDrawable track, SkinDrawable knob) {
                SkinDrawable.write(this, TRACK_SKIN, track);
                SkinDrawable.write(this, KNOB_SKIN, knob);
                return this;
            }

            public Builder sprites(UITexture off, UITexture offHover, UITexture on, UITexture onHover) {
                add(OFF_SPRITE, DrawableSerialization.serialize(off));
                add(OFF_HOVER_SPRITE, DrawableSerialization.serialize(offHover));
                add(ON_SPRITE, DrawableSerialization.serialize(on));
                add(ON_HOVER_SPRITE, DrawableSerialization.serialize(onHover));
                return this;
            }

            public Builder text(int on, int dim, int off) {
                add(TEXT_ON, on);
                add(TEXT_DIM, dim);
                add(TEXT_OFF, off);
                return this;
            }
        }
    }
}
