package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CrystallineAlloyDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CrystallineAlloyIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CrystallineAlloyMolten;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.DiamondGem;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HotCrystallineAlloyIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrogenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrogenPeroxide;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Nitrobenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.OganessonGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.OxygenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismarineContaminatedHydrogenPeroxide;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismarineContaminatedNitrobenzeneSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismarinePrecipitate;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismarineRichNitrobenzeneSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismarineShard;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismarineSolution;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismaticAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismaticCrystal;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PrismaticGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PulsatingCrystal;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PulsatingCrystalDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PulsatingIronMolten;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Autoclave;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.BlastFurnace;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.ChemicalBath;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.ChemicalPlant;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.DissolutionTank;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FluidExtractor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FluidSolidifier;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.LargeChemicalReactor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.LaserEngraver;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Macerator;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.MixerNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFreezer;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFurnace;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RedAlgaeBiomass;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.StrontiumDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.StrontiumHydroxideDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.StrontiumIngot;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.StrontiumMolten;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.XenonGas;

import net.minecraftforge.fluids.Fluid;

import com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog.StepRef;

import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.TierEU;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBees;

/** Prismarine shard processing through prismatic acid. */
public final class BottledChaosPrismaticPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        // Shard stage: precipitation -> recovered solvent -> crystal -> gas -> acid.
        // Starting at precipitation exposes the solvent return before the missing fresh solvent is supplied.
        catalog.registerChain("prismatic_acid", 32, TierEU.RECIPE_UHV, PrismarineShard,
            new Object[] { PrismaticAcid }, new Object[] {},
            catalog.markRecipe(ChemicalPlant, new Object[] { StrontiumHydroxideDust, PrismarineRichNitrobenzeneSolution }, PrismarinePrecipitate, -1),
            catalog.markRecipe(ChemicalBath, PrismarineContaminatedNitrobenzeneSolution, PrismarineRichNitrobenzeneSolution, 1),
            catalog.markRecipe(LaserEngraver, PrismarinePrecipitate, PrismaticCrystal, -1),
            catalog.markRecipe(VacuumFurnace, PrismaticCrystal, PrismaticGas, -1),
            catalog.markRecipe(VacuumFreezer, PrismaticGas, PrismaticAcid, -1));

        // Independent shard supply; the honeycomb source replaces quartz and doubles the native yield.
        catalog.registerAlternativeChain("prismatic_acid", TierEU.RECIPE_UHV, RedAlgaeBiomass,
            new Object[] { PrismarineShard }, new Object[] { PrismarineShard },
            catalog.markRecipe(Autoclave,
                new Object[] { RedAlgaeBiomass, ChaosItemKey.create(Materials.CertusQuartz.getDust(1)) }, PrismarineShard, -1));
        if (Mods.Forestry.isModLoaded() && GTBees.combs != null) {
            catalog.registerAugmentedChain("prismatic_acid", "honeycomb", TierEU.RECIPE_UHV, RedAlgaeBiomass,
                new Object[] { PrismarineShard }, new Object[] { PrismarineShard },
                catalog.markRecipe(Autoclave,
                    new Object[] { RedAlgaeBiomass, ChaosItemKey.create(GTBees.combs.getStackForType(CombType.PRISMATIC, 1)) }, PrismarineShard, -1));
        }
        catalog.orderDisplayRecipes("prismatic", DiamondGem, RedAlgaeBiomass, PrismarineShard);

        // Input branches: supplement extraction, then reuse the contaminated peroxide in looped leaching.
        catalog.includeSynthesisStep(ChemicalBath, PrismarineRichNitrobenzeneSolution,
            new Object[] { PrismarineSolution, Nitrobenzene }, 1);
        catalog.includeSynthesisStep(DissolutionTank, PrismarineSolution,
            new Object[] { PrismarineShard, PrismarineContaminatedHydrogenPeroxide }, -1);
        // Reuse the leaching return, then purify the remainder before synthesizing fresh peroxide.
        catalog.registerReagentRecycling(ChemicalPlant,
            PrismarineContaminatedHydrogenPeroxide, HydrogenPeroxide, -1);
        // Finish the recovered precipitate through the same native crystal, gas and acid steps.
        catalog.includeDecompositionStep(LaserEngraver, PrismarinePrecipitate, PrismaticCrystal, -1);
        catalog.includeDecompositionStep(VacuumFurnace, PrismaticCrystal, PrismaticGas, -1);
        catalog.includeDecompositionStep(VacuumFreezer, PrismaticGas, PrismaticAcid, -1);

        // Alloy preparation: autoclave crystals -> powder -> alloy -> protected blast -> cooling -> melt.
        // Both protective gases share the acid tag, including its crystalline-alloy preparation.
        if (Mods.EnderIO.isModLoaded()) {
            for (Fluid gas : new Fluid[] { OganessonGas, XenonGas }) {
                var steps = new StepRef[] {
                    catalog.markRecipe(Autoclave, new Object[] { DiamondGem, PulsatingIronMolten }, PulsatingCrystal, -1),
                    catalog.markRecipe(Macerator, PulsatingCrystal, PulsatingCrystalDust, -1),
                    catalog.markRecipe(MixerNonCell, PulsatingCrystalDust, CrystallineAlloyDust, 2),
                    catalog.markRecipe(BlastFurnace, new Object[] { CrystallineAlloyDust, gas }, HotCrystallineAlloyIngot, 11),
                    catalog.markRecipe(VacuumFreezer, HotCrystallineAlloyIngot, CrystallineAlloyIngot, -1),
                    catalog.markRecipe(FluidExtractor, CrystallineAlloyIngot, CrystallineAlloyMolten, -1)
                };
                catalog.registerAlternativeChain("prismatic_acid", TierEU.RECIPE_UHV, DiamondGem,
                    new Object[] { CrystallineAlloyMolten }, new Object[] { CrystallineAlloyMolten }, steps).hideInputMarker();
            }
            catalog.includeSynthesisPath(PulsatingIronMolten);
        }

        // Strontium return: crystallization melt -> ingot -> dust -> hydroxide.
        catalog.includeDecompositionStep(FluidSolidifier, StrontiumMolten, StrontiumIngot, -1);
        catalog.includeDecompositionStep(Macerator, StrontiumIngot, StrontiumDust, -1);
        catalog.includeSynthesisStep(LargeChemicalReactor, StrontiumHydroxideDust,
            new Object[] { StrontiumDust, HydrogenGas, OxygenGas }, -1);
        // spotless:on
    }
}
