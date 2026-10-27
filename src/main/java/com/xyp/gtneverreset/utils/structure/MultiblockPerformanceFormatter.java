// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GTNL PR #654, commit a6ee80aea6eaf98e2478e847a96dfdc3a620847b.
package com.xyp.gtneverreset.utils.structure;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.HeatRules;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TieredPart;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary.BonusStat;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary.TierBonus;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary.TierThreshold;

import gregtech.api.enums.GTValues;

final class MultiblockPerformanceFormatter {

    private MultiblockPerformanceFormatter() {}

    static List<String> bonuses(PerformanceSummary summary, BonusStat... shown) {
        Map<TieredPart, List<TierBonus>> byPart = new LinkedHashMap<>();
        for (TierBonus bonus : summary.tierBonuses()) {
            if (Arrays.asList(shown)
                .contains(bonus.stat())) {
                byPart.computeIfAbsent(bonus.part(), ignored -> new ArrayList<>())
                    .add(bonus);
            }
        }
        Map<Effects, List<String>> groups = new LinkedHashMap<>();
        for (var entry : byPart.entrySet()) {
            List<String> effects = entry.getValue()
                .stream()
                .map(MultiblockPerformanceFormatter::effect)
                .toList();
            boolean perTier = entry.getValue()
                .get(0)
                .perTier();
            groups.computeIfAbsent(new Effects(effects, perTier), ignored -> new ArrayList<>())
                .add(partName(entry.getKey()));
        }
        List<String> lines = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            String parts = String.join(" / ", entry.getValue());
            String effects = String.join(
                ", ",
                entry.getKey()
                    .lines());
            if (entry.getKey()
                .perTier()) {
                // #tr gtnr.tooltip.performance.per_tier
                // # Per %s tier: %s
                // # zh_CN 每%s等级：%s
                lines.add(StatCollector.translateToLocalFormatted("gtnr.tooltip.performance.per_tier", parts, effects));
            } else {
                // #tr gtnr.tooltip.performance.switch
                // # %s: %s
                // # zh_CN %s：%s
                lines.add(StatCollector.translateToLocalFormatted("gtnr.tooltip.performance.switch", parts, effects));
            }
        }
        return lines;
    }

    static List<String> rules(PerformanceSummary summary) {
        List<String> lines = new ArrayList<>();
        boolean perfect = summary.stats() != null && summary.stats()
            .perfectOverclock();
        if (summary.heat() != null) addHeat(lines, summary.heat(), perfect);
        if (summary.stats() == null) return lines;
        for (TierThreshold threshold : summary.perfectOverclockFrom()) {
            // #tr gtnr.tooltip.performance.perfect_from
            // # Perfect overclocking at %s or higher
            // # zh_CN 达到%s或更高时启用无损超频
            lines.add(
                StatCollector.translateToLocalFormatted("gtnr.tooltip.performance.perfect_from", threshold(threshold)));
        }
        if (summary.tierSkips()
            .isPresent())
            addTierSkips(
                lines,
                summary.tierSkips()
                    .getAsInt());
        return lines;
    }

    private static String effect(TierBonus bonus) {
        String change = bonus.multiplicative() ? "×" + number(bonus.value())
            : (bonus.value() > 0 ? "+" : "")
                + number(bonus.stat() == BonusStat.PARALLEL ? bonus.value() : bonus.value() * 100)
                + (bonus.stat() == BonusStat.PARALLEL ? "" : "%");
        String value = EnumChatFormatting.AQUA + change + EnumChatFormatting.GRAY;
        return switch (bonus.stat()) {
            case PARALLEL -> {
                // #tr gtnr.tooltip.performance.bonus.parallel
                // # Parallels %s
                // # zh_CN 并行数 %s
                yield StatCollector.translateToLocalFormatted("gtnr.tooltip.performance.bonus.parallel", value);
            }
            case SPEED -> {
                // #tr gtnr.tooltip.performance.bonus.speed
                // # Speed multiplier %s
                // # zh_CN 速度系数 %s
                yield StatCollector.translateToLocalFormatted("gtnr.tooltip.performance.bonus.speed", value);
            }
            case DURATION -> {
                // #tr gtnr.tooltip.performance.bonus.duration
                // # Recipe time multiplier %s
                // # zh_CN 配方时间系数 %s
                yield StatCollector.translateToLocalFormatted("gtnr.tooltip.performance.bonus.duration", value);
            }
            case ENERGY -> {
                // #tr gtnr.tooltip.performance.bonus.energy
                // # Energy multiplier %s
                // # zh_CN 耗电系数 %s
                yield StatCollector.translateToLocalFormatted("gtnr.tooltip.performance.bonus.energy", value);
            }
        };
    }

    private static void addHeat(List<String> lines, HeatRules heat, boolean perfect) {
        if (heat.heatPerVoltageTier() > 0) {
            String from = heat.voltageHeatFrom() >= 0 && heat.voltageHeatFrom() < GTValues.VN.length
                ? GTValues.VN[heat.voltageHeatFrom()]
                : Integer.toString(heat.voltageHeatFrom());
            // #tr gtnr.tooltip.performance.heat.voltage
            // # Each voltage tier above %s adds %sK of machine heat
            // # zh_CN 每超过%s一级电压，机器温度增加%sK
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.performance.heat.voltage",
                    colored(from),
                    colored(number(heat.heatPerVoltageTier()))));
        }
        if (!perfect && heat.freePerfectOverclocks() > 0) {
            // #tr gtnr.tooltip.performance.heat.free_oc
            // # Fixed machine heat provides %s free perfect overclocks
            // # zh_CN 固定机器温度可提供%s次免费无损超频
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.performance.heat.free_oc",
                    colored(number(heat.freePerfectOverclocks()))));
        }
        if (!heat.usesRecipeHeat()) {
            if (heat.discount()) {
                // #tr gtnr.tooltip.performance.heat.fixed_discount
                // # Fixed recipe heat: each %sK heat-discount step multiplies energy usage by %s
                // # zh_CN 使用固定配方温度：温度折扣步长为%sK，每级耗电系数乘以%s
                lines.add(
                    StatCollector.translateToLocalFormatted(
                        "gtnr.tooltip.performance.heat.fixed_discount",
                        colored(number(MultiblockTooltipSource.HEAT_DISCOUNT_THRESHOLD)),
                        colored(number(heat.discountFactor() * 100) + "%")));
            }
            return;
        }
        if (heat.discount()) {
            // #tr gtnr.tooltip.performance.heat.discount
            // # Every %sK above the recipe heat multiplies energy usage by %s
            // # zh_CN 每比配方要求高%sK，耗电系数乘以%s
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.performance.heat.discount",
                    colored(number(MultiblockTooltipSource.HEAT_DISCOUNT_THRESHOLD)),
                    colored(number(heat.discountFactor() * 100) + "%")));
        }
        if (!perfect && heat.overclock()) {
            // #tr gtnr.tooltip.performance.heat.overclock
            // # Every %sK above the recipe heat turns one overclock into a perfect overclock
            // # zh_CN 每比配方要求高%sK，可将一次超频变为无损超频
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.performance.heat.overclock",
                    colored(number(MultiblockTooltipSource.HEAT_OVERCLOCK_THRESHOLD))));
        }
    }

    private static void addTierSkips(List<String> lines, int skips) {
        if (skips == 1) return;
        if (skips == MultiblockTooltipSource.UNLIMITED_TIER_SKIPS) {
            // #tr gtnr.tooltip.performance.tier_skip.unlimited
            // # Recipe voltage tiers are unrestricted
            // # zh_CN 不限制配方电压越级
            lines.add(StatCollector.translateToLocal("gtnr.tooltip.performance.tier_skip.unlimited"));
        } else if (skips == MultiblockTooltipSource.NO_TIER_SKIP) {
            // #tr gtnr.tooltip.performance.tier_skip.none
            // # Recipes cannot exceed the input voltage tier
            // # zh_CN 配方电压不得高于输入电压等级
            lines.add(StatCollector.translateToLocal("gtnr.tooltip.performance.tier_skip.none"));
        } else {
            // #tr gtnr.tooltip.performance.tier_skip.max
            // # Recipes may exceed the input voltage by at most %s tiers
            // # zh_CN 配方电压最多可高于输入电压%s级
            lines.add(
                StatCollector
                    .translateToLocalFormatted("gtnr.tooltip.performance.tier_skip.max", colored(number(skips))));
        }
    }

    private static String threshold(TierThreshold threshold) {
        if (threshold.part() == TieredPart.HIGH_PRESSURE) return partName(threshold.part());
        if (threshold.tierName() != null) return partName(threshold.part()) + " " + threshold.tierName();
        // #tr gtnr.tooltip.performance.part_tier
        // # %s tier %s
        // # zh_CN %s等级%s
        return StatCollector.translateToLocalFormatted(
            "gtnr.tooltip.performance.part_tier",
            partName(threshold.part()),
            colored(number(threshold.tier())));
    }

    private static String partName(TieredPart part) {
        return StatCollector.translateToLocal(part.langKey);
    }

    private static String colored(String value) {
        return EnumChatFormatting.AQUA + value + EnumChatFormatting.GRAY;
    }

    private static String number(double value) {
        return new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }

    private record Effects(List<String> lines, boolean perTier) {}
}
