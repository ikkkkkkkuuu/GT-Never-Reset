package com.xyp.gtneverreset.client.projector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorEnergyHatches;
import com.xyp.gtneverreset.common.projector.ProjectorEnergyHatches.Flow;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.HatchSlot;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtnrlib.gui.widget.IconRowList.Row;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HatchElement;
import gregtech.api.util.GTUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;

@SideOnly(Side.CLIENT)
public class ProjectorHatchPage {

    public static final String CASING = "casing";
    private static final String LANG = ProjectorGui.LANG;
    private static final String[] DEPTH = { "back", "", "front" }, SIDE = { "left", "", "right" };
    private static final Map<Object, Integer> REMEMBERED = new HashMap<>();

    private final Supplier<ProjectorStructure> structure;
    private final ProjectorPlan working;
    private final IntSupplier selected;
    private final IntConsumer select;
    private boolean configChanged;
    private ProjectorStructure ordered;
    private int[] spots = new int[0];
    private ExtendedFacing placedFor;
    private String[] places = new String[0];
    private int[] layers = new int[0];
    private HatchSlot groupedFor;
    private Map<Object, Map<String, List<Integer>>> grouped = Collections.emptyMap();

    public ProjectorHatchPage(Supplier<ProjectorStructure> structure, ProjectorPlan working, IntSupplier selected,
        IntConsumer select) {
        this.structure = structure;
        this.working = working;
        this.selected = selected;
        this.select = select;
    }

    public void step(int direction) {
        ProjectorStructure current = structure.get();
        if (current == null || spots(current).length == 0) return;
        int index = indexOf(selected.getAsInt());
        int next = index < 0 ? (direction > 0 ? 0 : spots.length - 1) : Math.floorMod(index + direction, spots.length);
        select.accept(spots[next]);
    }

    public ItemStack icon() {
        ProjectorStructure current = structure.get();
        int cell = selected.getAsInt();
        return current == null || cell < 0 ? null : current.items[cell];
    }

    public String placeLine() {
        ProjectorStructure current = structure.get();
        if (current == null) return "";
        int cell = selected.getAsInt();
        // #tr gtneverreset.projector.gui.hatch_hint
        // # Pick a blue spot or use the arrows (%s spots)
        // # zh_CN 点预览里的蓝框或用箭头选择（共 %s 个仓位）
        if (cell < 0) return StatCollector.translateToLocalFormatted(LANG + "hatch_hint", spots(current).length);
        // #tr gtneverreset.projector.gui.slot_place
        // # Layer %s · %s
        // # zh_CN 第 %s 层 · %s
        return place(current, cell, "slot_place");
    }

    public String countLine() {
        ProjectorStructure current = structure.get();
        int index = indexOf(selected.getAsInt());
        return current == null || index < 0 ? "" : (index + 1) + "/" + spots(current).length;
    }

    public String nameLine() {
        ProjectorStructure current = structure.get();
        int cell = selected.getAsInt();
        if (current == null || cell < 0) return "";
        if (current.blocks[cell] == Blocks.air) return StatCollector
            // #tr gtneverreset.projector.spot.empty
            // # Leave empty
            // # zh_CN 留空
            // #tr gtneverreset.projector.gui.empty_spot
            // # Empty spot, takes only a hatch
            // # zh_CN 空位（只能放仓）
            .translateToLocal(choice() != null ? ProjectorSpotScreen.LANG + "empty" : LANG + "empty_spot");
        String name = current.items[cell] == null ? current.blocks[cell].getLocalizedName()
            : current.items[cell].getDisplayName();
        if (current.slotAt(cell) != null || choice() != null) return name;
        return StatCollector.translateToLocalFormatted(
            ProjectorClient.LANG + "joined",
            name,
            // #tr gtneverreset.projector.gui.not_hatch
            // # No hatch can go here
            // # zh_CN 这一格不能放仓
            StatCollector.translateToLocal(LANG + "not_hatch"));
    }

    public boolean sameKind() {
        return ClientConfig.projectorSameKind;
    }

    public void toggleSameKind() {
        ClientConfig.projectorSameKind = !ClientConfig.projectorSameKind;
        configChanged = true;
    }

    public boolean configChanged() {
        return configChanged;
    }

    public boolean editable() {
        return slot() != null;
    }

