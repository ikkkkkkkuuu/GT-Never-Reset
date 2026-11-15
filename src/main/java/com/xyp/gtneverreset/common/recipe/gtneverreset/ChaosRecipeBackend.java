package com.xyp.gtneverreset.common.recipe.gtneverreset;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBackendPropertiesBuilder;

/**
 * If recipe input items include GT Material meta item and stack size is larger than 64, use this class to replace
 * RecipeMapBackend .
 */
public class ChaosRecipeBackend extends RecipeMapBackend {

    public ChaosRecipeBackend(RecipeMapBackendPropertiesBuilder propertiesBuilder) {
        super(propertiesBuilder);
    }

    /**
     * Do nothing.
     */
    @Override
    public void reInit() {}
}
