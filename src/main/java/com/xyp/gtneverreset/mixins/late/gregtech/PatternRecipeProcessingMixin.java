package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xyp.gtneverreset.api.me.PatternRecipeModePreflight;
import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.me.PatternRecipeRouting;

import gregtech.api.interfaces.tileentity.IRecipeLockable;
import gregtech.api.interfaces.tileentity.IVoidable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

@Mixin(value = ProcessingLogic.class, remap = false)
public abstract class PatternRecipeProcessingMixin {

    @Shadow
    protected IVoidable machine;
    @Shadow
    protected IDualInputInventoryWithPattern activeDualInv;
    @Shadow
    protected IRecipeLockable recipeLockableMachine;
    @Shadow
    protected boolean isRecipeLocked;
    @Shadow
    protected ItemStack specialSlotItem;
    @Shadow
    protected int maxParallel;
    @Shadow
    protected Supplier<Integer> maxParallelSupplier;
    @Shadow
    protected Supplier<Double> euModSupplier;
    @Shadow
    protected Supplier<Double> speedBoostSupplier;
    @Shadow
    protected int batchSize;
    @Shadow
    protected Supplier<RecipeMap<?>> recipeMapSupplier;
    @Shadow
    protected double euModifier;
    @Shadow
    protected double speedBoost;
    @Shadow
    protected long availableVoltage;
    @Shadow
    protected long availableAmperage;
    @Shadow
    protected int maxTierSkips;
    @Shadow
    protected boolean protectItems;
    @Shadow
    protected boolean protectFluids;
    @Shadow
    protected double overClockTimeReduction;
    @Shadow
    protected double overClockPowerIncrease;
    @Shadow
    protected boolean amperageOC;
    @Shadow
    protected boolean recipeCaching;
    @Unique
    private RecipeMap<?> gtnr$pendingMap;
    @Unique
    private Runnable gtnr$restoreConfiguration;
    @Unique
    private boolean gtnr$preparingMode;

    @WrapMethod(method = "tryCachePossibleRecipesFromPattern")
    private boolean gtnr$selectPatternMap(IDualInputInventoryWithPattern inventory, Operation<Boolean> original) {
        gtnr$resetRouting();
        if (!(inventory instanceof RecipeMapPatternInventory mapped) || mapped.getRecipeMapName() == null)
            return original.call(inventory);
        activeDualInv = null;
        RecipeMap<?> requested = PatternRecipeMap.resolve(mapped.getRecipeMapName());
        try (var selection = PatternRecipeRouting.select(machine, requested)) {
            if (selection == null) return false;
            if (machine instanceof PatternRecipeModePreflight preflight && !preflight.gtnr$preparePatternRecipeMode())
                return false;
            boolean accepted = false;
            try {
                if (selection.changedMode()) {
                    // Run native setup before inputs arrive: it can clear logic and prepare transactional consumption.
                    gtnr$restoreConfiguration = gtnr$captureConfiguration();
                    gtnr$preparingMode = true;
                    ((MultiblockProcessingAccessor) machine).gtnr$setupProcessingLogic((ProcessingLogic) (Object) this);
                }
                accepted = original.call(inventory);
                if (accepted) gtnr$pendingMap = requested;
                return accepted;
            } finally {
                gtnr$preparingMode = false;
                if (!accepted) gtnr$resetRouting();
            }
        }
    }

    @WrapMethod(method = "process")
    private CheckRecipeResult gtnr$processPatternMap(Operation<CheckRecipeResult> original) {
        RecipeMap<?> requested = gtnr$pendingMap;
        if (requested == null) return original.call();
        try (var selection = PatternRecipeRouting.select(machine, requested)) {
            if (selection == null) return CheckRecipeResultRegistry.NO_RECIPE;
            return original.call();
        } finally {
            gtnr$resetRouting();
            activeDualInv = null;
        }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void gtnr$clearPatternMap(CallbackInfoReturnable<ProcessingLogic> cir) {
        if (!gtnr$preparingMode) gtnr$resetRouting();
    }

    @Unique
    private void gtnr$resetRouting() {
        gtnr$pendingMap = null;
        if (gtnr$restoreConfiguration != null) {
            gtnr$restoreConfiguration.run();
            gtnr$restoreConfiguration = null;
        }
    }

    @Unique
    private Runnable gtnr$captureConfiguration() {
        IRecipeLockable savedRecipeLockableMachine = recipeLockableMachine;
        boolean savedIsRecipeLocked = isRecipeLocked;
        ItemStack savedSpecialSlotItem = specialSlotItem;
        int savedMaxParallel = maxParallel;
        Supplier<Integer> savedMaxParallelSupplier = maxParallelSupplier;
        Supplier<Double> savedEuModSupplier = euModSupplier;
        Supplier<Double> savedSpeedBoostSupplier = speedBoostSupplier;
        int savedBatchSize = batchSize;
        Supplier<RecipeMap<?>> savedRecipeMapSupplier = recipeMapSupplier;
        double savedEuModifier = euModifier;
        double savedSpeedBoost = speedBoost;
        long savedAvailableVoltage = availableVoltage;
        long savedAvailableAmperage = availableAmperage;
        int savedMaxTierSkips = maxTierSkips;
        boolean savedProtectItems = protectItems;
        boolean savedProtectFluids = protectFluids;
        double savedOverClockTimeReduction = overClockTimeReduction;
        double savedOverClockPowerIncrease = overClockPowerIncrease;
        boolean savedAmperageOC = amperageOC;
        boolean savedRecipeCaching = recipeCaching;
        return () -> {
            recipeLockableMachine = savedRecipeLockableMachine;
            isRecipeLocked = savedIsRecipeLocked;
            specialSlotItem = savedSpecialSlotItem;
            maxParallel = savedMaxParallel;
            maxParallelSupplier = savedMaxParallelSupplier;
            euModSupplier = savedEuModSupplier;
            speedBoostSupplier = savedSpeedBoostSupplier;
            batchSize = savedBatchSize;
            recipeMapSupplier = savedRecipeMapSupplier;
            euModifier = savedEuModifier;
            speedBoost = savedSpeedBoost;
            availableVoltage = savedAvailableVoltage;
            availableAmperage = savedAvailableAmperage;
            maxTierSkips = savedMaxTierSkips;
            protectItems = savedProtectItems;
            protectFluids = savedProtectFluids;
            overClockTimeReduction = savedOverClockTimeReduction;
            overClockPowerIncrease = savedOverClockPowerIncrease;
            amperageOC = savedAmperageOC;
            recipeCaching = savedRecipeCaching;
        };
    }
}
