package com.xyp.gtneverreset.loader;

import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.gtnewhorizons.modularui.common.widget.ProgressBar;
import com.xyp.gtneverreset.client.nei.FragmentWorldCollectionFrontend;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectionBackend;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.maps.LargeNEIFrontend;

public final class GTNRRecipeMaps {

    // #tr gtnr.recipe.modern_mixer
    // # Small Mixer (Direct Fluids)
    // # zh_CN 小型搅拌机（直接流体）
    public static final RecipeMap<RecipeMapBackend> MODERN_MIXER_RECIPES = RecipeMapBuilder
        .of("gtnr.recipe.modern_mixer")
        .maxIO(6, 1, 2, 1)
        .progressBar(GTUITextures.PROGRESSBAR_MIXER, ProgressBar.Direction.CIRCULAR_CW)
        .progressBarMUI2(GTGuiTextures.PROGRESSBAR_MIXER, ProgressWidget.Direction.CIRCULAR_CW)
        .frontend(LargeNEIFrontend::new)
        .build();

    // #tr gtnr.recipe.modern_electrolyzer
    // # Small Electrolyzer (Direct Fluids)
    // # zh_CN 小型电解机（直接流体）
    public static final RecipeMap<RecipeMapBackend> MODERN_ELECTROLYZER_RECIPES = RecipeMapBuilder
        .of("gtnr.recipe.modern_electrolyzer")
        .maxIO(2, 6, 1, 6)
        .progressBar(GTUITextures.PROGRESSBAR_EXTRACT)
        .progressBarMUI2(GTGuiTextures.PROGRESSBAR_EXTRACT)
        .frontend(LargeNEIFrontend::new)
        .build();

    // #tr gtnr.recipe.modern_chemical_reactor
    // # Small Chemical Reactor (Direct Fluids)
    // # zh_CN 小型化学反应釜（直接流体）
    public static final RecipeMap<RecipeMapBackend> MODERN_CHEMICAL_RECIPES = RecipeMapBuilder
        .of("gtnr.recipe.modern_chemical_reactor")
        .maxIO(2, 2, 3, 2)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .progressBarMUI2(GTGuiTextures.PROGRESSBAR_ARROW_MULTIPLE)
        .frontend(LargeNEIFrontend::new)
        .build();

    public static final RecipeMap<FragmentWorldCollectionBackend> FRAGMENT_WORLD_COLLECTION_RECIPES = RecipeMapBuilder
        // #tr recipe.gtneverreset.fragment_world_collection
        // # Fragment World Collection
        // # zh_CN 碎片世界采集
        .of("recipe.gtneverreset.fragment_world_collection", FragmentWorldCollectionBackend::new)
        .maxIO(3, 12, 1, 1)
        .minInputs(1, 0)
        .progressBar(GTUITextures.PROGRESSBAR_MACERATE)
        .progressBarPos(62, 35)
        .logoPos(62, 80)
        .neiRecipeBackgroundSize(170, 108)
        .frontend(FragmentWorldCollectionFrontend::new)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(GTNRItemList.FragmentWorldCollector.get(1))
                .setHeight(160)
                .setMaxRecipesPerPage(1))
        .build();

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
