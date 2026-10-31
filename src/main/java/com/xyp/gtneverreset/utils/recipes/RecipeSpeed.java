package com.xyp.gtneverreset.utils.recipes;

import com.xyp.gtneverreset.config.RecipeSpeedConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

public final class RecipeSpeed {

    public static int getModifiedRecipeDuration(int original) {
        // Recipe registration can precede this mod's preInit.
        ConfigLoader.ensureLoaded();
        switch (RecipeSpeedConfig.mode) {
            case 1:
                return Math.max(1, RecipeSpeedConfig.fixedDuration);
            case 2:
                return Math.max(1, (int) (original * RecipeSpeedConfig.multiplier));
            default:
                return original;
        }
    }
}
