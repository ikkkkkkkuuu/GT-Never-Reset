package com.xyp.gtneverreset.common.recipe.gtneverreset;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.Gson;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtnrlib.mod.ModList;

import bwcrossmod.galacticgreg.VoidMinerUtility;
import galacticgreg.api.enums.DimensionDef.DimNames;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneType;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.common.WorldgenGTOreLayer;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
import gtneioreplugin.plugin.block.ModBlocks;
import gtneioreplugin.util.DimensionHelper;

public final class FragmentWorldCollectorRecipes {

    public static final int DRILL_INPUT_INDEX = 1;
    public static final int DRILL_CONSUMPTION_SCALE = 1_000_000;
    public static final RecipeMetadataKey<Integer> DRILL_CONSUMPTION_CHANCE = SimpleRecipeMetadataKey
        .create(Integer.class, "gtneverreset.fragment_world_drill_consumption_chance");

    private static final ModsItemlist[] DIMENSIONS = { ModsItemlist.DimensionOverworld, ModsItemlist.DimensionNether,
        ModsItemlist.DimensionEnd, ModsItemlist.DimensionTwilight, ModsItemlist.DimensionMoon,
        ModsItemlist.DimensionMars, ModsItemlist.DimensionVenus, ModsItemlist.DimensionMercury,
        ModsItemlist.DimensionCeres, ModsItemlist.DimensionIo, ModsItemlist.DimensionGanymede,
        ModsItemlist.DimensionPluto, ModsItemlist.DimensionEnceladus, ModsItemlist.DimensionTitan,
        ModsItemlist.DimensionTriton, ModsItemlist.DimensionBarnardC };
    private static final ModsItemlist[] PLANET_ROCKS = { ModsItemlist.MoonStone, ModsItemlist.MarsStone,
        ModsItemlist.VenusRock, ModsItemlist.MercuryRock, ModsItemlist.CeresRock, ModsItemlist.IoStone,
        ModsItemlist.GanymedeRock, ModsItemlist.PlutoStone, ModsItemlist.EnceladusRock, ModsItemlist.TitanStone,
        ModsItemlist.TritonStone };
    private static final Materials[] DRILLS = { Materials.Steel, Materials.Titanium, Materials.NaquadahAlloy,
        Materials.Neutronium, Materials.Infinity };
    private static final int[] DRILL_CONSUMPTION_CHANCES = { 100, 90, 80, 70, 60 };
    private static final int[] FLUID_MULTIPLIERS = { 1, 16, 128, 1024 };
    private static final int CIRCUITS_PER_PAGE = 24;
    private static final int DEEP_DARK_GROUP_SIZE = 8;
    private static final Set<String> unavailable = new LinkedHashSet<>();
    private static boolean loaded;

    public static void scheduleRecipes() {
        if (GregTechAPI.sGTCompleteLoad == null) loadRecipes();
        else GregTechAPI.sGTCompleteLoad.add(FragmentWorldCollectorRecipes::loadRecipes);
    }

    public static void loadRecipes() {
        if (loaded || !Mods.NEIOrePlugin.isModLoaded()) return;
        loaded = true;
        Data data = readData();
        Map<Block, Integer> reservedCircuits = new HashMap<>();
        for (int dimension = 0; dimension < DIMENSIONS.length; dimension++) {
            ModsItemlist marker = DIMENSIONS[dimension];
            if (marker.get(1) == null) continue;
            addLegacyDimension(data, dimension, marker);
            int lastOreCircuit = data.dimensionCounts[dimension][0] + (dimension == 0 ? 1 : 0);
            // Twilight circuit 10 already selects Damascus steel.
            reservedCircuits.put(marker.getBlock(), dimension == 3 ? Math.max(10, lastOreCircuit) : lastOreCircuit);
        }
        if (ModsItemlist.BarnardaCDirt.get(1) != null) {
            addLegacyDimension(data, 15, ModsItemlist.BarnardaCDirt);
            addBarnardPlants(ModsItemlist.BarnardaCDirt);
        }
        addRegisteredVeins(reservedCircuits);
        addSpecialRecipes();
        GTNeverReset.LOG.info(
            "registered {} fragment world collection recipes; unavailable source outputs: {}",
            GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getAllRecipes()
                .size(),
            unavailable);
    }

