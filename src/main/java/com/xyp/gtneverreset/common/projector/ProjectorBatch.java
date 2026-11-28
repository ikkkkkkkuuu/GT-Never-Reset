package com.xyp.gtneverreset.common.projector;

import java.util.Arrays;
import java.util.BitSet;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public final class ProjectorBatch {

    public static final int MAX_ADDITIONAL = 64;
    public static final int MAX_GAP = 64;
    public static final int MAX_COPIES = 64;

    private static final ForgeDirection[] POSITIVE = { ForgeDirection.EAST, ForgeDirection.UP, ForgeDirection.SOUTH };
    private final int[] additional = new int[ForgeDirection.VALID_DIRECTIONS.length];
    private final int[] gaps = new int[POSITIVE.length];
    public boolean copyHatches = true;

    public int additional(ForgeDirection direction) {
        return additional[direction.ordinal()];
    }

    public void setAdditional(ForgeDirection direction, int count) {
        additional[direction.ordinal()] = Math.max(0, Math.min(MAX_ADDITIONAL, count));
    }

    public int signedCopies(ForgeDirection positive) {
        int added = additional(positive);
        return added > 0 || additional(positive.getOpposite()) == 0 ? 1 + added
            : -1 - additional(positive.getOpposite());
    }

    public boolean hasBothDirections(ForgeDirection positive) {
        return additional(positive) > 0 && additional(positive.getOpposite()) > 0;
    }

    public void setSignedCopies(ForgeDirection positive, int copies) {
        copies = Math.max(-MAX_COPIES, Math.min(MAX_COPIES, copies));
        setAdditional(positive, Math.max(0, copies - 1));
        setAdditional(positive.getOpposite(), Math.max(0, -copies - 1));
    }

    public int gap(ForgeDirection direction) {
        return gaps[axisOf(direction)];
    }

    public void setGap(ForgeDirection direction, int gap) {
        gaps[axisOf(direction)] = Math.max(-MAX_GAP, Math.min(MAX_GAP, gap));
    }

    private static int axisOf(ForgeDirection direction) {
        return switch (direction) {
            case EAST, WEST -> 0;
            case UP, DOWN -> 1;
            case SOUTH, NORTH -> 2;
            default -> throw new IllegalArgumentException("Unsupported axis: " + direction);
        };
    }

    private int length(ForgeDirection positive) {
        return 1 + additional(positive) + additional(positive.getOpposite());
    }

    public int machines() {
        return length(ForgeDirection.EAST) * length(ForgeDirection.UP) * length(ForgeDirection.SOUTH);
    }

    public boolean withinLimit() {
        return machines() <= MAX_COPIES;
    }

    public long arrangementKey() {
        long key = 0;
        for (int count : additional) key = key * (MAX_ADDITIONAL + 1) + count;
        for (int gap : gaps) key = key * (2 * MAX_GAP + 1) + gap + MAX_GAP;
        return copyHatches ? key : ~key;
    }

    void write(NBTTagCompound plan) {
        boolean hasGap = gaps[0] != 0 || gaps[1] != 0 || gaps[2] != 0;
        if (machines() == 1 && !hasGap && copyHatches) return;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("Additional", additional.clone());
        if (hasGap) tag.setIntArray("Gaps", gaps.clone());
        if (!copyHatches) tag.setBoolean("CopyHatches", false);
        plan.setTag("Batch", tag);
    }

    void read(NBTTagCompound plan) {
        NBTTagCompound tag = plan.getCompoundTag("Batch");
        int[] counts = tag.getIntArray("Additional");
        Arrays.fill(additional, 0);
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (dir.ordinal() < counts.length) setAdditional(dir, counts[dir.ordinal()]);
        }
        int[] storedGaps = tag.getIntArray("Gaps");
        int shared = tag.getInteger("Shared");
        for (int axis = 0; axis < gaps.length; axis++) {
            ForgeDirection positive = POSITIVE[axis], negative = positive.getOpposite();
            int gap = axis < storedGaps.length ? storedGaps[axis] : 0;
            if (gap == 0 && (sharedAt(positive, shared) || sharedAt(negative, shared))) gap = -1;
            setGap(positive, gap);
        }
        copyHatches = !tag.hasKey("CopyHatches") || tag.getBoolean("CopyHatches");
    }

    private boolean sharedAt(ForgeDirection direction, int shared) {
        return additional(direction) > 0 && (shared & 1 << direction.ordinal()) != 0;
    }

    public static final class Layout {

        private final ProjectorStructure shape;
        private final ExtendedFacing facing;
        private final ProjectorBatchHatches hatches;
        private final int[] bounds, stride = new int[3], positive = new int[3], negative = new int[3];
        private final int[] local = new int[3], world = new int[3];
        private final int nx, nz, copies, cellsPerCopy;
        private final boolean valid;
        private final BitSet superseded = new BitSet();
        private Long2IntOpenHashMap winners;
        private int next;
        private boolean resolved;

        @Desugar
        public record Placement(int x, int y, int z, ExtendedFacing facing) {}

        public Layout(ProjectorBatch batch, ProjectorStructure shape, ExtendedFacing facing) {
            this(batch, shape, facing, null);
        }

        public Layout(ProjectorPlan plan, ProjectorStructure shape, ExtendedFacing facing) {
            this(plan.batch, shape, facing, plan);
        }

        private Layout(ProjectorBatch batch, ProjectorStructure shape, ExtendedFacing facing, ProjectorPlan plan) {
            this.shape = shape;
            this.facing = facing;
            bounds = shape.worldBounds(facing);
            for (long cell : shape.emptyCells) {
                unpack(cell);
                facing.getWorldOffset(local, world);
                for (int axis = 0; axis < 3; axis++) {
                    bounds[axis] = Math.min(bounds[axis], world[axis]);
                    bounds[axis + 3] = Math.max(bounds[axis + 3], world[axis]);
                }
            }
            boolean overlap = false;
            for (int axis = 0; axis < 3; axis++) {
                ForgeDirection direction = POSITIVE[axis];
                positive[axis] = batch.additional(direction);
                negative[axis] = batch.additional(direction.getOpposite());
                int size = bounds[axis + 3] - bounds[axis] + 1;
                stride[axis] = Math.max(1, size + batch.gap(direction));
                overlap |= stride[axis] < size && positive[axis] + negative[axis] > 0;
            }
            nx = batch.length(ForgeDirection.EAST);
            nz = batch.length(ForgeDirection.SOUTH);
            copies = batch.machines();
            cellsPerCopy = shape.size + shape.emptyCells.length;
            valid = batch.withinLimit();
            resolved = !valid || !overlap || cellsPerCopy == 0;
            if (!resolved) winners = new Long2IntOpenHashMap();
            hatches = plan != null
                && (!plan.batchOverrides.isEmpty() || !batch.copyHatches && !plan.overrides.isEmpty())
                    ? new ProjectorBatchHatches(shape, plan, 0)
                    : null;
        }

        ProjectorBatchHatches hatches() {
            return hatches;
        }

        private int offset(int index, int axis) {
            int step = index <= positive[axis] ? index : positive[axis] - index;
            return step * stride[axis];
        }

        public void copyOffset(int index, int[] out) {
            out[0] = offset(index % nx, 0);
            out[1] = offset(index / nx / nz, 1);
            out[2] = offset(index / nx % nz, 2);
        }

        public void copyWorldBounds(int index, int[] out) {
            copyOffset(index, out);
            for (int axis = 0; axis < 3; axis++) {
                out[axis + 3] = out[axis] + bounds[axis + 3];
                out[axis] += bounds[axis];
            }
        }

        public int[] worldBounds() {
            int[] all = bounds.clone();
            for (int axis = 0; axis < 3; axis++) {
                all[axis] -= negative[axis] * stride[axis];
                all[axis + 3] += positive[axis] * stride[axis];
            }
            return all;
        }

        public Placement placement(int index) {
            return new Placement(offset(index % nx, 0), offset(index / nx / nz, 1), offset(index / nx % nz, 2), facing);
        }

        public boolean solve() {
            advanceSearch(Long.MAX_VALUE);
            return valid;
        }

        public boolean advanceSearch(long deadline) {
            if (resolved) return true;
            int total = copies * cellsPerCopy;
            while (next < total && System.nanoTime() < deadline) {
                int copy = next / cellsPerCopy, cell = next % cellsPerCopy;
                long position = position(copy, cell);
                boolean air = isAir(position, copy, cell);
                int previous = winners.get(position);
                if (previous == 0) {
                    winners.put(position, air ? -(next + 1) : next + 1);
                } else if (previous < 0 && !air) {
                    superseded.set(-previous - 1);
                    winners.put(position, next + 1);
                } else {
                    superseded.set(next);
                }
                next++;
            }
            if (next != total) return false;
            resolved = true;
            winners = null;
            return true;
        }

        public boolean ownsCell(int copy, int cell) {
            return !superseded.get(copy * cellsPerCopy + cell);
        }

        private boolean isAir(long position, int copy, int cell) {
            if (cell >= shape.size) return true;
            return hatches == null ? shape.blocks[cell] == Blocks.air : hatches.isAir(position, cell, copy);
        }

        private long position(int copy, int cell) {
            if (cell < shape.size) {
                local[0] = shape.a[cell];
                local[1] = shape.b[cell];
                local[2] = shape.c[cell];
            } else {
                unpack(shape.emptyCells[cell - shape.size]);
            }
            facing.getWorldOffset(local, world);
            world[0] += offset(copy % nx, 0);
            world[1] += offset(copy / nx / nz, 1);
            world[2] += offset(copy / nx % nz, 2);
            facing.getOffsetABC(world, local);
            return ProjectorPlan.pack(local[0], local[1], local[2]);
        }

        private void unpack(long cell) {
            local[0] = ProjectorPlan.unpackA(cell);
            local[1] = ProjectorPlan.unpackB(cell);
            local[2] = ProjectorPlan.unpackC(cell);
        }
    }
}
