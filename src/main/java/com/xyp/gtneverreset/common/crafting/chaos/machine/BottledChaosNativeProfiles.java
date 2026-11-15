package com.xyp.gtneverreset.common.crafting.chaos.machine;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.Part;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.Rule;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.Structure;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import bartworks.API.recipe.BartWorksRecipeMaps;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import goodgenerator.loader.Loaders;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GlassTier;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.core.material.MaterialsAlloy;
import tectech.thing.block.BlockQuantumGlass;
import tectech.thing.casing.TTCasingsContainer;

/** Paid source structures with native controllers; execution retains the selected source's parameters. */
public final class BottledChaosNativeProfiles {

    private static final String PREFIX = "gtnr.chaos.source.";
    private static final Map<BottledChaosSupportedMachine, Definition> definitions = new LinkedHashMap<>();

    private BottledChaosNativeProfiles() {}

    static void register() {
        definitions.clear();
        for (Definition definition : declarations()) {
            definitions.put(definition.source, definition);
            ItemStack controller = Arrays.stream(GregTechAPI.METATILEENTITIES)
                .filter(machine -> BottledChaosSupportedMachine.find(machine) == definition.controllerSource)
                .findFirst()
                .orElseThrow(
                    () -> new IllegalStateException("Missing native source controller " + definition.controllerSource))
                .getStackForm(1);
            if (definition.source.structureKind == BottledChaosSupportedMachine.StructureKind.REPEATING) {
                BottledChaosStructureCatalog.addNativeRepeatedRules(
                    rule(definition, controller, 1),
                    rule(definition, controller, 2),
                    definition.source.displayProfiles());
            } else for (int profile : definition.source.displayProfiles())
                BottledChaosStructureCatalog.addNativeRule(rule(definition, controller, profile));
        }
    }

