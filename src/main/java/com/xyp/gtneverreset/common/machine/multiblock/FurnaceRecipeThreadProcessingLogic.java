package com.xyp.gtneverreset.common.machine.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.utils.machine.RecipeThreadProcessingLogic;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

public final class FurnaceRecipeThreadProcessingLogic extends RecipeThreadProcessingLogic {

    @Override
    protected Stream<GTRecipe> findRecipeMatches(RecipeMap<?> map) {
        if (map != RecipeMaps.furnaceRecipes) return super.findRecipeMatches(map);
        // The furnace backend examines only the first input stack in each query.
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack stack : inputItems) {
            if (stack == null || stack.stackSize <= 0) continue;
            if (inputs.stream()
                .noneMatch(other -> GTUtility.areStacksEqual(other, stack))) inputs.add(stack);
        }
        return inputs.stream()
            .flatMap(
                stack -> map.findRecipeQuery()
                    .items(stack)
                    .findAll());
    }
}
