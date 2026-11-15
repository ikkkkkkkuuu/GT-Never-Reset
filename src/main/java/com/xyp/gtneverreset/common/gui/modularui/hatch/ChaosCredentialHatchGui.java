package com.xyp.gtneverreset.common.gui.modularui.hatch;

import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtneverreset.common.item.items.ChaosCredentialItem;
import com.xyp.gtneverreset.common.machine.hatch.ChaosCredentialHatch;

import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

public final class ChaosCredentialHatchGui extends MTEHatchBaseGui<ChaosCredentialHatch> {

    public ChaosCredentialHatchGui(ChaosCredentialHatch machine) {
        super(machine);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
        return getEmptyContent().child(new ItemSlot().slot(new ModularSlot(machine.inventoryHandler, 0) {

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return !machine.locked() && super.canTakeStack(player);
            }
        }.filter(ChaosCredentialItem::isBound)
            .singletonSlotGroup())
            .pos(72, 4))
            .child(
                IKey.lang("gtnr.chaos.hatch.help")
                    .asWidget()
                    .pos(4, 28)
                    .size(152, 32));
    }
}
