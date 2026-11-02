package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.recipes.FluidContainerRecipeConverter;

import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

public final class ModernNonCellRecipes {

    private static boolean recipesConverted;

    public static void registerLateRecipes() {
        if (recipesConverted) return;
        recipesConverted = true;
        FluidContainerRecipeConverter
            .convertRecipes(RecipeMaps.mixerRecipes, GTNRRecipeMaps.MODERN_MIXER_RECIPES, 6, 1, 2, 1);
        FluidContainerRecipeConverter
            .convertRecipes(RecipeMaps.electrolyzerRecipes, GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES, 2, 6, 1, 6);
        FluidContainerRecipeConverter
            .convertRecipes(RecipeMaps.chemicalReactorRecipes, GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES, 6, 6, 6, 6);
        GTRecipeBuilder.builder()
            .itemInputs(Materials.Sulfur.getDust(1))
            .circuit(24)
            .fluidInputs(Materials.Oxygen.getGas(3_000), Materials.Water.getFluid(1_000))
            .fluidOutputs(Materials.SulfuricAcid.getFluid(1_000))
            .duration(24 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES);
        FluidContainerRecipeConverter
            .convertRecipes(RecipeMaps.centrifugeRecipes, GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES, 2, 6, 1, 6);
    }
}
