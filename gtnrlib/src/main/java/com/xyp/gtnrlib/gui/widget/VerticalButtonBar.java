package com.xyp.gtnrlib.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.INotifyEnabled;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Stencil;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Unit.Measure;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2PanelPainter;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;
import com.xyp.gtnrlib.gui.theme.ae2.PlainLook;

/**
 * A column of icon buttons on the left of a panel, laid out like AE2 1.21's vertical toolbar: 18-by-20-pixel buttons 22
 * pixels apart on a background strip under AE2 themes, and 18-pixel square buttons 20 apart in other themes, whose
 * buttons have no shadow below them ({@link AE2Look#iconButtonHeight()}). Icons are {@code Icon}s,
 * {@code AE2SettingIcon}s or any texture or item through {@link IconSource#of}. {@link #add(Widget)} puts any widget in
 * the next slot, such as a {@link SettingToggleButton} or an {@link IconButton} of your own, as AE2's
 * {@code addToLeftToolbar} takes any button.
 * {@link #available(BooleanSupplier)} makes the button added last unavailable when it should not be used: it then looks
 * disabled and swallows clicks, as every GTNRLib widget's {@code available} does.
 * <p>
 * The bar stands outside the panel, so it tells NEI to keep its panels clear of it, as AE2's toolbar does. Place the
 * bar at {@code panelLeft - WIDTH + OVERLAP} and add it after the panel's other children: its right
 * {@link #OVERLAP} pixels are drawn over the panel's left border, so the strip and the panel read as one piece without
 * a dividing line. The height grows with each added button. A button that is turned off with {@code setEnabled(false)}
 * or {@code setEnabledIf} leaves no gap: the buttons below move up and the strip gets shorter, and with no button shown
 * nothing is drawn, as in AE2. Outside AE2 themes the strip uses the panel background of the current theme without its
 * right border, covering the panel's left border the same way. {@link #buttonSize(int, int)} changes the button size
 * from AE2's. It can be built on either side, so it also works in GUIs the server opens.
 */
public class VerticalButtonBar extends ParentWidget<VerticalButtonBar> implements INotifyEnabled {

    /** Strip width, distance between button tops under AE2 themes and pixels drawn over the panel's left border. */
    public static final int WIDTH = 21, PITCH = 22, OVERLAP = 2;
    private static final int BUTTON_X = 3, BUTTON_Y = 3, EDGES = 6, BUTTON_GAP = PITCH - AE2Theme.ICON_BUTTON_HEIGHT;
    private final IDrawable plainStrip = AE2PanelPainter.panelBackground(this, 0, 0);
    private final List<Widget<?>> buttons = new ArrayList<>();
    private int buttonWidth = AE2Theme.ICON_BUTTON_WIDTH, buttonHeight, shownButtons;
    private Widget<?> last;

    /**
     * Creates an empty bar; add buttons with {@code button} and {@code toggle}.
     */
    public VerticalButtonBar() {
        size(WIDTH, EDGES).excludeAreaInRecipeViewer();
    }

    /**
     * Adds a button below the existing ones.
     *
     * @param icon    supplier of the icon, read every frame so a toggle can show its state
     * @param tooltip supplier of the tooltip line, read every frame
     * @param action  run on a left click
     * @return this bar
     */
    public VerticalButtonBar button(Supplier<? extends IconSource> icon, Supplier<String> tooltip, Runnable action) {
        return button(icon, lines -> lines.addLine(tooltip.get()), mouse -> {
            if (mouse != 0) return false;
            action.run();
            return true;
        });
    }

    /**
     * Adds a button below the existing ones that can tell the mouse buttons apart.
     *
     * @param icon    supplier of the icon, read every frame
     * @param tooltip fills the tooltip, again every frame
     * @param click   receives the pressed mouse button (0 left, 1 right) and returns whether it handled it
     * @return this bar
     */
    public VerticalButtonBar button(Supplier<? extends IconSource> icon, Consumer<RichTooltip> tooltip,
        IntPredicate click) {
        return add(icon, tooltip, click, () -> false);
    }

