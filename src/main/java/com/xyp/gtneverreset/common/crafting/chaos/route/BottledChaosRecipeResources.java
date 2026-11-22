package com.xyp.gtneverreset.common.crafting.chaos.route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey;
import com.xyp.gtneverreset.common.crafting.chaos.material.ChaosRecipeMaterials;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.config.ChaosConfig;

import bartworks.API.recipe.BartWorksRecipeMaps;
import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBees;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.chemplant.MTEChemicalPlant;
import gtnhlanth.api.recipe.LanthanidesRecipeMaps;
import gtnhlanth.common.register.BotWerkstoffMaterialPool;
import gtnhlanth.common.register.WerkstoffMaterialPool;
import kubatech.loaders.DEFCRecipes;
import tectech.recipe.TecTechRecipeMaps;

/**
 * Shared recipe resources. Access after material and fluid registration (recipe loading at load-complete).
 * Elemental forms follow atomic-number order; compounds and mixtures are added only when used.
 * Chemical names are written in full. Undefined sludge/solution compositions retain their source names.
 * IDs are read-only templates; obtain an ItemStack copy before modifying its NBT.
 */
public final class BottledChaosRecipeResources {

    public static final int PLATINUM_LEAVES_PER_POWDER = 4;
    private static final Map<ChaosItemKey, ChaosItemKey> UNIFIED_DUSTS = new HashMap<>();
    private static final List<ItemStack> FRUIT_INPUTS = GTOreDictUnificator.getOresImmutable("listAllfruit");
    /** Wildcard vanilla log markers; together the wood route accepts every vanilla wood type. */
    public static final ChaosItemKey WoodLog = ChaosItemKey.createAsWildcard(new ItemStack(Blocks.log));
    public static final ChaosItemKey WoodLog2 = ChaosItemKey.createAsWildcard(new ItemStack(Blocks.log2));

    /** Tag identities and runtime indexes; capacity costs are published by route registration. */
    public static final class CredentialModel {

        // Item names are queried during post-init, before all recipe resources have been registered.
        private static final boolean HAS_HONEYCOMBS = Mods.Forestry.isModLoaded() && GTBees.combs != null;
        private static final boolean HAS_NETHERITE = Mods.EtFuturumRequiem.isModLoaded();
        private static final boolean HAS_ETERNITY = Mods.EternalSingularity.isModLoaded();
        public static final String TAGS_NBT_KEY = "bottledChaosTags";
        public static final String OCCUPIED_WEIGHT_NBT_KEY = "bottledChaosOccupiedWeight";

        /** Declaration order determines display priority and runtime indexes; NBT stores tag names. */
        private record TagDefinition(String tag, boolean enabled, String family, BottledChaosRecipePage page) {}

