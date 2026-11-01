package com.xyp.gtneverreset.common.me.overloaded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.me.CircuitPatternMolds;
import com.xyp.gtneverreset.common.me.CircuitPatternQuantities;
import com.xyp.gtneverreset.common.me.CircuitRecipeInputs;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.common.items.ItemIntegratedCircuit;

/** Matches complete AE patterns to deterministic GT recipes, including virtual catalysts. */
public final class OverloadedMachineRecipeMatcher {

    /** Weak pattern keys release cached matches when AE replaces or removes its pattern details. */
    private static final Map<RecipeMap<?>, Map<ICraftingPatternDetails, List<Match>>> MATCHES = new WeakHashMap<>();

    private OverloadedMachineRecipeMatcher() {}

    /**
     * Resolves the exact encoded ingredients for a read-only pattern classifier. Unlike push candidates,
     * every returned configuration must match the encoded material itself, not just an allowed CPU substitute.
     * No machine voltage filter is applied: this describes recipe configuration, not current executability.
     *
     * @param map     user-selected machine recipe map
     * @param pattern decoded processing pattern with catalysts omitted
     * @return distinct circuit-damage / mold-catalog-index pairs; -1 denotes an absent catalyst
     */
    public static List<int[]> patternConfigurations(RecipeMap<?> map, ICraftingPatternDetails pattern) {
        List<int[]> result = new ArrayList<>();
        if (map == null || pattern == null || pattern.isCraftable()) return result;
        Map<Ingredient, Long> inputs = new HashMap<>();
        Map<Ingredient, Long> outputs = new HashMap<>();
        for (IAEStack<?> stack : pattern.getAEInputs()) {
            if (stack != null && !addAE(inputs, stack, false)) return result;
        }
        for (IAEStack<?> stack : pattern.getAEOutputs()) {
            if (stack != null && !addAE(outputs, stack, true)) return result;
        }
        for (Match match : matches(map, pattern)) {
            long batches = CircuitPatternQuantities.requestedOutputBatches(match.outputs, outputs);
            if (batches <= 0 || matchingInputs(match, inputs) != batches) continue;
            int circuit = match.circuit == null ? -1 : match.circuit.getItemDamage();
            int mold = OverloadedMachineMolds.indexOf(match.mold);
            if (result.stream()
                .noneMatch(pair -> pair[0] == circuit && pair[1] == mold)) {
                result.add(new int[] { circuit, mold });
            }
        }
        return result;
    }

    /** Caches full recipe comparisons; repeated busy retries do not scan the recipe registry. */
    static List<Match> matches(RecipeMap<?> map, ICraftingPatternDetails pattern) {
        Map<ICraftingPatternDetails, List<Match>> patterns = MATCHES
            .computeIfAbsent(map, ignored -> new WeakHashMap<>());
        return patterns.computeIfAbsent(pattern, ignored -> {
            List<Match> result = new ArrayList<>();
            Map<Ingredient, Long> encoded = new HashMap<>();
            Map<Ingredient, Long> outputs = new HashMap<>();
            for (IAEStack<?> stack : pattern.getAEInputs()) {
                if (stack != null && !addAE(encoded, stack, false)) return result;
            }
            boolean hasOutput = false;
            for (IAEStack<?> stack : pattern.getAEOutputs()) {
                if (stack == null) continue;
                if (!addAE(outputs, stack, true)) return result;
                hasOutput = true;
            }
            if (!hasOutput) return result;
            boolean validEncodedRecipe = false;
            ItemStack[] encodedItems = itemInputs(encoded);
            FluidStack[] encodedFluids = fluidInputs(encoded);
            if (encodedItems == null || encodedFluids == null) return result;
            for (GTRecipe recipe : map.getAllRecipes()) {
                Match match = describe(recipe);
                if (match == null || !matchesConfiguration(pattern, match)) continue;
                long operations = CircuitPatternQuantities.requestedOutputBatches(match.outputs, outputs);
                if (operations <= 0
                    || CircuitRecipeInputs.quantityBatches(recipe, encodedItems, encodedFluids) != operations) continue;
                // Retain registered variants with the same yield and quantity ratio. The CPU may supply an
                // allowed substitute, but both the encoded pattern and actual delivery must be valid GT inputs.
                result.add(match);
                if (CircuitRecipeInputs.batches(
                    recipe,
                    match.circuit,
                    OverloadedMachineMolds.append(encodedItems, match.mold),
                    encodedFluids) == operations) validEncodedRecipe = true;
            }
            if (!validEncodedRecipe) result.clear();
            return result;
        });
    }