    private static void addLegacyDimension(Data data, int dimension, ModsItemlist marker) {
        int ores = data.dimensionCounts[dimension][0];
        for (int index = 0; index < ores; index++) {
            int vein = data.dimensionMaps[dimension][index];
            Outputs output = new Outputs();
            output.add(marker.get(1), 50);
            for (int slot = 0; slot < 4; slot++) {
                output.add(ore(data.ores[vein][slot], data.oreCounts[vein][slot]), 10000);
            }
            addRock(output, dimension);
            output.apply(base(marker, dimension == 0 ? index + 2 : index + 1).duration(24000 / data.oreCounts[vein][4]))
                .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
        }
        for (int index = 0; index < data.dimensionCounts[dimension][1]; index++) {
            int fluid = data.dimensionMaps[dimension][ores + index];
            for (int drill = 0; drill < DRILLS.length; drill++) {
                int amount = drill == 4 ? Integer.MAX_VALUE : data.fluidAmounts[fluid] * FLUID_MULTIPLIERS[drill];
                FluidStack output = fluid(data.fluids[fluid], amount);
                ItemStack head = GTOreDictUnificator.get(OrePrefixes.toolHeadDrill, DRILLS[drill], 1);
                if (output == null || head == null) continue;
                // The collector rolls drill consumption separately for rates below GT's 0.01% resolution.
                GTRecipeBuilder.builder()
                    .itemInputs(marker.get(0), head, circuit(index + 1))
                    .inputChances(10000, 0, 10000)
                    .metadata(DRILL_CONSUMPTION_CHANCE, DRILL_CONSUMPTION_CHANCES[drill])
                    .itemOutputs(marker.get(1))
                    .outputChances(50)
                    .fluidOutputs(output)
                    .duration(200)
                    .eut(8)
                    .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
            }
        }
    }

    private static GTRecipeBuilder base(int dimension, int circuit) {
        return base(DIMENSIONS[dimension], circuit);
    }

    private static GTRecipeBuilder base(ModsItemlist marker, int circuit) {
        return GTRecipeBuilder.builder()
            .itemInputs(marker.get(0), circuit(circuit))
            .eut(8)
            .duration(200);
    }

    private static ItemStack circuit(int number) {
        ItemStack stack = GTUtility.getIntegratedCircuit(number);
        stack.stackSize = 0;
        return stack;
    }

    private static void addRock(Outputs output, int dimension) {
        if (dimension == 0) {
            output.add(new ItemStack(Blocks.stone, 32), 10000);
            output.add(Materials.Stone.getDust(32), 10000);
            output.add(ModsItemlist.Deepslate.get(32), 10000);
            output.add(StoneType.Deepslate.getDust(true, 32), 10000);
        } else if (dimension == 1) {
            output.add(new ItemStack(Blocks.netherrack, 32), 10000);
            output.add(Materials.Netherrack.getDust(32), 10000);
            output.add(ModsItemlist.Basalt.get(32), 10000);
            output.add(Materials.Basalt.getDust(32), 10000);
        } else if (dimension == 2) {
            output.add(new ItemStack(Blocks.end_stone, 64), 10000);
            output.add(Materials.Endstone.getDust(64), 10000);
        } else if (dimension == 3) {
            output.add(ModsItemlist.Diorite.get(64), 10000);
            output.add(dust("Diorite", 64), 10000);
        } else if (dimension < 15) {
            output.add(PLANET_ROCKS[dimension - 4].get(64), 10000);
        } else output.add(new ItemStack(Blocks.stone, 64), 10000);
    }

