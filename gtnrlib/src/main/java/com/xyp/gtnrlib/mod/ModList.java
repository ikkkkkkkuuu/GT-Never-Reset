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
    MatterManipulator("matter-manipulator", "Matter Manipulator", false);

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