    public List<Object> kinds() {
        HatchSlot slot = slot();
        return slot == null ? Collections.emptyList() : new ArrayList<>(groups(slot).keySet());
    }

    public Object kind() {
        int id = heldId();
        if (id != 0) return id < 0 ? null : kindKey(id);
        HatchSlot slot = slot();
        return slot != null && slot.casing() != null ? CASING : null;
    }

    public String kindName(Object kind) {
        if (kind == null) return "";
        if (CASING.equals(kind)) return StatCollector.translateToLocal(LANG + "casing");
        if (kind instanceof Flow flow)
            return (flow == Flow.INPUT ? HatchElement.Energy : HatchElement.Dynamo).getDisplayName();
        if (kind instanceof HatchElement element) return element.getDisplayName();
        HatchSlot slot = slot();
        Map<String, List<Integer>> variants = slot == null ? null : groups(slot).get(kind);
        return variants == null ? "" : variantName(firstOf(variants));
    }

    public ItemStack kindIcon(Object kind) {
        HatchSlot slot = slot();
        if (slot == null || kind == null) return null;
        if (CASING.equals(kind)) return casingStack();
        Map<String, List<Integer>> variants = groups(slot).get(kind);
        return variants == null ? null : ProjectorMachines.stackOf(firstOf(variants));
    }

    public void chooseKind(Object kind) {
        ProjectorStructure current = structure.get();
        HatchSlot slot = slot();
        int cell = selected.getAsInt();
        if (current == null || slot == null || kind == null || kind.equals(kind())) return;
        Map<String, List<Integer>> variants = groups(slot).get(kind);
        int best = remembered(slot, kind, variants);
        if (best == 0 && variants != null) {
            for (List<Integer> ids : variants.values()) {
                for (int candidate : ids) {
                    if (best == 0 || ProjectorMachines.compareHatches(candidate, best, working.hatchTier) < 0)
                        best = candidate;
                }
            }
        }
        put(current, cell, best);
        if (best > 0) REMEMBERED.put(kind, best);
    }

    private int remembered(HatchSlot slot, Object kind, Map<String, List<Integer>> variants) {
        Integer last = REMEMBERED.get(kind);
        if (last == null || variants == null) return 0;
        if (accepts(slot, last)) return last;
        List<Integer> ids = ProjectorMachines.prototype(last) == null ? null : variants.get(modelKey(last));
        return ids == null ? 0 : closest(ids, last);
    }

    public List<Integer> tiers() {
        List<Integer> ids = modelOfHeld();
        if (ids == null) return Collections.emptyList();
        return energyHeld() ? ProjectorEnergyHatches.tierChoices(ids, heldId()) : ids;
    }

    public List<Integer> amperes() {
        List<Integer> ids = modelOfHeld();
        if (ids == null || !energyHeld()) return Collections.emptyList();
        return ProjectorEnergyHatches.amperesChoices(ids, heldId());
    }

    public boolean hasAmperes() {
        return amperes().size() > 1;
    }

    public String amperesLabel() {
        return energyHeld() ? amperesName(heldId()) : "";
    }

    public void stepAmperes(int step) {
        int id = heldId();
        stepWithin(
            amperes(),
            step,
            choice -> ProjectorEnergyHatches.amperes(choice) == ProjectorEnergyHatches.amperes(id)
                && ProjectorEnergyHatches.isLaser(choice) == ProjectorEnergyHatches.isLaser(id));
    }

    public void chooseAmperes(int id) {
        ProjectorStructure current = structure.get();
        if (current != null && amperes().contains(id) && id != heldId()) setModel(current, selected.getAsInt(), id);
    }

    public static String amperesName(int id) {
        String amperes = ProjectorEnergyHatches.amperesLabel(ProjectorEnergyHatches.amperes(id));
        return ProjectorEnergyHatches.isLaser(id)
            // #tr gtneverreset.projector.gui.laser_amperes
            // # %s Laser
            // # zh_CN %s 激光
            ? StatCollector.translateToLocalFormatted(LANG + "laser_amperes", amperes)
            : amperes;
    }

    private boolean energyHeld() {
        int id = heldId();
        return id > 0 && ProjectorEnergyHatches.flowOf(id) != null;
    }

    public int held() {
        return heldId();
    }

