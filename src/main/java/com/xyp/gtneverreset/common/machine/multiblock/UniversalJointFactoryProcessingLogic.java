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
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
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
        return Stream.ofNullable(patternRecipeMap == null ? selected : patternRecipeMap)
            .flatMap(
                map -> map.findRecipeQuery()
                    .items(inputItems)
                    .fluids(inputFluids)
                    .specialSlot(specialSlotItem)
                    .findAll()
                    .peek(recipe -> controller.setProcessingRecipeMap(map)))
            .filter(this::matchesPatternOutputs);
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
        return matchesPatternOutputs(recipe) ? super.validateRecipe(recipe) : CheckRecipeResultRegistry.NO_RECIPE;
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