        // Families follow their entry voltage, then progression; main-page tags stay last.
        // This order also drives credential injection and NEI tabs. NBT stores names, not indexes.
        // Material bindings stay in CredentialRoutes and are not initialized with this metadata.
        // spotless:off
        private static final TagDefinition[] DEFINITIONS = {
            // MV and EV chemical families.
            new TagDefinition("wood", true, "wood", BottledChaosRecipePage.WOOD),
            new TagDefinition("petrochemical", true, "petrochemical", BottledChaosRecipePage.PETROCHEMICAL),
            // LuV families.
            new TagDefinition("titanium", true, "titanium", BottledChaosRecipePage.TITANIUM),
            new TagDefinition("tungsten", true, "tungsten", BottledChaosRecipePage.TUNGSTEN),
            new TagDefinition("indium", true, "indium", BottledChaosRecipePage.INDIUM),
            new TagDefinition("isamill", true, "isamill", BottledChaosRecipePage.ISA_MILL),
            new TagDefinition("purified_water_1", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            new TagDefinition("purified_water_2", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            new TagDefinition("purified_water_3", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            new TagDefinition("purified_water_4", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            new TagDefinition("purified_water_5", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            new TagDefinition("purified_water_6", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            new TagDefinition("purified_water_7", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            new TagDefinition("purified_water_8", true, "purified_water", BottledChaosRecipePage.PURIFIED_WATER),
            // ZPM families.
            new TagDefinition("platinum_metallic_powder", true, "platinum", BottledChaosRecipePage.PLATINUM),
            new TagDefinition("palladium_metallic_powder", true, "platinum", BottledChaosRecipePage.PLATINUM),
            new TagDefinition("platinum_residue", true, "platinum", BottledChaosRecipePage.PLATINUM),
            new TagDefinition("crude_rhodium_metal", true, "platinum", BottledChaosRecipePage.PLATINUM),
            new TagDefinition("leach_residue", true, "platinum", BottledChaosRecipePage.PLATINUM),
            new TagDefinition("rarest_metal_residue", true, "platinum", BottledChaosRecipePage.PLATINUM),
            new TagDefinition("iridium_metal_residue", true, "platinum", BottledChaosRecipePage.PLATINUM),
            // UV families.
            new TagDefinition("monazite", true, "lanthanide", BottledChaosRecipePage.LANTHANIDE),
            new TagDefinition("bastnasite", true, "lanthanide", BottledChaosRecipePage.LANTHANIDE),
            new TagDefinition("cerium", true, "lanthanide", BottledChaosRecipePage.LANTHANIDE),
            new TagDefinition("samarium", true, "lanthanide", BottledChaosRecipePage.LANTHANIDE),
            new TagDefinition("ancient_debris", HAS_NETHERITE, "ancient_debris", BottledChaosRecipePage.NETHERITE),
            new TagDefinition("kevlar", true, "kevlar", BottledChaosRecipePage.KEVLAR),
            // UHV families.
            new TagDefinition("naquadah", true, "naquadah", BottledChaosRecipePage.NAQUADAH),
            new TagDefinition("enriched_naquadah", true, "naquadah", BottledChaosRecipePage.NAQUADAH),
            new TagDefinition("naquadria", true, "naquadah", BottledChaosRecipePage.NAQUADAH),
            new TagDefinition("nano_extraction", true, "nano_extraction", BottledChaosRecipePage.NANO_EXTRACTION),
            new TagDefinition("prismatic_acid", true, "prismatic", BottledChaosRecipePage.PRISMATIC),
            new TagDefinition("growth_medium_sterilized", true, "biological", BottledChaosRecipePage.BIOLOGICAL),
            new TagDefinition("stem_cells", true, "biological", BottledChaosRecipePage.BIOLOGICAL),
            new TagDefinition("seaweed_broth", true, "biological", BottledChaosRecipePage.BIOLOGICAL),
            new TagDefinition("tau_ceti_e_seaweed_extract", true, "biological", BottledChaosRecipePage.BIOLOGICAL),
            new TagDefinition("bio_medium_sterilized", true, "biological", BottledChaosRecipePage.BIOLOGICAL),
            new TagDefinition("bio_cells", true, "biological", BottledChaosRecipePage.BIOLOGICAL),
            // UEV families; higher fuel variants retain their native power.
            new TagDefinition("super_heavy_radox", true, "radox", BottledChaosRecipePage.RADOX),
            new TagDefinition("heavy_radox", true, "radox", BottledChaosRecipePage.RADOX),
            new TagDefinition("light_radox", true, "radox", BottledChaosRecipePage.RADOX),
            new TagDefinition("super_light_radox", true, "radox", BottledChaosRecipePage.RADOX),
            new TagDefinition("xenoxene", true, "radox", BottledChaosRecipePage.RADOX),
            new TagDefinition("naquadah_fuel_preparation", true, "naquadah_fuel_preparation", BottledChaosRecipePage.NAQUADAH_FUEL),
            new TagDefinition("naquadah_fuel_3", true, "naquadah_fuel_3", BottledChaosRecipePage.NAQUADAH_FUEL),
            new TagDefinition("naquadah_fuel_4", true, "naquadah_fuel_4", BottledChaosRecipePage.NAQUADAH_FUEL),
            new TagDefinition("naquadah_fuel_5", true, "naquadah_fuel_5", BottledChaosRecipePage.NAQUADAH_FUEL),
            new TagDefinition("naquadah_fuel_6", true, "naquadah_fuel_6", BottledChaosRecipePage.NAQUADAH_FUEL),
            new TagDefinition("fusion", true, "fusion", BottledChaosRecipePage.FUSION),
            // Main-page routes always follow the dedicated families.
            new TagDefinition("honeycomb", HAS_HONEYCOMBS, "honeycomb", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("crystal_cycle", ChaosConfig.BottledChaosCycleRecipes, "crystal_cycle", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("cerium_lutetium_garnet", ChaosConfig.BottledChaosCycleRecipes, "cerium_lutetium_garnet", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("tesseract", ChaosConfig.BottledChaosCycleRecipes, "tesseract", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("eternity", HAS_ETERNITY && ChaosConfig.BottledChaosCycleRecipes, "eternity", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("phonon_medium", ChaosConfig.BottledChaosCycleRecipes, "phonon_medium", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("magmatter", true, "magmatter", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("quark_gluon_plasma", true, "quark_gluon_plasma", BottledChaosRecipePage.DEFAULT),
            new TagDefinition("rocket_fuel", true, "rocket_fuel", BottledChaosRecipePage.ROCKET_FUEL),
            new TagDefinition("naquadah_fuel_1", true, "naquadah_fuel_1", BottledChaosRecipePage.NAQUADAH_FUEL),
            new TagDefinition("naquadah_fuel_2", true, "naquadah_fuel_2", BottledChaosRecipePage.NAQUADAH_FUEL),
            new TagDefinition("pbi", true, "pbi", BottledChaosRecipePage.PBI)
        };
        // spotless:on
        private static final TagDefinition[] ACTIVE = Arrays.stream(DEFINITIONS)
            .filter(TagDefinition::enabled)
            .toArray(TagDefinition[]::new);
        public static final int ROUTE_COUNT = ACTIVE.length;
        public static final long ALL_ROUTE_MASK = (1L << ROUTE_COUNT) - 1;
        private static final int[] DISPLAY_ORDER = IntStream.range(0, ROUTE_COUNT)
            .toArray();
        private static final Map<String, Integer> TAG_INDICES = new HashMap<>();

        static {
            if (ROUTE_COUNT >= Long.SIZE) throw new IllegalStateException("Too many credential tags for a route mask");
            for (int index = 0; index < ROUTE_COUNT; index++) {
                if (TAG_INDICES.put(ACTIVE[index].tag(), index) != null)
                    throw new IllegalStateException("Duplicate credential tag: " + ACTIVE[index].tag());
            }
        }

        private static volatile int[] registeredCapacityCosts;

        static void publishCapacityCosts(int[] costs) {
            if (costs.length != ROUTE_COUNT) throw new IllegalArgumentException("incomplete credential capacity costs");
            for (int cost : costs)
                if (cost <= 0) throw new IllegalArgumentException("invalid credential capacity cost");
            registeredCapacityCosts = costs.clone();
        }

        public static int[] displayOrder() {
            return DISPLAY_ORDER.clone();
        }

        /** Honeycomb variants share their production tag's position, regardless of the authorization bit. */
        public static int displayIndex(long requiredTagMask) {
            int tagIndex = ownerTagIndex(requiredTagMask);
            return tagIndex < ROUTE_COUNT ? tagIndex : Integer.MAX_VALUE;
        }

        public static String tag(int index) {
            return index >= 0 && index < ROUTE_COUNT ? ACTIVE[index].tag() : null;
        }

        public static int indexForTag(String tag) {
            return TAG_INDICES.getOrDefault(tag, -1);
        }

        /** Processing families are independent of NEI pages; additional honeycomb authorization keeps its owner. */
        public static String processingFamily(long requiredTagMask) {
            int index = ownerTagIndex(requiredTagMask);
            return index >= 0 && index < ROUTE_COUNT ? ACTIVE[index].family() : null;
        }

        /** Shared production owner for honeycomb authorization, runtime families and presentation. */
        public static int ownerTagIndex(long requiredTagMask) {
            if (HAS_HONEYCOMBS) requiredTagMask &= ~(1L << indexForTag("honeycomb"));
            return Long.numberOfTrailingZeros(requiredTagMask);
        }

        public static BottledChaosRecipePage displayPage(int index) {
            return index >= 0 && index < ROUTE_COUNT ? ACTIVE[index].page() : BottledChaosRecipePage.DEFAULT;
        }

        public static String translationKey(int index) {
            String name = tag(index);
            return name == null ? null : "gtnr.bottleinchaos.shared.route." + name;
        }

        /** One representative per NEI page; unrelated routes on the main page remain independent. */
        public static List<String> groupedTranslationKeys(long mask) {
            Map<String, Integer> lastByPage = new HashMap<>();
            for (int index = 0; index < ROUTE_COUNT; index++) {
                if ((mask & (1L << index)) == 0) continue;
                lastByPage.put(displayPage(index).groupKey(tag(index)), index);
            }
            long representatives = 0;
            for (int index : lastByPage.values()) representatives |= 1L << index;
            List<String> keys = new ArrayList<>();
            for (int index = 0; index < ROUTE_COUNT; index++) {
                if ((representatives & (1L << index)) == 0) continue;
                keys.add(displayPage(index).summaryKey(translationKey(index)));
            }
            if (keys.isEmpty()) keys.add("gtnr.machine.BottleInChaos.info.unbound");
            return keys;
        }

        public static long readRouteMask(NBTTagCompound tag) {
            if (tag == null) return 0;
            NBTTagList tags = tag.getTagList(TAGS_NBT_KEY, 8);
            long mask = 0;
            for (int i = 0; i < tags.tagCount(); i++) {
                int index = indexForTag(tags.getStringTagAt(i));
                if (index >= 0) mask |= 1L << index;
            }
            return mask;
        }

        public static void writeRouteMask(NBTTagCompound tag, long routeMask) {
            NBTTagList tags = new NBTTagList();
            for (int index = 0; index < ROUTE_COUNT; index++) {
                if ((routeMask & (1L << index)) != 0) tags.appendTag(new NBTTagString(tag(index)));
            }
            tag.setTag(TAGS_NBT_KEY, tags);
        }

        /** Every selected route pays its registered cost independently. */
        public static int getRequiredWeight(long routeMask) {
            if (routeMask == 0) return 0;
            int[] costs = registeredCapacityCosts;
            if (costs == null) return Integer.MAX_VALUE;
            int weight = 0;
            for (int index = 0; index < ROUTE_COUNT; index++) {
                if ((routeMask & (1L << index)) != 0) weight = Math.addExact(weight, costs[index]);
            }
            return weight;
        }
    }

    /**
     * Credential identity: tag/text key, runtime mask bit, binding marker and display template.
     * Material templates are accessed after registration, independently of frame metadata and chain entry order.
     */
    public record CredentialRoute(int index, String tag, Object root, Object display, String translationKey) {

        public long mask() {
            return 1L << index;
        }

        public ItemStack rootStartStack() {
            return root instanceof ChaosItemKey item ? item.getItemStack()
                : GTUtility.fillFluidContainer(
                    new FluidStack((Fluid) root, 1000),
                    ItemList.Cell_Universal_Fluid.get(1),
                    false,
                    true);
        }

        public ItemStack displayStack() {
            return display instanceof ChaosItemKey item ? item.getItemStack()
                : GTUtility.getFluidDisplayStack((Fluid) display);
        }
    }

    /** Access after material registration, like the other recipe templates in this class. */
    public static final class CredentialRoutes {

        private static final CredentialRoute[] ROUTES = new CredentialRoute[CredentialModel.ROUTE_COUNT];

        static {
            // spotless:off
            register("wood", WoodLog, WoodLog);
            register("pbi", Benzene, PolybenzimidazoleMolten);
            register("rocket_fuel", CarbonDust, GTPPFluids.RP1RocketFuel);
            register("naquadah_fuel_1", GGMaterial.lightNaquadahFuel.getFluidOrGas(1).getFluid(), GGMaterial.naquadahBasedFuelMkI.getFluidOrGas(1).getFluid());
            register("naquadah_fuel_2", GGMaterial.lightNaquadahFuel.getFluidOrGas(1).getFluid(), GGMaterial.naquadahBasedFuelMkII.getFluidOrGas(1).getFluid());
            register("petrochemical", Materials.Oil.getFluid(1).getFluid(), Materials.Oil.getFluid(1).getFluid());
            if (CredentialModel.HAS_HONEYCOMBS) register("honeycomb", recipeItemId(GTBees.combs.getStackForType(CombType.INDIUM, 1)), recipeItemId(GTBees.combs.getStackForType(CombType.INDIUM, 1)));
            register("titanium", RutileDust, TitaniumIngot);
            register("tungsten", CalciumTungstateDust, TungstenTrioxideDust);
            register("indium", SphaleriteDust, IndiumDust);
            register("platinum_metallic_powder", PlatinumMetallicPowder, PlatinumMetallicPowder);
            register("palladium_metallic_powder", PalladiumMetallicPowder, PalladiumMetallicPowder);
            register("platinum_residue", PlatinumResidueDust, PlatinumResidueDust);
            register("crude_rhodium_metal", CrudeRhodiumMetalDust, CrudeRhodiumMetalDust);
            register("leach_residue", LeachResidueDust, LeachResidueDust);
            register("rarest_metal_residue", RarestMetalResidueDust, RarestMetalResidueDust);
            register("iridium_metal_residue", IridiumMetalResidueDust, IridiumMetalResidueDust);
            register("monazite", MonaziteDust, DilutedMonaziteMud);
            register("bastnasite", BastnasiteDust, RoastedRareEarthOxidesDust);
            register("cerium", CeriumRichMixtureDust, CeriumDust);
            register("samarium", SamariumOreConcentrateDust, SamariumDust);
            register("nano_extraction", recipeItemId(GTOreDictUnificator.get(OrePrefixes.crushed, Materials.Lanthanum, 1)), Materials.Lanthanum.getMolten(1).getFluid());
            register("naquadah", NaquadahOxideMixtureDust, NaquadahDust);
            register("enriched_naquadah", EnrichedNaquadahOxideMixtureDust, EnrichedNaquadahDust);
            register("naquadria", NaquadriaOxideMixtureDust, NaquadriaDust);
            register("naquadah_fuel_preparation", NaquadriaDust, GGMaterial.heavyNaquadahFuel.getFluidOrGas(1).getFluid());
            register("naquadah_fuel_3", recipeItemId(GGMaterial.extremelyUnstableNaquadah.get(OrePrefixes.dust, 1)), GGMaterial.naquadahBasedFuelMkIII.getFluidOrGas(1).getFluid());
            register("naquadah_fuel_4", GGMaterial.naquadahBasedFuelMkIII.getFluidOrGas(1).getFluid(), GGMaterial.naquadahBasedFuelMkIV.getFluidOrGas(1).getFluid());
            register("naquadah_fuel_5", GGMaterial.naquadahBasedFuelMkIV.getFluidOrGas(1).getFluid(), GGMaterial.naquadahBasedFuelMkV.getFluidOrGas(1).getFluid());
            register("naquadah_fuel_6", GGMaterial.naquadahBasedFuelMkV.getFluidOrGas(1).getFluid(), GGMaterial.naquadahBasedFuelMkVI.getFluidOrGas(1).getFluid());
            register("fusion", recipeItemId(Materials.Cobalt.getDust(1)), MaterialsElements.STANDALONE.ASTRAL_TITANIUM.getPlasma());
            if (Mods.EtFuturumRequiem.isModLoaded()) register("ancient_debris", AncientDebris, NetheriteNanoparticles);
            register("isamill", MilledSphalerite, MilledSphalerite);
            register("purified_water_1", recipeItemId(Materials.Grade1PurifiedWater.getCells(1)), recipeItemId(Materials.Grade1PurifiedWater.getCells(1)));
            register("purified_water_2", recipeItemId(Materials.Grade2PurifiedWater.getCells(1)), recipeItemId(Materials.Grade2PurifiedWater.getCells(1)));
            register("prismatic_acid", PrismarineShard, PrismaticAcid);
            register("purified_water_3", recipeItemId(Materials.Grade3PurifiedWater.getCells(1)), recipeItemId(Materials.Grade3PurifiedWater.getCells(1)));
            register("purified_water_4", recipeItemId(Materials.Grade4PurifiedWater.getCells(1)), recipeItemId(Materials.Grade4PurifiedWater.getCells(1)));
            register("kevlar", KevlarFiber, KevlarPlate);
            register("super_heavy_radox", SuperHeavyRadox, SuperHeavyRadox);
            register("heavy_radox", HeavyRadox, HeavyRadox);
            register("light_radox", LightRadox, LightRadox);
            register("super_light_radox", SuperLightRadox, SuperLightRadox);
            register("purified_water_5", recipeItemId(Materials.Grade5PurifiedWater.getCells(1)), recipeItemId(Materials.Grade5PurifiedWater.getCells(1)));
            register("purified_water_6", recipeItemId(Materials.Grade6PurifiedWater.getCells(1)), recipeItemId(Materials.Grade6PurifiedWater.getCells(1)));
            register("growth_medium_sterilized", GrowthMediumSterilized, GrowthMediumSterilized);
            register("stem_cells", StemCells, StemCells);
            register("seaweed_broth", SeaweedBroth, SeaweedBroth);
            register("tau_ceti_e_seaweed_extract", TauCetiESeaweedExtract, TauCetiESeaweedExtract);
            register("bio_medium_sterilized", BioMediumSterilized, BioMediumSterilized);
            register("bio_cells", BioCells, BioCells);
            register("xenoxene", Xenoxene, RawRadox);
            register("purified_water_7", recipeItemId(Materials.Grade7PurifiedWater.getCells(1)), recipeItemId(Materials.Grade7PurifiedWater.getCells(1)));
            register("purified_water_8", recipeItemId(Materials.Grade8PurifiedWater.getCells(1)), recipeItemId(Materials.Grade8PurifiedWater.getCells(1)));
            if (ChaosConfig.BottledChaosCycleRecipes) {
                register("crystal_cycle", RawCrystalChip, RawCrystalChip);
                register("cerium_lutetium_garnet", CeriumLutetiumAluminiumOxygenBlendDust, ExquisiteCeriumLutetiumAluminiumGarnet);
                register("tesseract", Materials.ExcitedDTRC.getFluid(1).getFluid(), recipeItemId(ItemList.Tesseract.get(1)));
                if (CredentialModel.HAS_ETERNITY) register("eternity", EternalSingularity, EternityMolten);
                register("phonon_medium", MagMatterRound, PhononMedium);
            }
            register("magmatter", recipeItemId(Materials.MagMatter.getDust(1)), recipeItemId(Materials.MagMatter.getDust(1)));
            register("quark_gluon_plasma", recipeItemId(Materials.QuarkGluonPlasma.getCells(1)), recipeItemId(Materials.QuarkGluonPlasma.getCells(1)));
            // spotless:on
        }

        private static void register(String tag, Object root, Object display) {
            int index = CredentialModel.indexForTag(tag);
            ROUTES[index] = new CredentialRoute(index, tag, root, display, CredentialModel.translationKey(index));
        }

        public static CredentialRoute get(int index) {
            return index >= 0 && index < CredentialModel.ROUTE_COUNT ? ROUTES[index] : null;
        }
    }

    /** Exact item, metadata and NBT identity; marker stack size has no bearing on its route. */
    public static long getRouteMaskForMarker(ItemStack marker) {
        if (marker == null || marker.getItem() == null) return 0;
        ChaosItemKey id = ChaosItemKey.create(marker);
        FluidStack contents = GTUtility.getFluidForFilledItem(marker, true);
        for (CredentialRoute route : CredentialRoutes.ROUTES) {
            if (route.root() instanceof ChaosItemKey item && item.equals(id)
                || route.root() instanceof Fluid fluid && contents != null && contents.getFluid() == fluid)
                return route.mask();
        }
        return 0;
    }

    /** May be used before material registration without initializing the material IDs in the outer class. */
    public static final class RecipePools {

        // spotless:off
        public static final RecipeMap<?> BlastFurnace = RecipeMaps.blastFurnaceRecipes;
        public static final RecipeMap<?> ArcFurnace = RecipeMaps.arcFurnaceRecipes;
        public static final RecipeMap<?> LargeChemicalReactor = RecipeMaps.multiblockChemicalReactorRecipes;
        public static final RecipeMap<?> Sifter = RecipeMaps.sifterRecipes;
        public static final RecipeMap<?> DistillationTower = RecipeMaps.distillationTowerRecipes;
        public static final RecipeMap<?> Distillery = RecipeMaps.distilleryRecipes;
        public static final RecipeMap<?> Brewery = RecipeMaps.brewingRecipes;
        public static final RecipeMap<?> Fermenter = RecipeMaps.fermentingRecipes;
        public static final RecipeMap<?> MixerNonCell = RecipeMaps.mixerNonCellRecipes;
        public static final RecipeMap<?> ElectrolyzerNonCell = RecipeMaps.electrolyzerNonCellRecipes;
        public static final RecipeMap<?> CentrifugeNonCell = RecipeMaps.centrifugeNonCellRecipes;
        public static final RecipeMap<?> FluidHeater = RecipeMaps.fluidHeaterRecipes;
        public static final RecipeMap<?> FluidSolidifier = RecipeMaps.fluidSolidifierRecipes;
        public static final RecipeMap<?> FluidExtractor = RecipeMaps.fluidExtractionRecipes;
        public static final RecipeMap<?> Bender = RecipeMaps.benderRecipes;
        public static final RecipeMap<?> Compressor = RecipeMaps.compressorRecipes;
        public static final RecipeMap<?> Cutter = RecipeMaps.cutterRecipes;
        public static final RecipeMap<?> Furnace = RecipeMaps.furnaceRecipes;
        public static final RecipeMap<?> Macerator = RecipeMaps.maceratorRecipes;
        public static final RecipeMap<?> ForgeHammer = RecipeMaps.hammerRecipes;
        public static final RecipeMap<?> ChemicalBath = RecipeMaps.chemicalBathRecipes;
        public static final RecipeMap<?> ChemicalDehydratorNonCell = RecipeMaps.chemicalDehydratorNonCellRecipes;
        public static final RecipeMap<?> VacuumFreezer = RecipeMaps.vacuumFreezerRecipes;
        public static final RecipeMap<?> OilCracker = RecipeMaps.crackingRecipes;
        public static final RecipeMap<?> VacuumFurnace = RecipeMaps.vacuumFurnaceRecipes;
        public static final RecipeMap<?> FlotationCell = RecipeMaps.flotationCellRecipes;
        public static final RecipeMap<?> Milling = RecipeMaps.millingRecipes;
        public static final RecipeMap<?> Digester = LanthanidesRecipeMaps.digesterRecipes;
        public static final RecipeMap<?> DissolutionTank = LanthanidesRecipeMaps.dissolutionTankRecipes;
        public static final RecipeMap<?> ChemicalPlant = RecipeMaps.chemicalPlantRecipes;
        public static final RecipeMap<?> LaserEngraver = RecipeMaps.laserEngraverRecipes;
        public static final RecipeMap<?> AlloyBlastSmelter = RecipeMaps.alloyBlastSmelterRecipes;
        public static final RecipeMap<?> ElectromagneticSeparator = RecipeMaps.electroMagneticSeparatorRecipes;
        public static final RecipeMap<?> ThermalCentrifuge = RecipeMaps.thermalCentrifugeRecipes;
        public static final RecipeMap<?> Autoclave = RecipeMaps.autoclaveRecipes;
        public static final RecipeMap<?> NeutronActivator = GTNRRecipeMaps.NeutronActivatorWithEURecipeMap;
        public static final RecipeMap<?> BacterialVat = BartWorksRecipeMaps.bacterialVatRecipes;
        public static final RecipeMap<?> Extractor = RecipeMaps.extractorRecipes;
        public static final RecipeMap<?> ElectricImplosionCompressor = BartWorksRecipeMaps.electricImplosionCompressorRecipes;
        public static final RecipeMap<?> Assembler = RecipeMaps.assemblerRecipes;
        public static final RecipeMap<?> IndustrialCokeOven = RecipeMaps.industrialCokeOvenRecipes;
        public static final RecipeMap<?> QuantumForceTransformer = RecipeMaps.quantumForceTransformerRecipes;
        public static final RecipeMap<?> FusionCrafting = DEFCRecipes.fusionCraftingRecipes;
        public static final RecipeMap<?> Fusion = RecipeMaps.fusionRecipes;
        public static final RecipeMap<?> GodforgePlasma = TecTechRecipeMaps.godforgePlasmaRecipes;
        // spotless:on
    }

    // spotless:off
    // Elemental full dusts, ordered by atomic number. Initializers also register native dust aliases.
    public static final ChaosItemKey LithiumDust = dust(Materials.Lithium);
    public static final ChaosItemKey BerylliumDust = dust(Materials.Beryllium);
    public static final ChaosItemKey BoronDust = dust(Materials.Boron);
    public static final ChaosItemKey CarbonDust = dust(Materials.Carbon);
    public static final ChaosItemKey SodiumDust = dust(Materials.Sodium);
    public static final ChaosItemKey MagnesiumDust = dust(Materials.Magnesium);
    public static final ChaosItemKey AluminiumDust = dust(Materials.Aluminium);
    public static final ChaosItemKey SiliconDust = dust(Materials.Silicon);
    public static final ChaosItemKey PhosphorusDust = dust(Materials.Phosphorus);
    public static final ChaosItemKey SulfurDust = dust(Materials.Sulfur);
    public static final ChaosItemKey PotassiumDust = dust(Materials.Potassium);
    public static final ChaosItemKey CalciumDust = dust(Materials.Calcium);
    public static final ChaosItemKey ScandiumDust = dust(Materials.Scandium);
    public static final ChaosItemKey TitaniumDust = dust(Materials.Titanium);
    public static final ChaosItemKey VanadiumDust = dust(Materials.Vanadium);
    public static final ChaosItemKey ChromiumDust = dust(Materials.Chrome);
    public static final ChaosItemKey ManganeseDust = dust(Materials.Manganese);
    public static final ChaosItemKey IronDust = dust(Materials.Iron);
    public static final ChaosItemKey CobaltDust = dust(Materials.Cobalt);
    public static final ChaosItemKey NickelDust = dust(Materials.Nickel);
    public static final ChaosItemKey CopperDust = dust(Materials.Copper);
    public static final ChaosItemKey ZincDust = dust(Materials.Zinc);
    public static final ChaosItemKey GalliumDust = dust(Materials.Gallium);
    public static final ChaosItemKey GermaniumDust = dust(MaterialsElements.getInstance().GERMANIUM.getDust(1));
    public static final ChaosItemKey ArsenicDust = dust(Materials.Arsenic);
    public static final ChaosItemKey SeleniumDust = dust(MaterialsElements.getInstance().SELENIUM.getDust(1));
    public static final ChaosItemKey RubidiumDust = dust(Materials.Rubidium);
    public static final ChaosItemKey StrontiumDust = dust(Materials.Strontium);
    public static final ChaosItemKey YttriumDust = dust(Materials.Yttrium);
    public static final ChaosItemKey ZirconiumDust = dust(WerkstoffLoader.Zirconium);
    public static final ChaosItemKey NiobiumDust = dust(Materials.Niobium);
    public static final ChaosItemKey MolybdenumDust = dust(Materials.Molybdenum);
    public static final ChaosItemKey TechnetiumDust = dust(MaterialsElements.getInstance().TECHNETIUM.getDust(1));
    public static final ChaosItemKey RutheniumDust = dust(WerkstoffLoader.Ruthenium);
    public static final ChaosItemKey RhodiumDust = dust(WerkstoffLoader.Rhodium);
    public static final ChaosItemKey PalladiumDust = dust(Materials.Palladium);
    public static final ChaosItemKey SilverDust = dust(Materials.Silver);
    public static final ChaosItemKey CadmiumDust = dust(Materials.Cadmium);
    public static final ChaosItemKey IndiumDust = dust(Materials.Indium);
    public static final ChaosItemKey TinDust = dust(Materials.Tin);
    public static final ChaosItemKey AntimonyDust = dust(Materials.Antimony);
    public static final ChaosItemKey TelluriumDust = dust(Materials.Tellurium);
    public static final ChaosItemKey IodineDust = dust(MaterialsElements.getInstance().IODINE.getDust(1));
    public static final ChaosItemKey CaesiumDust = dust(Materials.Caesium);
    public static final ChaosItemKey BariumDust = dust(Materials.Barium);
    public static final ChaosItemKey LanthanumDust = dust(Materials.Lanthanum);
    public static final ChaosItemKey CeriumDust = dust(Materials.Cerium);
    public static final ChaosItemKey PraseodymiumDust = dust(Materials.Praseodymium);
    public static final ChaosItemKey NeodymiumDust = dust(Materials.Neodymium);
    public static final ChaosItemKey PromethiumDust = dust(Materials.Promethium);
    public static final ChaosItemKey SamariumDust = dust(Materials.Samarium);
    public static final ChaosItemKey EuropiumDust = dust(Materials.Europium);
    public static final ChaosItemKey GadoliniumDust = dust(Materials.Gadolinium);
    public static final ChaosItemKey TerbiumDust = dust(Materials.Terbium);
    public static final ChaosItemKey DysprosiumDust = dust(Materials.Dysprosium);
    public static final ChaosItemKey HolmiumDust = dust(Materials.Holmium);
    public static final ChaosItemKey ErbiumDust = dust(Materials.Erbium);
    public static final ChaosItemKey ThuliumDust = dust(Materials.Thulium);
    public static final ChaosItemKey YtterbiumDust = dust(Materials.Ytterbium);
    public static final ChaosItemKey LutetiumDust = dust(Materials.Lutetium);
    public static final ChaosItemKey HafniumDust = dust(WerkstoffMaterialPool.Hafnium);
    public static final ChaosItemKey TantalumDust = dust(Materials.Tantalum);
    public static final ChaosItemKey TungstenDust = dust(Materials.Tungsten);
    public static final ChaosItemKey RheniumDust = dust(MaterialsElements.getInstance().RHENIUM.getDust(1));
    public static final ChaosItemKey OsmiumDust = dust(Materials.Osmium);
    public static final ChaosItemKey IridiumDust = dust(Materials.Iridium);
    public static final ChaosItemKey PlatinumDust = dust(Materials.Platinum);
    public static final ChaosItemKey GoldDust = dust(Materials.Gold);
    public static final ChaosItemKey ThalliumDust = dust(MaterialsElements.getInstance().THALLIUM.getDust(1));
    public static final ChaosItemKey LeadDust = dust(Materials.Lead);
    public static final ChaosItemKey BismuthDust = dust(Materials.Bismuth);
    public static final ChaosItemKey PoloniumDust = dust(MaterialsElements.getInstance().POLONIUM.getDust(1));
    public static final ChaosItemKey FranciumDust = dust(Materials.Francium);
    public static final ChaosItemKey RadiumDust = dust(MaterialsElements.getInstance().RADIUM.getDust(1));
    public static final ChaosItemKey ThoriumDust = dust(Materials.Thorium);
    public static final ChaosItemKey ProtactiniumDust = dust(MaterialsElements.getInstance().PROTACTINIUM.getDust(1));
    public static final ChaosItemKey UraniumDust = dust(Materials.Uranium);
    public static final ChaosItemKey NeptuniumDust = dust(MaterialsElements.getInstance().NEPTUNIUM.getDust(1));
    public static final ChaosItemKey PlutoniumDust = dust(Materials.Plutonium);
    public static final ChaosItemKey AmericiumDust = dust(Materials.Americium);
    public static final ChaosItemKey CuriumDust = dust(MaterialsElements.getInstance().CURIUM.getDust(1));
    public static final ChaosItemKey CaliforniumDust = dust(WerkstoffLoader.Californium);
    public static final ChaosItemKey FermiumDust = dust(MaterialsElements.getInstance().FERMIUM.getDust(1));
    public static final ChaosItemKey FleroviumDust = dust(Materials.Flerovium);

    // Fictional elemental full dusts used by the naquadah processing family.
    public static final ChaosItemKey NaquadahDust = dust(Materials.Naquadah);
    public static final ChaosItemKey EnrichedNaquadahDust = dust(Materials.NaquadahEnriched);
    public static final ChaosItemKey NaquadriaDust = dust(Materials.Naquadria);
    public static final ChaosItemKey TriniumDust = dust(Materials.Trinium);
    public static final ChaosItemKey AdamantiumDust = dust(Materials.Adamantium);

    // Elemental ingots currently used by recipes.
    public static final ChaosItemKey PotassiumIngot = ChaosItemKey.create(Materials.Potassium.getIngots(1));
    public static final ChaosItemKey StrontiumIngot = ChaosItemKey.create(Materials.Strontium.getIngots(1));
    public static final ChaosItemKey TitaniumIngot = ChaosItemKey.create(Materials.Titanium.getIngots(1));
    public static final ChaosItemKey HotTitaniumIngot = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.ingotHot, Materials.Titanium, 1));
    public static final ChaosItemKey EnrichedNaquadahIngot = ChaosItemKey.create(Materials.NaquadahEnriched.getIngots(1));
    public static final ChaosItemKey HotEnrichedNaquadahIngot = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.ingotHot, Materials.NaquadahEnriched, 1));
    public static final ChaosItemKey NaquadriaIngot = ChaosItemKey.create(Materials.Naquadria.getIngots(1));
    public static final ChaosItemKey HotNaquadriaIngot = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.ingotHot, Materials.Naquadria, 1));
    public static final ChaosItemKey HafniumIngot = ChaosItemKey.create(WerkstoffMaterialPool.Hafnium.get(OrePrefixes.ingot, 1));
    public static final ChaosItemKey HotHafniumIngot = ChaosItemKey.create(WerkstoffMaterialPool.Hafnium.get(OrePrefixes.ingotHot, 1));
    public static final ChaosItemKey ZirconiumIngot = ChaosItemKey.create(WerkstoffLoader.Zirconium.get(OrePrefixes.ingot, 1));
    public static final ChaosItemKey HotZirconiumIngot = ChaosItemKey.create(WerkstoffLoader.Zirconium.get(OrePrefixes.ingotHot, 1));

    // Alloy ingots used by the crystalline-alloy input path.
    public static final ChaosItemKey CrystallineAlloyIngot = ChaosItemKey.create(Materials.CrystallineAlloy.getIngots(1));
    public static final ChaosItemKey HotCrystallineAlloyIngot = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.ingotHot, Materials.CrystallineAlloy, 1));
    public static final ChaosItemKey PulsatingIronIngot = ChaosItemKey.create(Materials.PulsatingIron.getIngots(1));

    // Elemental isotope full dusts.
    public static final ChaosItemKey Lithium7Dust = dust(MaterialsElements.getInstance().LITHIUM7.getDust(1));
    public static final ChaosItemKey Uranium232Dust = dust(MaterialsElements.getInstance().URANIUM232.getDust(1));
    public static final ChaosItemKey Uranium233Dust = dust(MaterialsElements.getInstance().URANIUM233.getDust(1));
    public static final ChaosItemKey Uranium235Dust = dust(Materials.Uranium235);
    public static final ChaosItemKey Plutonium238Dust = dust(MaterialsElements.getInstance().PLUTONIUM238.getDust(1));
    public static final ChaosItemKey Plutonium241Dust = dust(Materials.Plutonium241);

    // Compound full dusts currently used by recipes.
    public static final ChaosItemKey AdamantiumOxideDust = dust(GGMaterial.adamantine);
    public static final ChaosItemKey CalciumChlorideDust = dust(WerkstoffLoader.CalciumChloride);
    public static final ChaosItemKey CalciumCarbonateDust = dust(GregtechItemList.CalciumCarbonateDust.get(1));
    public static final ChaosItemKey CalciumHydrideDust = dust(Materials.Calciumhydride);
    public static final ChaosItemKey CalciumDisilicideDust = dust(Materials.CalciumDisilicide);
    public static final ChaosItemKey IridiumChlorideDust = dust(WerkstoffLoader.IridiumChloride);
    public static final ChaosItemKey IridiumDioxideDust = dust(WerkstoffLoader.IridiumDioxide);
    public static final ChaosItemKey PotassiumDichromateDust = dust(Materials.Potassiumdichromate);
    public static final ChaosItemKey PotassiumDisulfateDust = dust(WerkstoffLoader.PotassiumDisulfate);
    public static final ChaosItemKey PotassiumHydroxideDust = dust(Materials.PotassiumHydroxide);
    public static final ChaosItemKey PotassiumNitrateDust = dust(Materials.Saltpeter);
    public static final ChaosItemKey RhodiumNitrateDust = dust(WerkstoffLoader.RHNitrate);
    public static final ChaosItemKey RhodiumIIIChlorideDust = dust(Materials.RhodiumChloride);
    public static final ChaosItemKey RutheniumTetroxideDust = dust(WerkstoffLoader.RutheniumTetroxide);
    public static final ChaosItemKey SiliconDioxideDust = dust(Materials.SiliconDioxide);
    public static final ChaosItemKey SodiumChlorideDust = dust(Materials.Salt);
    public static final ChaosItemKey SodiumHydrideDust = dust(Materials.SodiumHydride);
    public static final ChaosItemKey SodiumBorohydrideDust = dust(Materials.SodiumBorohydride);
    public static final ChaosItemKey SodiumMethoxideDust = dust(Materials.SodiumMethoxide);
    public static final ChaosItemKey SodiumHydroxideDust = dust(Materials.SodiumHydroxide);
    public static final ChaosItemKey SodiumAluminateDust = dust(Materials.SodiumAluminate);
    public static final ChaosItemKey AluminiumHydroxideDust = dust(Materials.Aluminiumhydroxide);
    public static final ChaosItemKey SodiumNitrateDust = dust(WerkstoffLoader.SodiumNitrate);
    public static final ChaosItemKey SodiumRuthenateDust = dust(WerkstoffLoader.SodiumRuthenate);
    public static final ChaosItemKey SodiumSulfateDust = dust(WerkstoffLoader.Sodiumsulfate);
    public static final ChaosItemKey ZincSulfateDust = dust(WerkstoffLoader.ZincSulfate);
    public static final ChaosItemKey AmmoniumNitrateDust = dust(BotWerkstoffMaterialPool.AmmoniumNitrate);
    public static final ChaosItemKey RedZirconDust = dust(WerkstoffLoader.RedZircon);
    public static final ChaosItemKey ThoriumDioxideDust = dust(WerkstoffLoader.Thorianit);
    public static final ChaosItemKey HafniumDioxideDust = dust(WerkstoffMaterialPool.Hafnia);
    public static final ChaosItemKey ZirconiumDioxideDust = dust(WerkstoffMaterialPool.Zirconia);
    public static final ChaosItemKey HafniumTetrachlorideDust = dust(WerkstoffMaterialPool.HafniumTetrachloride);
    public static final ChaosItemKey ZirconiumTetrachlorideDust = dust(WerkstoffMaterialPool.ZirconiumTetrachloride);
    public static final ChaosItemKey HafniumIodideDust = dust(WerkstoffMaterialPool.HafniumIodide);
    public static final ChaosItemKey MagnesiumChlorideDust = dust(Materials.Magnesiumchloride);
    public static final ChaosItemKey LithiumTungstateDust = dust(Materials.Tungstate);
    public static final ChaosItemKey ManganeseTungstateDust = dust(WerkstoffLoader.Huebnerit);
    public static final ChaosItemKey IronTungstateDust = dust(WerkstoffLoader.Ferberite);
    public static final ChaosItemKey CalciumTungstateDust = dust(Materials.Scheelite);
    public static final ChaosItemKey TungsticAcidDust = dust(BotWerkstoffMaterialPool.TungsticAcid);
    public static final ChaosItemKey TungstenTrioxideDust = dust(BotWerkstoffMaterialPool.TungstenTrioxide);
    public static final ChaosItemKey RutileDust = dust(Materials.Rutile);
    public static final ChaosItemKey SphaleriteDust = dust(Materials.Sphalerite);
    public static final ChaosItemKey GalenaDust = dust(Materials.Galena);
    public static final ChaosItemKey CalciteDust = dust(Materials.Calcite);
    public static final ChaosItemKey SodiumCarbonateDust = dust(Materials.SodiumCarbonate);
    public static final ChaosItemKey HafniumZirconiumOxideBlendDust = dust(WerkstoffMaterialPool.HafniaZirconiaBlend);
    public static final ChaosItemKey LowPurityHafniumDust = dust(WerkstoffMaterialPool.LowPurityHafnium);
    public static final ChaosItemKey OrangeMetalCatalyst = ChaosItemKey.create(GregtechItemList.OrangeMetalCatalyst.get(1));
    public static final ChaosItemKey SugarDust = dust(Materials.Sugar);
    public static final ChaosItemKey VanadiumPentoxideDust = dust(GGMaterial.vanadiumPentoxide);
    public static final ChaosItemKey CeriumDioxideDust = dust(WerkstoffMaterialPool.CeriumDioxide);
    public static final ChaosItemKey CeriumChlorideDust = dust(WerkstoffMaterialPool.CeriumChloride);
    public static final ChaosItemKey CeriumOxalateDust = dust(WerkstoffMaterialPool.CeriumOxalate);
    public static final ChaosItemKey CeriumIIIOxideDust = dust(WerkstoffMaterialPool.CeriumIIIOxide);
    public static final ChaosItemKey EuropiumIIIOxideDust = dust(WerkstoffMaterialPool.EuropiumIIIOxide);
    public static final ChaosItemKey EuropiumOxideDust = dust(WerkstoffMaterialPool.EuropiumOxide);
    public static final ChaosItemKey LanthanumChlorideDust = dust(WerkstoffMaterialPool.LanthanumChloride);
    public static final ChaosItemKey NeodymiumOxideDust = dust(WerkstoffMaterialPool.NeodymiumOxide);
    public static final ChaosItemKey TerbiumNitrateDust = dust(WerkstoffMaterialPool.TerbiumNitrate);
    public static final ChaosItemKey StrontiumHydroxideDust = dust(MaterialMisc.STRONTIUM_HYDROXIDE.getDust(1));
    public static final ChaosItemKey TitaniumTrifluorideDust = dust(GGMaterial.titaniumTrifluoride);
    public static final ChaosItemKey NaquadahDioxideDust = dust(GGMaterial.naquadahine);
    public static final ChaosItemKey EnrichedNaquadahSulfateDust = dust(GGMaterial.enrichedNaquadahSulphate);
    public static final ChaosItemKey NaquadriaSulfateDust = dust(GGMaterial.naquadriaSulphate);
    public static final ChaosItemKey MagnesiumSulfateDust = dust(GGMaterial.magnesiumSulphate);
    public static final ChaosItemKey TriniumSulfateDust = dust(GGMaterial.triniumSulphate);
    public static final ChaosItemKey GalliumHydroxideDust = dust(GGMaterial.galliumHydroxide);
    public static final ChaosItemKey IndiumPhosphateDust = dust(GGMaterial.indiumPhosphate);
    public static final ChaosItemKey SodiumOxideDust = dust(Materials.SodiumOxide);
    public static final ChaosItemKey QuicklimeDust = dust(Materials.Quicklime);
    public static final ChaosItemKey BariumSulfateDust = dust(Materials.Barite);
    public static final ChaosItemKey TricalciumPhosphateDust = dust(Materials.TricalciumPhosphate);
    public static final ChaosItemKey PhosphateDust = dust(Materials.Phosphate);
    public static final ChaosItemKey CalciumFluorideDust = dust(WerkstoffMaterialPool.CalciumFluoride);
    public static final ChaosItemKey FluorsparDust = dust(WerkstoffLoader.Fluorspar);
    public static final ChaosItemKey BoraxDust = dust(Materials.Borax);
    public static final ChaosItemKey TriphenylphosphineDust = dust(Materials.Triphenylphosphene);

    // Purification reagents and reusable optical tools.
    public static final ChaosItemKey PreActivatedCarbonDust = dust(Materials.PreActivatedCarbon);
    public static final ChaosItemKey DirtyActivatedCarbonDust = dust(Materials.DirtyActivatedCarbon);
    public static final ChaosItemKey ActivatedCarbonDust = dust(Materials.ActivatedCarbon);
    public static final ChaosItemKey ActivatedCarbonFilterMesh = ChaosItemKey.create(ItemList.ActivatedCarbonFilterMesh.get(1));
    public static final ChaosItemKey ZincFoil = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.foil, Materials.Zinc, 1));
    public static final ChaosItemKey ZincIngot = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Zinc, 1));
    public static final ChaosItemKey BlueTopazLens = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.lens, Materials.BlueTopaz, 1));
    public static final ChaosItemKey LapisDust = dust(Materials.Lapis);
    public static final ChaosItemKey CallistoIceDust = dust(Materials.CallistoIce);
    public static final ChaosItemKey LedoxDust = dust(Materials.Ledox);


