package com.xyp.gtneverreset.utils.crafting;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.BatchPlan;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.CommitResult;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.MediumStrategy;
import com.xyp.gtneverreset.utils.crafting.CraftingBatchPlanner.SessionConsumption;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.me.diagnostics.CraftingDiagnosticSessionId;

public final class BatchDispatchState {

    private final ICraftingPatternDetails details;
    private final LongSupplier taskValue;
    private final LongConsumer updateTask;
    private final MediumStrategy mediumStrategy;
    private BatchPlan plan;
    private boolean rejected;
    private boolean committed;
    private SessionConsumption<CraftingDiagnosticSessionId> sessionConsumption;

    public BatchDispatchState(ICraftingPatternDetails details, LongSupplier taskValue, LongConsumer updateTask,
        MediumStrategy mediumStrategy) {
        this.details = details;
        this.taskValue = taskValue;
        this.updateTask = updateTask;
        this.mediumStrategy = mediumStrategy;
    }

    public boolean hasTask() {
        return taskValue != null;
    }

    public long taskValue() {
        return taskValue.getAsLong();
    }

    public ICraftingPatternDetails details() {
        return details;
    }

    public MediumStrategy mediumStrategy() {
        return mediumStrategy;
    }

    public BatchPlan plan() {
        return plan;
    }

    public void resetForPlan(BatchPlan plan) {
        this.plan = plan;
        rejected = false;
        committed = false;
        sessionConsumption = null;
    }

    public boolean isPlannedBatch() {
        return plan != null && plan.isBatched();
    }

    public void reject() {
        rejected = true;
    }

    public boolean isRejected() {
        return rejected;
    }

    public int complete(CommitResult result) {
        updateTask.accept(result.getTaskValueBeforeNativeDecrement());
        committed = true;
        return result.getRemainingOperationsBeforeNativeDecrement();
    }

    public boolean isCommitted() {
        return committed;
    }

    public void recordSessions(SessionConsumption<CraftingDiagnosticSessionId> sessions) {
        sessionConsumption = sessions;
    }

    public SessionConsumption<CraftingDiagnosticSessionId> sessions() {
        return sessionConsumption;
    }
}
