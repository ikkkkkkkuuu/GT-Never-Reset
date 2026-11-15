package com.xyp.gtneverreset.common.recipe.gtneverreset;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosProcessAlternatives;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

/** Registers Bottle in Chaos processing chains and their display recipes in the required startup order. */
public final class BottleInChaosRecipes {

    public static void registerRecipes() {
        BottledChaosRecipeCatalog.initialize();
        ChaosProcessingRecipes.registerRecipes();
        ChaosStructureModuleRecipes.registerRecipes();
    }

    /** Resolve source substitutions after the native stellar-forge recipes are available. */
    public static void resolveLateSources() {
        BottledChaosProcessAlternatives.resolve();
    }
}