    public void chooseTier(int id) {
        ProjectorStructure current = structure.get();
        if (current != null && tiers().contains(id) && id != heldId()) setModel(current, selected.getAsInt(), id);
    }

    public boolean sameCasing() {
        return ClientConfig.projectorSameCasing;
    }

    public void toggleSameCasing() {
        ClientConfig.projectorSameCasing = !ClientConfig.projectorSameCasing;
        configChanged = true;
    }

    public Choice choice() {
        ProjectorStructure current = structure.get();
        Choice choice = current == null ? null : current.choiceAt(selected.getAsInt());
        return choice == null || choice.channel() != null ? null : choice;
    }

    private Choice channelChoice() {
        ProjectorStructure current = structure.get();
        Choice choice = current == null || slot() == null ? null : current.choiceAt(selected.getAsInt());
        return choice == null || choice.channel() == null ? null : choice;
    }

    public String casingChannel() {
        Choice choice = channelChoice();
        return choice == null ? null : choice.channel();
    }

    public boolean channelCasing() {
        return CASING.equals(kind()) && casingChannel() != null;
    }

    public List<ItemStack> casings() {
        Choice channel = channelChoice();
        if (channel != null) return new ArrayList<>(
            ProjectorChannels.tiers(channel)
                .values());
        Choice choice = choice();
        if (choice != null) return choice.blocks();
        HatchSlot slot = slot();
        return slot == null || slot.casing() == null ? Collections.emptyList()
            : Collections.singletonList(slot.casing());
    }

    public String casingKey() {
        ProjectorStructure current = structure.get();
        Choice channel = channelChoice();
        if (current != null && channel != null) {
            ItemStack tier = ProjectorChannels.tiers(channel)
                .get(working.channels.getOrDefault(channel.channel(), 0));
            return ProjectorPlan.blockKey(tier != null ? tier : channel.base());
        }
        Choice choice = choice();
        if (current == null || choice == null) {
            HatchSlot slot = slot();
            return slot == null || slot.casing() == null ? null : ProjectorPlan.blockKey(slot.casing());
        }
        String own = working.cellChoices.get(key(current, selected.getAsInt()));
        return own != null ? own : working.chosenBlock(choice);
    }

    public ItemStack casingStack() {
        String key = casingKey();
        ItemStack chosen = key == null ? null : stackOf(key);
        if (chosen != null) return chosen;
        HatchSlot slot = slot();
        return slot == null ? null : slot.casing();
    }

    public void chooseCasing(String blockKey) {
        ProjectorStructure current = structure.get();
        Choice channel = channelChoice();
        if (channel != null) {
            for (Map.Entry<Integer, ItemStack> tier : ProjectorChannels.tiers(channel)
                .entrySet()) {
                if (ProjectorPlan.blockKey(tier.getValue())
                    .equals(blockKey)) working.setChannel(channel.channel(), tier.getKey());
            }
            return;
        }
        Choice choice = choice();
        int cell = selected.getAsInt();
        if (current == null || choice == null) return;
        if (!ClientConfig.projectorSameCasing) {
            if (blockKey.equals(working.chosenBlock(choice))) working.cellChoices.remove(key(current, cell));
            else working.cellChoices.put(key(current, cell), blockKey);
            return;
        }
        working.chooseBlock(choice, blockKey);
        int group = current.choiceOf[cell];
        for (int other = 0; other < current.size; other++) {
            if (current.choiceOf[other] == group) working.cellChoices.remove(key(current, other));
        }
    }

    public boolean blockCell() {
        return slot() == null && choice() != null;
    }

    public List<String> casingKeys() {
        List<String> keys = new ArrayList<>();
        for (ItemStack casing : casings()) keys.add(ProjectorPlan.blockKey(casing));
        return keys;
    }

    public List<String> blockKeys() {
        Choice choice = choice();
        List<String> keys = new ArrayList<>();
        if (choice == null) return keys;
        if (choice.air()) keys.add(ProjectorPlan.AIR);
        for (ItemStack block : choice.blocks()) keys.add(ProjectorPlan.blockKey(block));
        return keys;
    }

    public ItemStack stackOf(String key) {
        for (ItemStack casing : casings()) {
            if (ProjectorPlan.blockKey(casing)
                .equals(key)) return casing;
        }
        Choice choice = choice();
        return choice == null ? null : choice.find(key);
    }

