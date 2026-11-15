package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AdamantiumOxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BariumSulfateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.ConcentratedEnrichedNaquadahSludgeDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.DilutedSulfuricAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EnrichedNaquadahDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EnrichedNaquadahIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EnrichedNaquadahOxideMixtureDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EnrichedNaquadahRichSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EnrichedNaquadahSulfateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.FluorineRichWasteLiquid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.FluorsparDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.GalliumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.GalliumHydroxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HotEnrichedNaquadahIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HotNaquadriaIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HotTitaniumIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrochloricAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IndiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IndiumPhosphateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LowQualityNaquadahEmulsion;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LowQualityNaquadahSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LowQualityNaquadriaPhosphateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LowQualityNaquadriaSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LowQualityNaquadriaSulfateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MagnesiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MagnesiumSulfateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadahAdamantiumSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadahDioxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadahDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadahOxideMixtureDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadahRichSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadahWasteLiquid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadriaDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadriaIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadriaOxideMixtureDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadriaRichSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaquadriaSulfateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Phenol;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.QuicklimeDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Autoclave;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.BlastFurnace;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.CentrifugeNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.DistillationTower;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.ElectrolyzerNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.LargeChemicalReactor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Macerator;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.NeutronActivator;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFreezer;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SaltWater;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SodiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SodiumHydroxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SodiumOxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TitaniumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TitaniumIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TitaniumTrifluorideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TricalciumPhosphateDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TriniumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TriniumSulfateDust;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import gregtech.api.enums.TierEU;