    private static void addRegisteredVeins(Map<Block, Integer> reservedCircuits) {
        List<WorldgenGTOreLayer> veins = new ArrayList<>(WorldgenGTOreLayer.sList);
        veins.sort(Comparator.comparing(vein -> vein.mWorldGenName));
        for (DimensionHelper.Dimension dimension : DimensionHelper.getAllDim()) {
            Block display = ModBlocks.getBlock(dimension.abbr());
            if (display == null) continue;
            int index = reservedCircuits.getOrDefault(display, 0);
            List<ItemStack> markers = new ArrayList<>();
            markers.add(new ItemStack(display, 0));
            ModsItemlist alternate = switch (dimension.internalName()) {
                case DimNames.DEEPDARK -> ModsItemlist.DarkPortal;
                case DimNames.EVERGLADES -> ModsItemlist.ContainmentFrame;
                case DimNames.ROSS128B -> ModsItemlist.HeavyDutyPlateTier3;
                case DimNames.ROSS128BA -> ModsItemlist.HeavyDutyPlateTier5;
                case DimNames.BARNARDC -> ModsItemlist.BarnardaCDirt;
                default -> null;
            };
            if (alternate != null) {
                ItemStack marker = alternate.get(0);
                if (marker != null) markers.add(marker);
            }
            for (WorldgenGTOreLayer vein : veins) {
                if (!vein.getAllowedDimensions()
                    .contains(dimension.internalName()) || vein.mWeight <= 0) continue;
                ItemStack[] ores = { veinOre(vein.mPrimary, 48), veinOre(vein.mSecondary, 32),
                    veinOre(vein.mBetween, 32), veinOre(vein.mSporadic, 16) };
                int selector = index++;
                if (Arrays.asList(ores)
                    .contains(null)) continue;
                for (ItemStack marker : markers) {
                    veinRecipe(marker, selector).itemOutputs(ores)
                        .duration(Math.max(1, 24000 / vein.mWeight))
                        .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
                }
            }
            if (DimNames.DEEPDARK.equals(dimension.internalName())) addDeepDarkDrops(markers, index);
        }
    }

    private static ItemStack veinOre(IOreMaterial material, int amount) {
        String name = material.getInternalName();
        ItemStack stack = oreDictionary("rawOre" + name, amount);
        if (stack != null) return stack;
        stack = oreDictionary("crushed" + name, amount);
        if (stack != null) return stack;
        try (OreInfo<IOreMaterial> info = OreInfo.getNewInfo()) {
            info.material = material;
            for (var stone : material.getValidStones()) {
                info.stoneType = stone;
                stack = OreManager.getStack(info, amount);
                if (stack != null) return stack;
            }
        }
        unavailable.add("vein ore:" + name);
        return null;
    }

    // The ghost circuit has 24 numbered settings. Page selectors share the spare physical slot with fluid drills.
    private static GTRecipeBuilder veinRecipe(ItemStack marker, int index) {
        int page = index / CIRCUITS_PER_PAGE;
        ItemStack selection = circuit(index % CIRCUITS_PER_PAGE + 1);
        GTRecipeBuilder recipe = GTRecipeBuilder.builder()
            .eut(8)
            .duration(200);
        if (page == 0) return recipe.itemInputs(marker.copy(), selection);
        return recipe.itemInputs(marker.copy(), new ItemStack(Blocks.stained_glass, 0, page - 1), selection);
    }

    private static void addDeepDarkDrops(List<ItemStack> markers, int index) {
        var drops = VoidMinerUtility.dropMapsByDimName.get(DimNames.DEEPDARK);
        if (drops == null) return;
        var entries = new ArrayList<>(
            drops.getInternalMap()
                .entrySet());
        entries.removeIf(entry -> entry.getValue() <= 0);
        entries.sort(
            Map.Entry.comparingByKey(
                Comparator.comparing(
                    (GTUtility.ItemId id) -> Item.itemRegistry.getNameForObject(id.item())
                        .toString())
                    .thenComparingInt(GTUtility.ItemId::metaData)));
        double maxWeight = entries.stream()
            .mapToDouble(Map.Entry::getValue)
            .max()
            .orElse(1);
        for (int start = 0; start < entries.size(); start += DEEP_DARK_GROUP_SIZE) {
            Outputs output = new Outputs();
            for (int i = start; i < Math.min(start + DEEP_DARK_GROUP_SIZE, entries.size()); i++) {
                var entry = entries.get(i);
                int chance = Math.max(1, (int) Math.round(10000 * (entry.getValue() / maxWeight)));
                output.add(
                    entry.getKey()
                        .getItemStack(16),
                    chance);
            }
            for (ItemStack marker : markers) {
                output.apply(veinRecipe(marker, index))
                    .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
            }
            index++;
        }
    }

