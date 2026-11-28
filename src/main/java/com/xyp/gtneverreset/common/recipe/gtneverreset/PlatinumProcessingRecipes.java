package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static bartworks.system.material.WerkstoffLoader.AcidicIridiumSolution;
import static bartworks.system.material.WerkstoffLoader.AcidicOsmiumSolution;
import static bartworks.system.material.WerkstoffLoader.AmmoniumChloride;
import static bartworks.system.material.WerkstoffLoader.AquaRegia;
import static bartworks.system.material.WerkstoffLoader.CalciumChloride;
import static bartworks.system.material.WerkstoffLoader.CrudeRhMetall;
import static bartworks.system.material.WerkstoffLoader.HotRutheniumTetroxideSollution;
import static bartworks.system.material.WerkstoffLoader.IrLeachResidue;
import static bartworks.system.material.WerkstoffLoader.IrOsLeachResidue;
import static bartworks.system.material.WerkstoffLoader.IridiumChloride;
import static bartworks.system.material.WerkstoffLoader.IridiumDioxide;
import static bartworks.system.material.WerkstoffLoader.LeachResidue;
import static bartworks.system.material.WerkstoffLoader.OsmiumSolution;
import static bartworks.system.material.WerkstoffLoader.PDAmmonia;
import static bartworks.system.material.WerkstoffLoader.PDMetallicPowder;
import static bartworks.system.material.WerkstoffLoader.PDRawPowder;
import static bartworks.system.material.WerkstoffLoader.PDSalt;
import static bartworks.system.material.WerkstoffLoader.PTConcentrate;
import static bartworks.system.material.WerkstoffLoader.PTMetallicPowder;
import static bartworks.system.material.WerkstoffLoader.PTRawPowder;
import static bartworks.system.material.WerkstoffLoader.PTResidue;
import static bartworks.system.material.WerkstoffLoader.PTSaltCrude;
import static bartworks.system.material.WerkstoffLoader.PTSaltRefined;
import static bartworks.system.material.WerkstoffLoader.PotassiumDisulfate;
import static bartworks.system.material.WerkstoffLoader.RHFilterCakeSolution;
import static bartworks.system.material.WerkstoffLoader.RHNitrate;
import static bartworks.system.material.WerkstoffLoader.RHSalt;
import static bartworks.system.material.WerkstoffLoader.RHSaltSolution;
import static bartworks.system.material.WerkstoffLoader.RHSulfate;
import static bartworks.system.material.WerkstoffLoader.RHSulfateSolution;
import static bartworks.system.material.WerkstoffLoader.ReRh;
import static bartworks.system.material.WerkstoffLoader.RhFilterCake;
import static bartworks.system.material.WerkstoffLoader.Rhodium;
import static bartworks.system.material.WerkstoffLoader.Ruthenium;
import static bartworks.system.material.WerkstoffLoader.RutheniumTetroxide;
import static bartworks.system.material.WerkstoffLoader.RutheniumTetroxideSollution;
import static bartworks.system.material.WerkstoffLoader.SodiumNitrate;
import static bartworks.system.material.WerkstoffLoader.SodiumRuthenate;
import static bartworks.system.material.WerkstoffLoader.ZincSulfate;
import static bartworks.system.material.WerkstoffLoader.Zirconium;
import static gregtech.api.enums.OrePrefixes.block;
import static gregtech.api.enums.OrePrefixes.crushed;
import static gregtech.api.enums.OrePrefixes.crushedPurified;
import static gregtech.api.enums.OrePrefixes.dust;
import static gregtech.api.enums.OrePrefixes.ingot;
import static gregtech.api.enums.OrePrefixes.ingotHot;
import static gregtech.api.enums.OrePrefixes.milled;
import static gregtech.api.recipe.RecipeMaps.blastFurnaceRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalBathRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalDehydratorRecipes;
import static gregtech.api.recipe.RecipeMaps.crackingRecipes;
import static gregtech.api.recipe.RecipeMaps.distillationTowerRecipes;
import static gregtech.api.recipe.RecipeMaps.electroMagneticSeparatorRecipes;
import static gregtech.api.recipe.RecipeMaps.electrolyzerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.electrolyzerRecipes;
import static gregtech.api.recipe.RecipeMaps.flotationCellRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidExtractionRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidHeaterRecipes;
import static gregtech.api.recipe.RecipeMaps.fluidSolidifierRecipes;
import static gregtech.api.recipe.RecipeMaps.hammerRecipes;
import static gregtech.api.recipe.RecipeMaps.maceratorRecipes;
import static gregtech.api.recipe.RecipeMaps.millingRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.mixerRecipes;
import static gregtech.api.recipe.RecipeMaps.multiblockChemicalReactorRecipes;
import static gregtech.api.recipe.RecipeMaps.sifterRecipes;
import static gregtech.api.recipe.RecipeMaps.thermalCentrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.vacuumFreezerRecipes;
import static gregtech.api.recipe.RecipeMaps.vacuumFurnaceRecipes;
import static gregtech.api.util.GTModHandler.getModItem;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
import static gtPlusPlus.core.material.MaterialMisc.RARE_EARTH_HIGH;
import static gtPlusPlus.core.material.MaterialMisc.RARE_EARTH_LOW;
import static gtPlusPlus.core.material.MaterialMisc.RARE_EARTH_MID;
import static gtnhlanth.api.recipe.LanthanidesRecipeMaps.digesterRecipes;
import static gtnhlanth.api.recipe.LanthanidesRecipeMaps.dissolutionTankRecipes;
import static gtnhlanth.common.register.WerkstoffMaterialPool.AcidicMonazitePowder;
import static gtnhlanth.common.register.WerkstoffMaterialPool.CeriumChloride;
import static gtnhlanth.common.register.WerkstoffMaterialPool.CeriumDioxide;
import static gtnhlanth.common.register.WerkstoffMaterialPool.CeriumIIIOxide;
import static gtnhlanth.common.register.WerkstoffMaterialPool.CeriumOxalate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.CooledMonaziteRareEarthConcentrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.DilutedMonaziteSulfate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.DilutedRareEarthMonaziteMud;
import static gtnhlanth.common.register.WerkstoffMaterialPool.DriedMonaziteRareEarthConcentrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.EuropiumIIIOxide;
import static gtnhlanth.common.register.WerkstoffMaterialPool.EuropiumOxide;
import static gtnhlanth.common.register.WerkstoffMaterialPool.Hafnia;
import static gtnhlanth.common.register.WerkstoffMaterialPool.HafniaZirconiaBlend;
import static gtnhlanth.common.register.WerkstoffMaterialPool.Hafnium;
import static gtnhlanth.common.register.WerkstoffMaterialPool.HafniumIodide;
import static gtnhlanth.common.register.WerkstoffMaterialPool.HafniumTetrachloride;
import static gtnhlanth.common.register.WerkstoffMaterialPool.HafniumTetrachlorideSolution;
import static gtnhlanth.common.register.WerkstoffMaterialPool.LowPurityHafnium;
import static gtnhlanth.common.register.WerkstoffMaterialPool.MonaziteHeterogenousHalogenicRareEarthMixture;
import static gtnhlanth.common.register.WerkstoffMaterialPool.MonaziteRareEarthFiltrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.MonaziteRareEarthHydroxideConcentrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.MonaziteRarerEarthSediment;
import static gtnhlanth.common.register.WerkstoffMaterialPool.MonaziteSulfate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.MuddyRareEarthMonaziteSolution;
import static gtnhlanth.common.register.WerkstoffMaterialPool.NeutralizedMonaziteRareEarthFiltrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.NeutralizedUraniumFiltrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.NitratedRareEarthMonaziteConcentrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.NitricLeachedMonaziteMixture;
import static gtnhlanth.common.register.WerkstoffMaterialPool.NitricMonaziteLeachedConcentrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.SamaricResidue;
import static gtnhlanth.common.register.WerkstoffMaterialPool.SaturatedMonaziteRareEarthMixture;
import static gtnhlanth.common.register.WerkstoffMaterialPool.ThoriumPhosphateCake;
import static gtnhlanth.common.register.WerkstoffMaterialPool.ThoriumPhosphateConcentrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.UraniumFiltrate;
import static gtnhlanth.common.register.WerkstoffMaterialPool.Zirconia;
import static gtnhlanth.common.register.WerkstoffMaterialPool.ZirconiumTetrachloride;
import static gtnhlanth.common.register.WerkstoffMaterialPool.ZirconiumTetrachlorideSolution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import bartworks.system.material.Werkstoff;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.material.MaterialsElements;

