package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.crafting.chaos.ChaosChain;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/** Compiles processing patterns into exact, registered recipes without trusting encoded output declarations. */
public final class ChaosPatternCompiler {

    private ChaosPatternCompiler() {}

    public static ChaosChain compile(List<ItemStack> patterns, World world) {
        List<ChaosChain.Step> steps = new ArrayList<>();
        for (ItemStack pattern : patterns) {
            if (pattern == null) continue;
            if (!(pattern.getItem() instanceof ICraftingPatternItem item))
                // #tr gtnr.chaos.error.pattern
                // # Use valid AE processing patterns with exact recipe amounts
                // # zh_CN 请使用数量与真实配方一致的 AE 加工样板
                throw new IllegalArgumentException("gtnr.chaos.error.pattern");
            var details = item.getPatternForItem(pattern, world);
            if (details == null || details.isCraftable())
                throw new IllegalArgumentException("gtnr.chaos.error.pattern");
            PatternMaterials inputs = convert(details.getAEInputs());
            PatternMaterials outputs = convert(details.getAEOutputs());
            if (inputs.isEmpty() || outputs.isEmpty()) throw new IllegalArgumentException("gtnr.chaos.error.pattern");
            String explicit = PatternRecipeMap.name(pattern);
            List<RecipeMap<?>> maps = explicit == null ? ChaosChain.SUPPORTED_MAPS
                : ChaosChain.SUPPORTED_MAPS.stream()
                    .filter(map -> map.unlocalizedName.equals(explicit))
                    .toList();
            ChaosChain.Step match = null;
            for (RecipeMap<?> map : maps) for (GTRecipe recipe : map.getAllRecipes()) {
                if (!ChaosChain.safe(recipe) || !matches(recipe, inputs, outputs)) continue;
                // #tr gtnr.chaos.error.ambiguous
                // # Ambiguous recipe: import a pattern with a recipe-map identifier
                // # zh_CN 配方有多义匹配，请导入带配方池标记的样板
                if (match != null) {
                    if (match.map() == map && ChaosChain.equivalent(match.recipe(), recipe)) continue;
                    throw new IllegalArgumentException("gtnr.chaos.error.ambiguous");
                }
                match = new ChaosChain.Step(map, recipe, Math.max(1, pattern.stackSize));
            }
            // #tr gtnr.chaos.error.recipe
            // # Recipe unavailable, changed, or requires unsupported conditions
            // # zh_CN 配方不可用、已变化或含未支持的加工条件
            if (match == null) throw new IllegalArgumentException("gtnr.chaos.error.recipe");
            steps.add(match);
        }
        if (steps.isEmpty() || steps.size() > ChaosChain.MAX_STEPS)
            throw new IllegalArgumentException("gtnr.chaos.error.pattern");
        return new ChaosChain("AE", steps);
    }

    private static PatternMaterials convert(IAEStack<?>[] stacks) {
        PatternMaterials result = new PatternMaterials();
        if (stacks == null || stacks.length > 256) throw new IllegalArgumentException("gtnr.chaos.error.pattern");
        for (IAEStack<?> stack : stacks) {
            if (stack == null) continue;
            long amount = stack.getStackSize();
            if (amount <= 0 || amount > Integer.MAX_VALUE)
                throw new IllegalArgumentException("gtnr.chaos.error.pattern");
            if (stack instanceof IAEItemStack item)
                result.items.merge(GTUtility.ItemId.createNoCopy(item.getItemStack()), amount, Math::addExact);
            else if (stack instanceof IAEFluidStack fluid)
                result.fluids.merge(GTUtility.FluidId.create(fluid.getFluidStack()), amount, Math::addExact);
            else throw new IllegalArgumentException("gtnr.chaos.error.pattern");
        }
        return result;
    }

    private static boolean matches(GTRecipe recipe, PatternMaterials inputs, PatternMaterials outputs) {
        if (!outputs.matchesOutputs(recipe)) return false;
        for (int i = 0; i < recipe.mOutputs.length; i++) if (recipe.getOutputChance(i) != 10000) return false;
        for (int i = 0; i < recipe.mFluidOutputs.length; i++) if (recipe.getFluidOutputChance(i) != 10000) return false;
        for (int i = 0; i < recipe.mInputs.length; i++) if (recipe.getInputChance(i) != 10000) return false;
        for (int i = 0; i < recipe.mFluidInputs.length; i++) if (recipe.getFluidInputChance(i) != 10000) return false;
        ItemStack[] items = inputs.itemsForMatching();
        FluidStack[] fluids = inputs.fluidsForMatching();
        if (!recipe.isRecipeInputEqual(true, fluids, items)) return false;
        for (ItemStack leftover : items) if (leftover != null && leftover.stackSize > 0) {
            boolean catalyst = false;
            for (ItemStack input : recipe.mInputs) if (input != null && input.stackSize == 0
                && input.isItemEqual(leftover)
                && ItemStack.areItemStackTagsEqual(input, leftover)) catalyst = true;
            if (!catalyst) return false;
        }
        for (FluidStack leftover : fluids) if (leftover != null && leftover.amount > 0) return false;
        return true;
    }

    private static final class PatternMaterials {

        private final Map<GTUtility.ItemId, Long> items = new LinkedHashMap<>();
        private final Map<GTUtility.FluidId, Long> fluids = new LinkedHashMap<>();

        private boolean isEmpty() {
            return items.isEmpty() && fluids.isEmpty();
        }

        private ItemStack[] itemsForMatching() {
            List<ItemStack> stacks = new ArrayList<>(items.size());
            items.forEach((key, amount) -> {
                for (long left = amount; left > 0; left -= Integer.MAX_VALUE)
                    stacks.add(key.getItemStack((int) Math.min(left, Integer.MAX_VALUE)));
            });
            return stacks.toArray(new ItemStack[0]);
        }

        private FluidStack[] fluidsForMatching() {
            List<FluidStack> stacks = new ArrayList<>(fluids.size());
            fluids.forEach((key, amount) -> {
                for (long left = amount; left > 0; left -= Integer.MAX_VALUE)
                    stacks.add(key.getFluidStack((int) Math.min(left, Integer.MAX_VALUE)));
            });
            return stacks.toArray(new FluidStack[0]);
        }

        private boolean matchesOutputs(GTRecipe recipe) {
            // Most recipes in a pool make a different product. Allocate totals only for possible matches.
            for (ItemStack stack : recipe.mOutputs) {
                if (stack != null && stack.stackSize > 0 && !items.containsKey(GTUtility.ItemId.createNoCopy(stack)))
                    return false;
            }
            for (FluidStack stack : recipe.mFluidOutputs) {
                if (stack != null && stack.amount > 0 && !fluids.containsKey(GTUtility.FluidId.create(stack)))
                    return false;
            }
            PatternMaterials totals = new PatternMaterials();
            for (ItemStack stack : recipe.mOutputs) {
                if (stack != null && stack.stackSize > 0)
                    totals.items.merge(GTUtility.ItemId.createNoCopy(stack), (long) stack.stackSize, Math::addExact);
            }
            for (FluidStack stack : recipe.mFluidOutputs) {
                if (stack != null && stack.amount > 0)
                    totals.fluids.merge(GTUtility.FluidId.create(stack), (long) stack.amount, Math::addExact);
            }
            return items.equals(totals.items) && fluids.equals(totals.fluids);
        }
    }
}
