package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_CHANCE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_CONSUMPTION_SCALE;
import static com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes.DRILL_INPUT_INDEX;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBackendPropertiesBuilder;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

public final class FragmentWorldCollectionBackend extends RecipeMapBackend {

    public FragmentWorldCollectionBackend(RecipeMapBackendPropertiesBuilder properties) {
        super(properties);
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
}
