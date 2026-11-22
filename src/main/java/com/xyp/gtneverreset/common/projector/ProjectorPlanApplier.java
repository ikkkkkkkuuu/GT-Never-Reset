package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.HatchSlot;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class ProjectorPlanApplier {

    private final ProjectorSimulation simulation;
    private final ProjectorSimulationWorld world;
    private final Map<IStructureElement<Object>, Boolean> anyMeta = new IdentityHashMap<>();

    public ProjectorPlanApplier(ProjectorSimulation simulation) {
        this.simulation = simulation;
        this.world = simulation.world;
    }

    public void applyChoices() {
        Map<String, String> wanted = simulation.settings.choices();
        if (wanted.isEmpty()) return;
        for (Long2ObjectMap.Entry<IStructureElement<Object>> entry : simulation.elements.long2ObjectEntrySet()) {
            Choice choice = simulation.choiceFor(entry.getValue());
            String key = choice == null ? null : wanted.get(choice.key());
            if (key == null) continue;
            long position = entry.getLongKey();
            replace(
                choice,
                key,
                ProjectorPlan.unpackA(position),
                ProjectorPlan.unpackB(position),
                ProjectorPlan.unpackC(position));
        }
    }

    public void applyCellChoices() {
        int[] position = new int[3];
        for (Map.Entry<Long, String> entry : simulation.settings.cellChoices()
            .entrySet()) {
            toWorld(entry.getKey(), position);
            Choice choice = simulation
                .choiceFor(simulation.elements.get(ProjectorPlan.pack(position[0], position[1], position[2])));
            if (choice != null) replace(choice, entry.getValue(), position[0], position[1], position[2]);
        }
    }

    private void replace(Choice choice, String key, int x, int y, int z) {
        ItemStack target = choice.find(key);
        if (target == null && !(choice.air() && ProjectorPlan.AIR.equals(key))) return;
        if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity) return;
        world.setBlockToAir(x, y, z);
        if (target != null) simulation.place(target, x, y, z);
    }

    public void applyOverrides() {
        int[] position = new int[3];
        for (Map.Entry<Long, Integer> override : simulation.settings.overrides()
            .entrySet()) {
            toWorld(override.getKey(), position);
            int x = position[0], y = position[1], z = position[2];
            IStructureElement<Object> element = simulation.elements.get(ProjectorPlan.pack(x, y, z));
            Integer slot = element == null ? null : simulation.slotOfElement.get(element);
            if (slot == null || slot < 0) continue;
            HatchSlot hatchSlot = simulation.hatchSlots.get(slot);
            int id = override.getValue();
            if (id == ProjectorPlan.CASING) {
                ItemStack casing = casingFor(element, hatchSlot);
                if (casing == null) continue;
                world.setBlockToAir(x, y, z);
                simulation.place(casing, x, y, z);
            } else if (contains(hatchSlot.candidates(), id)) {
                world.setBlockToAir(x, y, z);
                simulation.placeMachine(id, x, y, z);
            }
        }
    }

    private void toWorld(long cell, int[] out) {
        simulation.canonical.getWorldOffset(
            new int[] { ProjectorPlan.unpackA(cell), ProjectorPlan.unpackB(cell), ProjectorPlan.unpackC(cell) },
            out);
        out[0] += ProjectorSimulation.CONTROLLER_X;
        out[1] += ProjectorSimulation.CONTROLLER_Y;
        out[2] += ProjectorSimulation.CONTROLLER_Z;
    }

    private ItemStack casingFor(IStructureElement<Object> element, HatchSlot slot) {
        Choice choice = simulation.choiceFor(element);
        String wanted = choice == null ? null
            : simulation.settings.choices()
                .get(choice.key());
        ItemStack chosen = wanted == null ? null : choice.find(wanted);
        return chosen != null ? chosen : slot.casing();
    }

    public ProjectorStructure collect() {
        world.dropRemovedTileEntities();
        int[] relative = new int[3];
        long[] cells = cells(relative);
        int[] bounds = simulation.bounds;
        bounds[0] = bounds[1] = bounds[2] = Integer.MAX_VALUE;
        bounds[3] = bounds[4] = bounds[5] = Integer.MIN_VALUE;
        for (long cell : cells) {
            simulation.relative(
                ProjectorPlan.unpackA(cell),
                ProjectorPlan.unpackB(cell),
                ProjectorPlan.unpackC(cell),
                relative);
            for (int axis = 0; axis < 3; axis++) {
                bounds[axis] = Math.min(bounds[axis], relative[axis]);
                bounds[axis + 3] = Math.max(bounds[axis + 3], relative[axis]);
            }
        }
        if (cells.length == 0) Arrays.fill(bounds, 0);
        ItemStackMap<Tally> totals = new ItemStackMap<>(false);
        ProjectorStructure structure = new ProjectorStructure(simulation, cells.length);
        for (int i = 0; i < cells.length; i++) {
            int x = ProjectorPlan.unpackA(cells[i]), y = ProjectorPlan.unpackB(cells[i]),
                z = ProjectorPlan.unpackC(cells[i]);
            simulation.relative(x, y, z, relative);
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            int id = world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile ? tile.getMetaTileID() : 0;
            IStructureElement<Object> element = simulation.elements.get(cells[i]);
            Integer slot = element == null ? null : simulation.slotOfElement.get(element);
            Integer choice = element == null ? null : simulation.choiceOfElement.get(element);
            ItemStack item = id > 0 ? ProjectorMachines.stackOf(id)
                : block == Blocks.air ? null : simulation.itemOf(block, meta, x, y, z);
            if (item != null) {
                Tally tally = totals.computeIfAbsent(item, Tally::new);
                tally.count++;
                item = tally.stack;
            }
            int shownMeta = id == 0 && element != null && block != Blocks.air && acceptsAnyMeta(element, x, y, z)
                ? ProjectorStructure.ANY_META
                : meta;
            structure.set(
                i,
                relative[0],
                relative[1],
                relative[2],
                block,
                shownMeta,
                id,
                slot == null ? -1 : slot,
                choice == null ? -1 : choice,
                item);
        }
        simulation.materials = new ArrayList<>();
        for (Tally tally : totals.values()) simulation.materials.add(new Material(tally.stack, tally.count));
        simulation.materials.sort((first, second) -> Integer.compare(second.count(), first.count()));
        structure.materials = simulation.materials;
        return structure;
    }

    private long[] cells(int[] relative) {
        simulation.emptyCells.clear();
        LongArrayList optional = new LongArrayList();
        for (Long2ObjectMap.Entry<IStructureElement<Object>> entry : simulation.elements.long2ObjectEntrySet()) {
            long position = entry.getLongKey();
            int x = ProjectorPlan.unpackA(position), y = ProjectorPlan.unpackB(position),
                z = ProjectorPlan.unpackC(position);
            if (!world.isAirBlock(x, y, z)) continue;
            boolean empty = simulation.checks(entry.getValue(), x, y, z);
            if (empty && simulation.choiceFor(entry.getValue()) == null) {
                simulation.relative(x, y, z, relative);
                simulation.emptyCells.add(ProjectorPlan.pack(relative[0], relative[1], relative[2]));
            } else if (empty) {
                optional.add(position);
            } else {
                Integer slot = simulation.slotOfElement.get(entry.getValue());
                if (slot != null && slot >= 0) optional.add(position);
            }
        }
        LongArrayList positions = new LongArrayList(world.positions);
        positions.addAll(optional);
        long[] cells = positions.toLongArray();
        Arrays.sort(cells);
        return cells;
    }

    // Elements such as any-colour glass accept every variant, so the world check must not insist on one.
    private boolean acceptsAnyMeta(IStructureElement<Object> element, int x, int y, int z) {
        Boolean known = anyMeta.get(element);
        if (known != null) return known;
        int meta = world.getBlockMetadata(x, y, z);
        boolean any = world.getTileEntity(x, y, z) == null;
        for (int other = 0; any && other < 16; other++) {
            if (other == meta) continue;
            world.setBlockMetadataWithNotify(x, y, z, other, 2);
            any = simulation.checks(element, x, y, z);
        }
        world.setBlockMetadataWithNotify(x, y, z, meta, 2);
        anyMeta.put(element, any);
        return any;
    }

    private static boolean contains(int[] values, int value) {
        for (int candidate : values) {
            if (candidate == value) return true;
        }
        return false;
    }

    private static class Tally {

        private final ItemStack stack;
        private int count;

        private Tally(ItemStack stack) {
            this.stack = stack;
        }
    }
}
