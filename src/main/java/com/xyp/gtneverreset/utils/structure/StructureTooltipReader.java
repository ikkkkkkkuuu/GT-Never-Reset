package com.xyp.gtneverreset.utils.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.utils.structure.StructureSummary.BlockLine;
import com.xyp.gtneverreset.utils.structure.StructureSummary.ControllerPosition;
import com.xyp.gtneverreset.utils.structure.StructureSummary.HatchLine;
import com.xyp.gtneverreset.utils.structure.StructureSummary.MaterialTier;
import com.xyp.gtneverreset.utils.structure.StructureSummary.Section;
import com.xyp.gtneverreset.utils.structure.StructureSummary.Size;
import com.xyp.gtneverreset.utils.structure.StructureSummary.TierDetails;
import com.xyp.gtneverreset.utils.structure.StructureTooltipHooks.HatchRule;
import com.xyp.gtneverreset.utils.structure.StructureTooltipHooks.PieceCall;

import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.structure.IStructureChannels;
import gregtech.api.structure.error.StructureError;
import gregtech.common.misc.GTStructureChannels;

public final class StructureTooltipReader {

    private static final String CHANNEL_TAG = "channels";
    private static final int MAX_PROJECTOR_STACK = 64;
    private static final int MIN_LAYERED_SIZES = 3;
    private static final String[] HATCH_PREFIXES = { "GT5U.MBTT.", "gt.blockmachines.", "hatch_type_" };
    private static final HatchElement[] HATCH_ORDER = { HatchElement.Energy, HatchElement.ExoticEnergy,
        HatchElement.MultiAmpEnergy, HatchElement.Dynamo, HatchElement.ExoticDynamo, HatchElement.Maintenance,
        HatchElement.Muffler, HatchElement.InputBus, HatchElement.InputHatch, HatchElement.OutputBus,
        HatchElement.OutputHatch };

    public static MTEEnhancedMultiBlockBase<?> detachedCopy(MTEEnhancedMultiBlockBase<?> machine) {
        IMetaTileEntity copy = machine.newMetaEntity(null);
        if (!(copy instanceof MTEEnhancedMultiBlockBase<?>detached) || copy == machine
            || copy.getClass() != machine.getClass()
            || copy.getBaseMetaTileEntity() != null) {
            throw new IllegalStateException(
                "Cannot create a detached tooltip machine: " + machine.getClass()
                    .getName());
        }
        return detached;
    }

