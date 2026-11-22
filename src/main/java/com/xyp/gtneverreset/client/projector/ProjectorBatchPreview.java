package com.xyp.gtneverreset.client.projector;

import java.util.ArrayList;
import java.util.IdentityHashMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulationWorld;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.client.fx.world.FxBlockMesh;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

@SideOnly(Side.CLIENT)
final class ProjectorBatchPreview extends ProjectorStructure {

    final ProjectorStructure template;
    final boolean outlineOnly;
    final int[] originals;
    final ExtendedFacing[] copyPoses;
    final ExtendedFacing baseFacing;
    final boolean compatible;
    final boolean limited;
    final int blockedControllers;

    private ProjectorBatchPreview(ProjectorStructure template, Long2IntLinkedOpenHashMap cells, long[] empty,
        int[] bounds, ExtendedFacing baseFacing, boolean compatible, boolean limited) {
        super(template, cells.size(), empty, bounds, new PreviewWorld());
        this.template = template;
        this.baseFacing = baseFacing;
        this.compatible = compatible;
        this.limited = limited;
        copyPoses = new ExtendedFacing[size];
        ((PreviewWorld) world).array = this;
        outlineOnly = cells.isEmpty();
        originals = new int[size];
        var totals = new IdentityHashMap<ItemStack, Integer>();
        int i = 0;
        for (var it = Long2IntMaps.fastIterator(cells); it.hasNext(); i++) {
            var entry = it.next();
            long pos = entry.getLongKey();
            int src = entry.getIntValue() % template.size;
            copyPoses[i] = ExtendedFacing.byIndex(entry.getIntValue() / template.size);
            originals[i] = src;
            if (template.items[src] != null) totals.merge(template.items[src], 1, Integer::sum);
            set(
                i,
                ProjectorPlan.unpackA(pos),
                ProjectorPlan.unpackB(pos),
                ProjectorPlan.unpackC(pos),
                template.blocks[src],
                template.metas[src],
                template.machines[src],
                template.slots[src],
                template.choiceOf[src],
                template.items[src]);
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
        materials = new ArrayList<>();
        for (Material entry : template.materials) {
            Integer count = totals.get(entry.stack());
            if (count != null) materials.add(new Material(entry.stack(), count));
        }
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
            return src < 0 ? Blocks.air : array.template.blocks[src];
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            int cell = cellAt(x, y, z);
            int src = cell < 0 ? -1 : array.originals[cell];
            if (src < 0) return 0;
            int[][] pos = array.template.simulationPositions();
            return array.template.world.getBlockMetadata(pos[0][src], pos[1][src], pos[2][src]);
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            int cell = cellAt(x, y, z);
            int src = cell < 0 ? -1 : array.originals[cell];
            if (src < 0) return null;
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
        private ExtendedFacing facing;
        private long arrangement;
        private ProjectorBatchPreview result;

        void clear() {
            template = null;
            result = null;
        }

        ProjectorStructure get(ProjectorStructure shape, ProjectorPlan plan, ExtendedFacing nextFacing) {
            if (shape == null || shape.isEmpty() || !plan.batchProjection || plan.batch.machines() == 1) {
                clear();
                return shape;
            }
            long nextKey = plan.batch.arrangementKey();
            if (shape != template || nextFacing != facing || nextKey != arrangement) {
                result = expand(shape, plan, nextFacing);
                template = shape;
                facing = nextFacing;
                arrangement = nextKey;
            }
            return result;
        }
    }

    private static ProjectorBatchPreview expand(ProjectorStructure shape, ProjectorPlan plan, ExtendedFacing facing) {
        ProjectorBatch.Layout layout = new ProjectorBatch.Layout(plan.batch, shape, facing);
        int copies = plan.batch.machines();
        int[] world = new int[3], first = new int[3], last = new int[3];
        layout.copyOffset(0, world);
        facing.getOffsetABC(world, first);
        layout.copyOffset(copies - 1, world);
        facing.getOffsetABC(world, last);
        int[] bounds = { shape.minA, shape.minB, shape.minC, shape.maxA, shape.maxB, shape.maxC };
        for (int axis = 0; axis < 3; axis++) {
            bounds[axis] += Math.min(first[axis], last[axis]);
            bounds[axis + 3] += Math.max(first[axis], last[axis]);
        }
        var cells = new Long2IntLinkedOpenHashMap();
        if ((long) copies * (shape.size + shape.emptyCells.length) > FxBlockMesh.MAX_BLOCKS)
            return new ProjectorBatchPreview(shape, cells, new long[0], bounds, facing, true, false);
        cells.defaultReturnValue(-1);
        var empty = new LongOpenHashSet();
        int[] abc = new int[3];
        boolean compatible = layout.solve();
        int[] source = new int[3];
        for (int copy = 0; copy < copies; copy++) {
            var at = layout.placement(copy);
            for (int i = 0; i < shape.size + shape.emptyCells.length; i++) {
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
        }
        empty.removeAll(cells.keySet());
        return new ProjectorBatchPreview(
            shape,
            cells,
            empty.toLongArray(),
            bounds,
            facing,
            compatible,
            layout.searchLimited());
    }
}