    // Biological feedstocks, polymer intermediates and non-consumed tools.
    public static final ChaosItemKey RawMeatDust = dust(Materials.MeatRaw);
    public static final ChaosItemKey BoneDust = dust(Materials.Bone);
    public static final ChaosItemKey OsmiridiumDust = dust(Materials.Osmiridium);
    public static final ChaosItemKey MysteriousCrystalDust = dust(Materials.MysteriousCrystal);
    public static final ChaosItemKey TritaniumDust = dust(Materials.Tritanium);
    public static final ChaosItemKey CosmicNeutroniumDust = dust(Materials.CosmicNeutronium);
    public static final ChaosItemKey MytrylDust = dust(Materials.Mytryl);
    public static final ChaosItemKey RubyDust = dust(Materials.Ruby);
    public static final ChaosItemKey AshDust = dust(Materials.Ash);
    public static final ChaosItemKey AshDarkDust = dust(Materials.AshDark);
    public static final ChaosItemKey EnergiumDust = ChaosItemKey.create(ItemList.IC2_Energium_Dust.get(1));
    public static final ChaosItemKey StemCells = ChaosItemKey.create(ItemList.Circuit_Chip_Stemcell.get(1));
    public static final ChaosItemKey BioCells = ChaosItemKey.create(ItemList.Circuit_Chip_Biocell.get(1));
    public static final ChaosItemKey Agar = modItem(Mods.NewHorizonsCoreMod, "GTNHBioItems", 2);
    public static final ChaosItemKey UnknownCrystal = modItem(Mods.GalaxySpace, "item.UnknowCrystal", 0);
    public static final ChaosItemKey TauCetiESeaweed = modItem(Mods.GalaxySpace, "tcetiedandelions", 0);
    public static final ChaosItemKey TauCetiESeaweedExtract = modItem(Mods.NewHorizonsCoreMod, "TCetiESeaweedExtract", 0);
    public static final ChaosItemKey PamSeaweed = modItem(Mods.PamsHarvestCraft, "seaweedItem", 0);
    public static final ChaosItemKey KevlarPlate = ChaosItemKey.create(Materials.Kevlar.getPlates(1));
    public static final ChaosItemKey KevlarFiber = ChaosItemKey.create(ItemList.KevlarFiber.get(1));
    public static final ChaosItemKey WovenKevlar = ChaosItemKey.create(ItemList.WovenKevlar.get(1));
    public static final ChaosItemKey GoldenBrownCelluloseFiber = ChaosItemKey.create(GregtechItemList.GoldenBrownCelluloseFiber.get(1));
    public static final ChaosItemKey RedCelluloseFiber = ChaosItemKey.create(GregtechItemList.RedCelluloseFiber.get(1));
    public static final ChaosItemKey GoldenBrownAlgaeBiomass = ChaosItemKey.create(GregtechItemList.GoldenBrownAlgaeBiomass.get(1));
    public static final ChaosItemKey RedAlgaeBiomass = ChaosItemKey.create(GregtechItemList.RedAlgaeBiomass.get(1));
    public static final ChaosItemKey FormaldehydeCatalyst = ChaosItemKey.create(GregtechItemList.FormaldehydeCatalyst.get(1));
    public static final ChaosItemKey KevlarCatalystDust = dust(Materials.KevlarCatalyst);
    public static final ChaosItemKey PentaerythritolDust = dust(Materials.Pentaerythritol);
    public static final ChaosItemKey DiphenylmethaneDiisocyanateDust = dust(Materials.DiphenylmethaneDiisocyanate);
    public static final ChaosItemKey ParaphenylenediamineDust = dust(Materials.ParaPhenylenediamine);
    public static final ChaosItemKey TerephthaloylChlorideDust = dust(Materials.TerephthaloylChloride);
    public static final ChaosItemKey OrganorhodiumCatalystDust = dust(Materials.OrganorhodiumCatalyst);
    public static final ChaosItemKey CopperIIOxideDust = dust(Materials.CupricOxide);
    public static final ChaosItemKey BismuthIIIOxideDust = dust(Materials.BismuthIIIOxide);
    public static final ChaosItemKey AluminiumOxideDust = dust(Materials.Aluminiumoxide);
    public static final ChaosItemKey ButynediolDust = dust(Materials.IIButinIIVdiol);
    public static final ChaosItemKey ActivatedRaneyNickelDust = dust(Materials.RaneyNickelActivated);
    public static final ChaosItemKey CalciumCarbideDust = dust(Materials.CalciumCarbide);
    public static final ChaosItemKey CobaltNaphthenateDust = dust(Materials.CobaltIINaphthenate);
    public static final ChaosItemKey CobaltHydroxideDust = dust(Materials.CobaltIIHydroxide);
    public static final ChaosItemKey CobaltNitrateDust = dust(Materials.CobaltIINitrate);
    public static final ChaosItemKey NickelAluminideIngot = ChaosItemKey.create(Materials.NickelAluminide.getIngots(1));
    public static final ChaosItemKey InfinityCatalystDust = dust(Materials.InfinityCatalyst);

