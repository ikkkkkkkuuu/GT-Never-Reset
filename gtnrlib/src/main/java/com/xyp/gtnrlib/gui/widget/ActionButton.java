package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A button drawn by the active theme: a text button ({@link AE2Theme#button} with {@link AE2Theme#label}) or a
 * toolbar icon button ({@link AE2Theme#iconButton}). It reacts to the left mouse button only. While it is not
 * available it looks disabled and swallows left clicks, so they never reach a widget below it.
 */
@SideOnly(Side.CLIENT)
public class ActionButton extends ButtonWidget<ActionButton> {

    private BooleanSupplier available = () -> true, selected = () -> false;

    /**
     * Creates a text button. The caller sets its size.
     *
     * @param label  supplier of the label, read every frame; trimmed to the button width
     * @param action run on a left click while the button is available
     * @return the button
     */
    public static ActionButton text(Supplier<String> label, Runnable action) {
        ActionButton button = new ActionButton();
        IDrawable look = AE2Theme.button(button, button::isAvailable, button::isSelected);
        return button.background(look)
            .hoverBackground(look)
            .overlay(AE2Theme.label(button, label, button::isAvailable, button::isSelected))
            .onClick(action);
    }

    /**
     * Creates a toolbar icon button of the standard {@link AE2Theme#ICON_BUTTON_WIDTH} by
     * {@link AE2Theme#ICON_BUTTON_HEIGHT} size.
     *
     * @param icon   supplier of the icon, read every frame
     * @param action run on a left click while the button is available
     * @return the button
     */
    public static ActionButton icon(Supplier<Icon> icon, Runnable action) {
        ActionButton button = new ActionButton();
        IDrawable look = AE2Theme.iconButton(button, icon, button::isAvailable, button::isSelected);
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
        return button == 0 && !isAvailable() ? Result.STOP : super.onMousePressed(button);
    }
}