public final class PlatinumProcessingRecipes {

    public static final RecipeMetadataKey<Integer> STATION = SimpleRecipeMetadataKey
        .create(Integer.class, "gtnr_platinum_processing_station");
    public static final RecipeMetadataKey<Integer> RING_REQUIREMENT = SimpleRecipeMetadataKey
        .create(Integer.class, "gtnr_platinum_processing_ring_requirement");
    public static final RecipeMetadataKey<Integer> DIGESTER_HEAT = SimpleRecipeMetadataKey
        .create(Integer.class, "gtnr_platinum_processing_digester_heat");
    public static final int STATION_COUNT = 4;

    private static final int PLATINUM_CONCENTRATE_BATCHES = 20;
    private static final int RARE_EARTH_BATH_BATCHES = 3;
    private static final int MONAZITE_DIGESTION_BATCHES = 10;
    private static final int NETHERITE_BATH_BATCHES = 3;

    private PlatinumProcessingRecipes() {}

    public static void register() {
        registerFamily(1, PlatinumProcessingRecipes::platinum);
        registerFamily(2, PlatinumProcessingRecipes::rareEarth);
        registerFamily(2, PlatinumProcessingRecipes::indium);
        registerFamily(3, PlatinumProcessingRecipes::monazite);
        registerFamily(3, PlatinumProcessingRecipes::polybenzimidazole);
        if (Mods.EtFuturumRequiem.isModLoaded()) registerFamily(4, PlatinumProcessingRecipes::netherite);
    }

    private static void registerFamily(int ring, Supplier<GTRecipeBuilder> recipe) {
        try {
            recipe.get()
                .metadata(STATION, ring - 1)
                .metadata(RING_REQUIREMENT, ring)
                .addTo(GTNRRecipeMaps.PLATINUM_PROCESSING);
        } catch (IllegalStateException | ArithmeticException exception) {
            GTNeverReset.LOG.warn("cannot register platinum gate recipe for ring {}: {}", ring, exception.getMessage());
        }
    }

