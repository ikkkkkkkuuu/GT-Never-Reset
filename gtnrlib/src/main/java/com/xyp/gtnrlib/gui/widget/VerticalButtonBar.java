package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A column of icon buttons on the left of a panel, laid out like AE2 1.21's vertical toolbar: 18-by-20-pixel
 * buttons 22 pixels apart on a background strip.
 * <p>
 * Place the bar at {@code panelLeft - WIDTH + OVERLAP} and add it after the panel's other children: its right
 * {@link #OVERLAP} pixels are drawn over the panel's left border, so the strip and the panel read as one piece
 * without a dividing line. The height grows with each added button. Outside AE2 themes the strip uses the panel
 * background of the current theme.
 */
@SideOnly(Side.CLIENT)
public class VerticalButtonBar extends ParentWidget<VerticalButtonBar> {

    /** Strip width, distance between button tops and pixels drawn over the panel's left border. */
    public static final int WIDTH = 21, PITCH = 22, OVERLAP = 2;
    private static final int BUTTON_X = 3, BUTTON_Y = 3, EDGES = 6;
    private int count;

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
    public VerticalButtonBar button(Supplier<Icon> icon, Supplier<String> tooltip, Runnable action) {
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
    public VerticalButtonBar button(Supplier<Icon> icon, Consumer<RichTooltip> tooltip, IntPredicate click) {
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
    public VerticalButtonBar toggle(Supplier<Icon> icon, Supplier<String> tooltip, BooleanSupplier on, Runnable flip) {
        return add(icon, lines -> lines.addLine(tooltip.get()), mouse -> {
            if (mouse != 0) return false;
            flip.run();
            return true;
        }, on);
    }

    private VerticalButtonBar add(Supplier<Icon> icon, Consumer<RichTooltip> tooltip, IntPredicate click,
        BooleanSupplier selected) {
        ButtonWidget<?> button = new ButtonWidget<>();
        IDrawable look = AE2Theme.iconButton(button, icon, () -> true, selected);
        child(
            button.background(look)
                .hoverBackground(look)
                .pos(BUTTON_X, BUTTON_Y + count * PITCH)
                .size(AE2Theme.ICON_BUTTON_WIDTH, AE2Theme.ICON_BUTTON_HEIGHT)
                .tooltipAutoUpdate(true)
                .tooltipBuilder(tooltip)
                .onMousePressed(click::test));
        count++;
        height(count * PITCH + EDGES);
        return this;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int width = getArea().width, height = getArea().height;
        Variant variant = AE2Theme.active(context);
        if (variant != null) {
            AE2Theme.drawButtonBar(variant, 0, 0, width, height);
            return;
        }
        ITheme theme = context.getTheme();
        WidgetTheme panel = theme.getPanelTheme()
            .getTheme();
        IDrawable background = panel.getBackground();
        if (background != null) background.draw(context, 0, 0, width, height, panel);
    }
}
