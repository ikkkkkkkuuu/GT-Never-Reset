package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static java.math.MathContext.DECIMAL128;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.GTValues;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility.FluidId;
import gregtech.api.util.GTUtility.ItemId;

/** Balances the native stages of one Platinum Gate recipe before it is registered. */
final class PlatinumGateBatch {

    private static final BigDecimal ROUNDING_TOLERANCE = new BigDecimal("1e-20");

    private final Map<ItemId, BigDecimal> items = new LinkedHashMap<>();
    private final Map<FluidId, BigDecimal> fluids = new LinkedHashMap<>();
    private final Set<ItemId> producedItems = new HashSet<>();
    private final Set<FluidId> producedFluids = new HashSet<>();

    private BigDecimal energy = BigDecimal.ZERO;
    private int maxEut;

    void add(GTRecipe recipe, long repeats) {
        add(recipe, BigDecimal.valueOf(repeats));
    }

    void add(GTRecipe recipe, BigDecimal repeats) {
        if (repeats.signum() <= 0 || recipe.mDuration <= 0 || recipe.mEUt <= 0) {
            throw new IllegalArgumentException("A native stage needs positive repetitions, duration and EU/t");
        }
        for (int i = 0; i < recipe.mInputs.length; i++) {
            ItemStack input = recipe.mInputs[i];
            if (input == null) continue;
            int chance = recipe.getInputChance(i);
            if (input.stackSize > 0 && chance > 0) {
                subtract(items, ItemId.create(input), expected(input.stackSize, chance), repeats);
            }
        }
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            FluidStack input = recipe.mFluidInputs[i];
            if (input == null) continue;
            int chance = recipe.getFluidInputChance(i);
            if (input.amount > 0 && chance > 0) {
                subtract(fluids, FluidId.create(input), expected(input.amount, chance), repeats);
            }
        }
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack output = recipe.mOutputs[i];
            if (output != null) {
                credit(
                    items,
                    producedItems,
                    ItemId.create(output),
                    expected(output.stackSize, recipe.getOutputChance(i)),
                    repeats);
            }
        }
        for (int i = 0; i < recipe.mFluidOutputs.length; i++) {
            FluidStack output = recipe.mFluidOutputs[i];
            if (output != null) {
                credit(
                    fluids,
                    producedFluids,
                    FluidId.create(output),
                    expected(output.amount, recipe.getFluidOutputChance(i)),
                    repeats);
            }
        }
        BigDecimal ticks = BigDecimal.valueOf(recipe.mDuration)
            .multiply(repeats, DECIMAL128);
        energy = energy.add(ticks.multiply(BigDecimal.valueOf(recipe.mEUt), DECIMAL128), DECIMAL128);
        maxEut = Math.max(maxEut, recipe.mEUt);
    }

    void consume(GTRecipe recipe, ItemStack intermediate) {
        ItemId id = ItemId.create(intermediate);
        BigDecimal amount = BigDecimal.ZERO;
        for (int i = 0; i < recipe.mInputs.length; i++) {
            ItemStack input = recipe.mInputs[i];
            if (input != null && id.matches(input)) {
                amount = amount.add(expected(input.stackSize, recipe.getInputChance(i)));
            }
        }
        add(recipe, repeatsFor(balance(items, producedItems, id), amount, intermediate.toString()));
    }

    void consume(GTRecipe recipe, FluidStack intermediate) {
        FluidId id = FluidId.create(intermediate);
        BigDecimal amount = BigDecimal.ZERO;
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            FluidStack input = recipe.mFluidInputs[i];
            if (input != null && id.matches(input)) {
                amount = amount.add(expected(input.amount, recipe.getFluidInputChance(i)));
            }
        }
        add(
            recipe,
            repeatsFor(
                balance(fluids, producedFluids, id),
                amount,
                intermediate.getFluid()
                    .getName()));
    }

    void replenish(GTRecipe recipe, ItemStack reagent) {
        replenish(recipe, reagent, itemBalance(reagent).negate());
    }

    void replenish(GTRecipe recipe, ItemStack reagent, BigDecimal recoveryLimit) {
        BigDecimal needed = itemBalance(reagent).negate()
            .min(recoveryLimit);
        if (needed.signum() <= 0) return;
        ItemId id = ItemId.create(reagent);
        BigDecimal amount = BigDecimal.ZERO;
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack output = recipe.mOutputs[i];
            if (output != null && id.matches(output)) {
                amount = amount.add(expected(output.stackSize, recipe.getOutputChance(i)));
            }
        }
        add(recipe, repeatsFor(needed, amount, reagent.toString()));
    }

    void replenish(GTRecipe recipe, FluidStack reagent) {
        replenish(recipe, reagent, fluidBalance(reagent).negate());
    }

    void replenish(GTRecipe recipe, FluidStack reagent, BigDecimal recoveryLimit) {
        BigDecimal needed = fluidBalance(reagent).negate()
            .min(recoveryLimit);
        if (needed.signum() <= 0) return;
        BigDecimal amount = BigDecimal.ZERO;
        for (int i = 0; i < recipe.mFluidOutputs.length; i++) {
            FluidStack output = recipe.mFluidOutputs[i];
            if (output != null && reagent.isFluidEqual(output)) {
                amount = amount.add(expected(output.amount, recipe.getFluidOutputChance(i)));
            }
        }
        add(
            recipe,
            repeatsFor(
                needed,
                amount,
                reagent.getFluid()
                    .getName()));
    }

    void replaceItemInput(ItemStack nativeInput, ItemStack gateInput) {
        ItemId nativeId = ItemId.create(nativeInput);
        BigDecimal amount = balance(items, producedItems, nativeId);
        if (amount.signum() >= 0) throw new IllegalStateException("Missing native feed " + nativeInput);
        items.remove(nativeId);
        items.merge(ItemId.create(gateInput), amount, PlatinumGateBatch::addAmounts);
    }

    BigDecimal itemBalance(ItemStack stack) {
        return balance(items, producedItems, ItemId.create(stack));
    }

    BigDecimal fluidBalance(FluidStack stack) {
        return balance(fluids, producedFluids, FluidId.create(stack));
    }

    int maxEut() {
        return maxEut;
    }

    BigDecimal energy() {
        return energy;
    }

    GTRecipeBuilder build(ItemStack[] terminalItems, FluidStack[] terminalFluids) {
        List<ItemStack> itemInputs = new ArrayList<>();
        for (ItemId id : items.keySet()) {
            BigDecimal amount = balance(items, producedItems, id);
            if (amount.signum() < 0) itemInputs.add(id.getItemStack(roundAmount(amount.negate(), CEILING)));
        }
        List<FluidStack> fluidInputs = new ArrayList<>();
        for (FluidId id : fluids.keySet()) {
            BigDecimal amount = balance(fluids, producedFluids, id);
            if (amount.signum() < 0) fluidInputs.add(id.getFluidStack(roundAmount(amount.negate(), CEILING)));
        }
        if (itemInputs.isEmpty() && fluidInputs.isEmpty()) {
            throw new IllegalStateException("A merged recipe must consume external materials");
        }
        Set<ItemId> itemOutputIds = new LinkedHashSet<>();
        for (ItemStack stack : terminalItems) itemOutputIds.add(ItemId.create(stack));
        List<ItemStack> itemOutputs = new ArrayList<>();
        for (ItemId id : itemOutputIds) {
            BigDecimal stock = balance(items, producedItems, id);
            if (stock.signum() <= 0) continue;
            int amount = roundAmount(stock, FLOOR);
            if (amount > 0) itemOutputs.add(id.getItemStack(amount));
        }
        Set<FluidId> fluidOutputIds = new LinkedHashSet<>();
        for (FluidStack stack : terminalFluids) fluidOutputIds.add(FluidId.create(stack));
        List<FluidStack> fluidOutputs = new ArrayList<>();
        for (FluidId id : fluidOutputIds) {
            BigDecimal stock = balance(fluids, producedFluids, id);
            if (stock.signum() <= 0) continue;
            int amount = roundAmount(stock, FLOOR);
            if (amount > 0) fluidOutputs.add(id.getFluidStack(amount));
        }
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            throw new IllegalStateException("A merged recipe must have a positive terminal output");
        }

        return GTValues.RA.stdBuilder()
            .itemInputsUnified(itemInputs.toArray(ItemStack[]::new))
            .fluidInputs(fluidInputs.toArray(FluidStack[]::new))
            .itemOutputs(itemOutputs.toArray(ItemStack[]::new))
            .fluidOutputs(fluidOutputs.toArray(FluidStack[]::new));
    }

    private static BigDecimal expected(int amount, int chance) {
        return BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(chance, 4));
    }

    private static BigDecimal repeatsFor(BigDecimal stock, BigDecimal input, String name) {
        if (stock.signum() <= 0 || input.signum() <= 0) {
            throw new IllegalStateException("Cannot balance " + name + " with stock " + stock + " and input " + input);
        }
        return stock.divide(input, DECIMAL128);
    }

    private static <K> void subtract(Map<K, BigDecimal> amounts, K id, BigDecimal amount, BigDecimal repeats) {
        amounts.merge(
            id,
            amount.multiply(repeats, DECIMAL128)
                .negate(),
            PlatinumGateBatch::addAmounts);
    }

    private static <K> void credit(Map<K, BigDecimal> amounts, Set<K> produced, K id, BigDecimal amount,
        BigDecimal repeats) {
        if (amount.signum() <= 0) return;
        amounts.merge(id, amount.multiply(repeats, DECIMAL128), PlatinumGateBatch::addAmounts);
        produced.add(id);
    }

    private static BigDecimal addAmounts(BigDecimal left, BigDecimal right) {
        return left.add(right, DECIMAL128);
    }

    private static <K> BigDecimal balance(Map<K, BigDecimal> amounts, Set<K> produced, K id) {
        BigDecimal amount = amounts.getOrDefault(id, BigDecimal.ZERO);
        return produced.contains(id) && amount.abs()
            .compareTo(ROUNDING_TOLERANCE) < 0 ? BigDecimal.ZERO : amount;
    }

    private static int roundAmount(BigDecimal amount, RoundingMode mode) {
        BigDecimal nearest = amount.setScale(0, RoundingMode.HALF_EVEN);
        if (nearest.signum() > 0 && amount.subtract(nearest)
            .abs()
            .compareTo(ROUNDING_TOLERANCE) < 0) {
            return nearest.intValueExact();
        }
        return amount.setScale(0, mode)
            .intValueExact();
    }
}
