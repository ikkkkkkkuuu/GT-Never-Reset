package com.xyp.gtneverreset.common.crafting.chaos;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/** Credentials store recipe signatures; execution always resolves the current registered recipe. */
public final class ChaosChain {

    public static final int MAX_STEPS = 32;
    public static final List<RecipeMap<?>> SUPPORTED_MAPS = List.of(
        RecipeMaps.maceratorRecipes,
        RecipeMaps.oreWasherRecipes,
        RecipeMaps.thermalCentrifugeRecipes,
        RecipeMaps.centrifugeNonCellRecipes,
        RecipeMaps.electrolyzerNonCellRecipes,
        RecipeMaps.mixerNonCellRecipes,
        RecipeMaps.multiblockChemicalReactorRecipes,
        RecipeMaps.chemicalBathRecipes,
        RecipeMaps.chemicalDehydratorNonCellRecipes,
        RecipeMaps.benderRecipes,
        RecipeMaps.compressorRecipes,
        RecipeMaps.hammerRecipes,
        RecipeMaps.cutterRecipes,
        RecipeMaps.extruderRecipes,
        RecipeMaps.latheRecipes,
        RecipeMaps.wiremillRecipes,
        RecipeMaps.formingPressRecipes,
        RecipeMaps.polarizerRecipes,
        RecipeMaps.laserEngraverRecipes,
        RecipeMaps.fluidSolidifierRecipes,
        RecipeMaps.assemblerRecipes,
        RecipeMaps.circuitAssemblerRecipes,
        RecipeMaps.sifterRecipes,
        RecipeMaps.extractorRecipes,
        RecipeMaps.autoclaveRecipes,
        RecipeMaps.fluidExtractionRecipes,
        RecipeMaps.fluidHeaterRecipes,
        RecipeMaps.distilleryRecipes,
        RecipeMaps.brewingRecipes,
        RecipeMaps.fermentingRecipes);

    private final String name;
    private final List<Step> steps;

    public ChaosChain(String name, List<Step> steps) {
        if (steps.isEmpty() || steps.size() > MAX_STEPS) throw new IllegalArgumentException("invalid chain length");
        this.name = name == null ? "" : name.substring(0, Math.min(80, name.length()));
        this.steps = List.copyOf(steps);
    }

    public String name() {
        return name;
    }

    public List<Step> steps() {
        return steps;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("version", 1);
        tag.setString("name", name);
        NBTTagList list = new NBTTagList();
        for (Step step : steps) list.appendTag(step.save());
        tag.setTag("steps", list);
        return tag;
    }

    public static ChaosChain load(NBTTagCompound tag) {
        if (tag.getInteger("version") != 1) throw new IllegalArgumentException("unknown credential format");
        NBTTagList list = tag.getTagList("steps", 10);
        if (list.tagCount() == 0 || list.tagCount() > MAX_STEPS)
            throw new IllegalArgumentException("invalid chain length");
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound data = list.getCompoundTagAt(i);
            RecipeMap<?> map = RecipeMap.ALL_RECIPE_MAPS.get(data.getString("map"));
            if (map == null || !SUPPORTED_MAPS.contains(map))
                throw new IllegalArgumentException("unsupported recipe map");
            GTRecipe match = null;
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (!safe(recipe) || !signature(recipe).equals(data.getCompoundTag("recipe"))) continue;
                match = recipe;
                break;
            }
            if (match == null) throw new IllegalArgumentException("recipe removed or changed");
            int repeats = data.getInteger("repeats");
            steps.add(new Step(map, match, repeats));
        }
        return new ChaosChain(tag.getString("name"), steps);
    }

    public static boolean safe(GTRecipe recipe) {
        // Heat, cleanrooms, coils, research and other special conditions need a dedicated adapter.
        return recipe.getClass() == GTRecipe.class && recipe.mEnabled
            && !recipe.mFakeRecipe
            && recipe.mCanBeBuffered
            && !recipe.mNeedsEmptyOutput
            && recipe.mEUt > 0
            && recipe.mDuration > 0
            && recipe.mSpecialValue == 0
            && recipe.mSpecialItems == null
            && recipe.getMetadataStorage()
                .getEntries()
                .isEmpty();
    }

    private static NBTTagCompound signature(GTRecipe recipe) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("inputs", itemSignature(recipe.mInputs));
        tag.setTag("outputs", itemSignature(recipe.mOutputs));
        tag.setTag("fluidInputs", fluidSignature(recipe.mFluidInputs));
        tag.setTag("fluidOutputs", fluidSignature(recipe.mFluidOutputs));
        tag.setInteger("eut", recipe.mEUt);
        tag.setInteger("duration", recipe.mDuration);
        tag.setBoolean("nbtSensitive", recipe.isNBTSensitive);
        int[] chances = new int[recipe.mOutputs.length];
        for (int i = 0; i < chances.length; i++) chances[i] = recipe.getOutputChance(i);
        tag.setIntArray("chances", chances);
        int[] inputChances = new int[recipe.mInputs.length];
        for (int i = 0; i < inputChances.length; i++) inputChances[i] = recipe.getInputChance(i);
        tag.setIntArray("inputChances", inputChances);
        int[] fluidInputChances = new int[recipe.mFluidInputs.length];
        for (int i = 0; i < fluidInputChances.length; i++) fluidInputChances[i] = recipe.getFluidInputChance(i);
        tag.setIntArray("fluidInputChances", fluidInputChances);
        int[] fluidOutputChances = new int[recipe.mFluidOutputs.length];
        for (int i = 0; i < fluidOutputChances.length; i++) fluidOutputChances[i] = recipe.getFluidOutputChance(i);
        tag.setIntArray("fluidOutputChances", fluidOutputChances);
        return tag;
    }

    public static boolean equivalent(GTRecipe first, GTRecipe second) {
        return signature(first).equals(signature(second));
    }

    private static NBTTagList itemSignature(ItemStack[] items) {
        NBTTagList list = new NBTTagList();
        for (ItemStack item : items) {
            NBTTagCompound tag = new NBTTagCompound();
            if (item != null) gregtech.api.util.GTUtility.saveItem(tag, "stack", item);
            list.appendTag(tag);
        }
        return list;
    }

    private static NBTTagList fluidSignature(net.minecraftforge.fluids.FluidStack[] fluids) {
        NBTTagList list = new NBTTagList();
        for (var fluid : fluids) {
            NBTTagCompound tag = new NBTTagCompound();
            if (fluid != null) tag.setTag("stack", fluid.writeToNBT(new NBTTagCompound()));
            list.appendTag(tag);
        }
        return list;
    }

    public record Step(RecipeMap<?> map, GTRecipe recipe, int repeats) {

        public Step {
            if (map == null || !SUPPORTED_MAPS.contains(map)
                || recipe == null
                || !safe(recipe)
                || repeats < 1
                || repeats > 4096) throw new IllegalArgumentException("invalid processing step");
        }

        private NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("map", map.unlocalizedName);
            tag.setTag("recipe", signature(recipe));
            tag.setInteger("repeats", repeats);
            return tag;
        }
    }
}
