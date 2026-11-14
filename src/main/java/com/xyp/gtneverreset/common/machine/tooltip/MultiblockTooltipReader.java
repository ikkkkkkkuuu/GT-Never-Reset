package com.xyp.gtneverreset.common.machine.tooltip;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TooltipStats;
import com.xyp.gtneverreset.common.machine.tooltip.StructureSummary.HatchLine;
import com.xyp.gtneverreset.common.machine.tooltip.StructureSummary.Section;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;

import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.util.tooltip.MarkdownTooltipLoader;
import gregtech.api.util.tooltip.TooltipHelper;

final class MultiblockTooltipReader {

    private MultiblockTooltipReader() {}

    static InfoSummary readInfo(MTEEnhancedMultiBlockBase<?> machine, String keyPrefix) {
        MTEEnhancedMultiBlockBase<?> detached = read(
            machine,
            "prototype",
            () -> StructureTooltipReader.detachedCopy(machine),
            null);
        if (detached == null) return null;
        MultiblockTooltipSource source = detached instanceof MultiblockTooltipSource declarations ? declarations
            : new MultiblockTooltipSource() {};
        StructureSummary structure = read(
            machine,
            "structure",
            () -> StructureTooltipReader.readDetachedStructure(detached),
            null);
        PerformanceSummary performance = read(
            machine,
            "performance",
            () -> MultiblockPerformanceReader.readPerformance(machine, structure),
            null);
        TooltipStats stats = performance == null ? null : performance.stats();
        String type = read(machine, "machine type", () -> machineType(detached, source, keyPrefix), null);
        Map<String, Object> variables = read(machine, "variables", () -> tooltipVariables(source, stats), Map.of());
        String path = read(machine, "document path", source::getTooltipPath, null);
        if ((path == null || path.isEmpty()) && keyPrefix != null)
            path = keyPrefix.substring(keyPrefix.lastIndexOf('.') + 1)
                .replace('_', '-');
        List<String> lines = markdownLines(path, variables);
        if (lines == null) lines = legacyLines(keyPrefix);
        Set<String> hatchKeys = hatchKeys(structure);
        List<List<String>> groups = new ArrayList<>();
        List<String> hatchLines = new ArrayList<>();
        for (String key : hatchKeys)
            hatchLines.addAll(read(machine, "hatch features", () -> source.getTooltipHatchLines(key), List.of()));
        groups.add(List.copyOf(hatchLines));
        groups.add(List.copyOf(read(machine, "upgrades", source::getTooltipUpgradeLines, List.of())));
        Map<MachineModuleType, Integer> limits = new EnumMap<>(MachineModuleType.class);
        for (MachineModuleType module : MachineModuleType.values()) {
            int limit = read(machine, "module limit", () -> source.getParameterModuleLimit(module), 0);
            if (limit > 0) limits.put(module, limit);
        }
        return new InfoSummary(
            structure,
            performance,
            type,
            List.copyOf(lines),
            List.copyOf(read(machine, "features", source::getTooltipFeatureLines, List.of())),
            List.copyOf(read(machine, "structure notes", source::getTooltipStructureNotes, List.of())),
            List.copyOf(groups),
            Set.copyOf(hatchKeys),
            Map.copyOf(limits),
            read(machine, "energy type", source::usesSteamForTooltip, false),
            read(machine, "recipe threads", source::getTooltipRecipeThreads, 0),
            read(machine, "pollution", () -> detached.getPollutionPerSecond(null), 0));
    }

    private static Set<String> hatchKeys(StructureSummary structure) {
        Set<String> keys = new LinkedHashSet<>();
        if (structure == null) return keys;
        for (HatchLine hatch : structure.hatches()) keys.addAll(hatch.nameKeys());
        for (Section section : structure.sections()) {
            for (HatchLine hatch : section.hatches()) keys.addAll(hatch.nameKeys());
            if (section.details() != null) for (HatchLine hatch : section.details()
                .hatches()) keys.addAll(hatch.nameKeys());
        }
        return keys;
    }

    private static <T> T read(MTEEnhancedMultiBlockBase<?> machine, String part, Supplier<T> action, T fallback) {
        try {
            T result = action.get();
            return result == null ? fallback : result;
        } catch (RuntimeException failure) {
            GTNeverReset.LOG.warn(
                "cannot read {} tooltip {}",
                machine.getClass()
                    .getSimpleName(),
                part,
                failure);
            return fallback;
        }
    }

    static String keyPrefixOf(MTEEnhancedMultiBlockBase<?> machine) {
        String key = machine.getLocalNameKey();
        if (key != null && key.startsWith("gtnr.machine.") && key.endsWith(".name"))
            return key.substring(0, key.length() - 5);
        String name = machine.getClass()
            .getSimpleName()
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .toLowerCase(Locale.ROOT);
        String prefix = "gtnr.machine." + name;
        return StatCollector.canTranslate(prefix + ".name") || StatCollector.canTranslate(prefix + ".recipe_type")
            ? prefix
            : null;
    }

    private static String machineType(MTEEnhancedMultiBlockBase<?> machine, MultiblockTooltipSource source,
        String prefix) {
        if (prefix != null && StatCollector.canTranslate(prefix + ".recipe_type"))
            return StatCollector.translateToLocal(prefix + ".recipe_type");
        String explicit = source.getTooltipMachineType();
        if (explicit != null && !explicit.isEmpty()) return explicit;
        Set<String> names = new LinkedHashSet<>();
        Set<Integer> modes = new HashSet<>();
        int original = machine.machineMode;
        try {
            do {
                modes.add(machine.machineMode);
                for (var map : machine.getAvailableRecipeMaps())
                    if (map != null) names.add(StatCollector.translateToLocal(map.unlocalizedName));
                if (!machine.supportsMachineModeSwitch()) break;
                machine.machineMode = machine.nextMachineMode();
            } while (modes.size() < 64 && !modes.contains(machine.machineMode));
        } finally {
            machine.machineMode = original;
        }
        return names.isEmpty() ? null : String.join(" / ", names);
    }

    private static List<String> legacyLines(String prefix) {
        if (prefix == null) return List.of();
        for (String suffix : List.of(".tooltip", ".info")) {
            List<String> lines = new ArrayList<>();
            for (int index = 0; index < 128; index++) addLegacyLine(lines, prefix + suffix + "." + index);
            if (!lines.isEmpty()) return lines;
        }
        List<String> lines = new ArrayList<>();
        addLegacyLine(lines, prefix + ".info");
        return lines;
    }

    private static void addLegacyLine(List<String> lines, String key) {
        if (!StatCollector.canTranslate(key)) return;
        String text = StatCollector.translateToLocal(key);
        if (!Pattern.compile("%(?:[0-9]+\\$)?[-#+ 0,(]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z]")
            .matcher(text)
            .find()) lines.add(text);
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
        if (path == null || path.isEmpty()) return null;
        try {
            return MarkdownTooltipLoader.STANDARD
                .loadStandardPath(new ResourceLocation(GTNeverReset.MODID, path), variables);
        } catch (RuntimeException failure) {
            GTNeverReset.LOG.warn("Cannot load tooltip document {}", path, failure);
            return null;
        }
    }

    record InfoSummary(StructureSummary structure, PerformanceSummary performance, String machineType,
        List<String> lines, List<String> features, List<String> structureNotes, List<List<String>> groups,
        Set<String> hatchKeys, Map<MachineModuleType, Integer> moduleLimits, boolean steam, int recipeThreads,
        int pollution) {}
}