    public String blockKey() {
        Choice choice = choice();
        return choice == null ? null : working.chosenBlock(choice);
    }

    public void chooseBlock(String key) {
        Choice choice = choice();
        if (choice != null && key != null) working.chooseBlock(choice, key);
    }

    public int groupSize() {
        ProjectorStructure current = structure.get();
        int cell = selected.getAsInt();
        return current == null || cell < 0 || cell >= current.size ? 0 : current.countOf(current.choiceOf[cell]);
    }

    public List<Row> choiceRows() {
        List<Row> casings = new ArrayList<>(), blocks = new ArrayList<>();
        ProjectorStructure current = structure.get();
        if (current == null) return casings;
        int cell = selected.getAsInt();
        for (Map.Entry<Long, String> entry : working.cellChoices.entrySet()) {
            long position = entry.getKey();
            int spot = current.indexOf(
                ProjectorPlan.unpackA(position),
                ProjectorPlan.unpackB(position),
                ProjectorPlan.unpackC(position));
            Choice choice = current.choiceAt(spot);
            if (choice == null) continue;
            casings.add(
                // #tr gtneverreset.projector.gui.slot_place_short
                // # L%s %s
                // # zh_CN %s层 %s
                choiceRow(choice, entry.getValue(), place(current, spot, "slot_place_short"), spot, cell)
                    .onRightClick(() -> working.cellChoices.remove(position)));
        }
        for (Map.Entry<String, String> entry : working.choices.entrySet()) {
            int group = groupOf(current, entry.getKey());
            int first = group < 0 ? -1 : firstCellOf(current, group);
            if (first < 0) continue;
            // #tr gtneverreset.projector.gui.whole_group
            // # whole group · %s
            // # zh_CN 整组 · %s 格
            String detail = StatCollector.translateToLocalFormatted(LANG + "whole_group", current.countOf(group));
            Row row = choiceRow(current.choices.get(group), entry.getValue(), detail, first, cell)
                .onRightClick(() -> working.choices.remove(entry.getKey()));
            (current.slots[first] >= 0 ? casings : blocks).add(row);
        }
        List<Row> rows = new ArrayList<>();
        if (!casings.isEmpty()) {
            // #tr gtneverreset.projector.gui.changed_casings
            // # Changed casings · %s
            // # zh_CN 已换外壳 · %s
            rows.add(Row.header(StatCollector.translateToLocalFormatted(LANG + "changed_casings", casings.size())));
            rows.addAll(casings);
        }
        if (!blocks.isEmpty()) {
            // #tr gtneverreset.projector.gui.changed_blocks
            // # Changed blocks · %s
            // # zh_CN 已换方块 · %s
            rows.add(Row.header(StatCollector.translateToLocalFormatted(LANG + "changed_blocks", blocks.size())));
            rows.addAll(blocks);
        }
        return rows;
    }

    private Row choiceRow(Choice choice, String key, String detail, int spot, int selectedCell) {
        ItemStack block = choice.find(key);
        String name = block == null ? StatCollector.translateToLocal(ProjectorSpotScreen.LANG + "empty")
            : block.getDisplayName();
        return Row.item(block, name, detail, spot == selectedCell, () -> select.accept(spot));
    }

    private static int groupOf(ProjectorStructure current, String key) {
        for (int group = 0; group < current.choices.size(); group++) {
            if (current.choices.get(group)
                .key()
                .equals(key)) return group;
        }
        return -1;
    }

    private static int firstCellOf(ProjectorStructure current, int group) {
        for (int cell = 0; cell < current.size; cell++) {
            if (current.choiceOf[cell] == group) return cell;
        }
        return -1;
    }

    public List<Integer> variants() {
        Map<String, List<Integer>> variants = variantsOfHeld();
        List<Integer> firsts = new ArrayList<>();
        if (variants != null) for (List<Integer> ids : variants.values()) firsts.add(ids.get(0));
        return firsts;
    }

    public Integer variant() {
        List<Integer> ids = modelOfHeld();
        return ids == null ? null : ids.get(0);
    }

