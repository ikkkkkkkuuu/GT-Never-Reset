package com.xyp.gtnrlib.gui.widget;

import java.util.List;
import java.util.Objects;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.menu.DropdownWidget;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CycleDropdown<T> extends DropdownWidget<T, CycleDropdown<T>> {

    /** Height of an {@link #iconOption} row and of its icon in pixels. */
    public static final int OPTION_HEIGHT = 16;
    private static final int MENU_EDGE = 1, VALUE_INSET = 3, CARET_SPACE = AE2Theme.CARET_WIDTH + 4;
    private final List<T> choices = new ObjectArrayList<>();
    private T shown;
    private int menuWidth;

    public CycleDropdown(String panelName, Class<T> valueType) {
        super(panelName, valueType);
    }

    // MUI2 keeps the menu of a removed dropdown open, or open until its closing animation ends, and a click meanwhile
    // asks the removed dropdown for its parent.
    @Override
    public void dispose() {
        if (isValid() && isOpen()) closeMenu(false);
        super.dispose();
    }

    @Override
    protected void checkClose(boolean soft, boolean requireNoHover) {
        if (isValid()) super.checkClose(soft, requireNoHover);
    }

    @Override
    protected IWidget valueToWidget(T value, boolean forSelectedDisplay) {
        IWidget widget = super.valueToWidget(value, forSelectedDisplay);
        if (widget instanceof Widget<?>option)
            option.margin(VALUE_INSET, forSelectedDisplay ? CARET_SPACE : VALUE_INSET, 0, 0);
        return widget;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        Variant variant = AE2Theme.active(context);
        int color = variant == null ? widgetTheme.getTheme()
            .getTextColor() : variant.colors.text();
        AE2Theme.drawCaret(
            getArea().width - CARET_SPACE + 1,
            (getArea().height - AE2Theme.CARET_HEIGHT) / 2,
            isOpen(),
            color);
    }

    @Override
    protected void setValue(T value, boolean updateValue) {
        super.setValue(value, updateValue);
        shown = value;
    }

    // MUI2 only refreshes the shown option on its own selections; follow values changed from elsewhere too.
    @Override
    public void onUpdate() {
        super.onUpdate();
        IValue<T> value = getValue();
        if (value != null && isValid() && !Objects.equals(value.getValue(), shown)) setValue(value.getValue(), false);
    }

    public CycleDropdown<T> choices(List<T> choices) {
        if (this.choices.equals(choices)) return this;
        this.choices.clear();
        this.choices.addAll(choices);
        clearOptions().options(choices);
        deleteMenu();
        return this;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        IValue<T> value = getValue();
        if (value == null || choices.isEmpty()) return false;
        int index = Math.max(0, choices.indexOf(value.getValue()));
        setValue(choices.get(Math.floorMod(index - scrollDirection.modifier, choices.size())), true);
        return true;
    }

    /**
     * Makes the opened menu at least this wide so long option names fit; its right edge stays on the dropdown's
     * right edge, so a dropdown at the right of a panel does not push the menu past it.
     *
     * @param width menu width in pixels; zero keeps the dropdown's own width
     * @return this dropdown
     */
    public CycleDropdown<T> menuWidth(int width) {
        menuWidth = width;
        deleteMenu();
        return this;
    }

    /**
     * Creates an option row for {@link #optionToWidget}: an optional item icon, then the name, which scrolls while
     * hovered when it is too long.
     *
     * @param icon item drawn as a {@value #OPTION_HEIGHT}-pixel icon, or {@code null} for none
     * @param name option name
     * @return the row, {@value #OPTION_HEIGHT} pixels high and as wide as the dropdown
     */
    public static IWidget iconOption(@Nullable ItemStack icon, String name) {
        Flow row = Flow.row()
            .widthRel(1f)
            .height(OPTION_HEIGHT)
            .childPadding(2);
        if (icon != null) row.child(
            new ItemDrawable(icon).asWidget()
                .size(OPTION_HEIGHT, OPTION_HEIGHT));
        return row.child(
            new ScrollingTextWidget(IKey.str(name)).alignment(Alignment.CenterLeft)
                .expanded()
                .heightRel(1f));
    }

    @Override
    protected Menu<?> createMenu() {
        Menu<?> menu = super.createMenu();
        if (menuWidth > getArea().width) menu.width(menuWidth)
            .right(0);
        // MUI2 menus draw their options over the panel border, so keep the options inside it.
        if (AE2Theme.active(getContext()) != null)
            menu.padding(AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_BOTTOM_EDGE);
        else menu.padding(MENU_EDGE);
        return menu;
    }
}