    private static GTRecipeBuilder platinum() {
        Route route = new Route();
        route.add(
            multiblockChemicalReactorRecipes,
            9,
            PTMetallicPowder.get(dust),
            PTResidue.get(dust),
            2L * PLATINUM_CONCENTRATE_BATCHES);
        route.consume(multiblockChemicalReactorRecipes, 3, fluid(PTConcentrate), PTSaltCrude.get(dust));
        route.consume(sifterRecipes, -1, PTSaltCrude.get(dust), PTSaltRefined.get(dust));
        route.consume(blastFurnaceRecipes, 1, PTSaltRefined.get(dust), PTMetallicPowder.get(dust));
        route.consume(multiblockChemicalReactorRecipes, -1, PTRawPowder.get(dust), Materials.Platinum.getDust(1));

        // Two palladium digestion batches per platinum concentrate batch consume the enriched ammonia.
        // Native salt recovery reduces the required fresh feed without including a return loop in the recipe.
        route.add(
            multiblockChemicalReactorRecipes,
            1,
            PDMetallicPowder.get(dust),
            fluid(PDAmmonia),
            PLATINUM_CONCENTRATE_BATCHES * 72L / 5);
        route.consume(multiblockChemicalReactorRecipes, 9, fluid(PDAmmonia), PDRawPowder.get(dust));
        route.consume(sifterRecipes, -1, PDSalt.get(dust), PDMetallicPowder.get(dust));
        route.consume(multiblockChemicalReactorRecipes, -1, PDRawPowder.get(dust), Materials.Palladium.getDust(1));

        route.consume(blastFurnaceRecipes, 11, PTResidue.get(dust), LeachResidue.get(dust));
        route.consume(multiblockChemicalReactorRecipes, 3, fluid(RHSulfate), fluid(RHSulfateSolution));
        route.consume(multiblockChemicalReactorRecipes, -1, fluid(RHSulfateSolution), CrudeRhMetall.get(dust));
        route.consume(blastFurnaceRecipes, -1, CrudeRhMetall.get(dust), RHSalt.get(dust));
        route.consume(mixerRecipes, -1, RHSalt.get(dust), fluid(RHSaltSolution));
        route.consume(multiblockChemicalReactorRecipes, 1, fluid(RHSaltSolution), RHNitrate.get(dust));
        route.consume(sifterRecipes, -1, RHNitrate.get(dust), RhFilterCake.get(dust));
        route.consume(mixerRecipes, -1, RhFilterCake.get(dust), fluid(RHFilterCakeSolution));
        route.consume(multiblockChemicalReactorRecipes, -1, fluid(RHFilterCakeSolution), ReRh.get(dust));
        route.consume(multiblockChemicalReactorRecipes, -1, ReRh.get(dust), Rhodium.get(dust));

        route.consume(blastFurnaceRecipes, -1, LeachResidue.get(dust), SodiumRuthenate.get(dust));
        route.consume(
            multiblockChemicalReactorRecipes,
            -1,
            SodiumRuthenate.get(dust),
            fluid(RutheniumTetroxideSollution));
        route
            .consume(fluidHeaterRecipes, -1, fluid(RutheniumTetroxideSollution), fluid(HotRutheniumTetroxideSollution));
        route.consume(distillationTowerRecipes, -1, fluid(HotRutheniumTetroxideSollution), fluid(RutheniumTetroxide));
        route.consume(fluidSolidifierRecipes, 1, fluid(RutheniumTetroxide), RutheniumTetroxide.get(dust));
        route.consume(multiblockChemicalReactorRecipes, -1, RutheniumTetroxide.get(dust), Ruthenium.get(dust));

        route.consume(blastFurnaceRecipes, 11, IrOsLeachResidue.get(dust), fluid(AcidicOsmiumSolution));
        route.consume(distillationTowerRecipes, -1, fluid(AcidicOsmiumSolution), fluid(OsmiumSolution));
        route.consume(multiblockChemicalReactorRecipes, -1, fluid(OsmiumSolution), Materials.Osmium.getDust(1));
        route.consume(blastFurnaceRecipes, 1, IrLeachResidue.get(dust), IridiumDioxide.get(dust));
        route.consume(multiblockChemicalReactorRecipes, -1, IridiumDioxide.get(dust), fluid(AcidicIridiumSolution));
        route.consume(multiblockChemicalReactorRecipes, -1, fluid(AcidicIridiumSolution), IridiumChloride.get(dust));
        route.consume(multiblockChemicalReactorRecipes, -1, IridiumChloride.get(dust), Materials.Iridium.getDust(1));
        recoverPlatinumReagents(route);

        return route.build(
            Materials.Platinum.getDust(1),
            Materials.Palladium.getDust(1),
            Rhodium.get(dust),
            Ruthenium.get(dust),
            Materials.Osmium.getDust(1),
            Materials.Iridium.getDust(1));
    }