    public void chooseVariant(int first) {
        Map<String, List<Integer>> variants = variantsOfHeld();
        ProjectorStructure current = structure.get();
        int id = heldId();
        if (variants == null || current == null || id <= 0) return;
        List<Integer> ids = variants.get(modelKey(first));
        if (ids != null && !ids.contains(id)) setModel(current, selected.getAsInt(), closest(ids, id));
    }

    public String variantLabel(int first) {
        String model = ProjectorEnergyHatches.flowOf(first) == null ? null : ProjectorEnergyHatches.modelOf(first);
        if (ProjectorEnergyHatches.WIRED.equals(model) || ProjectorEnergyHatches.WIRELESS.equals(model))
            // #tr gtneverreset.projector.gui.wired
            // # Wired
            // # zh_CN 有线
            // #tr gtneverreset.projector.gui.wireless
            // # Wireless
            // # zh_CN 无线
            return StatCollector.translateToLocal(LANG + model);
        return variantName(nearestOfModel(first));
    }

    public ItemStack variantIcon(int first) {
        return ProjectorMachines.stackOf(nearestOfModel(first));
    }

    // Models that join hatches of different names show the one closest to the held hatch.
    private int nearestOfModel(int first) {
        Map<String, List<Integer>> variants = variantsOfHeld();
        List<Integer> ids = variants == null ? null : variants.get(modelKey(first));
        int held = heldId();
        return ids == null || held <= 0 ? first : closest(ids, held);
    }

    public boolean hasTiers() {
        return tiers().size() > 1;
    }

    public String tierLabel() {
        int id = heldId();
        return id > 0 ? tierName(id) : "";
    }

    public void stepTier(int step) {
        int tier = ProjectorMachines.tier(heldId());
        stepWithin(tiers(), step, choice -> ProjectorMachines.tier(choice) == tier);
    }

    // A hatch that copies another mod's hatch at the same parameters is not itself among the choices.
    private void stepWithin(List<Integer> choices, int step, IntPredicate sameAsHeld) {
        ProjectorStructure current = structure.get();
        int id = heldId();
        if (choices.isEmpty() || current == null || id <= 0) return;
        int index = choices.indexOf(id);
        for (int i = 0; index < 0 && i < choices.size(); i++) if (sameAsHeld.test(choices.get(i))) index = i;
        if (index < 0) return;
        int position = Math.max(0, Math.min(choices.size() - 1, index + step));
        if (position != index) setModel(current, selected.getAsInt(), choices.get(position));
    }

    public List<Row> spotRows() {
        List<Row> rows = new ArrayList<>();
        ProjectorStructure current = structure.get();
        if (current == null) return rows;
        int cell = selected.getAsInt();
        List<Integer> chosen = new ArrayList<>();
        for (int spot : spots(current)) {
            if (working.overrides.containsKey(key(current, spot))) chosen.add(spot);
        }
        // #tr gtneverreset.projector.gui.chosen_slots
        // # Chosen hatches · %s
        // # zh_CN 已选仓室 · %s
        rows.add(Row.header(StatCollector.translateToLocalFormatted(LANG + "chosen_slots", chosen.size())));
        for (int spot : chosen) {
            ItemStack item = current.items[spot];
            String name = item == null ? current.blocks[spot].getLocalizedName() : item.getDisplayName();
            rows.add(
                Row.item(item, name, place(current, spot, "slot_place_short"), spot == cell, () -> select.accept(spot))
                    .onRightClick(() -> working.overrides.remove(key(current, spot))));
        }
        return rows;
    }

    private HatchSlot slot() {
        ProjectorStructure current = structure.get();
        return current == null ? null : current.slotAt(selected.getAsInt());
    }

    private int heldId() {
        ProjectorStructure current = structure.get();
        int cell = selected.getAsInt();
        if (current == null || current.slotAt(cell) == null) return -1;
        // The shown structure lags behind a choice until the new simulation finishes.
        Integer chosen = working.overrides.get(key(current, cell));
        return chosen != null ? chosen : current.machines[cell];
    }

    private Map<String, List<Integer>> variantsOfHeld() {
        HatchSlot slot = slot();
        int id = heldId();
        return slot == null || id <= 0 ? null : groups(slot).get(kindKey(id));
    }

    private List<Integer> modelOfHeld() {
        Map<String, List<Integer>> variants = variantsOfHeld();
        int id = heldId();
        return variants == null ? null : variants.get(modelKey(id));
    }

