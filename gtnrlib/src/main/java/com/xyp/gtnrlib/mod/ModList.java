package com.xyp.gtnrlib.mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import cpw.mods.fml.common.Loader;
import lombok.Getter;

@Getter
public enum ModList implements IMod, ITargetMod {

    GTNeverReset(ModIds.GT_NEVER_RESET, Names.GT_NEVER_RESET),
    GTNHLib(ModIds.GTNH_LIB, Names.GTNH_LIB),
    Roguelike("Roguelike", "Roguelike Dungeons"),
    LootGames("lootgames", "LootGames"),
    GTNotLeisure(ModIds.GT_NOT_LEISURE, Names.GT_NOT_LEISURE),
    GTNotGood(ModIds.GT_NOT_GOOD, Names.GT_NOT_GOOD),
    JourneyMap(ModIds.JOURNEY_MAP, Names.JOURNEY_MAP),
    AE2(ModIds.APPLIED_ENERGISTICS_2, Names.APPLIED_ENERGISTICS_2),
    AE2FluidCraft(ModIds.AE2_FLUID_CRAFT, Names.AE2_FLUID_CRAFT),
    CropsNH(ModIds.CROPSNH, Names.CROPSNH),
    DraconicEvolution(ModIds.DRACONIC_EVOLUTION, Names.DRACONIC_EVOLUTION),
    SpiceOfLife(ModIds.SPICE_OF_LIFE, Names.SPICE_OF_LIFE),
    Forestry(ModIds.FORESTRY, Names.FORESTRY),
    MagicBees(ModIds.MAGIC_BEES, Names.MAGIC_BEES),
    ExtraBees(ModIds.EXTRA_BEES, Names.EXTRA_BEES),
    Railcraft(ModIds.RAILCRAFT, Names.RAILCRAFT),
    GregTech(ModIds.GREGTECH, Names.GREGTECH),
    ModularUI2(ModIds.MODULAR_UI_2, Names.MODULAR_UI_2),
    StructureLib(ModIds.STRUCTURE_LIB, Names.STRUCTURE_LIB),
    KubaTech(ModIds.KUBATECH, Names.KUBATECH),
    Thaumcraft(ModIds.THAUMCRAFT, Names.THAUMCRAFT),
    TinkersConstruct("TConstruct", "Tinkers' Construct", false),
    ThaumcraftResearchTweaks(ModIds.THAUMCRAFT_RESEARCH_TWEAKS, Names.THAUMCRAFT_RESEARCH_TWEAKS),
    BloodMagic(ModIds.BLOOD_MAGIC, Names.BLOOD_MAGIC),
    EnderIO(ModIds.ENDER_IO, Names.ENDER_IO),
    BetterQuesting(ModIds.BETTER_QUESTING, Names.BETTER_QUESTING),
    WarpTheory(ModIds.WARP_THEORY, Names.WARP_THEORY),

    Angelica("angelica", "Angelica", "com.gtnewhorizons.angelica.loading.AngelicaTweaker", false),
    TakoTech("TakoTech", "Tako Tech"),
    EyeOfHarmonyBuffer("eyeofharmonybuffer", "Eye Of Harmony Buffer"),
    ProgrammableHatches("programmablehatches", "Programmable Hatches"),
    TwistSpaceTechnology("TwistSpaceTechnology", "Twist Space Technology"),
    BoxPlusPlus("boxplusplus", "Box Plus Plus"),
    NHUtilities("NHUtilities", "NH-Utilities", "com.xir.NHUtilities.main.NHUtilitiesCore"),
    AE2Thing("ae2thing", "AE2 Things"),
    AE2ConvenienceFeatures(ModIds.AE2_CONVENIENCE_FEATURES, "AE2 Convenience Features", false),
    NeoEcoAEExtension("neoecoae", "Neo ECO AE Extension", false),
    QzMiner("qz_miner", "Qz Miner", false),
    OTHTechnology("123Technology", "123Technology"),
    Baubles("Baubles", "Baubles", false),
    Overpowered("Overpowered", "Overpowered"),
    ThinkTech("thinktech", "Think Tech"),
    VMTweak("vmtweak", "Void Miner Tweak"),
    Sudoku("sudoku", "Sudoku", false),
    GiveCount("givecount", "Give Count", false),
    ChromaticTooltips("chromatictooltips", "Chromatic Tooltips", false),
    ChromaticTooltipsCompat("chromatictooltipscompat", "Chromatic Tooltips Compat", false),
    AppliedEnergistics("appliedenergistics2", "Applied Energistics 2", "appeng.transformer.AppEngCore", false),
    WirelessCraftingTerminal("ae2wct", "Wireless Crafting Terminal", false),
    ThaumicEnergistics("thaumicenergistics", "Thaumic Energistics", false),