    private static ItemStack ore(String name, int amount) {
        if (name.equals("Goethite")) name = "BrownLimonite";
        if (name.equals("Hematite")) name = "BandedIron";
        if (name.equals("Plutonium239")) name = "Plutonium";
        ItemStack raw = oreDictionary("rawOre" + name, amount);
        if (raw != null) return raw;
        ItemStack crushed = oreDictionary("crushed" + name, amount);
        if (crushed != null) return crushed;
        unavailable.add("ore:" + name);
        return null;
    }

    private static ItemStack dust(String name, int amount) {
        ItemStack stack = oreDictionary("dust" + name, amount);
        if (stack == null) unavailable.add("dust:" + name);
        return stack;
    }

    private static ItemStack oreDictionary(String name, int amount) {
        for (ItemStack candidate : OreDictionary.getOres(name, false)) {
            if (candidate == null || candidate.getItem() == null) continue;
            return GTUtility.copyAmount(amount, candidate);
        }
        return null;
    }

    private static FluidStack fluid(String name, int amount) {
        String materialName = switch (name) {
            case "RawOil" -> "Oil";
            case "Oil" -> "OilMedium";
            default -> name;
        };
        Materials material = switch (name) {
            case "NetherAir" -> Materials.NetherAir;
            case "NaturalGas" -> Materials.NaturalGas;
            case "Helium3" -> Materials.Helium3;
            case "HydrochloricAcid" -> Materials.HydrochloricAcid;
            default -> Materials.get(materialName);
        };
        if (material != Materials._NULL) {
            FluidStack nativeFluid = material.getFluid(amount);
            if (nativeFluid == null) nativeFluid = material.getGas(amount);
            if (nativeFluid != null) return nativeFluid;
        }
        String nativeName = switch (name) {
            case "RawOil" -> "oil";
            case "Oil" -> "oilmedium";
            case "UnknowWater" -> "unknowwater";
            case "CoalGas" -> "coalgas";
            default -> name.toLowerCase(Locale.ROOT);
        };
        FluidStack stack = FluidRegistry.getFluidStack(nativeName, amount);
        if (stack == null) stack = FluidRegistry.getFluidStack("fluid." + nativeName, amount);
        if (stack == null) unavailable.add("fluid:" + name);
        return stack;
    }

    private static void addSpecialRecipes() {
        Outputs earth = new Outputs();
        earth.add(DIMENSIONS[0].get(1), 50);
        earth.add(new ItemStack(Blocks.dirt, 16), 6000);
        earth.add(new ItemStack(Blocks.gravel, 16), 4000);
        earth.add(new ItemStack(Blocks.sand, 16), 3000);
        earth.add(new ItemStack(Items.clay_ball, 64), 2000);
        for (int meta = 0; meta < 4; meta++) earth.add(new ItemStack(Blocks.sapling, 8, meta), 2000);
        earth.add(ModsItemlist.CherrySapling.get(8), 2000);
        earth.add(ModsItemlist.MangrovePropagule.get(8), 2000);
        earth.apply(base(0, 0))
            .fluidOutputs(new FluidStack(FluidRegistry.LAVA, 1000))
            .fluidOutputChances(500)
            .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);

