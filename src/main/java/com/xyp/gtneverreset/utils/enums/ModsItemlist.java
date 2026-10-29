package com.xyp.gtneverreset.utils.enums;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.MetaTileEntityIDs;
import gregtech.api.enums.Mods;
import gregtech.api.util.GTModHandler;
import ic2.api.item.IC2Items;

public enum ModsItemlist {

    QuantumBridgeCard(Mods.AE2FluidCraft.ID, "quantum_bridge_card", true),

    EnergyCard(Mods.AE2FluidCraft.ID, "energy_card", true),

    FluidInterfaceBlock(Mods.AE2FluidCraft.ID, "fluid_interface", 0),

    FluidStorageComponent256k(Mods.AE2FluidCraft.ID, "fluid_part", 4),

    FluidStorageComponent4096k(Mods.AE2FluidCraft.ID, "fluid_part", 6),

    FluidStorageComponent16384k(Mods.AE2FluidCraft.ID, "fluid_part", 7),

    UniverseFluidStorageCell(Mods.AE2FluidCraft.ID, "fluid_storage.Universe", 0),

    AdvancedFluidStorageHousing(Mods.AE2FluidCraft.ID, "fluid_storage_housing", 3),

    FluidInterfacePart(Mods.AE2FluidCraft.ID, "part_fluid_interface", 0),

    FluidStorageBus(Mods.AE2FluidCraft.ID, "part_fluid_storage_bus", 0),

    InfinityBoosterCard(Mods.AE2WCT.ID, "infinityBoosterCard", true),

    CalculationPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 13),

    EngineeringPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 14),

    LogicPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 15),

    SiliconPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 19),

    MEInterface(Mods.AppliedEnergistics2.ID, "tile.BlockInterface"),

    SkyStone(Mods.AppliedEnergistics2.ID, "tile.BlockSkyStone", 0),

    AkashicRecord(Mods.Avaritia.ID, "Akashic_Record", 0),

    CosmicMeatballs(Mods.Avaritia.ID, "Cosmic_Meatballs", 0),

    CrystalMatrix(Mods.Avaritia.ID, "Crystal_Matrix", 0),

    EndestPearl(Mods.Avaritia.ID, "Endest_Pearl", 0),

    InfinityChestplate(Mods.Avaritia.ID, "Infinity_Chest", 0),

    InfinityHelmet(Mods.Avaritia.ID, "Infinity_Helm", 0),

    InfinityLeggings(Mods.Avaritia.ID, "Infinity_Pants", 0),

    InfinityPickaxe(Mods.Avaritia.ID, "Infinity_Pickaxe", 0),

    InfinityBoots(Mods.Avaritia.ID, "Infinity_Shoes", 0),

    InfinitySword(Mods.Avaritia.ID, "Infinity_Sword", 0),

    OrbArmok(Mods.Avaritia.ID, "Orb_Armok", 0),

    DiamondLattice(Mods.Avaritia.ID, "Resource", 0),

    CrystalMatrixIngot(Mods.Avaritia.ID, "Resource", 1),

    InfinityCatalyst(Mods.Avaritia.ID, "Resource", 5),

    InfinityIngot(Mods.Avaritia.ID, "Resource", 6),

    RecordFragment(Mods.Avaritia.ID, "Resource", 7),

    StellarFuel(Mods.Avaritia.ID, "Resource", 8),

    IronSingularity(Mods.Avaritia.ID, "Singularity", 0),

    GoldSingularity(Mods.Avaritia.ID, "Singularity", 1),

    EnderiumSingularity(Mods.Avaritia.ID, "Singularity", 10),

    ClaySingularity(Mods.Avaritia.ID, "Singularity", 11),

    LapisSingularity(Mods.Avaritia.ID, "Singularity", 2),

    RedstoneSingularity(Mods.Avaritia.ID, "Singularity", 3),

    QuartzSingularity(Mods.Avaritia.ID, "Singularity", 4),

    CopperSingularity(Mods.Avaritia.ID, "Singularity", 5),

    TinSingularity(Mods.Avaritia.ID, "Singularity", 6),

    LeadSingularity(Mods.Avaritia.ID, "Singularity", 7),

    SilverSingularity(Mods.Avaritia.ID, "Singularity", 8),

    NickelSingularity(Mods.Avaritia.ID, "Singularity", 9),

    UltimateStew(Mods.Avaritia.ID, "Ultimate_Stew", 0),

    ClusterOpener(Mods.Avaritia.ID, "cluster_opener", 0),

    CompressedChest(Mods.AvaritiaAddons.ID, "CompressedChest", 0),

    ExtremeAutoCrafter(Mods.AvaritiaAddons.ID, "ExtremeAutoCrafter", 0),

    InfinityChest(Mods.AvaritiaAddons.ID, "InfinityChest", 0),

    BorosilicateGlass(Mods.BartWorks.ID, "BW_TieredGlass", 0),

    PineLeaves(Mods.BiomesOPlenty.ID, "colorizedLeaves2", 1),

    PineSapling(Mods.BiomesOPlenty.ID, "colorizedSaplings", 5),

    PinkCoral(Mods.BiomesOPlenty.ID, "coral1", 12),

    OrangeCoral(Mods.BiomesOPlenty.ID, "coral1", 13),

    BlueCoral(Mods.BiomesOPlenty.ID, "coral1", 14),

    GlowingCoral(Mods.BiomesOPlenty.ID, "coral1", 15),

    LargeLilyPad(Mods.BiomesOPlenty.ID, "lilyBop", 0),

    MediumLilyPad(Mods.BiomesOPlenty.ID, "lilyBop", 1),

    SmallLilyPad(Mods.BiomesOPlenty.ID, "lilyBop", 2),

    BiomesOPlentyPineLog(Mods.BiomesOPlenty.ID, "logs4", 0),

    PineCone(Mods.BiomesOPlenty.ID, "misc", 13),

    BloodTnt(Mods.BloodArsenal.ID, "blood_tnt", 0),

    LifeInfuser(Mods.BloodArsenal.ID, "life_infuser", 0),

    LpMaterializer(Mods.BloodArsenal.ID, "lp_materializer", 0),

    BloodAltar(Mods.BloodMagic.ID, "Altar", 0),

    AwakenedActivationCrystal(Mods.BloodMagic.ID, "activationCrystal", 1),

    ApprenticeBloodOrb(Mods.BloodMagic.ID, "apprenticeBloodOrb", 0),

    ArchmageBloodOrb(Mods.BloodMagic.ID, "archmageBloodOrb", 0),

    AlchemyTable(Mods.BloodMagic.ID, "blockWritingTable", 0),

    CreativeBloodOrb(Mods.BloodMagic.ID, "creativeFiller", 0),

    DaggerOfSacrifice(Mods.BloodMagic.ID, "daggerOfSacrifice", 0),

    RitualDiviner(Mods.BloodMagic.ID, "itemRitualDiviner", 2),

    MagicianBloodOrb(Mods.BloodMagic.ID, "magicianBloodOrb", 0),

    MasterBloodOrb(Mods.BloodMagic.ID, "masterBloodOrb", 0),

    MasterRitualStone(Mods.BloodMagic.ID, "masterStone", 0),

    TranscendentBloodOrb(Mods.BloodMagic.ID, "transcendentBloodOrb", 0),

    WeakBloodOrb(Mods.BloodMagic.ID, "weakBloodOrb", 0),

    ManasteelIngot(Mods.Botania.ID, "manaResource", 0),

    ManaPearl(Mods.Botania.ID, "manaResource", 1),

    ManaDiamond(Mods.Botania.ID, "manaResource", 2),

    AlchemyCatalyst(Mods.Botania.ID, "alchemyCatalyst", 0),

    AlfheimPortal(Mods.Botania.ID, "alfheimPortal", 0),

    WillOfAhrim(Mods.Botania.ID, "ancientWill", 0),

    WillOfDharok(Mods.Botania.ID, "ancientWill", 1),

    WillOfGuthan(Mods.Botania.ID, "ancientWill", 2),

    WillOfTorag(Mods.Botania.ID, "ancientWill", 3),

    WillOfVerac(Mods.Botania.ID, "ancientWill", 4),

    WillOfKaril(Mods.Botania.ID, "ancientWill", 5),

    BifrostBlock(Mods.Botania.ID, "bifrostPerm", 0),

    BifrostPane(Mods.Botania.ID, "bifrostPermPane", 0),

    BlackLotus(Mods.Botania.ID, "blackLotus", 0),

    BlackestLotus(Mods.Botania.ID, "blackLotus", 1),

    ConjurationCatalyst(Mods.Botania.ID, "conjurationCatalyst", 0),

    CorporeaSpark(Mods.Botania.ID, "corporeaSpark", 0),

    MasterCorporeaSpark(Mods.Botania.ID, "corporeaSpark", 1),

    Dice(Mods.Botania.ID, "dice", 0),

    ElfGlass(Mods.Botania.ID, "elfGlass", 0),

    GaiaHead(Mods.Botania.ID, "gaiaHead", 0),

    WarpLens(Mods.Botania.ID, "lens", 18),

    BounceLens(Mods.Botania.ID, "lens", 5),

    LexicaBotania(Mods.Botania.ID, "lexicon", 0),

    MagnetRing(Mods.Botania.ID, "magnetRing", 0),

    ManaBeacon(Mods.Botania.ID, "manaBeacon", 0),

    GaiaSpiritIngot(Mods.Botania.ID, "manaResource", 14),

    EnderAirBottle(Mods.Botania.ID, "manaResource", 15),

    TerrasteelIngot(Mods.Botania.ID, "manaResource", 4),

    GaiaSpirit(Mods.Botania.ID, "manaResource", 5),

    ElementiumIngot(Mods.Botania.ID, "manaResource", 7),

    PixieDust(Mods.Botania.ID, "manaResource", 8),

    Dragonstone(Mods.Botania.ID, "manaResource", 9),

    OvergrowthSeed(Mods.Botania.ID, "overgrowthSeed", 0),

    Pinkinator(Mods.Botania.ID, "pinkinator", 0),

    PistonRelay(Mods.Botania.ID, "pistonRelay", 0),

    EverlastingManaPool(Mods.Botania.ID, "pool", 1),

    FabulousManaPool(Mods.Botania.ID, "pool", 3),

    Pump(Mods.Botania.ID, "pump", 0),

    ManaPylon(Mods.Botania.ID, "pylon", 0),

    NaturaPylon(Mods.Botania.ID, "pylon", 1),

    GaiaPylon(Mods.Botania.ID, "pylon", 2),

    ReachRing(Mods.Botania.ID, "reachRing", 0),

    ScathedMusicDisc(Mods.Botania.ID, "recordGaia2", 0),

    RfGenerator(Mods.Botania.ID, "rfGenerator", 0),

    WaterRune(Mods.Botania.ID, "rune", 0),

    FireRune(Mods.Botania.ID, "rune", 1),

    GluttonyRune(Mods.Botania.ID, "rune", 10),

    GreedRune(Mods.Botania.ID, "rune", 11),

    SlothRune(Mods.Botania.ID, "rune", 12),

    WrathRune(Mods.Botania.ID, "rune", 13),

    EnvyRune(Mods.Botania.ID, "rune", 14),

    PrideRune(Mods.Botania.ID, "rune", 15),

    EarthRune(Mods.Botania.ID, "rune", 2),

    AirRune(Mods.Botania.ID, "rune", 3),

    SpringRune(Mods.Botania.ID, "rune", 4),

    SummerRune(Mods.Botania.ID, "rune", 5),

    AutumnRune(Mods.Botania.ID, "rune", 6),

    WinterRune(Mods.Botania.ID, "rune", 7),

    ManaRune(Mods.Botania.ID, "rune", 8),

    LustRune(Mods.Botania.ID, "rune", 9),

    RunicAltar(Mods.Botania.ID, "runeAltar", 0),

    Spark(Mods.Botania.ID, "spark", 0),

    SpecialFlower(Mods.Botania.ID, "specialFlower", 0),

    GaiaManaSpreader(Mods.Botania.ID, "spreader", 3),

    Andesite(Mods.Botania.ID, "stone", 0),

    Basalt(Mods.Botania.ID, "stone", 1),

    Diorite(Mods.Botania.ID, "stone", 2),

    Granite(Mods.Botania.ID, "stone", 3),

    ManasteelBlock(Mods.Botania.ID, "storage", 0),

    TerraShatterer(Mods.Botania.ID, "terraPick", 0),

    TerrestrialAgglomerationPlate(Mods.Botania.ID, "terraPlate", 0),

    TankBlock(Mods.BuildCraftFactory.ID, "tankBlock", 0),

    DiamondChisel(Mods.Chisel.ID, "diamondChisel", 0),
    ChiselYellowHempcrete(Mods.Chisel.ID, "hempcrete", 4),
    ChiselLightGrayHempcrete(Mods.Chisel.ID, "hempcrete", 8),
    ChiselBlackHempcrete(Mods.Chisel.ID, "hempcrete", 15),

    CreativeMemory(Mods.Computronics.ID, "computronics.ocSpecialParts", 0),

    AwakenedCore(Mods.DraconicEvolution.ID, "awakenedCore", 0),

    SmallChaosFragment(Mods.DraconicEvolution.ID, "chaosFragment", 1),

    ChaosShard(Mods.DraconicEvolution.ID, "chaosShard", 0),

    ChaoticCore(Mods.DraconicEvolution.ID, "chaoticCore", 0),

    DezilsMarshmallow(Mods.DraconicEvolution.ID, "dezilsMarshmallow", 0),

    DraconicCore(Mods.DraconicEvolution.ID, "draconicCore", 0),

    DraconiumBlock(Mods.DraconicEvolution.ID, "draconium", 0),

    ChargedDraconiumBlock(Mods.DraconicEvolution.ID, "draconium", 2),

    DragonHeart(Mods.DraconicEvolution.ID, "dragonHeart", 0),

    WyvernItemDislocator(Mods.DraconicEvolution.ID, "magnet", 0),

    AwakenedItemDislocator(Mods.DraconicEvolution.ID, "magnet", 1),

    ReactorCore(Mods.DraconicEvolution.ID, "reactorCore", 0),

    TeleporterMKII(Mods.DraconicEvolution.ID, "teleporterMKII", 0),

    ChaosInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars4", 14),

    AirInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars4", 15),

    WaterInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars5", 1),

    FireInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars5", 2),

    PotentiaEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 0),

    FireEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 1),

    VitalEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 2),

    WoodEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 3),

    AirEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 4),

    GreedEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 5),

    CapacitorBank(Mods.EnderIO.ID, "blockCapBank", 0),

    DarkSteelAnvil(Mods.EnderIO.ID, "blockDarkSteelAnvil", 0),

    EndermanSkull(Mods.EnderIO.ID, "blockEndermanSkull", 0),

    TormentedEndermanHead(Mods.EnderIO.ID, "blockEndermanSkull", 2),

    FarmingStation(Mods.EnderIO.ID, "blockFarmStation", 0),

    RedstoneAlloyBlock(Mods.EnderIO.ID, "blockIngotStorage", 3),

    DarkSteelBlock(Mods.EnderIO.ID, "blockIngotStorage", 6),

    PoweredSpawner(Mods.EnderIO.ID, "blockPoweredSpawner", 0),

    PhotovoltaicCell(Mods.EnderIO.ID, "blockSolarPanel", 0),

    AdvancedPhotovoltaicCell(Mods.EnderIO.ID, "blockSolarPanel", 1),

    VibrantPhotovoltaicCell(Mods.EnderIO.ID, "blockSolarPanel", 2),

    BasicCapacitor(Mods.EnderIO.ID, "itemBasicCapacitor", 0),

    ZombieElectrode(Mods.EnderIO.ID, "itemFrankenSkull", 0),

    ZLogicController(Mods.EnderIO.ID, "itemFrankenSkull", 1),

    ArtificialZombie(Mods.EnderIO.ID, "itemFrankenSkull", 2),

    EnderResonator(Mods.EnderIO.ID, "itemFrankenSkull", 3),

    SentientEnderResonator(Mods.EnderIO.ID, "itemFrankenSkull", 4),

    GuardianDiode(Mods.EnderIO.ID, "itemFrankenSkull", 6),

    PrescientCrystal(Mods.EnderIO.ID, "itemMaterial", 13),

    VibrantCrystalPowder(Mods.EnderIO.ID, "itemMaterial", 14),

    EnderCrystalPowder(Mods.EnderIO.ID, "itemMaterial", 16),

    PrescientCrystalPowder(Mods.EnderIO.ID, "itemMaterial", 17),

    VibrantCrystal(Mods.EnderIO.ID, "itemMaterial", 5),

    EnderCrystal(Mods.EnderIO.ID, "itemMaterial", 8),

    AttractorCrystal(Mods.EnderIO.ID, "itemMaterial", 9),

    PowerConduit(Mods.EnderIO.ID, "itemPowerConduit", 0),

    EnderChest(Mods.EnderStorage.ID, "enderChest", 0),

    EnderTank(Mods.EnderStorage.ID, "enderChest", 1),

    CombinedSingularity(Mods.EternalSingularity.ID, "combined_singularity", 0),

    HolySingularity(Mods.EternalSingularity.ID, "combined_singularity", 1),

    VoidSingularity(Mods.EternalSingularity.ID, "combined_singularity", 15),

    SpaghettiSingularity(Mods.EternalSingularity.ID, "combined_singularity", 2),

    AtmosphericSingularity(Mods.EternalSingularity.ID, "combined_singularity", 3),

    MysticSingularity(Mods.EternalSingularity.ID, "combined_singularity", 4),

    EpicSingularity(Mods.EternalSingularity.ID, "combined_singularity", 5),

    AstralSingularity(Mods.EternalSingularity.ID, "combined_singularity", 6),

    EternalSingularity(Mods.EternalSingularity.ID, "eternal_singularity", 0),

    EtFuturumRequiemOrangeConcrete(Mods.EtFuturumRequiem.ID, "concrete", 1),
    EtFuturumRequiemBrownConcrete(Mods.EtFuturumRequiem.ID, "concrete", 12),

    Blackstone(Mods.EtFuturumRequiem.ID, "blackstone", 0),

    BlueIce(Mods.EtFuturumRequiem.ID, "blue_ice", 0),

    CobbledDeepslate(Mods.EtFuturumRequiem.ID, "cobbled_deepslate", 0),

    Deepslate(Mods.EtFuturumRequiem.ID, "deepslate", 0),

    Elytra(Mods.EtFuturumRequiem.ID, "elytra", 0),

    PlaceableEndCrystal(Mods.EtFuturumRequiem.ID, "end_crystal", 0),

    MagmaBlock(Mods.EtFuturumRequiem.ID, "magma", 0),

    NetheriteAxe(Mods.EtFuturumRequiem.ID, "netherite_axe", 0),

    NetheriteBoots(Mods.EtFuturumRequiem.ID, "netherite_boots", 0),

    NetheriteChestplate(Mods.EtFuturumRequiem.ID, "netherite_chestplate", 0),

    NetheriteHelmet(Mods.EtFuturumRequiem.ID, "netherite_helmet", 0),

    NetheriteHoe(Mods.EtFuturumRequiem.ID, "netherite_hoe", 0),

    NetheriteLeggings(Mods.EtFuturumRequiem.ID, "netherite_leggings", 0),

    NetheritePickaxe(Mods.EtFuturumRequiem.ID, "netherite_pickaxe", 0),

    NetheriteScrap(Mods.EtFuturumRequiem.ID, "netherite_scrap", 0),

    NetheriteSpade(Mods.EtFuturumRequiem.ID, "netherite_spade", 0),

    NetheriteSword(Mods.EtFuturumRequiem.ID, "netherite_sword", 0),

    ShulkerShell(Mods.EtFuturumRequiem.ID, "shulker_shell", 0),

    Slime(Mods.EtFuturumRequiem.ID, "slime", 0),

    SoulTorch(Mods.EtFuturumRequiem.ID, "soul_torch", 0),

    EtFuturumRequiemSponge(Mods.EtFuturumRequiem.ID, "sponge", 0),

    WetSponge(Mods.EtFuturumRequiem.ID, "sponge", 1),

    TotemOfUndying(Mods.EtFuturumRequiem.ID, "totem_of_undying", 0),

    CherrySapling(Mods.EtFuturumRequiem.ID, "sapling", 1),

    MangrovePropagule(Mods.EtFuturumRequiem.ID, "sapling", 0),

    Honeycomb(Mods.EtFuturumRequiem.ID, "honeycomb", 0),

    SoulSoil(Mods.EtFuturumRequiem.ID, "soul_soil", 0),

    AncientDebris(Mods.EtFuturumRequiem.ID, "ancient_debris", 0),

    CrimsonFungus(Mods.EtFuturumRequiem.ID, "nether_fungus", 0),

    WarpedFungus(Mods.EtFuturumRequiem.ID, "nether_fungus", 1),

    DragonBreath(Mods.EtFuturumRequiem.ID, "dragon_breath", 0),

    ChorusFruit(Mods.EtFuturumRequiem.ID, "chorus_fruit", 0),

    ChorusFlower(Mods.EtFuturumRequiem.ID, "chorus_flower", 0),

    ExtraBeesHive(ModList.ExtraBees.getID(), "hive"),

    CompressedCobbleSeven(Mods.ExtraUtilities.ID, "cobblestone_compressed", 6),

    CompressedCobbleEight(Mods.ExtraUtilities.ID, "cobblestone_compressed", 7),

    DarkPortal(Mods.ExtraUtilities.ID, "dark_portal", 0),

    UnstableIngotBlock(Mods.ExtraUtilities.ID, "decorativeBlock1", 5),

    GlowstoneGlass(Mods.ExtraUtilities.ID, "decorativeBlock2", 7),

    DiamondSpike(Mods.ExtraUtilities.ID, "spike_base_diamond", 0),

    FluidTrashCan(Mods.ExtraUtilities.ID, "trashcan", 1),

    UnstableIngot(Mods.ExtraUtilities.ID, "unstableingot", 0),

    EldritchOrb(Mods.ForbiddenMagic.ID, "EldritchOrb", 0),

    ForestryHive(ModList.Forestry.getID(), "beehives"),

    Alveary(Mods.Forestry.ID, "alveary", 0),

    AlvearySwarmer(Mods.Forestry.ID, "alveary", 2),

    AlvearyStabilizer(Mods.Forestry.ID, "alveary", 6),

    Apiary(Mods.Forestry.ID, "apiculture", 0),

    Beeswax(Mods.Forestry.ID, "beeswax", 0),

    FarmGearbox(Mods.Forestry.ID, "ffarm", 2),

    FarmValve(Mods.Forestry.ID, "ffarm", 4),

    FarmControl(Mods.Forestry.ID, "ffarm", 5),

    Leaves(Mods.Forestry.ID, "leaves", 0),

    ForestryPineLog(Mods.Forestry.ID, "logs", 20),

    Pollen(Mods.Forestry.ID, "pollen", 0),

    ForestryRoyalJelly(Mods.Forestry.ID, "royalJelly", 0),

    EtherealFamiliar(Mods.Gadomancy.ID, "ItemEtherealFamiliar", 0),

    LightweightAlloyPlate(Mods.GalacticraftAmunRa.ID, "item.baseItem", 15),

    DarkMatterFragment(Mods.GalacticraftAmunRa.ID, "item.baseItem", 26),

    ShuttleSchematic(Mods.GalacticraftAmunRa.ID, "item.schematic", 0),

    DarkMatter(Mods.GalacticraftAmunRa.ID, "tile.baseBlockRock", 14),

    AdvancedWafer(Mods.GalacticraftCore.ID, "item.basicItem", 14),

    InfiniteOxygenTank(Mods.GalacticraftCore.ID, "item.infiniteOxygen", 0),

    Tier2RocketSchematic(Mods.GalacticraftCore.ID, "item.schematic", 1),

    Tier1Rocket(Mods.GalacticraftCore.ID, "item.spaceship", 0),

    NasaWorkbench(Mods.GalacticraftCore.ID, "tile.rocketWorkbench", 0),

    MoonStone(Mods.GalacticraftCore.ID, "tile.moonBlock", 4),

    Tier3Rocket(Mods.GalacticraftMars.ID, "item.itemTier3Rocket", 0),

    Tier3RocketSchematic(Mods.GalacticraftMars.ID, "item.schematic", 0),

    Tier2Rocket(Mods.GalacticraftMars.ID, "item.spaceshipTier2", 0),

    MarsStone(Mods.GalacticraftMars.ID, "tile.mars", 9),

    BarnardaCLog(Mods.GalaxySpace.ID, "barnardaClog", 0),

    BarnardaCLeaves(Mods.GalaxySpace.ID, "barnardaCleaves", 0),

    VenusRock(Mods.GalaxySpace.ID, "venusblocks", 1),

    MercuryRock(Mods.GalaxySpace.ID, "mercuryblocks", 1),

    CeresRock(Mods.GalaxySpace.ID, "ceresblocks", 1),

    IoStone(Mods.GalaxySpace.ID, "ioblocks", 2),

    GanymedeRock(Mods.GalaxySpace.ID, "ganymedeblocks", 1),

    PlutoStone(Mods.GalaxySpace.ID, "plutoblocks", 5),

    EnceladusRock(Mods.GalaxySpace.ID, "enceladusblocks", 1),

    TitanStone(Mods.GalaxySpace.ID, "titanblocks", 2),

    TritonStone(Mods.GalaxySpace.ID, "tritonblocks", 2),

    MysteriousCrystalShard(Mods.GalaxySpace.ID, "item.UnknowCrystal", 0),

    RocketControlComputerTier4(Mods.GalaxySpace.ID, "item.RocketControlComputer", 4),

    RocketControlComputerTier7(Mods.GalaxySpace.ID, "item.RocketControlComputer", 7),

    Tier4RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier4", 0),

    Tier5RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier5", 0),

    Tier6RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier6", 0),

    Tier7RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier7", 0),

    Tier8RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier8", 0),

    Tier4Rocket(Mods.GalaxySpace.ID, "item.Tier4Rocket", 0),

    Tier5Rocket(Mods.GalaxySpace.ID, "item.Tier5Rocket", 0),

    Tier6Rocket(Mods.GalaxySpace.ID, "item.Tier6Rocket", 0),

    Tier7Rocket(Mods.GalaxySpace.ID, "item.Tier7Rocket", 0),

    Tier8Rocket(Mods.GalaxySpace.ID, "item.Tier8Rocket", 0),

    CetiESeaweedFormI(Mods.GalaxySpace.ID, "tcetiedandelions", 0),

    CetiESeaweedFormII(Mods.GalaxySpace.ID, "tcetiedandelions", 1),

    CetiESeaweedFormIII(Mods.GalaxySpace.ID, "tcetiedandelions", 2),

    CetiESeaweedFormIV(Mods.GalaxySpace.ID, "tcetiedandelions", 3),

    CetiESeaweedFormV(Mods.GalaxySpace.ID, "tcetiedandelions", 4),

    CetiESeaweedFormVI(Mods.GalaxySpace.ID, "tcetiedandelions", 5),

    SingleUseFileMold(Mods.GGFab.ID, "gt.ggfab.d1", 30),

    SingleUseWrenchMold(Mods.GGFab.ID, "gt.ggfab.d1", 31),

    SingleUseCrowbarMold(Mods.GGFab.ID, "gt.ggfab.d1", 32),

    SingleUseWireCutterMold(Mods.GGFab.ID, "gt.ggfab.d1", 33),

    SingleUseHardHammerMold(Mods.GGFab.ID, "gt.ggfab.d1", 34),

    SingleUseSoftMalletMold(Mods.GGFab.ID, "gt.ggfab.d1", 35),

    SingleUseScrewdriverMold(Mods.GGFab.ID, "gt.ggfab.d1", 36),

    SingleUseSawMold(Mods.GGFab.ID, "gt.ggfab.d1", 37),

    RadiationProtectionPlate(Mods.GoodGenerator.ID, "radiationProtectionPlate"),

    JetEngine(Mods.GraviSuite.ID, "itemSimpleItem", 6),

    CircuitAssemblyLine(Mods.GregTech.ID, "gt.blockmachines", MetaTileEntityIDs.CircuitAssemblyLine.ID),

    RedGranite(Mods.GregTech.ID, "gt.blockgranites", 8),

    Marble(Mods.GregTech.ID, "gt.blockstones", 0),

    TstLowSpeedPerfectOverclockController(Mods.GregTech.ID, "gt.blockmachines", 18840),

    IridiumAlloyIngot(Mods.GregTech.ID, "gt.metaitem.01", 32480),

    DehydratorCoil(Mods.GTPlusPlus.ID, "itemDehydratorCoil", 3),

    BufferCoreUHV(Mods.GTPlusPlus.ID, "item.itemBufferCore10", 0),

    EndPowder(Mods.HardcoreEnderExpansion.ID, "end_powder", 0),

    EndPowderOre(Mods.HardcoreEnderExpansion.ID, "end_powder_ore", 0),

    LVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 3),

    MVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 4),

    HVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 5),

    EVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 6),

    MixedMetalIngot(Mods.IndustrialCraft2.ID, "mixedMetalIngot", false, true),

    ReinforcedIridiumPlate(Mods.IndustrialCraft2.ID, "iridiumPlate", false, true),

    NuclearReactor(Mods.IndustrialCraft2.ID, "blockGenerator", 5),

    IndustrialTnt(Mods.IndustrialCraft2.ID, "blockITNT", 0),

    IronFurnace(Mods.IndustrialCraft2.ID, "blockMachine", 1),

    Nuke(Mods.IndustrialCraft2.ID, "blockNuke", 0),

    ReactorChamber(Mods.IndustrialCraft2.ID, "blockReactorChamber", OreDictionary.WILDCARD_VALUE),

    RubberLog(Mods.IndustrialCraft2.ID, "blockRubWood", 0),

    EnergyCrystal(Mods.IndustrialCraft2.ID, "itemBatCrystal", 26),

    LapotronCrystal(Mods.IndustrialCraft2.ID, "itemBatLamaCrystal", 26),

    EmptyCell(Mods.IndustrialCraft2.ID, "itemCellEmpty", 13),

    StickyResin(Mods.IndustrialCraft2.ID, "itemHarz", 0),

    RawCarbonFibre(Mods.IndustrialCraft2.ID, "itemPartCarbonFibre", 0),

    ElectronicCircuit(Mods.IndustrialCraft2.ID, "itemPartCircuit", 0),

    AdvancedCircuit(Mods.IndustrialCraft2.ID, "itemPartCircuitAdv", 0),

    ThickNeutronReflector(Mods.IndustrialCraft2.ID, "reactorReflectorThick", 1),

    IronChest(Mods.IronChests.ID, "BlockIronChest", 0),

    GoldChest(Mods.IronChests.ID, "BlockIronChest", 1),

    DiamondChest(Mods.IronChests.ID, "BlockIronChest", 2),

    CopperChest(Mods.IronChests.ID, "BlockIronChest", 3),

    SteelChest(Mods.IronChests.ID, "BlockIronChest", 4),

    CrystalChest(Mods.IronChests.ID, "BlockIronChest", 5),

    ObsidianChest(Mods.IronChests.ID, "BlockIronChest", 6),

    NetheriteChest(Mods.IronChests.ID, "BlockIronChest", 8),

    DarkSteelChest(Mods.IronChests.ID, "BlockIronChest", 9),

    DiamondTank(Mods.IronTanks.ID, "diamondTank", 0),

    IfuBuildingKit(Mods.IWillFindYou.ID, "ifu_buildingKit", 0),

    LapotronicEnergyUnitBlock(Mods.KekzTech.ID, "kekztech_lapotronicenergyunit_block", 0),

    MagicBeesHive(ModList.MagicBees.getID(), "hive"),

    PiercingArrow(Mods.MineAndBladeBattleGear2.ID, "mb.arrow", 3),

    BlackCloud(Mods.Natura.ID, "Cloud", 1),

    GrayCloud(Mods.Natura.ID, "Cloud", 2),

    SulfurCloud(Mods.Natura.ID, "Cloud", 3),

    DimensionOverworld(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ow"),

    DimensionNether(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ne"),

    DimensionTwilight(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_TF"),

    DimensionEnd(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_ED"),

    DimensionEndAsteroids(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_EA"),

    DimensionEverglades(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Eg"),

    DimensionMoon(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Mo"),

    DimensionDeimos(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_De"),

    DimensionMars(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ma"),

    DimensionPhobos(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ph"),

    DimensionAsteroids(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_As"),

    DimensionCallisto(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ca"),

    DimensionCeres(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ce"),

    DimensionEuropa(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Eu"),

    DimensionGanymede(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ga"),

    DimensionRoss128b(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Rb"),

    DimensionIo(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Io"),

    DimensionMercury(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Me"),

    DimensionVenus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ve"),

    DimensionEnceladus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_En"),

    DimensionMiranda(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Mi"),

    DimensionOberon(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ob"),

    DimensionTitan(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ti"),

    DimensionRoss128ba(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ra"),

    DimensionProteus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Pr"),

    DimensionTriton(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Tr"),

    DimensionHaumea(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ha"),

    DimensionKuiperBelt(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_KB"),

    DimensionMakeMake(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_MM"),

    DimensionPluto(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Pl"),

    DimensionBarnardC(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_BC"),

    DimensionBarnardE(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_BE"),

    DimensionBarnardF(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_BF"),

    DimensionCentauriBb(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_CB"),

    DimensionTauCetiE(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_TE"),

    DimensionVegaB(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_VB"),

    DimensionAnubis(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_An"),

    DimensionHorus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ho"),

    DimensionMaahes(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Mh"),

    DimensionMehenBelt(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_MB"),

    DimensionNeper(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Np"),

    DimensionSeth(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Se"),

    DimensionDeepDark(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_DD"),

    HighEnergyFlowCircuit(Mods.NewHorizonsCoreMod.ID, "HighEnergyFlowCircuit"),

    ReinforcedGlassLens(Mods.NewHorizonsCoreMod.ID, "ReinforcedGlassLense"),

    MysteriousCrystalLens(Mods.NewHorizonsCoreMod.ID, "MysteriousCrystalLens"),

    RadoxPolymerLens(Mods.NewHorizonsCoreMod.ID, "RadoxPolymerLens"),

    ChromaticLens(Mods.NewHorizonsCoreMod.ID, "ChromaticLens"),

    Agar(Mods.NewHorizonsCoreMod.ID, "GTNHBioItems", 2),

    Elevator(Mods.OpenBlocks.ID, "elevator", 0),

    OpenBlocksSponge(Mods.OpenBlocks.ID, "sponge", 0),

    Tier3APU(Mods.OpenComputers.ID, "item", 103),

    CreativeComponentBus(Mods.OpenComputers.ID, "item", 114),

    Tier4Server(Mods.OpenComputers.ID, "item", 69),

    Tier3MicrocontrollerCase(Mods.OpenComputers.ID, "item", 90),

    Tier3DroneCase(Mods.OpenComputers.ID, "item", 91),

    Tier3TabletCase(Mods.OpenComputers.ID, "item", 93),

    RawAnchovy(Mods.PamsHarvestCraft.ID, "anchovyrawItem", 0),

    RawBass(Mods.PamsHarvestCraft.ID, "bassrawItem", 0),

    RawCalamari(Mods.PamsHarvestCraft.ID, "calamarirawItem", 0),

    RawCarp(Mods.PamsHarvestCraft.ID, "carprawItem", 0),

    RawCatfish(Mods.PamsHarvestCraft.ID, "catfishrawItem", 0),

    RawCharr(Mods.PamsHarvestCraft.ID, "charrrawItem", 0),

    RawClam(Mods.PamsHarvestCraft.ID, "clamrawItem", 0),

    RawCrab(Mods.PamsHarvestCraft.ID, "crabrawItem", 0),

    Cranberry(Mods.PamsHarvestCraft.ID, "cranberryItem", 0),

    RawCrayfish(Mods.PamsHarvestCraft.ID, "crayfishrawItem", 0),

    RawEel(Mods.PamsHarvestCraft.ID, "eelrawItem", 0),

    RawFrog(Mods.PamsHarvestCraft.ID, "frograwItem", 0),

    GreenHeartFish(Mods.PamsHarvestCraft.ID, "greenheartfishItem", 0),

    RawGrouper(Mods.PamsHarvestCraft.ID, "grouperrawItem", 0),

    RawHerring(Mods.PamsHarvestCraft.ID, "herringrawItem", 0),

    HotWings(Mods.PamsHarvestCraft.ID, "hotwingsItem", 0),

    RawJellyfish(Mods.PamsHarvestCraft.ID, "jellyfishrawItem", 0),

    RawMudfish(Mods.PamsHarvestCraft.ID, "mudfishrawItem", 0),

    RawOctopus(Mods.PamsHarvestCraft.ID, "octopusrawItem", 0),

    RawPerch(Mods.PamsHarvestCraft.ID, "perchrawItem", 0),

    Rice(Mods.PamsHarvestCraft.ID, "riceItem", 0),

    PamsHarvestCraftRoyalJelly(Mods.PamsHarvestCraft.ID, "royaljellyItem", 0),

    RawScallop(Mods.PamsHarvestCraft.ID, "scalloprawItem", 0),

    Seaweed(Mods.PamsHarvestCraft.ID, "seaweedItem", 0),

    RawShrimp(Mods.PamsHarvestCraft.ID, "shrimprawItem", 0),

    RawSnail(Mods.PamsHarvestCraft.ID, "snailrawItem", 0),

    RawSnapper(Mods.PamsHarvestCraft.ID, "snapperrawItem", 0),

    RawTilapia(Mods.PamsHarvestCraft.ID, "tilapiarawItem", 0),

    RawTrout(Mods.PamsHarvestCraft.ID, "troutrawItem", 0),

    RawTuna(Mods.PamsHarvestCraft.ID, "tunarawItem", 0),

    RawTurtle(Mods.PamsHarvestCraft.ID, "turtlerawItem", 0),

    RawWalleye(Mods.PamsHarvestCraft.ID, "walleyerawItem", 0),

    WaterChestnut(Mods.PamsHarvestCraft.ID, "waterchestnutItem", 0),

    CreativeICChip(Mods.ProjectRedFabrication.ID, "projectred.fabrication.icchip", 1),

    InvertedCyanLamp(Mods.ProjectRedIllumination.ID, "projectred.illumination.lamp", 25),

    InvertedOrangeLamp(Mods.ProjectRedIllumination.ID, "projectred.illumination.lamp", 17),

    Track(Mods.Railcraft.ID, "track", 0),
    TrackLegacyDamage736(Mods.Railcraft.ID, "track", 736),
    TrackLegacyDamage816(Mods.Railcraft.ID, "track", 816),

    WaterTankWall(Mods.Railcraft.ID, "machine.alpha", 14),

    AdvancedCokeOvenBrick(Mods.Railcraft.ID, "machine.alpha", 12),

    HobbyistSteamEngine(Mods.Railcraft.ID, "machine.beta", 7),

    FertilizedDirt(Mods.RandomThings.ID, "fertilizedDirt", 0),

    Spirit(Mods.RandomThings.ID, "ingredient", 3),

    Ic2Capacitor(Mods.SGCraft.ID, "ic2Capacitor", 0),

    RfPowerUnit(Mods.SGCraft.ID, "rfPowerUnit", 0),

    StargateChevronUpgrade(Mods.SGCraft.ID, "sgChevronUpgrade", 0),

    StargateControllerCrystal(Mods.SGCraft.ID, "sgControllerCrystal", 0),

    StargateCoreCrystal(Mods.SGCraft.ID, "sgCoreCrystal", 0),

    StargateIrisUpgrade(Mods.SGCraft.ID, "sgIrisUpgrade", 0),

    StargateBase(Mods.SGCraft.ID, "stargateBase", 0),

    StargateController(Mods.SGCraft.ID, "stargateController", 0),

    StargateRing(Mods.SGCraft.ID, "stargateRing", 0),

    StargateChevronBlock(Mods.SGCraft.ID, "stargateRing", 1),

    StandardHull(Mods.StevesCarts2.ID, "CartModule", 38),

    InfinityEngine(Mods.StevesCarts2.ID, "CartModule", 61),

    CreativeUpgrade(Mods.StevesCarts2.ID, "upgrade", 14),

    DrawerController(Mods.StorageDrawers.ID, "controller", 0),

    CapacityUpgradeII(Mods.StorageDrawers.ID, "upgrade", 2),

    CapacityUpgradeIII(Mods.StorageDrawers.ID, "upgrade", 3),

    CapacityUpgradeIV(Mods.StorageDrawers.ID, "upgrade", 4),

    CapacityUpgradeV(Mods.StorageDrawers.ID, "upgrade", 5),

    CapacityUpgradeVI(Mods.StorageDrawers.ID, "upgrade", 6),

    CapacityUpgradeVII(Mods.StorageDrawers.ID, "upgrade", 7),

    CapacityUpgradeVIII(Mods.StorageDrawers.ID, "upgrade", 8),

    UpgradeTemplate(Mods.StorageDrawers.ID, "upgradeTemplate", 0),

    StructureHologram(Mods.StructureLib.ID, "item.structurelib.constructableTrigger", 0),

    FocusTime(Mods.TaintedMagic.ID, "ItemFocusTime", 0),

    FocusPrimal(Mods.Thaumcraft.ID, "FocusPrimal", 0),

    FocusWarding(Mods.Thaumcraft.ID, "FocusWarding", 0),

    MundaneAmulet(Mods.Thaumcraft.ID, "ItemBaubleBlanks", 0),

    MundaneRing(Mods.Thaumcraft.ID, "ItemBaubleBlanks", 1),

    PrimordialPearl(Mods.Thaumcraft.ID, "ItemEldritchObject", 3),

    WhiteTallowCandle(Mods.Thaumcraft.ID, "blockCandle", 0),

    AirCrystal(Mods.Thaumcraft.ID, "blockCrystal", 0),

    FireCrystal(Mods.Thaumcraft.ID, "blockCrystal", 1),

    WaterCrystal(Mods.Thaumcraft.ID, "blockCrystal", 2),

    MixedCrystal(Mods.Thaumcraft.ID, "blockCrystal", 6),

    RunicMatrix(Mods.Thaumcraft.ID, "blockStoneDevice", 2),

    ArcaneWorkbench(Mods.Thaumcraft.ID, "blockTable", 15),

    GildedObsidian(Mods.ThaumicBases.ID, "eldritchArk", 0),

    RainbowCactus(Mods.ThaumicBases.ID, "rainbowCactus", 0),

    Revolver(Mods.ThaumicBases.ID, "revolver", 0),

    EssentiaStorageCell(Mods.ThaumicEnergistics.ID, "storage.essentia", 4),

    ArcaneAssembler(Mods.ThaumicEnergistics.ID, "thaumicenergistics.block.arcane.assembler", 0),

    EverburnUrn(Mods.ThaumicExploration.ID, "everburnUrn", 0),

    IchorPickGem(Mods.ThaumicTinkerer.ID, "ichorPickGem", 0),

    CobaltOre(Mods.TinkerConstruct.ID, "SearedBrick", 1),

    ArditeOre(Mods.TinkerConstruct.ID, "SearedBrick", 2),

    CraftingStation(Mods.TinkerConstruct.ID, "CraftingStation", 0),

    SlimeCrystal(Mods.TinkerConstruct.ID, "materials", 1),

    BlueSlimeCrystal(Mods.TinkerConstruct.ID, "materials", 17),

    SlimeGel(Mods.TinkerConstruct.ID, "slime.gel", 0),

    SlimePad(Mods.TinkerConstruct.ID, "slime.pad", 0),

    AlphaFur(Mods.TwilightForest.ID, "item.alphaFur", 0),

    ArcticFur(Mods.TwilightForest.ID, "item.arcticFur", 0),

    Carminite(Mods.TwilightForest.ID, "item.carminite", 0),

    CharmOfKeepingIII(Mods.TwilightForest.ID, "item.charmOfKeeping3", 0),

    CharmOfLifeII(Mods.TwilightForest.ID, "item.charmOfLife2", 0),

    CrumbleHorn(Mods.TwilightForest.ID, "item.crumbleHorn", 0),

    FieryBlood(Mods.TwilightForest.ID, "item.fieryBlood", 0),

    FieryTears(Mods.TwilightForest.ID, "item.fieryTears", 0),

    GiantPick(Mods.TwilightForest.ID, "item.giantPick", 0),

    GiantSword(Mods.TwilightForest.ID, "item.giantSword", 0),

    HydraChop(Mods.TwilightForest.ID, "item.hydraChop", 0),

    IceBomb(Mods.TwilightForest.ID, "item.iceBomb", 0),

    IronwoodIngot(Mods.TwilightForest.ID, "item.ironwoodIngot", 0),

    KnightmetalIngot(Mods.TwilightForest.ID, "item.knightMetal", 0),

    LampOfCinders(Mods.TwilightForest.ID, "item.lampOfCinders", 0),

    MagicBeans(Mods.TwilightForest.ID, "item.magicBeans", 0),

    MagicMapFocus(Mods.TwilightForest.ID, "item.magicMapFocus", 0),

    MazeMapFocus(Mods.TwilightForest.ID, "item.mazeMapFocus", 0),

    MazebreakerPick(Mods.TwilightForest.ID, "item.mazebreakerPick", 0),

    MeefSteak(Mods.TwilightForest.ID, "item.meefSteak", 0),

    MeefStroganoff(Mods.TwilightForest.ID, "item.meefStroganoff", 0),

    NagaScale(Mods.TwilightForest.ID, "item.nagaScale", 0),

    PhantomHelm(Mods.TwilightForest.ID, "item.phantomHelm", 0),

    PhantomPlate(Mods.TwilightForest.ID, "item.phantomPlate", 0),

    ScepterLifeDrain(Mods.TwilightForest.ID, "item.scepterLifeDrain", 0),

    ScepterTwilight(Mods.TwilightForest.ID, "item.scepterTwilight", 0),

    ScepterZombie(Mods.TwilightForest.ID, "item.scepterZombie", 0),

    SteeleafIngot(Mods.TwilightForest.ID, "item.steeleafIngot", 0),

    TripleBow(Mods.TwilightForest.ID, "item.tripleBow", 0),

    HydraTrophy(Mods.TwilightForest.ID, "item.trophy", 0),

    NagaTrophy(Mods.TwilightForest.ID, "item.trophy", 1),

    LichTrophy(Mods.TwilightForest.ID, "item.trophy", 2),

    UrGhastTrophy(Mods.TwilightForest.ID, "item.trophy", 3),

    SnowQueenTrophy(Mods.TwilightForest.ID, "item.trophy", 4),

    MinoshroomTrophy(Mods.TwilightForest.ID, "item.trophy", 5),

    KnightPhantomTrophy(Mods.TwilightForest.ID, "item.trophy", 6),

    AlphaYetiTrophy(Mods.TwilightForest.ID, "item.trophy", 7),

    QuestingRamTrophy(Mods.TwilightForest.ID, "item.trophy", 8),

    AuroraPillar(Mods.TwilightForest.ID, "tile.AuroraPillar", 0),

    FluffyCloud(Mods.TwilightForest.ID, "tile.FluffyCloud", 0),

    GiantCobble(Mods.TwilightForest.ID, "tile.GiantCobble", 0),

    GiantLog(Mods.TwilightForest.ID, "tile.GiantLog", 0),

    GiantObsidian(Mods.TwilightForest.ID, "tile.GiantObsidian", 0),

    HugeGloomBlock(Mods.TwilightForest.ID, "tile.HugeGloomBlock", 0),

    HugeLilyPad(Mods.TwilightForest.ID, "tile.HugeLilyPad", 0),

    HugeStalk(Mods.TwilightForest.ID, "tile.HugeStalk", 0),

    AuroraBrick(Mods.TwilightForest.ID, "tile.TFAuroraBrick", 0),

    TwilightOakLog(Mods.TwilightForest.ID, "tile.TFLog", 0),

    TreeOfTimeClock(Mods.TwilightForest.ID, "tile.TFMagicLogSpecial", 0),

    TreeOfTimeSapling(Mods.TwilightForest.ID, "tile.TFSapling", 5),

    SpiralBricks(Mods.TwilightForest.ID, "tile.TFSpiralBricks", 0),

    ReappearingBlock(Mods.TwilightForest.ID, "tile.TFTowerDevice", 0),

    VanishingBlock(Mods.TwilightForest.ID, "tile.TFTowerDevice", 2),

    WispyCloud(Mods.TwilightForest.ID, "tile.WispyCloud", 0),

    USConductiveIronSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 0),

    USElectricalSteelSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 1),

    USEnergeticAlloySingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 2),

    USDarkSteelSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 3),

    USPulsatingIronSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 4),

    USRedstoneAlloySingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 5),

    USSoulariumSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 6),

    USVibrantAlloySingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 7),

    USUnstableSingularity(Mods.UniversalSingularities.ID, "universal.extraUtilities.singularity", 0),

    USAluminumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 0),

    USBrassSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 1),

    USSapphireSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 10),

    USSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 11),

    USTitaniumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 12),

    USTungstenSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 13),

    USUraniumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 14),

    USZincSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 15),

    USTricalciumPhosphateSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 16),

    USPalladiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 17),

    USDamascusSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 18),

    USBlackSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 19),

    USBronzeSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 2),

    USElectrumFluxSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 20),

    USQuicksilverSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 21),

    USShadowSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 22),

    USIridiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 23),

    USNetherStarSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 24),

    USPlatinumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 25),

    USNaquadriaSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 26),

    USPlutoniumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 27),

    USMeteoricIronSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 28),

    USDeshSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 29),

    USCharcoalSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 3),

    USEuropiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 30),

    USGangueSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 31),

    USElectrumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 4),

    USInvarSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 5),

    USMagnesiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 6),

    USOsmiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 7),

    USPeridotSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 8),

    USRubySingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 9),

    USElectrotineSingularity(Mods.UniversalSingularities.ID, "universal.projectRed.singularity", 0),

    USAlumiteSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 1),

    USArditeSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 2),

    USCobaltSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 3),

    USEnderSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 4),

    USManyullynSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 6),

    USCoalSingularity(Mods.UniversalSingularities.ID, "universal.vanilla.singularity", 0),

    USEmeraldSingularity(Mods.UniversalSingularities.ID, "universal.vanilla.singularity", 1),

    USDiamondSingularity(Mods.UniversalSingularities.ID, "universal.vanilla.singularity", 2),

    InfinityEgg(Mods.Witchery.ID, "infinityegg", 0),

    WoodAsh(Mods.Witchery.ID, "ingredient", 18),

    WaterArtichokeBrew(Mods.Witchery.ID, "ingredient", 96),

    ArcaneCalculator(Mods.WitchingGadgets.ID, "item.WG_Material", 7);

    private final String modId;
    private final String registryName;
    private final int metadata;
    private final boolean forgeRegistry;
    private final boolean ic2NamedItem;

    ModsItemlist(String modId, String registryName) {
        this(modId, registryName, 0);
    }

    ModsItemlist(String modId, String registryName, int metadata) {
        this(modId, registryName, metadata, false, false);
    }

    ModsItemlist(String modId, String registryName, boolean forgeRegistry) {
        this(modId, registryName, 0, forgeRegistry, false);
    }

    ModsItemlist(String modId, String registryName, boolean forgeRegistry, boolean ic2NamedItem) {
        this(modId, registryName, 0, forgeRegistry, ic2NamedItem);
    }

    ModsItemlist(String modId, String registryName, int metadata, boolean forgeRegistry, boolean ic2NamedItem) {
        this.modId = modId;
        this.registryName = registryName;
        this.metadata = metadata;
        this.forgeRegistry = forgeRegistry;
        this.ic2NamedItem = ic2NamedItem;
    }

    public Block getBlock() {
        return GameRegistry.findBlock(modId, registryName);
    }

    public ItemStack get(long amount) {
        if (ic2NamedItem) {
            ItemStack stack = IC2Items.getItem(registryName);
            if (stack == null) return null;
            ItemStack copy = stack.copy();
            copy.stackSize = (int) amount;
            return copy;
        }
        if (forgeRegistry) return GameRegistry.findItemStack(modId, registryName, (int) amount);
        return GTModHandler.getModItem(modId, registryName, amount, metadata);
    }
}
