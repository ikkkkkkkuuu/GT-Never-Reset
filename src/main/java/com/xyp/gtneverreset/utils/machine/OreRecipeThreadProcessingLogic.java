package com.xyp.gtneverreset.utils.machine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.loader.GTNRRecipeMaps;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTUtility;

public final class OreRecipeThreadProcessingLogic extends RecipeThreadProcessingLogic {

    @Override
    @Nonnull
    protected CheckRecipeResult processRecipe() {
        RecipeMap<?> map = getCurrentRecipeMap();
        if (map != GTNRRecipeMaps.ORE_PROCESSING_RECIPES || inputItems == null) return super.processRecipe();
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack input : inputItems) {
            if (input == null || input.stackSize <= 0 || GTUtility.isAnyIntegratedCircuit(input)) continue;
            // Classification must not depend on available power, parallel limits or the selected recipe lock.
            if (map.findRecipeQuery()
                .items(input)
                .notUnificated(true)
                .dontCheckStackSizes(true)
                .find() != null) continue;
            outputs.add(input.copy());
            input.stackSize = 0;
        }
        if (outputs.isEmpty()) return super.processRecipe();
        // Use the existing input transaction and persistent output ledger for the entire batch.
        outputItems = outputs.toArray(new ItemStack[0]);
        outputFluids = null;
        calculatedEut = 0;
        duration = 1;
        calculatedParallels = 1;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }
}
