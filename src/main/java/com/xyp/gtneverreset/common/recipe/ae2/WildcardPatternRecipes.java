package com.xyp.gtneverreset.common.recipe.ae2;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

public final class WildcardPatternRecipes {

    private WildcardPatternRecipes() {}

    public static void register() {
        GameRegistry.addRecipe(
            GTNRItemList.WildcardPattern.get(1),
            "B",
            'B',
            AEApi.instance()
                .definitions()
                .materials()
                .blankPattern()
                .maybeStack(1)
                .get());
    }
}
