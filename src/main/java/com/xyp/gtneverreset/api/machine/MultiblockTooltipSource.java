// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GTNL PR #654, commit 1cd98b4f61e4c77f65263171d3f03dda9c813382.
// Structure variants updated from commit a6ee80aea6eaf98e2478e847a96dfdc3a620847b.
package com.xyp.gtneverreset.api.machine;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.structure.IStructureChannels;
import gregtech.api.structure.error.StructureError;

public interface MultiblockTooltipSource {

    int NO_TIER_SKIP = 0;
    int UNLIMITED_TIER_SKIPS = Integer.MAX_VALUE;
    // GT5 OverclockCalculator heat steps, in kelvin.
    int HEAT_DISCOUNT_THRESHOLD = 900;
    int HEAT_OVERCLOCK_THRESHOLD = 1800;

    /** Speed is a multiplier; NaN omits a stat that does not describe this machine's process. */
    record TooltipStats(int parallels, double speed, double energyUsage, boolean perfectOverclock) {}

    default TooltipStats getTooltipStats() {
        return null;
    }

    /** Reads one tiered part on a detached controller, or returns null when that part is unsupported. */
    default TooltipStats getTooltipStatsAtTier(TieredPart part, int tier) {
        return null;
    }

    /** Parts whose formulas should be sampled; machines may omit parts they do not support. */
    default List<TieredPart> getTooltipTieredParts() {
        return List.of(TieredPart.values());
    }

    /** Valid formula inputs, inclusive, rather than a request to construct the world at these tiers. */
    default TierRange getTooltipTierRange(TieredPart part) {
        int first = switch (part) {
            case VOLTAGE -> 1;
            case GLASS -> 3;
            case PARALLEL_CONTROLLER -> 2;
            default -> 0;
        };
        return new TierRange(first, part.maxTier);
    }

    /** Real calculator heat rules, or null when the process has none. */
    default HeatRules getTooltipHeatRules() {
        return null;
    }

    /** The permitted recipe tier difference; empty keeps GT's ordinary one-tier rule implicit. */
    default OptionalInt getTooltipTierSkips() {
        return OptionalInt.empty();
    }

    default String getTooltipMachineType() {
        return null;
    }

    default Map<String, Object> getTooltipVariables() {
        return Map.of();
    }

    default List<String> getTooltipStructureNotes() {
        return List.of();
    }

    default List<String> getTooltipFeatureLines() {
        return List.of();
    }

    /** Shared with the real structure check; only pure count requirements belong here. */
    default void collectTooltipStructureRequirements(List<StructureError> errors) {}

    /**
     * Collects pure count requirements on the detached controller for one sampled structure. The projector stack
     * size is the first actual stack size, from 1 through 64, that constructs this structure; it is not a tier or
     * layer index when construction clamps the stack size. Do not access the world or run the full structure check.
     */
    default void collectTooltipStructureRequirements(List<StructureError> errors, int projectorStackSize) {
        collectTooltipStructureRequirements(errors);
    }

    /** Number of material variants, or zero when materials are not split into tiers. */
    default int getStructureTooltipMaterialTiers() {
        return 0;
    }

    /** Localized material-tier header, with tiers starting at one; null selects the default header. */
    default String getStructureTooltipMaterialTierHeader(int tier) {
        return null;
    }

    /** Projector sub-channel selecting the material tier, or null when none is used. */
    default IStructureChannels getStructureTooltipMaterialTierChannel() {
        return null;
    }

    /**
     * Describes a recorded casing minimum shared by several materials or varying with the structure size. Returning
     * localized lines keeps that minimum separate from individual material ranges; an empty list assigns it to the
     * replaceable material with the most positions.
     */
    default List<String> getStructureTooltipCasingRequirements(int minimum) {
        return List.of();
    }

    enum TieredPart {

        // #tr gtnr.tooltip.performance.part.voltage
        // # Voltage
        // # zh_CN 电压
        VOLTAGE("gtnr.tooltip.performance.part.voltage", VoltageIndex.MAX),
        // #tr gtnr.tooltip.performance.part.coil
        // # Heating coil
        // # zh_CN 加热线圈
        COIL("gtnr.tooltip.performance.part.coil", HeatingCoilLevel.getMaxTier()),
        // #tr gtnr.tooltip.performance.part.glass
        // # Glass
        // # zh_CN 玻璃
        GLASS("gtnr.tooltip.performance.part.glass", VoltageIndex.UXV),
        // #tr gtnr.tooltip.performance.part.casing
        // # Casing
        // # zh_CN 外壳
        CASING("gtnr.tooltip.performance.part.casing", 12),
        // #tr gtnr.tooltip.performance.part.high_pressure
        // # High-pressure structure
        // # zh_CN 高压结构
        HIGH_PRESSURE("gtnr.tooltip.performance.part.high_pressure", 1),
        // #tr gtnr.tooltip.performance.part.parallel_controller
        // # Parallel controller
        // # zh_CN 并行控制仓
        PARALLEL_CONTROLLER("gtnr.tooltip.performance.part.parallel_controller", VoltageIndex.MAX);

        public final String langKey;
        public final int maxTier;

        TieredPart(String langKey, int maxTier) {
            this.langKey = langKey;
            this.maxTier = maxTier;
        }
    }

    record TierRange(int first, int last) {

        private static final int MAX_PROBE_TIER = 64;

        public TierRange {
            if (first < 0 || last < first || last > MAX_PROBE_TIER) {
                throw new IllegalArgumentException("Tooltip tier ranges must be within 0 through " + MAX_PROBE_TIER);
            }
        }
    }

    /** Rules read from the calculator without executing a recipe or consuming machine inputs. */
    record HeatRules(boolean overclock, boolean discount, double discountFactor, boolean usesRecipeHeat,
        int heatPerVoltageTier, int voltageHeatFrom, int freePerfectOverclocks) {

        public HeatRules {
            if (!Double.isFinite(discountFactor) || discountFactor <= 0) {
                throw new IllegalArgumentException("A heat discount multiplier must be finite and positive");
            }
            if (heatPerVoltageTier < 0 || voltageHeatFrom < 0 || voltageHeatFrom > VoltageIndex.MAX
                || freePerfectOverclocks < 0) {
                throw new IllegalArgumentException("Invalid voltage heat or free overclock rule");
            }
        }
    }
}
