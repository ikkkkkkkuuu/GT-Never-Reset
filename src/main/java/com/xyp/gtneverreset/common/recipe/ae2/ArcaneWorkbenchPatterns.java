package com.xyp.gtneverreset.common.recipe.ae2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.storage.data.IAEItemStack;
import appeng.tile.inventory.IAEStackInventory;

public final class ArcaneWorkbenchPatterns {

    public static final String GRID = "GTNGArcaneGrid";

    private ArcaneWorkbenchPatterns() {}

    /** Checks that a shaped layout consumes exactly the items in the processing recipe. */
    public static boolean matchesInputs(NBTTagList layout, IAEStackInventory inputs) {
        if (layout == null || layout.tagCount() != 9) return false;
        long[] remaining = new long[inputs.getSizeInventory()];
        long total = 0;
        for (int i = 0; i < remaining.length; i++) {
            var stack = inputs.getAEStackInSlot(i);
            if (stack == null) continue;
            if (!stack.isItem() || stack.getStackSize() <= 0 || stack.getStackSize() > 9) return false;
            remaining[i] = stack.getStackSize();
            total += remaining[i];
            if (total > 9) return false;
        }
        if (total == 0) return false;
        for (int i = 0; i < 9; i++) {
            NBTTagCompound entry = layout.getCompoundTagAt(i);
            if (entry.hasNoTags()) continue;
            ItemStack required = ItemStack.loadItemStackFromNBT(entry);
            if (required == null || required.stackSize != 1) return false;
            int match = -1;
            for (int j = 0; j < remaining.length; j++) {
                if (remaining[j] == 0) continue;
                ItemStack supplied = ((IAEItemStack) inputs.getAEStackInSlot(j)).getItemStack();
                if (required.isItemEqual(supplied) && ItemStack.areItemStackTagsEqual(required, supplied)) {
                    match = j;
                    break;
                }
            }
            if (match < 0) return false;
            remaining[match]--;
            total--;
        }
        return total == 0;
    }

}