    private int closest(List<Integer> ids, int id) {
        return ProjectorEnergyHatches.flowOf(id) != null ? ProjectorEnergyHatches.closest(ids, id)
            : closestTier(ids, ProjectorMachines.tier(id));
    }

    private int closestTier(List<Integer> ids, int tier) {
        int wanted = tier < 0 ? working.hatchTier : tier, best = ids.get(0);
        for (int candidate : ids) {
            if (Math.abs(ProjectorMachines.tier(candidate) - wanted) < Math.abs(ProjectorMachines.tier(best) - wanted))
                best = candidate;
        }
        return best;
    }

    private void setModel(ProjectorStructure current, int cell, int id) {
        put(current, cell, id);
        REMEMBERED.put(kindKey(id), id);
        if (!ClientConfig.projectorSameKind) return;
        Object kind = kindKey(id);
        for (int spot : spots(current)) {
            int held = current.machines[spot];
            if (spot != cell && held != 0 && kind.equals(kindKey(held)) && accepts(current.slotAt(spot), id))
                put(current, spot, id);
        }
    }

    private void put(ProjectorStructure current, int cell, int id) {
        working.overrides.put(key(current, cell), id == 0 ? ProjectorPlan.CASING : id);
    }

    private static boolean accepts(HatchSlot slot, int id) {
        if (slot == null) return false;
        if (id == 0) return slot.casing() != null;
        for (int candidate : slot.candidates()) {
            if (candidate == id) return true;
        }
        return false;
    }

    private static long key(ProjectorStructure current, int cell) {
        return ProjectorPlan.pack(current.a[cell], current.b[cell], current.c[cell]);
    }

    private Map<Object, Map<String, List<Integer>>> groups(HatchSlot slot) {
        if (slot == groupedFor) return grouped;
        Map<Object, Map<String, List<Integer>>> kinds = new LinkedHashMap<>();
        if (slot.casing() != null) kinds.put(CASING, null);
        List<Integer> candidates = new ArrayList<>();
        for (int id : slot.candidates()) candidates.add(id);
        candidates.sort((first, second) -> ProjectorMachines.compareHatches(first, second, 0));
        for (int id : candidates) {
            kinds.computeIfAbsent(kindKey(id), key -> new LinkedHashMap<>())
                .computeIfAbsent(modelKey(id), key -> new ArrayList<>())
                .add(id);
        }
        for (Map.Entry<Object, Map<String, List<Integer>>> kind : kinds.entrySet()) {
            Map<String, List<Integer>> variants = kind.getValue();
            if (variants == null) continue;
            boolean energy = kind.getKey() instanceof Flow;
            for (List<Integer> ids : variants.values())
                ids.sort(energy ? ProjectorEnergyHatches.order() : Comparator.comparingInt(ProjectorMachines::tier));
            if (energy) kind.setValue(plainModelsFirst(variants));
        }
        groupedFor = slot;
        grouped = kinds;
        return kinds;
    }

    private static Map<String, List<Integer>> plainModelsFirst(Map<String, List<Integer>> variants) {
        Map<String, List<Integer>> ordered = new LinkedHashMap<>();
        for (String model : new String[] { ProjectorEnergyHatches.WIRED, ProjectorEnergyHatches.WIRELESS }) {
            if (variants.containsKey(model)) ordered.put(model, variants.get(model));
        }
        ordered.putAll(variants);
        return ordered;
    }

    private static Object kindKey(int id) {
        Flow flow = ProjectorEnergyHatches.flowOf(id);
        if (flow != null) return flow;
        HatchElement element = ProjectorMachines.kindOf(id);
        return element != null ? element
            : ProjectorMachines.prototype(id)
                .getClass();
    }

    // GT gives some hatches of one class different names, such as the auto maintenance hatch, and those are not tiers.
    private static String modelKey(int id) {
        String energy = ProjectorEnergyHatches.flowOf(id) == null ? null : ProjectorEnergyHatches.modelOf(id);
        if (energy != null) return energy;
        String series = ProjectorMachines.seriesOf(id);
        if (series != null) return series;
        return ProjectorMachines.prototype(id)
            .getClass()
            .getName() + "|"
            + variantName(id);
    }

