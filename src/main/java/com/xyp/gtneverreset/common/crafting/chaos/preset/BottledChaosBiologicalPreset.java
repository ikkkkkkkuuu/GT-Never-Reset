package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Agar;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BacterialSludge;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BioCells;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BioMediumRaw;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BioMediumSterilized;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CosmicNeutroniumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.DilutedSulfuricAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.DistilledWater;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EnergiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EnrichedBacterialSludge;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.FermentedBacterialSludge;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.GrowthMediumRaw;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.GrowthMediumSterilized;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrogenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrogenSulfideGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.InfinityCatalystDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IridiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MeatExtract;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Mutagen;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MysteriousCrystalDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MytrylDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.OsmiridiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.OsmiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PamSeaweed;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PhosphoricAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RawMeatDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Autoclave;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.BacterialVat;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Brewery;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.CentrifugeNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Distillery;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Extractor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Fermenter;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FluidExtractor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FluidHeater;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.LargeChemicalReactor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.MixerNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RedstoneDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RubyDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SeaweedBroth;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SodiumChlorideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.StemCells;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.SulfurDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TauCetiESeaweed;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.TauCetiESeaweedExtract;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.UnknownCrystal;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.UnknownNutrientAgar;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.UraniumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Water;

import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import gregtech.api.enums.TierEU;

/** Six biological production stages; stem-cell coproducts include their sludge-recovery steps. */
public final class BottledChaosBiologicalPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        // Stem-cell finishing entry; preparation entries execute before this primary entry.
        catalog.registerChain("stem_cells", 48, TierEU.RECIPE_UHV, GrowthMediumSterilized,
            new Object[] { StemCells }, new Object[] { StemCells, Mutagen },
            catalog.markRecipe(LargeChemicalReactor, new Object[] { UnknownCrystal, OsmiridiumDust },
                new Object[] { StemCells, BacterialSludge }, -1),
            catalog.markRecipe(Brewery, new Object[] { BacterialSludge, UraniumDust }, EnrichedBacterialSludge, -1),
            catalog.markRecipe(Fermenter, EnrichedBacterialSludge, FermentedBacterialSludge, -1),
            catalog.markRecipe(Distillery, FermentedBacterialSludge, Mutagen, 3)).setDisplayOutputs(StemCells);

        // Stage 1: raw materials -> mutagen growth medium -> sterile growth medium.
        catalog.registerChain("growth_medium_sterilized", 16, TierEU.RECIPE_UHV, RawMeatDust,
            new Object[] { GrowthMediumSterilized }, new Object[] { GrowthMediumSterilized },
            catalog.markRecipe(BacterialVat, new Object[] { RawMeatDust, Agar, Mutagen }, GrowthMediumRaw, -1),
            catalog.markRecipe(FluidHeater, GrowthMediumRaw, GrowthMediumSterilized, -1)).hideInputMarker();

        // Resolve the native centrifuge selector for the registered Tau Ceti E crop variant.
        int seaweedCircuit = 0;
        var variants = OreDictionary.getOres("cropTcetiESeaweed");
        for (int index = 0; index < variants.size(); index++) {
            if (TauCetiESeaweed.equals(ChaosItemKey.create(variants.get(index)))) {
                seaweedCircuit = index + 1;
                break;
            }
        }
        if (seaweedCircuit == 0) throw new IllegalStateException("missing Tau Ceti E seaweed crop variant");

        // Biological-cell finishing entry; preparation entries execute before this primary entry.
        catalog.registerChain("bio_cells", 96, TierEU.RECIPE_UHV, BioMediumSterilized,
            new Object[] { BioCells }, new Object[] { Mutagen },
            catalog.markRecipe(LargeChemicalReactor, new Object[] { StemCells, CosmicNeutroniumDust },
                new Object[] { BioCells, Mutagen }, -1)).setDisplayOutputs(BioCells);

        // Stage 3: stem cells and raw materials -> nutrient agar -> seaweed broth.
        catalog.registerChain("seaweed_broth", 16, TierEU.RECIPE_UHV, StemCells,
            new Object[] { SeaweedBroth }, new Object[] { SeaweedBroth },
            catalog.markRecipe(LargeChemicalReactor, new Object[] { Agar, StemCells, MeatExtract }, UnknownNutrientAgar, -1),
            catalog.markRecipe(BacterialVat, new Object[] { EnergiumDust, MytrylDust, PamSeaweed }, SeaweedBroth, -1));

        // Stage 4: seaweed broth -> seaweed -> seaweed extract.
        catalog.registerChain("tau_ceti_e_seaweed_extract", 48, TierEU.RECIPE_UHV, SeaweedBroth,
            new Object[] { TauCetiESeaweedExtract }, new Object[] { TauCetiESeaweedExtract },
            catalog.markRecipe(CentrifugeNonCell, SeaweedBroth, TauCetiESeaweed, seaweedCircuit),
            catalog.markRecipe(Extractor, TauCetiESeaweed, TauCetiESeaweedExtract, -1));

        // Stage 5: produce the biological-cell seed from stem cells, then cancel its medium and mutagen return.
        catalog.registerChain("bio_medium_sterilized", 32, TierEU.RECIPE_UHV, TauCetiESeaweedExtract,
            new Object[] { BioMediumSterilized }, new Object[] { BioMediumSterilized },
            catalog.markRecipe(LargeChemicalReactor, new Object[] { StemCells, CosmicNeutroniumDust }, BioCells, -1),
            catalog.markRecipe(BacterialVat, new Object[] { BioCells, TauCetiESeaweedExtract, InfinityCatalystDust }, BioMediumRaw, -1),
            catalog.markRecipe(FluidHeater, BioMediumRaw, BioMediumSterilized, -1));

        // Shared input branches: fresh raw growth medium uses externally supplied agar.
        catalog.includeSynthesisStep(BacterialVat, GrowthMediumRaw, new Object[] { Agar, Mutagen }, -1);
        catalog.includeSynthesisStep(Autoclave, UnknownCrystal,
            new Object[] { MysteriousCrystalDust, DistilledWater }, -1);
        catalog.includeSynthesisStep(MixerNonCell, OsmiridiumDust, new Object[] { OsmiumDust, IridiumDust }, 1);

        // Seaweed preparation inputs; intermediate stage outputs have no automatic input synthesis.
        catalog.includeSynthesisStep(FluidExtractor, MeatExtract, RawMeatDust, -1);
        catalog.includeSynthesisStep(MixerNonCell, EnergiumDust, new Object[] { RedstoneDust, RubyDust }, -1);
        catalog.includeSynthesisPath(SodiumChlorideDust, PhosphoricAcid, DistilledWater);
        // Avoid an input/output acid cycle; supply this dilute input through its native hydrogen-sulfide source.
        catalog.includeSynthesisStep(LargeChemicalReactor, DilutedSulfuricAcid, new Object[] { HydrogenSulfideGas, Water }, -1);
        catalog.includeSynthesisStep(LargeChemicalReactor, HydrogenSulfideGas, new Object[] { SulfurDust, HydrogenGas }, 1);

        catalog.setExistingEntriesKeyOutputs("stem_cells", new Object[] { StemCells, Mutagen });
        catalog.setExistingEntriesKeyOutputs("bio_cells", new Object[] { BioCells, Mutagen });
        // spotless:on
    }
}
