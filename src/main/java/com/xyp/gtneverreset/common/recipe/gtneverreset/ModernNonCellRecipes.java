package com.xyp.gtneverreset.common.recipe.gtneverreset;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.recipes.FluidContainerRecipeConverter;

import gregtech.api.recipe.RecipeMaps;

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
        FluidContainerRecipeConverter
            .convertRecipes(RecipeMaps.centrifugeRecipes, GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES, 2, 6, 1, 6);
    }
}
