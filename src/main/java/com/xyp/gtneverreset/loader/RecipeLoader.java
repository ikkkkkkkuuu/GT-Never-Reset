package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeCandidateCache;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingTiers;
import com.xyp.gtneverreset.common.recipe.CraftingTableRecipes;
import com.xyp.gtneverreset.common.recipe.FurnaceRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.AssemblerRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.BenderRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.BlastFurnaceRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.ExtruderRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.LargeChemicalReactorRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.DisassemblerRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.LightningProcessorRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.ModernNonCellRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.OreProcessingRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.SatelliteRockBreakingRecipes;
import com.xyp.gtneverreset.compat.etfuturum.EtFuturumCompat;
import com.xyp.gtneverreset.config.ChaosConfig;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
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
    public void init() {
        if (!ChaosConfig.enableBottleInChaos) return;
        gregtech.api.recipe.check.CheckRecipeResultRegistry
            .register(com.xyp.gtneverreset.common.crafting.chaos.result.ChaosTextResult.ofSuccess(""));
        gregtech.api.recipe.check.CheckRecipeResultRegistry
            .register(new com.xyp.gtneverreset.common.crafting.chaos.result.BottledChaosPoolFailure());
        gregtech.api.recipe.check.CheckRecipeResultRegistry
            .register(new com.xyp.gtneverreset.common.crafting.chaos.result.BottledChaosMissingCategoriesResult(0));
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

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        RecipeCandidateCache.start();
    }

    @Override
    public void serverStopping() {
        RecipeCandidateCache.stop();
    }

    public static void registerRecipes() {
        SatelliteRockBreakingRecipes.register();
        LightningProcessorRecipes.loadRecipes();
        CraftingTableRecipes.register();
        AssemblerRecipes.register();
        com.xyp.gtneverreset.common.recipe.gregtech.SpaceAssemblerRecipes.register();
        com.xyp.gtneverreset.common.recipe.gregtech.AssemblyLineRecipes.register();
        com.xyp.gtneverreset.common.recipe.gregtech.CondensateAssemblerRecipes.register();
        com.xyp.gtneverreset.common.recipe.gregtech.FluidSolidifierRecipes.register();
        com.xyp.gtneverreset.common.recipe.gregtech.LaserEngraverRecipes.register();
        LargeChemicalReactorRecipes.register();
        BenderRecipes.register();
        com.xyp.gtneverreset.common.recipe.gregtech.FormingPressRecipes.register();
        BlastFurnaceRecipes.register();
        FurnaceRecipes.register();
        OreProcessingRecipes.register();
    }

    public static void registerLateRecipes() {
        ExtruderRecipes.register();
        OreProcessingRecipes.registerExternalRecipes();
        ModernNonCellRecipes.registerLateRecipes();
        FragmentWorldCollectorRecipes.scheduleRecipes();
        DisassemblerRecipes.register();
        if (ChaosConfig.enableBottleInChaos) {
            com.xyp.gtneverreset.common.recipe.gtneverreset.ChaosNeutronActivatorRecipes.loadRecipes();
            com.xyp.gtneverreset.common.recipe.gtneverreset.ChaosCoolingRecipes.register();
            com.xyp.gtneverreset.common.recipe.gtneverreset.ChaosStellarForgeRecipes.loadRecipes();
            com.xyp.gtneverreset.common.recipe.gtneverreset.ChaosStellarForgeRecipes.loadOnServerStarted();
            com.xyp.gtneverreset.common.recipe.gtneverreset.BottleInChaosRecipes.registerRecipes();
            com.xyp.gtneverreset.common.recipe.gtneverreset.BottleInChaosRecipes.resolveLateSources();
        }
    }
}
