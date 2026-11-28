package com.xyp.gtneverreset.utils.recipes;

import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
import static gregtech.api.util.GTRecipeConstants.NFR_COIL_TIER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.fluids.FluidStack;

import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.MaterialsElements;

/** Fuel formulas shared by the Bottled Chaos and Platinum Gate recipe maps. */
public final class FuelRefiningRecipeSource {

    private FuelRefiningRecipeSource() {}

    public static List<RefiningRecipe> rocketFuels() {
        return List.of(
            rocketFuel(
                2,
                64,
                6_300,
                TierEU.RECIPE_IV,
                1_200,
                new FluidStack(GTPPFluids.RP1RocketFuel, 4_000),
                new FluidStack(GTPPFluids.CoalGas, 80_000),
                Materials.Oxygen.getGas(10_000)),
            rocketFuel(
                3,
                32,
                7_400,
                TierEU.RECIPE_EV,
                800,
                new FluidStack(GTPPFluids.DenseHydrazineFuelMixture, 4_000),
                Materials.Hydrogen.getGas(12_000),
                Materials.Oxygen.getGas(8_000),
                Materials.Nitrogen.getGas(10_000),
                new FluidStack(GTPPFluids.HydrogenPeroxide, 4_000)),
            rocketFuel(
                4,
                12,
                8_100,
                TierEU.RECIPE_IV,
                1_200,
                new FluidStack(GTPPFluids.CN3H7O3RocketFuel, 4_000),
                Materials.Hydrogen.getGas(14_000),
                Materials.Nitrogen.getGas(6_000),
                Materials.NitricAcid.getFluid(3_000),
                new FluidStack(GTPPFluids.HydrogenPeroxide, 2_000)),
            rocketFuel(
                5,
                13,
                9_000,
                TierEU.RECIPE_IV,
                1_200,
                new FluidStack(GTPPFluids.H8N4C2O4RocketFuel, 5_000),
                Materials.Hydrogen.getGas(15_000),
                Materials.Nitrogen.getGas(7_000),
                Materials.NitricAcid.getFluid(3_000),
                Materials.Oxygen.getGas(1_000),
                new FluidStack(GTPPFluids.HydrogenPeroxide, 2_000)));
    }

    private static RefiningRecipe rocketFuel(int circuit, int carbon, int heat, long eut, int duration,
        FluidStack output, FluidStack... inputs) {
        GTRecipe recipe = GTValues.RA.stdBuilder()
            .itemInputs(Materials.Carbon.getDust(carbon))
            .fluidInputs(inputs)
            .fluidOutputs(output)
            .metadata(COIL_HEAT, heat)
            .duration(duration)
            .eut(eut)
            .build()
            .orElseThrow();
        return new RefiningRecipe("rocket_fuel", circuit == 2 ? 4 : 0, circuit, recipe);
    }

    public static List<RefiningRecipe> naquadahFuels() {
        List<RefiningRecipe> recipes = new ArrayList<>();
        recipes.add(
            naquadahFuel(
                "naquadah_fuel_1",
                8,
                0,
                1,
                TierEU.RECIPE_UHV,
                100,
                GGMaterial.naquadahBasedFuelMkI.getFluidOrGas(1_250),
                GTValues.RA.stdBuilder()
                    .fluidInputs(
                        GGMaterial.lightNaquadahFuel.getFluidOrGas(2_875),
                        GGMaterial.heavyNaquadahFuel.getFluidOrGas(1_375),
                        GGMaterial.atomicSeparationCatalyst.getMolten(750),
                        Materials.Duranium.getMolten(144))));
        recipes.add(
            naquadahFuel(
                "naquadah_fuel_2",
                16,
                0,
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
                        Materials.Americium.getPlasma(288))));
        recipes.add(
            naquadahFuel(
                "naquadah_fuel_3",
                0,
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
                        GGMaterial.plutoniumBasedLiquidFuelExcited.getFluidOrGas(750))));
        recipes.add(
            naquadahFuel(
                "naquadah_fuel_4",
                0,
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
                        MaterialsElements.STANDALONE.HYPOGEN.getFluidStack(480))));
        recipes.add(
            naquadahFuel(
                "naquadah_fuel_5",
                0,
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
                        Materials.SpaceTime.getMolten(144))));
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
            recipes.add(
                naquadahFuel(
                    "naquadah_fuel_6",
                    0,
                    circuit,
                    4,
                    TierEU.RECIPE_MAX,
                    circuit == 1 ? 200 : 100,
                    GGMaterial.naquadahBasedFuelMkVI.getFluidOrGas(circuit == 1 ? 1_500 : 3_750),
                    GTValues.RA.stdBuilder()
                        .fluidInputs(feed)));
        }
        return recipes;
    }

    private static RefiningRecipe naquadahFuel(String tag, int capacity, int circuit, int coilTier, long eut,
        int duration, FluidStack output, GTRecipeBuilder builder) {
        GTRecipe recipe = builder.fluidOutputs(output)
            .metadata(NFR_COIL_TIER, coilTier)
            .duration(duration)
            .eut(eut)
            .build()
            .orElseThrow();
        return new RefiningRecipe(tag, capacity, circuit, recipe);
    }

    public record RefiningRecipe(String tag, int capacity, int circuit, GTRecipe recipe) {}
}
