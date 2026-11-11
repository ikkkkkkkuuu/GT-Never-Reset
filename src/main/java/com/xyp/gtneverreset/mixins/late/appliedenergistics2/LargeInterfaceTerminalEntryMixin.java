package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.tile.inventory.AppEngInternalInventory;

/** Keeps native terminal recipe caches aligned with changing provider rows. */
@Mixin(targets = "appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceTerminalEntry", remap = false)
public abstract class LargeInterfaceTerminalEntryMixin {

    @Shadow
    private AppEngInternalInventory inv;

    @Shadow
    private boolean[] filteredRecipes;

    @Shadow
    private Boolean[] useSubstitute;

    @Inject(method = "fullItemUpdate", at = @At("RETURN"), remap = false)
    private void gtnr$resizeRecipeCaches(NBTTagList items, int newSize, CallbackInfo ci) {
        // postUpdate refreshes search results before returning to the combined terminal. Every provider must resize
        // its caches here, before that refresh can index the newly expanded inventory.
        int size = inv.getSizeInventory();
        filteredRecipes = new boolean[size];
        useSubstitute = new Boolean[size];
    }

    @Inject(method = "setItemInSlot", at = @At("RETURN"), remap = false)
    private void gtnr$invalidateSubstituteCache(ItemStack stack, int slot, CallbackInfo ci) {
        if (slot >= 0 && slot < useSubstitute.length) {
            useSubstitute[slot] = null;
        }
    }
}
