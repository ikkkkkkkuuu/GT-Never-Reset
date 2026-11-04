package com.xyp.gtnrlib.client.animation;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** The first-person passes share the ore lookup for the currently equipped item and metadata. */
final class OreGrip {

    private static Item lastItem;
    private static int lastDamage;
    private static boolean ingot;
    private static ToolGrip tool;

    static boolean isIngot(ItemStack stack) {
        if (stack == null || stack.getItem() instanceof ItemBlock) return false;
        update(stack);
        return ingot;
    }

    static ToolGrip tool(ItemStack stack) {
        update(stack);
        return tool;
    }

    private static void update(ItemStack stack) {
        if (stack.getItem() != lastItem || stack.getItemDamage() != lastDamage) {
            lastItem = stack.getItem();
            lastDamage = stack.getItemDamage();
            ingot = false;
            tool = null;
            for (int ore : OreDictionary.getOreIDs(stack)) {
                String name = OreDictionary.getOreName(ore);
                if (name.startsWith("ingot")) ingot = true;
                if (name.startsWith("craftingTool")) {
                    ToolGrip next = switch (name) {
                        case "craftingToolSword", "craftingToolKnife", "craftingToolBlade", "craftingToolElectricButcherKnife" -> ToolGrip.SWORD;
                        case "craftingToolPickaxe", "craftingToolDrill", "craftingToolMiningDrill", "craftingToolJackHammer" -> ToolGrip.PICKAXE;
                        case "craftingToolAxe", "craftingToolSaw" -> ToolGrip.AXE;
                        case "craftingToolShovel", "craftingToolSpade", "craftingToolScoop" -> ToolGrip.SHOVEL;
                        case "craftingToolHoe", "craftingToolPlow" -> ToolGrip.HOE;
                        default -> ToolGrip.HANDHELD;
                    };
                    if (tool == null || next.compareTo(tool) < 0) tool = next;
                }
            }
        }
    }

    static void invalidate() {
        lastItem = null;
    }

    private OreGrip() {}
}
