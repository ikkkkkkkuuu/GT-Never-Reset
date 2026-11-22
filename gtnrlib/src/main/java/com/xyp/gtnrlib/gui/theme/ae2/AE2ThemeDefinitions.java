package com.xyp.gtnrlib.gui.theme.ae2;

import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_HEIGHT;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_LABEL_X;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.CHECKBOX_WIDTH;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.FIELD_SELECTION;
import static com.xyp.gtnrlib.gui.theme.AE2Theme.STYLE;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.theme.SelectableTheme;
import com.cleanroommc.modularui.theme.ThemeAPI;
import com.cleanroommc.modularui.theme.ThemeBuilder;
import com.cleanroommc.modularui.utils.JsonBuilder;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Colors;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ButtonPurpose;
import com.xyp.gtnrlib.gui.theme.PaletteThemes;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;
import com.xyp.gtnrlib.gui.widget.LayoutTheme;
import com.xyp.gtnrlib.gui.widget.SlideSwitch;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The MUI2 theme definitions of the AE2 skins: what each skin's panel theme and context-menu theme give MUI2's own
 * widgets and GTNRLib's theme-data widgets. {@link AE2Theme#register()} registers them; other code selects a theme by
 * its identifier and has no need to build one.
 * <p>
 * Client only, like {@link PaletteThemes} which it builds on: the class does not exist on a dedicated server, so a call
 * from there fails at once instead of going unnoticed.
 */
@SideOnly(Side.CLIENT)
public class AE2ThemeDefinitions {

    // IDrawable.EMPTY is saved as JSON null, which the theme loader reads as unset and replaces with the parent
    // theme's framed button; a transparent rectangle keeps menu options flat.
    private static final IDrawable FLAT_OPTION = new Rectangle().color(0);

    private AE2ThemeDefinitions() {}

    /**
     * Builds the context-menu theme of a skin, registered as {@link Variant#menuId}: the skin's panel theme with
     * flat menu options that take the selected list row as their hover and selected background.
     *
     * @param variant AE2 skin
     * @return a fresh theme definition, ready to register
     */
    public static ThemeBuilder<?> menuTheme(Variant variant) {
        Colors colors = variant.colors;
        return new ThemeBuilder<>(variant.menuId).parent(variant.id)
            .background(IThemeApi.PANEL, variant.panel)
            .background(IThemeApi.BUTTON, FLAT_OPTION)
            .hoverBackground(IThemeApi.BUTTON, variant.listRowSelected)
            .textColor(IThemeApi.BUTTON, colors.text())
            .textHoverColor(IThemeApi.BUTTON, colors.hoverText())
            .widgetTheme(
                IThemeApi.TOGGLE_BUTTON,
                new SelectableTheme.Builder<>().background(FLAT_OPTION)
                    .textColor(colors.text())
                    .selectedBackground(variant.listRowSelected)
                    .selectedTextColor(colors.hoverText()))
            .widgetThemeHover(
                IThemeApi.TOGGLE_BUTTON,
                new SelectableTheme.Builder<>().background(variant.listRowSelected)
                    .textColor(colors.hoverText())
                    .selectedBackground(variant.listRowSelected)
                    .selectedTextColor(colors.hoverText()));
    }

    /**
     * Builds the panel theme of a skin, registered as {@link Variant#id}: the skin's palette and textures for
     * panels, buttons, slots, text fields and scroll bars, the theme data of GTNRLib's own widgets, and the
     * {@link AE2Look#KEY} entry that names the skin.
     *
     * @param variant AE2 skin
     * @return a fresh theme definition, ready to register
     */
    public static ThemeBuilder<?> panelTheme(Variant variant) {
        Colors colors = variant.colors;
        IDrawable button = new AE2ButtonDrawable(State.NORMAL, variant.folder),
            lit = new AE2ButtonDrawable(State.HOVER, variant.folder);
        int normal = colors.buttonText(), hover = colors.hoverText();
        ThemeBuilder<?> theme = PaletteThemes
            .build(variant.id, ThemeAPI.DEFAULT_ID, variant.palette, STYLE, variant.skin)
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
            .background(IThemeApi.FLUID_SLOT, variant.slotFluid)
            .background(IThemeApi.TEXT_FIELD, variant.textField)
            .textColor(IThemeApi.TEXT_FIELD, colors.fieldText())
            .textFieldHintColor(colors.placeholder())
            .textFieldMarkedColor(FIELD_SELECTION)
            .background(IThemeApi.SCROLLBAR, variant.scroller)
            .defaultWidth(IThemeApi.SCROLLBAR, AE2Theme.SCROLLBAR_WIDTH)
            .widgetTheme(
                SlideSwitch.Theme.KEY,
                new SlideSwitch.Theme.Builder().text(colors.text(), colors.muted(), colors.text())
                    .sprites(
                        variant.checkboxOff,
                        variant.checkboxOffHover,
                        variant.checkboxOn,
                        variant.checkboxOnHover))
            .hoverBackground(SlideSwitch.Theme.KEY, IDrawable.EMPTY)
            .widgetTheme(AE2Look.KEY, new AE2Look.Marker.Builder().skin(variant.folder))
            .widgetTheme(
                LayoutTheme.KEY,
                new LayoutTheme.Builder().values(STYLE.layout())
                    .switches(CHECKBOX_WIDTH, CHECKBOX_HEIGHT, CHECKBOX_LABEL_X - CHECKBOX_WIDTH))
            .background(ButtonPurpose.PRIMARY.key, lit)
            .hoverBackground(ButtonPurpose.PRIMARY.key, lit)
            .textColor(ButtonPurpose.PRIMARY.key, hover)
            .textHoverColor(ButtonPurpose.PRIMARY.key, hover)
            .textShadow(ButtonPurpose.PRIMARY.key, false)
            .textHoverShadow(ButtonPurpose.PRIMARY.key, false)
            .background(ButtonPurpose.DANGER.key, button)
            .hoverBackground(ButtonPurpose.DANGER.key, lit)
            .textColor(ButtonPurpose.DANGER.key, colors.bad())
            .textHoverColor(ButtonPurpose.DANGER.key, colors.bad())
            .background(ButtonPurpose.ICON.key, variant.iconButton)
            .hoverBackground(ButtonPurpose.ICON.key, variant.iconButtonHover);
        theme.mergeAdd(
            IThemeApi.TOGGLE_BUTTON.getFullName(),
            new JsonBuilder().add(IThemeApi.SELECTED_TEXT_SHADOW, false));
        PaletteThemes.decorate(theme, variant.palette);
        return theme;
    }
}
