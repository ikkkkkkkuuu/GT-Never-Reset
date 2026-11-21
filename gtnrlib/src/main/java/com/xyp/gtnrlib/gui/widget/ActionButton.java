package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Buttons;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;

/**
 * A button drawn by the active theme: a text button ({@link AE2Buttons#button} with {@link AE2Buttons#label}) or a
 * toolbar icon button ({@link AE2Buttons#iconButton}). It reacts to the left mouse button only, unless a mouse handler
 * set afterwards replaces the action. While it is not available it looks disabled, shows no hover and swallows every
 * mouse button, so clicks never reach a widget below it. An icon button takes the mouse on its whole frame; AE2's react
 * only on the 16-by-16 icon square. It can be built on either side, so it also works in GUIs the server opens.
 */
public class ActionButton extends ButtonWidget<ActionButton> {

    private BooleanSupplier available = () -> true, selected = () -> false;

    /**
     * Creates a text button. The caller sets its size.
     *
     * @param label  supplier of the label, read every frame; under AE2 themes a label wider than the button scrolls
     *               inside it, other themes trim it to the button width
     * @param action run on a left click while the button is available
     * @return the button
     */
    public static ActionButton text(Supplier<String> label, Runnable action) {
        ActionButton button = new ActionButton();
        IDrawable look = AE2Buttons.button(button, button::isAvailable, button::isSelected);
        return button.background(look)
            .hoverBackground(look)
            .overlay(AE2Buttons.label(button, label, button::isAvailable, button::isSelected))
            .onClick(action);
    }

    /**
     * Creates a toolbar icon button of the standard {@link AE2Theme#ICON_BUTTON_WIDTH} by
     * {@link AE2Theme#ICON_BUTTON_HEIGHT} size.
     *
     * @param icon   supplier of the icon, read every frame: a built-in {@code Icon}, or any texture or item through
     *               {@link IconSource#of}
     * @param action run on a left click while the button is available
     * @return the button
     */
    public static ActionButton icon(Supplier<? extends IconSource> icon, Runnable action) {
        ActionButton button = new ActionButton();
        IDrawable look = AE2Buttons.iconButton(button, icon, button::isAvailable, button::isSelected);
        return button.background(look)
            .hoverBackground(look)
            .size(AE2Theme.ICON_BUTTON_WIDTH, AE2Theme.ICON_BUTTON_HEIGHT)
            .onClick(action);
    }

    /**
     * Sets when the button can be used; it looks disabled otherwise.
     *
     * @param available supplier read every frame and on every click
     * @return this button
     */
    public ActionButton available(BooleanSupplier available) {
        this.available = available;
        return this;
    }

    /**
     * Sets when the button shows the theme's selected look, such as the button of the current choice.
     *
     * @param selected supplier read every frame
     * @return this button
     */
    public ActionButton selected(BooleanSupplier selected) {
        this.selected = selected;
        return this;
    }

    private ActionButton onClick(Runnable action) {
        return onMousePressed(button -> {
            if (button != 0) return false;
            action.run();
            return true;
        });
    }

    private boolean isAvailable() {
        return available.getAsBoolean();
    }

    private boolean isSelected() {
        return selected.getAsBoolean();
    }

    @Override
    public boolean canBeSeen(IViewportStack stack) {
        return isEnabled() && super.canBeSeen(stack);
    }

    @Override
    public @NotNull Result onMousePressed(int button) {
        return isAvailable() ? super.onMousePressed(button) : Result.STOP;
    }
}
