package com.xyp.gtneverreset.common.recipe.ae2;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

public final class WirelessFrequencyCardRecipes {

    private WirelessFrequencyCardRecipes() {}

    public static void register() {
        var definitions = AEApi.instance()
            .definitions();
        GameRegistry.addShapelessRecipe(
            GTNRItemList.WirelessFrequencyCard.get(1),
            definitions.materials()
                .advCard()
                .maybeStack(1)
                .get(),
            definitions.items()
                .memoryCard()
                .maybeStack(1)
                .get(),
            definitions.materials()
                .wireless()
                .maybeStack(1)
                .get(),
            definitions.materials()
                .engProcessor()
                .maybeStack(1)
                .get());
    }
}
