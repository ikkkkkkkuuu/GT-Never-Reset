package com.xyp.gtneverreset.common.machine.multiMachineBase.processing;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.xyp.gtneverreset.common.crafting.chaos.processing.ChaosOverclockType;
import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.FluidStackLong;
import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.ItemStackLong;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;

public class ChaosLongProcessingLogic extends ProcessingLogic {

    private ChaosParallelHelper longOutputHelper;
    private List<ItemStackLong> longItemOutputs;
    private List<FluidStackLong> longFluidOutputs;

    /**
     * Override to tweak parallel logic if needed.
     */
    @Nonnull
    @Override
    protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
        ChaosParallelHelper helper = new ChaosParallelHelper();
        helper.setRecipe(recipe)
            .setItemInputs(inputItems)
            .setFluidInputs(inputFluids)
            .setAvailableEUt(availableVoltage * availableAmperage)
            .setMachine(machine, protectItems, protectFluids)
            .setRecipeLocked(recipeLockableMachine, isRecipeLocked)
            .setMaxParallel(maxParallel)
            .setEUtModifier(euModifier)
            .enableBatchMode(batchSize)
            .setConsumption(true)
            .setOutputCalculation(true);
        if (machine instanceof ChaosLongInputMachineBase<?>tstMachine && tstMachine.isMEOutputEnabled()) {
            helper.enableLongOutputCalculation();
        }
        return helper;
    }

    @Nonnull
    @Override
    protected CheckRecipeResult applyRecipe(@Nonnull GTRecipe recipe, @Nonnull ParallelHelper helper,
        @Nonnull OverclockCalculator calculator, @Nonnull CheckRecipeResult result) {
        longOutputHelper = null;
        longItemOutputs = null;
        longFluidOutputs = null;
        CheckRecipeResult appliedResult = super.applyRecipe(recipe, helper, calculator, result);
        if (appliedResult.wasSuccessful() && helper instanceof ChaosParallelHelper tstHelper
            && tstHelper.isCalculatingOutputsAsLong()) {
            longOutputHelper = tstHelper;
        }
        return appliedResult;
    }

    public boolean hasLongOutputs() {
        return longOutputHelper != null || longItemOutputs != null || longFluidOutputs != null;
    }

    public List<ItemStackLong> getLongItemOutputs() {
        if (longItemOutputs != null) return longItemOutputs;
        return longOutputHelper == null ? Collections.emptyList() : longOutputHelper.getLongItemOutputs();
    }

    public List<FluidStackLong> getLongFluidOutputs() {
        if (longFluidOutputs != null) return longFluidOutputs;
        return longOutputHelper == null ? Collections.emptyList() : longOutputHelper.getLongFluidOutputs();
    }

    public void setLongOutputs(List<ItemStackLong> itemOutputs, List<FluidStackLong> fluidOutputs) {
        longOutputHelper = null;
        longItemOutputs = itemOutputs;
        longFluidOutputs = fluidOutputs;
    }

    public void clearLongOutputs() {
        longOutputHelper = null;
        longItemOutputs = null;
        longFluidOutputs = null;
    }

    public ChaosLongProcessingLogic setOverclockType(ChaosOverclockType t) {
        setOverclock(t.timeReduction, t.powerIncrease);
        return this;
    }
}
