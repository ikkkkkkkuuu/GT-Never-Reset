package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CrackedRadox;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.DilutedXenoxene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.FermentedBacterialSludge;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HeavyRadox;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LightRadox;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Mutagen;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RadoxGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RadoxPolymerMolten;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RawRadox;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.CentrifugeNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.DistillationTower;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Distillery;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.IndustrialCokeOven;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.LargeChemicalReactor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.OilCracker;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SuperHeavyRadox;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SuperLightRadox;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Xenoxene;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import gregtech.api.enums.TierEU;

/** Xenoxene fractionation and four independent fraction routes through molten Radox polymer. */
public final class BottledChaosRadoxPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        // Close the native return once: superlight -> cracked -> gas + light -> returned superlight.
        var recoveredGas = catalog.markRecipeSequence(OilCracker, SuperLightRadox, RadoxGas,
            catalog.markRecipe(OilCracker, SuperLightRadox, CrackedRadox, 24),
            catalog.markRecipe(DistillationTower, CrackedRadox, RadoxGas, -1),
            catalog.markRecipe(OilCracker, LightRadox, SuperLightRadox, 24));

        var heavyCracking = catalog.markRecipe(OilCracker, HeavyRadox, LightRadox, 24);
        var lightCracking = catalog.markRecipe(OilCracker, LightRadox, SuperLightRadox, 24);
        var polymerization = catalog.markRecipe(LargeChemicalReactor, RadoxGas, RadoxPolymerMolten, 2);

        // Each fraction has an independent tag; display order follows fractionation before fraction processing.
        catalog.registerChain("super_light_radox", 32, TierEU.RECIPE_UEV, SuperLightRadox,
            new Object[] { RadoxPolymerMolten }, new Object[] {}, recoveredGas, polymerization);

        // Main line: xenoxene -> coke -> raw Radox distillation -> xenoxene recovery.
        // Cancel the recovered xenoxene against the input; retain every fraction and native coproduct.
        catalog.registerChain("xenoxene", 32, TierEU.RECIPE_UEV, Xenoxene,
            new Object[] { SuperHeavyRadox, HeavyRadox, LightRadox, SuperLightRadox },
            new Object[] { SuperHeavyRadox, HeavyRadox, LightRadox, SuperLightRadox, Mutagen },
            catalog.markRecipe(IndustrialCokeOven, Xenoxene, RawRadox, 24),
            catalog.markRecipe(DistillationTower, RawRadox, DilutedXenoxene, -1),
            catalog.markRecipe(DistillationTower, DilutedXenoxene, Xenoxene, -1),
            // Recover the fermented sludge coproduct within the fractionation entry.
            catalog.markRecipe(Distillery, FermentedBacterialSludge, Mutagen, 3))
            .setDisplayOutputs(SuperHeavyRadox, HeavyRadox, LightRadox, SuperLightRadox);

        // Superheavy fraction: centrifuge -> heavy -> light -> closed return -> polymer.
        catalog.registerChain("super_heavy_radox", 80, TierEU.RECIPE_UEV, SuperHeavyRadox,
            new Object[] { RadoxPolymerMolten }, new Object[] {},
            catalog.markRecipe(CentrifugeNonCell, SuperHeavyRadox, HeavyRadox, -1),
            heavyCracking, lightCracking, recoveredGas, polymerization);

        // Heavy fraction: heavy -> light -> closed return -> polymer.
        catalog.registerChain("heavy_radox", 64, TierEU.RECIPE_UEV, HeavyRadox,
            new Object[] { RadoxPolymerMolten }, new Object[] {},
            heavyCracking, lightCracking, recoveredGas, polymerization);

        // Light fraction: light -> closed return -> polymer.
        catalog.registerChain("light_radox", 48, TierEU.RECIPE_UEV, LightRadox,
            new Object[] { RadoxPolymerMolten }, new Object[] {}, lightCracking, recoveredGas, polymerization);

        catalog.setExistingEntriesKeyOutputs("xenoxene",
            new Object[] { SuperHeavyRadox, HeavyRadox, LightRadox, SuperLightRadox, Mutagen });
        catalog.orderDisplayRecipes("radox", Xenoxene, SuperHeavyRadox, HeavyRadox, LightRadox, SuperLightRadox);
        // spotless:on
    }
}
