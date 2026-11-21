package com.xyp.gtnrlib.gui.widget;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.menu.DropdownWidget;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Buttons;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Draw;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Panels;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * A dropdown whose menu stays inside the panel, follows values changed elsewhere and marks itself with a caret; after
 * {@link #wheel(boolean) wheel(true)} it also cycles with the mouse wheel while the pointer is over it. It can be built
 * on either side, so it also works in GUIs the server opens.
 * <p>
 * Under AE2 themes the shown value reads like a button label: text widgets in it that have no color of their own,
 * and the caret, use the button text color of the current state, and the shown value sinks with the button face on
 * hover. The sinking uses the shown widget's {@code transform} hook, so a transform set on it by
 * {@code optionToWidget} is replaced.
 * <p>
 * While {@link #available(BooleanSupplier)} reports false the dropdown looks like a disabled button, shows no hover,
 * closes its menu and ignores clicks and the wheel.
 */
public class CycleDropdown<T> extends DropdownWidget<T, CycleDropdown<T>> {

    /** Height of an {@link #iconOption} row and of its icon in pixels. */
    public static final int OPTION_HEIGHT = 16;
    private static final int MENU_EDGE = 1, VALUE_INSET = 3, CARET_SPACE = AE2Theme.CARET_WIDTH + 4;
    private final List<T> choices = new ObjectArrayList<>();
    private final IDrawable disabledLook = AE2Buttons.button(this, () -> false, () -> false);
    private BooleanSupplier available = () -> true;
    private T shown;
    private int menuWidth;
    private boolean wheel;

    public CycleDropdown(String panelName, Class<T> valueType) {
        super(panelName, valueType);
    }

    /**
     * Sets when the dropdown can be used.
     *
     * @param available supplier read every frame and on every input
     * @return this dropdown
     */
    public CycleDropdown<T> available(BooleanSupplier available) {
        this.available = available;
        return this;
    }

    /**
     * Sets whether the mouse wheel over the closed dropdown moves to the previous or next choice, wrapping around at
     * the ends. Off by default, so the wheel goes to the widgets around it, such as a scrolling list.
     *
     * @param wheel true to cycle one choice per wheel notch
     * @return this dropdown
     */
    public CycleDropdown<T> wheel(boolean wheel) {
        this.wheel = wheel;
        return this;
    }

    private boolean isAvailable() {
        return available.getAsBoolean();
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        return isAvailable() ? super.onMousePressed(mouseButton) : Result.STOP;
    }

    @Override
    public @Nullable IDrawable getThemeBackground(WidgetThemeEntry<?> widgetTheme, WidgetTheme activeTheme) {
        return isAvailable() ? super.getThemeBackground(widgetTheme, activeTheme) : disabledLook;
    }

    // MUI2 keeps the menu of a removed dropdown open, or open until its closing animation ends, and a click meanwhile
    // asks the removed dropdown for its parent.
    @Override
    public void dispose() {
        if (isValid() && isOpen()) closeMenu(false);
        super.dispose();
    }

    @Override
    protected void checkClose(boolean soft, boolean requireNoHover) {
        if (isValid()) super.checkClose(soft, requireNoHover);
    }

    @Override
    protected IWidget valueToWidget(T value, boolean forSelectedDisplay) {
        IWidget widget = super.valueToWidget(value, forSelectedDisplay);
        if (widget instanceof Widget<?>option) {
            option.margin(VALUE_INSET, forSelectedDisplay ? CARET_SPACE : VALUE_INSET, 0, 0);
            if (forSelectedDisplay) {
                option.transform((self, stack) -> stack.translate(0, labelSink()));
                followButtonText(option);
            }
        }
        return widget;
    }

    // The shown value is a plain text widget, which MUI2 colors with the panel text color rather than the button's.
    private void followButtonText(IWidget widget) {
        if (widget instanceof TextWidget<?>text && text.getColor() == null) text.color(this::labelColor);
        for (IWidget child : widget.getChildren()) followButtonText(child);
    }

    private int labelColor() {
        Variant variant = AE2Look.of(this)
            .variant();
        if (variant != null) return variant.colors.button(State.of(this, isAvailable(), false));
        if (!isAvailable()) return AE2Draw.panelTextColor(this, false);
        return getPanel().getTheme()
            .getFallback()
            .getTheme()
            .getTextColor();
    }

    private int labelSink() {
        if (!AE2Look.of(this)
            .isAE2()) return 0;
        int height = getArea().height;
        return AE2ButtonDrawable.textY(State.of(this, isAvailable(), false), height)
            - AE2ButtonDrawable.textY(State.NORMAL, height);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        int color = AE2Look.of(this)
            .isAE2() || !isAvailable() ? labelColor()
                : widgetTheme.getTheme()
                    .getTextColor();
        AE2Panels.drawCaret(
            getArea().width - CARET_SPACE + 1,
            (getArea().height - AE2Theme.CARET_HEIGHT) / 2 + labelSink(),
            isOpen(),
            color);
    }

    @Override
    protected void setValue(T value, boolean updateValue) {
        super.setValue(value, updateValue);
        shown = value;
    }

    // MUI2 only refreshes the shown option on its own selections; follow values changed from elsewhere too.
    @Override
    public void onUpdate() {
        super.onUpdate();
        IValue<T> value = getValue();
        if (value != null && isValid() && !Objects.equals(value.getValue(), shown)) setValue(value.getValue(), false);
        if (!isAvailable() && isValid() && isOpen()) closeMenu(false);
    }

    public CycleDropdown<T> choices(List<T> choices) {
        if (this.choices.equals(choices)) return this;
        this.choices.clear();
        this.choices.addAll(choices);
        clearOptions().options(choices);
        deleteMenu();
        return this;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        IValue<T> value = getValue();
        if (!wheel || value == null || choices.isEmpty() || !isAvailable()) return false;
        int index = Math.max(0, choices.indexOf(value.getValue()));
        setValue(choices.get(Math.floorMod(index - scrollDirection.modifier, choices.size())), true);
        return true;
    }

    /**
     * Makes the opened menu at least this wide so long option names fit; its right edge stays on the dropdown's
     * right edge, so a dropdown at the right of a panel does not push the menu past it.
     *
     * @param width menu width in pixels; zero keeps the dropdown's own width
     * @return this dropdown
     */
    public CycleDropdown<T> menuWidth(int width) {
        menuWidth = width;
        deleteMenu();
        return this;
    }

    /**
     * Creates an option row for {@link #optionToWidget}: an optional item icon, then the name, which scrolls while
     * hovered when it is too long.
     *
     * @param icon item drawn as a {@value #OPTION_HEIGHT}-pixel icon, or {@code null} for none
     * @param name option name
     * @return the row, {@value #OPTION_HEIGHT} pixels high and as wide as the dropdown
     */
    public static IWidget iconOption(@Nullable ItemStack icon, String name) {
        return iconOption(icon, name, Alignment.CenterLeft);
    }

    /**
     * Creates an option row like {@link #iconOption(ItemStack, String)} with the name placed by the given alignment
     * in the space right of the icon, such as {@link Alignment#Center} for centred tier names. Formatting codes in the
     * name, such as GT's tier colors, are kept.
     *
     * @param icon      item drawn as a {@value #OPTION_HEIGHT}-pixel icon, or {@code null} for none
     * @param name      option name
     * @param alignment placement of the name
     * @return the row, {@value #OPTION_HEIGHT} pixels high and as wide as the dropdown
     */
    public static IWidget iconOption(@Nullable ItemStack icon, String name, Alignment alignment) {
        Flow row = Flow.row()
            .widthRel(1f)
            .height(OPTION_HEIGHT)
            .childPadding(2);
        if (icon != null) row.child(
            new ItemDrawable(icon).asWidget()
                .size(OPTION_HEIGHT, OPTION_HEIGHT));
        return row.child(
            new ScrollingTextWidget(IKey.str(name)).alignment(alignment)
                .expanded()
                .heightRel(1f));
    }

    @Override
    protected Menu<?> createMenu() {
        Menu<?> menu = super.createMenu();
        if (menuWidth > getArea().width) menu.width(menuWidth)
            .right(0);
        // MUI2 menus draw their options over the panel border, so keep the options inside it.
        if (AE2Look.of(this)
            .isAE2())
            menu.padding(AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_BOTTOM_EDGE);
        else menu.padding(MENU_EDGE);
        return menu;
    }
}
