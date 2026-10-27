package com.xyp.gtneverreset.common.recipe.ae2;

import java.util.Arrays;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class WirelessTerminalUpgradeRecipe extends ShapelessRecipes {

    private final Item terminal;
    private final ItemStack card;
    private final String upgradeKey;

    public WirelessTerminalUpgradeRecipe(ItemStack terminal, ItemStack card, String upgradeKey) {
        super(install(terminal, upgradeKey), Arrays.asList(terminal.copy(), card.copy()));
        this.terminal = terminal.getItem();
        this.card = card.copy();
        this.upgradeKey = upgradeKey;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean foundTerminal = false;
        boolean foundCard = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) continue;
            if (stack.stackSize <= 0) return false;
            if (stack.getItem() == terminal && !foundTerminal) {
                if (stack.hasTagCompound() && stack.getTagCompound()
                    .getBoolean(upgradeKey)) return false;
                foundTerminal = true;
            } else if (card.isItemEqual(stack) && !foundCard) {
                foundCard = true;
            } else {
                return false;
            }
        }
        return foundTerminal && foundCard;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() == terminal) return install(stack, upgradeKey);
        }
        return null;
    }

    private static ItemStack install(ItemStack terminal, String upgradeKey) {
        ItemStack installed = terminal.copy();
        installed.stackSize = 1;
        if (!installed.hasTagCompound()) installed.setTagCompound(new NBTTagCompound());
        installed.getTagCompound()
            .setBoolean(upgradeKey, true);
        return installed;
    }
}
