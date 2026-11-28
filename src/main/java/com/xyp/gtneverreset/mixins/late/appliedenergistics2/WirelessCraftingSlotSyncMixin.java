package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal;

import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.SlotCraftingTerm;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;

@Mixin(ContainerCraftingTerm.class)
public abstract class WirelessCraftingSlotSyncMixin extends ContainerMEMonitorable {

    @Shadow(remap = false)
    @Final
    private SlotCraftingTerm outputSlot;

    @Unique
    private boolean gtnr$applyingCraftingSnapshot;

    protected WirelessCraftingSlotSyncMixin(InventoryPlayer inventory, ITerminalHost host) {
        super(inventory, host);
    }

    @Override
    public void putStacksInSlots(ItemStack[] stacks) {
        if (stacks.length <= outputSlot.slotNumber || !getPlayerInv().player.worldObj.isRemote
            || !(getTarget() instanceof WirelessCraftingTerminalGuiObject terminal)
            || !(terminal.getItemStack()
                .getItem() instanceof ItemWirelessDualInterfaceTerminal)) {
            super.putStacksInSlots(stacks);
            return;
        }

        // The snapshot writes the server's result after the matrix slots.
        boolean wasApplyingSnapshot = gtnr$applyingCraftingSnapshot;
        gtnr$applyingCraftingSnapshot = true;
        try {
            super.putStacksInSlots(stacks);
        } finally {
            gtnr$applyingCraftingSnapshot = wasApplyingSnapshot;
        }
    }

    @Inject(method = "onCraftMatrixChanged", at = @At("HEAD"), cancellable = true)
    private void gtnr$skipSnapshotRecipePreview(IInventory inventory, CallbackInfo ci) {
        if (gtnr$applyingCraftingSnapshot) ci.cancel();
    }
}
