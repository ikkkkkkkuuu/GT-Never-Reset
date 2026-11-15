package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AluminiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AluminiumHydroxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AluminiumOxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BauxiteDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BauxiteSlagDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BauxiteSlurry;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CalciteDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CarbonDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CrushedBauxite;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CrushedIlmenite;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HeatedBauxiteSlurry;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HotTitaniumIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IlmeniteDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IlmeniteSlagDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IronDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IronIISulfateSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.QuicklimeDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.BlastFurnace;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.CentrifugeNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.ChemicalBath;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.ElectrolyzerNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FluidHeater;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.LargeChemicalReactor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.MixerNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFreezer;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RutileDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SiliconDioxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SiliconDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SluiceJuice;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SodiumCarbonateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SodiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SodiumHydroxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TitaniumIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TitaniumTetrachloride;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import gregtech.api.enums.TierEU;

/** LuV mineral processing; alternative sources and consecutive stages share one credential tag. */
public final class BottledChaosTitaniumPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        // 1. Final stage: chlorinate rutile, reduce its chloride, then cool to titanium ingots.
        catalog.registerChain("titanium", 1, TierEU.RECIPE_LuV, RutileDust,
            new Object[] { TitaniumIngot }, new Object[] {},
            catalog.markRecipe(LargeChemicalReactor, RutileDust, TitaniumTetrachloride, -1),
            catalog.markRecipe(BlastFurnace, TitaniumTetrachloride, HotTitaniumIngot, 12),
            catalog.markRecipe(VacuumFreezer, HotTitaniumIngot, TitaniumIngot, -1));

        // 2. Ilmenite preparation: use the native crushed-ore bath, retaining its 60% slag yield.
        // Replace only the ore input with full mineral dust before balancing the cached chain.
        var ilmenite = catalog.registerAlternativeChain("titanium", TierEU.RECIPE_LuV, IlmeniteDust,
            new Object[] { RutileDust }, new Object[] { RutileDust },
            catalog.markRecipeWithSubstitutedInput(ChemicalBath, CrushedIlmenite, IlmeniteDust, RutileDust, -1));
        ilmenite.includeDecompositionStep(CentrifugeNonCell, IlmeniteSlagDust, IronDust, -1);
        ilmenite.includeDecompositionStep(ElectrolyzerNonCell, IronIISulfateSolution, IronDust, -1);

        // 3. Bauxite preparation: slurry mixing, heating, precipitation and slag separation.
        var bauxite = catalog.registerAlternativeChain("titanium", TierEU.RECIPE_LuV, BauxiteDust,
            new Object[] { RutileDust }, new Object[] { RutileDust },
            catalog.markRecipeWithSubstitutedInput(MixerNonCell, CrushedBauxite, BauxiteDust, BauxiteSlurry, 8),
            catalog.markRecipe(FluidHeater, BauxiteSlurry, HeatedBauxiteSlurry, -1),
            catalog.markRecipe(LargeChemicalReactor, HeatedBauxiteSlurry, BauxiteSlagDust, -1),
            catalog.markRecipe(CentrifugeNonCell, BauxiteSlagDust, RutileDust, -1));
        catalog.orderDisplayRecipes("titanium", IlmeniteDust, BauxiteDust, RutileDust);

        // 4. Bauxite coproducts: recover sluice metals and decompose the remaining mineral compounds.
        bauxite.includeDecompositionStep(CentrifugeNonCell, SluiceJuice, IronDust, -1);
        bauxite.includeDecompositionStep(ElectrolyzerNonCell, AluminiumOxideDust, AluminiumDust, -1);
        bauxite.includeDecompositionStep(ElectrolyzerNonCell, SodiumCarbonateDust, SodiumDust, -1);
        bauxite.includeDecompositionStep(ElectrolyzerNonCell, CalciteDust, CarbonDust, -1);
        bauxite.includeDecompositionStep(ElectrolyzerNonCell, SiliconDioxideDust, SiliconDust, -1);
        catalog.includeDecompositionPath(QuicklimeDust);
        catalog.includeSynthesisPath(AluminiumHydroxideDust, SodiumHydroxideDust, QuicklimeDust);

        // NEI uses each stage's declared root and terminal products as its only material query markers.
        // spotless:on
    }
}
