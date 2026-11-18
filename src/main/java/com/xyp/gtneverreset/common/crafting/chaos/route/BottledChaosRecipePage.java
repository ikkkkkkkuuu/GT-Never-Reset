package com.xyp.gtneverreset.common.crafting.chaos.route;

import java.util.function.Supplier;

import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.recipe.RecipeCategory;

/** Page identity, NEI routing and compact credential naming, without eager recipe-map initialization. */
public enum BottledChaosRecipePage {

    // spotless:off
    DEFAULT(() -> GTNRRecipeMaps.BottledChaosRecipes.getDefaultRecipeCategory()),
    WOOD(() -> GTNRRecipeMaps.BottledChaosWoodCategory),
    PETROCHEMICAL(() -> GTNRRecipeMaps.BottledChaosPetrochemicalCategory),
    TITANIUM(() -> GTNRRecipeMaps.BottledChaosTitaniumCategory),
    TUNGSTEN(() -> GTNRRecipeMaps.BottledChaosTungstenCategory),
    INDIUM(() -> GTNRRecipeMaps.BottledChaosIndiumCategory),
    PLATINUM(() -> GTNRRecipeMaps.BottledChaosPlatinumCategory, "gtnr.bottleinchaos.recipe.category.platinum.name"),
    LANTHANIDE(() -> GTNRRecipeMaps.BottledChaosLanthanideCategory, "gtnr.bottleinchaos.recipe.category.lanthanide.name"),
    NANO_EXTRACTION(() -> GTNRRecipeMaps.BottledChaosNanoExtractionCategory, true),
    NAQUADAH(() -> GTNRRecipeMaps.BottledChaosNaquadahCategory, "gtnr.bottleinchaos.recipe.category.naquadah.name"),
    NAQUADAH_FUEL(() -> GTNRRecipeMaps.BottledChaosNaquadahFuelCategory),
    FUSION(() -> GTNRRecipeMaps.BottledChaosFusionCategory, true),
    NETHERITE(() -> GTNRRecipeMaps.BottledChaosNetheriteCategory),
    ISA_MILL(() -> GTNRRecipeMaps.BottledChaosIsaMillCategory),
    PRISMATIC(() -> GTNRRecipeMaps.BottledChaosPrismaticCategory),
    PURIFIED_WATER(() -> GTNRRecipeMaps.BottledChaosPurifiedWaterCategory),
    KEVLAR(() -> GTNRRecipeMaps.BottledChaosKevlarCategory),
    RADOX(() -> GTNRRecipeMaps.BottledChaosRadoxCategory, "gtnr.bottleinchaos.recipe.category.radox.name"),
    BIOLOGICAL(() -> GTNRRecipeMaps.BottledChaosBiologicalCategory);
    // spotless:on

    private final Supplier<RecipeCategory> category;
    private final String summaryKey;
    private final boolean keepExpanded;

    BottledChaosRecipePage(Supplier<RecipeCategory> category) {
        this(category, null, false);
    }

    BottledChaosRecipePage(Supplier<RecipeCategory> category, String summaryKey) {
        this(category, summaryKey, false);
    }

    BottledChaosRecipePage(Supplier<RecipeCategory> category, boolean keepExpanded) {
        this(category, null, keepExpanded);
    }

    BottledChaosRecipePage(Supplier<RecipeCategory> category, String summaryKey, boolean keepExpanded) {
        this.category = category;
        this.summaryKey = summaryKey;
        this.keepExpanded = keepExpanded;
    }

    public RecipeCategory category() {
        return category.get();
    }

    public boolean keepExpanded() {
        return keepExpanded;
    }

    public String groupKey(String tag) {
        return this == DEFAULT ? tag : name();
    }

    public String summaryKey(String tagTranslationKey) {
        return summaryKey == null ? tagTranslationKey : summaryKey;
    }
}