    public static StructureSummary readStructure(MTEEnhancedMultiBlockBase<?> machine) {
        return readDetachedStructure(detachedCopy(machine));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static StructureSummary readDetachedStructure(MTEEnhancedMultiBlockBase<?> detached) {
        if (detached.getBaseMetaTileEntity() != null || !(detached instanceof MultiblockTooltipSource)) {
            throw new IllegalArgumentException("Automatic structure tooltips require a detached tooltip source");
        }
        return readTyped((MTEEnhancedMultiBlockBase) detached);
    }

    private static <T extends MTEEnhancedMultiBlockBase<T>> StructureSummary readTyped(T controller) {
        Set<String> channels = new LinkedHashSet<>();
        List<Variant<T>> variants = recordVariants(controller, channels);
        Variant<T> first = variants.get(0), last = variants.get(variants.size() - 1);
        Map<Position, IStructureElement<T>> layer = layerCells(variants);
        boolean layered = layer != null;
        boolean independent = !layered && !isAdditive(variants);
        List<Section> sections = new ArrayList<>();
        ShapeData firstData = readVariant(controller, first, first.cells(), channels, true);
        ShapeData largestData = firstData;
        if (layered) {
            sections.add(toSection(firstData, null, true));
            sections.add(toSection(readVariant(controller, variants.get(1), layer, channels, false), null, false));
            largestData = readVariant(controller, last, last.cells(), channels, false);
        } else {
            Map<Position, IStructureElement<T>> previous = Map.of();
            for (Variant<T> variant : variants) {
                Map<Position, IStructureElement<T>> shown = variant.cells();
                if (!independent) {
                    shown = new LinkedHashMap<>(shown);
                    shown.keySet()
                        .removeAll(previous.keySet());
                }
                ShapeData fullData = variant == first ? firstData
                    : readVariant(controller, variant, variant.cells(), channels, true);
                ShapeData data = independent || variant == first ? fullData
                    : readVariant(controller, variant, shown, channels, false);
                TierDetails details = independent ? new TierDetails(
                    size(
                        variant.cells()
                            .keySet()),
                    hollow(variant.cells()),
                    controller(
                        variant.cells()
                            .keySet()),
                    hatchLines(data.hatches(), data.rules())) : null;
                Section section = toSection(data, details, independent || variant == first);
                if (!independent && variant != first) {
                    section = new Section(
                        section.blocks(),
                        section.materialTiers(),
                        null,
                        cumulativeCasingRequirements(fullData),
                        hatchLines(fullData.hatches(), fullData.rules()));
                }
                sections.add(section);
                largestData = fullData;
                previous = variant.cells();
            }
        }
        List<BlockLine> firstBlocks = new ArrayList<>(
            sections.get(0)
                .blocks());
        if (!sections.get(0)
            .materialTiers()
            .isEmpty()) {
            firstBlocks.addAll(
                sections.get(0)
                    .materialTiers()
                    .get(0)
                    .blocks());
        }
        return new StructureSummary(
            size(
                first.cells()
                    .keySet()),
            hollow(last.cells()),
            controller(
                first.cells()
                    .keySet()),
            List.copyOf(firstBlocks),
            independent ? List.of()
                : hatchLines(layered ? largestData.hatches() : firstData.hatches(), firstData.rules()),
            channelDescriptions(
                channels,
                firstData.source()
                    .getStructureTooltipMaterialTierChannel()),
            size(
                last.cells()
                    .keySet()),
            List.copyOf(sections),
            layered);
    }

    @SuppressWarnings("unchecked")
    private static <T extends MTEEnhancedMultiBlockBase<T>> T freshCopy(T machine) {
        return (T) detachedCopy(machine);
    }

    private static <T extends MTEEnhancedMultiBlockBase<T>> List<Variant<T>> recordVariants(T machine,
        Set<String> channels) {
        List<Variant<T>> variants = new ArrayList<>();
        Set<PieceCall> previous = Set.of();
        IStructureDefinition<T> definition = null;
        for (int stackSize = 1; stackSize <= MAX_PROJECTOR_STACK; stackSize++) {
            T probe = freshCopy(machine);
            int selected = stackSize;
            List<PieceCall> pieces = StructureTooltipHooks.record(probe, recording -> {
                probe.construct(channelProbe(selected, channels), true);
                if (recording.pieces.isEmpty()) throw new IllegalStateException("No structure pieces were recorded");
                return List.copyOf(new LinkedHashSet<>(recording.pieces));
            });
            Set<PieceCall> current = new HashSet<>(pieces);
            if (!current.equals(previous)) {
                // Keep element identities stable even when definitions are cached per controller instance.
                if (definition == null) definition = probe.getStructureDefinition();
                variants.add(new Variant<>(selected, pieces, cells(definition, pieces)));
            }
            previous = current;
        }
        return variants;
    }

    private static <T> boolean isAdditive(List<Variant<T>> variants) {
        for (int index = 1; index < variants.size(); index++) {
            if (!new HashSet<>(
                variants.get(index)
                    .pieces()).containsAll(
                        variants.get(index - 1)
                            .pieces()))
                return false;
        }
        return true;
    }

    private static <T> Map<Position, IStructureElement<T>> layerCells(List<Variant<T>> variants) {
        if (variants.size() < MIN_LAYERED_SIZES || isAdditive(variants)) return null;
        Map<IStructureElement<T>, Integer> layer = null;
        Map<IStructureElement<T>, Integer> previous = elementCounts(
            variants.get(0)
                .cells());
        Size previousSize = size(
            variants.get(0)
                .cells()
                .keySet());
        Size growth = null;
        for (int index = 1; index < variants.size(); index++) {
            Variant<T> variant = variants.get(index);
            Map<IStructureElement<T>, Integer> counts = elementCounts(variant.cells());
            Map<IStructureElement<T>, Integer> added = new LinkedHashMap<>();
            Set<IStructureElement<T>> elements = new LinkedHashSet<>(previous.keySet());
            elements.addAll(counts.keySet());
            for (IStructureElement<T> element : elements) {
                int delta = counts.getOrDefault(element, 0) - previous.getOrDefault(element, 0);
                if (delta < 0) return null;
                if (delta > 0) added.put(element, delta);
            }
            Size current = size(
                variant.cells()
                    .keySet());
            Size step = new Size(
                current.width() - previousSize.width(),
                current.height() - previousSize.height(),
                current.depth() - previousSize.depth());
            int growingAxes = (step.width() > 0 ? 1 : 0) + (step.height() > 0 ? 1 : 0) + (step.depth() > 0 ? 1 : 0);
            if (step.width() < 0 || step.height() < 0
                || step.depth() < 0
                || growingAxes != 1
                || added.isEmpty()
                || layer != null && (!layer.equals(added) || !growth.equals(step))) return null;
            layer = added;
            growth = step;
            previous = counts;
            previousSize = current;
        }
        Map<Position, IStructureElement<T>> cells = new LinkedHashMap<>();
        int index = 0;
        for (var entry : layer.entrySet()) {
            for (int count = 0; count < entry.getValue(); count++)
                cells.put(new Position(index++, 0, 0), entry.getKey());
        }
        return cells;
    }

    private static <T> Map<IStructureElement<T>, Integer> elementCounts(Map<Position, IStructureElement<T>> cells) {
        Map<IStructureElement<T>, Integer> counts = new LinkedHashMap<>();
        for (IStructureElement<T> element : cells.values()) {
            if (element != StructureUtility.<T>isAir() && element != StructureUtility.<T>notAir()) {
                counts.merge(element, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static <T extends MTEEnhancedMultiBlockBase<T>> ShapeData readVariant(T machine, Variant<T> variant,
        Map<Position, IStructureElement<T>> shown, Set<String> channels, boolean collectRequirements) {
        T probe = freshCopy(machine);
        return StructureTooltipHooks.record(probe, recording -> {
            probe.construct(channelProbe(variant.stackSize(), channels), true);
            MultiblockTooltipSource source = (MultiblockTooltipSource) probe;
            if (collectRequirements) {
                List<StructureError> errors = new ArrayList<>();
                source.collectTooltipStructureRequirements(errors, variant.stackSize());
                if (!errors.isEmpty()) throw new IllegalStateException("Unrecorded structure count requirements");
            }
            Map<MaterialKey, BlockGroup> blocks = new LinkedHashMap<>();
            Map<String, HatchGroup> hatches = new LinkedHashMap<>();
            readGroups(probe, shown, channels, source, blocks, hatches);
            return new ShapeData(
                source,
                new ArrayList<>(blocks.values()),
                hatches,
                List.copyOf(recording.casingMinimums),
                List.copyOf(recording.hatchRules));
        });
    }

    private static Section toSection(ShapeData data, TierDetails details, boolean requirements) {
        List<String> casingRequirements = new ArrayList<>();
        List<Integer> inferred = new ArrayList<>();
        if (requirements) {
            for (int minimum : data.minimums()) {
                List<String> lines = data.source()
                    .getStructureTooltipCasingRequirements(minimum);
                if (lines.isEmpty()) inferred.add(minimum);
                else casingRequirements.addAll(lines);
            }
            applyCasingMinimum(data.blocks(), inferred);
        }
        data.blocks()
            .sort(
                Comparator.comparingInt(BlockGroup::total)
                    .reversed());
        List<BlockLine> blocks = new ArrayList<>();
        List<BlockGroup> tiered = new ArrayList<>();
        for (BlockGroup group : data.blocks()) {
            if (group.materialNames.isEmpty()) blocks.add(blockLine(group, group.names));
            else tiered.add(group);
        }
        List<MaterialTier> materialTiers = new ArrayList<>();
        if (!tiered.isEmpty()) {
            for (int tier = 1; tier <= data.source()
                .getStructureTooltipMaterialTiers(); tier++) {
                List<BlockLine> tierBlocks = new ArrayList<>();
                for (BlockGroup group : tiered) tierBlocks.add(blockLine(group, group.materialNames.get(tier - 1)));
                String header = data.source()
                    .getStructureTooltipMaterialTierHeader(tier);
                if (header == null) {
                    // #tr gtnr.tooltip.structure.material_tier
                    // # Material tier %s
                    // # zh_CN 材料等级 %s
                    header = StatCollector.translateToLocalFormatted("gtnr.tooltip.structure.material_tier", tier);
                }
                materialTiers.add(new MaterialTier(header, List.copyOf(tierBlocks)));
            }
        }
        return new Section(List.copyOf(blocks), List.copyOf(materialTiers), details, List.copyOf(casingRequirements));
    }

    private static BlockLine blockLine(BlockGroup group, List<String> names) {
        return new BlockLine(count(group.minimum, group.total()), names, group.tiered && group.materialNames.isEmpty());
    }

    private static List<String> cumulativeCasingRequirements(ShapeData data) {
        List<String> requirements = new ArrayList<>();
        List<Integer> inferred = new ArrayList<>();
        for (int minimum : data.minimums()) {
            List<String> lines = data.source()
                .getStructureTooltipCasingRequirements(minimum);
            if (lines.isEmpty()) inferred.add(minimum);
            else requirements.addAll(lines);
        }
        if (!inferred.isEmpty()) {
            BlockGroup target = casingMinimumTarget(data.blocks(), inferred);
            // #tr gtnr.tooltip.structure.casing.total
            // # At least %s %s in total at this structure tier
            // # zh_CN 此结构等级的整体至少需要 %s 个%s
            requirements.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.structure.casing.total",
                    inferred.get(0),
                    String.join(" / ", target.names)));
        }
        return List.copyOf(requirements);
    }

    private static <T> Map<Position, IStructureElement<T>> cells(IStructureDefinition<T> definition,
        List<PieceCall> pieces) {
        Map<Position, IStructureElement<T>> cells = new LinkedHashMap<>();
        for (PieceCall piece : pieces) {
            int a = 0, b = 0, c = 0;
            for (IStructureElement<T> element : definition.getStructureFor(piece.piece())) {
                if (element.isNavigating()) {
                    a = (element.resetA() ? 0 : a) + element.getStepA();
                    b = (element.resetB() ? 0 : b) + element.getStepB();
                    c = (element.resetC() ? 0 : c) + element.getStepC();
                } else {
                    cells.put(
                        new Position(a - piece.horizontalOffset(), b - piece.verticalOffset(), c - piece.depthOffset()),
                        element);
                    a++;
                }
            }
        }
        if (cells.isEmpty()) throw new IllegalStateException("The recorded structure has no elements");
        return cells;
    }

    private static <T> void readGroups(T controller, Map<Position, IStructureElement<T>> cells, Set<String> channels,
        MultiblockTooltipSource source, Map<MaterialKey, BlockGroup> blocks, Map<String, HatchGroup> hatches) {
        int materialTiers = source.getStructureTooltipMaterialTiers();
        if (materialTiers < 0 || materialTiers > MAX_PROJECTOR_STACK) {
            throw new IllegalArgumentException("Material tiers must be between 0 and " + MAX_PROJECTOR_STACK);
        }
        IStructureChannels materialChannel = source.getStructureTooltipMaterialTierChannel();
        if (materialTiers > 0 && materialChannel != null) channels.add(materialChannel.get());
        Map<IStructureElement<T>, List<Position>> elements = new LinkedHashMap<>();
        for (var cell : cells.entrySet()) {
            if (cell.getValue() == StructureUtility.<T>isAir() || cell.getValue() == StructureUtility.<T>notAir())
                continue;
            elements.computeIfAbsent(cell.getValue(), key -> new ArrayList<>())
                .add(cell.getKey());
        }
        for (var element : elements.entrySet()) {
            List<String> description;
            BlocksToPlace placement;
            try {
                description = element.getKey()
                    .getDescription(controller);
                placement = element.getKey()
                    .getBlocksToPlace(controller, null, 0, 0, 0, materialProbe(1, channels, materialChannel), null);
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                    "Cannot read a tooltip structure element of " + controller.getClass()
                        .getName()
                        + ": "
                        + element.getKey()
                            .getClass()
                            .getName(),
                    e);
            }
            List<String> blockNames = new ArrayList<>();
            List<String> hatchKeys = new ArrayList<>();
            if (description != null) {
                for (String key : description) {
                    if (isHatchKey(key)) hatchKeys.add(key);
                    else {
                        blockNames.add(StatCollector.translateToLocal(key));
                    }
                }
            }
            if (hatchKeys.isEmpty() && placement != null) {
                blockNames = placedNames(placement, blockNames);
            } else if (placement != null && placement.getStacks() != null) {
                List<String> placedNames = new ArrayList<>();
                for (ItemStack stack : placement.getStacks()) placedNames.add(stack.getDisplayName());
                if (!placedNames.isEmpty()) blockNames = placedNames;
            }
            if (blockNames.isEmpty() && hatchKeys.isEmpty()) {
                throw new IllegalStateException(
                    "A structure element has no description or placement: " + element.getKey()
                        .getClass()
                        .getName());
            }
            List<String> replacementNames = blockNames;
            if (!blockNames.isEmpty()) {
                List<String> names = List.copyOf(new LinkedHashSet<>(blockNames));
                List<List<String>> materialNames = new ArrayList<>();
                List<String> second = placedNames(
                    element.getKey()
                        .getBlocksToPlace(controller, null, 0, 0, 0, channelProbe(2, channels), null),
                    names);
                boolean tiered = !names.equals(second);
                if (materialTiers > 0) {
                    for (int tier = 1; tier <= materialTiers; tier++) {
                        materialNames.add(
                            placedNames(
                                element.getKey()
                                    .getBlocksToPlace(
                                        controller,
                                        null,
                                        0,
                                        0,
                                        0,
                                        materialProbe(tier, channels, materialChannel),
                                        null),
                                names));
                    }
                    if (materialNames.stream()
                        .distinct()
                        .count() == 1) materialNames.clear();
                }
                if (tiered && materialNames.isEmpty() && description != null) {
                    List<String> describedNames = description.stream()
                        .filter(key -> !isHatchKey(key))
                        .map(StatCollector::translateToLocal)
                        .distinct()
                        .toList();
                    if (!describedNames.isEmpty()) names = describedNames;
                }
                MaterialKey key = new MaterialKey(names, List.copyOf(materialNames), tiered);
                replacementNames = materialNames.isEmpty() ? names
                    : materialNames.stream()
                        .flatMap(List::stream)
                        .distinct()
                        .toList();
                BlockGroup block = blocks.computeIfAbsent(key, BlockGroup::new);
                int amount = element.getValue()
                    .size();
                if (hatchKeys.isEmpty()) {
                    block.fixed += amount;
                    block.minimum += amount;
                } else block.replaceable += amount;
            }
            for (String key : hatchKeys) {
                HatchGroup hatch = hatches.computeIfAbsent(key, ignored -> new HatchGroup());
                hatch.keys.add(key);
                hatch.order = Math.min(hatch.order, hatchOrder(key));
                if (replacementNames.isEmpty()) hatch.dedicated.addAll(element.getValue());
                else hatch.replaces.addAll(replacementNames);
            }
        }
    }

    private static List<String> placedNames(BlocksToPlace placement, List<String> fallback) {
        if (placement == null || placement.getStacks() == null) return fallback;
        Set<String> names = new LinkedHashSet<>();
        for (ItemStack stack : placement.getStacks()) names.add(stack.getDisplayName());
        return names.isEmpty() ? fallback : List.copyOf(names);
    }

    private static ItemStack materialProbe(int tier, Set<String> channels, IStructureChannels materialChannel) {
        ItemStack trigger = channelProbe(materialChannel == null ? tier : 1, channels);
        if (materialChannel != null) ChannelDataAccessor.setChannelData(trigger, materialChannel.get(), tier);
        return trigger;
    }

    private static void applyCasingMinimum(Iterable<BlockGroup> blocks, List<Integer> minimums) {
        if (minimums.isEmpty()) return;
        BlockGroup target = casingMinimumTarget(blocks, minimums);
        target.minimum = Math.max(target.minimum, minimums.get(0));
    }

    private static BlockGroup casingMinimumTarget(Iterable<BlockGroup> blocks, List<Integer> minimums) {
        if (minimums.size() != 1) throw new IllegalStateException("Multiple casing counters need an explicit mapping");
        BlockGroup target = null;
        for (BlockGroup block : blocks) {
            if (block.replaceable > 0 && (target == null || block.replaceable > target.replaceable)) target = block;
        }
        if (target == null || minimums.get(0) > target.total()) {
            throw new IllegalStateException("Cannot map the casing minimum to its structure element");
        }
        return target;
    }

    private static List<HatchLine> hatchLines(Map<String, HatchGroup> recordedHatches, List<HatchRule> rules) {
        Map<String, HatchGroup> hatches = new LinkedHashMap<>();
        for (var entry : recordedHatches.entrySet()) hatches.put(entry.getKey(), new HatchGroup(entry.getValue()));
        for (HatchRule rule : rules) {
            for (String key : rule.keys()) {
                if (!hatches.containsKey(key)) {
                    throw new IllegalStateException("A hatch requirement has no described structure position: " + key);
                }
            }
        }
        for (HatchRule rule : rules) {
            HatchGroup combined = hatches.get(
                rule.keys()
                    .get(0));
            for (String key : rule.keys()) {
                HatchGroup other = hatches.get(key);
                if (other != combined) {
                    combined.keys.addAll(other.keys);
                    combined.replaces.addAll(other.replaces);
                    combined.dedicated.addAll(other.dedicated);
                    combined.order = Math.min(combined.order, other.order);
                    for (String alias : other.keys) hatches.put(alias, combined);
                }
            }
        }
        List<HatchGroup> groups = new ArrayList<>(new LinkedHashSet<>(hatches.values()));
        groups.sort(Comparator.comparingInt(group -> group.order));
        List<HatchLine> lines = new ArrayList<>();
        for (HatchGroup group : groups) {
            int fixed = 0;
            for (Position position : group.dedicated) {
                boolean shared = groups.stream()
                    .anyMatch(other -> other != group && other.dedicated.contains(position));
                if (!shared) fixed++;
            }
            int min = fixed, max = group.replaces.isEmpty() ? group.dedicated.size() : StructureTooltipHooks.UNLIMITED;
            Map<String, Integer> individualMinimums = new HashMap<>();
            Map<String, Integer> individualMaximums = new HashMap<>();
            Map<String, Integer> individualPerLayer = new HashMap<>();
            int perLayer = 0;
            for (HatchRule rule : rules) {
                if (Collections.disjoint(group.keys, rule.keys())) continue;
                if (group.keys.size() == rule.keys()
                    .size() && group.keys.containsAll(rule.keys())) {
                    min = Math.max(min, rule.min());
                    max = Math.min(max, rule.max());
                    perLayer = Math.max(perLayer, rule.perLayer());
                } else if (rule.keys()
                    .size() == 1) {
                        individualPerLayer.merge(
                            rule.keys()
                                .get(0),
                            rule.perLayer(),
                            Math::max);
                        individualMinimums.merge(
                            rule.keys()
                                .get(0),
                            rule.min(),
                            Math::max);
                        individualMaximums.merge(
                            rule.keys()
                                .get(0),
                            rule.max(),
                            Math::min);
                    }
            }
            min = Math.max(
                min,
                individualMinimums.values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum());
            perLayer = Math.max(
                perLayer,
                individualPerLayer.values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum());
            if (individualMaximums.size() == group.keys.size()) {
                long sum = individualMaximums.values()
                    .stream()
                    .mapToLong(Integer::longValue)
                    .sum();
                max = (int) Math.min(max, Math.min(Integer.MAX_VALUE, sum));
            }
            if (min > max) throw new IllegalStateException("Conflicting hatch tooltip requirements: " + group.keys);
            List<String> locations = new ArrayList<>();
            if (perLayer > 0) {
                // #tr gtnr.tooltip.structure.hatch.per_layer
                // # %s per layer
                // # zh_CN 每层 %s 个
                locations
                    .add(StatCollector.translateToLocalFormatted("gtnr.tooltip.structure.hatch.per_layer", perLayer));
            }
            lines.add(
                new HatchLine(
                    perLayer > 0 ? null : count(min, max),
                    List.copyOf(group.keys),
                    List.copyOf(group.replaces),
                    List.copyOf(locations)));
        }
        return lines;
    }

    private static Size size(Set<Position> positions) {
        int[] min = { 0, 0, 0 }, max = { 0, 0, 0 };
        for (Position position : positions) {
            min[0] = Math.min(min[0], position.a());
            min[1] = Math.min(min[1], position.b());
            min[2] = Math.min(min[2], position.c());
            max[0] = Math.max(max[0], position.a());
            max[1] = Math.max(max[1], position.b());
            max[2] = Math.max(max[2], position.c());
        }
        return new Size(max[0] - min[0] + 1, max[1] - min[1] + 1, max[2] - min[2] + 1);
    }

    private static ControllerPosition controller(Set<Position> positions) {
        int minA = 0, maxA = 0, maxB = 0;
        boolean front = true;
        for (Position position : positions) {
            minA = Math.min(minA, position.a());
            maxA = Math.max(maxA, position.a());
            maxB = Math.max(maxB, position.b());
            if (position.c() < 0) front = false;
        }
        return new ControllerPosition(maxB + 1, front && -minA == maxA);
    }

    private static <T> boolean hollow(Map<Position, IStructureElement<T>> cells) {
        Map<Line, int[]> alongA = new HashMap<>(), alongC = new HashMap<>();
        for (var cell : cells.entrySet()) {
            if (cell.getValue() == StructureUtility.<T>isAir()) continue;
            Position position = cell.getKey();
            widen(alongA, new Line(position.b(), position.c()), position.a());
            widen(alongC, new Line(position.a(), position.b()), position.c());
        }
        for (var cell : cells.entrySet()) {
            if (cell.getValue() != StructureUtility.<T>isAir()) continue;
            Position position = cell.getKey();
            if (inside(alongA.get(new Line(position.b(), position.c())), position.a())
                && inside(alongC.get(new Line(position.a(), position.b())), position.c())) return true;
        }
        return false;
    }

    private static void widen(Map<Line, int[]> spans, Line line, int value) {
        int[] span = spans.computeIfAbsent(line, ignored -> new int[] { value, value });
        span[0] = Math.min(span[0], value);
        span[1] = Math.max(span[1], value);
    }

    private static boolean inside(int[] span, int value) {
        return span != null && span[0] < value && value < span[1];
    }

    private static boolean isHatchKey(String key) {
        if (StructureTooltipHooks.isHatchKey(key)) return true;
        return Arrays.stream(HATCH_PREFIXES)
            .anyMatch(key::startsWith);
    }

    private static int hatchOrder(String key) {
        for (int i = 0; i < HATCH_ORDER.length; i++) {
            if (HATCH_ORDER[i].getDescriptionLangKeys()
                .contains(key)) return i;
        }
        return HATCH_ORDER.length;
    }

    private static String count(int min, int max) {
        if (min == max) return String.valueOf(min);
        return max == StructureTooltipHooks.UNLIMITED ? min + "+" : min + "-" + max;
    }

    private static ItemStack channelProbe(int stackSize, Set<String> channels) {
        ItemStack trigger = new ItemStack(Items.stick, stackSize);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag(CHANNEL_TAG, new ChannelRecorder(channels));
        trigger.setTagCompound(root);
        return trigger;
    }

    private static List<IStructureChannels> channelDescriptions(Set<String> channels,
        IStructureChannels materialChannel) {
        Map<String, IStructureChannels> known = new LinkedHashMap<>();
        for (GTStructureChannels channel : GTStructureChannels.values()) known.putIfAbsent(channel.get(), channel);
        if (materialChannel != null) known.put(materialChannel.get(), materialChannel);
        List<IStructureChannels> descriptions = new ArrayList<>();
        for (String channel : channels) descriptions.add(known.getOrDefault(channel, new CustomChannel(channel)));
        return List.copyOf(descriptions);
    }

    private record CustomChannel(String get) implements IStructureChannels {

        @Override
        public String getDefaultTooltip() {
            return get;
        }

        @Override
        public void registerAsIndicator(ItemStack indicator, int channelValue) {
            StructureLibAPI.registerChannelItem(get, GTNeverReset.MODID, channelValue, indicator);
        }
    }

    private static final class ChannelRecorder extends NBTTagCompound {

        private final Set<String> channels;

        private ChannelRecorder(Set<String> channels) {
            this.channels = channels;
        }

        @Override
        public boolean hasKey(String key, int type) {
            channels.add(key);
            return super.hasKey(key, type);
        }
    }

    private record Position(int a, int b, int c) {}

    private record Line(int first, int second) {}

    private record Variant<T> (int stackSize, List<PieceCall> pieces, Map<Position, IStructureElement<T>> cells) {}

    private record ShapeData(MultiblockTooltipSource source, List<BlockGroup> blocks, Map<String, HatchGroup> hatches,
        List<Integer> minimums, List<HatchRule> rules) {}

    private record MaterialKey(List<String> names, List<List<String>> tiers, boolean tiered) {}

    private static final class BlockGroup {

        private final List<String> names;
        private final List<List<String>> materialNames;
        private final boolean tiered;
        private int fixed;
        private int replaceable;
        private int minimum;

        private BlockGroup(MaterialKey key) {
            this.names = key.names();
            this.materialNames = key.tiers();
            this.tiered = key.tiered();
        }

        private int total() {
            return fixed + replaceable;
        }
    }

    private static final class HatchGroup {

        private final Set<String> keys = new LinkedHashSet<>();
        private final Set<String> replaces = new LinkedHashSet<>();
        private final Set<Position> dedicated = new LinkedHashSet<>();
        private int order = Integer.MAX_VALUE;

        private HatchGroup() {}

        private HatchGroup(HatchGroup other) {
            keys.addAll(other.keys);
            replaces.addAll(other.replaces);
            dedicated.addAll(other.dedicated);
            order = other.order;
        }
    }
}
