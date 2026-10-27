package com.xyp.gtneverreset.utils.recipes;

import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

public final class RecipeSpeed {

    private RecipeSpeed() {}

    public static int getModifiedRecipeDuration(int original) {
        // Recipe registration can precede this mod's preInit.
        ConfigLoader.ensureLoaded();
        switch (MainConfig.recipeSpeed.mode) {
            case 1:
                return Math.max(1, MainConfig.recipeSpeed.fixedDuration);
            case 2:
                return Math.max(1, (int) (original * MainConfig.recipeSpeed.multiplier));
            default:
                return original;
        }
    }
}
