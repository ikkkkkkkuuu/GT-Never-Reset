package com.xyp.gtneverreset.common.machine.tooltip;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.HeatRules;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TieredPart;
import com.xyp.gtneverreset.common.machine.tooltip.PerformanceSummary.BonusStat;
import com.xyp.gtneverreset.common.machine.tooltip.PerformanceSummary.TierBonus;
import com.xyp.gtneverreset.common.machine.tooltip.PerformanceSummary.TierThreshold;

import gregtech.api.enums.GTValues;
import gregtech.api.util.tooltip.TooltipHelper;

final class MultiblockPerformanceFormatter {

    // #tr gtnr.structure.tooltip.stat_with_bonus
    // # %s, %s
    // # zh_CN %s，%s
    private static final String WITH_BONUS_KEY = "gtnr.structure.tooltip.stat_with_bonus";
    // #tr gtnr.structure.tooltip.per_tier
    // # %2$s per %1$s
    // # zh_CN 每%1$s%2$s
    private static final String BONUS_KEY = "gtnr.structure.tooltip.per_tier";
    // #tr gtnr.structure.tooltip.change.increase
    // # increases by %s
    // # zh_CN 增加%s
    private static final String INCREASE_KEY = "gtnr.structure.tooltip.change.increase";
    // #tr gtnr.structure.tooltip.change.decrease
    // # decreases by %s
    // # zh_CN 减少%s
    private static final String DECREASE_KEY = "gtnr.structure.tooltip.change.decrease";
    // #tr gtnr.structure.tooltip.per_tier.duration
    // # recipe time %2$s per %1$s
    // # zh_CN 每%1$s%2$s配方时间
    private static final String DURATION_BONUS_KEY = "gtnr.structure.tooltip.per_tier.duration";
    // #tr gtnr.structure.tooltip.change.compound
    // # %s (compounding)
    // # zh_CN %s(乘算叠加)
    private static final String COMPOUND_KEY = "gtnr.structure.tooltip.change.compound";
    // #tr gtnr.structure.tooltip.switch_bonus
    // # %2$s at %1$s
    // # zh_CN %s%s
    private static final String SWITCH_BONUS_KEY = "gtnr.structure.tooltip.switch_bonus";
    // #tr gtnr.structure.tooltip.join.list
    // # %s, %s
    // # zh_CN %s、%s
    private static final String LIST_JOIN_KEY = "gtnr.structure.tooltip.join.list";
    // #tr gtnr.structure.tooltip.join.last
    // # %s and %s
    // # zh_CN %s和%s
    private static final String LAST_JOIN_KEY = "gtnr.structure.tooltip.join.last";
    // #tr gtnr.structure.tooltip.part.tier
    // # %s §fTier§7
    // # zh_CN %s§f等级§7
    private static final String PART_TIER_KEY = "gtnr.structure.tooltip.part.tier";

    static String statLine(String line, PerformanceSummary summary, BonusStat... stats) {
        List<BonusStat> shown = Arrays.asList(stats);
        Map<TierBonus, List<TieredPart>> partsBySameBonus = new LinkedHashMap<>();
        for (TierBonus bonus : summary.tierBonuses()) {
            if (!shown.contains(bonus.stat())) continue;
            partsBySameBonus
                .computeIfAbsent(sameBonusAs(partsBySameBonus.keySet(), bonus), ignored -> new ArrayList<>())
                .add(bonus.part());
        }
        String casingName = summary.casingName();
        for (Map.Entry<TierBonus, List<TieredPart>> entry : partsBySameBonus.entrySet()) {
            line = StatCollector.translateToLocalFormatted(
                WITH_BONUS_KEY,
                line,
                bonusText(entry.getKey(), entry.getValue(), casingName));
        }
        return line;
    }

    private static TierBonus sameBonusAs(Collection<TierBonus> shown, TierBonus bonus) {
        for (TierBonus other : shown) {
            if (other.perTier() && bonus.perTier()
                && other.stat() == bonus.stat()
                && other.multiplicative() == bonus.multiplicative()
                && changeText(other).equals(changeText(bonus))) return other;
        }
        return bonus;
    }

    private static String partTierText(List<TieredPart> parts, String casingName) {
        List<String> names = new ArrayList<>();
        for (TieredPart part : parts) names.add(partName(part, casingName));
        return StatCollector.translateToLocalFormatted(PART_TIER_KEY, joinPhrases(names));
    }

    private static String partName(TieredPart part, String casingName) {
        boolean namedCasing = part == TieredPart.CASING && casingName != null;
        return TooltipHelper.tierText(namedCasing ? casingName : StatCollector.translateToLocal(part.langKey));
    }

    private static String bonusText(TierBonus bonus, List<TieredPart> parts, String casingName) {
        String part = partTierText(parts, casingName);
        if (!bonus.perTier()) return StatCollector.translateToLocalFormatted(SWITCH_BONUS_KEY, part, changeText(bonus));
        String key = bonus.stat() == BonusStat.DURATION ? DURATION_BONUS_KEY : BONUS_KEY;
        String text = StatCollector.translateToLocalFormatted(key, part, changeText(bonus));
        return bonus.multiplicative() ? StatCollector.translateToLocalFormatted(COMPOUND_KEY, text) : text;
    }

    private static String changeText(TierBonus bonus) {
        double change = bonus.multiplicative() ? bonus.value() - 1 : bonus.value();
        return changeText(bonus.stat(), change, bonus.multiplicative());
    }

    private static String changeText(BonusStat stat, double change, boolean multiplicative) {
        String amount = (stat == BonusStat.PARALLEL || stat == BonusStat.THREADS) && !multiplicative
            ? Long.toString(Math.round(Math.abs(change)))
            : TooltipHelper.percentageFormat.format(Math.abs(change));
        return StatCollector.translateToLocalFormatted(
            change < 0 ? DECREASE_KEY : INCREASE_KEY,
            TooltipHelper.coloredText(amount, colorOf(stat)));
    }

    private static EnumChatFormatting colorOf(BonusStat stat) {
        return switch (stat) {
            case PARALLEL, THREADS -> TooltipHelper.PARALLEL_COLOR;
            case SPEED, DURATION -> TooltipHelper.SPEED_COLOR;
            case ENERGY -> TooltipHelper.EFF_COLOR;
        };
    }

    public static String joinPhrases(List<String> phrases) {
        if (phrases.isEmpty()) return "";
        String joined = phrases.get(0);
        for (int i = 1; i < phrases.size() - 1; i++) {
            joined = StatCollector.translateToLocalFormatted(LIST_JOIN_KEY, joined, phrases.get(i));
        }
        if (phrases.size() == 1) return joined;
        return StatCollector.translateToLocalFormatted(LAST_JOIN_KEY, joined, phrases.get(phrases.size() - 1));
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

}
