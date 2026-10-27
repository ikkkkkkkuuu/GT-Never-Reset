package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.maps.LargeNEIFrontend;

public final class GTNRRecipeMaps {

    public static final RecipeMap<RecipeMapBackend> ORE_PROCESSING_RECIPES = RecipeMapBuilder
        // #tr recipe.gtneverreset.ore_processing
        // # Ore Processing
        // # zh_CN 矿石处理
        .of("recipe.gtneverreset.ore_processing")
        .maxIO(1, 18, 0, 0)
        .minInputs(1, 0)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .frontend(LargeNEIFrontend::new)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(GTNRItemList.LargeOreProcessor.get(1))
                .setHeight(160)
                .setMaxRecipesPerPage(1))
        .build();

    private GTNRRecipeMaps() {}
}