    private static int firstOf(Map<String, List<Integer>> variants) {
        return variants == null ? 0
            : variants.values()
                .iterator()
                .next()
                .get(0);
    }

    public static String tierName(int id) {
        return coloredTier(ProjectorMachines.tier(id));
    }

    public static String coloredTier(int tier) {
        return tier >= 0 && tier < GTValues.VN.length && tier < GTValues.TIER_COLORS.length
            ? GTUtility.getColoredTierNameFromTier((byte) tier)
            : "";
    }

    public static String variantName(int id) {
        String name = ProjectorMachines.stackOf(id)
            .getDisplayName();
        int tier = ProjectorMachines.tier(id);
        if (tier >= 0 && tier < GTValues.VN.length) name = name.replace(GTValues.VN[tier], "");
        return name.replace("()", "")
            .replace("（）", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private int indexOf(int cell) {
        for (int i = 0; i < spots.length; i++) {
            if (spots[i] == cell) return i;
        }
        return -1;
    }

    private int[] spots(ProjectorStructure current) {
        if (current != ordered || working.facing != placedFor) {
            ordered = current;
            placedFor = working.facing;
            int[] bounds = current.worldBounds(working.facing);
            int[] offset = new int[3];
            IntArrayList cells = new IntArrayList();
            for (int cell = 0; cell < current.size; cell++) {
                if (current.slots[cell] >= 0) cells.add(cell);
            }
            int[][] world = new int[current.size][];
            for (int cell : cells) {
                working.facing.getWorldOffset(new int[] { current.a[cell], current.b[cell], current.c[cell] }, offset);
                world[cell] = offset.clone();
            }
            cells.sort((first, second) -> {
                int[] one = world[first], two = world[second];
                if (one[1] != two[1]) return Integer.compare(one[1], two[1]);
                if (one[2] != two[2]) return Integer.compare(one[2], two[2]);
                return Integer.compare(one[0], two[0]);
            });
            spots = cells.toIntArray();
            places = new String[current.size];
            layers = new int[current.size];
            for (int cell : spots) {
                places[cell] = placeWords(world[cell], bounds);
                layers[cell] = world[cell][1] - bounds[1] + 1;
            }
        }
        return spots;
    }

    private String place(ProjectorStructure current, int cell, String key) {
        spots(current);
        if (cell >= layers.length || places[cell] == null) return "";
        return StatCollector.translateToLocalFormatted(LANG + key, layers[cell], places[cell]);
    }

    private String placeWords(int[] world, int[] bounds) {
        ForgeDirection front = working.facing.getDirection();
        // #tr gtneverreset.projector.gui.place.center
        // # middle
        // # zh_CN 中间
        if (front.offsetY != 0) return StatCollector.translateToLocal(LANG + "place.center");
        double x = world[0] - (bounds[0] + bounds[3]) / 2.0, z = world[2] - (bounds[2] + bounds[5]) / 2.0;
        double depth = x * front.offsetX + z * front.offsetZ, side = x * front.offsetZ - z * front.offsetX;
        String depthWord = DEPTH[depth > 0.5 ? 2 : depth < -0.5 ? 0 : 1];
        String sideWord = SIDE[side > 0.5 ? 2 : side < -0.5 ? 0 : 1];
        String key = depthWord.isEmpty() ? sideWord.isEmpty() ? "center" : sideWord
            : sideWord.isEmpty() ? depthWord : depthWord + "_" + sideWord;
        // #tr gtneverreset.projector.gui.place.front_left
        // # front left
        // # zh_CN 左前
        // #tr gtneverreset.projector.gui.place.front
        // # front
        // # zh_CN 前
        // #tr gtneverreset.projector.gui.place.front_right
        // # front right
        // # zh_CN 右前
        // #tr gtneverreset.projector.gui.place.left
        // # left
        // # zh_CN 左
        // #tr gtneverreset.projector.gui.place.right
        // # right
        // # zh_CN 右
        // #tr gtneverreset.projector.gui.place.back_left
        // # back left
        // # zh_CN 左后
        // #tr gtneverreset.projector.gui.place.back
        // # back
        // # zh_CN 后
        // #tr gtneverreset.projector.gui.place.back_right
        // # back right
        // # zh_CN 右后
        return StatCollector.translateToLocal(LANG + "place." + key);
    }
}