    private static void recoverPlatinumReagents(Route route) {
        FluidStack nitricAcid = Materials.NitricAcid.getFluid(1);
        route.consume(multiblockChemicalReactorRecipes, 24, Materials.NitrogenDioxide.getGas(1), nitricAcid);
        route.replenish(GTNRRecipeMaps.MODERN_MIXER_RECIPES, 3, nitricAcid, fluid(AquaRegia));

        FluidStack saltWater = Materials.SaltWater.getFluid(1);
        route.replenish(mixerRecipes, 3, Materials.Salt.getDust(1), saltWater);
        recoverSodiumNitrate(route);
        FluidStack calciumChloride = fluid(CalciumChloride);
        route.consume(fluidExtractionRecipes, -1, CalciumChloride.get(dust), calciumChloride);
        route.consume(electrolyzerRecipes, -1, calciumChloride, Materials.Calcium.getDust(1));

        route.consume(multiblockChemicalReactorRecipes, -1, ZincSulfate.get(dust), Materials.Zinc.getDust(1));
        recoverPotassiumDisulfate(route);

        FluidStack hydrochloricAcid = Materials.HydrochloricAcid.getFluid(1);
        FluidStack chlorine = Materials.Chlorine.getGas(1);
        route.batch.replenish(
            route.find(multiblockChemicalReactorRecipes, 8, chlorine, recipe -> outputs(recipe, hydrochloricAcid)),
            hydrochloricAcid,
            route.batch.fluidBalance(chlorine));
        FluidStack ammoniumChloride = fluid(AmmoniumChloride);
        route.batch.replenish(
            route.find(
                multiblockChemicalReactorRecipes,
                9,
                hydrochloricAcid,
                recipe -> outputs(recipe, ammoniumChloride)),
            ammoniumChloride,
            route.batch.fluidBalance(hydrochloricAcid));
    }

    private static void recoverPotassiumDisulfate(Route route) {
        ItemStack potassiumIngot = Materials.Potassium.getIngots(1);
        ItemStack potassiumDust = Materials.Potassium.getDust(1);
        ItemStack disulfateDust = PotassiumDisulfate.get(dust);
        route.consume(fluidSolidifierRecipes, -1, Materials.Potassium.getMolten(1), potassiumIngot);
        route.consume(maceratorRecipes, -1, potassiumIngot, potassiumDust);
        route.consume(multiblockChemicalReactorRecipes, -1, potassiumDust, disulfateDust);
        route.consume(fluidExtractionRecipes, -1, disulfateDust, PotassiumDisulfate.getMolten(1));
    }

    private static void recoverSodiumNitrate(Route route) {
        ItemStack sodium = Materials.Sodium.getDust(1);
        ItemStack salt = Materials.Salt.getDust(1);
        ItemStack nitrate = SodiumNitrate.get(dust);
        // Each pair of returned salt dust yields one sodium dust for five nitrate dust.
        BigDecimal recoverableNitrate = route.batch.itemBalance(salt)
            .multiply(BigDecimal.valueOf(2.5));
        route.batch.replenish(
            route.find(multiblockChemicalReactorRecipes, -1, sodium, recipe -> outputs(recipe, nitrate)),
            nitrate,
            recoverableNitrate);
        route.batch.replenish(route.find(electrolyzerRecipes, -1, salt, recipe -> outputs(recipe, sodium)), sodium);
    }

    private static GTRecipeBuilder rareEarth() {
        Route route = new Route();
        List<ItemStack> products = new ArrayList<>();
        processRareEarth(route, RARE_EARTH_LOW, Materials.SulfuricAcid.getFluid(1), products);
        processRareEarth(route, RARE_EARTH_MID, Materials.HydrochloricAcid.getFluid(1), products);
        processRareEarth(route, RARE_EARTH_HIGH, Materials.HydrofluoricAcid.getFluid(1), products);
        return route.build(products.toArray(ItemStack[]::new));
    }

    private static void processRareEarth(Route route, Material grade, FluidStack acid, List<ItemStack> products) {
        ItemStack feed = Materials.RareEarth.getDust(1);
        ItemStack crushedOre = grade.getCrushed(1);
        ItemStack centrifugedOre = grade.getCrushedCentrifuged(1);
        ItemStack gradeDust = grade.getDust(1);
        GTRecipe bath = route.find(
            chemicalBathRecipes,
            -1,
            feed,
            recipe -> outputs(recipe, crushedOre) && contains(recipe.mInputs, recipe.mFluidInputs, acid));
        route.batch.add(bath, RARE_EARTH_BATH_BATCHES);
        route.consume(thermalCentrifugeRecipes, -1, crushedOre, centrifugedOre);
        route.consume(hammerRecipes, -1, centrifugedOre, gradeDust);
        GTRecipe dehydrator = route
            .find(chemicalDehydratorRecipes, -1, gradeDust, recipe -> recipe.mOutputs.length == 9);
        route.batch.consume(dehydrator, gradeDust);
        for (ItemStack product : dehydrator.mOutputs) {
            products.add(product);
        }
    }

