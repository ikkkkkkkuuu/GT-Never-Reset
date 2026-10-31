package com.xyp.gtneverreset.common.machine.tooltip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TooltipStats;
import com.xyp.gtneverreset.common.machine.tooltip.PerformanceSummary.BonusStat;
import com.xyp.gtneverreset.common.machine.tooltip.StructureSummary.BlockLine;
import com.xyp.gtneverreset.common.machine.tooltip.StructureSummary.ControllerPosition;
import com.xyp.gtneverreset.common.machine.tooltip.StructureSummary.HatchLine;
import com.xyp.gtneverreset.common.machine.tooltip.StructureSummary.Section;
import com.xyp.gtneverreset.common.machine.tooltip.StructureSummary.Size;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtnrlib.text.TooltipLineWrapper;

import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.tooltip.MarkdownTooltipLoader;
import gregtech.api.util.tooltip.TooltipHelper;

public final class AutoMultiblockTooltipBuilder extends MultiblockTooltipBuilder {

    private String[] information;
    private String[] structureInformation;

    @Override
    public MultiblockTooltipBuilder toolTipFinisher(EnumChatFormatting separatorColor, int separatorLength) {
        super.toolTipFinisher(separatorColor, separatorLength);
        information = TooltipLineWrapper.wrap(super.getInformation());
        structureInformation = TooltipLineWrapper.wrap(super.getStructureInformation());
        return this;
    }

    @Override
    public String[] getInformation() {
        return information;
    }

    @Override
    public String[] getStructureInformation() {
        return structureInformation;
    }

