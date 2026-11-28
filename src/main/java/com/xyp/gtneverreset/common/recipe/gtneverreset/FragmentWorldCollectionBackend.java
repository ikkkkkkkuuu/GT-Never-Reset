package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_CHANCE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_SCALE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_INPUT_INDEX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBackendPropertiesBuilder;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

public final class FragmentWorldCollectionBackend extends RecipeMapBackend {

    private DimensionRecipeIndex dimensionRecipeIndex;

    public FragmentWorldCollectionBackend(RecipeMapBackendPropertiesBuilder properties) {
        super(properties);
    }

    @Override
    public GTRecipe compileRecipe(GTRecipe recipe) {
        dimensionRecipeIndex = null;
        return super.compileRecipe(recipe);
    }

    @Override
    public void removeRecipes(Collection<? extends GTRecipe> recipes) {
        dimensionRecipeIndex = null;
        super.removeRecipes(recipes);
    }

    @Override
    public void clearRecipes() {
        dimensionRecipeIndex = null;
        super.clearRecipes();
    }

    @Override
    public void reInit() {
        dimensionRecipeIndex = null;
        super.reInit();
    }

    public List<GTRecipe> getDimensionRecipes(ItemStack marker) {
        if (marker == null) return List.of();
        return getDimensionRecipeIndex().recipes()
            .getOrDefault(GTUtility.ItemId.createNoCopy(marker), List.of());
    }

    public int getDrillWear(ItemStack stack) {
        return getDimensionRecipeIndex().drillWear()
            .getOrDefault(GTUtility.ItemId.createNoCopy(stack), 0);
    }

    public static boolean isFluidCollection(GTRecipe recipe) {
        return recipe.getMetadataOrDefault(DRILL_CONSUMPTION_CHANCE, 0) > 0;
    }

    public static List<ItemStack> getDimensionRecipeInputs(GTRecipe recipe, ItemStack marker,
        List<ItemStack> available) {
        List<ItemStack> inputs = new ArrayList<>(recipe.mInputs.length);
        inputs.add(marker);
        for (int index = 1; index < recipe.mInputs.length; index++) {
            ItemStack required = recipe.mInputs[index];
            if (required == null) continue;
            if (isAutomaticSelector(required)) {
                inputs.add(GTUtility.copyAmount(1, required));
            } else {
                for (ItemStack supplied : available) {
                    if (supplied.stackSize > 0 && GTUtility.areStacksEqual(supplied, required)) inputs.add(supplied);
                }
            }
        }
        return inputs;
    }

    public static void consumeDrillHeads(GTRecipe recipe, ItemStack[] inputs, int operations, IntUnaryOperator random) {
        int chance = recipe.getMetadataOrDefault(DRILL_CONSUMPTION_CHANCE, 0);
        if (chance == 0) return;
        int consumed = rollDrillConsumption(operations, chance, random);
        ItemStack required = recipe.mInputs[DRILL_INPUT_INDEX];
        for (ItemStack input : inputs) {
            if (consumed == 0) break;
            if (input == null || input.stackSize <= 0 || !GTUtility.areStacksEqual(input, required)) continue;
            int amount = Math.min(consumed, input.stackSize);
            input.stackSize -= amount;
            consumed -= amount;
        }
    }

    private static int rollDrillConsumption(int operations, int chance, IntUnaryOperator random) {
        if (operations == 1) return random.applyAsInt(DRILL_CONSUMPTION_SCALE) < chance ? 1 : 0;
        if (chance >= DRILL_CONSUMPTION_SCALE) return operations;
        // At these low wear rates, sample the geometric gaps between worn heads instead of visiting every parallel.
        double logMiss = Math.log1p(-chance / (double) DRILL_CONSUMPTION_SCALE);
        ThreadLocalRandom sampler = ThreadLocalRandom.current();
        int consumed = 0;
        long remaining = operations;
        while (remaining > 0) {
            long misses = (long) (Math.log1p(-sampler.nextDouble()) / logMiss);
            remaining -= misses + 1;
            if (remaining >= 0) consumed++;
        }
        return consumed;
    }

    public boolean matchesCollectorInputs(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids) {
        return filterFindRecipe(recipe, items, fluids, null, false);
    }

    @Override
    protected boolean filterFindRecipe(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids, ItemStack specialSlot,
        boolean dontCheckStackSizes) {
        if (!super.filterFindRecipe(recipe, items, fluids, specialSlot, dontCheckStackSizes)) return false;
        // A drill must select fluid collection even when an ore recipe uses the same circuit.
        for (ItemStack supplied : items) {
            if (supplied == null) continue;
            boolean matched = false;
            for (ItemStack required : recipe.mInputs) {
                if (GTUtility.areStacksEqual(supplied, required)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    private DimensionRecipeIndex getDimensionRecipeIndex() {
        if (dimensionRecipeIndex != null) return dimensionRecipeIndex;
        Map<GTUtility.ItemId, List<GTRecipe>> recipesByDimension = new HashMap<>();
        Map<GTUtility.ItemId, Integer> drillWear = new HashMap<>();
        for (GTRecipe recipe : getAllRecipes()) {
            if (recipe.mFakeRecipe || recipe.mInputs.length < 2
                || recipe.mInputs[0] == null
                || recipe.mInputs[0].stackSize != 0) continue;
            boolean hasCircuit = false;
            for (ItemStack input : recipe.mInputs) {
                if (input != null && input.stackSize == 0 && GTUtility.isAnyIntegratedCircuit(input)) {
                    hasCircuit = true;
                    break;
                }
            }
            if (!hasCircuit) continue;
            recipesByDimension.computeIfAbsent(GTUtility.ItemId.create(recipe.mInputs[0]), ignored -> new ArrayList<>())
                .add(recipe);
            if (isFluidCollection(recipe)) {
                drillWear.put(
                    GTUtility.ItemId.create(recipe.mInputs[DRILL_INPUT_INDEX]),
                    recipe.getMetadataOrDefault(DRILL_CONSUMPTION_CHANCE, 0));
            }
        }
        recipesByDimension.replaceAll((dimension, recipes) -> List.copyOf(recipes));
        dimensionRecipeIndex = new DimensionRecipeIndex(recipesByDimension, drillWear);
        return dimensionRecipeIndex;
    }

    private static boolean isAutomaticSelector(ItemStack stack) {
        return stack.stackSize == 0 && (GTUtility.isAnyIntegratedCircuit(stack)
            || stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass));
    }

    private record DimensionRecipeIndex(Map<GTUtility.ItemId, List<GTRecipe>> recipes,
        Map<GTUtility.ItemId, Integer> drillWear) {}
}