    private static GTRecipeBuilder indium() {
        Route route = new Route();
        ItemStack purifiedGalena = GTOreDictUnificator.get(crushedPurified, Materials.Galena, 1);
        ItemStack purifiedSphalerite = GTOreDictUnificator.get(crushedPurified, Materials.Sphalerite, 1);
        FluidStack concentrate = new FluidStack(ItemList.sIndiumConcentrate, 1);
        FluidStack leadZincSolution = new FluidStack(ItemList.sLeadZincSolution, 1);
        ItemStack indium = Materials.Indium.getDust(1);
        ItemStack lead = Materials.Lead.getDust(1);
        route.add(multiblockChemicalReactorRecipes, 8, purifiedGalena, concentrate, 1);
        route.batch.replaceItemInput(purifiedGalena, Materials.Galena.getDust(1));
        route.batch.replaceItemInput(purifiedSphalerite, Materials.Sphalerite.getDust(1));
        route.consume(multiblockChemicalReactorRecipes, 9, concentrate, indium);
        route.consume(electrolyzerNonCellRecipes, -1, leadZincSolution, lead);
        return route.build(
            new FluidStack[] { Materials.Water.getFluid(1) },
            indium,
            lead,
            Materials.Silver.getDust(1),
            Materials.Zinc.getDust(1),
            Materials.Sulfur.getDust(1));
    }

    private static GTRecipeBuilder monazite() {
        Route route = new Route();
        ItemStack crushedMonazite = GTOreDictUnificator.get(crushed, Materials.Monazite, 1);
        route.add(
            digesterRecipes,
            -1,
            crushedMonazite,
            fluid(MuddyRareEarthMonaziteSolution),
            MONAZITE_DIGESTION_BATCHES);
        route.batch.replaceItemInput(crushedMonazite, Materials.Monazite.getDust(1));
        route.consume(
            dissolutionTankRecipes,
            9,
            fluid(MuddyRareEarthMonaziteSolution),
            fluid(DilutedRareEarthMonaziteMud));
        route.consume(sifterRecipes, -1, fluid(DilutedRareEarthMonaziteMud), MonaziteSulfate.get(dust));
        route.consume(mixerRecipes, -1, MonaziteSulfate.get(dust), fluid(DilutedMonaziteSulfate));
        route.consume(
            multiblockChemicalReactorRecipes,
            14,
            fluid(DilutedMonaziteSulfate),
            AcidicMonazitePowder.get(dust));
        route.consume(sifterRecipes, -1, AcidicMonazitePowder.get(dust), MonaziteRareEarthFiltrate.get(dust));
        route.consume(blastFurnaceRecipes, -1, ThoriumPhosphateCake.get(dust), ThoriumPhosphateConcentrate.get(dust));
        route
            .consume(thermalCentrifugeRecipes, -1, ThoriumPhosphateConcentrate.get(dust), Materials.Thorium.getDust(1));

        route.consume(
            chemicalBathRecipes,
            -1,
            MonaziteRareEarthFiltrate.get(dust),
            NeutralizedMonaziteRareEarthFiltrate.get(dust));
        route.consume(
            sifterRecipes,
            -1,
            NeutralizedMonaziteRareEarthFiltrate.get(dust),
            MonaziteRareEarthHydroxideConcentrate.get(dust));
        route.consume(chemicalBathRecipes, -1, UraniumFiltrate.get(dust), NeutralizedUraniumFiltrate.get(dust));
        route.consume(sifterRecipes, -1, NeutralizedUraniumFiltrate.get(dust), Materials.Uranium.getDust(1));

        route.consume(
            blastFurnaceRecipes,
            -1,
            MonaziteRareEarthHydroxideConcentrate.get(dust),
            DriedMonaziteRareEarthConcentrate.get(dust));
        route.consume(
            multiblockChemicalReactorRecipes,
            -1,
            DriedMonaziteRareEarthConcentrate.get(dust),
            fluid(NitratedRareEarthMonaziteConcentrate));
        FluidStack nitrated = fluid(NitratedRareEarthMonaziteConcentrate);
        GTRecipe leaching = route.find(
            mixerNonCellRecipes,
            -1,
            nitrated,
            recipe -> outputs(recipe, fluid(NitricLeachedMonaziteMixture))
                && contains(recipe.mInputs, recipe.mFluidInputs, Materials.Water.getFluid(1)));
        route.batch.consume(leaching, nitrated);
        route.consume(sifterRecipes, -1, fluid(NitricLeachedMonaziteMixture), fluid(NitricMonaziteLeachedConcentrate));
        route.consume(
            vacuumFreezerRecipes,
            -1,
            fluid(NitricMonaziteLeachedConcentrate),
            CooledMonaziteRareEarthConcentrate.get(dust));
        route.consume(
            electroMagneticSeparatorRecipes,
            -1,
            CooledMonaziteRareEarthConcentrate.get(dust),
            MonaziteRarerEarthSediment.get(dust));
        route.consume(
            blastFurnaceRecipes,
            -1,
            MonaziteRarerEarthSediment.get(dust),
            MonaziteHeterogenousHalogenicRareEarthMixture.get(dust));
        ItemStack halogenicMixture = MonaziteHeterogenousHalogenicRareEarthMixture.get(dust);
        GTRecipe saturation = route.find(
            mixerRecipes,
            -1,
            halogenicMixture,
            recipe -> outputs(recipe, SaturatedMonaziteRareEarthMixture.get(dust))
                && contains(recipe.mInputs, recipe.mFluidInputs, Materials.Salt.getDust(1)));
        route.batch.consume(saturation, halogenicMixture);
        route.consume(centrifugeRecipes, -1, SaturatedMonaziteRareEarthMixture.get(dust), SamaricResidue.get(dust));
        route.consume(sifterRecipes, -1, SamaricResidue.get(dust), Materials.Samarium.getDust(1));

        route.consume(multiblockChemicalReactorRecipes, -1, EuropiumIIIOxide.get(dust), EuropiumOxide.get(dust));
        route.consume(electrolyzerRecipes, -1, EuropiumOxide.get(dust), Materials.Europium.getDust(1));
        route.consume(multiblockChemicalReactorRecipes, -1, CeriumDioxide.get(dust), CeriumChloride.get(dust));
        route.consume(multiblockChemicalReactorRecipes, 1, CeriumChloride.get(dust), CeriumOxalate.get(dust));
        route.consume(blastFurnaceRecipes, -1, CeriumOxalate.get(dust), CeriumIIIOxide.get(dust));
        route.consume(electrolyzerNonCellRecipes, -1, CeriumIIIOxide.get(dust), Materials.Cerium.getDust(1));
        recoverMonaziteMetals(route);
        recoverMonaziteReagents(route);

        return route.build(
            Materials.Cerium.getDust(1),
            Materials.Thorium.getDust(1),
            Materials.Uranium.getDust(1),
            Materials.Uranium235.getDust(1),
            Materials.Samarium.getDust(1),
            Materials.Gadolinium.getDust(1),
            Materials.Europium.getDust(1),
            Hafnium.get(ingot),
            Zirconium.get(ingot));
    }

