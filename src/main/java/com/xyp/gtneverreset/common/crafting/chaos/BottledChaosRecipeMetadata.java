package com.xyp.gtneverreset.common.crafting.chaos;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor;

import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;

/** Shared NEI display metadata for folded recipes. */
public final class BottledChaosRecipeMetadata {

    public static final RecipeMetadataKey<BottledChaosRecipeProcessor.DisplayRecipe> DISPLAY_BATCH = SimpleRecipeMetadataKey
        .create(BottledChaosRecipeProcessor.DisplayRecipe.class, "bottled_chaos_display_batch");

    private BottledChaosRecipeMetadata() {}
}
