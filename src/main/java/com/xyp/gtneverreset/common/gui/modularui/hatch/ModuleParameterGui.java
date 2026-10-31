package com.xyp.gtneverreset.common.gui.modularui.hatch;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

public final class ModuleParameterGui extends MTEHatchBaseGui<ParameterModuleHatch> {

    public ModuleParameterGui(ParameterModuleHatch machine) {
        super(machine);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
        ParentWidget<?> content = getEmptyContent();
        IntSyncValue value = new IntSyncValue(machine::getParameter, machine::setParameter).allowC2S();
        sync.syncValue("module_parameter", value);
        // #tr gtnr.module.parameter.label
        // # Parameter
        // # zh_CN 参数
        content.child(
            IKey.lang("gtnr.module.parameter.label")
                .asWidget()
                .pos(24, 0));
        content.child(
            new TextFieldWidget().value(value)
                .numbersInt(1, machine.getMaximumParameter())
                .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                .size(120, 18)
                .pos(20, 16));
        return content;
    }
}