    private static void recoverMonaziteMetals(Route route) {
        route.consume(centrifugeRecipes, -1, HafniaZirconiaBlend.get(dust), Hafnia.get(dust));
        route.consume(multiblockChemicalReactorRecipes, 1, Zirconia.get(dust), ZirconiumTetrachloride.get(dust));
        route.consume(
            multiblockChemicalReactorRecipes,
            1,
            ZirconiumTetrachloride.get(dust),
            fluid(ZirconiumTetrachlorideSolution));
        route.consume(blastFurnaceRecipes, 2, fluid(ZirconiumTetrachlorideSolution), Zirconium.get(ingotHot));
        route.consume(vacuumFreezerRecipes, -1, Zirconium.get(ingotHot), Zirconium.get(ingot));

        route.consume(multiblockChemicalReactorRecipes, 1, Hafnia.get(dust), HafniumTetrachloride.get(dust));
        route.consume(
            multiblockChemicalReactorRecipes,
            1,
            HafniumTetrachloride.get(dust),
            fluid(HafniumTetrachlorideSolution));
        route.consume(blastFurnaceRecipes, 2, fluid(HafniumTetrachlorideSolution), LowPurityHafnium.get(dust));
        route.consume(multiblockChemicalReactorRecipes, 1, LowPurityHafnium.get(dust), HafniumIodide.get(dust));
        route.consume(blastFurnaceRecipes, 12, HafniumIodide.get(dust), Hafnium.get(ingotHot));
        route.consume(vacuumFreezerRecipes, -1, Hafnium.get(ingotHot), Hafnium.get(ingot));
    }

    private static void recoverMonaziteReagents(Route route) {
        route.replenish(multiblockChemicalReactorRecipes, 9, Materials.Ammonia.getGas(1), fluid(AmmoniumChloride));

        ItemStack magnesium = Materials.Magnesium.getDust(1);
        ItemStack sodium = Materials.Sodium.getDust(1);
        ItemStack salt = Materials.Salt.getDust(1);
        route.consume(multiblockChemicalReactorRecipes, -1, Materials.Magnesiumchloride.getDust(1), magnesium);
        route.batch.replenish(route.find(electrolyzerRecipes, -1, salt, recipe -> outputs(recipe, sodium)), sodium);

        ItemStack carbon = Materials.Carbon.getDust(1);
        FluidStack hydrochloricAcid = Materials.HydrochloricAcid.getFluid(1);
        FluidStack chlorine = Materials.Chlorine.getGas(1);
        FluidStack hydrogen = Materials.Hydrogen.getGas(1);
        route.consume(electrolyzerNonCellRecipes, -1, Materials.Chloromethane.getGas(1), carbon);
        route.batch.replenish(
            route.find(electrolyzerRecipes, 1, Materials.CarbonMonoxide.getGas(1), recipe -> outputs(recipe, carbon)),
            carbon);
        route.replenish(multiblockChemicalReactorRecipes, 8, chlorine, hydrochloricAcid);
        route.replenish(electrolyzerNonCellRecipes, 1, Materials.Water.getFluid(1), hydrogen);
    }

