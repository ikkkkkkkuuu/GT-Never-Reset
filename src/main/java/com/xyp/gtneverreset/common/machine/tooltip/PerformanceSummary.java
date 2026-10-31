package com.xyp.gtneverreset.common.machine.tooltip;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.HeatRules;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TieredPart;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TooltipStats;

public record PerformanceSummary(TooltipStats stats, List<TierBonus> tierBonuses, HeatRules heat,
    List<TierThreshold> perfectOverclockFrom, OptionalInt tierSkips) {

    public PerformanceSummary {
        tierBonuses = List.copyOf(tierBonuses);
        perfectOverclockFrom = List.copyOf(perfectOverclockFrom);
        Objects.requireNonNull(tierSkips, "tierSkips");
    }

    public enum BonusStat {
        PARALLEL,
        SPEED,
        DURATION,
        ENERGY
    }

    public record TierBonus(TieredPart part, BonusStat stat, boolean multiplicative, double value, boolean perTier) {}

    public record TierThreshold(TieredPart part, int tier, String tierName) {}
}
