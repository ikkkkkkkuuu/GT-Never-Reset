package com.xyp.gtneverreset.common.projector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorBatchHatches.Sample;

import goodgenerator.blocks.regularBlock.BlockCasing;
import gregtech.api.metatileentity.implementations.MTEFrame;
import gregtech.api.util.GlassTier;
import gregtech.common.blocks.BlockCasingsAbstract;
import gregtech.common.blocks.BlockFrameBox;
import gregtech.common.blocks.BlockSheetMetal;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public final class ProjectorBatch {

    public static final int MAX_ADDITIONAL = 64;
    public static final int MAX_GAP = 64;
    private static final ForgeDirection[] POSITIVE = { ForgeDirection.EAST, ForgeDirection.UP, ForgeDirection.SOUTH };
    private final int[] additional = new int[ForgeDirection.VALID_DIRECTIONS.length];
    private final int[] gaps = new int[POSITIVE.length];
    private int shared;
    public boolean alternateVertical, faceOutward = true, copyHatches = true;

    public int additional(ForgeDirection direction) {
        return additional[direction.ordinal()];
    }

    public void setAdditional(ForgeDirection direction, int count) {
        additional[direction.ordinal()] = Math.max(0, Math.min(MAX_ADDITIONAL, count));
    }

    public int gap(ForgeDirection direction) {
        return gaps[axisOf(direction)];
    }

    public void setGap(ForgeDirection direction, int gap) {
        gaps[axisOf(direction)] = Math.max(0, Math.min(MAX_GAP, gap));
    }

    public boolean shares(ForgeDirection direction) {
        return (shared & (1 << direction.ordinal())) != 0;
    }

    public void setShared(ForgeDirection direction, boolean enabled) {
        int bit = 1 << direction.ordinal();
        shared = enabled ? shared | bit : shared & ~bit;
    }

    private boolean sharesFace(ForgeDirection direction) {
        return gap(direction) == 0 && shares(direction);
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

    public long arrangementKey() {
        // Mixed radix keeps the flags, six counts, and three gaps in one collision-free key.
        long key = shared | (alternateVertical ? 64 : 0) | (faceOutward ? 128 : 0);
        for (int count : additional) key = key * (MAX_ADDITIONAL + 1) + count;
        for (int gap : gaps) key = key * (MAX_GAP + 1) + gap;
        return copyHatches ? key : ~key;
    }

    void write(NBTTagCompound plan) {
        boolean hasGap = gaps[0] != 0 || gaps[1] != 0 || gaps[2] != 0;
        if (machines() == 1 && !hasGap && shared == 0 && !alternateVertical && faceOutward && copyHatches) return;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("Additional", additional.clone());
        if (hasGap) tag.setIntArray("Gaps", gaps.clone());
        tag.setInteger("Shared", shared);
        tag.setBoolean("AlternateVertical", alternateVertical);
        tag.setBoolean("FaceOutward", faceOutward);
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
        Arrays.fill(gaps, 0);
        for (int axis = 0; axis < Math.min(gaps.length, storedGaps.length); axis++) {
            setGap(POSITIVE[axis], storedGaps[axis]);
        }
        shared = tag.getInteger("Shared") & 63;
        alternateVertical = tag.getBoolean("AlternateVertical");
        faceOutward = !tag.hasKey("FaceOutward") || tag.getBoolean("FaceOutward");
        copyHatches = !tag.hasKey("CopyHatches") || tag.getBoolean("CopyHatches");
    }

    public static final class Layout {

        private final ProjectorBatch batch;
        private final ProjectorStructure shape;
        private final ExtendedFacing facing;
        private final ProjectorBatchHatches hatches;
        private final int origin;
        private final int[] bounds, size = new int[3];
        private final LongOpenHashSet empty;
        private static final int INVERTED = 4, MIRRORED = 8, POSE_COUNT = 16;
        private final ExtendedFacing[] poses = new ExtendedFacing[POSE_COUNT];
        private final int[][] shift = new int[POSE_COUNT][3];
        private static final int[] TURN_ORDER = { 0, 2, 1, 3 };
        private final int[] gridLength = new int[3], other = new int[3], abc = new int[3];
        private byte[] chosen;
        private final Map<Integer, int[]> preferences = new HashMap<>();
        private final Map<Integer, Boolean> joins = new HashMap<>();
        private final IntArrayList[][] faces = new IntArrayList[POSE_COUNT][6];
        private boolean solved, valid, limited;
        private int searchIndex;
        private byte[] next;
        private long trials;
        private static final long BACKTRACK_ALLOWANCE = 8192L;

        @com.github.bsideup.jabel.Desugar
        public record Placement(int x, int y, int z, ExtendedFacing facing) {}

        public Layout(ProjectorBatch batch, ProjectorStructure shape, ExtendedFacing facing) {
            this(batch, shape, facing, null);
        }

        public Layout(ProjectorPlan plan, ProjectorStructure shape, ExtendedFacing facing) {
            this(plan.batch, shape, facing, plan);
        }

        private Layout(ProjectorBatch batch, ProjectorStructure shape, ExtendedFacing facing, ProjectorPlan plan) {
            this.batch = batch;
            this.shape = shape;
            this.facing = facing;
            bounds = shape.worldBounds(facing);
            empty = new LongOpenHashSet(shape.emptyCells);
            for (long cell : shape.emptyCells) {
                abc[0] = ProjectorPlan.unpackA(cell);
                abc[1] = ProjectorPlan.unpackB(cell);
                abc[2] = ProjectorPlan.unpackC(cell);
                facing.getWorldOffset(abc, other);
                for (int axis = 0; axis < 3; axis++) {
                    bounds[axis] = Math.min(bounds[axis], other[axis]);
                    bounds[axis + 3] = Math.max(bounds[axis + 3], other[axis]);
                }
            }
            for (int axis = 0; axis < 3; axis++) {
                size[axis] = bounds[axis + 3] - bounds[axis] + 1;
                gridLength[axis] = batch.length(POSITIVE[axis]);
            }
            origin = batch.additional(ForgeDirection.WEST) + gridLength[0]
                * (batch.additional(ForgeDirection.DOWN) + gridLength[1] * batch.additional(ForgeDirection.NORTH));
            hatches = plan != null
                && (!plan.batchOverrides.isEmpty() || !batch.copyHatches && !plan.overrides.isEmpty())
                    ? new ProjectorBatchHatches(shape, plan, origin)
                    : null;
            var valid = ProjectorMachines.validFacings(shape.settings.machine());
            for (int code = 0; code < POSE_COUNT; code++) {
                ExtendedFacing target = ProjectorOrientation.rotateY(facing, code % 4, candidate -> true);
                if ((code & INVERTED) != 0) target = ProjectorOrientation.upsideDown(target, candidate -> true);
                if ((code & MIRRORED) != 0) target = ProjectorOrientation.mirrorFront(target);
                poses[code] = ProjectorOrientation.allowedEquivalent(target, valid);
                if (poses[code] == null) continue;
                int[] minimum = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
                for (int corner = 0; corner < 8; corner++) {
                    for (int k = 0; k < 3; k++) other[k] = bounds[k + ((corner & (1 << k)) == 0 ? 0 : 3)];
                    facing.getOffsetABC(other, abc);
                    poses[code].getWorldOffset(abc, other);
                    for (int k = 0; k < 3; k++) minimum[k] = Math.min(other[k], minimum[k]);
                }
                for (int k = 0; k < 3; k++) shift[code][k] = bounds[k] - minimum[k];
            }
        }

        ProjectorBatchHatches hatches() {
            return hatches;
        }

        boolean hasDistinctOrigins() {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                if (size[axisOf(dir)] == 1 && batch.sharesFace(dir) && batch.additional(dir) > 0) return false;
            }
            return true;
        }

        boolean flipSupported() {
            if (!batch.alternateVertical || gridLength[1] == 1) return true;
            if (!batch.faceOutward) return poses[INVERTED] != null;
            for (int code = INVERTED; code < POSE_COUNT; code++)
                if ((code & INVERTED) != 0 && poses[code] != null && ((code & 1) == 0 || size[0] == size[2]))
                    return true;
            return false;
        }

        private int grid(int index, int axis) {
            if (axis > 0) index /= gridLength[0];
            if (axis > 1) index /= gridLength[1];
            return index % gridLength[axis] - batch.additional(POSITIVE[axis].getOpposite());
        }

        private int offset(int step, int axis) {
            ForgeDirection side = step < 0 ? POSITIVE[axis].getOpposite() : POSITIVE[axis];
            int gap = batch.gaps[axis];
            return step * (size[axis] + gap - (gap == 0 && batch.shares(side) ? 1 : 0));
        }

        public void copyOffset(int index, int[] out) {
            for (int k = 0; k < 3; k++) out[k] = offset(grid(index, k), k);
        }

        public void copyWorldBounds(int index, int[] out) {
            copyOffset(index, out);
            for (int axis = 0; axis < 3; axis++) {
                out[axis + 3] = out[axis] + bounds[axis + 3];
                out[axis] += bounds[axis];
            }
        }

        public Placement placement(int index) {
            int gx = grid(index, 0), gy = grid(index, 1), gz = grid(index, 2);
            int code = valid ? chosen[index] : candidates(gx, gy, gz)[0];
            int[] pivot = shift[code];
            return new Placement(
                offset(gx, 0) + pivot[0],
                offset(gy, 1) + pivot[1],
                offset(gz, 2) + pivot[2],
                pose(code));
        }

        private ExtendedFacing pose(int code) {
            return poses[code];
        }

        public boolean searchLimited() {
            return limited;
        }

        // Pair compatibility depends on the two poses and their relative face, not on the
        // machine ID or the absolute world coordinates. Repeated rows reuse the same tests.
        public boolean solve() {
            advanceSearch(Long.MAX_VALUE);
            return valid;
        }

        public boolean advanceSearch(long deadline) {
            if (solved) return true;
            int count = batch.machines();
            if (next == null) {
                if (!hasDistinctOrigins() || !flipSupported()) {
                    solved = true;
                    return true;
                }
                chosen = new byte[count];
                next = new byte[count];
            }
            long budget = BACKTRACK_ALLOWANCE + 12L * count;
            do {
                int index = searchIndex;
                int[] order = candidates(grid(index, 0), grid(index, 1), grid(index, 2));
                if (next[index] == order.length) {
                    next[index] = 0;
                    searchIndex--;
                } else {
                    if (++trials > budget) {
                        limited = true;
                        break;
                    }
                    int candidate = order[next[index]++];
                    if (matchesEarlier(index, grid(index, 0), grid(index, 1), grid(index, 2), candidate))
                        chosen[searchIndex++] = (byte) candidate;
                }
            } while (searchIndex >= 0 && searchIndex < count && System.nanoTime() < deadline);
            valid = searchIndex == count;
            solved = valid || limited || searchIndex < 0;
            if (solved) next = null;
            return solved;
        }

        private int[] candidates(int gx, int gy, int gz) {
            int[] g = { gx, gy, gz };
            int exposed = 0;
            for (int i = 0; i < 3; i++) {
                if (g[i] == batch.additional(POSITIVE[i])) exposed |= 1 << POSITIVE[i].ordinal();
                if (g[i] == -batch.additional(POSITIVE[i].getOpposite())) exposed |= 1 << POSITIVE[i].getOpposite()
                    .ordinal();
            }
            boolean origin = gx == 0 && gy == 0 && gz == 0;
            int lateral = facing.getDirection().offsetX == 0 ? gx : gz;
            int key = exposed | (gy & 1) << 6 | (origin ? 128 : 0) | (lateral & 1) << 8;
            int[] order = preferences.get(key);
            if (order != null) return order;
            IntArrayList choices = new IntArrayList();
            for (int priority = 0; priority <= 1; priority++)
                for (int swap = 0; swap <= 1; swap++) for (int turn : TURN_ORDER) {
                    int preferred = (batch.alternateVertical || batch.faceOutward && hasVerticalSeam()) ? (gy & 1) : 0;
                    int layer = preferred ^ swap, code = layer * 4 + turn;
                    if (origin && code != 0 || !batch.faceOutward && turn != 0 || (turn & 1) != 0 && size[0] != size[2])
                        continue;
                    if (swap != 0 && (batch.alternateVertical || !batch.faceOutward || !hasVerticalSeam())) continue;
                    // Prefer the same horizontal controller column across vertical layers and
                    // its reflection on the adjacent lateral copy. Always try the other hand
                    // when the preferred one cannot mate with an already placed neighbour.
                    int mirrored = code | MIRRORED;
                    boolean mirrorFirst = batch.faceOutward && !origin
                        && poses[mirrored] != null
                        && (poses[code] == null || closerToColumn(mirrored, code, lateral));
                    addCandidate(choices, mirrorFirst ? mirrored : code, exposed, priority);
                    if (batch.faceOutward && !origin)
                        addCandidate(choices, mirrorFirst ? code : mirrored, exposed, priority);
                }
            order = choices.isEmpty() ? new int[] { 0 } : choices.toIntArray();
            preferences.put(key, order);
            return order;
        }

        private void addCandidate(IntArrayList choices, int code, int exposed, int priority) {
            ExtendedFacing pose = poses[code];
            if (pose == null) return;
            boolean openFront = (exposed & 1 << pose.getDirection()
                .ordinal()) != 0;
            if ((openFront ? 0 : 1) == priority) choices.add(code);
        }

        private boolean closerToColumn(int mirrored, int plain, int lateral) {
            int target = frontColumn(0) * ((lateral & 1) == 0 ? 1 : -1);
            return Math.abs(frontColumn(mirrored) - target) < Math.abs(frontColumn(plain) - target);
        }

        private int frontColumn(int code) {
            ForgeDirection front = poses[code].getDirection();
            int axis = front.offsetX == 0 ? 0 : 2;
            int sign = front.offsetX == 0 ? -front.offsetZ : front.offsetX;
            return (2 * shift[code][axis] - bounds[axis] - bounds[axis + 3]) * sign;
        }

        private boolean hasVerticalSeam() {
            return batch.sharesFace(ForgeDirection.UP) && batch.additional(ForgeDirection.UP) > 0
                || batch.sharesFace(ForgeDirection.DOWN) && batch.additional(ForgeDirection.DOWN) > 0;
        }

        private boolean overlaps(int g, int axis, int direction) {
            if (direction == 0) return true;
            ForgeDirection pos = POSITIVE[axis], neg = pos.getOpposite();
            if (direction < 0) return g > -batch.additional(neg) && batch.sharesFace(g <= 0 ? neg : pos);
            return g < batch.additional(pos) && batch.sharesFace(g < 0 ? neg : pos);
        }

        private boolean matchesEarlier(int index, int gx, int gy, int gz, int code) {
            boolean checkOrigin = hatches != null && !batch.copyHatches;
            for (int dz = -1; dz <= 0; dz++) {
                if (!overlaps(gz, 2, dz)) continue;
                for (int dy = -1; dy <= (dz < 0 ? 1 : 0); dy++) {
                    if (!overlaps(gy, 1, dy)) continue;
                    for (int dx = -1; dx <= ((dz < 0 || dy < 0) ? 1 : -1); dx++) {
                        if (!overlaps(gx, 0, dx)) continue;
                        int prev = index + dx + gridLength[0] * (dy + gridLength[1] * dz);
                        int originMask = !checkOrigin ? 0 : (index == origin ? 1 : 0) | (prev == origin ? 2 : 0);
                        if (!join(code, chosen[prev], dx, dy, dz, originMask)) return false;
                    }
                }
            }
            return true;
        }

        private boolean join(int code, int neighbor, int dx, int dy, int dz, int originMask) {
            int key = (((code * POSE_COUNT + neighbor) * 27) + 13 + dx + dy * 3 + dz * 9) * 4 + originMask;
            Boolean known = joins.get(key);
            if (known != null) return known;
            boolean result = checkJoin(code, neighbor, dx, dy, dz, originMask);
            joins.put(key, result);
            return result;
        }

        private boolean checkJoin(int code, int neighbor, int dx, int dy, int dz, int originMask) {
            int[] delta = { dx, dy, dz };
            int axis = dx != 0 ? 0 : dy != 0 ? 1 : 2;
            int face = axis * 2 + (delta[axis] > 0 ? 1 : 0);
            if (faces[code][face] == null) indexFaces(code);
            boolean touch = false;
            int[] pivot = shift[code], nextPivot = shift[neighbor];
            for (int cell : faces[code][face]) {
                projectCell(code, cell);
                boolean inside = true;
                for (int k = 0; k < 3; k++) {
                    int at = other[k] + pivot[k];
                    if (delta[k] != 0 && at != bounds[k + (delta[k] > 0 ? 3 : 0)]) {
                        inside = false;
                        break;
                    }
                    other[k] = at - delta[k] * (size[k] - 1) - nextPivot[k];
                }
                if (!inside) continue;
                pose(neighbor).getOffsetABC(other, abc);
                int n = shape.indexOf(abc[0], abc[1], abc[2]);
                Sample first = (originMask & 1) != 0 ? hatches.originEdit(cell) : null;
                boolean air = blockAt(cell, first) == Blocks.air;
                if (n < 0) {
                    if (!air && empty.contains(ProjectorPlan.pack(abc[0], abc[1], abc[2]))) return false;
                } else {
                    Sample second = (originMask & 2) != 0 ? hatches.originEdit(n) : null;
                    if (!shareable(cell, n, first, second)) return false;
                    touch |= !air;
                }
            }
            return touch || Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1;
        }

        private void projectCell(int code, int cell) {
            if (cell < shape.size) {
                abc[0] = shape.a[cell];
                abc[1] = shape.b[cell];
                abc[2] = shape.c[cell];
            } else {
                long p = shape.emptyCells[cell - shape.size];
                abc[0] = ProjectorPlan.unpackA(p);
                abc[1] = ProjectorPlan.unpackB(p);
                abc[2] = ProjectorPlan.unpackC(p);
            }
            pose(code).getWorldOffset(abc, other);
        }

        private void indexFaces(int code) {
            for (int f = 0; f < 6; f++) faces[code][f] = new IntArrayList();
            int[] pivot = shift[code];
            for (int cell = 0; cell < shape.size + shape.emptyCells.length; cell++) {
                projectCell(code, cell);
                for (int k = 0; k < 3; k++) {
                    int at = other[k] + pivot[k];
                    if (at == bounds[k]) faces[code][2 * k].add(cell);
                    if (at == bounds[k + 3]) faces[code][2 * k + 1].add(cell);
                }
            }
        }

        private Block blockAt(int index, Sample edit) {
            if (edit != null) return edit.block();
            return index >= shape.size ? Blocks.air : shape.blocks[index];
        }

        private boolean shareable(int cell, int neighbor, Sample edit, Sample neighborEdit) {
            Block block = blockAt(cell, edit);
            if (block != blockAt(neighbor, neighborEdit)) return false;
            if (block == Blocks.air) return true;
            int machine = edit == null ? shape.machines[cell] : edit.machine();
            int neighborMachine = neighborEdit == null ? shape.machines[neighbor] : neighborEdit.machine();
            if (machine != 0 || neighborMachine != 0) return machine == neighborMachine
                && (ProjectorMachines.isHatch(machine) || ProjectorMachines.prototype(machine) instanceof MTEFrame);
            int meta = metadata(cell, edit);
            if (meta != metadata(neighbor, neighborEdit)) return false;
            ItemStack first = edit == null ? shape.items[cell] : edit.item();
            ItemStack second = neighborEdit == null ? shape.items[neighbor] : neighborEdit.item();
            if (first != null && second != null
                && first.isItemEqual(second)
                && ItemStack.areItemStackTagsEqual(first, second)
                && (block instanceof BlockSheetMetal || block instanceof BlockFrameBox || isFrame(first))) return true;
            if (block.hasTileEntity(meta)) return false;
            return block instanceof BlockCasingsAbstract || block instanceof BlockCasing
                || GlassTier.getGlassBlockTier(block, meta) != null
                || casingAt(cell, first)
                || casingAt(neighbor, second);
        }

        private int metadata(int cell, Sample edit) {
            if (edit != null) return edit.meta();
            int[][] pos = shape.simulationPositions();
            return shape.world.getBlockMetadata(pos[0][cell], pos[1][cell], pos[2][cell]);
        }

        private static boolean isFrame(ItemStack stack) {
            for (int ore : OreDictionary.getOreIDs(stack)) if (OreDictionary.getOreName(ore)
                .startsWith("frameGt")) return true;
            return false;
        }

        private boolean casingAt(int cell, ItemStack item) {
            var slot = shape.slotAt(cell);
            return slot != null && slot.casing() != null
                && item != null
                && slot.casing()
                    .isItemEqual(item);
        }
    }
}
