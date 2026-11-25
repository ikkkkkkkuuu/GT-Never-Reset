package com.xyp.gtneverreset.common.recipe.gregtech;

import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.fluids.GTPPFluids;

public final class LargeChemicalReactorRecipes {

    private LargeChemicalReactorRecipes() {}

    public static void register() {
        registerZsm5Recipes();
        GTRecipeBuilder.builder()
            .itemInputs(GTUtility.copyAmount(0, Materials.Platinum.getDust(1)))
            .circuit(24)
            .fluidInputs(Materials.Hydrogen.getGas(2000), Materials.Oxygen.getGas(2000))
            .fluidOutputs(new FluidStack(GTPPFluids.HydrogenPeroxide, 1000))
            .duration(20 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.multiblockChemicalReactorRecipes);
    }

    private static void registerZsm5Recipes() {
        GTRecipeBuilder.builder()
            .itemInputs(Materials.Aluminium.getDust(2))
            .circuit(1)
            .itemOutputs(GTNRItemList.AluminiumSulfateDust.get(17))
            .fluidInputs(Materials.SulfuricAcid.getFluid(3_000))
            .fluidOutputs(Materials.Hydrogen.getGas(6_000))
            .duration(5 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.multiblockChemicalReactorRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Shape_Mold_Plate.get(0),
                Materials.SodiumHydroxide.getDust(2),
                GTNRItemList.AluminiumSulfateDust.get(17),
                Materials.SiliconDioxide.getDust(6))
            .itemOutputs(GTNRItemList.Zsm5Plate.get(1))
            .fluidInputs(Materials.Water.getFluid(1_000), Materials.Ethanol.getFluid(1))
            .duration(30 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(RecipeMaps.multiblockChemicalReactorRecipes);
    }
}
