package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui.InterfacePatternRows;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui.InterfaceRowFilter;

import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;

/** Avoids decoding and rendering patterns below the scissor rectangle while preserving the entry's full height. */
@Mixin(value = GuiInterfaceTerminal.class, remap = false)
public abstract class InterfaceTerminalViewportMixin {

    @Shadow
    private int viewHeight;

    // The combined terminal uses its side panels for storage and encoding. Upgrade editing opens the native window.
    @WrapOperation(
        method = { "updateLayout", "drawBG" },
        at = @At(
            value = "INVOKE",
            target = "Lappeng/container/implementations/ContainerInterfaceTerminal;getToolboxSize()I"),
        require = 2)
    private int gtnr$toolboxSize(ContainerInterfaceTerminal container, Operation<Integer> original) {
        return (Object) this instanceof InterfaceRowFilter ? 0 : original.call(container);
    }

    @WrapOperation(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/tile/inventory/AppEngInternalInventory;getStackInSlot(I)Lnet/minecraft/item/ItemStack;",
            remap = true),
        require = 1)
    private ItemStack skipOffscreenPattern(AppEngInternalInventory inventory, int slot, Operation<ItemStack> original,
        @Coerce Object entry, int viewY, int titleBottom, int mouseX, int mouseY) {
        if (!((Object) this instanceof InterfaceRowFilter)) return original.call(inventory, slot);
        int rowSize = ((InterfaceEntryViewportAccessor) entry).getRowSize();
        int visibleSlot = ((InterfacePatternRows) entry).gtneverreset$getVisibleSlot(slot);
        if (visibleSlot < 0 || rowSize > 0 && viewY + visibleSlot / rowSize * 18 >= viewHeight) return null;
        return original.call(inventory, slot);
    }
}
