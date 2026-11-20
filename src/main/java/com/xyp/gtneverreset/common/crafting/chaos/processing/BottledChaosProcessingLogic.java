package com.xyp.gtneverreset.common.crafting.chaos.processing;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosSourceParameters;
import com.xyp.gtneverreset.common.crafting.chaos.material.BottledChaosMaterialCache;
import com.xyp.gtneverreset.common.crafting.chaos.result.BottledChaosMissingCategoriesResult;
import com.xyp.gtneverreset.common.crafting.chaos.result.ChaosTextResult;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.PreparedPlan;
import com.xyp.gtneverreset.common.item.items.ChaosRouteItem;
import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.FluidStackLong;
import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.ItemStackLong;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.ChaosLongProcessingLogic;
import com.xyp.gtneverreset.config.ChaosConfig;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;

/** Folds only the cached recipe chains, against the input snapshot of one processing group. */
public final class BottledChaosProcessingLogic extends ChaosLongProcessingLogic {

    private ItemStackLong[] items;
    private FluidStackLong[] fluids;

    private boolean wireless;
    private BigInteger wirelessCost = BigInteger.ZERO;

    private final BottledChaosRecipeProcessor.PlanningCache planningCache = new BottledChaosRecipeProcessor.PlanningCache();
    private BottledChaosRecipeProcessor.CustomRoutes customRoutes;
    private boolean invalidCustomRoute;
    private long routeMask;

    public void setCustomRoutes(BottledChaosRecipeProcessor.CustomRoutes routes, boolean invalid) {
        customRoutes = routes;
        invalidCustomRoute = invalid;
    }

    private BigInteger pools = BigInteger.ZERO;
    private long maximumRecipeVoltage = Long.MAX_VALUE;
    private BottledChaosMaterialCache materials;
    private PreparedPlan preparedPlan;
    private CheckRecipeResult blockedRoute = CheckRecipeResultRegistry.NO_RECIPE;
    private final Map<Long, Double> durationLimits = new HashMap<>();
    private Function<BottledChaosRecipeProcessor.Plan, CheckRecipeResult> outputCheck;
    private BottledChaosSourceParameters machineParameters = BottledChaosSourceParameters.DEFAULT;

    public void setInputs(ItemStackLong[] items, FluidStackLong[] fluids) {
        this.items = items;
        this.fluids = fluids;
    }

    public void setWireless(boolean wireless) {
        this.wireless = wireless;
    }

    public BigInteger getWirelessCost() {
        return wirelessCost;
    }

    public void setMaterialCache(BottledChaosMaterialCache materials,
        Function<BottledChaosRecipeProcessor.Plan, CheckRecipeResult> outputCheck) {
        this.materials = materials;
        this.outputCheck = outputCheck;
    }

    public PreparedPlan getPreparedPlan() {
        return preparedPlan;
    }

    public CheckRecipeResult getBlockedRoute() {
        return blockedRoute;
    }

    public void setAuthorization(ItemStack credential, BigInteger poolMask) {
        routeMask = ChaosRouteItem.isBound(credential) ? ChaosRouteItem.executionMask(credential) : 0;
        pools = poolMask;
    }

    public BigInteger getRequiredPools() {
        return preparedPlan.requiredPools();
    }

    public long getBaseRecipeVoltage() {
        return preparedPlan.baseEUt();
    }

    public List<BottledChaosUpkeep.Demand> getUpkeep() {
        return preparedPlan == null ? Collections.emptyList() : preparedPlan.upkeep();
    }

    public void setMachineConfiguration(long maximumVoltage, BottledChaosSourceParameters parameters) {
        maximumRecipeVoltage = maximumVoltage;
        machineParameters = parameters;
    }

