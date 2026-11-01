package com.xyp.gtneverreset.qa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.xyp.gtneverreset.compat.etfuturum.EtFuturumCompat;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.util.GTModHandler;

@Mod(
    modid = "etfuturumrecipeqa",
    name = "Et Futurum recipe QA",
    version = "1",
    dependencies = "after:gtneverreset;required-after:etfuturum")
public final class EtFuturumRecipeChecks {

    private final Map<String, List<ItemStack>> ores = new HashMap<>();
    private List<IRecipe> recipes;
    private List<IRecipe> bufferedRecipes;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GregTechAPI.sBeforeGTLoad.add(0, () -> {
            for (String name : OreDictionary.getOreNames()) {
                ores.put(name, new ArrayList<>(OreDictionary.getOres(name)));
            }
            recipes = new ArrayList<>(
                CraftingManager.getInstance()
                    .getRecipeList());
            bufferedRecipes = new ArrayList<>(GTModHandler.sBufferRecipeList);
        });
        GregTechAPI.sBeforeGTLoad.add(() -> {
            require(GameRegistry.findItemStack("etfuturum", "blast_furnace", 1) != null, "EFR block unavailable");
            for (Map.Entry<String, List<ItemStack>> entry : ores.entrySet()) {
                List<ItemStack> actual = OreDictionary.getOres(entry.getKey());
                require(actual.containsAll(entry.getValue()), "removed ore entry: " + entry.getKey());
                if (!entry.getKey()
                    .equals("craftingBlastFurnace")) {
                    require(actual.equals(entry.getValue()), "changed unrelated ore: " + entry.getKey());
                }
            }
            for (String name : OreDictionary.getOreNames()) {
                require(ores.containsKey(name) || name.equals("craftingBlastFurnace"), "added unrelated ore: " + name);
            }
            require(
                recipes.equals(
                    CraftingManager.getInstance()
                        .getRecipeList()),
                "changed crafting recipes");
            require(bufferedRecipes.equals(GTModHandler.sBufferRecipeList), "changed buffered crafting recipes");
            List<ItemStack> blastFurnaces = new ArrayList<>(OreDictionary.getOres("craftingBlastFurnace"));
            ItemStack furnace = GameRegistry.findItemStack("etfuturum", "blast_furnace", 1);
            require(
                blastFurnaces.stream()
                    .anyMatch(stack -> stack.isItemEqual(furnace)),
                "missing EFR ore entry");
            EtFuturumCompat.registerBlastFurnaceOre();
            require(blastFurnaces.equals(OreDictionary.getOres("craftingBlastFurnace")), "duplicate ore entry");
            System.out
                .println("EFR_RECIPE_QA: ore restoration, unrelated registry preservation and idempotence passed");
        });
    }

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        for (ItemList target : new ItemList[] { ItemList.Machine_Bronze_Boiler, ItemList.Machine_Steel_Boiler,
            ItemList.Machine_Bricked_BlastFurnace, ItemList.Machine_Multi_BlastFurnace }) {
            boolean matched = false;
            for (Object entry : CraftingManager.getInstance()
                .getRecipeList()) {
                if (!(entry instanceof ShapedOreRecipe recipe) || recipe.getRecipeOutput() == null
                    || !recipe.getRecipeOutput()
                        .isItemEqual(target.get(1)))
                    continue;
                InventoryCrafting grid = new InventoryCrafting(new Container() {

                    @Override
                    public boolean canInteractWith(EntityPlayer player) {
                        return true;
                    }
                }, 3, 3);
                Object[] input = recipe.getInput();
                for (int i = 0; i < input.length; i++) {
                    Object ingredient = input[i];
                    if (ingredient instanceof List<?>alternatives) {
                        require(!alternatives.isEmpty(), "empty ingredient for " + target);
                        ingredient = alternatives.get(0);
                    }
                    if (ingredient instanceof ItemStack stack) grid.setInventorySlotContents(i, stack.copy());
                }
                if (recipe.matches(grid, null)) matched = true;
            }
            require(matched, "no matching crafting recipe for " + target);
            System.out.println("EFR_RECIPE_QA: matching crafting recipe for " + target);
        }
        System.out.println("EFR_RECIPE_QA: PASS");
        System.exit(0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
