package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.crafting.chaos.ChaosBuffer;
import com.xyp.gtneverreset.common.crafting.chaos.ChaosChain;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

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
            ChaosBuffer inputs = convert(details.getAEInputs());
            ChaosBuffer outputs = convert(details.getAEOutputs());
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

    private static ChaosBuffer convert(IAEStack<?>[] stacks) {
        ChaosBuffer result = new ChaosBuffer();
        if (stacks == null || stacks.length > 256) throw new IllegalArgumentException("gtnr.chaos.error.pattern");
        for (IAEStack<?> stack : stacks) {
            if (stack == null) continue;
            long amount = stack.getStackSize();
            if (amount <= 0 || amount > Integer.MAX_VALUE)
                throw new IllegalArgumentException("gtnr.chaos.error.pattern");
            if (stack instanceof IAEItemStack item) result.add(item.getItemStack(), amount);
            else if (stack instanceof IAEFluidStack fluid) result.add(fluid.getFluidStack(), amount);
            else throw new IllegalArgumentException("gtnr.chaos.error.pattern");
        }
        return result;
    }

    private static boolean matches(GTRecipe recipe, ChaosBuffer inputs, ChaosBuffer outputs) {
        ChaosBuffer expected = new ChaosBuffer();
        expected.add(recipe.mOutputs, recipe.mFluidOutputs);
        // Probabilistic recipes can be preset routes, but their nominal outputs do not authorize exact AE promises.
        for (int i = 0; i < recipe.mOutputs.length; i++) if (recipe.getOutputChance(i) != 10000) return false;
        for (int i = 0; i < recipe.mFluidOutputs.length; i++) if (recipe.getFluidOutputChance(i) != 10000) return false;
        for (int i = 0; i < recipe.mInputs.length; i++) if (recipe.getInputChance(i) != 10000) return false;
        for (int i = 0; i < recipe.mFluidInputs.length; i++) if (recipe.getFluidInputChance(i) != 10000) return false;
        if (!equalAmounts(expected, outputs)) return false;
        ChaosBuffer.View view = inputs.view();
        if (!recipe.isRecipeInputEqual(true, view.fluids, view.items)) return false;
        for (ItemStack leftover : view.items) if (leftover != null && leftover.stackSize > 0) {
            boolean catalyst = false;
            for (ItemStack input : recipe.mInputs) if (input != null && input.stackSize == 0
                && input.isItemEqual(leftover)
                && ItemStack.areItemStackTagsEqual(input, leftover)) catalyst = true;
            if (!catalyst) return false;
        }
        for (FluidStack leftover : view.fluids) if (leftover != null && leftover.amount > 0) return false;
        return true;
    }

    private static boolean equalAmounts(ChaosBuffer expected, ChaosBuffer actual) {
        var a = expected.view();
        var b = actual.view();
        if (a.items.length != b.items.length || a.fluids.length != b.fluids.length) return false;
        for (ItemStack item : a.items) {
            boolean found = false;
            for (ItemStack other : b.items) if (ItemStack.areItemStacksEqual(item, other)) found = true;
            if (!found) return false;
        }
        for (FluidStack fluid : a.fluids) {
            boolean found = false;
            for (FluidStack other : b.fluids)
                if (fluid.isFluidEqual(other) && fluid.amount == other.amount) found = true;
            if (!found) return false;
        }
        return true;
    }
}
