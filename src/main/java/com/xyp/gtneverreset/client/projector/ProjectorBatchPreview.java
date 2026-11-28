package com.xyp.gtneverreset.client.projector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorBatchCells;
import com.xyp.gtneverreset.common.projector.ProjectorBatchHatches;
import com.xyp.gtneverreset.common.projector.ProjectorBatchHatches.Sample;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulationWorld;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;

@SideOnly(Side.CLIENT)
final class ProjectorBatchPreview extends ProjectorStructure {

    private static final int MAX_COPY_OUTLINE_BOXES = 64, MAX_COPY_OUTLINE_EDGE_BLOCKS = 8192;
    private static final int[][] NO_COPY_BOUNDS = new int[0][];
    final ProjectorStructure template;
    private final ProjectorBatchHatches edits;
    final boolean outlineOnly;
    final int[] originals;
    final ExtendedFacing[] copyPoses;
    final ExtendedFacing baseFacing;
    final int[][] copyBounds;
    final boolean compatible;
    final int blockedControllers;
    final Formation formation;

    /** Counts controllers, including every controller whose structure visits a shared hatch. */
    static final class Formation {

        final int total;
        int finished, formed, unchecked;
        final Map<String, Integer> errors = new LinkedHashMap<>();
        private final Map<Map<Long, Integer>, Integer> pending = new LinkedHashMap<>();
        private ProjectorPlan.Settings requested;

        Formation(int total) {
            this.total = total;
        }

        void update(ProjectorStructure template) {
            if (pending.isEmpty()) return;
            var it = pending.entrySet()
                .iterator();
            var group = it.next();
            ProjectorStructure checked = template;
            if (!group.getKey()
                .isEmpty()) {
                if (requested == null) {
                    var base = template.settings;
                    var overrides = new TreeMap<>(base.overrides());
                    overrides.putAll(group.getKey());
                    requested = new ProjectorPlan.Settings(
                        base.machine(),
                        base.master(),
                        base.channels(),
                        Collections.unmodifiableMap(overrides),
                        base.choices(),
                        base.cellChoices());
                }
                checked = ProjectorStructures.get(requested);
                if (checked == null) return;
            }
            int count = group.getValue();
            if (!checked.failure.isEmpty() || !checked.limit.isEmpty() || !checked.checked) unchecked += count;
            else if (checked.formed) formed += count;
            else for (String error : checked.errors) errors.merge(error, count, Integer::sum);
            finished += count;
            it.remove();
            requested = null;
        }
    }

    private ProjectorBatchPreview(ProjectorStructure template, ProjectorBatchCells batch, int[] bounds,
        ExtendedFacing baseFacing, Formation formation, ProjectorBatch.Layout layout) {
        super(
            template,
            batch.cells()
                .size(),
            batch.emptyCells(),
            bounds,
            new PreviewWorld());
        this.template = template;
        edits = batch.hatches();
        this.formation = formation;
        this.baseFacing = baseFacing;
        compatible = batch.compatible();
        copyPoses = new ExtendedFacing[size];
        ((PreviewWorld) world).array = this;
        outlineOnly = batch.cells()
            .isEmpty();
        copyBounds = outlineOnly ? NO_COPY_BOUNDS : outlineBounds(layout, formation.total);
        originals = new int[size];
        int i = 0;
        for (var it = Long2IntMaps.fastIterator(batch.cells()); it.hasNext(); i++) {
            var entry = it.next();
            long pos = entry.getLongKey();
            int src = entry.getIntValue() % template.size;
            copyPoses[i] = ExtendedFacing.byIndex(entry.getIntValue() / template.size);
            originals[i] = src;
            Sample edit = edits == null ? null : edits.get(pos);
            ItemStack item = edit == null ? template.items[src] : edit.item();
            set(
                i,
                ProjectorPlan.unpackA(pos),
                ProjectorPlan.unpackB(pos),
                ProjectorPlan.unpackC(pos),
                edit == null ? template.blocks[src] : edit.block(),
                edit == null ? template.metas[src] : edit.meta(),
                edit == null ? template.machines[src] : edit.machine(),
                template.slots[src],
                template.choiceOf[src],
                item);
        }
        int hidden = 0;
        int[] axis = new int[3];
        for (int cell = 0; cell < size; cell++) {
            int src = originals[cell];
            if (template.a[src] != 0 || template.b[src] != 0 || template.c[src] != 0) continue;
            ForgeDirection front = copyPoses[cell].getDirection();
            baseFacing.getOffsetABC(new int[] { front.offsetX, front.offsetY, front.offsetZ }, axis);
            int neighbor = indexOf(a[cell] + axis[0], b[cell] + axis[1], c[cell] + axis[2]);
            if (neighbor >= 0 && blocks[neighbor] != Blocks.air) hidden++;
        }
        blockedControllers = hidden;
        materials = batch.materials(template);
    }

    private static int[][] outlineBounds(ProjectorBatch.Layout layout, int copies) {
        if (copies > MAX_COPY_OUTLINE_BOXES) return NO_COPY_BOUNDS;
        int[] first = new int[6];
        layout.copyWorldBounds(0, first);
        long outlineLength = 4L * copies * (first[3] - first[0] + first[4] - first[1] + first[5] - first[2] + 3);
        if (outlineLength > MAX_COPY_OUTLINE_EDGE_BLOCKS) return NO_COPY_BOUNDS;
        int[][] result = new int[copies][];
        result[0] = first;
        for (int copy = 1; copy < copies; copy++) {
            result[copy] = new int[6];
            layout.copyWorldBounds(copy, result[copy]);
        }
        return result;
    }

