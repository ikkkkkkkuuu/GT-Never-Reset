package com.xyp.gtnrlib.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Panels;
import com.xyp.gtnrlib.gui.theme.ae2.IconSource;

/**
 * A column of icon buttons on the left of a panel, laid out like AE2 1.21's vertical toolbar: 18-by-20-pixel
 * buttons 22 pixels apart on a background strip. Icons are built-in {@code Icon}s or any texture or item through
 * {@link IconSource#of}. {@link #available(BooleanSupplier)} makes the button added last unavailable when it should
 * not be used: it then looks disabled and swallows clicks, as every GTNRLib widget's {@code available} does.
 * <p>
 * Place the bar at {@code panelLeft - WIDTH + OVERLAP} and add it after the panel's other children: its right
 * {@link #OVERLAP} pixels are drawn over the panel's left border, so the strip and the panel read as one piece
 * without a dividing line. The height grows with each added button. Outside AE2 themes the strip uses the panel
 * background of the current theme. {@link #buttonSize(int, int)} changes the button size from AE2's. It can be built
 * on either side, so it also works in GUIs the server opens.
 */
public class VerticalButtonBar extends ParentWidget<VerticalButtonBar> {

    /** Strip width, distance between button tops and pixels drawn over the panel's left border. */
    public static final int WIDTH = 21, PITCH = 22, OVERLAP = 2;
    private static final int BUTTON_X = 3, BUTTON_Y = 3, EDGES = 6, BUTTON_GAP = PITCH - AE2Theme.ICON_BUTTON_HEIGHT;
    private final IDrawable plainStrip = AE2Panels.panelBackground(this, 0, 0);
    private final List<ActionButton> buttons = new ArrayList<>();
    private int buttonWidth = AE2Theme.ICON_BUTTON_WIDTH, buttonHeight = AE2Theme.ICON_BUTTON_HEIGHT;
    private ActionButton last;

    public VerticalButtonBar() {
        size(WIDTH, EDGES);
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
     * Sets when the button added last can be used; while it cannot, it looks disabled, shows no hover and swallows
     * clicks.
     *
     * @param available supplier read every frame and on every click
     * @return this bar
     * @throws IllegalStateException if no button was added yet
     */
    public VerticalButtonBar available(BooleanSupplier available) {
        if (last == null) throw new IllegalStateException("Add a button before setting when it is available");
        last.available(available);
        return this;
    }

    /**
     * Sets the size of every button, also of buttons added before; AE2's are 18 by 20 pixels. Buttons stay
     * {@code height + 2} pixels apart, and the strip becomes {@code width + 3} pixels wide, so place a bar with other
     * buttons at {@code panelLeft - getArea().width + OVERLAP}. The frame stretches and the icon stays 16 by 16.
     *
     * @param width  button width in pixels, at least AE2's 18
     * @param height button height in pixels, at least AE2's 20
     * @return this bar
     */
    public VerticalButtonBar buttonSize(int width, int height) {
        buttonWidth = Math.max(AE2Theme.ICON_BUTTON_WIDTH, width);
        buttonHeight = Math.max(AE2Theme.ICON_BUTTON_HEIGHT, height);
        for (int i = 0; i < buttons.size(); i++) buttons.get(i)
            .pos(BUTTON_X, BUTTON_Y + i * pitch())
            .size(buttonWidth, buttonHeight);
        resize();
        return this;
    }

    private int pitch() {
        return buttonHeight + BUTTON_GAP;
    }

    private void resize() {
        size(BUTTON_X + buttonWidth, buttons.size() * pitch() + EDGES);
    }

    private VerticalButtonBar add(Supplier<? extends IconSource> icon, Consumer<RichTooltip> tooltip,
        IntPredicate click, BooleanSupplier selected) {
        last = ActionButton.icon(icon, () -> {})
            .selected(selected)
            .pos(BUTTON_X, BUTTON_Y + buttons.size() * pitch())
            .size(buttonWidth, buttonHeight)
            .tooltipAutoUpdate(true)
            .tooltipBuilder(tooltip)
            .onMousePressed(click::test);
        buttons.add(last);
        child(last);
        resize();
        return this;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int width = getArea().width, height = getArea().height;
        Variant variant = AE2Look.of(this)
            .variant();
        if (variant != null) AE2Panels.drawButtonBar(variant, 0, 0, width, height);
        else plainStrip.draw(context, 0, 0, width, height, widgetTheme.getTheme());
    }
}
