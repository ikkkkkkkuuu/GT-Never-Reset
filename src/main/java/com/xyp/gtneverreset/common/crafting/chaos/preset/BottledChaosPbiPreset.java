package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Benzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Chlorobenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CopperDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Diaminobenzidine;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Dichlorobenzidine;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Dimethylbenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.DiphenylIsophthalate;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Nitrochlorobenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PhthalicAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PolybenzimidazoleMolten;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PotassiumDichromateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.LargeChemicalReactor;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import gregtech.api.enums.TierEU;

public final class BottledChaosPbiPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        var diamine = catalog.markRecipeSequence(LargeChemicalReactor, Benzene, Diaminobenzidine,
            catalog.markRecipe(LargeChemicalReactor, Benzene, Chlorobenzene, 1),
            catalog.markRecipe(LargeChemicalReactor, Chlorobenzene, Nitrochlorobenzene, 1),
            catalog.markRecipe(LargeChemicalReactor, new Object[] { Nitrochlorobenzene, CopperDust }, Dichlorobenzidine, 9),
            catalog.markRecipe(LargeChemicalReactor, Dichlorobenzidine, Diaminobenzidine, -1));
        var pbi = catalog.registerChain("pbi", 4, TierEU.RECIPE_IV, Benzene,
            new Object[] { PolybenzimidazoleMolten },
            catalog.markRecipe(LargeChemicalReactor, Benzene, Dimethylbenzene, 1),
            catalog.markRecipe(LargeChemicalReactor, new Object[] { Dimethylbenzene, PotassiumDichromateDust }, PhthalicAcid, -1),
            catalog.markRecipe(LargeChemicalReactor, PhthalicAcid, DiphenylIsophthalate, -1),
            catalog.markRecipe(LargeChemicalReactor, new Object[] { DiphenylIsophthalate, Diaminobenzidine }, PolybenzimidazoleMolten, -1));
        pbi.includeSynthesisStep(Diaminobenzidine, diamine);
        // spotless:on
    }
}
