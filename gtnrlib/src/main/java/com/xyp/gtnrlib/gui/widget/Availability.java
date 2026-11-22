package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.widget.Interactable.Result;

/**
 * Whether a widget can be used right now, and the one rule every GTNRLib widget follows while it cannot: it swallows
 * mouse presses, so a click never reaches whatever lies below it, and runs nothing. Widgets with an
 * {@code available(BooleanSupplier)} method keep one of these and route their presses through {@link #press}.
 */
public class Availability {

    private BooleanSupplier available = () -> true;

    /**
     * Sets when the widget can be used.
     *
     * @param available supplier read every frame and on every input
     */
    public void set(BooleanSupplier available) {
        this.available = available;
    }

    /**
     * Reports whether the widget can be used right now.
     *
     * @return the supplier's current answer; {@code true} until one is set
     */
    public boolean get() {
        return available.getAsBoolean();
    }

    /**
     * Answers a mouse press: the widget's own handling while it is available, and otherwise {@link Result#STOP}, which
     * ends the press without running anything.
     *
     * @param handling the widget's handling of the press, run only while available
     * @return the handling's result, or {@link Result#STOP} while unavailable
     */
    public Result press(Supplier<Result> handling) {
        return get() ? handling.get() : Result.STOP;
    }
}
