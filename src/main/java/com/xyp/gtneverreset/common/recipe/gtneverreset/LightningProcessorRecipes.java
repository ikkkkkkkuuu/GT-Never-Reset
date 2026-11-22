package com.xyp.gtneverreset.common.recipe.gtneverreset;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class LightningProcessorRecipes {

    private LightningProcessorRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .fluidInputs(Materials.Air.getGas(10000))
            .fluidOutputs(Materials.NitricOxide.getGas(1000))
            .duration(200)
            .eut(TierEU.RECIPE_MV)
            .addTo(GTNRRecipeMaps.LightningProcessorRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(
                Materials.Beryllium.getDust(1),
                Materials.Potassium.getDust(4),
                GTUtility.getIntegratedCircuit(1))
            .fluidInputs(Materials.Nitrogen.getGas(5000))
            .itemOutputs(Materials.EnderPearl.getDust(10))
            .duration(400)
            .eut(TierEU.RECIPE_LV)
            .addTo(GTNRRecipeMaps.LightningProcessorRecipes);
    }
}