        Outputs organic = new Outputs();
        organic.add(DIMENSIONS[0].get(1), 50);
        organic.add(new ItemStack(Items.reeds, 8), 2000);
        organic.add(GTModHandler.getIC2Item("rubberSapling", 4), 1000);
        organic.add(new ItemStack(Items.leather, 4), 500);
        organic.add(new ItemStack(Items.string, 8), 500);
        organic.add(ModsItemlist.Honeycomb.get(1), 2000);
        organic.add(new ItemStack(Blocks.soul_sand, 4), 5);
        organic.add(ModsItemlist.TotemOfUndying.get(1), 10);
        organic.apply(base(0, 1))
            .fluidOutputs(Materials.Oil.getFluid(1000))
            .fluidOutputChances(2000)
            .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);

        Outputs ancient = new Outputs();
        ancient.add(DIMENSIONS[3].get(1), 50);
        ancient.add(new ItemStack(Blocks.dirt, 16), 6000);
        ancient.add(ModsItemlist.Diorite.get(16), 6000);
        ancient.add(ModsItemlist.Andesite.get(16), 6000);
        ancient.add(ModsItemlist.Granite.get(16), 6000);
        ancient.add(ModsItemlist.RedGranite.get(16), 6000);
        ancient.add(ModsItemlist.Marble.get(16), 6000);
        ancient.add(new ItemStack(Blocks.sand, 16), 6000);
        ancient.add(new ItemStack(Blocks.gravel, 16), 6000);
        ancient.add(ModsItemlist.SkyStone.get(16), 500);
        ancient.apply(base(3, 0))
            .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);

        Outputs nether = new Outputs();
        nether.add(DIMENSIONS[1].get(1), 50);
        nether.add(new ItemStack(Blocks.soul_sand, 16), 6000);
        nether.add(ModsItemlist.SoulSoil.get(16), 3000);
        nether.add(ModsItemlist.AncientDebris.get(4), 500);
        nether.add(new ItemStack(Items.nether_wart, 12), 200);
        nether.add(ModsItemlist.CrimsonFungus.get(8), 2000);
        nether.add(ModsItemlist.WarpedFungus.get(8), 2000);
        nether.add(new ItemStack(Items.blaze_rod, 8), 500);
        nether.apply(base(1, 0))
            .fluidOutputs(new FluidStack(FluidRegistry.LAVA, 8000))
            .fluidOutputChances(5000)
            .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);

        Outputs end = new Outputs();
        end.add(DIMENSIONS[2].get(1), 50);
        end.add(new ItemStack(Blocks.dragon_egg), 5);
        end.add(ModsItemlist.DragonBreath.get(1), 500);
        end.add(ModsItemlist.ShulkerShell.get(8), 2000);
        end.add(ModsItemlist.ChorusFruit.get(16), 4000);
        end.add(ModsItemlist.ChorusFlower.get(1), 500);
        end.apply(base(2, 0))
            .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);

        Outputs frost = new Outputs();
        frost.add(DIMENSIONS[14].get(1), 50);
        // Ad Astra ice shards become native ice dust; the GTL-only spirit has no native processing chain.
        frost.add(Materials.Ice.getDust(1), 9500);
        frost.apply(base(14, 0))
            .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);

        addBarnardPlants(DIMENSIONS[15]);

        // Twilight now has nine ore circuits; keep the Damascus steel conversion on its own circuit.
        base(3, 10).itemInputs(DIMENSIONS[3].get(0), Materials.Steel.getDust(1), circuit(10))
            .fluidInputs(Materials.Lubricant.getFluid(100))
            .itemOutputs(Materials.DamascusSteel.getDust(1))
            .addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
    }

    private static void addBarnardPlants(ModsItemlist marker) {
        Outputs barnard = new Outputs();
        barnard.add(marker.get(1), 50);
        barnard.add(ModsItemlist.BarnardaCLog.get(1), 500);
        barnard.add(ModsItemlist.BarnardaCLeaves.get(1), 9500);
        GTRecipeBuilder plants = barnard.apply(base(marker, 0));
        FluidStack air = fluid("BarnardaAir", 16000);
        if (air != null) plants.fluidOutputs(air)
            .fluidOutputChances(2000);
        plants.addTo(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
    }

    private static Data readData() {
        String path = "/assets/" + ModList.GTNeverReset.getID() + "/recipes/fragment_world_collection.json";
        try (InputStream input = FragmentWorldCollectorRecipes.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing collector recipe data: " + path);
            return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Data.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load collector recipes", exception);
        }
    }

    private static final class Data {

        String[][] ores;
        int[][] oreCounts;
        String[] fluids;
        int[] fluidAmounts;
        int[][] dimensionCounts;
        int[][] dimensionMaps;
    }

    private static final class Outputs {

        private final List<ItemStack> items = new ArrayList<>();
        private final List<Integer> chances = new ArrayList<>();

        void add(ItemStack stack, int chance) {
            if (stack == null || stack.getItem() == null) return;
            items.add(stack);
            chances.add(chance);
        }

        GTRecipeBuilder apply(GTRecipeBuilder recipe) {
            return recipe.itemOutputs(
                items.toArray(new ItemStack[0]),
                chances.stream()
                    .mapToInt(Integer::intValue)
                    .toArray());
        }
    }
}