    NewHorizonsCoreMod("dreamcraft", "GT New Horizons Core Mod", "com.dreammaster.coremod.DreamCoreMod", false),
    GalaxySpace("GalaxySpace", "Galaxy Space", false),
    BetterQuestingAPI("bqapi", "Better Questing API", false),
    EnhancedLootBags("enhancedlootbags", "Enhanced Loot Bags", false),
    NotEnoughItems("NotEnoughItems", "Not Enough Items", false),
    InventoryBogoSorter("bogosorter", "Inventory Bogo Sorter", false),
    MouseTweaks("MouseTweaks", "Mouse Tweaks", false),
    CraftingTweaks("craftingtweaks", "Gregory Tweaks For Crafting", false),
    NotEnoughEnergistics("neenergistics", "Not Enough Energistics", false),
    NEICustomDiagrams("neicustomdiagram", "NEI Custom Diagrams", false),
    AvaritiaAddons("avaritiaddons", "Avaritia Addons", false),
    EtFuturumRequiem("etfuturum", "Et Futurum Requiem", false),
    ForgeMultipart("McMultipart", "Forge Multipart", false),
    MatterManipulator("matter-manipulator", "Matter Manipulator", false),

    AdvancedBackups("advancedbackups", "Advanced Backups", false),
    AdvancedMemoryCard("advanced_memory_card", "Advanced Memory Card", false),
    AdvancedSolarPanels("AdvancedSolarPanel", "Advanced Solar Panels", false),
    AdventureBackpack("adventurebackpack", "Adventure Backpack", false),
    AE2AutoPatternUpload("ae2_auto_pattern_upload", "AE2 Auto Pattern Upload", false),
    AE2InfinityCell("aeinfinitycell", "AE2 Infinity Cell", false),
    AE2PatternGen("ae2patterngen", "AE2PatternGen", false),
    AFSU("AFSU", "AFSU", false),
    AkashicTome("akashictome", "Akashic Tome", false),
    AlchemyGrate("alchgrate", "Alchemical Grate", false),
    AmazingTrophies("amazingtrophies", "Amazing Trophies", false),
    AngerMod("angermod", "Anger Mod", false),
    AppleCore("AppleCore", "AppleCore", false),
    AppliedCooking("appliedcooking", "AppliedCooking", false),
    AppliedEnergisticsEUNetwork("appeu", "Applied Energistics: EU Network", false),
    ArchaicFix("archaicfix", "ArchaicFix", false),
    ArchitectureCraft("ArchitectureCraft", "ArchitectureCraft", false),
    Aroma1997Core("Aroma1997Core", "Aroma1997Core", false),
    Aroma1997CoreHelper("Aroma1997CoreHelper", "Aroma1997Core|Helper", false),
    AromaBackup("AromaBackup", "AromaBackup", false),
    AromaBackupRecovery("AromaBackupRecovery", "AromaBackup Recovery", false),
    AsieLib("asielib", "AsieLib", false),
    AspectRecipeIndex("aspectrecipeindex", "Aspect Recipe Index", false),
    Automagy("Automagy", "Automagy", false),
    Avaritia("Avaritia", "Avaritia", false),
    Backhand("backhand", "Backhand", false),
    Backpack("Backpack", "Backpack", false),
    BakaDanmaku("bakadanmaku", "BakaDanmaku", false),
    BanditLegacy("bandit", "Bandit Legacy", false),
    BartWorks("bartworks", "BartWorks", false),
    BartWorksCrossmod("bartworkscrossmod", "BartWorks Mod Additions", false),
    BartWorksTGregworks("bartworkscrossmodtgregworkscontainer", "BartWorks Mod Additions - TGregworks Container",
        false),
    BaublesExpanded("Baubles|Expanded", "Baubles Expanded", false),
    BdLib("bdlib", "BD lib", false),
    BeeBetterAtBees("beebetteratbees", "BeeBetterAtBees", false),
    BetterAchievements("BetterAchievements", "Better Achievements", false),
    BetterBuildersWands("betterbuilderswands", "BetterBuildersWands", false),
    BetterCrashes("bettercrashes", "BetterCrashes", false),
    BetterFoliage("BetterFoliage", "Better Foliage", false),
    BetterLineBreak("better_line_break", "Better Line Break", false),
    BetterLoadingScreen("betterloadingscreen", "Better Loading Screen GTNH", false),
    BetterP2P("betterp2p", "BetterP2P", false),
    BetterQuestingExt("bqext", "BQ Ext", false),
    BetterQuestingQuestBook("questbook", "BetterQuesting Quest Book", false),
    BetterQuestingStandardExpansion("bq_standard", "BetterQuesting Standard Expansion", false),
    BetterTooltipBox("BetterTooltipBox", "BetterTooltipBox", false),
    BiblioCraft("BiblioCraft", "BiblioCraft", false),
    BiblioWoodsBoPEdition("BiblioWoodsBoP", "BiblioWoodsBoP", false),
    BiblioWoodsForestryEdition("BiblioWoodsForestry", "BiblioWoodsForestry", false),
    BiblioWoodsNaturaEdition("BiblioWoodsNatura", "BiblioWoodsNatura", false),
    BinnieCore("BinnieCore", "Binnie Core", false),
    BiomesOPlenty("BiomesOPlenty", "Biomes O' Plenty", false),
    BlockLimiter("blocklimiter", "BlockLimiter", false),
    BlockRenderer6343("blockrenderer6343", "BlockRenderer6343", false),
    BloodArsenal("BloodArsenal", "Blood Arsenal", false),
    Botania("Botania", "Botania", false),
    BotanicHorizons("botanichorizons", "Botanic Horizons", false),
    Botany("Botany", "Botany", false),
    BugTorch("bugtorch", "BugTorch", false),
    BuildCraftBuilders("BuildCraft|Builders", "BuildCraft Builders", false),
    BuildCraftCompat("BuildCraft|Compat", "BuildCraft Compat", false),
    BuildCraftCore("BuildCraft|Core", "BuildCraft", false),
    BuildCraftEnergy("BuildCraft|Energy", "BuildCraft Energy", false),
    BuildCraftFactory("BuildCraft|Factory", "BuildCraft Factory", false),
    BuildCraftOilTweak("OilTweak", "BuildCraft Oil Tweak", false),
    BuildCraftRobotics("BuildCraft|Robotics", "BuildCraft Robotics", false),
    BuildCraftSilicon("BuildCraft|Silicon", "BuildCraft Silicon", false),
    BuildCraftTransport("BuildCraft|Transport", "Buildcraft Transport", false),
    CarpentersBlocks("CarpentersBlocks", "Carpenter's Blocks", false),
    CatWalks("catwalks", "Catwalks", false),
    CheaperVoidMiners("cheapervoidminers", "CheaperVoidMiners", false),
    Chisel("chisel", "Chisel", false),
    ChiselTones("chiseltones", "Chisel Tones", false),
    ChunkAPI("chunkapi", "ChunkAPI", false),
    CodeChickenCore("CodeChickenCore", "CodeChicken Core", false),
    CoFHCore("CoFHCore", "CoFH Core", false),
    CommandBlocksForBetterQuesting("cb4bq", "Command Blocks for Better Questing", false),
    CompactKineticGenerators("compactkineticgenerators", "Compact Kinetic Generators", false),
    Computronics("computronics", "Computronics", false),
    Controlling("controlling", "Controlling", false),
    CookingForBlockheads("cookingforblockheads", "Cooking For Blockheads", false),
    CoreTweaks("coretweaks", "CoreTweaks", false),
    CosmeticArmorReworked("cosmeticarmorreworked", "CosmeticArmorReworked", false),
    CraftPresence("craftpresence", "CraftPresence", false),
    CreativeCore("creativecore", "CreativeCore", false),
    CrosshairSelection("crosshairselection", "Crosshair-Selection", false),
    CustomMainMenu("custommainmenu", "Custom Main Menu", false),
    CutCorners("CutCorners", "CutCorners", false),
    DamageIndicators("DamageIndicatorsMod", "Damage Indicators", false),
    DangoMusicCommunityEdition("dangomusicce", "DangoMusic Community Edition", false),
    Darkerer("darkerer", "Darkerer", false),
    Dashboard("dashboard", "Dashboard", false),
    DefaultConfigs("defaultkeys", "Default Configs", false),
    DefaultServerList("defaultserverlist", "DefaultServerList", false),
    DefaultWorldGenerator("defaultworldgenerator", "Default World Generator", false),
    DetravScannerMod("detravscannermod", "GT Scanner Mod", false),
    DiskTerminalUnofficial("disk_terminal", "DiskTerminal Unofficial", false),
    DistantHorizons("distanthorizons", "Distant Horizons", false),
    DoABarrelRollNH("doabarrelroll", "Do a Barrel Roll NH", false),
    DummyCore("DummyCore", "DummyCore", false),
    DuraDisplay("duradisplay", "DuraDisplay", false),
    DynamicLights("DynamicLights", "Dynamic Lights", false),
    DynamicLightsFloodLight("DynamicLights_floodLights", "Dynamic Lights Flood Light", false),
    DynamicLightsOnBurning("DynamicLights_onFire", "Dynamic Lights on burning", false),
    DynamicLightsOnCreepers("DynamicLights_creepers", "Dynamic Lights on Creepers", false),
    DynamicLightsOnFlameEnchantedArrows("DynamicLights_flameArrows", "Dynamic Lights on Flame enchanted Arrows", false),
    DynamicLightsOnItemEntities("DynamicLights_dropItems", "Dynamic Lights on ItemEntities", false),
    DynamicLightsOnMobEquipment("DynamicLights_mobEquipment", "Dynamic Lights on Mob Equipment", false),
    DynamicLightsOnSpecifiedEntities("DynamicLights_entityClasses", "Dynamic Lights on specified Entities", false),
    DynamicLightsOtherPlayerLight("DynamicLights_otherPlayers", "Dynamic Lights Other Player Light", false),
    DynamicLightsPlayerLight("DynamicLights_thePlayer", "Dynamic Lights Player Light", false),
    DynamicSurroundings("dsurround", "Dynamic Surroundings", false),
    ElectroMagicTools("EMT", "Electro-Magic Tools", false),
    EnderCore("endercore", "EnderCore", false),
    EnderStorage("EnderStorage", "EnderStorage", false),
    EnderZoo("EnderZoo", "Ender Zoo", false),
    EndlessIDs("endlessids", "EndlessIDs", false),
    EnigmaticLegacy("enigmaticlegacy", "Enigmatic Legacy", false),
    EternalSingularity("eternalsingularity", "Eternal Singularity", false),
    ExtraPlayerRenderer("extraplayerrenderer", "Extra Player Renderer", false),
    ExtraTrees("ExtraTrees", "Extra Trees", false),
    ExtraUtilities("ExtraUtilities", "ExtraUtilities", false),
    ExtremeSoundMufflerLegacy("extremesoundmuffler", "Extreme Sound Muffler Legacy", false),
    EZMiner("EZMiner", "EZMiner", false),
    EZNuclear("eznuclear", "EZ Nuclear", false),
    FalsePatternLib("falsepatternlib", "FalsePatternLib", false),
    FancyBlockParticles("fbp", "Fancy Block Particles", false),
    Fether("fether", "Fether", false),
    FindIt("findit", "FindIt", false),
    FissionEvolved("fissionevolved", "Fission Evolved", false),
    FlameChunk("flamechunk", "FlameChunk", false),
    FloodLights("FloodLights", "Flood Lights", false),
    FMusic("FMusic", "FMusic", false),
    ForbiddenMagic("ForbiddenMagic", "Forbidden Magic", false),
    Foreman("foreman", "Foreman", false),
    Forge("Forge", "Minecraft Forge", false),
    Forgelin("forgelin", "Forgelin", false),
    ForgeMicroblocks("ForgeMicroblock", "Forge Microblocks", false),
    ForgeModLoader("FML", "Forge Mod Loader", false),
    ForgeMultipartLibrary("ForgeMultipart", "Forge Multipart", false),
    ForgeRelocation("ForgeRelocation", "ForgeRelocation", false),
    FPSReducer("FpsReducer", "FPS Reducer", false),
    Freecam("freecam-gtnh", "Freecam", false),
    FunctionalStorage("functionalstorage", "Functional Storage", false),
    Gadomancy("gadomancy", "Gadomancy", false),
    GalacticGreg("galacticgreg", "Galactic Greg", false),
    GalacticraftAmunRa("GalacticraftAmunRa", "Galacticraft Amun-Ra", false),
    GalacticraftCore("GalacticraftCore", "Galacticraft Core", false),
    GalacticraftMars("GalacticraftMars", "Galacticraft Planets", false),
    Gendustry("gendustry", "Gendustry", false),
    Genetics("Genetics", "Genetics", false),
    GGFab("ggfab", "GigaGramFab", false),
    GoodGenerator("GoodGenerator", "Good Generator", false),
    GraviSuite("GraviSuite", "Gravitation Suite", false),
    GraviSuiteNEO("gravisuiteneo", "Gravitation Suite Neo", false),
    GregTechNH("gregtech_nh", "GregTech GTNH", false),
    GregTechProductionLineSimplification("simplification", "simplification", false),
    GregTechRelicsOfTheHorizon("gtrh", "GTRH", false),
    GTInterestingThing("gtit", "GTInterestingThing", false),
    GTMiaoBlueprint("gtmb", "GT-Miao-Blueprint", false),
    GTNHCredits("gtnhcredits", "GTNH Credits", false),
    GTNHExtLib("gtnhextlib", "GTNHExtLib", false),
    GTNHExtraNerf("gtnhextranerf", "GTNHExtraNerf", false),
    GTNHIntergalactic("gtnhintergalactic", "GTNH-Intergalactic", false),
    GTNHLanthanides("gtnhlanth", "GTNH Lanthanides", false),
    GTNHMixins("gtnhmixins", "GTNHMixins", false),
    GTNHModify("GTNHModify", "GTNHModify", false),
    GTNHTCWands("gtnhtcwands", "GTNH-TC-Wands", false),
    GTNLLib("gtnllib", "GTNLLib", false),
    GTNN("gtnn", "GTNN", false),
    GTNotHard("gtnothard", "GT Not Hard", false),
    GTPlusPlus("miscutils", "GT++", false),
    GTPlusPlusEverglades("ToxicEverglades", "GT++ Toxic Everglades", false),
    GTSimpleWirelessNetwork("gtswn", "GTSimpleWirelessNetwork", false),
    GTSteamReborn("gtsr", "GTSteamReborn", false),
    GuideNH("guidenh", "GuideNH", false),
    HardcoreEnderExpansion("HardcoreEnderExpansion", "Hardcore Ender Expansion", false),
    Healer("healer", "Healer", false),
    HelpFixer("HelpFixer", "HelpFixer", false),
    HodgePodge("hodgepodge", "Hodgepodge", false),
    HoloInventory("holoinventory", "Holo Inventory", false),
    HungerOverhaul("HungerOverhaul", "Hunger Overhaul", false),
    HydroEnergy("hydroenergy", "HydroEnergy", false),
    IC2NuclearControl("IC2NuclearControl", "Nuclear Control 2", false),
    IguanaTweaksTinkerConstruct("IguanaTweaksTConstruct", "Iguana Tinker Tweaks", false),
    ImmersivePaintingsNH("ImmersivePaintingsNH", "Immersive Paintings NH", false),
    IndustrialCraft2("IC2", "IndustrialCraft 2", false),
    InfernalMobs("InfernalMobs", "Infernal Mobs", false),
    InGameInfoXML("InGameInfoXML", "InGame Info XML", false),
    InputFix("InputFix", "InputFix", false),
    InputMethodBlockerGTNH("inputmethodblockergtnh", "InputMethodBlocker-GTNH", false),
    InvHUD2GTNH("invhud2gtnh", "InvHUD2GTNH", false),
    IronChests("IronChest", "Iron Chests", false),
    IronChestsMinecarts("ironchestminecarts", "Iron Chest Minecarts", false),
    IronTanks("irontank", "Iron Tanks", false),
    IronTanksMinecarts("irontankminecarts", "Iron Tank Minecarts", false),
    ItemPhysicLite("itemphysic", "ItemPhysic Lite", false),
    IWillFindYou("ifu", "I Will Find You", false),
    JABBA("JABBA", "JABBA", false),
    JarJar("jarjar", "JarJar", false),
    JdkJarVersion21Enforcer("jdkjarversion21enforcer", "JDK Jar Version 21 Enforcer", false),
    JourneyMapServer("JourneyMapServer", "JourneyMapServer", false),
    KekzTech("kekztech", "KekzTech", false),
    LagGoggles("laggoggles", "LagGoggles", false),
    Libertas("libertas", "Libertas", false),
    LiquidLogged("liquidlogged", "LiquidLogged", false),
    LiteLoaderLoader("liteloaderloader", "LiteLoaderLoader", false),
    LittleTiles("littletiles", "LittleTiles", false),
    LogisticsPipes("LogisticsPipes", "Logistics Pipes", false),
    LunatriusCore("LunatriusCore", "LunatriusCore", false),
    LWJGL3ify("lwjgl3ify", "Lwjgl3ify", false),
    MalisisCore("malisiscore", "Malisis Core", false),
    MalisisDoors("malisisdoors", "Malisis' Doors", false),
    Mantle("Mantle", "Mantle", false),
    MAtmos("matmos", "MAtmos", false),
    MCPatcherForge("mcpatcherforge", "MCPatcherForge", false),
    MCphone("mcphone", "MCphone", false),
    MessTech("MessTech", "MessTech", false),
    MineAndBladeBattleGear2("battlegear2", "Mine & Blade Battlegear 2", false),
    Minecraft("minecraft", "Minecraft", false),
    MinecraftFrames("MCFrames", "MCFrames", false),
    Mineshot("mineshot", "Mineshot", false),
    MineTweaker("MineTweaker3", "CraftTweaker", false),
    MinetweakerGT5Addon("GTTweaker", "GTTweaker", false),
    MinoppClassic("minopp", "Mino++: Classic", false),
    MixinBooterLegacy("mixinbooterlegacy", "MixinBooterLegacy", false),
    MixinExtras("mixinextras", "UniMixins: MixinExtras", false),
    Mixingasm("mixingasm", "UniMixins: Mixingasm", false),
    MobsInfo("mobsinfo", "MobsInfo", false),
    ModernKeyBinding("mkb", "ModernKeyBinding", false),
    ModernMarkings("modernmarkings", "ModernMarkings", false),
    ModTweaker("modtweaker2", "Mod Tweaker 2", false),
    ModularUI("modularui", "ModularUI", false),
    Morpheus("Morpheus", "Morpheus", false),
    MouseSideButtonFix("msbf", "MouseSideButtonFix", false),
    MrTJPCore("MrTJPCoreMod", "MrTJPCore", false),
    MyCTMLib("MyCTMLib", "MyCTMLib", false),
    Natura("Natura", "Natura", false),
    NaturesCompass("naturescompass", "Nature's Compass", false),
    Navigator("navigator", "Navigator", false),
    NEIAddons("NEIAddons", "NEI Addons", false),
    NEIAddonsAppliedEnergistics2("NEIAddons|AppEng", "NEI Addons: Applied Energistics 2", false),
    NEIAddonsBotany("NEIAddons|Botany", "NEI Addons: Botany", false),
    NEIAddonsCraftingTables("NEIAddons|CraftingTables", "NEI Addons: Crafting Tables", false),
    NEIAddonsDeveloperTools("NEIAddons|Developer", "NEI Addons: Developer Tools", false),
    NEIAddonsExNihilo("NEIAddons|ExNihilo", "NEI Addons: Ex Nihilo", false),
    NEIAddonsForestry("NEIAddons|Forestry", "NEI Addons: Forestry", false),
    NEIGuess("neiguess", "NEI Guess", false),
    NEIIntegration("neiintegration", "NEIIntegration", false),
    NEIOrePlugin("gtneioreplugin", "GT NEI Ore Plugin GT:NH Mod", false),
    NEIRecipePanels("nei-recipe-panels", "NEI Recipe Panels", false),
    NEIRecipeTree("neirecipetree", "NEI-RecipeTree", false),
    NeoECOAEExtension("neoecoae", "Neo ECO AE Extension", false),
    NetherPortalFix("netherportalfix", "Nether Portal Fix", false),
    NeverEnoughBandwidthLegacy("neb", "NotEnoughBandwidth", false),
    NeverEnoughCharactersRework("nech", "NeverEnoughCharacters-Rework", false),
    NHKantan("nhkantan", "NH kantan", false),
    NodalMechanics("NodalMechanics", "NodalMechanics", false),
    NoFog("no_fog", "NoFog", false),
    NoHotbarNeeded("nohotbarneeded", "NoHotbarNeeded", false),
    NotEnoughCharacters("nechar", "Not Enough Characters", false),
    NotFine("notfine", "NotFine", false),
    Nutrition("nutrition", "Nutrition", false),
    Nyx("nyx", "Nyx", false),
    OAuth("oauth", "OAuth", false),
    OKBackpack("okbackpack", "OK Backpack", false),
    OmniOcularUnofficial("OmniOcularUnofficial", "Omni Ocular Unofficial", false),
    OpenBlocks("OpenBlocks", "OpenBlocks", false),
    OpenComputers("OpenComputers", "OpenComputers", false),
    OpenGlasses("openglasses", "OpenGlasses", false),
    OpenModsCore("OpenModsCore", "OpenModsCore", false),
    OpenModsLib("OpenMods", "OpenMods", false),
    OpenModularTurrets("openmodularturrets", "Open Modular Turrets", false),
    OpenPrinters("openprinter", "OpenPrinter", false),
    OpenSecurity("opensecurity", "OpenSecurity", false),
    Opis("Opis", "Opis", false),
    OTHTechnologyModern("123technology", "123Technology", false),
    OverloadedArmorBar("overloadedarmorbar", "Overloaded Armor Bar", false),
    PamsHarvestCraft("harvestcraft", "Pam's HarvestCraft", false),
    PatPat("patpat", "PatPat", false),
    PersonalSpace("personalspace", "PersonalSpace", false),
    Pico8GTNH("pico8", "PICO-8", false),
    PlanNH("plannh", "PlanNH", false),
    PlayerAPI("PlayerAPI", "Player API", false),
    Postea("postea", "Postea", false),
    ProjectBlue("ProjectBlue", "Project Blue", false),
    ProjectRedCompatibility("ProjRed|Compatibility", "ProjectRed Compatibility", false),
    ProjectRedCore("ProjRed|Core", "ProjectRed Core", false),
    ProjectRedExpansion("ProjRed|Expansion", "ProjectRed Expansion", false),
    ProjectRedExploration("ProjRed|Exploration", "ProjectRed Exploration", false),
    ProjectRedFabrication("ProjRed|Fabrication", "ProjectRed Fabrication", false),
    ProjectRedIllumination("ProjRed|Illumination", "ProjectRed Illumination", false),
    ProjectRedIntegration("ProjRed|Integration", "ProjectRed Integration", false),
    ProjectRedTransmission("ProjRed|Transmission", "ProjectRed Transmission", false),
    ProjectRedTransportation("ProjRed|Transportation", "ProjectRed Transportation", false),
    QuickEasyMenu("qemenu", "Quick Easy Menu", false),
    QzUILib("qz_uilib", "Qz-UILib", false),
    RandomBoubles("randomboubles", "Random Boubles", false),
    RandomThings("RandomThings", "RandomThings", false),
    RawInput("rawinput", "RawInput", false),
    RealisticWorldGen("RWG", "Realistic World Gen", false),
    RealisticWorldGen2("rwg2", "Realistic World Gen 2", false),
    RelocationFMP("RelocationFMP", "RelocationFMP", false),
    RemoteIO("RIO", "RemoteIO", false),
    ReplayMod("replaymod", "ReplayMod", false),
    SalisArcana("salisarcana", "Salis Arcana", false),
    Schematica("Schematica", "Schematica", false),
    ServerUtilities("serverutilities", "Server Utilities", false),
    SGCraft("SGCraft", "SG Craft", false),
    ShareWhereIAm("sharewhereiam", "Share Where I am", false),
    SimpleSkinbackport("simpleskinbackport", "SimpleSkinbackport", false),
    SkinPort("skinport", "SkinPort", false),
    SleepingBags("sleepingbag", "Sleeping Bag", false),
    SmallPhone("smallphone", "SmallPhone", false),
    SmoothFont("smoothfont", "Smooth Font", false),
    SpecialMobs("SpecialMobs", "Special Mobs", false),
    SpongeMixins("spongemixins", "SpongeMixins Loader", false),
    StevesAddons("StevesAddons", "Steve's Addons", false),
    StevesCarts2("StevesCarts", "Steve's Carts 2", false),
    StevesFactoryManager("StevesFactoryManager", "Steve's Factory Manager", false),
    StorageDrawers("StorageDrawers", "Storage Drawers", false),
    StorageDrawersBiomesOPlentyPack("StorageDrawersBop", "Storage Drawers: Biomes O' Plenty Pack", false),
    StorageDrawersErebusPack("StorageDrawersErebus", "Storage Drawers: Erebus Pack", false),
    StorageDrawersForestryPack("StorageDrawersForestry", "Storage Drawers: Forestry Pack", false),
    StorageDrawersMiscPack("StorageDrawersMisc", "Storage Drawers: Misc Pack", false),
    StorageDrawersNaturaPack("StorageDrawersNatura", "Storage Drawers: Natura Pack", false),
    StructureCompat("structurecompat", "StructureCompat", false),
    SuperSolarPanels("supersolarpanel", "Super Solar Panel", false),
    SuperTiC("SuperTic", "SuperTic", false),
    TaintedMagic("TaintedMagic", "Tainted Magic", false),
    TaskNH("tasknh", "TaskNH", false),
    TC4Tweaks("tc4tweak", "TC4 Tweak", false),
    TCNodeTracker("tcnodetracker", "TC Node Tracker", false),
    TecTech("tectech", "TecTech", false),
    Tempora("tempora", "Tempora", false),
    TenkeiTech("tenkeitech", "TenkeiTech", false),
    ThaumcraftMobAspects("ThaumcraftMobAspects", "Thaumcraft Mob Aspects", false),
    ThaumicBases("thaumicbases", "Thaumic Bases", false),
    ThaumicBoots("thaumicboots", "Thaumic Boots", false),
    ThaumicExploration("ThaumicExploration", "Thaumic Exploration", false),
    ThaumicHorizons("ThaumicHorizons", "ThaumicHorizons", false),
    ThaumicInsurgence("thaumicinsurgence", "Thaumic Insurgence", false),
    ThaumicMachina("ThaumicMachina", "Thaumic Machina", false),
    ThaumicTinkerer("ThaumicTinkerer", "Thaumic Tinkerer", false),
    TiCTooltips("TiCTooltips", "TiC Tooltips", false),
    TimeCrystal("timecrystal", "Time Crystal", false),
    TinkersDefence("tinkersdefense", "Tinkers' Defense", false),
    TinkersGregworks("TGregworks", "Tinkers' Gregworks", false),
    TinkersMechworks("TMechworks", "Tinkers Mechworks", false),
    TooMuchLoot("TML", "TooMuchLoot", false),
    Torcherino("Torcherino", "Torcherino", false),
    ToroHealth("torohealthmod", "ToroHealth", false),
    Translocator("Translocator", "Translocator", false),
    TwilightForest("TwilightForest", "Twilight Forest", false),
    TXLoader("txloader", "TX Loader", false),
    UniLib("unilib", "UniLib", false),
    UniMixins("unimixins", "UniMixins", false),
    UniMixinsCompat("unimixins-compat", "UniMixins: Compatibility", false),
    UniMixinsMixin("unimixins-mixin", "UniMixins: Mixin (UniMix)", false),
    UniversalSingularities("universalsingularities", "UniversalSingularities", false),
    Untranslator("Untranslator", "Untranslator", false),
    VendingMachine("vendingmachine", "Vending Machine", false),
    VillageNames("VillageNames", "Village Names", false),
    VisualProspecting("visualprospecting", "VisualProspecting", false),
    Waila("Waila", "Waila", false),
    WailaHarvestability("WailaHarvestability", "Waila Harvestability", false),
    WailaPlugins("wailaplugins", "WAILA Plugins", false),
    WanionLib("wanionlib", "WanionLib", false),
    WhatAreWeLookingAt("wawla", "WAWLA", false),
    WildcardPattern("wildcardpattern", "Wildcard Pattern", false),
    WirelessRedstoneCBEAddons("WR-CBE|Addons", "WR-CBE Addons", false),
    WirelessRedstoneCBECore("WR-CBE|Core", "WR-CBE Core", false),
    WirelessRedstoneCBELogic("WR-CBE|Logic", "WR-CBE Logic", false),
    Witchery("witchery", "Witchery", false),
    WitcheryExtras("WitcheryExtras", "Witchery++", false),
    WitchingGadgets("WitchingGadgets", "Witching Gadgets", false),
    WorldEdit("worldedit", "WorldEdit", false),
    WorldTooltips("world-tooltips", "World Tooltips", false),
    WTCT("wtct", "AE2实用", false),
    YAMCore("YAMCore", "YAMCL", false),
    YesSteveModelUnofficial("ysmu", "ysmu", false),
    ZeroPointBugfix("ZeroPointBugfix", "ZeroPointBugfix", false),
    ZTones("Ztones", "Ztones", false);

