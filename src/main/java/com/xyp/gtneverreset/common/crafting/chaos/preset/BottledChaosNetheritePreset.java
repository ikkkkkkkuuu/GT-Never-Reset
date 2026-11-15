package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AluminaMillingBall;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AncientDebris;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.BrittleNetheriteScrap;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Grade2PurifiedWater;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HeavyHellishMud;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HellishMetalBlock;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HellishMetalMolten;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HotNetheriteScrap;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IntenselyBondedNetheriteNanoparticles;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MilledNetherrack;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NefariousGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NefariousOil;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NetherAir;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NetherSemiFluid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NetheriteNanoparticles;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NetheriteScrap;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Netherrack;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NetherrackFlotationFroth;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NitricAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PoorNetherWaste;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.BlastFurnace;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.ChemicalBath;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.DistillationTower;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.ElectrolyzerNonCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FlotationCell;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FluidExtractor;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.FusionCrafting;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Macerator;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.Milling;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.OilCracker;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFreezer;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.VacuumFurnace;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RhodiumBlock;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RhodiumDust;

import com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import gregtech.api.enums.Mods;
import gregtech.api.enums.TierEU;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBees;

/** Netherite trunks and nether-air reagent branches, resolved from live source recipes. */
public final class BottledChaosNetheritePreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        if (!Mods.EtFuturumRequiem.isModLoaded()) return;
        if (AncientDebris == null || NetheriteScrap == null)
            throw new IllegalStateException("missing Et Futurum netherite process items");

        // spotless:off
        // Main trunk: UHV chemical bath -> brittle scrap -> ordinary nanoparticles.
        // Nether air remains a consumed execution root after the returned hot scraps cancel.
        catalog.registerChain("ancient_debris", 12, TierEU.RECIPE_UV, NetherAir,
            new Object[] { NetheriteNanoparticles, NitricAcid },
            new Object[] {},
            catalog.markRecipe(ChemicalBath, new Object[] { HotNetheriteScrap, HeavyHellishMud, PoorNetherWaste }, BrittleNetheriteScrap, -1),
            catalog.markRecipe(Macerator, BrittleNetheriteScrap, NetheriteNanoparticles, -1))
            .setDisplayOutputs(NetheriteNanoparticles);

        // Supply the bonding metal through either native process; one rhodium block represents nine dusts.
        catalog.registerAlternativeChain("ancient_debris", TierEU.RECIPE_UV, RhodiumDust,
            new Object[] { HellishMetalMolten },
            catalog.markRecipe(FusionCrafting, RhodiumDust, HellishMetalMolten, -1));
        catalog.registerAlternativeChain("ancient_debris", TierEU.RECIPE_UV, RhodiumDust,
            new Object[] { HellishMetalMolten },
            catalog.markRecipeWithSubstitutedInput(ElectrolyzerNonCell, RhodiumBlock, RhodiumDust, 9, HellishMetalBlock, -1),
            catalog.markRecipe(FluidExtractor, HellishMetalBlock, HellishMetalMolten, -1));

        // Keep the two native bonding recipes as independently searchable, chainable finishing stages.
        catalog.registerAlternativeChain("ancient_debris", TierEU.RECIPE_UV, NetheriteNanoparticles,
            new Object[] { IntenselyBondedNetheriteNanoparticles }, new Object[] {},
            catalog.markRecipe(BlastFurnace, NetheriteNanoparticles, IntenselyBondedNetheriteNanoparticles, 1));
        if (Mods.Forestry.isModLoaded() && GTBees.combs != null) {
            catalog.registerAugmentedChain("ancient_debris", "honeycomb", TierEU.RECIPE_UV, NetheriteNanoparticles,
                new Object[] { IntenselyBondedNetheriteNanoparticles }, new Object[] {},
                catalog.markRecipe(BlastFurnace,
                    new Object[] { NetheriteNanoparticles, ChaosItemKey.create(GTBees.combs.getStackForType(CombType.NETHERITE, 1)) },
                    IntenselyBondedNetheriteNanoparticles, 2));
        }

        // 1. Reheat returned scraps and cancel them against the UHV bath's hot-scrap input.
        catalog.includeDecompositionStep(ChemicalBath, NetheriteScrap, HotNetheriteScrap, -1);

        // 2. Waste branch: poor waste -> flotation froth -> milled netherrack -> netherrack.
        // Select the alumina-ball milling recipe explicitly.
        catalog.includeSynthesisStep(VacuumFurnace, PoorNetherWaste, NetherrackFlotationFroth, -1);
        catalog.includeSynthesisStep(FlotationCell, NetherrackFlotationFroth, new Object[] { MilledNetherrack, NefariousOil }, -1);
        catalog.includeSynthesisStep(Milling, MilledNetherrack, new Object[] { Netherrack, AluminaMillingBall }, 10);

        // 3. Oil branch: grade-2 purified water + nefarious gas -> oil for flotation.
        catalog.includeSynthesisStep(OilCracker, NefariousOil, new Object[] { Grade2PurifiedWater, NefariousGas }, 1);

        // 4. Air branch: vacuum freezing -> distillation. Reuse the tower's joint gas/mud outputs first.
        // Resolve gas before mud so its coproduct can satisfy the waste branch and the UHV trunk.
        catalog.includeSynthesisStep(DistillationTower, NefariousGas, NetherSemiFluid, -1);
        catalog.includeSynthesisStep(DistillationTower, HeavyHellishMud, NetherSemiFluid, -1);
        catalog.includeSynthesisStep(VacuumFreezer, NetherSemiFluid, NetherAir, -1);
        // spotless:on
    }
}