    /**
     * Adds an on/off button below the existing ones; it shows AE2's selected frame while on.
     *
     * @param icon    supplier of the icon, read every frame
     * @param tooltip supplier of the tooltip line, read every frame
     * @param on      supplier of the on state, read every frame
     * @param flip    run on a left click
     * @return this bar
     */
    public VerticalButtonBar toggle(Supplier<? extends IconSource> icon, Supplier<String> tooltip, BooleanSupplier on,
        Runnable flip) {
        return add(icon, lines -> lines.addLine(tooltip.get()), mouse -> {
            if (mouse != 0) return false;
            flip.run();
            return true;
        }, on);
    }

    /**
     * Adds any widget below the existing buttons, sized to the bar's button size; it keeps its own look, tooltip and
     * clicks. Use it for a {@link SettingToggleButton} or a button you built yourself.
     *
     * @param widget widget to add, not added anywhere else
     * @return this bar
     */
    public VerticalButtonBar add(Widget<?> widget) {
        last = widget;
        buttons.add(widget);
        child(widget);
        layoutButtons();
        return this;
    }

    /**
     * Sets when the button added last can be used; while it cannot, it looks disabled, shows no hover and swallows
     * clicks.
     *
     * @param available supplier read every frame and on every click
     * @return this bar
     * @throws IllegalStateException if no button was added yet, or the widget added last has no available state of its
     *                               own (only GTNRLib's buttons and {@link SettingToggleButton} have one)
     */
    public VerticalButtonBar available(BooleanSupplier available) {
        if (last instanceof ThemedButton<?>button) button.available(available);
        else if (last instanceof SettingToggleButton setting) setting.available(available);
        else throw new IllegalStateException("The widget added last has no available state");
        return this;
    }

    /**
     * Sets the size of every button in every look, also of buttons added before, instead of the look's toolbar button
     * size. Buttons stay {@code height + 2} pixels apart, and the strip becomes {@code width + 3} pixels wide, so place
     * a bar with other buttons at {@code panelLeft - getArea().width + OVERLAP}. The frame stretches and the icon stays
     * 16 by 16.
     *
     * @param width  button width in pixels, at least AE2's 18
     * @param height button height in pixels, at least AE2's 20
     * @return this bar
     */
    public VerticalButtonBar buttonSize(int width, int height) {
        buttonWidth = Math.max(AE2Theme.ICON_BUTTON_WIDTH, width);
        buttonHeight = Math.max(AE2Theme.ICON_BUTTON_HEIGHT, height);
        layoutButtons();
        return this;
    }

    @Override
    public void onInit() {
        super.onInit();
        layoutButtons();
    }

    @Override
    public void onChildChangeEnabled(IWidget child, boolean enabled) {
        layoutButtons();
    }

    private int buttonHeight() {
        return buttonHeight > 0 ? buttonHeight
            : AE2Look.of(this)
                .iconButtonHeight();
    }

    private int pitch() {
        return buttonHeight() + BUTTON_GAP;
    }

    private void layoutButtons() {
        int shown = 0;
        for (Widget<?> button : buttons) {
            if (!button.isEnabled()) continue;
            int slot = shown++;
            button.left(BUTTON_X)
                .top(() -> BUTTON_Y + slot * pitch(), Measure.PIXEL)
                .width(buttonWidth)
                .height(this::buttonHeight, Measure.PIXEL);
        }
        shownButtons = shown;
        width(BUTTON_X + buttonWidth)
            .height(() -> shownButtons == 0 ? 0 : shownButtons * pitch() + EDGES, Measure.PIXEL);
        if (isValid()) scheduleResize();
    }

    private VerticalButtonBar add(Supplier<? extends IconSource> icon, Consumer<RichTooltip> tooltip,
        IntPredicate click, BooleanSupplier selected) {
        return add(
            new IconButton(icon, () -> {}).selected(selected)
                .tooltipAutoUpdate(true)
                .tooltipBuilder(tooltip)
                .onMousePressed(click::test));
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        if (shownButtons == 0) return;
        int width = getArea().width, height = getArea().height;
        Variant variant = AE2Look.of(this)
            .variant();
        if (variant != null) {
            AE2PanelPainter.drawButtonBar(variant, 0, 0, width, height);
            return;
        }
        int cover = PlainLook.PANEL_BORDER - OVERLAP;
        Stencil.apply(0, 0, width + cover, height, context);
        plainStrip.draw(context, 0, 0, width + cover + PlainLook.PANEL_BORDER, height, widgetTheme.getTheme());
        Stencil.remove();
    }
}