    public static class ModIds {

        public static final String GT_NEVER_RESET = "gtneverreset";
        public static final String GTNH_LIB = "gtnhlib";
        public static final String GT_NOT_LEISURE = "sciencenotleisure";
        public static final String TWIST_SPACE_TECHNOLOGY = "TwistSpaceTechnology";
        public static final String GT_NOT_GOOD = "gtnotgood";
        public static final String ANGELICA = "angelica";
        public static final String JOURNEY_MAP = "journeymap";
        public static final String NOT_ENOUGH_ITEMS = "NotEnoughItems";
        public static final String BETTER_QUESTING_API = "bqapi";
        public static final String BETTER_QUESTING = "betterquesting";
        public static final String APPLIED_ENERGISTICS_2 = "appliedenergistics2";
        public static final String AE2_FLUID_CRAFT = "ae2fc";
        public static final String AE2_CONVENIENCE_FEATURES = "ae2conveniencefeatures";
        public static final String CROPSNH = "cropsnh";
        public static final String DRACONIC_EVOLUTION = "DraconicEvolution";
        public static final String SPICE_OF_LIFE = "SpiceOfLife";
        public static final String FORESTRY = "Forestry";
        public static final String MAGIC_BEES = "MagicBees";
        public static final String EXTRA_BEES = "ExtraBees";
        public static final String RAILCRAFT = "Railcraft";
        public static final String GREGTECH = "gregtech";
        public static final String MODULAR_UI_2 = "modularui2";
        public static final String STRUCTURE_LIB = "structurelib";
        public static final String KUBATECH = "kubatech";
        public static final String THAUMCRAFT = "Thaumcraft";
        public static final String THAUMCRAFT_RESEARCH_TWEAKS = "ThaumcraftResearchTweaks";
        public static final String BAUBLES = "Baubles";
        public static final String BLOOD_MAGIC = "AWWayofTime";
        public static final String THAUMIC_ENERGISTICS = "thaumicenergistics";
        public static final String WARP_THEORY = "WarpTheory";
        public static final String ENDER_IO = "EnderIO";
        public static final String MATTER_MANIPULATOR = "matter-manipulator";

    }

