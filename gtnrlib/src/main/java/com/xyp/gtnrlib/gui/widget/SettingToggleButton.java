package com.xyp.gtnrlib.gui.widget;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.api.value.IIntValue;
import com.cleanroommc.modularui.widget.sizer.Unit.Measure;
import com.cleanroommc.modularui.widgets.AbstractCycleButtonWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.ae2.AE2ButtonPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;

/**
 * AE2's setting button: a toolbar button ({@link AE2ButtonPainter#iconButton}) that shows one icon per value
 * and steps to the next value on a left click and to the previous one on a right click, like AE2's
 * {@code SettingToggleButton}. With a boolean value it is AE2's two-icon {@code ToggleButton}, where either click flips
 * it. Its tooltip is the {@link #title(IKey) title} followed by the description of the current value.
 * <p>
 * It is MUI2's cycle button, so it takes an {@code IIntValue}, an {@code IEnumValue}, an {@code IBoolValue} or a synced
 * value and works in GUIs the server opens. An enum or boolean value sets the number of values itself; for a plain
 * integer value call {@link #stateCount(int)}. Set the value before the states. While it is not
 * {@link #available(BooleanSupplier) available} it looks disabled and swallows every mouse button. A value without an
 * icon shows {@link Icon#MORE}.
 */
public class SettingToggleButton extends AbstractCycleButtonWidget<SettingToggleButton> {

    private IconSource[] icons = new IconSource[0];
    private final Availability availability = new Availability();
    private IKey title = IKey.EMPTY;

    /**
     * Creates a setting button of the toolbar button size: AE2's 18 by 20 under AE2 themes, an 18-pixel square in
     * others ({@link AE2Look#iconButtonHeight()}). Set its value, then its states.
     */
    public SettingToggleButton() {
        IDrawable look = AE2ButtonPainter.iconButton(this, this::icon, this::isAvailable);
        background(look).hoverBackground(look)
            .width(AE2Theme.ICON_BUTTON_WIDTH)
            .height(
                () -> AE2Look.of(this)
                    .iconButtonHeight(),
                Measure.PIXEL);
    }

    /**
     * Sets the value the button shows and changes.
     *
     * @param value an integer, enum or boolean value, or a synced one
     * @return this button
     */
    @Override
    public SettingToggleButton value(IIntValue<?> value) {
        return super.value(value);
    }

    /**
     * Sets how many values a plain integer value cycles through, from 0 to {@code count - 1}.
     *
     * @param count number of values, at least one
     * @return this button
     */
    @Override
    public SettingToggleButton stateCount(int count) {
        return super.stateCount(count);
    }

    /**
     * Sets the first tooltip line of every value, such as the name of the setting.
     *
     * @param title setting name
     * @return this button
     */
    public SettingToggleButton title(IKey title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the icon and the tooltip description of one value.
     *
     * @param state       value index, from zero
     * @param icon        icon shown while the value is current
     * @param description tooltip lines below the title while the value is current
     * @return this button
     */
    public SettingToggleButton state(int state, IconSource icon, IKey... description) {
        if (state >= icons.length) icons = Arrays.copyOf(icons, state + 1);
        icons[state] = icon;
        return tooltipBuilder(state, tooltip -> {
            tooltip.addLine(title);
            for (IKey line : description) tooltip.addLine(line);
        });
    }

    /**
     * Sets the icon and the tooltip description of one enum value.
     *
     * @param state       enum value
     * @param icon        icon shown while the value is current
     * @param description tooltip lines below the title while the value is current
     * @param <E>         enum type of the button's value
     * @return this button
     */
    public <E extends Enum<E>> SettingToggleButton state(E state, IconSource icon, IKey... description) {
        return state(state.ordinal(), icon, description);
    }

    /**
     * Sets the icon and the tooltip description of the on or off value of a boolean value.
     *
     * @param state       {@code true} for on
     * @param icon        icon shown while the value is current
     * @param description tooltip lines below the title while the value is current
     * @return this button
     */
    public SettingToggleButton state(boolean state, IconSource icon, IKey... description) {
        return state(state ? 1 : 0, icon, description);
    }

    /**
     * Sets when the button can be used; while it cannot, it looks disabled, shows no hover and swallows every mouse
     * button.
     *
     * @param available supplier read every frame and on every click
     * @return this button
     */
    public SettingToggleButton available(BooleanSupplier available) {
        availability.set(available);
        return this;
    }

    private boolean isAvailable() {
        return availability.get();
    }

    private IconSource icon() {
        int state = getState();
        return state >= 0 && state < icons.length && icons[state] != null ? icons[state] : Icon.MORE;
    }

    @Override
    public boolean canBeSeen(IViewportStack stack) {
        return isEnabled() && super.canBeSeen(stack);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        return availability.press(() -> super.onMousePressed(mouseButton));
    }
}
