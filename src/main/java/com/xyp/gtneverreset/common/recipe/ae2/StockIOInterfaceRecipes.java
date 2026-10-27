package com.xyp.gtneverreset.common.recipe.ae2;

import java.util.Collections;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

public final class StockIOInterfaceRecipes {

    private StockIOInterfaceRecipes() {}

    public static void register() {
        GameRegistry.addShapelessRecipe(
            GTNRItemList.StockIOInterface.get(1),
            AEApi.instance()
                .definitions()
                .blocks()
                .iface()
                .maybeStack(1)
                .get(),
            GTNRItemList.AdvancedIOBus.get(1));
        GameRegistry.addRecipe(new Conversion(GTNRItemList.StockIOInterface, GTNRItemList.StockIOInterfacePart));
        GameRegistry.addRecipe(new Conversion(GTNRItemList.StockIOInterfacePart, GTNRItemList.StockIOInterface));
    }

    /** Form conversion preserves ghost settings and real unsettled refunds; ordinary crafting would discard them. */
    private static final class Conversion extends ShapelessRecipes {

        private Conversion(GTNRItemList input, GTNRItemList output) {
            super(output.get(1), Collections.singletonList(input.get(1)));
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            if (!matches(inventory, null)) return null;
            ItemStack result = super.getCraftingResult(inventory);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack source = inventory.getStackInSlot(slot);
                if (source != null && source.hasTagCompound()) {
                    result.setTagCompound(
                        (NBTTagCompound) source.getTagCompound()
                            .copy());
                    break;
                }
            }
            return result;
        }
    }
}