    Sample editAt(int i) {
        return edits == null ? null : edits.get(ProjectorPlan.pack(a[i], b[i], c[i]));
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    // Keep render coordinates in array space. GT block renderers also ask the player's
    // preview world directly for metadata and tiles, bypassing RenderBlocks.blockAccess.
    private static final class PreviewWorld extends ProjectorSimulationWorld {

        private ProjectorBatchPreview array;
        private final int[] relative = new int[3], abc = new int[3];

        private int cellAt(int x, int y, int z) {
            if (array == null) return -1;
            relative[0] = x - array.originX;
            relative[1] = y - array.originY;
            relative[2] = z - array.originZ;
            array.canonical.getOffsetABC(relative, abc);
            return array.indexOf(abc[0], abc[1], abc[2]);
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            int cell = cellAt(x, y, z);
            int src = cell < 0 ? -1 : array.originals[cell];
            return src < 0 ? Blocks.air : array.blocks[cell];
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            int cell = cellAt(x, y, z);
            int src = cell < 0 ? -1 : array.originals[cell];
            if (src < 0) return 0;
            Sample edit = array.editAt(cell);
            if (edit != null) return edit.meta();
            int[][] pos = array.template.simulationPositions();
            return array.template.world.getBlockMetadata(pos[0][src], pos[1][src], pos[2][src]);
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            int cell = cellAt(x, y, z);
            int src = cell < 0 ? -1 : array.originals[cell];
            if (src < 0) return null;
            Sample edit = array.editAt(cell);
            if (edit != null) return edit.tile();
            int[][] pos = array.template.simulationPositions();
            int sx = pos[0][src], sy = pos[1][src], sz = pos[2][src];
            return array.template.world.getTileEntity(sx, sy, sz);
        }
    }

    void copyOrigin(int cell, int[] origin) {
        int src = originals[cell];
        templateOffset(src, copyPoses[cell], origin);
        origin[0] = a[cell] - origin[0];
        origin[1] = b[cell] - origin[1];
        origin[2] = c[cell] - origin[2];
    }

    int onCopy(int src, ExtendedFacing pose, int[] origin, int[] out) {
        templateOffset(src, pose, out);
        return indexOf(origin[0] + out[0], origin[1] + out[1], origin[2] + out[2]);
    }

    private void templateOffset(int src, ExtendedFacing pose, int[] out) {
        int[] position = { template.a[src], template.b[src], template.c[src] };
        pose.getWorldOffset(position, out);
        System.arraycopy(out, 0, position, 0, 3);
        baseFacing.getOffsetABC(position, out);
    }

    int atOrigin(int original) {
        return original < 0 || original >= template.size ? -1
            : indexOf(template.a[original], template.b[original], template.c[original]);
    }

    static final class Cache {

        private ProjectorStructure template;
        private ProjectorPlan.Settings originSettings;
        private ExtendedFacing facing;
        private long arrangement;
        private final Map<Long, Integer> overrides = new TreeMap<>();
        private ProjectorBatchPreview result;

        void clear() {
            template = null;
            originSettings = null;
            result = null;
        }

        ProjectorStructure get(ProjectorStructure shape, ProjectorPlan plan, ExtendedFacing nextFacing) {
            if (shape == null || shape.isEmpty() || !plan.batchProjection || plan.batch.machines() == 1) {
                clear();
                return shape;
            }
            ProjectorPlan.Settings batchSettings = plan.batchSettings();
            if (!shape.settings.equals(batchSettings)) {
                shape = ProjectorStructures.get(batchSettings);
                if (shape == null || shape.isEmpty()) {
                    clear();
                    return shape;
                }
            }
            ProjectorPlan.Settings nextOrigin = plan.settings();
            long nextKey = plan.batch.arrangementKey();
            if (shape != template || nextFacing != facing
                || nextKey != arrangement
                || !nextOrigin.equals(originSettings)
                || !overrides.equals(plan.batchOverrides)) {
                overrides.clear();
                overrides.putAll(plan.batchOverrides);
                result = expand(shape, plan, nextFacing);
                template = shape;
                originSettings = nextOrigin;
                facing = nextFacing;
                arrangement = nextKey;
            }
            return result;
        }
    }

    private static ProjectorBatchPreview expand(ProjectorStructure shape, ProjectorPlan plan, ExtendedFacing facing) {
        return expand(shape, plan, facing, new ProjectorBatch.Layout(plan, shape, facing));
    }

    static ProjectorBatchPreview expand(ProjectorStructure shape, ProjectorPlan plan, ExtendedFacing facing,
        ProjectorBatch.Layout layout) {
        int copies = plan.batch.machines();
        int[] world = layout.worldBounds();
        int[] first = new int[3], last = new int[3], bounds = new int[6];
        facing.getOffsetABC(new int[] { world[0], world[1], world[2] }, first);
        facing.getOffsetABC(new int[] { world[3], world[4], world[5] }, last);
        for (int axis = 0; axis < 3; axis++) {
            bounds[axis] = Math.min(first[axis], last[axis]);
            bounds[axis + 3] = Math.max(first[axis], last[axis]);
        }
        var formation = new Formation(copies);
        var batch = ProjectorBatchCells
            .collect(shape, plan, facing, layout, overrides -> formation.pending.merge(overrides, 1, Integer::sum));
        return new ProjectorBatchPreview(shape, batch, bounds, facing, formation, layout);
    }
}
