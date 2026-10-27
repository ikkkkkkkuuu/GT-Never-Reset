// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtneverreset.mixins.late.appliedenergistics2.compact;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.InventoryCrafting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.BatchPlan;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.CommitResult;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.MediumStrategy;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.SessionAllocation;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.SessionConsumption;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.SessionSegment;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlannerImpl;
import com.xyp.gtneverreset.utils.machine.DireCraftingPatternDetails;
import com.xyp.gtneverreset.utils.machine.ECraftingCPUCluster;
import com.xyp.gtneverreset.utils.machine.LargeInventoryCrafting;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.data.IAEStack;
import appeng.crafting.MECraftingInventory;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.diagnostics.CraftingDiagnosticSessionId;

/** Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance. */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUCluster {

    @Unique
    private static final CraftingBatchPlanner GTNR$BATCH_PLANNER = new CraftingBatchPlannerImpl();

    @Shadow
    private int remainingOperations;

    @Shadow
    private MECraftingInventory inventory;

    /**
     * Starts one dispatch context after AE resolves every medium registered for the pattern.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cache/CraftingGridCache;getMediums(Lappeng/api/networking/crafting/ICraftingPatternDetails;)Ljava/util/List;"),
        require = 1)
    private List<ICraftingMedium> gtnr$beginDispatch(CraftingGridCache cache, ICraftingPatternDetails details,
        Operation<List<ICraftingMedium>> original,
        @Local(name = "craftingEntry") Map.Entry<ICraftingPatternDetails, ?> craftingEntry,
        @Share("gtnr$batchDispatch") LocalRef<BatchDispatchContext> contextRef) {
        List<ICraftingMedium> media = original.call(cache, details);
        List<Boolean> compatibility = new ArrayList<>(media.size());
        for (ICraftingMedium medium : media) {
            compatibility.add(gtnr$isCompatiblePair(details, medium));
        }

        Object progress = craftingEntry.getValue();
        if (!(progress instanceof AccessorTaskProgress taskProgress)) {
            GTNeverReset.LOG.error("AE crafting task progress is missing the GTNL accessor for pattern {}", details);
            contextRef.set(new BatchDispatchContext(details, null, MediumStrategy.Native));
            return media;
        }

        contextRef.set(
            new BatchDispatchContext(details, taskProgress, GTNR$BATCH_PLANNER.resolveMediumStrategy(compatibility)));
        return media;
    }

    /**
     * Plans from AE's expanded one-craft inputs and returns independent stacks scaled to the exact batch.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;getExpandedInputs(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lappeng/me/cache/CraftingGridCache;)Ljava/util/List;"),
        require = 1)
    private List<IAEStack<?>> gtnr$planExpandedInputs(CraftingCPUCluster cluster, ICraftingPatternDetails details,
        CraftingGridCache cache, Operation<List<IAEStack<?>>> original, IEnergyGrid eg,
        @Share("gtnr$batchDispatch") LocalRef<BatchDispatchContext> contextRef) {
        List<IAEStack<?>> expandedInputs = original.call(cluster, details, cache);
        BatchDispatchContext context = contextRef.get();
        if (expandedInputs == null || context == null || context.taskProgress == null) return expandedInputs;

        boolean physicalHatch = !(details instanceof DireCraftingPatternDetails);
        long requestedCrafts = context.taskProgress.getValue();
        if (physicalHatch) {
            requestedCrafts = CraftingBatchPlannerImpl.limitPhysicalInputs(requestedCrafts, expandedInputs);
            if (!(cluster instanceof ECraftingCPUCluster)) {
                requestedCrafts = Math.min(requestedCrafts, this.remainingOperations);
            }
        }
        boolean freeDispatch = cluster instanceof ECraftingCPUCluster || !physicalHatch;
        BatchPlan plan = cluster instanceof ECraftingCPUCluster quantum && quantum.hasVariantBudget(details)
            ? GTNR$BATCH_PLANNER.plan(
                requestedCrafts,
                context.mediumStrategy,
                expandedInputs,
                details.getCondensedAEOutputs(),
                input -> quantum.getBudgetedAvailable(details, input),
                requested -> requested)
            : GTNR$BATCH_PLANNER.plan(
                requestedCrafts,
                context.mediumStrategy,
                expandedInputs,
                details.getCondensedAEOutputs(),
                this.inventory,
                requested -> freeDispatch ? requested
                    : eg.extractAEPower(requested, Actionable.SIMULATE, PowerMultiplier.CONFIG));
        context.resetForPlan(plan);
        if (!plan.isBatched()) return expandedInputs;

        try {
            return GTNR$BATCH_PLANNER.scaleInputs(expandedInputs, plan.getCrafts());
        } catch (ArithmeticException exception) {
            GTNeverReset.LOG.error(
                "AE batch input scaling violated its checked plan for pattern {} and {} crafts",
                details,
                plan.getCrafts(),
                exception);
            context.reject();
            return expandedInputs;
        }
    }

    /**
     * Rejects a planned dispatch if the actual extraction no longer matches the expanded request.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/crafting/MECraftingInventory;extractItems(Lappeng/api/storage/data/IAEStack;Lappeng/api/config/Actionable;)Lappeng/api/storage/data/IAEStack;"),
        require = 1)
    private IAEStack<?> gtnr$validateActualExtraction(MECraftingInventory craftingInventory, IAEStack<?> request,
        Actionable mode, Operation<IAEStack<?>> original,
        @Share("gtnr$batchDispatch") LocalRef<BatchDispatchContext> contextRef) {
        IAEStack<?> extracted = original.call(craftingInventory, request, mode);
        BatchDispatchContext context = contextRef.get();
        if (context == null || !context.isPlannedBatch() || mode != Actionable.MODULATE) return extracted;
        if (extracted != null && extracted.getStackSize() == request.getStackSize()) return extracted;

        GTNeverReset.LOG.error(
            "AE batch extraction contract changed after planning for pattern {}: requested {}, extracted {}",
            context.details,
            request.getStackSize(),
            extracted == null ? 0 : extracted.getStackSize());
        context.reject();
        if (extracted != null) craftingInventory.injectItems(extracted, Actionable.MODULATE);
        return null;
    }

    /**
     * Treats pushPattern as the transaction boundary for task, operation-budget, and diagnostics state.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingMedium;pushPattern(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)Z"),
        require = 1)
    private boolean gtnr$commitAcceptedBatch(ICraftingMedium medium, ICraftingPatternDetails details,
        InventoryCrafting craftingInventory, Operation<Boolean> original,
        @Share("gtnr$batchDispatch") LocalRef<BatchDispatchContext> contextRef) {
        BatchDispatchContext context = contextRef.get();
        if (context == null || !context.isPlannedBatch()) {
            return original.call(medium, details, craftingInventory);
        }
        if (context.rejected) return false;
        if (!(craftingInventory instanceof LargeInventoryCrafting largeInventory)) {
            GTNeverReset.LOG
                .error("AE supplied a crafting inventory without long batch support for pattern {}", details);
            context.reject();
            return false;
        }

        CommitResult commit;
        try {
            commit = GTNR$BATCH_PLANNER
                .commit(context.plan, true, context.taskProgress.getValue(), this.remainingOperations);
        } catch (RuntimeException exception) {
            GTNeverReset.LOG.error("AE batch commit precondition failed for pattern {}", details, exception);
            context.reject();
            return false;
        }

        largeInventory.setAssemblerSize(context.plan.getCrafts());
        boolean accepted = original.call(medium, details, craftingInventory);
        if (!accepted) return false;

        context.taskProgress.setValue(commit.getTaskValueBeforeNativeDecrement());
        this.remainingOperations = commit.getRemainingOperationsBeforeNativeDecrement();
        context.committed = true;
        return true;
    }

    /**
     * Consumes diagnostics counts by session segments instead of invoking AE's one-craft session method repeatedly.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cluster/implementations/CraftingCPUCluster$TaskProgress;consumeCraftSession()Lappeng/me/diagnostics/CraftingDiagnosticSessionId;"),
        require = 1)
    private CraftingDiagnosticSessionId gtnr$consumeBatchSessions(CraftingCPUCluster.TaskProgress taskProgress,
        Operation<CraftingDiagnosticSessionId> original,
        @Share("gtnr$batchDispatch") LocalRef<BatchDispatchContext> contextRef) {
        BatchDispatchContext context = contextRef.get();
        if (context == null || !context.committed) return original.call(taskProgress);

        AccessorTaskProgress accessor = (AccessorTaskProgress) taskProgress;
        context.sessionConsumption = GTNR$BATCH_PLANNER
            .consumeSessions(gtnr$sessionSegments(accessor.getDiagnosticSessionCrafts()), context.plan.getCrafts());
        long consumedCrafts = context.sessionConsumption.getConsumedCrafts();
        if (consumedCrafts > 0 && consumedCrafts < context.plan.getCrafts()) {
            GTNeverReset.LOG.error(
                "AE batch diagnostics session counts ended early for pattern {}: planned {}, consumed {}",
                context.details,
                context.plan.getCrafts(),
                consumedCrafts);
        }
        return context.sessionConsumption.getFirstSessionId();
    }

    /**
     * Returns checked output copies so diagnostics, postChange, waitingFor, and status observe one batch quantity.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingPatternDetails;getCondensedAEOutputs()[Lappeng/api/storage/data/IAEStack;"),
        require = 1)
    private IAEStack<?>[] gtnr$scaleCommittedOutputs(ICraftingPatternDetails details, Operation<IAEStack<?>[]> original,
        @Share("gtnr$batchDispatch") LocalRef<BatchDispatchContext> contextRef) {
        IAEStack<?>[] outputs = original.call(details);
        BatchDispatchContext context = contextRef.get();
        if (context == null || !context.committed) return outputs;
        return GTNR$BATCH_PLANNER.scaleOutputs(outputs, context.plan.getCrafts());
    }

    /**
     * Splits a scaled expected output across the session segments consumed by this committed batch.
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cluster/implementations/CraftingCpuDiagnostics;recordExpectedOutput(Lappeng/api/storage/data/IAEStack;JLappeng/me/diagnostics/CraftingDiagnosticSessionId;)V"),
        require = 1)
    private void gtnr$recordBatchExpectedOutput(@Coerce Object diagnostics, IAEStack<?> scaledOutput,
        long outputObservedAtTick, CraftingDiagnosticSessionId nativeSession, Operation<Void> original,
        @Share("gtnr$batchDispatch") LocalRef<BatchDispatchContext> contextRef) {
        BatchDispatchContext context = contextRef.get();
        if (context == null || !context.committed || context.sessionConsumption == null) {
            original.call(diagnostics, scaledOutput, outputObservedAtTick, nativeSession);
            return;
        }

        long crafts = context.plan.getCrafts();
        if (scaledOutput.getStackSize() % crafts != 0) {
            GTNeverReset.LOG.error(
                "AE batch diagnostics received a non-divisible output for pattern {}: amount {}, crafts {}",
                context.details,
                scaledOutput.getStackSize(),
                crafts);
            return;
        }

        long baseOutput = scaledOutput.getStackSize() / crafts;
        for (SessionAllocation<CraftingDiagnosticSessionId> allocation : context.sessionConsumption.getAllocations()) {
            IAEStack<?> sessionOutput = scaledOutput.copy();
            sessionOutput.setStackSize(GTNR$BATCH_PLANNER.checkedMultiply(baseOutput, allocation.getCrafts()));
            original.call(diagnostics, sessionOutput, outputObservedAtTick, allocation.getSessionId());
        }
    }

    @Unique
    private static boolean gtnr$isCompatiblePair(ICraftingPatternDetails details, ICraftingMedium medium) {
        return (details instanceof DireCraftingPatternDetails && medium instanceof AssemblerMatrix)
            || (medium instanceof SuperMTEHatchCraftingInputME && !details.isCraftable()
                && !details.isInputOnly()
                && !details.canSubstitute());
    }

    @Unique
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Deque<SessionSegment<CraftingDiagnosticSessionId>> gtnr$sessionSegments(LinkedList<?> segments) {
        return (Deque) segments;
    }

    @Unique
    private static class BatchDispatchContext {

        private final ICraftingPatternDetails details;
        private final AccessorTaskProgress taskProgress;
        private final MediumStrategy mediumStrategy;
        private BatchPlan plan;
        private boolean rejected;
        private boolean committed;
        private SessionConsumption<CraftingDiagnosticSessionId> sessionConsumption;

        private BatchDispatchContext(ICraftingPatternDetails details, AccessorTaskProgress taskProgress,
            MediumStrategy mediumStrategy) {
            this.details = details;
            this.taskProgress = taskProgress;
            this.mediumStrategy = mediumStrategy;
        }

        private void resetForPlan(BatchPlan plan) {
            this.plan = plan;
            this.rejected = false;
            this.committed = false;
            this.sessionConsumption = null;
        }

        private boolean isPlannedBatch() {
            return this.plan != null && this.plan.isBatched();
        }

        private void reject() {
            this.rejected = true;
        }
    }
}
