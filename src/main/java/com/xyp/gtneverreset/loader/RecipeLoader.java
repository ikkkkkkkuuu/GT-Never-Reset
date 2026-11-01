package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingTiers;
import com.xyp.gtneverreset.common.recipe.CraftingTableRecipes;
import com.xyp.gtneverreset.common.recipe.FurnaceRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.AssemblerRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.BenderRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.ExtruderRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.ModernNonCellRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.OreProcessingRecipes;
import com.xyp.gtneverreset.compat.etfuturum.EtFuturumCompat;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Mods;

public final class RecipeLoader implements IContentLoader {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        if (Mods.EtFuturumRequiem.isModLoaded()) {
            // All mods finish preInit before this hook runs, ahead of GT's machine recipes.
            GregTechAPI.sBeforeGTLoad.add(EtFuturumCompat::registerBlastFurnaceOre);
        }
    }

    @Override
    public void postInit() {
        registerRecipes();
    }

    @Override
    public void loadComplete() {
        registerLateRecipes();
        if (Mods.Forestry.isModLoaded()) BeeBreedingTiers.initialize();
    }

    public static void registerRecipes() {
        CraftingTableRecipes.register();
        AssemblerRecipes.register();
        BenderRecipes.register();
        FurnaceRecipes.register();
        OreProcessingRecipes.register();
    }

    public static void registerLateRecipes() {
        ExtruderRecipes.register();
        OreProcessingRecipes.registerExternalRecipes();
        ModernNonCellRecipes.registerLateRecipes();
        FragmentWorldCollectorRecipes.scheduleRecipes();
    }
}