    public AutoMultiblockTooltipBuilder addAutoTooltip(MTEEnhancedMultiBlockBase<?> machine) {
        MTEEnhancedMultiBlockBase<?> detached = StructureTooltipReader.detachedCopy(machine);
        if (!(detached instanceof MultiblockTooltipSource source)) {
            throw new IllegalArgumentException(
                "Missing automatic tooltip source: " + machine.getClass()
                    .getName());
        }
        String type = source.getTooltipMachineType();
        if (type != null && !type.isEmpty()) addMachineType(type);
        StructureSummary structure = StructureTooltipReader.readDetachedStructure(detached);
        PerformanceSummary performance = MultiblockPerformanceReader.readPerformance(machine, structure);
        TooltipStats stats = performance.stats();
        if (stats != null) {
            if (stats.parallels() > 0) addStaticParallelInfo(stats.parallels());
            for (String line : MultiblockPerformanceFormatter.bonuses(performance, BonusStat.PARALLEL)) addInfo(line);
        }
        int recipeThreads = source.getTooltipRecipeThreads();
        if (recipeThreads > 0) {
            // #tr gtnr.multiblock.threads.capacity
            // # Concurrent recipe threads: {\\AQUA}%s{\\RESET}
            // # zh_CN 跨配方线程数：{\\AQUA}%s{\\RESET}
            addInfo(StatCollector.translateToLocalFormatted("gtnr.multiblock.threads.capacity", recipeThreads));
        }
        if (stats != null) {
            if (Double.isFinite(stats.speed())) {
                addStaticSpeedInfo((float) stats.speed());
                for (String line : MultiblockPerformanceFormatter
                    .bonuses(performance, BonusStat.SPEED, BonusStat.DURATION)) addInfo(line);
            }
            if (Double.isFinite(stats.energyUsage())) {
                addStaticEuEffInfo((float) stats.energyUsage());
                for (String line : MultiblockPerformanceFormatter.bonuses(performance, BonusStat.ENERGY)) addInfo(line);
            }
        }
        Map<String, Object> variables = tooltipVariables(source, stats);
        for (String line : markdownLines(source.getTooltipPath(), variables)) addInfo(line);
        for (String line : source.getTooltipFeatureLines()) addInfo(line);
        for (String line : MultiblockPerformanceFormatter.rules(performance)) addInfo(line);
        if (stats != null && stats.perfectOverclock()) addPerfectOCInfo();
        addStructure(structure);
        for (String note : source.getTooltipStructureNotes()) addStructureInfo(note);
        for (MachineModuleType module : MachineModuleType.values()) {
            int limit = source.getParameterModuleLimit(module);
            if (limit <= 0) continue;
            // #tr gtnr.tooltip.structure.module_limit
            // # %s: at most %s per machine
            // # zh_CN %s：每台机器最多 %s 个
            addStructureInfo(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.structure.module_limit",
                    StatCollector.translateToLocal(module.getTranslationKey()),
                    limit));
        }
        return this;
    }

    private static Map<String, Object> tooltipVariables(MultiblockTooltipSource source, TooltipStats stats) {
        Map<String, Object> variables = new HashMap<>();
        if (stats != null) {
            variables.put("parallels", stats.parallels());
            if (Double.isFinite(stats.speed())) {
                variables.put("speed", TooltipHelper.percentageFormat.format(stats.speed()));
            }
            if (Double.isFinite(stats.energyUsage())) {
                variables.put("energy_usage", TooltipHelper.percentageFormat.format(stats.energyUsage()));
            }
        }
        variables.putAll(source.getTooltipVars());
        return variables;
    }

    private static List<String> markdownLines(String path, Map<String, Object> variables) {
        if (path == null || path.isEmpty()) return List.of();
        try {
            return MarkdownTooltipLoader.STANDARD
                .loadStandardPath(new ResourceLocation(GTNeverReset.MODID, path), variables);
        } catch (RuntimeException failure) {
            GTNeverReset.LOG.warn("Cannot load tooltip document {}", path, failure);
            return List.of();
        }
    }

    private void addStructure(StructureSummary summary) {
        boolean structureTiers = summary.sections()
            .size() > 1;
        boolean independentTiers = summary.hasIndependentTiers();
        if (!independentTiers) {
            addSize(summary.size(), summary.allTiers(), structureTiers, summary.hollow());
            addControllerLine(summary.controller());
            for (HatchLine hatch : summary.hatches()) addHatch(hatch);
        }
        for (int tier = 0; tier < summary.sections()
            .size(); tier++) {
            if (structureTiers) {
                addStructureInfo("");
                addStructureInfo(
                    summary.layered() ? layerHeader(tier) : structureTierHeader(tier + 1, !independentTiers));
            }
            Section section = summary.sections()
                .get(tier);
            if (section.details() != null) {
                var details = section.details();
                addSize(details.size(), details.size(), false, details.hollow());
                addControllerLine(details.controller());
                for (HatchLine hatch : details.hatches()) addHatch(hatch);
            }
            addSection(section);
        }
        if (structureTiers) {
            addStructureInfo("");
            // #tr gtnr.tooltip.structure.channel.master
            // # Projector stack size determines: %s
            // # zh_CN 投影器物品堆叠数量决定：%s
            addStructureInfo(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.structure.channel.master",
                    EnumChatFormatting.AQUA + masterChannel(summary)));
        }
        for (var channel : summary.channels()) addSubChannelUsage(channel);
    }

    private void addSize(Size first, Size all, boolean variable, boolean hollow) {
        if (!variable) {
            beginStructureBlock(first.width(), first.height(), first.depth(), hollow);
            return;
        }
        beginVariableStructureBlock(
            first.width(),
            all.width(),
            first.height(),
            all.height(),
            first.depth(),
            all.depth(),
            hollow);
    }

    private void addControllerLine(ControllerPosition controller) {
        String position;
        if (controller.frontCenter()) {
            // #tr gtnr.tooltip.structure.controller.front
            // # Front center, layer %s from bottom
            // # zh_CN 正面中央，自底向上第%s层
            position = StatCollector
                .translateToLocalFormatted("gtnr.tooltip.structure.controller.front", controller.layer());
        } else {
            // #tr gtnr.tooltip.structure.controller.layer
            // # Layer %s from bottom
            // # zh_CN 自底向上第%s层
            position = StatCollector
                .translateToLocalFormatted("gtnr.tooltip.structure.controller.layer", controller.layer());
        }
        addController(position);
    }

    private void addSection(Section section) {
        for (HatchLine hatch : section.hatches()) addHatch(hatch);
        for (BlockLine block : section.blocks()) addBlock(block);
        for (var tier : section.materialTiers()) {
            addStructureInfo("");
            addStructureInfo(tier.header());
            for (BlockLine block : tier.blocks()) addBlock(block);
        }
        for (String requirement : section.casingRequirements()) addStructureInfo(requirement);
    }

    private void addBlock(BlockLine block) {
        addCasing(block.count(), String.join(" / ", block.names()), block.tiered());
    }

    private static String masterChannel(StructureSummary summary) {
        if (!summary.layered()) {
            // #tr gtnr.tooltip.structure.channel.structure_tier
            // # Structure tier
            // # zh_CN 结构等级
            return StatCollector.translateToLocal("gtnr.tooltip.structure.channel.structure_tier");
        }
        Size first = summary.size();
        Size all = summary.allTiers();
        if (first.height() != all.height()) {
            // #tr gtnr.tooltip.structure.channel.height
            // # Structure height
            // # zh_CN 结构高度
            return StatCollector.translateToLocal("gtnr.tooltip.structure.channel.height");
        }
        if (first.depth() != all.depth()) {
            // #tr gtnr.tooltip.structure.channel.length
            // # Structure length
            // # zh_CN 结构长度
            return StatCollector.translateToLocal("gtnr.tooltip.structure.channel.length");
        }
        // #tr gtnr.tooltip.structure.channel.width
        // # Structure width
        // # zh_CN 结构宽度
        return StatCollector.translateToLocal("gtnr.tooltip.structure.channel.width");
    }

    private static String structureTierHeader(int tier, boolean additive) {
        if (tier == 1 || !additive) {
            // #tr gtnr.tooltip.structure.tier
            // # Structure tier %s
            // # zh_CN 结构等级 %s
            return StatCollector.translateToLocalFormatted("gtnr.tooltip.structure.tier", tier);
        }
        // #tr gtnr.tooltip.structure.tier.extra
        // # Structure tier %s: additional blocks
        // # zh_CN 结构等级 %s：新增方块
        return StatCollector.translateToLocalFormatted("gtnr.tooltip.structure.tier.extra", tier);
    }

    private static String layerHeader(int section) {
        if (section == 0) {
            // #tr gtnr.tooltip.structure.base
            // # Base structure
            // # zh_CN 基础结构
            return StatCollector.translateToLocal("gtnr.tooltip.structure.base");
        }
        // #tr gtnr.tooltip.structure.per_layer
        // # Each additional layer
        // # zh_CN 每新增一层
        return StatCollector.translateToLocal("gtnr.tooltip.structure.per_layer");
    }

    private void addHatch(HatchLine hatch) {
        List<String> names = new ArrayList<>();
        for (String key : hatch.nameKeys()) names.add(StatCollector.translateToLocal(key));
        List<String> locations = new ArrayList<>(hatch.locations());
        if (!hatch.replaces()
            .isEmpty()) {
            // #tr gtnr.tooltip.structure.hatch.replaces
            // # Replaces %s
            // # zh_CN 可替换%s
            locations.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.structure.hatch.replaces",
                    String.join(" / ", hatch.replaces())));
        }
        if (locations.isEmpty()) {
            // #tr gtnr.tooltip.structure.hatch.dedicated
            // # Designated positions
            // # zh_CN 指定位置
            locations.add(StatCollector.translateToLocal("gtnr.tooltip.structure.hatch.dedicated"));
        }
        String count = hatch.count() == null ? "" : EnumChatFormatting.YELLOW + hatch.count() + " ";
        addStructureInfo(
            count + EnumChatFormatting.WHITE
                + String.join(" / ", names)
                + ": "
                + EnumChatFormatting.GRAY
                + String.join(" / ", locations));
    }

}
