// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GTNL PR #654, commit a6ee80aea6eaf98e2478e847a96dfdc3a620847b.
package com.xyp.gtneverreset.utils.structure;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.ToDoubleFunction;

import net.minecraft.util.EnumChatFormatting;

import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.HeatRules;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TierRange;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TieredPart;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TooltipStats;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary.BonusStat;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary.TierBonus;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary.TierThreshold;

import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.util.tooltip.TooltipHelper;

public final class MultiblockPerformanceReader {

    private static final int MAX_PROBE_TIER = 64;
    private static final double BONUS_TOLERANCE = 1e-6;

    private MultiblockPerformanceReader() {}

    public static PerformanceSummary readPerformance(MTEEnhancedMultiBlockBase<?> machine) {
        return readPerformance(machine, null);
    }

    public static PerformanceSummary readPerformance(MTEEnhancedMultiBlockBase<?> machine, StructureSummary structure) {
        TooltipStats base = freshSource(machine).getTooltipStats();
        List<TierBonus> bonuses = new ArrayList<>();
        List<TierThreshold> thresholds = new ArrayList<>();
        if (base != null) {
            MultiblockTooltipSource declarations = freshSource(machine);
            EnumSet<TieredPart> parts = EnumSet.noneOf(TieredPart.class);
            parts.addAll(Objects.requireNonNull(declarations.getTooltipTieredParts(), "tooltip tiered parts"));
            for (TieredPart part : parts) {
                TierRange range = Objects.requireNonNull(declarations.getTooltipTierRange(part), "tooltip tier range");
                validateRange(part, range);
                if (part == TieredPart.HIGH_PRESSURE) {
                    if (range.first() <= 1 && range.last() >= 1) addSwitchBonuses(machine, base, bonuses, thresholds);
                    continue;
                }
                TooltipStats[] probes = probeTiers(machine, part, range);
                if (probes == null) continue;
                addTierBonuses(bonuses, part, probes);
                if (!base.perfectOverclock()) addPerfectOverclockThreshold(thresholds, part, range.first(), probes);
            }
        }
        HeatRules heat = freshSource(machine).getTooltipHeatRules();
        OptionalInt skips = Objects.requireNonNull(freshSource(machine).getTooltipTierSkips(), "tooltip tier skips");
        if (skips.isPresent() && skips.getAsInt() < 0) {
            throw new IllegalArgumentException("Tooltip tier skips must not be negative");
        }
        return new PerformanceSummary(base, bonuses, heat, thresholds, skips);
    }

    private static MultiblockTooltipSource freshSource(MTEEnhancedMultiBlockBase<?> machine) {
        MTEEnhancedMultiBlockBase<?> detached = StructureTooltipReader.detachedCopy(machine);
        if (!(detached instanceof MultiblockTooltipSource source)) {
            throw new IllegalArgumentException(
                "Missing automatic performance tooltip source: " + machine.getClass()
                    .getName());
        }
        return source;
    }

    private static void validateRange(TieredPart part, TierRange range) {
        if (range.first() < 0 || range.last() < range.first()
            || range.last() > MAX_PROBE_TIER
            || part == TieredPart.HIGH_PRESSURE && range.last() > 1) {
            throw new IllegalArgumentException("Invalid tooltip probe range for " + part + ": " + range);
        }
    }

    private static TooltipStats[] probeTiers(MTEEnhancedMultiBlockBase<?> machine, TieredPart part, TierRange range) {
        TooltipStats[] probes = new TooltipStats[range.last() - range.first() + 1];
        for (int index = 0; index < probes.length; index++) {
            probes[index] = freshSource(machine).getTooltipStatsAtTier(part, range.first() + index);
            if (probes[index] == null) return null;
        }
        return probes;
    }

    private static void addTierBonuses(List<TierBonus> bonuses, TieredPart part, TooltipStats[] probes) {
        if (probes.length < 2) return;
        addBonus(bonuses, part, BonusStat.PARALLEL, values(probes, TooltipStats::parallels), true);
        double[] speeds = values(probes, TooltipStats::speed);
        Double speedStep = steadyStep(speeds, false);
        if (speedStep != null) {
            bonuses.add(new TierBonus(part, BonusStat.SPEED, false, speedStep, true));
        } else {
            double[] durations = values(probes, stats -> 1 / stats.speed());
            addBonus(bonuses, part, BonusStat.DURATION, durations, false);
        }
        addBonus(bonuses, part, BonusStat.ENERGY, values(probes, TooltipStats::energyUsage), false);
    }

