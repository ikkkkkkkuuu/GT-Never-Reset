package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.tile.inventory.AppEngInternalInventory;

/** Keeps native terminal recipe caches aligned with changing provider rows. */
@Mixin(targets = "appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceTerminalEntry", remap = false)
public abstract class LargeInterfaceTerminalEntryMixin {

    @Shadow
    private ItemStack selfRep;

    @Shadow
    private AppEngInternalInventory inv;

    @Shadow
    private boolean[] filteredRecipes;

    @Shadow
    private Boolean[] useSubstitute;

    @Inject(method = "fullItemUpdate", at = @At("RETURN"), remap = false)
    private void gtnr$resizeRecipeCaches(NBTTagList items, int newSize, CallbackInfo ci) {
        if (!gtnr$isLargeInterface()) return;
        int size = inv.getSizeInventory();
        filteredRecipes = new boolean[size];
        useSubstitute = new Boolean[size];
    }

    @Inject(method = "setItemInSlot", at = @At("RETURN"), remap = false)
    private void gtnr$invalidateSubstituteCache(ItemStack stack, int slot, CallbackInfo ci) {
        if (gtnr$isLargeInterface() && slot >= 0 && slot < useSubstitute.length) {
            useSubstitute[slot] = null;
        }
    }

    @Unique
    private boolean gtnr$isLargeInterface() {
        if (selfRep == null) return false;
        ItemStack matrix = GTNRItemList.AssemblerMatrix.get(1);
        ItemStack packaged = GTNRItemList.WirelessPackagedPatternProvider.get(1);
        return matrix != null && selfRep.isItemEqual(matrix) || packaged != null && selfRep.isItemEqual(packaged);
    }
}
