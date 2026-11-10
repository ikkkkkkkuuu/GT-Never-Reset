package com.xyp.gtneverreset.common.machine.multiblock;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public final class UniversalJointFactoryProcessingLogic extends RecipeThreadProcessingLogic {

    private final UniversalJointFactory controller;
    private ICraftingPatternDetails pattern;
    private RecipeMap<?> patternRecipeMap;

    public UniversalJointFactoryProcessingLogic(UniversalJointFactory controller) {
        this.controller = controller;
    }

    @Override
    public boolean tryCachePossibleRecipesFromPattern(IDualInputInventoryWithPattern inventory) {
        resetPatternInventory();
        pattern = null;
        patternRecipeMap = null;
        if (inventory instanceof RecipeMapPatternInventory mapped && mapped.getRecipeMapName() != null) {
            RecipeMap<?> requested = PatternRecipeMap.resolve(mapped.getRecipeMapName());
            if (requested == null || !UniversalJointFactory.RECIPE_MAPS.contains(requested)) return false;
            patternRecipeMap = requested;
        }
        pattern = inventory instanceof SuperMTEHatchCraftingInputME.PatternSlot<?>slot ? slot.getPatternDetails()
            : inventory instanceof MTEHatchCraftingInputME.PatternSlot<?>slot ? slot.getPatternDetails() : null;
        // Match current accumulated inputs, including virtual tools and fractional AE deliveries.
        // The native input-only cache cannot distinguish recipes with identical inputs in different maps.
        return true;
    }

    @Override
    @Nonnull
    protected CheckRecipeResult processRecipe() {
        try {
            return super.processRecipe();
        } finally {
            pattern = null;
            patternRecipeMap = null;
            resetPatternInventory();
            controller.setProcessingRecipeMap(null);
        }
    }

    @Override
    @Nonnull
    protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> selected) {
        Stream<GTRecipe> query = Stream.ofNullable(patternRecipeMap == null ? selected : patternRecipeMap)
            .flatMap(
                map -> map.findRecipeQuery()
                    .items(inputItems)
                    .fluids(inputFluids)
                    .specialSlot(specialSlotItem)
                    .findAll()
                    .peek(recipe -> controller.setProcessingRecipeMap(map)))
            .filter(this::matchesPatternOutputs);
        if (pattern == null) return query;
        // A pattern is an explicit ingredient list, so an output-only match must not run a recipe that ignores some
        // of those ingredients and produce the output while leaving them behind.
        return query.filter(this::consumesPatternInputs);
    }

    /**
     * A pattern lists the ingredients its craft is expected to consume. Every listed item and fluid must appear in
     * the recipe's own inputs; otherwise an output-identical recipe that uses only part of the pattern could run and
     * leave the remaining ingredients untouched. Matches are checked with GT's own oredict, alternative and
     * non-consumed-input rules, so catalysts and tools keep working.
     */
    private boolean consumesPatternInputs(GTRecipe recipe) {
        IAEStack<?>[] inputs = pattern.getAEInputs();
        if (inputs == null || inputs.length == 0) return true;
        GTRecipe.RecipeItemInput[] itemInputs = recipe.getCachedCombinedItemInputs();
        for (IAEStack<?> input : inputs) {
            if (input == null || input.getStackSize() <= 0) continue;
            if (input instanceof IAEItemStack item) {
                ItemStack stack = item.getItemStack();
                ItemData association = GTOreDictUnificator.getAssociation(stack);
                boolean matched = false;
                for (GTRecipe.RecipeItemInput candidate : itemInputs) {
                    if (candidate.matchesRecipe(association, stack)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            } else if (input instanceof IAEFluidStack fluid && !consumesFluid(recipe, fluid.getFluidStack())) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumesFluid(GTRecipe recipe, FluidStack fluid) {
        if (recipe.mFluidInputs == null || fluid == null) return false;
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            FluidStack[] alternatives = recipe.mAltFluidInputs != null && i < recipe.mAltFluidInputs.length
                && recipe.mAltFluidInputs[i] != null ? recipe.mAltFluidInputs[i]
                    : recipe.mFluidInputs[i] == null ? null : new FluidStack[] { recipe.mFluidInputs[i] };
            if (alternatives == null) continue;
            for (FluidStack candidate : alternatives) {
                if (candidate != null && candidate.isFluidEqual(fluid)) return true;
            }
        }
        return false;
    }

    @Override
    @Nonnull
    protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
        // Also guard the native single-recipe-lock path, which bypasses findRecipeMatches.
        if (isRecipeLocked && recipeLockableMachine != null && recipeLockableMachine.getSingleRecipeCheck() != null) {
            RecipeMap<?> requested = patternRecipeMap == null ? controller.getRecipeMap() : patternRecipeMap;
            if (requested != recipeLockableMachine.getSingleRecipeCheck()
                .getRecipeMap()) {
                return CheckRecipeResultRegistry.NO_RECIPE;
            }
            controller.setProcessingRecipeMap(
                recipeLockableMachine.getSingleRecipeCheck()
                    .getRecipeMap());
        }
        if (pattern == null) return super.validateRecipe(recipe);
        return matchesPatternOutputs(recipe) && consumesPatternInputs(recipe) ? super.validateRecipe(recipe)
            : CheckRecipeResultRegistry.NO_RECIPE;
    }

    private boolean matchesPatternOutputs(GTRecipe recipe) {
        if (pattern == null) return true;
        IAEStack<?>[] outputs = pattern.getAEOutputs();
        if (outputs == null || outputs.length == 0) return false;
        for (IAEStack<?> output : outputs) {
            if (output == null || output.getStackSize() <= 0) continue;
            boolean matched = false;
            if (output instanceof IAEItemStack item && recipe.mOutputs != null) {
                ItemStack expected = item.getItemStack();
                for (ItemStack actual : recipe.mOutputs) {
                    if (actual != null && actual.isItemEqual(expected)
                        && ItemStack.areItemStackTagsEqual(actual, expected)) {
                        matched = true;
                        break;
                    }
                }
            } else if (output instanceof IAEFluidStack fluid && recipe.mFluidOutputs != null) {
                for (FluidStack actual : recipe.mFluidOutputs) {
                    if (actual != null && actual.isFluidEqual(fluid.getFluidStack())) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) return false;
        }
        return true;
    }
}
