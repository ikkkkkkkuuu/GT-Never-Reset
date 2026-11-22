package com.xyp.gtnrlib.gui.widget;

import java.util.List;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.menu.DropdownWidget;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.widget.AE2ButtonDrawable.State;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CycleDropdown<T> extends DropdownWidget<T, CycleDropdown<T>> {

    private static final int MENU_EDGE = 1;
    private final String menuName;
    private final List<T> choices = new ObjectArrayList<>();

    public CycleDropdown(String panelName, Class<T> valueType) {
        super(panelName, valueType);
        menuName = panelName;
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
        if (!isHovering() || value == null || choices.isEmpty()) return false;
        int index = Math.max(0, choices.indexOf(value.getValue()));
        setValue(choices.get(Math.floorMod(index - scrollDirection.modifier, choices.size())), true);
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        int height = getArea().h();
        var style = AE2Theme.buttonText(context, State.of(this, true, false), height);
        int x = getArea().w() - 10, y = style.y() + 2;
        for (int row = 0; row < 3; row++)
            GuiDraw.drawRect(x + row, y + row, 5 - row * 2, 1, 0xFF000000 | style.color());
    }

    @Override
    protected Menu<?> createMenu() {
        var variant = AE2Theme.variant(
            getPanel().getTheme()
                .getId());
        var screen = getScreen();
        AE2Theme.useMenuTheme(variant == null ? null : variant.id, screen.getOwner(), screen.getName(), menuName);
        Menu<?> menu = super.createMenu();
        // MUI2 menus draw their options over the panel border, so keep the options inside it.
        if (variant != null) {
            AE2Theme.polish(menu, variant.id);
            menu.padding(AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_BOTTOM_EDGE);
        } else menu.padding(MENU_EDGE);
        return menu;
    }
}