    // Mixture full dusts currently used by recipes.
    public static final ChaosItemKey IlmeniteDust = dust(Materials.Ilmenite);
    public static final ChaosItemKey BauxiteDust = dust(Materials.Bauxite);
    public static final ChaosItemKey IlmeniteSlagDust = dust(Materials.IlmeniteSlag);
    public static final ChaosItemKey BauxiteSlagDust = dust(Materials.BauxiteSlag);

    // Source markers retain native crushed inputs; preprocessing replaces only these with full mineral dusts.
    public static final ChaosItemKey CrushedIlmenite = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.crushed, Materials.Ilmenite, 1));
    public static final ChaosItemKey CrushedBauxite = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.crushed, Materials.Bauxite, 1));
    public static final ChaosItemKey PurifiedSphalerite = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Sphalerite, 1));
    public static final ChaosItemKey PurifiedGalena = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.crushedPurified, Materials.Galena, 1));

    // Native crushed inputs and milled intermediates for indium flotation.
    public static final ChaosItemKey CrushedSphalerite = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.crushed, Materials.Sphalerite, 1));
    public static final ChaosItemKey CrushedChalcopyrite = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.crushed, Materials.Chalcopyrite, 1));
    public static final ChaosItemKey MilledSphalerite = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.milled, Materials.Sphalerite, 1));
    public static final ChaosItemKey MilledChalcopyrite = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.milled, Materials.Chalcopyrite, 1));
    public static final ChaosItemKey CrushedPineMaterials = ChaosItemKey.create(GregtechItemList.CrushedPineMaterials.get(1));

    public static final ChaosItemKey CrystallineAlloyDust = dust(Materials.CrystallineAlloy);
    public static final ChaosItemKey PulsatingIronDust = dust(Materials.PulsatingIron);
    public static final ChaosItemKey RedstoneAlloyDust = dust(Materials.RedstoneAlloy);
    public static final ChaosItemKey RedstoneDust = dust(Materials.Redstone);
    public static final ChaosItemKey CoalDust = dust(Materials.Coal);
    public static final ChaosItemKey EnderPearlDust = dust(Materials.EnderPearl);
    public static final ChaosItemKey EmeraldDust = dust(Materials.Emerald);
    public static final ChaosItemKey CrudeRhodiumMetalDust = dust(WerkstoffLoader.CrudeRhMetall);
    public static final ChaosItemKey IridiumMetalResidueDust = dust(WerkstoffLoader.IrLeachResidue);
    public static final ChaosItemKey LeachResidueDust = dust(WerkstoffLoader.LeachResidue);
    public static final ChaosItemKey MetallicSludgeResidueDust = dust(WerkstoffLoader.PGSDResidue2);
    public static final ChaosItemKey PalladiumMetallicPowder = dust(WerkstoffLoader.PDMetallicPowder);
    public static final ChaosItemKey PalladiumSaltDust = dust(WerkstoffLoader.PDSalt);
    public static final ChaosItemKey PlatinumMetallicPowder = dust(WerkstoffLoader.PTMetallicPowder);
    public static final ChaosItemKey PlatinumResidueDust = dust(WerkstoffLoader.PTResidue);
    public static final ChaosItemKey PlatinumSaltDust = dust(WerkstoffLoader.PTSaltCrude);
    public static final ChaosItemKey RarestMetalResidueDust = dust(WerkstoffLoader.IrOsLeachResidue);
    public static final ChaosItemKey RefinedPlatinumSaltDust = dust(WerkstoffLoader.PTSaltRefined);
    public static final ChaosItemKey ReprecipitatedPalladiumDust = dust(WerkstoffLoader.PDRawPowder);
    public static final ChaosItemKey ReprecipitatedPlatinumDust = dust(WerkstoffLoader.PTRawPowder);
    public static final ChaosItemKey ReprecipitatedRhodiumDust = dust(WerkstoffLoader.ReRh);
    public static final ChaosItemKey RhodiumFilterCakeDust = dust(WerkstoffLoader.RhFilterCake);
    public static final ChaosItemKey RhodiumSaltDust = dust(WerkstoffLoader.RHSalt);
    public static final ChaosItemKey SludgeResidueDust = dust(WerkstoffLoader.PGSDResidue);
    public static final ChaosItemKey MonaziteSulfateDust = dust(WerkstoffMaterialPool.MonaziteSulfate);
    public static final ChaosItemKey AcidicMonaziteDust = dust(WerkstoffMaterialPool.AcidicMonazitePowder);
    public static final ChaosItemKey MonaziteRareEarthFiltrateDust = dust(WerkstoffMaterialPool.MonaziteRareEarthFiltrate);
    public static final ChaosItemKey NeutralizedMonaziteRareEarthFiltrateDust = dust(WerkstoffMaterialPool.NeutralizedMonaziteRareEarthFiltrate);
    public static final ChaosItemKey MonaziteRareEarthHydroxideConcentrateDust = dust(WerkstoffMaterialPool.MonaziteRareEarthHydroxideConcentrate);
    public static final ChaosItemKey DriedMonaziteRareEarthConcentrateDust = dust(WerkstoffMaterialPool.DriedMonaziteRareEarthConcentrate);
    public static final ChaosItemKey CooledMonaziteRareEarthConcentrateDust = dust(WerkstoffMaterialPool.CooledMonaziteRareEarthConcentrate);
    public static final ChaosItemKey MonaziteRarerEarthSedimentDust = dust(WerkstoffMaterialPool.MonaziteRarerEarthSediment);
    public static final ChaosItemKey MonaziteHalogenicRareEarthMixtureDust = dust(WerkstoffMaterialPool.MonaziteHeterogenousHalogenicRareEarthMixture);
    public static final ChaosItemKey SaturatedMonaziteRareEarthMixtureDust = dust(WerkstoffMaterialPool.SaturatedMonaziteRareEarthMixture);
    public static final ChaosItemKey BastnasiteRareEarthOxideDust = dust(WerkstoffMaterialPool.BastnasiteRareEarthOxidePowder);
    public static final ChaosItemKey LeachedBastnasiteRareEarthOxidesDust = dust(WerkstoffMaterialPool.LeachedBastnasiteRareEarthOxides);
    public static final ChaosItemKey RoastedRareEarthOxidesDust = dust(WerkstoffMaterialPool.RoastedRareEarthOxides);
    public static final ChaosItemKey WetRareEarthOxidesDust = dust(WerkstoffMaterialPool.WetRareEarthOxides);
    public static final ChaosItemKey CeriumOxidizedRareEarthOxidesDust = dust(WerkstoffMaterialPool.CeriumOxidisedRareEarthOxides);
    public static final ChaosItemKey BastnasiteRarerEarthOxidesDust = dust(WerkstoffMaterialPool.BastnasiteRarerEarthOxides);
    public static final ChaosItemKey NeodymicRareEarthConcentrateDust = dust(WerkstoffMaterialPool.NeodymicRareEarthConcentrate);
    public static final ChaosItemKey SamaricRareEarthConcentrateDust = dust(WerkstoffMaterialPool.SamaricRareEarthConcentrate);
    public static final ChaosItemKey FluorinatedSamaricConcentrateDust = dust(WerkstoffMaterialPool.FluorinatedSamaricConcentrate);
    public static final ChaosItemKey SamariumTerbiumMixtureDust = dust(WerkstoffMaterialPool.SamariumTerbiumMixture);
    public static final ChaosItemKey NitratedSamariumTerbiumMixtureDust = dust(WerkstoffMaterialPool.NitratedSamariumTerbiumMixture);
    public static final ChaosItemKey SamaricResidueDust = dust(WerkstoffMaterialPool.SamaricResidue);
    public static final ChaosItemKey CeriumRichMixtureDust = dust(WerkstoffMaterialPool.CeriumRichMixture);
    public static final ChaosItemKey SamariumOreConcentrateDust = dust(WerkstoffMaterialPool.SamariumOreConcentrate);
    public static final ChaosItemKey DephosphatedSamariumConcentrateDust = dust(WerkstoffMaterialPool.DephosphatedSamariumConcentrate);
    public static final ChaosItemKey SamariumOxalateDust = dust(WerkstoffMaterialPool.SamariumOxalate);
    public static final ChaosItemKey SamariumChlorideDust = dust(WerkstoffMaterialPool.SamariumChloride);
    public static final ChaosItemKey SamariumChlorideSodiumChlorideBlendDust = dust(WerkstoffMaterialPool.SamariumChlorideSodiumChlorideBlend);
    public static final ChaosItemKey RarestEarthResidueDust = dust(WerkstoffMaterialPool.RarestEarthResidue);
    public static final ChaosItemKey ThoriumPhosphateCakeDust = dust(WerkstoffMaterialPool.ThoriumPhosphateCake);
    public static final ChaosItemKey ThoriumPhosphateConcentrateDust = dust(WerkstoffMaterialPool.ThoriumPhosphateConcentrate);
    public static final ChaosItemKey UraniumFiltrateDust = dust(WerkstoffMaterialPool.UraniumFiltrate);
    public static final ChaosItemKey NeutralizedUraniumFiltrateDust = dust(WerkstoffMaterialPool.NeutralizedUraniumFiltrate);
    public static final ChaosItemKey NaquadahOxideMixtureDust = dust(GGMaterial.naquadahEarth);
    public static final ChaosItemKey EnrichedNaquadahOxideMixtureDust = dust(GGMaterial.enrichedNaquadahEarth);
    public static final ChaosItemKey NaquadriaOxideMixtureDust = dust(GGMaterial.naquadriaEarth);
    public static final ChaosItemKey ConcentratedEnrichedNaquadahSludgeDust = dust(GGMaterial.concentratedEnrichedNaquadahSludge);
    public static final ChaosItemKey LowQualityNaquadriaPhosphateDust = dust(GGMaterial.lowQualityNaquadriaPhosphate);
    public static final ChaosItemKey LowQualityNaquadriaSulfateDust = dust(GGMaterial.lowQualityNaquadriaSulphate);
    public static final ChaosItemKey PrismaticNaquadahDust = dust(Materials.PrismaticNaquadah);

    // Physical entry markers and prismatic process intermediates.
    public static final ChaosItemKey Fruit = ChaosItemKey.create(Items.apple);
    public static final ChaosItemKey BastnasiteDust = dust(Materials.Bastnasite);
    public static final ChaosItemKey MonaziteDust = dust(Materials.Monazite);
    public static final ChaosItemKey PrismarineShard = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.shard, Materials.Prismarine, 1));
    public static final ChaosItemKey PrismarinePrecipitate = ChaosItemKey.create(ItemList.Prismarine_Precipitate.get(1));
    public static final ChaosItemKey PrismaticCrystal = ChaosItemKey.create(ItemList.Prismatic_Crystal.get(1));
    public static final ChaosItemKey DiamondGem = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.gem, Materials.Diamond, 1));
    public static final ChaosItemKey EnderPearl = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.gem, Materials.EnderPearl, 1));
    public static final ChaosItemKey PulsatingCrystal = enderIOItem(5);
    public static final ChaosItemKey PulsatingCrystalDust = enderIOItem(14);

    // Advanced-material process items and optional singularity identities.
    public static final ChaosItemKey EnderQuantumComponent = modItem(Mods.SuperSolarPanels, "enderquantumcomponent", 0);
    public static final ChaosItemKey EnrichedSunnarium = modItem(Mods.AdvancedSolarPanel, "asp_crafting_items", 3);
    public static final ChaosItemKey IrradiantUranium = modItem(Mods.AdvancedSolarPanel, "asp_crafting_items", 2);
    public static final ChaosItemKey MagMatterRound = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.round, Materials.MagMatter, 1));
    public static final ChaosItemKey PhononicSeedCrystal = ChaosItemKey.create(ItemList.Phononic_Seed_Crystal.get(1));
    public static final ChaosItemKey RawCrystalChip = recipeItemId(ItemList.Circuit_Parts_RawCrystalChip.get(1));
    public static final ChaosItemKey RawCrystalParts = recipeItemId(ItemList.Circuit_Parts_RawCrystalParts.get(1));
    public static final ChaosItemKey CeriumLutetiumAluminiumOxygenBlendDust = dust(WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumOxygenBlend);
    public static final ChaosItemKey CeriumLutetiumAluminiumGarnetDust = dust(WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet);
    public static final ChaosItemKey CeriumLutetiumAluminiumGarnet = recipeItemId(WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gem, 1));
    public static final ChaosItemKey FlawlessCeriumLutetiumAluminiumGarnet = recipeItemId(WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemFlawless, 1));
    public static final ChaosItemKey ExquisiteCeriumLutetiumAluminiumGarnet = recipeItemId(WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumGarnet.get(OrePrefixes.gemExquisite, 1));
    public static final ChaosItemKey EternityDust = dust(Materials.Eternity);
    public static final ChaosItemKey EternalSingularity = modItem(Mods.EternalSingularity, "eternal_singularity", 0);
    public static final ChaosItemKey ChronicSingularity = modItem(Mods.EternalSingularity, "combined_singularity", 15);

    // Registered process items used by the netherite trunks and reagent branches.
    public static final ChaosItemKey AncientDebris = etFuturumItem("ancient_debris");
    public static final ChaosItemKey NetheriteScrap = etFuturumItem("netherite_scrap");
    public static final ChaosItemKey HotNetheriteScrap = ChaosItemKey.create(ItemList.Hot_Netherite_Scrap.get(1));
    public static final ChaosItemKey BrittleNetheriteScrap = ChaosItemKey.create(ItemList.Brittle_Netherite_Scrap.get(1));
    public static final ChaosItemKey NetheriteNanoparticles = ChaosItemKey.create(ItemList.Netherite_Nanoparticles.get(1));
    public static final ChaosItemKey IntenselyBondedNetheriteNanoparticles = ChaosItemKey.create(ItemList.Intensely_Bonded_Netherite_Nanoparticles.get(1));
    public static final ChaosItemKey HeavyHellishMud = ChaosItemKey.create(ItemList.Heavy_Hellish_Mud.get(1));
    public static final ChaosItemKey HellishMetalBlock = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.block, Materials.HellishMetal, 1));
    public static final ChaosItemKey RhodiumBlock = ChaosItemKey.create(MaterialsElements.getInstance().RHODIUM.getBlock(1));
    public static final ChaosItemKey Netherrack = ChaosItemKey.create(new ItemStack(Blocks.netherrack));
    public static final ChaosItemKey MilledNetherrack = ChaosItemKey.create(GTOreDictUnificator.get(OrePrefixes.milled, Materials.Netherrack, 1));
    public static final ChaosItemKey AluminaMillingBall = ChaosItemKey.create(GregtechItemList.Milling_Ball_Alumina.get(1));

    public static ChaosItemKey resolvePlatinumLeafAlias() {
        return resolveCropsLeafAlias(12);
    }

    public static ChaosItemKey resolveCropsLeafAlias(int metadata) {
        if (!Mods.CropsNH.isModLoaded()) return null;
        ItemStack fallback = new ItemStack(Items.stick);
        ItemStack leaf = findModItem("cropsnh", "materialLeaf", 1, metadata, fallback);
        return leaf == fallback ? null : ChaosItemKey.create(leaf);
    }

    public static ItemStack[] platinumInputAlternatives(ChaosItemKey platinumLeafAlias) {
        return platinumLeafAlias == null ? null
            : new ItemStack[] { PlatinumMetallicPowder.getItemStack(1),
                platinumLeafAlias.getItemStack(PLATINUM_LEAVES_PER_POWDER) };
    }

    // Elemental gases, liquids and melts, ordered by atomic number.
    public static final Fluid HydrogenGas = fluid(Materials.Hydrogen.getGas(1));
    public static final Fluid NitrogenGas = fluid(Materials.Nitrogen.getGas(1));
    public static final Fluid OxygenGas = fluid(Materials.Oxygen.getGas(1));
    public static final Fluid FluorineGas = fluid(Materials.Fluorine.getGas(1));
    public static final Fluid ChlorineGas = fluid(Materials.Chlorine.getGas(1));
    public static final Fluid PotassiumMolten = fluid(Materials.Potassium.getMolten(1));
    public static final Fluid StrontiumMolten = fluid(Materials.Strontium.getMolten(1));
    public static final Fluid XenonGas = fluid(WerkstoffLoader.Xenon.getFluidOrGas(1));
    public static final Fluid OganessonGas = fluid(WerkstoffLoader.Oganesson.getFluidOrGas(1));
    public static final Fluid EuropiumMolten = fluid(Materials.Europium.getMolten(1));
    public static final Fluid CeriumLutetiumAluminiumOxygenBlendMolten = fluid(WerkstoffMaterialPool.CeriumDopedLutetiumAluminiumOxygenBlend.getMolten(1));

    // Compound fluids currently used by recipes.
    public static final Fluid TitaniumTetrachloride = fluid(Materials.Titaniumtetrachloride.getFluid(1));
    public static final Fluid SodiumTungstate = fluid(BotWerkstoffMaterialPool.SodiumTungstate.getFluidOrGas(1));
    public static final Fluid IndiumConcentrate = fluid(new FluidStack(ItemList.sIndiumConcentrate, 1));
    public static final Fluid LeadZincSolution = fluid(new FluidStack(ItemList.sLeadZincSolution, 1));
    public static final Fluid NitricAcid = fluid(Materials.NitricAcid.getFluid(1));
    public static final Fluid HydrochloricAcid = fluid(Materials.HydrochloricAcid.getFluid(1));
    public static final Fluid AmmoniaGas = fluid(Materials.Ammonia.getGas(1));
    public static final Fluid Water = fluid(Materials.Water.getFluid(1));
    public static final Fluid Steam = fluid(Materials.Steam.getGas(1));
    public static final Fluid SuperheatedSteam = fluid(GTModHandler.getSuperHeatedSteam(1));
    public static final Fluid Grade2PurifiedWater = fluid(Materials.Grade2PurifiedWater.getFluid(1));
    public static final Fluid Air = fluid(Materials.Air.getGas(1));
    public static final Fluid OzoneGas = fluid(Materials.Ozone.getGas(1));
    public static final Fluid PolyAluminiumChloride = fluid(Materials.PolyAluminiumChloride.getFluid(1));
    public static final Fluid FlocculationWasteLiquid = fluid(Materials.FlocculationWasteLiquid.getFluid(1));
    public static final Fluid SuperCoolant = fluid(Materials.SuperCoolant.getFluid(1));
    public static final Fluid IC2Coolant = fluid(GTModHandler.getIC2Coolant(1));
    public static final Fluid CarbonMonoxideGas = fluid(Materials.CarbonMonoxide.getGas(1));
    public static final Fluid CarbonDioxideGas = fluid(Materials.CarbonDioxide.getGas(1));
    public static final Fluid MethaneGas = fluid(Materials.Methane.getGas(1));
    public static final Fluid Benzene = fluid(Materials.Benzene.getFluid(1));
    public static final Fluid Chlorobenzene = fluid(Materials.Chlorobenzene.getFluid(1));
    public static final Fluid PhosphorusTrichloride = fluid(Materials.PhosphorusTrichloride.getFluid(1));
    public static final Fluid TrimethylBorate = fluid(Materials.TrimethylBorate.getFluid(1));
    public static final Fluid BoricAcid = Objects.requireNonNull(FluidRegistry.getFluid("boricacid"), "boricacid");
    public static final Fluid Nitrochlorobenzene = fluid(Materials.Nitrochlorobenzene.getFluid(1));
    public static final Fluid Dichlorobenzidine = fluid(Materials.Dichlorobenzidine.getFluid(1));
    public static final Fluid Diaminobenzidine = fluid(Materials.Diaminobenzidin.getFluid(1));
    public static final Fluid DiphenylIsophthalate = fluid(Materials.Diphenylisophthalate.getFluid(1));
    public static final Fluid PolybenzimidazoleMolten = fluid(Materials.Polybenzimidazole.getMolten(1));
    public static final Fluid Dimethylbenzene = fluid(Materials.Dimethylbenzene.getFluid(1));
    public static final Fluid SulfuricAcid = fluid(Materials.SulfuricAcid.getFluid(1));
    public static final Fluid DilutedSulfuricAcid = fluid(Materials.DilutedSulfuricAcid.getFluid(1));
    public static final Fluid Phenol = fluid(Materials.Phenol.getFluid(1));
    public static final Fluid EthyleneGas = fluid(Materials.Ethylene.getGas(1));
    public static final Fluid NitrogenDioxideGas = fluid(Materials.NitrogenDioxide.getGas(1));
    public static final Fluid NitrogenMonoxideGas = fluid(Materials.NitricOxide.getGas(1));
    public static final Fluid AmmoniumChlorideFluid = fluid(WerkstoffLoader.AmmoniumChloride.getFluidOrGas(1));
    public static final Fluid SodiumFormateFluid = fluid(WerkstoffLoader.Sodiumformate.getFluidOrGas(1));
    public static final Fluid FormicAcid = fluid(WerkstoffLoader.FormicAcid.getFluidOrGas(1));
    public static final Fluid CalciumChlorideFluid = fluid(WerkstoffLoader.CalciumChloride.getFluidOrGas(1));
    public static final Fluid RutheniumTetroxideFluid = fluid(WerkstoffLoader.RutheniumTetroxide.getFluidOrGas(1));
    public static final Fluid RhodiumSulfateGas = fluid(WerkstoffLoader.RHSulfate.getFluidOrGas(1));
    public static final Fluid PotassiumDisulfateMolten = fluid(WerkstoffLoader.PotassiumDisulfate.getMolten(1));
    public static final Fluid OxalicAcid = fluid(GGMaterial.oxalate.getFluidOrGas(1));
    public static final Fluid AmmoniumNitrateSolution = fluid(WerkstoffMaterialPool.AmmoniumNitrate.getFluidOrGas(1));
    public static final Fluid SodiumFluorosilicate = fluid(WerkstoffMaterialPool.SodiumFluorosilicate.getFluidOrGas(1));
    public static final Fluid CalciumFluorideFluid = fluid(WerkstoffMaterialPool.CalciumFluoride.getFluidOrGas(1));
    public static final Fluid Acetone = fluid(Materials.Acetone.getFluid(1));
    public static final Fluid AceticAcid = fluid(Materials.AceticAcid.getFluid(1));
    public static final Fluid ChloromethaneGas = fluid(Materials.Chloromethane.getGas(1));
    public static final Fluid HexafluorosilicicAcid = fluid(WerkstoffLoader.HexafluorosilicicAcid.getFluidOrGas(1));
    public static final Fluid HydratedAmmoniumNitrateSlurry = fluid(new FluidStack(GTPPFluids.HydratedAmmoniumNitrateSlurry, 1));
    public static final Fluid PhosphoricAcid = fluid(Materials.PhosphoricAcid.getFluid(1));
    public static final Fluid Nitrobenzene = fluid(new FluidStack(GTPPFluids.Nitrobenzene, 1));
    public static final Fluid HydrogenPeroxide = fluid(new FluidStack(GTPPFluids.HydrogenPeroxide, 1));
    public static final Fluid Ethylanthraquinone = fluid(new FluidStack(GTPPFluids.Ethylanthraquinone, 1));
    public static final Fluid Ethylanthrahydroquinone = fluid(new FluidStack(GTPPFluids.Ethylanthrahydroquinone, 1));
    public static final Fluid Anthracene = fluid(new FluidStack(GTPPFluids.Anthracene, 1));
    public static final Fluid DistilledWater = fluid(GTModHandler.getDistilledWater(1));
    public static final Fluid LiquidNitrogen = fluid(Materials.LiquidNitrogen.getFluid(1));
    public static final Fluid HafniumTetrachlorideSolution = fluid(WerkstoffMaterialPool.HafniumTetrachlorideSolution.getFluidOrGas(1));
    public static final Fluid ZirconiumTetrachlorideSolution = fluid(WerkstoffMaterialPool.ZirconiumTetrachlorideSolution.getFluidOrGas(1));


    // Biological media and polymer-processing fluids used by the registered chains.
    public static final Fluid Xenoxene = fluid(Materials.Xenoxene.getFluid(1));
    public static final Fluid RawRadox = fluid(Materials.RadoxRaw.getFluid(1));
    public static final Fluid DilutedXenoxene = fluid(Materials.XenoxeneDiluted.getFluid(1));
    public static final Fluid SuperHeavyRadox = fluid(Materials.RadoxSuperHeavy.getFluid(1));
    public static final Fluid HeavyRadox = fluid(Materials.RadoxHeavy.getFluid(1));
    public static final Fluid LightRadox = fluid(Materials.RadoxLight.getGas(1));
    public static final Fluid SuperLightRadox = fluid(Materials.RadoxSuperLight.getGas(1));
    public static final Fluid CrackedRadox = fluid(Materials.RadoxCracked.getGas(1));
    public static final Fluid RadoxGas = fluid(Materials.RadoxGas.getGas(1));
    public static final Fluid RadoxPolymerMolten = fluid(Materials.RadoxPolymer.getMolten(1));
    public static final Fluid GrowthMediumRaw = fluid(Materials.GrowthMediumRaw.getFluid(1));
    public static final Fluid GrowthMediumSterilized = fluid(Materials.GrowthMediumSterilized.getFluid(1));
    public static final Fluid BioMediumRaw = fluid(Materials.BioMediumRaw.getFluid(1));
    public static final Fluid BioMediumSterilized = fluid(Materials.BioMediumSterilized.getFluid(1));
    public static final Fluid EnrichedBacterialSludge = fluid(Materials.EnrichedBacterialSludge.getFluid(1));
    public static final Fluid FermentedBacterialSludge = fluid(Materials.FermentedBacterialSludge.getFluid(1));
    public static final Fluid HydrogenSulfideGas = fluid(Materials.HydricSulfide.getGas(1));
    public static final Fluid LiquidCrystalKevlar = fluid(Materials.LiquidCrystalKevlar.getFluid(1));
    public static final Fluid PolyurethaneResin = fluid(Materials.PolyurethaneResin.getFluid(1));
    public static final Fluid Methanol = fluid(Materials.Methanol.getFluid(1));
    public static final Fluid EthyleneOxideGas = fluid(Materials.EthyleneOxide.getGas(1));
    public static final Fluid EthyleneGlycol = fluid(Materials.Ethyleneglycol.getFluid(1));
    public static final Fluid SiliconeOil = fluid(Materials.SiliconOil.getFluid(1));
    public static final Fluid Dimethyldichlorosilane = fluid(Materials.Dimethyldichlorosilane.getFluid(1));
    public static final Fluid MethylamineGas = fluid(Materials.Methylamine.getGas(1));
    public static final Fluid GammaButyrolactone = fluid(Materials.GammaButyrolactone.getFluid(1));
    public static final Fluid AcetyleneGas = fluid(Materials.Acetylene.getGas(1));
    public static final Fluid Butyraldehyde = fluid(Materials.Butyraldehyde.getFluid(1));
    public static final Fluid Nitroaniline = fluid(Materials.IVNitroaniline.getFluid(1));
    public static final Fluid NitrationMixture = fluid(Materials.NitrationMixture.getFluid(1));
    public static final Fluid NMethylpyrrolidone = fluid(Materials.NMethylIIPyrrolidone.getFluid(1));
    public static final Fluid SulfurTrioxideGas = fluid(Materials.SulfurTrioxide.getGas(1));
    public static final Fluid SulfurDichloride = fluid(Materials.SulfurDichloride.getFluid(1));
    public static final Fluid ThionylChloride = fluid(Materials.ThionylChloride.getFluid(1));
    public static final Fluid DimethylTerephthalate = fluid(Materials.DimethylTerephthalate.getFluid(1));
    public static final Fluid TerephthalicAcid = fluid(Materials.TerephthalicAcid.getFluid(1));
    public static final Fluid Paraxylene = fluid(Materials.IVDimethylbenzene.getFluid(1));
    public static final Fluid NaphthenicAcid = fluid(Materials.NaphthenicAcid.getFluid(1));
    public static final Fluid DiphenylmethaneDiisocyanateMixture = fluid(Materials.DiphenylmethaneDiisocyanateMixture.getFluid(1));
    public static final Fluid DiaminodiphenylmethaneMixture = fluid(Materials.DiaminodiphenylmethanMixture.getFluid(1));
    public static final Fluid AcetaldehydeGas = fluid(Materials.Acetaldehyde.getGas(1));
    public static final Fluid NickelTetracarbonyl = fluid(Materials.NickelTetracarbonyl.getFluid(1));
    public static final Fluid IronIIIChloride = fluid(Materials.IronIIIChloride.getFluid(1));
    public static final Fluid SulfurDioxideGas = fluid(Materials.SulfurDioxide.getGas(1));
    public static final Fluid DilutedHydrochloricAcid = fluid(Materials.DilutedHydrochloricAcid.getFluid(1));
    public static final Fluid MeatExtract = Objects.requireNonNull(FluidRegistry.getFluid("meatextract"), "meatextract");
    public static final Fluid UnknownNutrientAgar = Objects.requireNonNull(FluidRegistry.getFluid("unknownnutrientagar"), "unknownnutrientagar");
    public static final Fluid SeaweedBroth = Objects.requireNonNull(FluidRegistry.getFluid("seaweedbroth"), "seaweedbroth");
    public static final Fluid BacterialSludge = Objects.requireNonNull(FluidRegistry.getFluid("bacterialsludge"), "bacterialsludge");
    public static final Fluid Mutagen = Objects.requireNonNull(FluidRegistry.getFluid("mutagen"), "mutagen");
    public static final Fluid Butanediol = Objects.requireNonNull(FluidRegistry.getFluid("1,4-butanediol"), "1,4-butanediol");
    public static final Fluid Phosgene = fluid(BotWerkstoffMaterialPool.Phosgene.getFluidOrGas(1));
    public static final Fluid Butanol = Objects.requireNonNull(GTPPFluids.Butanol, "Butanol");
    public static final Fluid Aniline = Objects.requireNonNull(GTPPFluids.Aniline, "Aniline");
    public static final Fluid Formaldehyde = Objects.requireNonNull(GTPPFluids.Formaldehyde, "Formaldehyde");
    public static final Fluid PropionicAcid = Objects.requireNonNull(GTPPFluids.PropionicAcid, "PropionicAcid");
    public static final Fluid FermentationBase = Objects.requireNonNull(GTPPFluids.FermentationBase, "FermentationBase");
    public static final Fluid AceticAnhydride = fluid(MaterialMisc.ACETIC_ANHYDRIDE.getFluidStack(1));

    // Wood-chemical and petrochemical fluids used by the GTNL wood and petrochemical routes.
    public static final Fluid Creosote = fluid(Materials.Creosote.getFluid(1));
    public static final Fluid WoodTar = fluid(Materials.WoodTar.getFluid(1));
    public static final Fluid Toluene = fluid(Materials.Toluene.getFluid(1));
    public static final Fluid MethylAcetate = fluid(Materials.MethylAcetate.getFluid(1));
    public static final Fluid Ethanol = fluid(Materials.Ethanol.getFluid(1));
    public static final Fluid CoalGas = fluid(new FluidStack(GTPPFluids.CoalGas, 1));
    public static final Fluid III_Dimethylbenzene = fluid(Materials.IIIDimethylbenzene.getFluid(1));
    public static final Fluid IVDimethylbenzene = fluid(Materials.IVDimethylbenzene.getFluid(1));
    public static final Fluid Oil = fluid(Materials.Oil.getFluid(1));
    public static final Fluid LightOil = fluid(Materials.OilLight.getFluid(1));
    public static final Fluid MediumOil = fluid(Materials.OilMedium.getFluid(1));
    public static final Fluid HeavyOil = fluid(Materials.OilHeavy.getFluid(1));
    public static final Fluid ExtraHeavyOil = fluid(Materials.OilExtraHeavy.getFluid(1));
    public static final Fluid Naphtha = fluid(Materials.Naphtha.getFluid(1));
    public static final Fluid LightFuel = fluid(Materials.LightFuel.getFluid(1));
    public static final Fluid HeavyFuel = fluid(Materials.HeavyFuel.getFluid(1));
    public static final Fluid Lubricant = fluid(Materials.Lubricant.getFluid(1));
    public static final Fluid NaturalGas = fluid(Materials.Gas.getGas(1));
    public static final Fluid HeliumGas = fluid(Materials.Helium.getGas(1));
    public static final Fluid PropaneGas = fluid(Materials.Propane.getGas(1));
    public static final Fluid PropeneGas = fluid(Materials.Propene.getGas(1));
    public static final Fluid EthaneGas = fluid(Materials.Ethane.getGas(1));
    public static final Fluid ButaneGas = fluid(Materials.Butane.getGas(1));
    public static final Fluid ButeneGas = fluid(Materials.Butene.getGas(1));
    public static final Fluid ButadieneGas = fluid(Materials.Butadiene.getGas(1));
    public static final Fluid Octane = fluid(Materials.Octane.getFluid(1));
    public static final Fluid CoalTar = fluid(new FluidStack(GTPPFluids.CoalTar, 1));
    public static final Fluid CoalTarOil = fluid(new FluidStack(GTPPFluids.CoalTarOil, 1));
    public static final Fluid Ethylbenzene = fluid(new FluidStack(GTPPFluids.Ethylbenzene, 1));
    public static final Fluid Kerosene = fluid(new FluidStack(GTPPFluids.Kerosene, 1));
    public static final Fluid Naphthalene = fluid(new FluidStack(GTPPFluids.Naphthalene, 1));

    // Mixture fluids currently used by recipes.
    public static final Fluid BauxiteSlurry = fluid(Materials.BauxiteSlurry.getFluid(1));
    public static final Fluid HeatedBauxiteSlurry = fluid(Materials.HeatedBauxiteSlurry.getFluid(1));
    public static final Fluid IronIISulfateSolution = ItemList.sGreenVitriol;
    public static final Fluid SluiceJuice = fluid(Materials.SluiceJuice.getFluid(1));
    public static final Fluid CrystallineAlloyMolten = fluid(Materials.CrystallineAlloy.getMolten(1));
    public static final Fluid PulsatingIronMolten = fluid(Materials.PulsatingIron.getMolten(1));
    public static final Fluid SaltWater = fluid(Materials.SaltWater.getFluid(1));
    public static final Fluid Lava = fluid(Materials.Lava.getFluid(1));
    public static final Fluid PoorNetherWaste = fluid(Materials.PoorNetherWaste.getFluid(1));
    public static final Fluid NetherAir = fluid(Materials.NetherAir.getFluid(1));
    public static final Fluid NetherSemiFluid = fluid(Materials.NetherSemiFluid.getFluid(1));
    public static final Fluid NefariousGas = fluid(Materials.NefariousGas.getFluid(1));
    public static final Fluid NefariousOil = fluid(Materials.NefariousOil.getFluid(1));
    public static final Fluid HellishMetalMolten = fluid(Materials.HellishMetal.getMolten(1));
    public static final Fluid NetherrackFlotationFroth = fluid(new FluidStack(GTPPFluids.NetherrackFlotationFroth, 1));
    public static final Fluid SphaleriteFlotationFroth = fluid(new FluidStack(GTPPFluids.SphaleriteFlotationFroth, 1));
    public static final Fluid ChalcopyriteFlotationFroth = fluid(new FluidStack(GTPPFluids.ChalcopyriteFlotationFroth, 1));
    public static final Fluid PineOil = fluid(new FluidStack(GTPPFluids.PineOil, 1));
    public static final Fluid RedMud = fluid(new FluidStack(GTPPFluids.RedMud, 1));
    public static final Fluid HydrofluoricAcid = fluid(Materials.HydrofluoricAcid.getFluid(1));
    public static final Fluid PhthalicAcid = fluid(Materials.PhthalicAcid.getFluid(1));
    public static final Fluid PlatinumConcentrateFluid = fluid(WerkstoffLoader.PTConcentrate.getFluidOrGas(1));
    public static final Fluid PalladiumEnrichedAmmoniaFluid = fluid(WerkstoffLoader.PDAmmonia.getFluidOrGas(1));
    public static final Fluid RutheniumTetroxideSolution = fluid(WerkstoffLoader.RutheniumTetroxideSollution.getFluidOrGas(1));
    public static final Fluid HotRutheniumTetroxideSolution = fluid(WerkstoffLoader.HotRutheniumTetroxideSollution.getFluidOrGas(1));
    public static final Fluid AcidicOsmiumSolution = fluid(WerkstoffLoader.AcidicOsmiumSolution.getFluidOrGas(1));
    public static final Fluid OsmiumSolution = fluid(WerkstoffLoader.OsmiumSolution.getFluidOrGas(1));
    public static final Fluid AcidicIridiumSolution = fluid(WerkstoffLoader.AcidicIridiumSolution.getFluidOrGas(1));
    public static final Fluid RhodiumSulfateSolution = fluid(WerkstoffLoader.RHSulfateSolution.getFluidOrGas(1));
    public static final Fluid RhodiumSaltSolution = fluid(WerkstoffLoader.RHSaltSolution.getFluidOrGas(1));
    public static final Fluid RhodiumFilterCakeSolution = fluid(WerkstoffLoader.RHFilterCakeSolution.getFluidOrGas(1));
    public static final Fluid AquaRegia = fluid(WerkstoffLoader.AquaRegia.getFluidOrGas(1));
    public static final Fluid MuddyMonaziteSolution = fluid(WerkstoffMaterialPool.MuddyRareEarthMonaziteSolution.getFluidOrGas(1));
    public static final Fluid DilutedMonaziteMud = fluid(WerkstoffMaterialPool.DilutedRareEarthMonaziteMud.getFluidOrGas(1));
    public static final Fluid DilutedMonaziteSulfate = fluid(WerkstoffMaterialPool.DilutedMonaziteSulfate.getFluidOrGas(1));
    public static final Fluid NitratedMonaziteConcentrate = fluid(WerkstoffMaterialPool.NitratedRareEarthMonaziteConcentrate.getFluidOrGas(1));
    public static final Fluid NitricLeachedMonaziteMixture = fluid(WerkstoffMaterialPool.NitricLeachedMonaziteMixture.getFluidOrGas(1));
    public static final Fluid NitricMonaziteLeachedConcentrate = fluid(WerkstoffMaterialPool.NitricMonaziteLeachedConcentrate.getFluidOrGas(1));
    public static final Fluid MuddySamariumRareEarthSolution = fluid(WerkstoffMaterialPool.MuddySamariumRareEarthSolution.getFluidOrGas(1));
    public static final Fluid SamariumRareEarthMud = fluid(WerkstoffMaterialPool.SamariumRareEarthMud.getFluidOrGas(1));
    public static final Fluid DilutedSamariumRareEarthSolution = fluid(WerkstoffMaterialPool.DilutedSamariumRareEarthSolution.getFluidOrGas(1));
    public static final Fluid MuddyBastnasiteSolution = fluid(WerkstoffMaterialPool.MuddyRareEarthBastnasiteSolution.getFluidOrGas(1));
    public static final Fluid SteamCrackedBastnasiteSolution = fluid(WerkstoffMaterialPool.SteamCrackedBasnasiteSolution.getFluidOrGas(1));
    public static final Fluid ConditionedBastnasiteMud = fluid(WerkstoffMaterialPool.ConditionedBastnasiteMud.getFluidOrGas(1));
    public static final Fluid DilutedBastnasiteMud = fluid(WerkstoffMaterialPool.DiltedRareEarthBastnasiteMud.getFluidOrGas(1));
    public static final Fluid FilteredBastnasiteMud = fluid(WerkstoffMaterialPool.FilteredBastnasiteMud.getFluidOrGas(1));
    public static final Fluid NitratedBastnasiteRarerEarthOxides = fluid(WerkstoffMaterialPool.NitratedBastnasiteRarerEarthOxides.getFluidOrGas(1));
    public static final Fluid SaturatedBastnasiteRarerEarthOxides = fluid(WerkstoffMaterialPool.SaturatedBastnasiteRarerEarthOxides.getFluidOrGas(1));
    public static final Fluid DilutedAcetone = fluid(WerkstoffMaterialPool.DilutedAcetone.getFluidOrGas(1));
    public static final Fluid LowQualityNaquadahEmulsion = fluid(GGMaterial.lowQualityNaquadahEmulsion.getFluidOrGas(1));
    public static final Fluid LowQualityNaquadahSolution = fluid(GGMaterial.lowQualityNaquadahSolution.getFluidOrGas(1));
    public static final Fluid NaquadahAdamantiumSolution = fluid(GGMaterial.naquadahAdamantiumSolution.getFluidOrGas(1));
    public static final Fluid NaquadahRichSolution = fluid(GGMaterial.naquadahRichSolution.getFluidOrGas(1));
    public static final Fluid EnrichedNaquadahRichSolution = fluid(GGMaterial.enrichedNaquadahRichSolution.getFluidOrGas(1));
    public static final Fluid NaquadriaRichSolution = fluid(GGMaterial.naquadriaRichSolution.getFluidOrGas(1));
    public static final Fluid LowQualityNaquadriaSolution = fluid(GGMaterial.lowQualityNaquadriaSolution.getFluidOrGas(1));
    public static final Fluid FluorineRichWasteLiquid = fluid(GGMaterial.fluorineRichWasteLiquid.getFluidOrGas(1));
    public static final Fluid NaquadahWasteLiquid = fluid(GGMaterial.wasteLiquid.getFluidOrGas(1));
    public static final Fluid PrismarineSolution = fluid(Materials.PrismarineSolution.getFluid(1));
    public static final Fluid PrismarineContaminatedHydrogenPeroxide = fluid(Materials.PrismarineContaminatedHydrogenPeroxide.getFluid(1));
    public static final Fluid PrismarineRichNitrobenzeneSolution = fluid(Materials.PrismarineRichNitrobenzeneSolution.getFluid(1));
    public static final Fluid PrismarineContaminatedNitrobenzeneSolution = fluid(Materials.PrismarineContaminatedNitrobenzeSolution.getFluid(1));
    public static final Fluid PrismaticGas = fluid(Materials.PrismaticGas.getFluid(1));
    public static final Fluid PrismaticAcid = fluid(Materials.PrismaticAcid.getFluid(1));
    public static final Fluid PhononCrystalSolution = fluid(Materials.PhononCrystalSolution.getFluid(1));
    public static final Fluid PhononMedium = fluid(Materials.PhononMedium.getFluid(1));
    public static final Fluid EternityMolten = fluid(Materials.Eternity.getMolten(1));
    // spotless:on

    static {
        // The rocket-fuel source produces the same ore-dictionary reagent; cache the native Werkstoff powder.
        UNIFIED_DUSTS.put(ChaosItemKey.create(GregtechItemList.AmmoniumNitrateDust.get(1)), AmmoniumNitrateDust);
    }

    /** Runtime lookup and consumption share the cached fruit and native full-dust identities. */
    public static ChaosItemKey inputItemId(ItemStack stack) {
        // Native chemical catalysts match item and metadata even after their wear NBT changes.
        if (MTEChemicalPlant.isCatalyst(stack)) return ChaosItemKey.createNoNBT(stack);
        ChaosItemKey physical = ChaosItemKey.create(stack);
        for (ItemStack fruit : FRUIT_INPUTS) {
            if (GTUtility.areStacksEqual(fruit, stack, true)) return Fruit;
        }
        return UNIFIED_DUSTS.getOrDefault(physical, physical);
    }

    /** Source preprocessing uses the same normalization as runtime input lookup. */
    public static ChaosItemKey recipeItemId(ItemStack stack) {
        return inputItemId(stack);
    }

    /** Sensitive source inputs keep their exact identity instead of joining a permissive fruit group. */
    static ChaosItemKey recipeItemId(ItemStack stack, boolean nbtSensitive) {
        if (nbtSensitive) {
            ChaosItemKey physical = ChaosItemKey.create(stack);
            return UNIFIED_DUSTS.getOrDefault(physical, physical);
        }
        ItemStack template = stack.copy();
        template.setTagCompound(null);
        return inputItemId(template);
    }

    private static ChaosItemKey modItem(Mods mod, String name, int metadata) {
        if (!mod.isModLoaded()) return null;
        ItemStack fallback = new ItemStack(Items.stick);
        ItemStack stack = findModItem(mod.ID, name, 1, metadata, fallback);
        return stack == fallback ? null : ChaosItemKey.create(stack);
    }

    private static ChaosItemKey etFuturumItem(String name) {
        if (!Mods.EtFuturumRequiem.isModLoaded()) return null;
        ItemStack fallback = new ItemStack(Items.stick);
        ItemStack stack = findModItem(Mods.EtFuturumRequiem.ID, name, 1, fallback);
        return stack == fallback ? null : ChaosItemKey.create(stack);
    }

    private static ChaosItemKey enderIOItem(int metadata) {
        if (!Mods.EnderIO.isModLoaded()) return null;
        ItemStack fallback = new ItemStack(Items.stick);
        ItemStack stack = findModItem(Mods.EnderIO.ID, "itemMaterial", 1, metadata, fallback);
        return stack == fallback ? null : ChaosItemKey.create(stack);
    }

    private static ChaosItemKey dust(Materials material) {
        return dust(ChaosRecipeMaterials.nativeDust(material));
    }

    private static ChaosItemKey dust(Werkstoff material) {
        return dust(material.get(OrePrefixes.dust));
    }

    private static ChaosItemKey dust(ItemStack stack) {
        if (stack == null || stack.getItem() == null) throw new IllegalStateException("missing full dust template");
        ChaosItemKey nativeId = ChaosItemKey.create(stack.copy());
        // GT has already unified so
        // urce recipes, but their outputs must retain our native dust identities.
        ChaosItemKey unified = ChaosItemKey.create(GTOreDictUnificator.get(false, stack));
        if (!unified.equals(nativeId)) UNIFIED_DUSTS.put(unified, nativeId);
        return nativeId;
    }

    private static Fluid fluid(FluidStack stack) {
        return Objects.requireNonNull(
            Objects.requireNonNull(stack, "missing fluid template")
                .getFluid(),
            "missing fluid");
    }

    private static ItemStack findModItem(String mod, String name, int amount, ItemStack fallback) {
        return findModItem(mod, name, amount, 0, fallback);
    }

    private static ItemStack findModItem(String mod, String name, int amount, int meta, ItemStack fallback) {
        ItemStack stack = gregtech.api.util.GTModHandler.getModItem(mod, name, amount, meta);
        return stack == null ? fallback : stack;
    }

}