    public static class Names {

        public static final String GT_NEVER_RESET = "GT-Never-Reset";
        public static final String GTNH_LIB = "GTNHLib";
        public static final String GT_NOT_LEISURE = "GT Not Leisure";
        public static final String TWIST_SPACE_TECHNOLOGY = "Twist Space Technology";
        public static final String GT_NOT_GOOD = "GT-Not-Good";
        public static final String ANGELICA = "Angelica";
        public static final String JOURNEY_MAP = "JourneyMap";
        public static final String NOT_ENOUGH_ITEMS = "Not Enough Items";
        public static final String BETTER_QUESTING_API = "Better Questing API";
        public static final String BETTER_QUESTING = "Better Questing";
        public static final String APPLIED_ENERGISTICS_2 = "Applied Energistics 2";
        public static final String AE2_FLUID_CRAFT = "AE2 Fluid Crafting";
        public static final String CROPSNH = "CropsNH";
        public static final String DRACONIC_EVOLUTION = "Draconic Evolution";
        public static final String SPICE_OF_LIFE = "Spice of Life";
        public static final String FORESTRY = "Forestry";
        public static final String MAGIC_BEES = "Magic Bees";
        public static final String EXTRA_BEES = "Extra Bees";
        public static final String RAILCRAFT = "Railcraft";
        public static final String GREGTECH = "GregTech";
        public static final String MODULAR_UI_2 = "Modular UI 2";
        public static final String STRUCTURE_LIB = "StructureLib";
        public static final String KUBATECH = "KubaTech";
        public static final String THAUMCRAFT = "Thaumcraft";
        public static final String THAUMCRAFT_RESEARCH_TWEAKS = "Thaumcraft Research Tweaks";
        public static final String BAUBLES = "Baubles";
        public static final String BLOOD_MAGIC = "Blood Magic";
        public static final String THAUMIC_ENERGISTICS = "Thaumic Energistics";
        public static final String WARP_THEORY = "WarpTheory";
        public static final String ENDER_IO = "Ender IO";
        public static final String MATTER_MANIPULATOR = "Matter Manipulator";

    }

