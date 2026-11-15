package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps.NeutronActivatorWithEURecipeMap;
import static goodgenerator.api.recipe.GoodGeneratorRecipeMaps.neutronActivatorRecipes;

import gregtech.api.util.GTRecipe;

public class ChaosNeutronActivatorRecipes {

    public static void loadRecipes() {

        for (GTRecipe recipeOrigin : neutronActivatorRecipes.getAllRecipes()) {
            GTRecipe recipeNew = recipeOrigin.copy();
            int kn = recipeNew.mSpecialValue / 10000;
            recipeNew.mEUt = kn * kn;
            NeutronActivatorWithEURecipeMap.addRecipe(recipeNew);
        }

    }
}
