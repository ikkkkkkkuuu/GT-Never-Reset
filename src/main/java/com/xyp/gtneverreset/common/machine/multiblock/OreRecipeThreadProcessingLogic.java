package com.xyp.gtneverreset.common.machine.multiblock;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

public final class OreRecipeThreadProcessingLogic extends RecipeThreadProcessingLogic {

    private boolean sawCandidate;

    @Override
    public void prepareRecipe(long power) {
        super.prepareRecipe(power);
        sawCandidate = false;
    }

    @Override
    public boolean shouldRetryWithMorePower(CheckRecipeResult result) {
        // Ore lookup and passthrough classification are independent of power. A larger share can only help after
        // a candidate reached validation. Preserve non-lookup failures from the controller and its hatches.
        return sawCandidate || result != CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    @Nonnull
    protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
        sawCandidate = true;
        return super.validateRecipe(recipe);
    }

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
