package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

@Desugar
public record ProjectorBatchCells(Long2IntLinkedOpenHashMap cells, long[] emptyCells, ProjectorBatchHatches hatches,
    boolean compatible, boolean limited) {

    private static final int MAX_PREVIEW_CELLS = 250_000;

    static boolean fitsPreview(ProjectorStructure shape, ProjectorBatch batch) {
        return (long) batch.machines() * (shape.size + shape.emptyCells.length) <= MAX_PREVIEW_CELLS;
    }

    public static ProjectorBatchCells collect(ProjectorStructure shape, ProjectorPlan plan, ExtendedFacing facing,
        ProjectorBatch.Layout layout, Consumer<Map<Long, Integer>> copyOverrides) {
        var cells = new Long2IntLinkedOpenHashMap();
        if (!fitsPreview(shape, plan.batch)) return new ProjectorBatchCells(cells, new long[0], null, true, false);

        cells.defaultReturnValue(-1);
        var empty = new LongOpenHashSet();
        int[] abc = new int[3], world = new int[3];
        boolean compatible = layout.solve();
        var hatches = layout.hatches();
        int[] source = new int[3];
        int copies = plan.batch.machines();
        int cellCount = shape.size + shape.emptyCells.length;
        for (int copy = 0; copy < copies; copy++) {
            var at = layout.placement(copy);
            Map<Long, Integer> localOverrides = null;
            for (int i = 0; i < cellCount; i++) {
                if (i < shape.size) {
                    source[0] = shape.a[i];
                    source[1] = shape.b[i];
                    source[2] = shape.c[i];
                } else {
                    long pos = shape.emptyCells[i - shape.size];
                    source[0] = ProjectorPlan.unpackA(pos);
                    source[1] = ProjectorPlan.unpackB(pos);
                    source[2] = ProjectorPlan.unpackC(pos);
                }
                at.facing()
                    .getWorldOffset(source, world);
                world[0] += at.x();
                world[1] += at.y();
                world[2] += at.z();
                facing.getOffsetABC(world, abc);
                long key = ProjectorPlan.pack(abc[0], abc[1], abc[2]);
                if (hatches != null) {
                    var hatch = hatches.sample(key, i, copy);
                    if (copyOverrides != null && hatch != null && i < shape.size) {
                        if (localOverrides == null) localOverrides = new TreeMap<>();
                        localOverrides.put(ProjectorPlan.pack(source[0], source[1], source[2]), hatch.machine());
                    }
                }
                if (i >= shape.size) {
                    empty.add(key);
                    continue;
                }
                int value = at.facing()
                    .getIndex() * shape.size + i;
                int previous = cells.putIfAbsent(key, value);
                if (previous >= 0 && shape.blocks[previous % shape.size] == Blocks.air && shape.blocks[i] != Blocks.air)
                    cells.put(key, value);
            }
            if (copyOverrides != null)
                copyOverrides.accept(localOverrides == null ? Collections.emptyMap() : localOverrides);
        }
        empty.removeAll(cells.keySet());
        return new ProjectorBatchCells(
            cells,
            empty.toLongArray(),
            hatches,
            compatible && (hatches == null || !hatches.incompatible),
            layout.searchLimited());
    }

    public List<Material> materials(ProjectorStructure template) {
        Map<ItemStack, Integer> totals = new ItemStackMap<>(true);
        for (var it = Long2IntMaps.fastIterator(cells); it.hasNext();) {
            var entry = it.next();
            var hatch = hatches == null ? null : hatches.get(entry.getLongKey());
            ItemStack item = hatch == null ? template.items[entry.getIntValue() % template.size] : hatch.item();
            if (item != null) totals.merge(item, 1, Integer::sum);
        }
        List<Material> materials = new ArrayList<>();
        for (Material entry : template.materials) {
            Integer count = totals.remove(entry.stack());
            if (count != null) materials.add(new Material(entry.stack(), count));
        }
        totals.forEach((stack, count) -> materials.add(new Material(stack, count)));
        return materials;
    }
}