    private static GTRecipeBuilder polybenzimidazole() {
        Route route = new Route();
        FluidStack benzene = Materials.Benzene.getFluid(1);
        FluidStack xylene = Materials.Dimethylbenzene.getFluid(1);
        FluidStack phthalicAcid = Materials.PhthalicAcid.getFluid(1);
        FluidStack ester = Materials.Diphenylisophthalate.getFluid(1);
        FluidStack diamine = Materials.Diaminobenzidin.getFluid(1);
        FluidStack pbi = Materials.Polybenzimidazole.getMolten(1);
        route.add(multiblockChemicalReactorRecipes, -1, ester, pbi, 9);
        route.replenish(multiblockChemicalReactorRecipes, -1, phthalicAcid, ester);
        GTRecipe oxidation = route.find(
            multiblockChemicalReactorRecipes,
            -1,
            xylene,
            recipe -> outputs(recipe, phthalicAcid)
                && contains(recipe.mInputs, recipe.mFluidInputs, Materials.Potassiumdichromate.getDust(1)));
        route.batch.replenish(oxidation, phthalicAcid);
        route.replenish(multiblockChemicalReactorRecipes, 1, benzene, xylene);

        FluidStack dichlorobenzidine = Materials.Dichlorobenzidine.getFluid(1);
        FluidStack nitrochlorobenzene = Materials.Nitrochlorobenzene.getFluid(1);
        FluidStack chlorobenzene = Materials.Chlorobenzene.getFluid(1);
        route.replenish(multiblockChemicalReactorRecipes, -1, dichlorobenzidine, diamine);
        route.replenish(multiblockChemicalReactorRecipes, 9, nitrochlorobenzene, dichlorobenzidine);
        route.replenish(multiblockChemicalReactorRecipes, 1, chlorobenzene, nitrochlorobenzene);
        route.replenish(multiblockChemicalReactorRecipes, 1, benzene, chlorobenzene);
        preparePolybenzimidazoleReagents(route);
        return route
            .build(new FluidStack[] { Materials.HydrochloricAcid.getFluid(1), Materials.Water.getFluid(1), pbi });
    }

    private static void preparePolybenzimidazoleReagents(Route route) {
        route.replenish(multiblockChemicalReactorRecipes, 1, Materials.Carbon.getDust(1), Materials.Methane.getGas(1));
        route.replenish(multiblockChemicalReactorRecipes, 1, Materials.Nitrogen.getGas(1), Materials.Ammonia.getGas(1));
        route.replenish(
            mixerNonCellRecipes,
            1,
            Materials.NitricAcid.getFluid(1),
            Materials.NitrationMixture.getFluid(1));

        FluidStack sulfuricAcid = Materials.SulfuricAcid.getFluid(1);
        GTRecipe regeneration = route.find(
            multiblockChemicalReactorRecipes,
            23,
            Materials.Sulfur.getDust(1),
            recipe -> outputs(recipe, sulfuricAcid)
                && contains(recipe.mInputs, recipe.mFluidInputs, Materials.DilutedSulfuricAcid.getFluid(1)));
        route.batch.replenish(regeneration, sulfuricAcid);
    }

    private static GTRecipeBuilder netherite() {
        Route route = new Route();
        ItemStack scrap = getModItem(Mods.EtFuturumRequiem.ID, "netherite_scrap", 1);
        if (scrap == null) throw new IllegalStateException("missing Et Futurum netherite scrap");
        ItemStack hotScrap = ItemList.Hot_Netherite_Scrap.get(1);
        ItemStack brittleScrap = ItemList.Brittle_Netherite_Scrap.get(1);
        ItemStack nanoparticles = ItemList.Netherite_Nanoparticles.get(1);
        ItemStack bondedNanoparticles = ItemList.Intensely_Bonded_Netherite_Nanoparticles.get(1);
        route.add(chemicalBathRecipes, -1, hotScrap, brittleScrap, NETHERITE_BATH_BATCHES);
        route.consume(chemicalBathRecipes, -1, scrap, hotScrap);
        route.consume(maceratorRecipes, -1, brittleScrap, nanoparticles);
        route.consume(blastFurnaceRecipes, 1, nanoparticles, bondedNanoparticles);
        prepareNetheriteBathReagents(route);

        // The native rhodium electrolysis stage is supplied by the core mod's Thaumic Tinkerer integration.
        if (Mods.NewHorizonsCoreMod.isModLoaded() && Mods.ThaumicTinkerer.isModLoaded()) {
            ItemStack hellishMetalBlock = GTOreDictUnificator.get(block, Materials.HellishMetal, 1);
            ItemStack rhodiumBlock = MaterialsElements.getInstance().RHODIUM.getBlock(1);
            GTRecipe synthesis = route
                .find(electrolyzerNonCellRecipes, -1, rhodiumBlock, recipe -> outputs(recipe, hellishMetalBlock), true);
            if (synthesis != null) {
                route.replenish(fluidExtractionRecipes, -1, hellishMetalBlock, Materials.HellishMetal.getMolten(1));
                route.batch.replenish(synthesis, hellishMetalBlock);
            }
        }
        return route.build(bondedNanoparticles);
    }

