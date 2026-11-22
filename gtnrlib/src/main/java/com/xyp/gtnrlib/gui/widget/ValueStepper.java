package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;

/**
 * Steps through a list of named values: {@code <} and {@code >} buttons around the current value's name, and, after
 * {@link #wheel(boolean) wheel(true)}, the mouse wheel anywhere over it. It holds no values itself; the step callback
 * moves the owner's selection, so it suits values that are not numbers, such as voltage tiers. For numbers use
 * {@link NumberEntryWidget}. The name takes the panel's text color, or the list's text color and shadow when the
 * stepper is the control of a {@link ListRow}. While {@link #available(BooleanSupplier)} reports false the arrows look
 * disabled, the name is greyed out, and clicks and the wheel do nothing. It can be built on either side, so it also
 * works in GUIs the server opens.
 */
public class ValueStepper extends ParentWidget<ValueStepper> implements Interactable, IListRowControl {

    /** Width of each arrow button in pixels. */
    public static final int ARROW_WIDTH = 12;
    private final IntConsumer step;
    private final TextWidget<?> name;
    private final Availability availability = new Availability();
    private boolean wheel, inListRow;

    /**
     * Creates a stepper; the caller sets its size, and the arrows fill its height.
     *
     * @param value supplier of the current value's name, read every frame
     * @param step  called with -1 or +1 for an arrow click or a wheel notch
     */
    public ValueStepper(Supplier<String> value, IntConsumer step) {
        this.step = step;
        name = IKey.dynamic(value)
            .asWidget();
        child(
            Flow.row()
                .sizeRel(1f)
                .childPadding(1)
                .child(
                    new AE2Button(() -> "<", () -> step.accept(-1)).available(availability::get)
                        .width(ARROW_WIDTH)
                        .heightRel(1f))
                .child(
                    name.alignment(Alignment.Center)
                        .expanded()
                        .heightRel(1f))
                .child(
                    new AE2Button(() -> ">", () -> step.accept(1)).available(availability::get)
                        .width(ARROW_WIDTH)
                        .heightRel(1f)));
    }

    /**
     * Sets when the stepper can be used.
     *
     * @param available supplier read every frame and on every input
     * @return this stepper
     */
    public ValueStepper available(BooleanSupplier available) {
        availability.set(available);
        return this;
    }

    /**
     * Sets whether the mouse wheel over the stepper steps it. Off by default, so the wheel goes to the widgets around
     * it, such as a scrolling list.
     *
     * @param wheel true to step one value per wheel notch
     * @return this stepper
     */
    public ValueStepper wheel(boolean wheel) {
        this.wheel = wheel;
        return this;
    }

    @Override
    public void placedInListRow() {
        inListRow = true;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        return availability.press(() -> Result.IGNORE);
    }

    @Override
    public boolean canHover() {
        return true;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        if (!wheel || !availability.get()) return false;
        step.accept(scrollDirection.modifier);
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        boolean usable = availability.get();
        AE2Look look = AE2Look.of(this);
        if (inListRow) name.color(usable ? look.listText() : look.listMuted())
            .shadow(usable && look.listShadow());
        else name.color(look.panelText(usable))
            .shadow(false);
    }
}
