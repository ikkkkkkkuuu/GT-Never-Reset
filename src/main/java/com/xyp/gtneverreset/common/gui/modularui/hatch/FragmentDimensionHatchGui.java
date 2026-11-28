package com.xyp.gtneverreset.common.gui.modularui.hatch;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtneverreset.common.machine.hatch.FragmentDimensionHatch;

import gregtech.api.modularui2.GTGuis;

public final class FragmentDimensionHatchGui {

    private FragmentDimensionHatchGui() {}

    public static ModularPanel build(FragmentDimensionHatch hatch, PosGuiData data, PanelSyncManager sync,
        UISettings settings) {
        sync.registerSlotGroup("dimension", 1);
        return GTGuis.mteTemplatePanelBuilder(hatch, data, sync, settings)
            .build()
            .child(
                new ItemSlot().slot(new ModularSlot(hatch.inventoryHandler, 0).slotGroup("dimension"))
                    .pos(79, 34));
    }
}
