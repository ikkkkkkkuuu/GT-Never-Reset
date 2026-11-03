package com.xyp.gtneverreset.common.recipe.gregtech;

import static gregtech.api.enums.TierEU.RECIPE_HV;
import static gregtech.api.enums.TierEU.RECIPE_MV;
import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.BlastFurnaceGasStat;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class BlastFurnaceRecipes {

    public static void register() {
        GTRecipeBuilder.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 64),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 64),
                ItemList.GalliumArsenideCrystal.get(1))
            .circuit(2)
            .itemOutputs(ItemList.Circuit_Silicon_Ingot.get(4))
            .duration(30 * MINUTES)
            .eut(RECIPE_MV)
            .metadata(COIL_HEAT, 1784)
            .addTo(blastFurnaceRecipes);

        registerPhosphorusDopedSilicon(BlastFurnaceGasStat.Nitrogen);
        registerPhosphorusDopedSilicon(BlastFurnaceGasStat.Helium);
    }

    private static void registerPhosphorusDopedSilicon(BlastFurnaceGasStat gas) {
        GTRecipeBuilder.builder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 64),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.SiliconSG, 64),
                ItemList.GalliumArsenideCrystal.get(1),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Phosphorus, 16))
            .circuit(3)
            .fluidInputs(GTUtility.copyAmount(16_000, gas.gas))
            .itemOutputs(ItemList.Circuit_Silicon_Ingot2.get(2))
            .duration((int) (20 * MINUTES * gas.recipeTimeMultiplier))
            .eut(RECIPE_HV)
            .metadata(COIL_HEAT, 2484)
            .addTo(blastFurnaceRecipes);
    }
}
