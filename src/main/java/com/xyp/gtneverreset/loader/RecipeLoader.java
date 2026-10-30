package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.recipe.CraftingTableRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.AssemblerRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.BenderRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.ExtruderRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.FurnaceRecipes;
import com.xyp.gtneverreset.common.recipe.gregtech.MixerRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.ModernNonCellRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.OreProcessingRecipes;

public final class RecipeLoader {

    public static void registerRecipes() {
        CraftingTableRecipes.register();
        AssemblerRecipes.register();
        BenderRecipes.register();
        FurnaceRecipes.register();
        OreProcessingRecipes.register();
        MixerRecipes.register();
    }

    public static void registerLateRecipes() {
        ExtruderRecipes.register();
        OreProcessingRecipes.registerExternalRecipes();
        ModernNonCellRecipes.registerLateRecipes();
        FragmentWorldCollectorRecipes.scheduleRecipes();
    }
}
