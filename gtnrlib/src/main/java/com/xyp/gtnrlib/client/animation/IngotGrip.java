package com.xyp.gtnrlib.client.animation;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** The first-person passes share the ore lookup for the currently equipped item and metadata. */
final class IngotGrip {

    private static Item lastItem;
    private static int lastDamage;
    private static boolean ingot;

    static boolean matches(ItemStack stack) {
        if (stack == null || stack.getItem() instanceof ItemBlock) return false;
        if (stack.getItem() != lastItem || stack.getItemDamage() != lastDamage) {
            lastItem = stack.getItem();
            lastDamage = stack.getItemDamage();
            ingot = false;
            for (int ore : OreDictionary.getOreIDs(stack)) {
                if (OreDictionary.getOreName(ore)
                    .startsWith("ingot")) {
                    ingot = true;
                    break;
                }
            }
        }
        return ingot;
    }

    static void invalidate() {
        lastItem = null;
    }

    private IngotGrip() {}
}