/** Three naquadah processing tags, using the existing EU-powered neutron activator recipe pool. */
public final class BottledChaosNaquadahPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        // 1. Prepare refining paths. Cross-line byproducts require their own processing tags.
        var enrichedSludgeRefining = catalog.markRecipeSequence(NeutronActivator,
            ConcentratedEnrichedNaquadahSludgeDust, EnrichedNaquadahDust,
            catalog.markRecipe(NeutronActivator, ConcentratedEnrichedNaquadahSludgeDust, EnrichedNaquadahSulfateDust, -1),
            catalog.markRecipe(BlastFurnace, EnrichedNaquadahSulfateDust, HotEnrichedNaquadahIngot, -1),
            catalog.markRecipe(VacuumFreezer, HotEnrichedNaquadahIngot, EnrichedNaquadahIngot, -1),
            catalog.markRecipe(Macerator, EnrichedNaquadahIngot, EnrichedNaquadahDust, -1));
        var lowQualitySulfateRecovery = catalog.markRecipeSequence(LargeChemicalReactor,
            LowQualityNaquadriaSulfateDust, NaquadriaDust,
            catalog.markRecipe(LargeChemicalReactor, LowQualityNaquadriaSulfateDust, LowQualityNaquadriaSolution, -1),
            catalog.markRecipe(DistillationTower, LowQualityNaquadriaSolution, NaquadriaRichSolution, -1),
            catalog.markRecipe(NeutronActivator, NaquadriaRichSolution, NaquadriaSulfateDust, -1),
            catalog.markRecipe(BlastFurnace, NaquadriaSulfateDust, HotNaquadriaIngot, -1),
            catalog.markRecipe(VacuumFreezer, HotNaquadriaIngot, NaquadriaIngot, -1),
            catalog.markRecipe(Macerator, NaquadriaIngot, NaquadriaDust, -1));

        // 2. Ordinary naquadah: stop at enriched sludge; its downstream products belong to other tags.
        catalog.registerChain("naquadah", 16, TierEU.RECIPE_UHV, NaquadahOxideMixtureDust,
            new Object[] { NaquadahDust, ConcentratedEnrichedNaquadahSludgeDust },
            new Object[] { ConcentratedEnrichedNaquadahSludgeDust },
            catalog.markRecipe(BlastFurnace, NaquadahOxideMixtureDust, LowQualityNaquadahEmulsion, 1),
            catalog.markRecipe(CentrifugeNonCell, LowQualityNaquadahEmulsion, LowQualityNaquadahSolution, 1),
            catalog.markRecipe(LargeChemicalReactor, LowQualityNaquadahSolution, NaquadahAdamantiumSolution, -1),
            catalog.markRecipe(NeutronActivator, NaquadahAdamantiumSolution, NaquadahRichSolution, -1),
            catalog.markRecipe(Autoclave, NaquadahRichSolution, NaquadahDioxideDust, -1),
            catalog.markRecipe(LargeChemicalReactor, NaquadahDioxideDust, NaquadahDust, -1));

        // 3. Enriched naquadah: refine its sludge, but leave low-quality naquadria sulfate for its own tag.
        catalog.registerChain("enriched_naquadah", 48, TierEU.RECIPE_UHV, EnrichedNaquadahOxideMixtureDust,
            new Object[] { EnrichedNaquadahDust, NaquadahOxideMixtureDust, LowQualityNaquadriaSulfateDust },
            new Object[] { NaquadahOxideMixtureDust, LowQualityNaquadriaSulfateDust },
            catalog.markRecipe(LargeChemicalReactor, EnrichedNaquadahOxideMixtureDust, EnrichedNaquadahRichSolution, -1),
            catalog.markRecipe(Autoclave, EnrichedNaquadahRichSolution, ConcentratedEnrichedNaquadahSludgeDust, -1),
            enrichedSludgeRefining);

        // 4. Naquadria loop: activate first, recover low-quality sulfate and cancel the returned rich solution.
        catalog.registerChain("naquadria", 64, TierEU.RECIPE_UHV, NaquadriaOxideMixtureDust,
            new Object[] { NaquadriaDust, EnrichedNaquadahOxideMixtureDust },
            new Object[] { EnrichedNaquadahOxideMixtureDust },
            catalog.markRecipe(NeutronActivator, NaquadriaRichSolution, NaquadriaSulfateDust, -1),
            catalog.markRecipe(LargeChemicalReactor, LowQualityNaquadriaSulfateDust, LowQualityNaquadriaSolution, -1),
            catalog.markRecipe(DistillationTower, LowQualityNaquadriaSolution, NaquadriaRichSolution, -1),
            catalog.markRecipe(BlastFurnace, NaquadriaSulfateDust, HotNaquadriaIngot, -1),
            catalog.markRecipe(VacuumFreezer, HotNaquadriaIngot, NaquadriaIngot, -1),
            catalog.markRecipe(Macerator, NaquadriaIngot, NaquadriaDust, -1));

        // 5. Independent cross-line entries, authorized by the tag of the metal they produce.
        catalog.registerAlternativeChain("enriched_naquadah", TierEU.RECIPE_UHV, ConcentratedEnrichedNaquadahSludgeDust,
            new Object[] { EnrichedNaquadahDust, LowQualityNaquadriaSulfateDust },
            new Object[] { LowQualityNaquadriaSulfateDust }, enrichedSludgeRefining);
        catalog.registerAlternativeChain("naquadria", TierEU.RECIPE_UHV, LowQualityNaquadriaSulfateDust,
            new Object[] { NaquadriaDust, EnrichedNaquadahOxideMixtureDust },
            new Object[] { EnrichedNaquadahOxideMixtureDust }, lowQualitySulfateRecovery);

        // 6. Input branches. Returned rich solution is canceled before supplementing the phosphate demand.
        catalog.includeSynthesisStep(LargeChemicalReactor, NaquadriaRichSolution, LowQualityNaquadriaPhosphateDust, -1);
        catalog.includeSynthesisStep(CentrifugeNonCell, LowQualityNaquadriaPhosphateDust, NaquadriaOxideMixtureDust, -1);
        // Gallium reduction returns NaOH; cancel it against all demand before processing the remainder.
        catalog.registerDeferredReagents(SodiumHydroxideDust);
        catalog.includeSynthesisPath(QuicklimeDust, SodiumHydroxideDust);
        // P507 is fully returned by precipitation/distillation and cancels without an input-synthesis branch.

        // 7. Metallic byproducts, followed by fluoride waste treatment.
        catalog.includeDecompositionStep(BlastFurnace, TitaniumTrifluorideDust, HotTitaniumIngot, 1);
        catalog.includeDecompositionStep(VacuumFreezer, HotTitaniumIngot, TitaniumIngot, -1);
        catalog.includeDecompositionStep(Macerator, TitaniumIngot, TitaniumDust, -1);
        catalog.includeDecompositionStep(LargeChemicalReactor, GalliumHydroxideDust, GalliumDust, -1);
        catalog.includeDecompositionStep(LargeChemicalReactor, IndiumPhosphateDust, IndiumDust, 1);
        catalog.includeDecompositionStep(LargeChemicalReactor, TriniumSulfateDust, TriniumDust, -1);
        catalog.includeDecompositionStep(ElectrolyzerNonCell, MagnesiumSulfateDust, MagnesiumDust, -1);
        catalog.includeDecompositionStep(ElectrolyzerNonCell, SodiumOxideDust, SodiumDust, -1);
        catalog.includeDecompositionStep(LargeChemicalReactor, FluorineRichWasteLiquid, NaquadahWasteLiquid, -1);
        catalog.includeDecompositionStep(DistillationTower, NaquadahWasteLiquid, new Object[] { SaltWater, HydrochloricAcid }, -1);

        // 8. Recover sulfuric acid and reduce mineral/organic byproducts to their elemental forms.
        catalog.includeDecompositionPath(DilutedSulfuricAcid, BariumSulfateDust,
            TricalciumPhosphateDust, QuicklimeDust, Phenol, SodiumHydroxideDust, AdamantiumOxideDust, FluorsparDust);
        // spotless:on
    }
}
