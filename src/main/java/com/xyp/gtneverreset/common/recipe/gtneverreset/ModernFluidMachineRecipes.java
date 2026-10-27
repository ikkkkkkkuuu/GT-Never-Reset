package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static com.xyp.gtneverreset.loader.GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES;
import static com.xyp.gtneverreset.loader.GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES;
import static com.xyp.gtneverreset.loader.GTNRRecipeMaps.MODERN_MIXER_RECIPES;
import static gregtech.api.util.GTRecipeConstants.SKIP_CELL_RECIPE_GENERATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.GTNeverReset;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.GTRecipe_WithAlt;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class ModernFluidMachineRecipes {

    private static boolean recipesConverted;

    private ModernFluidMachineRecipes() {}

    /** Converts only ordinary small-machine recipes, after the pack has finished adding them. */
    public static void registerLateRecipes() {
        if (recipesConverted) return;
        recipesConverted = true;
        convertRecipes(RecipeMaps.mixerRecipes, MODERN_MIXER_RECIPES, 6, 1, 2, 1);
        convertRecipes(RecipeMaps.electrolyzerRecipes, MODERN_ELECTROLYZER_RECIPES, 2, 6, 1, 6);
        convertRecipes(RecipeMaps.chemicalReactorRecipes, MODERN_CHEMICAL_RECIPES, 2, 2, 3, 2);
    }

    private static void convertRecipes(RecipeMap<?> source, RecipeMap<?> target, int itemInputs, int itemOutputs,
        int fluidInputs, int fluidOutputs) {
        int converted = 0;
        for (GTRecipe recipe : source.getAllRecipes()) {
            if (!recipe.mEnabled || recipe.mFakeRecipe
                || recipe.getMetadataOrDefault(SKIP_CELL_RECIPE_GENERATION, false)
                || !hasBalancedContainers(recipe)) continue;
            ConvertedSide inputs = convertSide(recipe, true);
            ConvertedSide outputs = convertSide(recipe, false);
            if (inputs == null || outputs == null
                || inputs.items.size() > itemInputs
                || outputs.items.size() > itemOutputs
                || inputs.fluids.size() > fluidInputs
                || outputs.fluids.size() > fluidOutputs) continue;
            GTRecipe adapted = buildConvertedRecipe(recipe, inputs, outputs);
            if (adapted != null) {
                adapted.setRecipeCategory(target.getDefaultRecipeCategory());
                target.add(adapted);
                converted++;
            }
        }
        GTNeverReset.LOG.info("loaded {} direct-fluid recipes for {}", converted, target.unlocalizedName);
    }

    private static ConvertedSide convertSide(GTRecipe recipe, boolean input) {
        ConvertedSide side = new ConvertedSide();
        ItemStack[] items = input ? recipe.mInputs : recipe.mOutputs;
        GTRecipe_WithAlt alternatives = recipe instanceof GTRecipe_WithAlt alt ? alt : null;
        for (int index = 0; index < items.length; index++) {
            ItemStack item = items[index];
            if (item == null) continue;
            // A zero-sized stack is a catalyst, including a fluid container used as a catalyst.
            int chance = input ? recipe.getInputChance(index) : recipe.getOutputChance(index);
            FluidStack fluid = item.stackSize > 0 ? GTUtility.getFluidForFilledItem(item, true) : null;
            if (fluid != null) {
                long amount = (long) fluid.amount * item.stackSize;
                if (amount > Integer.MAX_VALUE) return null;
                fluid = fluid.copy();
                fluid.amount = (int) amount;
                FluidStack[] choices = null;
                if (input && alternatives != null
                    && alternatives.mOreDictAlt != null
                    && index < alternatives.mOreDictAlt.length
                    && alternatives.mOreDictAlt[index] != null) {
                    List<FluidStack> fluidChoices = new ArrayList<>();
                    for (ItemStack choice : alternatives.mOreDictAlt[index]) {
                        if (choice == null) continue;
                        FluidStack alternativeFluid = GTUtility.getFluidForFilledItem(choice, true);
                        if (alternativeFluid == null || choice.stackSize <= 0) return null;
                        long alternativeAmount = (long) alternativeFluid.amount * choice.stackSize;
                        if (alternativeAmount > Integer.MAX_VALUE) return null;
                        alternativeFluid = alternativeFluid.copy();
                        alternativeFluid.amount = (int) alternativeAmount;
                        fluidChoices.add(alternativeFluid);
                    }
                    if (fluidChoices.isEmpty()) return null;
                    if (fluidChoices.size() > 1 || !fluidChoices.get(0)
                        .isFluidEqual(fluid) || fluidChoices.get(0).amount != fluid.amount) {
                        choices = fluidChoices.toArray(new FluidStack[0]);
                    }
                }
                if (!side.addFluid(fluid, chance, choices)) return null;
            } else if (item.stackSize <= 0 || !isEmptyContainer(item)) {
                side.items.add(item.copy());
                side.itemChances.add(chance);
                side.itemAlternatives.add(
                    input && alternatives != null
                        && alternatives.mOreDictAlt != null
                        && index < alternatives.mOreDictAlt.length ? copyItems(alternatives.mOreDictAlt[index]) : null);
                side.oreDictIds.add(
                    input && alternatives != null
                        && alternatives.mOreDictIds != null
                        && index < alternatives.mOreDictIds.length ? alternatives.mOreDictIds[index] : -1);
            }
        }
        FluidStack[] fluids = input ? recipe.mFluidInputs : recipe.mFluidOutputs;
        for (int index = 0; index < fluids.length; index++) {
            if (fluids[index] == null) continue;
            FluidStack[] choices = input && recipe.mAltFluidInputs != null && index < recipe.mAltFluidInputs.length
                ? copyFluids(recipe.mAltFluidInputs[index])
                : null;
            if (!side.addFluid(
                fluids[index].copy(),
                input ? recipe.getFluidInputChance(index) : recipe.getFluidOutputChance(index),
                choices)) return null;
        }
        return side;
    }

    private static GTRecipe buildConvertedRecipe(GTRecipe original, ConvertedSide inputs, ConvertedSide outputs) {
        // GTRecipe.copy() drops metadata in this GT5U version, so rebuild through the public builder.
        GTRecipeBuilder builder = GTValues.RA.stdBuilder()
            .itemInputsUnified(inputs.items.toArray(new ItemStack[0]))
            .forceOreDictInput()
            .inputChances(toIntArray(inputs.itemChances))
            .itemOutputs(outputs.items.toArray(new ItemStack[0]))
            .outputChances(toIntArray(outputs.itemChances))
            .fluidInputs(inputs.fluids.toArray(new FluidStack[0]))
            .fluidInputChances(toIntArray(inputs.fluidChances))
            .fluidOutputs(outputs.fluids.toArray(new FluidStack[0]))
            .fluidOutputChances(toIntArray(outputs.fluidChances))
            .special(original.mSpecialItems)
            .specialValue(original.mSpecialValue)
            .duration(original.mDuration)
            .eut(original.mEUt)
            .setNEIDesc(original.getNeiDesc());
        if (original.mHidden) builder.hidden();
        if (!original.mCanBeBuffered) builder.noBuffer();
        if (original.mNeedsEmptyOutput) builder.needsEmptyOutput();
        if (original.isNBTSensitive) builder.nbtSensitive();
        for (Map.Entry<RecipeMetadataKey<?>, Object> entry : original.getMetadataStorage()
            .getEntries()) {
            copyMetadata(builder, entry.getKey(), entry.getValue());
        }
        GTRecipe_WithAlt adapted = builder.buildWithAlt()
            .orElse(null);
        if (adapted == null) return null;
        adapted.mOreDictAlt = inputs.itemAlternatives.toArray(new ItemStack[0][]);
        adapted.mOreDictIds = toIntArray(inputs.oreDictIds);
        adapted.mAltFluidInputs = inputs.fluidAlternatives.toArray(new FluidStack[0][]);
        adapted.owners = original.owners == null ? null : new ArrayList<>(original.owners);
        return adapted;
    }

    private static <T> void copyMetadata(GTRecipeBuilder builder, RecipeMetadataKey<T> key, Object value) {
        builder.metadata(key, key.cast(value));
    }

    private static boolean hasBalancedContainers(GTRecipe recipe) {
        long[] balance = new long[4];
        boolean[] hasFilledContainer = new boolean[4];
        ItemStack emptyCell = ItemList.Cell_Empty.get(1);
        for (int side = 0; side < 2; side++) {
            for (ItemStack item : side == 0 ? recipe.mInputs : recipe.mOutputs) {
                if (item == null || item.stackSize <= 0) continue;
                // IC2 registers this legacy cell without a fluid; preserve GT++'s exclusion.
                if (item.getItem() == emptyCell.getItem() && item.getItemDamage() == 8) return false;
                int index = emptyContainerIndex(item);
                if (index < 0 && GTUtility.getFluidForFilledItem(item, true) != null) {
                    ItemStack container = GTUtility.getContainerForFilledItem(item, true);
                    if (container == null || (index = emptyContainerIndex(container)) < 0) return false;
                    hasFilledContainer[index] = true;
                }
                if (index >= 0) balance[index] += side == 0 ? item.stackSize : -item.stackSize;
            }
        }
        for (int index = 0; index < balance.length; index++) {
            if (balance[index] != 0) return false;
        }
        // Empty containers with no filled counterpart are ingredients, not fluid packaging.
        for (ItemStack item : recipe.mInputs) {
            if (item == null || item.stackSize <= 0) continue;
            int index = emptyContainerIndex(item);
            if (index >= 0 && !hasFilledContainer[index]) return false;
        }
        return true;
    }

    private static boolean isEmptyContainer(ItemStack item) {
        return emptyContainerIndex(item) >= 0;
    }

    private static int emptyContainerIndex(ItemStack item) {
        if (GTUtility.areStacksEqual(item, ItemList.Cell_Empty.get(1), true)) return 0;
        if (item.getItemDamage() != 0) return -1;
        if (item.getItem() == Items.bucket) return 1;
        if (item.getItem() == Items.glass_bottle) return 2;
        if (item.getItem() == Items.bowl) return 3;
        return -1;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < result.length; index++) result[index] = values.get(index);
        return result;
    }

    private static ItemStack[] copyItems(ItemStack[] items) {
        if (items == null) return null;
        ItemStack[] copy = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            if (items[index] != null) copy[index] = items[index].copy();
        }
        return copy;
    }

    private static FluidStack[] copyFluids(FluidStack[] fluids) {
        if (fluids == null) return null;
        FluidStack[] copy = new FluidStack[fluids.length];
        for (int index = 0; index < fluids.length; index++) {
            if (fluids[index] != null) copy[index] = fluids[index].copy();
        }
        return copy;
    }

    private static final class ConvertedSide {

        private final List<ItemStack> items = new ArrayList<>();
        private final List<Integer> itemChances = new ArrayList<>();
        private final List<ItemStack[]> itemAlternatives = new ArrayList<>();
        private final List<Integer> oreDictIds = new ArrayList<>();
        private final List<FluidStack> fluids = new ArrayList<>();
        private final List<Integer> fluidChances = new ArrayList<>();
        private final List<FluidStack[]> fluidAlternatives = new ArrayList<>();

        private boolean addFluid(FluidStack fluid, int chance, FluidStack[] alternatives) {
            if (alternatives != null && alternatives.length == 1
                && alternatives[0] != null
                && alternatives[0].isFluidEqual(fluid)
                && alternatives[0].amount == fluid.amount) alternatives = null;
            if (chance == 10000 && alternatives == null && fluid.amount > 0) {
                for (int index = 0; index < fluids.size(); index++) {
                    FluidStack existing = fluids.get(index);
                    if (fluidChances.get(index) == 10000 && fluidAlternatives.get(index) == null
                        && existing.amount > 0
                        && existing.isFluidEqual(fluid)) {
                        long amount = (long) existing.amount + fluid.amount;
                        if (amount > Integer.MAX_VALUE) return false;
                        existing.amount = (int) amount;
                        return true;
                    }
                }
            }
            fluids.add(fluid);
            fluidChances.add(chance);
            fluidAlternatives.add(alternatives);
            return true;
        }
    }
}
