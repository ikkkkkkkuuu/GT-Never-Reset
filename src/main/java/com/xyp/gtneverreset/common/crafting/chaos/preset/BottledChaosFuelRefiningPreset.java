package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CarbonDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CoalGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrogenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrogenPeroxide;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NitricAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NitrogenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.OxygenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFurnace;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
import static gregtech.api.util.GTRecipeConstants.NFR_COIL_TIER;

import java.util.Arrays;

import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.MaterialsElements;

public final class BottledChaosFuelRefiningPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        registerRocketFuels(catalog);
        registerNaquadahFuels(catalog);
    }

    private static void registerRocketFuels(BottledChaosRecipeCatalog catalog) {
        rocketFuel(
            catalog,
            2,
            64,
            6_300,
            TierEU.RECIPE_IV,
            1_200,
            new FluidStack(GTPPFluids.RP1RocketFuel, 4_000),
            new FluidStack(CoalGas, 80_000),
            new FluidStack(OxygenGas, 10_000));
        rocketFuel(
            catalog,
            3,
            32,
            7_400,
            TierEU.RECIPE_EV,
            800,
            new FluidStack(GTPPFluids.DenseHydrazineFuelMixture, 4_000),
            new FluidStack(HydrogenGas, 12_000),
            new FluidStack(OxygenGas, 8_000),
            new FluidStack(NitrogenGas, 10_000),
            new FluidStack(HydrogenPeroxide, 4_000));
        rocketFuel(
            catalog,
            4,
            12,
            8_100,
            TierEU.RECIPE_IV,
            1_200,
            new FluidStack(GTPPFluids.CN3H7O3RocketFuel, 4_000),
            new FluidStack(HydrogenGas, 14_000),
            new FluidStack(NitrogenGas, 6_000),
            new FluidStack(NitricAcid, 3_000),
            new FluidStack(HydrogenPeroxide, 2_000));
        rocketFuel(
            catalog,
            5,
            13,
            9_000,
            TierEU.RECIPE_IV,
            1_200,
            new FluidStack(GTPPFluids.H8N4C2O4RocketFuel, 5_000),
            new FluidStack(HydrogenGas, 15_000),
            new FluidStack(NitrogenGas, 7_000),
            new FluidStack(NitricAcid, 3_000),
            new FluidStack(OxygenGas, 1_000),
            new FluidStack(HydrogenPeroxide, 2_000));
    }

    private static void rocketFuel(BottledChaosRecipeCatalog catalog, int circuit, int carbon, int heat, long eut,
        int duration, FluidStack output, FluidStack... inputs) {
        var recipe = GTValues.RA.stdBuilder()
            .itemInputs(CarbonDust.getItemStack(carbon))
            .fluidInputs(inputs)
            .fluidOutputs(output)
            .metadata(COIL_HEAT, heat)
            .duration(duration)
            .eut(eut)
            .build()
            .orElseThrow();
        var step = catalog.markFixedRecipe(VacuumFurnace, CarbonDust, output.getFluid(), recipe);
        Object[] products = { output.getFluid() };
        var entry = circuit == 2 ? catalog.registerChain("rocket_fuel", 4, eut, CarbonDust, products, step)
            : catalog.registerAlternativeChain("rocket_fuel", eut, CarbonDust, products, step);
        entry.requireCircuit(circuit);
    }

    private static void registerNaquadahFuels(BottledChaosRecipeCatalog catalog) {
        naquadahFuel(
            catalog,
            "naquadah_fuel_1",
            8,
            1,
            TierEU.RECIPE_UHV,
            100,
            GGMaterial.naquadahBasedFuelMkI.getFluidOrGas(1_250),
            GTValues.RA.stdBuilder()
                .fluidInputs(
                    GGMaterial.lightNaquadahFuel.getFluidOrGas(2_875),
                    GGMaterial.heavyNaquadahFuel.getFluidOrGas(1_375),
                    GGMaterial.atomicSeparationCatalyst.getMolten(750),
                    Materials.Duranium.getMolten(144)));
        naquadahFuel(
            catalog,
            "naquadah_fuel_2",
            16,
            1,
            TierEU.RECIPE_UEV,
            120,
            GGMaterial.naquadahBasedFuelMkII.getFluidOrGas(1_250),
            GTValues.RA.stdBuilder()
                .itemInputs(ItemList.Gravistar.get(2))
                .fluidInputs(
                    GGMaterial.lightNaquadahFuel.getFluidOrGas(2_875),
                    GGMaterial.heavyNaquadahFuel.getFluidOrGas(1_375),
                    GGMaterial.atomicSeparationCatalyst.getMolten(750),
                    GGMaterial.naquadahGas.getFluidOrGas(5_000),
                    Materials.Americium.getPlasma(288)));
        naquadahFuel(
            catalog,
            "naquadah_fuel_3",
            0,
            2,
            TierEU.RECIPE_UIV,
            140,
            GGMaterial.naquadahBasedFuelMkIII.getFluidOrGas(1_000),
            GTValues.RA.stdBuilder()
                .itemInputs(GGMaterial.extremelyUnstableNaquadah.get(OrePrefixes.dust, 32))
                .fluidInputs(
                    GGMaterial.lightNaquadahFuel.getFluidOrGas(11_500),
                    GGMaterial.heavyNaquadahFuel.getFluidOrGas(5_500),
                    GGMaterial.atomicSeparationCatalyst.getMolten(3_000),
                    MaterialsElements.STANDALONE.CHRONOMATIC_GLASS.getFluidStack(5_184),
                    GGMaterial.uraniumBasedLiquidFuelExcited.getFluidOrGas(1_500),
                    GGMaterial.plutoniumBasedLiquidFuelExcited.getFluidOrGas(750)));
        naquadahFuel(
            catalog,
            "naquadah_fuel_4",
            0,
            3,
            TierEU.RECIPE_UMV,
            160,
            GGMaterial.naquadahBasedFuelMkIV.getFluidOrGas(1_000),
            GTValues.RA.stdBuilder()
                .itemInputs(Materials.DraconiumAwakened.getBlocks(64))
                .fluidInputs(
                    GGMaterial.lightNaquadahFuel.getFluidOrGas(46_000),
                    GGMaterial.heavyNaquadahFuel.getFluidOrGas(22_000),
                    GGMaterial.atomicSeparationCatalyst.getMolten(12_000),
                    MaterialsElements.STANDALONE.CHRONOMATIC_GLASS.getFluidStack(19_440),
                    GGMaterial.orundum.getMolten(9_216),
                    MaterialsElements.STANDALONE.HYPOGEN.getFluidStack(480)));
        naquadahFuel(
            catalog,
            "naquadah_fuel_5",
            0,
            4,
            TierEU.RECIPE_UXV,
            180,
            GGMaterial.naquadahBasedFuelMkV.getFluidOrGas(1_000),
            GTValues.RA.stdBuilder()
                .itemInputs(
                    GTOreDictUnificator.get(OrePrefixes.stick, Materials.TranscendentMetal, 20),
                    Materials.HotProtoHalkonite.getPlates(16))
                .fluidInputs(
                    GGMaterial.lightNaquadahFuel.getFluidOrGas(184_000),
                    GGMaterial.heavyNaquadahFuel.getFluidOrGas(88_000),
                    GGMaterial.atomicSeparationCatalyst.getMolten(48_000),
                    Materials.Infinity.getMolten(2_304),
                    Materials.SpaceTime.getMolten(144)));
        for (int circuit = 1; circuit <= 2; circuit++) {
            FluidStack[] feed = { GGMaterial.lightNaquadahFuel.getFluidOrGas(368_000),
                GGMaterial.heavyNaquadahFuel.getFluidOrGas(176_000),
                GGMaterial.atomicSeparationCatalyst.getMolten(96_000),
                MaterialsElements.STANDALONE.CELESTIAL_TUNGSTEN.getFluidStack(5_760),
                Materials.Mellion.getMolten(4_608), GGMaterial.shirabon.getMolten(288),
                Materials.RawStarMatter.getFluid(80) };
            if (circuit == 2) {
                feed = Arrays.copyOf(feed, feed.length + 1);
                feed[feed.length - 1] = Materials.Universium.getMolten(18);
            }
            naquadahFuel(
                catalog,
                "naquadah_fuel_6",
                0,
                4,
                TierEU.RECIPE_MAX,
                circuit == 1 ? 200 : 100,
                GGMaterial.naquadahBasedFuelMkVI.getFluidOrGas(circuit == 1 ? 1_500 : 3_750),
                GTValues.RA.stdBuilder()
                    .fluidInputs(feed)).requireCircuit(circuit);
        }
    }

    private static BottledChaosRecipeCatalog.Entry naquadahFuel(BottledChaosRecipeCatalog catalog, String tag,
        int capacity, int coilTier, long eut, int duration, FluidStack output, GTRecipeBuilder builder) {
        var recipe = builder.fluidOutputs(output)
            .metadata(NFR_COIL_TIER, coilTier)
            .duration(duration)
            .eut(eut)
            .build()
            .orElseThrow();
        var feed = GGMaterial.lightNaquadahFuel.getFluidOrGas(1)
            .getFluid();
        var step = catalog
            .markFixedRecipe(GoodGeneratorRecipeMaps.naquadahFuelRefineFactoryRecipes, feed, output.getFluid(), recipe);
        Object[] products = { output.getFluid() };
        return capacity > 0 ? catalog.registerChain(tag, capacity, eut, feed, products, step)
            : catalog.registerAlternativeChain(tag, eut, feed, products, step);
    }
}
