package com.xyp.gtneverreset.common.block.blocks.tile;

import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;

public final class OverloadedProviderInventory {

    private OverloadedProviderInventory() {}

    public static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    public static boolean insert(IInventory inventory, int face, List<ItemStack> inputs, boolean simulate) {
        ItemStack[] planned = new ItemStack[inventory.getSizeInventory()];
        for (int i = 0; i < planned.length; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            planned[i] = stack == null ? null : stack.copy();
        }
        for (ItemStack input : inputs) {
            int remaining = input.stackSize;
            for (int slot = 0; slot < planned.length && remaining > 0; slot++) {
                if (!canInsert(inventory, slot, input, face)) continue;
                ItemStack current = planned[slot];
                if (current != null && !same(current, input)) continue;
                int limit = Math.min(inventory.getInventoryStackLimit(), input.getMaxStackSize());
                int added = Math.min(remaining, Math.max(0, limit - (current == null ? 0 : current.stackSize)));
                if (added == 0) continue;
                if (current == null) {
                    current = input.copy();
                    current.stackSize = 0;
                    planned[slot] = current;
                }
                current.stackSize += added;
                remaining -= added;
            }
            if (remaining != 0) return false;
        }
        if (!simulate) {
            for (int slot = 0; slot < planned.length; slot++) {
                if (!ItemStack.areItemStacksEqual(inventory.getStackInSlot(slot), planned[slot])) {
                    inventory.setInventorySlotContents(slot, planned[slot]);
                }
            }
            inventory.markDirty();
        }
        return true;
    }

    private static boolean canInsert(IInventory inventory, int slot, ItemStack stack, int face) {
        if (!inventory.isItemValidForSlot(slot, stack)) return false;
        if (inventory instanceof TileEntityFurnace) {
            boolean smeltable = FurnaceRecipes.smelting()
                .getSmeltingResult(stack) != null;
            return slot == (smeltable ? 0 : TileEntityFurnace.isItemFuel(stack) ? 1 : -1);
        }
        if (inventory instanceof ISidedInventory sided) {
            for (int accessible : sided.getAccessibleSlotsFromSide(face)) {
                if (accessible == slot) return sided.canInsertItem(slot, stack, face);
            }
            return false;
        }
        return true;
    }

    public static boolean canExtract(IInventory inventory, int slot, ItemStack stack) {
        if (inventory instanceof TileEntityFurnace) return slot == 2;
        if (inventory instanceof ISidedInventory sided) {
            for (int face = 0; face < 6; face++) {
                for (int accessible : sided.getAccessibleSlotsFromSide(face)) {
                    if (accessible == slot && sided.canExtractItem(slot, stack, face)) return true;
                }
            }
            return false;
        }
        return true;
    }
}
