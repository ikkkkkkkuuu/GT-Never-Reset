package com.xyp.gtnrlib.gui.widget;

import java.util.List;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.widgets.menu.DropdownWidget;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.xyp.gtnrlib.gui.theme.AE2Theme;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CycleDropdown<T> extends DropdownWidget<T, CycleDropdown<T>> {

    private static final int MENU_EDGE = 1;
    private final List<T> choices = new ObjectArrayList<>();

    public CycleDropdown(String panelName, Class<T> valueType) {
        super(panelName, valueType);
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

    @Override
    protected Menu<?> createMenu() {
        Menu<?> menu = super.createMenu();
        // MUI2 menus draw their options over the panel border, so keep the options inside it.
        if (AE2Theme.active(getContext()) != null)
            menu.padding(AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_EDGE, AE2Theme.PANEL_BOTTOM_EDGE);
        else menu.padding(MENU_EDGE);
        return menu;
    }
}
