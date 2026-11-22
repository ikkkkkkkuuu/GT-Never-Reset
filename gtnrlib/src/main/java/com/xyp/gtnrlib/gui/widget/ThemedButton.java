package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.widgets.ButtonWidget;

/**
 * The button the AE2 buttons build on: {@link AE2Button}, {@link IconButton} and {@link TabButton}. On its own, with an
 * empty background, it is an invisible click area ({@code new ThemedButton<>()}). While it is not
 * {@link #available(BooleanSupplier) available} it looks disabled, shows no hover and swallows every mouse button, so
 * clicks never reach a widget below it ({@link Availability}). It can be built on either side, so it also works in GUIs
 * the server opens.
 *
 * @param <W> the button's own type, so chained calls keep returning the subclass
 */
public class ThemedButton<W extends ThemedButton<W>> extends ButtonWidget<W> {

    private final Availability availability = new Availability();
    private BooleanSupplier selected = () -> false;

    /**
     * Sets when the button can be used; it looks disabled otherwise.
     *
     * @param available supplier read every frame and on every click
     * @return this button
     */
    public W available(BooleanSupplier available) {
        availability.set(available);
        return getThis();
    }

    /**
     * Sets when the button shows the theme's selected look, such as the button of the current choice.
     *
     * @param selected supplier read every frame
     * @return this button
     */
    public W selected(BooleanSupplier selected) {
        this.selected = selected;
        return getThis();
    }

    /**
     * Sets what a left click does while the button is available; other mouse buttons do nothing. A mouse handler set
     * afterwards replaces it.
     *
     * @param action run on a left click
     * @return this button
     */
    public W action(Runnable action) {
        return onMousePressed(button -> {
            if (button != 0) return false;
            action.run();
            return true;
        });
    }

    protected boolean isAvailable() {
        return availability.get();
    }

    protected boolean isSelected() {
        return selected.getAsBoolean();
    }

    @Override
    public boolean canBeSeen(IViewportStack stack) {
        return isEnabled() && super.canBeSeen(stack);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        return availability.press(() -> super.onMousePressed(button));
    }
}