    private static void addSwitchBonuses(MTEEnhancedMultiBlockBase<?> machine, TooltipStats base,
        List<TierBonus> bonuses, List<TierThreshold> thresholds) {
        TooltipStats switched = freshSource(machine).getTooltipStatsAtTier(TieredPart.HIGH_PRESSURE, 1);
        if (switched == null) return;
        addSwitchBonus(bonuses, BonusStat.PARALLEL, base.parallels(), switched.parallels());
        addSwitchBonus(bonuses, BonusStat.SPEED, base.speed(), switched.speed());
        addSwitchBonus(bonuses, BonusStat.ENERGY, base.energyUsage(), switched.energyUsage());
        if (!base.perfectOverclock() && switched.perfectOverclock()) {
            thresholds.add(new TierThreshold(TieredPart.HIGH_PRESSURE, 1, null));
        }
    }

    private static void addSwitchBonus(List<TierBonus> bonuses, BonusStat stat, double base, double switched) {
        if (!positiveFinite(base) || !positiveFinite(switched)) return;
        double change = switched - base;
        if (!isNearly(change, 0)) bonuses.add(new TierBonus(TieredPart.HIGH_PRESSURE, stat, false, change, false));
    }

    private static double[] values(TooltipStats[] probes, ToDoubleFunction<TooltipStats> stat) {
        double[] values = new double[probes.length];
        for (int index = 0; index < probes.length; index++) values[index] = stat.applyAsDouble(probes[index]);
        return values;
    }

    private static void addBonus(List<TierBonus> bonuses, TieredPart part, BonusStat stat, double[] values,
        boolean whole) {
        Double step = steadyStep(values, whole);
        if (step != null) {
            bonuses.add(new TierBonus(part, stat, false, step, true));
            return;
        }
        Double factor = steadyFactor(values);
        if (factor != null) bonuses.add(new TierBonus(part, stat, true, factor, true));
    }

    private static Double steadyStep(double[] values, boolean whole) {
        if (!positiveFinite(values)) return null;
        double step = values[1] - values[0];
        if (!Double.isFinite(step) || isNearly(step, 0) || whole && !isNearly(step, Math.rint(step))) return null;
        for (int index = 2; index < values.length; index++) {
            if (!isNearly(values[index] - values[index - 1], step)) return null;
        }
        return step;
    }

    private static Double steadyFactor(double[] values) {
        if (!positiveFinite(values)) return null;
        double factor = values[1] / values[0];
        if (!positiveFinite(factor) || isNearly(factor, 1)) return null;
        for (int index = 2; index < values.length; index++) {
            if (!isNearly(values[index] / values[index - 1], factor)) return null;
        }
        return factor;
    }

    private static boolean positiveFinite(double[] values) {
        for (double value : values) if (!positiveFinite(value)) return false;
        return true;
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }

    private static boolean isNearly(double first, double second) {
        if (!Double.isFinite(first) || !Double.isFinite(second)) return false;
        return Math.abs(first - second) <= BONUS_TOLERANCE * Math.max(1, Math.max(Math.abs(first), Math.abs(second)));
    }

    private static void addPerfectOverclockThreshold(List<TierThreshold> thresholds, TieredPart part, int firstTier,
        TooltipStats[] probes) {
        int index = probes.length - 1;
        if (!probes[index].perfectOverclock()) return;
        while (index >= 0 && probes[index].perfectOverclock()) index--;
        int tier = firstTier + index + 1;
        thresholds.add(new TierThreshold(part, tier, tierName(part, tier)));
    }

    private static String tierName(TieredPart part, int tier) {
        return switch (part) {
            case VOLTAGE, GLASS, PARALLEL_CONTROLLER -> tier <= VoltageIndex.MAX
                ? TooltipHelper.voltageTierText(tier, false)
                : null;
            case COIL -> tier <= HeatingCoilLevel.getMaxTier() ? HeatingCoilLevel.getDisplayNameFromHeat(
                (int) HeatingCoilLevel.getFromTier((byte) tier)
                    .getHeat(),
                true) + EnumChatFormatting.GRAY : null;
            case CASING, HIGH_PRESSURE -> null;
        };
    }
}
