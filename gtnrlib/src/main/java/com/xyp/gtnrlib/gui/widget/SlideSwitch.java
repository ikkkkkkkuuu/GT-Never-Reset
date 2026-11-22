package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.NotNull;
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
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2CheckboxPainter;
import com.xyp.gtnrlib.utils.TextRenderUtils;

/**
 * An on/off switch with a label, drawn from theme data ({@link Theme}): a track and a knob in the theme's colors or
 * skins, or, when the theme supplies sprites as the AE2 themes do, AE2's check box sprites with the label wrapped and
 * placed as in AE2's check boxes. The caller owns the value and flips it in the click handler. While it is not
 * {@link #available(BooleanSupplier) available} it is drawn faded, shows no hover and swallows clicks.
 */
public class SlideSwitch extends ButtonWidget<SlideSwitch> {

    private static final int LABEL_Y = 1, LABEL_END = 2;

    private final AE2CheckboxPainter.CheckboxHover hover = new AE2CheckboxPainter.CheckboxHover();
    private final Availability availability = new Availability();
    private BooleanSupplier on = () -> false, glow = () -> true;
    private IKey label = IKey.EMPTY;

    /**
     * Sets the value the switch shows.
     *
     * @param on supplier of the on state, read every frame
     * @return this switch
     */
    public SlideSwitch value(BooleanSupplier on) {
        this.on = on;
        return this;
    }

    /**
     * Sets when a switch that is on shows its bright look; otherwise it is drawn on but dimmed, such as a module that
     * is switched on but not running. It does not change whether the switch can be used.
     *
     * @param glow supplier read every frame
     * @return this switch
     */
    public SlideSwitch glowWhen(BooleanSupplier glow) {
        this.glow = glow;
        return this;
    }

    /**
     * Same as {@link #glowWhen(BooleanSupplier)}.
     *
     * @param glow supplier read every frame
     * @return this switch
     * @deprecated use {@link #glowWhen(BooleanSupplier)}; this name reads like "can be used", which
     *             {@link #available(BooleanSupplier)} sets
     */
    @Deprecated
    public SlideSwitch liveWhen(BooleanSupplier glow) {
        return glowWhen(glow);
    }

    /**
     * Sets when the switch can be used. An unavailable switch is drawn faded, shows no hover and swallows clicks.
     *
     * @param available supplier read every frame and on every click
     * @return this switch
     */
    public SlideSwitch available(BooleanSupplier available) {
        availability.set(available);
        return this;
    }

    /**
     * Same as {@link #available(BooleanSupplier)}.
     *
     * @param available supplier read every frame and on every click
     * @return this switch
     * @deprecated use {@link #available(BooleanSupplier)}, the name every GTNRLib widget uses
     */
    @Deprecated
    public SlideSwitch availableWhen(BooleanSupplier available) {
        return available(available);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        return availability.press(() -> super.onMousePressed(mouseButton));
    }

    /**
     * Sets the label drawn right of the switch.
     *
     * @param label label text, read every frame
     * @return this switch
     */
    public SlideSwitch label(IKey label) {
        this.label = label;
        return this;
    }

    @Override
    public WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        WidgetThemeEntry<Theme> entry = theme.getWidgetTheme(Theme.KEY);
        return entry != null ? entry : theme.getFallback();
    }

    /**
     * Returns the switch theme data behind a widget theme.
     *
     * @param theme widget theme a switch is drawn with
     * @return the theme itself if it is switch theme data, else the default switch theme
     */
    public static Theme colors(WidgetTheme theme) {
        return theme instanceof Theme colors ? colors : Theme.KEY.getDefaultValue();
    }

    /**
     * Returns the width a switch needs for a label of the given width, with the default layout.
     *
     * @param textWidth label width in pixels
     * @return widget width in pixels
     */
    public static int widthFor(int textWidth) {
        return widthFor(LayoutTheme.DEFAULT, textWidth);
    }

    /**
     * Returns the width a switch needs for a label of the given width: the switch, the gap and the label.
     *
     * @param style     layout of the theme the switch is drawn with, which sets the switch width and gap
     * @param textWidth label width in pixels
     * @return widget width in pixels
     */
    public static int widthFor(LayoutTheme style, int textWidth) {
        return labelX(style) + textWidth + LABEL_END;
    }

    private static int labelX(LayoutTheme style) {
        return style.switchWidth + style.switchGap;
    }

    /**
     * Picks the track color for a switch state.
     *
     * @param colors switch theme data
     * @param on     whether the switch is on
     * @param lit    whether an on switch shows its bright look
     * @return ARGB color
     */
    public static int trackColor(Theme colors, boolean on, boolean lit) {
        return !on ? colors.track : lit ? colors.trackOn : colors.trackDim;
    }

    /**
     * Picks the label color for a switch state.
     *
     * @param colors    switch theme data
     * @param on        whether the switch is on
     * @param lit       whether an on switch shows its bright look
     * @param available whether the switch can be used
     * @return ARGB color
     */
    public static int textColor(Theme colors, boolean on, boolean lit, boolean available) {
        if (!on) return available ? colors.textOff : colors.textDim;
        return lit ? colors.textOn : colors.textDim;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        Theme colors = colors(widgetTheme.getTheme());
        LayoutTheme style = LayoutTheme.of(getPanel().getTheme());
        boolean on = this.on.getAsBoolean(), open = availability.get(), lit = on && open && glow.getAsBoolean();
        int width = getArea().w(), height = getArea().h(), switchWidth = style.switchWidth,
            switchHeight = style.switchHeight, top = (height - switchHeight) / 2, labelX = labelX(style);
        boolean hovered = hover.lit(isHovering(), on);
        UITexture sprite = colors.sprite(on, open && hovered);
        if (sprite != null) {
            GlStateManager.color(1, 1, 1, open ? 1 : AE2Theme.INACTIVE_ALPHA);
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
        int color = textColor(colors, on, lit, open);
        boolean shadow = lit && colors.getTextShadow();
        if (sprite != null) AE2CheckboxPainter.checkboxLabel(label.get(), labelX, top, width - labelX, color, shadow);
        else TextRenderUtils.drawTextFitted(
            label.get(),
            labelX,
            LABEL_Y,
            width - labelX,
            height,
            Alignment.CenterLeft,
            color,
            shadow,
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