    private static void prepareNetheriteBathReagents(Route route) {
        FluidStack froth = new FluidStack(GTPPFluids.NetherrackFlotationFroth, 1);
        ItemStack milledNetherrack = GTOreDictUnificator.get(milled, Materials.Netherrack, 1);
        route.replenish(vacuumFurnaceRecipes, -1, froth, Materials.PoorNetherWaste.getFluid(1));
        route.replenish(flotationCellRecipes, -1, milledNetherrack, froth);
        GTRecipe milling = route
            .find(millingRecipes, 10, new ItemStack(Blocks.netherrack), recipe -> outputs(recipe, milledNetherrack));
        route.batch.replenish(milling, milledNetherrack);

        FluidStack netherGas = Materials.NefariousGas.getFluid(1);
        FluidStack oil = Materials.NefariousOil.getFluid(1);
        route.batch.replenish(
            route.find(
                crackingRecipes,
                1,
                netherGas,
                recipe -> outputs(recipe, oil)
                    && contains(recipe.mInputs, recipe.mFluidInputs, Materials.Grade2PurifiedWater.getFluid(1))),
            oil);
        FluidStack netherSemiFluid = Materials.NetherSemiFluid.getFluid(1);
        ItemStack mud = ItemList.Heavy_Hellish_Mud.get(1);
        route.batch
            .replenish(route.find(distillationTowerRecipes, -1, netherSemiFluid, recipe -> outputs(recipe, mud)), mud);
        route.replenish(vacuumFreezerRecipes, -1, Materials.NetherAir.getFluid(1), netherSemiFluid);
    }

    private static boolean matchesCircuit(GTRecipe recipe, int circuit) {
        for (ItemStack item : recipe.mInputs) {
            if (item != null && GTUtility.isAnyIntegratedCircuit(item)) return item.getItemDamage() == circuit;
        }
        return circuit < 0;
    }

    private static boolean outputs(GTRecipe recipe, Object ingredient) {
        return contains(recipe.mOutputs, recipe.mFluidOutputs, ingredient);
    }

    private static boolean contains(ItemStack[] items, FluidStack[] fluids, Object ingredient) {
        return switch (ingredient) {
            case ItemStack stack -> Arrays.stream(items).anyMatch(item -> GTUtility.areStacksEqual(item, stack));
            case FluidStack stack -> Arrays.stream(fluids).anyMatch(stack::isFluidEqual);
            default -> false;
        };
    }

    private static FluidStack fluid(Werkstoff material) {
        return material.getFluidOrGas(1);
    }

    private static final class Route {

        private final PlatinumGateBatch batch = new PlatinumGateBatch();
        private int furnaceHeat;
        private int digesterHeat;

        private void add(RecipeMap<?> map, int circuit, Object input, Object output, long count) {
            batch.add(find(map, circuit, input, recipe -> outputs(recipe, output)), count);
        }

        private void consume(RecipeMap<?> map, int circuit, Object input, Object output) {
            GTRecipe recipe = find(map, circuit, input, candidate -> outputs(candidate, output));
            switch (input) {
                case ItemStack item -> batch.consume(recipe, item);
                case FluidStack fluid -> batch.consume(recipe, fluid);
                default -> throw new IllegalStateException("unsupported native input " + input);
            }
        }

        private void replenish(RecipeMap<?> map, int circuit, Object input, FluidStack output) {
            batch.replenish(find(map, circuit, input, recipe -> outputs(recipe, output)), output);
        }

        private GTRecipe find(RecipeMap<?> map, int circuit, Object input, Predicate<GTRecipe> match) {
            return find(map, circuit, input, match, false);
        }

        private GTRecipe find(RecipeMap<?> map, int circuit, Object input, Predicate<GTRecipe> match,
            boolean optional) {
            List<GTRecipe> candidates = map.getAllRecipes()
                .stream()
                .filter(
                    recipe -> recipe.mEnabled && !recipe.mFakeRecipe
                        && recipe.mDuration > 0
                        && recipe.mEUt > 0
                        && matchesCircuit(recipe, circuit)
                        && contains(recipe.mInputs, recipe.mFluidInputs, input)
                        && match.test(recipe))
                .limit(2)
                .toList();
            if (optional && candidates.isEmpty()) return null;
            if (candidates.size() != 1) throw new IllegalStateException(
                "expected one recipe in " + map.unlocalizedName
                    + " for "
                    + input
                    + " (circuit "
                    + circuit
                    + "), found "
                    + candidates.size());
            GTRecipe source = candidates.get(0);
            if (map == blastFurnaceRecipes || map == vacuumFurnaceRecipes)
                furnaceHeat = Math.max(furnaceHeat, source.getMetadataOrDefault(COIL_HEAT, source.mSpecialValue));
            if (map == digesterRecipes)
                digesterHeat = Math.max(digesterHeat, source.getMetadataOrDefault(COIL_HEAT, source.mSpecialValue));
            return source;
        }

        private GTRecipeBuilder build(ItemStack... products) {
            return build(GTValues.emptyFluidStackArray, products);
        }

        private GTRecipeBuilder build(FluidStack[] fluidProducts, ItemStack... products) {
            int eut = batch.maxEut();
            int duration = batch.energy()
                .divide(BigDecimal.valueOf(eut), 0, RoundingMode.CEILING)
                .intValueExact();
            GTRecipeBuilder builder = batch.build(products, fluidProducts)
                .duration(duration)
                .eut(eut)
                .specialValue(furnaceHeat);
            if (furnaceHeat > 0) builder.metadata(COIL_HEAT, furnaceHeat);
            if (digesterHeat > 0) builder.metadata(DIGESTER_HEAT, digesterHeat);
            return builder;
        }
    }
}
