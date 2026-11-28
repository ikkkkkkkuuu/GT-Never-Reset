package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CarbonDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFurnace;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;
import com.xyp.gtneverreset.utils.recipes.FuelRefiningRecipeSource;
import com.xyp.gtneverreset.utils.recipes.FuelRefiningRecipeSource.RefiningRecipe;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import goodgenerator.items.GGMaterial;
import gregtech.api.recipe.RecipeMap;

public final class BottledChaosFuelRefiningPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        for (RefiningRecipe recipe : FuelRefiningRecipeSource.rocketFuels()) {
            registerFuel(catalog, VacuumFurnace, CarbonDust, recipe);
        }
        var lightFuel = GGMaterial.lightNaquadahFuel.getFluidOrGas(1)
            .getFluid();
        for (RefiningRecipe recipe : FuelRefiningRecipeSource.naquadahFuels()) {
            registerFuel(catalog, GoodGeneratorRecipeMaps.naquadahFuelRefineFactoryRecipes, lightFuel, recipe);
        }
    }

    private static void registerFuel(BottledChaosRecipeCatalog catalog, RecipeMap<?> source, Object feed,
        RefiningRecipe formula) {
        var recipe = formula.recipe();
        var output = recipe.mFluidOutputs[0].getFluid();
        var step = catalog.markFixedRecipe(source, feed, output, recipe);
        Object[] products = { output };
        var entry = formula.capacity() > 0
            ? catalog.registerChain(formula.tag(), formula.capacity(), recipe.mEUt, feed, products, step)
            : catalog.registerAlternativeChain(formula.tag(), recipe.mEUt, feed, products, step);
        if (formula.circuit() > 0) entry.requireCircuit(formula.circuit());
    }
}