    @Override
    public @NotNull CheckRecipeResult process() {
        BottledChaosDiagnostics diagnostics = BottledChaosDiagnostics.begin();
        preparedPlan = null;
        wirelessCost = BigInteger.ZERO;
        blockedRoute = CheckRecipeResultRegistry.NO_RECIPE;
        durationLimits.clear();
        if (invalidCustomRoute) return ChaosTextResult.ofFailure("gtnr.chaos.error.recipe");
        if (routeMask == 0) {
            return ChaosTextResult.ofFailure("gtnr.machine.BottleInChaos.result.unbound");
        }
        if (customRoutes == null && !BottledChaosRecipeProcessor.isReady()) {
            // #tr gtnr.machine.BottleInChaos.result.recipe_cache_unavailable
            // # Bottle in Chaos processing recipes are unavailable. Check the startup log.
            // # zh_CN 混沌中瓶处理配方不可用. 请检查启动日志.
            return ChaosTextResult.ofFailure("gtnr.machine.BottleInChaos.result.recipe_cache_unavailable");
        }
        return wireless ? processWireless(diagnostics) : processWired(diagnostics);
    }

    private CheckRecipeResult processWired(BottledChaosDiagnostics diagnostics) {
        CheckRecipeResult failure = CheckRecipeResultRegistry.NO_RECIPE;
        var planner = BottledChaosRecipeProcessor.executionPlanner(
            customRoutes,
            routeMask,
            items,
            fluids,
            machineParameters,
            materials,
            planningCache,
            diagnostics);
        planner.setLimits(this::validateLimits, this::batchDurationLimit);
        // Output checks are reused only within this input/output snapshot, including fallback candidates.
        Map<PreparedPlan, CheckRecipeResult> outputChecks = new IdentityHashMap<>();
        Function<PreparedPlan, CheckRecipeResult> checkOutputs = candidate -> outputChecks
            .computeIfAbsent(candidate, plan -> prepareAndCheckOutputs(planner, plan));
        Predicate<PreparedPlan> fitsWork = candidate -> validatePlan(candidate).wasSuccessful()
            && candidate.durationTicks() <= batchDurationLimit(candidate);
        diagnostics.setupComplete();
        // 1. Try the last successful family first. Failures may select another entry/family without any consumption.
        for (int entry : BottledChaosRecipeProcessor.candidateEntries(customRoutes, routeMask, materials)) {
            for (int attempt = 0; attempt < 2; attempt++) {
                diagnostics.prepareStarted();
                PreparedPlan baseline = attempt == 0 ? planner.prepareEntry(entry) : planner.retryEntry(entry);
                diagnostics.prepared();
                if (baseline == null) {
                    if (failure == CheckRecipeResultRegistry.NO_RECIPE) failure = planner.preparationFailure();
                    break;
                }
                CheckRecipeResult preparationFailure = planner.preparationFailure();
                CheckRecipeResult result = validatePlan(baseline);
                if (!result.wasSuccessful()) {
                    failure = result;
                    break;
                }
                // 2. Extend selected goals from present stock once, then estimate/halve complete-line targets.
                PreparedPlan fallback = baseline;
                PreparedPlan chosen = planner.fitDownstream(baseline, batchDurationLimit(baseline), fitsWork);
                if (chosen.durationTicks()
                    <= batchDurationLimit(chosen) * ChaosConfig.BottledChaosMinimumOverclockSeconds
                        / ChaosConfig.BottledChaosChainedProcessingSeconds) {
                    chosen = planner.refineShortPlan(chosen, fitsWork);
                }
                diagnostics.fitted();
                // 3. Reserve outputs before changing even the caller's input copies; a smaller plan may still fit.
                PreparedPlan mainPlan = chosen;
                PreparedPlan recovered = planner.recoverAvailable(chosen, batchDurationLimit(chosen));
                if (fitsWork.test(recovered)) chosen = recovered;
                diagnostics.recovered();
                result = checkOutputs.apply(chosen);
                if (!result.wasSuccessful() && chosen != mainPlan) {
                    chosen = mainPlan;
                    result = checkOutputs.apply(chosen);
                }
                if (!result.wasSuccessful() && chosen != fallback) {
                    // Output blockage only shrinks the selected targets; do not repeat the growth estimate.
                    chosen = planner.reduceDownstream(
                        fallback,
                        chosen,
                        candidate -> fitsWork.test(candidate) && checkOutputs.apply(candidate)
                            .wasSuccessful());
                    result = checkOutputs.apply(chosen);
                }
                if (!result.wasSuccessful() && chosen != fallback) {
                    chosen = fallback;
                    result = checkOutputs.apply(chosen);
                }
                if (!result.wasSuccessful()) {
                    if (attempt == 0 && result == CheckRecipeResultRegistry.NO_RECIPE) continue;
                    if (result != CheckRecipeResultRegistry.NO_RECIPE) failure = result;
                    break;
                }
                diagnostics.outputsChecked();
                BottledChaosOverclock.Result clock = overclock(chosen);
                result = validateClock(clock);
                if (!result.wasSuccessful()) {
                    failure = result;
                    break;
                }
                calculatedEut = clock.eut();
                duration = Math.max(1, clock.duration());
                acceptPlan(chosen, preparationFailure);
                diagnostics.finish(entry, duration, calculatedEut);
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        }
        return failure;
    }

    /** Wireless rounds fill from materials, then bill the finished chain once at a fixed execution time. */
    private CheckRecipeResult processWireless(BottledChaosDiagnostics diagnostics) {
        CheckRecipeResult failure = CheckRecipeResultRegistry.NO_RECIPE;
        var planner = BottledChaosRecipeProcessor.executionPlanner(
            customRoutes,
            routeMask,
            items,
            fluids,
            machineParameters,
            materials,
            planningCache,
            diagnostics);
        planner.setRouteFilter((voltage, requiredPools) -> validatePools(requiredPools));
        diagnostics.setupComplete();
        for (int entry : BottledChaosRecipeProcessor.candidateEntries(customRoutes, routeMask, materials)) {
            for (int attempt = 0; attempt < 2; attempt++) {
                diagnostics.prepareStarted();
                PreparedPlan baseline = attempt == 0 ? planner.prepareEntry(entry) : planner.retryEntry(entry);
                diagnostics.prepared();
                if (baseline == null) {
                    if (failure == CheckRecipeResultRegistry.NO_RECIPE) failure = planner.preparationFailure();
                    break;
                }
                CheckRecipeResult preparationFailure = planner.preparationFailure();
                PreparedPlan chosen = planner.maximizeWireless(baseline);
                diagnostics.fitted();
                PreparedPlan mainPlan = chosen;
                chosen = planner.recoverAvailable(chosen, Double.POSITIVE_INFINITY);
                diagnostics.recovered();
                CheckRecipeResult result = prepareAndCheckOutputs(planner, chosen);
                if (!result.wasSuccessful() && chosen != mainPlan) {
                    chosen = mainPlan;
                    result = prepareAndCheckOutputs(planner, chosen);
                }
                if (!result.wasSuccessful() && chosen != baseline) {
                    PreparedPlan reduced = planner.reduceWireless(
                        chosen,
                        candidate -> prepareAndCheckOutputs(planner, candidate).wasSuccessful());
                    chosen = reduced == null ? baseline : reduced;
                    result = reduced == null ? prepareAndCheckOutputs(planner, baseline)
                        : CheckRecipeResultRegistry.SUCCESSFUL;
                }
                if (!result.wasSuccessful()) {
                    if (attempt == 0 && result == CheckRecipeResultRegistry.NO_RECIPE) continue;
                    if (result != CheckRecipeResultRegistry.NO_RECIPE) failure = result;
                    break;
                }
                diagnostics.outputsChecked();
                long voltage = machineParameters.normalizationVoltage(chosen.baseEUt());
                double power = voltage * euModifier;
                // Restore the source-folding voltage before applying the final host overclock curve.
                double work = chosen.durationTicks() * ((double) chosen.baseEUt() / voltage) * speedBoost;
                // BigInteger billing has no host EU/t or long power ceiling; reject only invalid arithmetic.
                if (!Double.isFinite(power) || power <= 0) return CheckRecipeResultRegistry.POWER_OVERFLOW;
                if (!Double.isFinite(work) || work <= 0) return CheckRecipeResultRegistry.DURATION_OVERFLOW;
                wirelessCost = BottledChaosOverclock
                    .wirelessCost(power, work, overClockTimeReduction, overClockPowerIncrease);
                calculatedEut = 0;
                duration = BottledChaosOverclock.minimumOverclockTicks();
                acceptPlan(chosen, preparationFailure);
                diagnostics.finish(entry, duration, calculatedEut);
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        }
        return failure;
    }

    private void acceptPlan(PreparedPlan chosen, CheckRecipeResult preparationFailure) {
        chosen.consumeInputCopies(items, fluids);
        preparedPlan = chosen;
        blockedRoute = preparationFailure;
        BottledChaosRecipeProcessor.Plan outputs = chosen.outputPlan();
        calculatedParallels = (int) Math.min(Integer.MAX_VALUE, outputs.executions());
        setLongOutputs(outputs.itemOutputs(), outputs.fluidOutputs());
    }

    private CheckRecipeResult prepareAndCheckOutputs(BottledChaosRecipeProcessor.ExecutionPlanner planner,
        PreparedPlan plan) {
        if (planner.completePlan(plan) == null) return CheckRecipeResultRegistry.NO_RECIPE;
        BottledChaosRecipeProcessor.Plan outputs;
        try {
            plan.prepareOutputs();
            outputs = plan.outputPlan();
        } catch (ArithmeticException overflow) {
            // #tr gtnr.machine.BottleInChaos.result.output_overflow
            // # Output quantity exceeds the limit.
            // # zh_CN 输出数量超过上限.
            return ChaosTextResult.ofFailure("gtnr.machine.BottleInChaos.result.output_overflow");
        }
        return outputCheck.apply(outputs);
    }

    private CheckRecipeResult validatePlan(PreparedPlan plan) {
        long baseRecipeVoltage = plan.baseEUt();
        BigInteger requiredPools = plan.requiredPools();
        CheckRecipeResult limits = validateLimits(baseRecipeVoltage, requiredPools);
        if (!limits.wasSuccessful()) return limits;
        double power = baseRecipeVoltage * euModifier;
        if (!Double.isFinite(power) || power <= 0 || power >= Long.MAX_VALUE)
            return CheckRecipeResultRegistry.POWER_OVERFLOW;
        double time = plan.durationTicks() * speedBoost;
        if (!Double.isFinite(time) || time <= 0) return CheckRecipeResultRegistry.DURATION_OVERFLOW;
        if (Math.ceil(power) > availableVoltage)
            return CheckRecipeResultRegistry.insufficientPower((long) Math.ceil(power));
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private CheckRecipeResult validateLimits(long baseRecipeVoltage, BigInteger requiredPools) {
        if (baseRecipeVoltage > maximumRecipeVoltage) {
            // #tr gtnr.machine.BottleInChaos.result.recipe_voltage_limit
            // # Recipe voltage exceeds the structure upgrade limit.
            // # zh_CN 配方电压超过结构升级上限.
            return ChaosTextResult.ofFailure("gtnr.machine.BottleInChaos.result.recipe_voltage_limit");
        }
        return validatePools(requiredPools);
    }

    private CheckRecipeResult validatePools(BigInteger requiredPools) {
        if (!pools.and(requiredPools)
            .equals(requiredPools)) return missingPools(requiredPools, pools, false);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private double batchDurationLimit(PreparedPlan plan) {
        return batchDurationLimit(plan.baseEUt());
    }

    private double batchDurationLimit(long voltage) {
        return durationLimits.computeIfAbsent(
            voltage,
            key -> Math.min(
                Double.MAX_VALUE,
                ChaosConfig.BottledChaosChainedProcessingSeconds * 20.0
                    * BottledChaosOverclock.maximumSpeed(
                        key * euModifier,
                        availableVoltage,
                        overClockTimeReduction,
                        overClockPowerIncrease)
                    / speedBoost));
    }

    private BottledChaosOverclock.Result overclock(PreparedPlan plan) {
        return BottledChaosOverclock.calculate(
            plan.baseEUt() * euModifier,
            plan.durationTicks() * speedBoost,
            availableVoltage,
            overClockTimeReduction,
            overClockPowerIncrease);
    }

    private CheckRecipeResult validateClock(BottledChaosOverclock.Result clock) {
        if (clock.eut() == Long.MAX_VALUE) return CheckRecipeResultRegistry.POWER_OVERFLOW;
        if (clock.eut() > availableVoltage) return CheckRecipeResultRegistry.insufficientPower(clock.eut());
        if (clock.duration() == Integer.MAX_VALUE) return CheckRecipeResultRegistry.DURATION_OVERFLOW;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    public static CheckRecipeResult missingPools(BigInteger required, BigInteger available, boolean running) {
        return new BottledChaosMissingCategoriesResult(required.andNot(available), running);
    }
}