    public static final ModList[] VALUES = values();

    public final String ID;
    public final String resourceDomain;
    public final String displayName;
    public final boolean showInModList;
    private final TargetModBuilder targetBuilder;
    private Boolean modLoaded;

    ModList(String ID, String displayName) {
        this(ID, displayName, null, true);
    }

    ModList(String ID, String displayName, boolean showInModList) {
        this(ID, displayName, null, showInModList);
    }

    ModList(String ID, String displayName, String coreModClass) {
        this(ID, displayName, coreModClass, true);
    }

    ModList(String ID, String displayName, String coreModClass, boolean showInModList) {
        this.ID = ID;
        this.resourceDomain = ID.toLowerCase(Locale.ENGLISH);
        this.displayName = displayName;
        this.showInModList = showInModList;
        this.targetBuilder = new TargetModBuilder().setModId(ID)
            .setCoreModClass(coreModClass);
    }

    @NotNull
    @Override
    public TargetModBuilder getBuilder() {
        return targetBuilder;
    }

    @Override
    public boolean isModLoaded() {
        if (this.modLoaded == null) {
            this.modLoaded = Loader.isModLoaded(ID);
        }
        return this.modLoaded;
    }

    public static List<ModList> loadedListedMods() {
        List<ModList> loaded = new ArrayList<>();
        for (ModList mod : VALUES) {
            if (mod.showInModList && mod.isModLoaded()) loaded.add(mod);
        }
        return loaded;
    }

    public static boolean allListedModsLoaded() {
        for (ModList mod : VALUES) {
            if (mod.showInModList && !mod.isModLoaded()) return false;
        }
        return true;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }

    public String getResourcePath(String path) {
        return this.getResourceLocation(path)
            .toString();
    }

    public String getResourcePath(String... path) {
        return this.getResourceLocation(path)
            .toString();
    }

    public ResourceLocation getResourceLocation(String path) {
        return new ResourceLocation(this.resourceDomain, path);
    }

    public ResourceLocation getResourceLocation(String... path) {
        return new ResourceLocation(this.resourceDomain, String.join("/", path));
    }
}
