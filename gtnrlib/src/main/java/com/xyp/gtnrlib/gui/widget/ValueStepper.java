package com.xyp.gtnrlib.gui.widget;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Steps through a list of named values: {@code <} and {@code >} buttons around the current value's name, and the
 * mouse wheel anywhere over it. It holds no values itself; the step callback moves the owner's selection, so it suits
 * values that are not numbers, such as voltage tiers. For numbers use {@link NumberEntry}.
 */
@SideOnly(Side.CLIENT)
public class ValueStepper extends ParentWidget<ValueStepper> implements Interactable {

    /** Width of each arrow button in pixels. */
    public static final int ARROW_WIDTH = 12;
    private final IntConsumer step;

    /**
     * Creates a stepper; the caller sets its size, and the arrows fill its height.
     *
     * @param value supplier of the current value's name, read every frame
     * @param step  called with -1 or +1 for an arrow click or a wheel notch
     */
    public ValueStepper(Supplier<String> value, IntConsumer step) {
        this.step = step;
        child(
            Flow.row()
                .sizeRel(1f)
                .childPadding(1)
                .child(
                    ActionButton.text(() -> "<", () -> step.accept(-1))
                        .width(ARROW_WIDTH)
                        .heightRel(1f))
                .child(
                    IKey.dynamic(value)
                        .asWidget()
                        .alignment(Alignment.Center)
                        .expanded()
                        .heightRel(1f))
                .child(
                    ActionButton.text(() -> ">", () -> step.accept(1))
                        .width(ARROW_WIDTH)
                        .heightRel(1f)));
    }

    @Override
    public boolean canHover() {
        return true;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        step.accept(scrollDirection.modifier);
        return true;
    }
}
