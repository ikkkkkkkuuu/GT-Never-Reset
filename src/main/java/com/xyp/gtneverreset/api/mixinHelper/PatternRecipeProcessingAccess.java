package com.xyp.gtneverreset.api.mixinHelper;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

/** Routes virtual processing calls before a subclass can read mode-dependent settings. */
public interface PatternRecipeProcessingAccess {

    boolean gtnr$cachePattern(IDualInputInventoryWithPattern inventory);

    CheckRecipeResult gtnr$processPattern();

    void gtnr$resetRouting();
}