    static boolean matchesConfiguration(ICraftingPatternDetails pattern, Match match) {
        ItemStack stack = pattern.getPattern();
        if (stack == null || !stack.hasTagCompound()) return true;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("gtnrPatternCircuit")) {
            if (!tag.hasKey("gtnrPatternCircuit", Constants.NBT.TAG_INT) || match.circuit == null
                || match.circuit.getItemDamage() != tag.getInteger("gtnrPatternCircuit")) return false;
        }
        if (tag.hasKey("gtnrPatternMolds")) {
            if (!tag.hasKey("gtnrPatternMolds", Constants.NBT.TAG_LIST)) return false;
            NBTTagList molds = tag.getTagList("gtnrPatternMolds", Constants.NBT.TAG_COMPOUND);
            if (molds.tagCount() != 1 || match.mold == null) return false;
            ItemStack mold = ItemStack.loadItemStackFromNBT(molds.getCompoundTagAt(0));
            if (OverloadedMachineMolds.indexOf(mold) < 0 || !CircuitPatternMolds.isNonConsumed(match.recipe, mold))
                return false;
            match.mold = mold.copy();
            match.mold.stackSize = 0;
        }
        return true;
    }

    /** Accepts one circuit and one catalog mold as zero-size catalysts; excludes unsupported/probabilistic recipes. */
    static Match describe(GTRecipe recipe) {
        if (recipe == null || !recipe.mEnabled
            || recipe.mFakeRecipe
            || recipe.mSpecialItems != null
            || !CircuitRecipeInputs.deterministic(recipe)) return null;
        Match match = new Match(recipe);
        for (ItemStack item : recipe.mInputs) {
            if (item == null) continue;
            if (item.getItem() instanceof ItemIntegratedCircuit && item.stackSize == 0) {
                if (match.circuit != null && !sameCircuit(match.circuit, item)) return null;
                match.circuit = item.copy();
            } else if (item.stackSize == 0 && OverloadedMachineMolds.indexOf(item) >= 0) {
                if (match.mold != null && !sameCircuit(match.mold, item)) return null;
                match.mold = item.copy();
            } else {
                if (item.stackSize <= 0) return null;
                add(match.inputs, new Ingredient(item, false), item.stackSize);
            }
        }
        for (FluidStack fluid : recipe.mFluidInputs) {
            if (fluid == null) continue;
            if (fluid.amount <= 0) return null;
            add(match.inputs, new Ingredient(fluid, false), fluid.amount);
        }
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack item = recipe.mOutputs[i];
            if (item == null || item.stackSize <= 0) continue;
            if (recipe.getOutputChance(i) != 10000) return null;
            add(match.outputs, new Ingredient(item, true), item.stackSize);
        }
        for (FluidStack fluid : recipe.mFluidOutputs) {
            if (fluid != null && fluid.amount > 0) add(match.outputs, new Ingredient(fluid, true), fluid.amount);
        }
        return match;
    }

    static boolean addAE(Map<Ingredient, Long> quantities, IAEStack<?> stack, boolean output) {
        if (stack.getStackSize() <= 0) return false;
        if (stack instanceof IAEItemStack item) {
            ItemStack value = item.getItemStack();
            if (!output && value.getItem() instanceof ItemIntegratedCircuit) return false;
            return add(quantities, new Ingredient(value, output), stack.getStackSize());
        }
        if (stack instanceof IAEFluidStack fluid) {
            return add(quantities, new Ingredient(fluid.getFluidStack(), output), stack.getStackSize());
        }
        return false;
    }

    static boolean add(Map<Ingredient, Long> quantities, Ingredient ingredient, long amount) {
        long previous = quantities.getOrDefault(ingredient, 0L);
        if (amount <= 0 || previous > Long.MAX_VALUE - amount) return false;
        quantities.put(ingredient, previous + amount);
        return true;
    }

    static boolean sameCircuit(ItemStack a, ItemStack b) {
        return a == null ? b == null : b != null && sameItem(a, b);
    }

    /**
     * Compares recipe contents for both GT lookup validation and continuous refilling. Object identity, registration
     * order, duration and EU/t are not recipe identity here; GT's lookup still enforces the machine's voltage limit.
     * Unsupported catalysts, special-slot recipes and probabilistic outputs remain excluded by {@link #describe}.
     *
     * @param left  previously selected or delivered recipe; null means there is no known match
     * @param right recipe being checked against it
     * @return true only for identical outputs, circuits and molds with mutually accepted input definitions
     */
    static boolean sameRecipe(Match left, Match right) {
        return left != null && right != null
            && CircuitPatternQuantities.sameRecipe(
                left.outputs,
                right.outputs,
                sameCircuit(left.circuit, right.circuit) && sameCircuit(left.mold, right.mold))
            && matchingInputs(left, right.inputs) == 1
            && matchingInputs(right, left.inputs) == 1;
    }

    /** Uses copied resource stacks so GT can test alternative ingredients without changing inventories. */
    static long matchingInputs(Match match, Map<Ingredient, Long> inputs) {
        ItemStack[] items = itemInputs(inputs);
        FluidStack[] fluids = fluidInputs(inputs);
        return items == null || fluids == null ? 0
            : CircuitRecipeInputs
                .batches(match.recipe, match.circuit, OverloadedMachineMolds.append(items, match.mold), fluids);
    }

    private static ItemStack[] itemInputs(Map<Ingredient, Long> inputs) {
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<Ingredient, Long> entry : inputs.entrySet()) {
            Ingredient key = entry.getKey();
            if (!(key.type instanceof Item item)) continue;
            long count = entry.getValue();
            if (count <= 0 || count > Integer.MAX_VALUE) return null;
            ItemStack stack = new ItemStack(item, (int) count, key.damage);
            if (key.tag != null) stack.setTagCompound((NBTTagCompound) key.tag.copy());
            result.add(stack);
        }
        return result.toArray(new ItemStack[0]);
    }

    private static FluidStack[] fluidInputs(Map<Ingredient, Long> inputs) {
        List<FluidStack> result = new ArrayList<>();
        for (Map.Entry<Ingredient, Long> entry : inputs.entrySet()) {
            Ingredient key = entry.getKey();
            if (!(key.type instanceof Fluid fluid)) continue;
            long amount = entry.getValue();
            if (amount <= 0 || amount > Integer.MAX_VALUE) return null;
            result.add(new FluidStack(fluid, (int) amount, key.tag == null ? null : (NBTTagCompound) key.tag.copy()));
        }
        return result.toArray(new FluidStack[0]);
    }

    static boolean sameItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
            && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** A recipe's consumed quantities and optional non-consumed circuit and mold configuration. */
    static final class Match {

        final GTRecipe recipe;
        final Map<Ingredient, Long> inputs = new HashMap<>();
        final Map<Ingredient, Long> outputs = new HashMap<>();
        ItemStack circuit;
        ItemStack mold;

        Match(GTRecipe recipe) {
            this.recipe = recipe;
        }
    }

    /** Quantity-independent, NBT-sensitive identity with separate namespaces for inputs and outputs. */
    static final class Ingredient {

        final Object type;
        final int damage;
        final NBTTagCompound tag;
        final boolean output;

        Ingredient(ItemStack item, boolean output) {
            ItemStack unified = GTOreDictUnificator.get(item);
            this.type = unified.getItem();
            this.damage = unified.getItemDamage();
            this.tag = unified.hasTagCompound() ? (NBTTagCompound) unified.getTagCompound()
                .copy() : null;
            this.output = output;
        }

        Ingredient(FluidStack fluid, boolean output) {
            this.type = fluid.getFluid();
            this.damage = 0;
            this.tag = fluid.tag == null ? null : (NBTTagCompound) fluid.tag.copy();
            this.output = output;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Ingredient ingredient && type == ingredient.type
                && damage == ingredient.damage
                && output == ingredient.output
                && Objects.equals(tag, ingredient.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, damage, tag, output);
        }
    }

}