    static BottledChaosSupportedMachine source(String key) {
        if (key == null || !key.startsWith(PREFIX)) return null;
        try {
            return BottledChaosSupportedMachine.valueOf(key.substring(PREFIX.length()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static List<RecipeMap<?>> pools(BottledChaosSupportedMachine source) {
        Definition definition = definitions.get(source);
        return definition == null ? List.of() : definition.pools;
    }

    static int poolCount(Structure structure) {
        Definition definition = structure == null ? null : definitions.get(source(structure.machineKey()));
        return definition == null ? 0 : definition.poolCount;
    }

    public static String displayName(String key) {
        BottledChaosSupportedMachine source = source(key);
        if (source == null) return null;
        return name(source);
    }

    private static Rule rule(Definition definition, ItemStack controller, int profile) {
        Map<Position, Character> positions = new LinkedHashMap<>();
        for (var piece : definition.source.pieces(null, profile)) {
            String path = definition.pieces.get(piece.name());
            if (path == null) throw new IllegalStateException("Missing source shape " + piece.name());
            String[][] shape = MultiblockStructureLoader.load(path);
            for (int y = 0; y < shape.length; y++)
                for (int z = 0; z < shape[y].length; z++) for (int x = 0; x < shape[y][z].length(); x++) {
                    char symbol = shape[y][z].charAt(x);
                    if (symbol != ' ' && symbol != '~' && symbol != '-')
                        positions.put(new Position(x - piece.a(), y - piece.b(), z - piece.c()), symbol);
                }
        }
        Map<Character, Integer> counts = new TreeMap<>();
        for (char symbol : positions.values()) counts.merge(symbol, 1, Math::addExact);
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(0, controller.copy()));
        int groups = 0;
        for (var entry : counts.entrySet()) groups = BottledChaosStructureCatalog.appendParts(
            parts,
            definition.materials.get(entry.getKey(), controller, definition.source),
            entry.getValue(),
            groups);
        var requirements = BottledChaosStructureCatalog.dedicatedMaterials(definition.source, profile);
        groups = BottledChaosStructureCatalog.moveMixedPhotonUpgrades(definition.source, parts, requirements, groups);
        BigInteger pools = BigInteger.ZERO;
        for (RecipeMap<?> pool : definition.pools) pools = pools.or(BottledChaosMachineConfiguration.mask(pool));
        return new Rule(
            PREFIX + definition.source.name(),
            profile,
            pools,
            controller,
            parts.toArray(new Part[0]),
            groups,
            requirements.toArray(new BottledChaosStructureCatalog.Requirement[0]));
    }

    private static List<ItemStack> variants(Block block, int first, int last) {
        List<ItemStack> result = new ArrayList<>();
        for (int meta = first; meta <= last; meta++) result.add(new ItemStack(block, 1, meta));
        return result;
    }

    private static List<ItemStack> glasses(int exactTier) {
        List<ItemStack> result = new ArrayList<>();
        for (var glass : GlassTier.getGlassList())
            if (exactTier < 0 || GlassTier.getGlassBlockTier(glass.getLeft(), glass.getRight()) == exactTier)
                result.add(new ItemStack(glass.getLeft(), 1, glass.getRight()));
        return result;
    }

    private static List<ItemStack> coils(ItemStack controller, BottledChaosSupportedMachine source) {
        return BottledChaosStructureCatalog.nativeChoices(
            GTStructureUtility
                .<Object>ofCoil((Object context, HeatingCoilLevel coil) -> {}, context -> HeatingCoilLevel.None),
            controller,
            source);
    }

    @SafeVarargs
    private static List<ItemStack> join(List<ItemStack>... values) {
        List<ItemStack> result = new ArrayList<>();
        for (List<ItemStack> value : values) result.addAll(value);
        return result;
    }

    private static List<Definition> declarations() {
        return List.of(
            new Definition(
                BottledChaosSupportedMachine.GTCM_CRYSTALLINE_INFINITIER,
                BottledChaosSupportedMachine.MTEMULTI_AUTOCLAVE,
                3,
                List.of(RecipeMaps.autoclaveRecipes, RecipeMaps.chemicalBathRecipes),
                Map.ofEntries(
                    Map.entry("mainCrystallineInfinitier", "chaos_sources/gtcm_crystalline_infinitier/shape")),
                BottledChaosNativeProfiles::materialsGtcmCrystallineInfinitier),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_HOLY_SEPARATOR,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_CUTTING_MACHINE,
                2,
                List.of(RecipeMaps.latheRecipes, RecipeMaps.cutterRecipes),
                Map.ofEntries(
                    Map.entry("mainHolySeparator", "chaos_sources/gt_tile_entity_holy_separator/shapemain"),
                    Map.entry("middleHolySeparator", "chaos_sources/gt_tile_entity_holy_separator/shapemiddle"),
                    Map.entry("endHolySeparator", "chaos_sources/gt_tile_entity_holy_separator/shapeend")),
                BottledChaosNativeProfiles::materialsGtTileEntityHolySeparator),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_INTENSIFY_CHEMICAL_DISTORTER,
                BottledChaosSupportedMachine.MTEMEGA_CHEMICAL_REACTOR,
                3,
                List.of(RecipeMaps.multiblockChemicalReactorRecipes, RecipeMaps.chemicalPlantRecipes),
                Map.ofEntries(Map.entry("main", "chaos_sources/gt_tile_entity_intensify_chemical_distorter/shape")),
                BottledChaosNativeProfiles::materialsGtTileEntityIntensifyChemicalDistorter),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_MAGNETIC_DOMAIN_CONSTRUCTOR,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_ELECTROMAGNETIC_SEPARATOR,
                2,
                List.of(RecipeMaps.polarizerRecipes, RecipeMaps.electroMagneticSeparatorRecipes),
                Map.ofEntries(
                    Map.entry(
                        "mainMagneticDomainConstructor",
                        "chaos_sources/gt_tile_entity_magnetic_domain_constructor/shapemain"),
                    Map.entry(
                        "middleMagneticDomainConstructor",
                        "chaos_sources/gt_tile_entity_magnetic_domain_constructor/shapemiddle"),
                    Map.entry(
                        "endMagneticDomainConstructor",
                        "chaos_sources/gt_tile_entity_magnetic_domain_constructor/shapeend")),
                BottledChaosNativeProfiles::materialsGtTileEntityMagneticDomainConstructor),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_MAGNETIC_DRIVE_PRESSURE_FORMER,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_BENDING_MACHINE,
                4,
                List.of(
                    RecipeMaps.benderRecipes,
                    RecipeMaps.formingPressRecipes,
                    RecipeMaps.hammerRecipes,
                    RecipeMaps.extruderRecipes),
                Map.ofEntries(Map.entry("main", "chaos_sources/gt_tile_entity_magnetic_drive_pressure_former/shape")),
                BottledChaosNativeProfiles::materialsGtTileEntityMagneticDrivePressureFormer),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_MAGNETIC_MIXER,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_MIXER,
                1,
                List.of(RecipeMaps.mixerNonCellRecipes),
                Map.ofEntries(Map.entry("main", "chaos_sources/gt_tile_entity_magnetic_mixer/shape")),
                BottledChaosNativeProfiles::materialsGtTileEntityMagneticMixer),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_MOLECULE_DECONSTRUCTOR,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_CENTRIFUGE,
                2,
                List.of(RecipeMaps.centrifugeNonCellRecipes, RecipeMaps.electrolyzerNonCellRecipes),
                Map.ofEntries(
                    Map.entry(
                        "mainMoleculeDeconstructor",
                        "chaos_sources/gt_tile_entity_molecule_deconstructor/shapemain"),
                    Map.entry(
                        "middleMoleculeDeconstructor",
                        "chaos_sources/gt_tile_entity_molecule_deconstructor/shapemiddle"),
                    Map.entry(
                        "endMoleculeDeconstructor",
                        "chaos_sources/gt_tile_entity_molecule_deconstructor/shapeend")),
                BottledChaosNativeProfiles::materialsGtTileEntityMoleculeDeconstructor),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_PHYSICAL_FORM_SWITCHER,
                BottledChaosSupportedMachine.MTEMASS_SOLIDIFIER,
                2,
                List.of(RecipeMaps.fluidExtractionRecipes, RecipeMaps.fluidSolidifierRecipes),
                Map.ofEntries(Map.entry("main", "chaos_sources/gt_tile_entity_physical_form_switcher/shape")),
                BottledChaosNativeProfiles::materialsGtTileEntityPhysicalFormSwitcher),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_PRECISE_HIGH_ENERGY_PHOTONIC_QUANTUM_MASTER,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_LASER_ENGRAVER,
                2,
                List.of(RecipeMaps.laserEngraverRecipes),
                Map.ofEntries(
                    Map.entry(
                        "main",
                        "chaos_sources/gt_tile_entity_precise_high_energy_photonic_quantum_master/shape")),
                BottledChaosNativeProfiles::materialsGtTileEntityPreciseHighEnergyPhotonicQuantumMaster),
            new Definition(
                BottledChaosSupportedMachine.GT_TILE_ENTITY_SPACE_SCALER,
                BottledChaosSupportedMachine.MTEELECTRIC_IMPLOSION_COMPRESSOR,
                5,
                List.of(
                    RecipeMaps.extractorRecipes,
                    RecipeMaps.cyclotronRecipes,
                    RecipeMaps.compressorRecipes,
                    BartWorksRecipeMaps.electricImplosionCompressorRecipes,
                    RecipeMaps.neutroniumCompressorRecipes),
                Map.ofEntries(Map.entry("mainSpaceScaler", "chaos_sources/gt_tile_entity_space_scaler/shapemain")),
                BottledChaosNativeProfiles::materialsGtTileEntitySpaceScaler),
            new Definition(
                BottledChaosSupportedMachine.TST_ADVANCED_MEGA_OIL_CRACKER,
                BottledChaosSupportedMachine.MTEMEGA_OIL_CRACKER,
                1,
                List.of(RecipeMaps.crackingRecipes),
                Map.ofEntries(
                    Map.entry("mainAdvancedMegaOilCracker", "chaos_sources/tst_advanced_mega_oil_cracker/shapemain")),
                BottledChaosNativeProfiles::materialsTstAdvancedMegaOilCracker),
            new Definition(
                BottledChaosSupportedMachine.TST_BALL_LIGHTNING,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_ARC_FURNACE,
                1,
                List.of(RecipeMaps.arcFurnaceRecipes),
                Map.ofEntries(
                    Map.entry("BallLightningMK1", "chaos_sources/tst_ball_lightning/shapemk1"),
                    Map.entry("BallLightningMK2", "chaos_sources/tst_ball_lightning/shapemk2")),
                BottledChaosNativeProfiles::materialsTstBallLightning),
            new Definition(
                BottledChaosSupportedMachine.TST_BIOSPHERE_III,
                BottledChaosSupportedMachine.MTEBIO_VAT,
                3,
                List.of(
                    BartWorksRecipeMaps.bacterialVatRecipes,
                    RecipeMaps.brewingRecipes,
                    RecipeMaps.fermentingRecipes),
                Map.ofEntries(Map.entry("mainBiosphereIII", "chaos_sources/tst_biosphere_iii/shapemain")),
                BottledChaosNativeProfiles::materialsTstBiosphereIii),
            new Definition(
                BottledChaosSupportedMachine.TST_CORE_DEVICE_OF_HUMAN_POWER_GENERATION_FACILITY,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_FLUID_HEATER,
                1,
                List.of(RecipeMaps.fluidHeaterRecipes),
                Map.ofEntries(
                    Map.entry(
                        "mainCoreDeviceOfHumanPowerGenerationFacility",
                        "chaos_sources/tst_core_device_of_human_power_generation_facility/shape_main")),
                BottledChaosNativeProfiles::materialsTstCoreDeviceOfHumanPowerGenerationFacility),
            new Definition(
                BottledChaosSupportedMachine.TST_GIANT_VACUUM_DRYING_FURNACE,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_DEHYDRATOR,
                2,
                List.of(RecipeMaps.chemicalDehydratorNonCellRecipes, RecipeMaps.vacuumFurnaceRecipes),
                Map.ofEntries(
                    Map.entry("VacuumPump", "chaos_sources/tst_giant_vacuum_drying_furnace/shapevacuumpump"),
                    Map.entry("MainFrame", "chaos_sources/tst_giant_vacuum_drying_furnace/shapemainframe"),
                    Map.entry("DryingTowerBase", "chaos_sources/tst_giant_vacuum_drying_furnace/shapedryingtowerbase"),
                    Map.entry(
                        "DryingTowerMiddle",
                        "chaos_sources/tst_giant_vacuum_drying_furnace/shapedryingtowermiddle"),
                    Map.entry("DryingTowerHat", "chaos_sources/tst_giant_vacuum_drying_furnace/shapedryingtowerhat")),
                BottledChaosNativeProfiles::materialsTstGiantVacuumDryingFurnace),
            new Definition(
                BottledChaosSupportedMachine.TST_HEPHAESTUS_ATELIER,
                BottledChaosSupportedMachine.MTEMULTI_FURNACE,
                2,
                List.of(RecipeMaps.alloySmelterRecipes, RecipeMaps.furnaceRecipes),
                Map.ofEntries(Map.entry("mainHephaestusAtelier", "chaos_sources/tst_hephaestus_atelier/shape_main")),
                BottledChaosNativeProfiles::materialsTstHephaestusAtelier),
            new Definition(
                BottledChaosSupportedMachine.TST_INDISTINCT_TENTACLE,
                BottledChaosSupportedMachine.MTEPRECISE_ASSEMBLER,
                4,
                List.of(
                    GoodGeneratorRecipeMaps.componentAssemblyLineRecipes,
                    RecipeMaps.assemblerRecipes,
                    GoodGeneratorRecipeMaps.preciseAssemblerRecipes),
                Map.ofEntries(Map.entry("mainIndistinctTentacle", "chaos_sources/tst_indistinct_tentacle/shapemain")),
                BottledChaosNativeProfiles::materialsTstIndistinctTentacle),
            new Definition(
                BottledChaosSupportedMachine.TST_INDUSTRIAL_MAGNETAR_SEPARATOR,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_ELECTROMAGNETIC_SEPARATOR,
                1,
                List.of(RecipeMaps.electroMagneticSeparatorRecipes),
                Map.ofEntries(
                    Map.entry("STRUCTURE_PIECE_MAIN", "chaos_sources/tst_industrial_magnetar_separator/structure")),
                BottledChaosNativeProfiles::materialsTstIndustrialMagnetarSeparator),
            new Definition(
                BottledChaosSupportedMachine.TST_MANUFACTURING_CENTER,
                BottledChaosSupportedMachine.MTEAUTO_CRAFTER,
                9,
                List.of(
                    RecipeMaps.compressorRecipes,
                    RecipeMaps.latheRecipes,
                    RecipeMaps.polarizerRecipes,
                    RecipeMaps.fermentingRecipes,
                    RecipeMaps.fluidExtractionRecipes,
                    RecipeMaps.extractorRecipes,
                    RecipeMaps.laserEngraverRecipes,
                    RecipeMaps.autoclaveRecipes,
                    RecipeMaps.fluidSolidifierRecipes),
                Map.ofEntries(Map.entry("main", "chaos_sources/tst_manufacturing_center/shape_main")),
                BottledChaosNativeProfiles::materialsTstManufacturingCenter),
            new Definition(
                BottledChaosSupportedMachine.TST_MIRACLE_DOOR,
                BottledChaosSupportedMachine.MTEPLASMA_FORGE,
                2,
                List.of(GTNRRecipeMaps.StellarForgeRecipeMap, GTNRRecipeMaps.StellarForgeAlloySmelterRecipeMap),
                Map.ofEntries(
                    Map.entry("mainMiracleDoor", "chaos_sources/tst_miracle_door/shapemain"),
                    Map.entry("mainMiracleDoorErr", "chaos_sources/tst_miracle_door/shapemainerr")),
                BottledChaosNativeProfiles::materialsTstMiracleDoor),
            new Definition(
                BottledChaosSupportedMachine.TST_HYPER_THERMAL_CONVECTOR,
                BottledChaosSupportedMachine.MTECRYOGENIC_FREEZER,
                2,
                List.of(GTNRRecipeMaps.RapidCoolingDownRecipeMap),
                Map.ofEntries(
                    Map.entry("main", "chaos_sources/tst_hyper_thermal_convector/shapemain"),
                    Map.entry("old", "chaos_sources/tst_hyper_thermal_convector/shapeold")),
                BottledChaosNativeProfiles::materialsTstHyperThermalConvector),
            new Definition(
                BottledChaosSupportedMachine.TST_MEGA_MACERATOR,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_MACERATOR,
                1,
                List.of(RecipeMaps.maceratorRecipes),
                Map.ofEntries(
                    Map.entry("mainMegaMacerator", "chaos_sources/tst_mega_macerator/shape"),
                    Map.entry("oldMegaMacerator", "chaos_sources/tst_mega_macerator/shapeold")),
                BottledChaosNativeProfiles::materialsTstMegaMacerator),
            new Definition(
                BottledChaosSupportedMachine.TST_SCAVENGER,
                BottledChaosSupportedMachine.MTEINDUSTRIAL_SIFTER,
                1,
                List.of(RecipeMaps.sifterRecipes),
                Map.ofEntries(Map.entry("mainScavenger", "chaos_sources/tst_scavenger/shapemain")),
                BottledChaosNativeProfiles::materialsTstScavenger),
            new Definition(
                BottledChaosSupportedMachine.TST_SWELEGFYR_BLAST_FURNACE,
                BottledChaosSupportedMachine.MTEELECTRIC_BLAST_FURNACE,
                1,
                List.of(RecipeMaps.blastFurnaceRecipes),
                Map.ofEntries(
                    Map.entry("mainT1", "chaos_sources/tst_swelegfyr_blast_furnace/shapemaint1"),
                    Map.entry("mainT2", "chaos_sources/tst_swelegfyr_blast_furnace/shapemaint2"),
                    Map.entry("BlazeT1", "chaos_sources/tst_swelegfyr_blast_furnace/shapeblazet1"),
                    Map.entry("BlazeT2", "chaos_sources/tst_swelegfyr_blast_furnace/shapeblazet2")),
                BottledChaosNativeProfiles::materialsTstSwelegfyrBlastFurnace),
            new Definition(
                BottledChaosSupportedMachine.TST_THERMAL_ENERGY_DEVOURER,
                BottledChaosSupportedMachine.MTEVACUUM_FREEZER,
                1,
                List.of(RecipeMaps.vacuumFreezerRecipes),
                Map.ofEntries(
                    Map.entry("mainThermalEnergyDevourer", "chaos_sources/tst_thermal_energy_devourer/shape_main")),
                BottledChaosNativeProfiles::materialsTstThermalEnergyDevourer),
            new Definition(
                BottledChaosSupportedMachine.TST_VACUUM_FILTER_EXTRACTOR,
                BottledChaosSupportedMachine.MTEADV_DISTILLATION_TOWER,
                2,
                List.of(RecipeMaps.distillationTowerRecipes, RecipeMaps.distilleryRecipes),
                Map.ofEntries(
                    Map.entry("mainVacuumFilterExtractor", "chaos_sources/tst_vacuum_filter_extractor/shape")),
                BottledChaosNativeProfiles::materialsTstVacuumFilterExtractor),
            new Definition(
                BottledChaosSupportedMachine.MM_INDISTINCT_TENTACLE_PROTOTYPE_MK2,
                BottledChaosSupportedMachine.MTEPRECISE_ASSEMBLER,
                4,
                List.of(
                    GoodGeneratorRecipeMaps.componentAssemblyLineRecipes,
                    RecipeMaps.assemblerRecipes,
                    GoodGeneratorRecipeMaps.preciseAssemblerRecipes),
                Map.ofEntries(
                    Map.entry(
                        "mainIndistinctTentaclePrototypeMK2",
                        "chaos_sources/mm_indistinct_tentacle_prototype_mk2/shape_main")),
                BottledChaosNativeProfiles::materialsMmIndistinctTentaclePrototypeMk2),
            new Definition(
                BottledChaosSupportedMachine.MM_DIMENSIONALLY_TRANSCENDENT_MATTER_PLASMA_FORGE_PROTOTYPE_MK2,
                BottledChaosSupportedMachine.MTEPLASMA_FORGE,
                1,
                List.of(RecipeMaps.plasmaForgeRecipes),
                Map.ofEntries(
                    Map.entry(
                        "main",
                        "chaos_sources/mm_dimensionally_transcendent_matter_plasma_forge_prototype_mk2/shape_main")),
                BottledChaosNativeProfiles::materialsMmDimensionallyTranscendentMatterPlasmaForgePrototypeMk2),
            new Definition(
                BottledChaosSupportedMachine.MM_LARGE_NEUTRON_OSCILLATOR,
                BottledChaosSupportedMachine.MTENEUTRON_ACTIVATOR,
                1,
                List.of(GTNRRecipeMaps.NeutronActivatorWithEURecipeMap),
                Map.ofEntries(Map.entry("main", "chaos_sources/mm_large_neutron_oscillator/shape_main")),
                BottledChaosNativeProfiles::materialsMmLargeNeutronOscillator));
    }

    private static List<ItemStack> materialsGtcmCrystallineInfinitier(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'C' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 4));
            case 'D' -> join(
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 6)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 14)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 0)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 1)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 2)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 3)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 4)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 5)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 6)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 7)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 8)));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 8));
            case 'F' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 9));
            case 'G' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityHolySeparator(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'C' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 0));
            case 'D' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 4));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 6));
            case 'F' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 8));
            case 'G' -> List.of(new ItemStack(ModBlocks.blockCasings3Misc, 1, 15));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityIntensifyChemicalDistorter(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 's' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 1));
            case 'v' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 0));
            case 'p' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 1));
            case 'c' -> coils(controller, source);
            case 'h' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 0));
            case 'b' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 1));
            case 'e' -> List.of(new ItemStack(GregTechAPI.sBlockCasings1, 1, 11));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityMagneticDomainConstructor(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(Loaders.compactFusionCoil, 1, 0));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 8));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'O' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'F' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1));
            case 'G' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.TengamAttuned, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityMagneticDrivePressureFormer(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> List.of(new ItemStack(Loaders.compactFusionCoil, 1, 0));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 5));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14));
            case 'E' -> coils(controller, source);
            case 'F' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityMagneticMixer(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 8));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 2));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 3));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'E' -> List.of(new ItemStack(ModBlocks.blockCasings3Misc, 1, 11));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityMoleculeDeconstructor(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 15));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14));
            case 'D' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 0));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 8));
            case 'F' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14));
            case 'G' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14));
            case 'H' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14));
            case 'I' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.CosmicNeutronium, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityPhysicalFormSwitcher(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> List.of(new ItemStack(Loaders.MAR_Casing, 1, 0));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 8));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 15));
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'F' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntityPreciseHighEnergyPhotonicQuantumMaster(char symbol,
        ItemStack controller, BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 5));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 9));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings9, 1, 1));
            case 'M' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'I' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'X' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 0));
            case 'A' -> join(
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 0)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 1)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 2)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 3)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 4)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 5)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 6)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 7)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 8)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 9)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 10)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 11)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 12)),
                List.of(new ItemStack(BlockLoader.chaosPhotonUpgrade, 1, 13)),
                List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7)));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsGtTileEntitySpaceScaler(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 0));
            case 'B' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 4));
            case 'C' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 6));
            case 'D' -> List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 4));
            case 'G' -> join(
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 6)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 14)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 0)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 1)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 2)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 3)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 4)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 5)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 6)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 7)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 8)));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstAdvancedMegaOilCracker(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'C' -> coils(controller, source);
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 1));
            case 'F' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Vanadium, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstBallLightning(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> join(
                List.of(new ItemStack(Loaders.compactFusionCoil, 1, 0)),
                List.of(new ItemStack(Loaders.compactFusionCoil, 1, 1)),
                List.of(new ItemStack(Loaders.compactFusionCoil, 1, 2)),
                List.of(new ItemStack(Loaders.compactFusionCoil, 1, 3)),
                List.of(new ItemStack(Loaders.compactFusionCoil, 1, 4)),
                List.of(new ItemStack(BlockLoader.chaosCasing01, 1, 2)));
            case 'C' -> join(
                List.of(new ItemStack(GregTechAPI.sBlockCasings1, 1, 14)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 10)));
            case 'D' -> coils(controller, source);
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 5));
            case 'F' -> join(
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 8)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 9)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 0)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 1)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 2)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 3)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 4)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 5)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 6)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 7)),
                List.of(new ItemStack(TTCasingsContainer.StabilisationFieldGenerators, 1, 8)));
            case 'G' -> List.of(new ItemStack(BlockLoader.chaosCasing01, 1, 1));
            case 'H' -> List.of(new ItemStack(ModBlocks.blockCasingsTieredGTPP, 1, 9));
            case 'I' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsDyson, 1, 1));
            case 'J' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsDyson, 1, 8));
            case 'K' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsDyson, 1, 0));
            case 'L' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsDyson, 1, 5));
            case 'N' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.SuperconductorUIVBase, 1));
            case 'O' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1));
            case 'Y' -> List.of(new ItemStack(BlockLoader.chaosCasing01, 1, 1));
            case 'Z' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 5));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstBiosphereIii(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 15));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 1));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 0));
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 6));
            case 'F' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 5));
            case 'H' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 1));
            case 'I' -> List.of();
            case 'J' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Osmiridium, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstCoreDeviceOfHumanPowerGenerationFacility(char symbol,
        ItemStack controller, BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings1, 1, 11));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 0));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 4));
            case 'E' -> coils(controller, source);
            case 'F' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'G' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Osmiridium, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstGiantVacuumDryingFurnace(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'S' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 1));
            case 'G' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 1));
            case 'V' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 0));
            case 'I' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'F' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 1));
            case 'H' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 1));
            case 'J' -> List.of(new ItemStack(ModBlocks.blockCasings4Misc, 1, 10));
            case 'C' -> coils(controller, source);
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 2));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstHephaestusAtelier(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(GregTechAPI.sBlockCasings1, 1, 11));
            case 'B' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 4));
            case 'C' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 7));
            case 'D' -> List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0));
            case 'E' -> join(
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 7)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 14)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 10)));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstIndistinctTentacle(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> variants(Loaders.componentAssemblylineCasing, 0, 13);
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 9));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings9, 1, 1));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsBA0, 1, 12));
            case 'F' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 7));
            case 'G' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 9));
            case 'H' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 10));
            case 'I' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            case 'J' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 13));
            case 'K' -> join(
                List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 13)));
            case 'L' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            case 'M' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 14));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstIndustrialMagnetarSeparator(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 3));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstManufacturingCenter(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'C' -> List.of(new ItemStack(ModBlocks.blockCasings3Misc, 1, 2));
            case 'A' -> variants(BlockLoader.chaosMultiUseCore, 5, 8);
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstMiracleDoor(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 13));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsSE, 1, 1));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsSE, 1, 2));
            case 'D' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 4));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 6));
            case 'F' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 9));
            case 'G' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 10));
            case 'H' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            case 'I' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 14));
            case 'J' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsDyson, 1, 9));
            case 'K' -> List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0));
            case 'L' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstHyperThermalConvector(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(MaterialsAlloy.HS188A.getBlock(1));
            case 'B' -> List.of(MaterialsAlloy.QUANTUM.getBlock(1));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings10, 1, 11));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 1));
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14));
            case 'F' -> List.of(new ItemStack(GregTechAPI.sBlockCasings6, 1, 10));
            case 'G' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'H' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'I' -> List.of(new ItemStack(GregTechAPI.sBlockCasings9, 1, 1));
            case 'J' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Iridium, 1));
            case 'K' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.CosmicNeutronium, 1));
            case 'L' -> List.of(new ItemStack(Loaders.pressureResistantWalls, 1, 0));
            case 'M' -> List.of(new ItemStack(GregTechAPI.sBlockCasingsDyson, 1, 9));
            case 'N' -> List.of(new ItemStack(BlockLoader.chaosCasing02, 1, 2));
            case 'O' -> List.of(new ItemStack(ModBlocks.blockCasings5Misc, 1, 2));
            case 'P' -> List.of(new ItemStack(BlockLoader.chaosCasing02, 1, 4));
            case 'Q' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 8));
            case 'R' -> List.of(new ItemStack(BlockLoader.chaosCasing02, 1, 2));
            case 'S' -> List.of(new ItemStack(BlockLoader.chaosCasing02, 1, 2));
            case 'T' -> List.of(new ItemStack(BlockLoader.chaosCasing02, 1, 2));
            case 'U' -> List.of(new ItemStack(BlockLoader.chaosCasing02, 1, 2));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstMegaMacerator(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 0));
            case 'b' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 0));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 8));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 3));
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'F' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'G' -> join(
                List.of(new ItemStack(GregTechAPI.sBlockMetal2, 1, 9)),
                List.of(new ItemStack(GregTechAPI.sBlockMetal5, 1, 2)),
                List.of(new ItemStack(GregTechAPI.sBlockMetal9, 1, 8)));
            case 'H' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1));
            case 'I' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.CosmicNeutronium, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstScavenger(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'C' -> join(glasses(-1), List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14)));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 14));
            case 'E' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Osmiridium, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstSwelegfyrBlastFurnace(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(-1);
            case 'a' -> List.of();
            case 'B' -> List.of(ItemList.FenceIron.get(1));
            case 'C' -> List.of(new ItemStack(Loaders.compactFusionCoil, 1, 0));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings1, 1, 11));
            case 'E' -> coils(controller, source);
            case 'F' -> List.of(new ItemStack(ModBlocks.blockCasings5Misc, 1, 4));
            case 'G' -> List.of(new ItemStack(BlockLoader.chaosCasing02, 1, 2));
            case 'H' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'h' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'I' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1));
            case 'J' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1));
            case 'K' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.CosmicNeutronium, 1));
            case 'L' -> List.of(new ItemStack(ModBlocks.blockCustomMachineCasings, 1, 3));
            case 'M' -> List.of(new ItemStack(ModBlocks.blockCasingsMisc, 1, 14));
            case 'N' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'O' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'P' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'S' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'Z' -> List.of();
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstThermalEnergyDevourer(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(Loaders.MAR_Casing, 1, 0));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings1, 1, 11));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 1));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 8));
            case 'E' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 3));
            case 'F' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsTstVacuumFilterExtractor(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 8));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 10));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 3));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings9, 1, 0));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 8));
            case 'F' -> List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0));
            case 'G' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsMmIndistinctTentaclePrototypeMk2(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> glasses(12);
            case 'B' -> join(
                List.of(new ItemStack(Loaders.componentAssemblylineCasing, 1, 12)),
                List.of(new ItemStack(Loaders.componentAssemblylineCasing, 1, 13)));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings2, 1, 9));
            case 'D' -> List.of(new ItemStack(GregTechAPI.sBlockCasings9, 1, 1));
            case 'E' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsBA0, 1, 12));
            case 'F' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 7));
            case 'G' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 9));
            case 'H' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 10));
            case 'I' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            case 'J' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 13));
            case 'K' -> join(
                List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 13)));
            case 'L' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            case 'M' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 14));
            case 'N' -> join(
                List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0)),
                List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 13)));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsMmDimensionallyTranscendentMatterPlasmaForgePrototypeMk2(char symbol,
        ItemStack controller, BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> coils(controller, source);
            case 'B' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsBA0, 1, 12));
            case 'C' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 10));
            case 'D' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            case 'E' -> List.of(new ItemStack(BlockQuantumGlass.INSTANCE, 1, 0));
            case 'F' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 12));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static List<ItemStack> materialsMmLargeNeutronOscillator(char symbol, ItemStack controller,
        BottledChaosSupportedMachine source) {
        return switch (symbol) {
            case 'A' -> List.of(new ItemStack(GregTechAPI.sBlockCasings4, 1, 1));
            case 'B' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 7));
            case 'C' -> List.of(new ItemStack(GregTechAPI.sBlockCasings8, 1, 10));
            case 'D' -> List.of(new ItemStack(TTCasingsContainer.sBlockCasingsTT, 1, 0));
            case 'E' -> List.of(new ItemStack(Loaders.speedingPipe, 1, 0));
            case 'F' -> glasses(-1);
            case 'G' -> List.of(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1));
            default -> throw new IllegalArgumentException(
                "Unknown source structure symbol " + symbol + " for " + source);
        };
    }

    private static String name(BottledChaosSupportedMachine source) {
        return switch (source) {
            case GTCM_CRYSTALLINE_INFINITIER -> {
                // #tr gtnr.chaos.source.gtcm_crystalline_infinitier
                // # Crystalline Infinitier
                // # zh_CN 无限晶胞
                yield StatCollector.translateToLocal("gtnr.chaos.source.gtcm_crystalline_infinitier");
            }
            case GT_TILE_ENTITY_HOLY_SEPARATOR -> {
                // #tr gtnr.chaos.source.gt_tile_entity_holy_separator
                // # Holy Separator
                // # zh_CN 神圣分离者
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_holy_separator");
            }
            case GT_TILE_ENTITY_INTENSIFY_CHEMICAL_DISTORTER -> {
                // #tr gtnr.chaos.source.gt_tile_entity_intensify_chemical_distorter
                // # Intensify Chemical Distorter
                // # zh_CN 深度化学扭曲仪
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_intensify_chemical_distorter");
            }
            case GT_TILE_ENTITY_MAGNETIC_DOMAIN_CONSTRUCTOR -> {
                // #tr gtnr.chaos.source.gt_tile_entity_magnetic_domain_constructor
                // # Magnetic Domain Constructor
                // # zh_CN 磁畴构建器
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_magnetic_domain_constructor");
            }
            case GT_TILE_ENTITY_MAGNETIC_DRIVE_PRESSURE_FORMER -> {
                // #tr gtnr.chaos.source.gt_tile_entity_magnetic_drive_pressure_former
                // # Magnetic Drive Pressure Former
                // # zh_CN 磁驱压力成型机
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_magnetic_drive_pressure_former");
            }
            case GT_TILE_ENTITY_MAGNETIC_MIXER -> {
                // #tr gtnr.chaos.source.gt_tile_entity_magnetic_mixer
                // # "Mini" Magnetic Mixer
                // # zh_CN "小型"磁力搅拌机
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_magnetic_mixer");
            }
            case GT_TILE_ENTITY_MOLECULE_DECONSTRUCTOR -> {
                // #tr gtnr.chaos.source.gt_tile_entity_molecule_deconstructor
                // # Molecule Deconstructor
                // # zh_CN 分子解构器
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_molecule_deconstructor");
            }
            case GT_TILE_ENTITY_PHYSICAL_FORM_SWITCHER -> {
                // #tr gtnr.chaos.source.gt_tile_entity_physical_form_switcher
                // # Physical Form Switcher
                // # zh_CN 物质形态转换器
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_physical_form_switcher");
            }
            case GT_TILE_ENTITY_PRECISE_HIGH_ENERGY_PHOTONIC_QUANTUM_MASTER -> {
                yield StatCollector
                    // #tr gtnr.chaos.source.gt_tile_entity_precise_high_energy_photonic_quantum_master
                    // # Precise High-Energy Photonic Quantum Master
                    // # zh_CN 精密高能光量子掌控者
                    .translateToLocal("gtnr.chaos.source.gt_tile_entity_precise_high_energy_photonic_quantum_master");
            }
            case GT_TILE_ENTITY_SPACE_SCALER -> {
                // #tr gtnr.chaos.source.gt_tile_entity_space_scaler
                // # Space Scaler
                // # zh_CN 空间缩放仪
                yield StatCollector.translateToLocal("gtnr.chaos.source.gt_tile_entity_space_scaler");
            }
            case TST_ADVANCED_MEGA_OIL_CRACKER -> {
                // #tr gtnr.chaos.source.tst_advanced_mega_oil_cracker
                // # Advanced Mega Oil Cracker
                // # zh_CN 进阶巨型石油裂化机
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_advanced_mega_oil_cracker");
            }
            case TST_BALL_LIGHTNING -> {
                // #tr gtnr.chaos.source.tst_ball_lightning
                // # BallLightning
                // # zh_CN 球状闪电
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_ball_lightning");
            }
            case TST_BIOSPHERE_III -> {
                // #tr gtnr.chaos.source.tst_biosphere_iii
                // # Biosphere III
                // # zh_CN 生物圈III号
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_biosphere_iii");
            }
            case TST_CORE_DEVICE_OF_HUMAN_POWER_GENERATION_FACILITY -> {
                yield StatCollector
                    // #tr gtnr.chaos.source.tst_core_device_of_human_power_generation_facility
                    // # Core Device of Human Power Generation Facility
                    // # zh_CN 人类能源设施的核心装置
                    .translateToLocal("gtnr.chaos.source.tst_core_device_of_human_power_generation_facility");
            }
            case TST_GIANT_VACUUM_DRYING_FURNACE -> {
                // #tr gtnr.chaos.source.tst_giant_vacuum_drying_furnace
                // # Giant Vacuum Drying Furnace
                // # zh_CN 巨型真空干燥炉
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_giant_vacuum_drying_furnace");
            }
            case TST_HEPHAESTUS_ATELIER -> {
                // #tr gtnr.chaos.source.tst_hephaestus_atelier
                // # Hephaestus' Atelier
                // # zh_CN 赫菲斯托斯的工坊
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_hephaestus_atelier");
            }
            case TST_INDISTINCT_TENTACLE -> {
                // #tr gtnr.chaos.source.tst_indistinct_tentacle
                // # §l§8Indistinct Tentacle
                // # zh_CN §8§l不可视之触§r
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_indistinct_tentacle");
            }
            case TST_INDUSTRIAL_MAGNETAR_SEPARATOR -> {
                // #tr gtnr.chaos.source.tst_industrial_magnetar_separator
                // # Industrial Magnetar Separator
                // # zh_CN 工业电磁离析机
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_industrial_magnetar_separator");
            }
            case TST_MANUFACTURING_CENTER -> {
                // #tr gtnr.chaos.source.tst_manufacturing_center
                // # Manufacturing Center
                // # zh_CN 加工中心
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_manufacturing_center");
            }
            case TST_MIRACLE_DOOR -> {
                // #tr gtnr.chaos.source.tst_miracle_door
                // # Miracle Door
                // # zh_CN 奇迹之门
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_miracle_door");
            }
            case TST_HYPER_THERMAL_CONVECTOR -> {
                // #tr gtnr.chaos.source.tst_hyper_thermal_convector
                // # Hyper Thermal Convector
                // # zh_CN 高能态热对流器
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_hyper_thermal_convector");
            }
            case TST_MEGA_MACERATOR -> {
                // #tr gtnr.chaos.source.tst_mega_macerator
                // # "Mini" Household Cell Fragmentizer
                // # zh_CN "小型"家用破壁机
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_mega_macerator");
            }
            case TST_SCAVENGER -> {
                // #tr gtnr.chaos.source.tst_scavenger
                // # Scavenger
                // # zh_CN 拾荒者
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_scavenger");
            }
            case TST_SWELEGFYR_BLAST_FURNACE -> {
                // #tr gtnr.chaos.source.tst_swelegfyr_blast_furnace
                // # Swelegfyr Blast Furnace
                // # zh_CN 熯焱高炉
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_swelegfyr_blast_furnace");
            }
            case TST_THERMAL_ENERGY_DEVOURER -> {
                // #tr gtnr.chaos.source.tst_thermal_energy_devourer
                // # Thermal Energy Devourer
                // # zh_CN 热能饕餮
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_thermal_energy_devourer");
            }
            case TST_VACUUM_FILTER_EXTRACTOR -> {
                // #tr gtnr.chaos.source.tst_vacuum_filter_extractor
                // # Vacuum Filter Extractor
                // # zh_CN 真空抽滤器
                yield StatCollector.translateToLocal("gtnr.chaos.source.tst_vacuum_filter_extractor");
            }
            case MM_INDISTINCT_TENTACLE_PROTOTYPE_MK2 -> {
                // #tr gtnr.chaos.source.mm_indistinct_tentacle_prototype_mk2
                // # §8§lIndistinct Tentacle §rPrototype MK-II
                // # zh_CN §8§l不可视之触§r原型机MK-II
                yield StatCollector.translateToLocal("gtnr.chaos.source.mm_indistinct_tentacle_prototype_mk2");
            }
            case MM_DIMENSIONALLY_TRANSCENDENT_MATTER_PLASMA_FORGE_PROTOTYPE_MK2 -> {
                yield StatCollector.translateToLocal(
                    // #tr gtnr.chaos.source.mm_dimensionally_transcendent_matter_plasma_forge_prototype_mk2
                    // # Dimensionally Transcendent Matter Plasma Forge Prototype MK-II
                    // # zh_CN 超维度物质等离子锻炉原型机MK-II
                    "gtnr.chaos.source.mm_dimensionally_transcendent_matter_plasma_forge_prototype_mk2");
            }
            case MM_LARGE_NEUTRON_OSCILLATOR -> {
                // #tr gtnr.chaos.source.mm_large_neutron_oscillator
                // # Large Neutron Oscillator
                // # zh_CN 大型中子振荡器
                yield StatCollector.translateToLocal("gtnr.chaos.source.mm_large_neutron_oscillator");
            }
            default -> source.name();
        };
    }

    private record Position(int x, int y, int z) {}

    @FunctionalInterface
    private interface MaterialChoices {

        List<ItemStack> get(char symbol, ItemStack controller, BottledChaosSupportedMachine source);
    }

    private record Definition(BottledChaosSupportedMachine source, BottledChaosSupportedMachine controllerSource,
        int poolCount, List<RecipeMap<?>> pools, Map<String, String> pieces, MaterialChoices materials) {}
}
