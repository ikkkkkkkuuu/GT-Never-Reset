package com.xyp.gtnrlib.client.fx.world;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Line outlines around sets of whole blocks, drawn into the current GL coordinate space. Block positions are packed
 * with {@link #pack(int, int, int)}; each axis keeps 21 signed bits.
 * <p>
 * The {@code *Lines} methods return line vertices (x, y, z per vertex, two vertices per line) so callers can keep
 * them while the set does not change and only {@link #draw} them every frame.
 */
@SideOnly(Side.CLIENT)
public class FxVoxelOutline {

    private static final double LIFT = 0.003;

    private FxVoxelOutline() {}

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    public static int unpackX(long packed) {
        return (int) (packed << 1 >> 43);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 22 >> 43);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 43 >> 43);
    }

    /**
     * One slightly enlarged box per block, like vanilla's block highlight.
     *
     * @param blocks packed block positions
     * @return line vertices
     */
    public static float[] boxLines(LongSet blocks) {
        FloatArrayList lines = new FloatArrayList(blocks.size() * 72);
        LongIterator iterator = blocks.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);
            box(lines, x - LIFT, y - LIFT, z - LIFT, x + 1 + LIFT, y + 1 + LIFT, z + 1 + LIFT);
        }
        return lines.toFloatArray();
    }

    /**
     * The outline of the whole set as one shape: an edge is drawn only where the surface bends, so edges between two
     * touching blocks disappear.
     *
     * @param blocks packed block positions
     * @return line vertices
     */
    public static float[] mergedLines(LongSet blocks) {
        LongSet[] seen = { new LongOpenHashSet(), new LongOpenHashSet(), new LongOpenHashSet() };
        FloatArrayList lines = new FloatArrayList();
        LongIterator iterator = blocks.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);
            for (int axis = 0; axis < 3; axis++) {
                for (int i = 0; i < 4; i++) {
                    int first = i & 1, second = i >> 1;
                    int cornerX = x + offset(axis, 0, first, second), cornerY = y + offset(axis, 1, first, second),
                        cornerZ = z + offset(axis, 2, first, second);
                    if (seen[axis].add(pack(cornerX, cornerY, cornerZ)))
                        edge(lines, blocks, cornerX, cornerY, cornerZ, axis);
                }
            }
        }
        return lines.toFloatArray();
    }

    /**
     * Draws line vertices made by {@link #boxLines} or {@link #mergedLines}.
     */
    public static void draw(Tessellator tessellator, float[] lines, int rgb, int alpha) {
        if (lines.length == 0) return;
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_I(rgb, alpha);
        for (int i = 0; i < lines.length; i += 3) tessellator.addVertex(lines[i], lines[i + 1], lines[i + 2]);
        tessellator.draw();
    }

    /**
     * Draws one slightly enlarged box per block.
     */
    public static void boxes(Tessellator tessellator, LongSet blocks, int rgb, int alpha) {
        draw(tessellator, boxLines(blocks), rgb, alpha);
    }

    /**
     * Draws the outline of the whole set as one shape.
     *
     * @see #mergedLines(LongSet)
     */
    public static void merged(Tessellator tessellator, LongSet blocks, int rgb, int alpha) {
        draw(tessellator, mergedLines(blocks), rgb, alpha);
    }

    /**
     * Adds the twelve edges of a box to a tessellator that is drawing {@code GL_LINES}.
     */
    public static void box(Tessellator tessellator, double minX, double minY, double minZ, double maxX, double maxY,
        double maxZ) {
        for (int i = 0; i < 4; i++) {
            boolean low = (i & 1) == 0, near = (i >> 1) == 0;
            double y = low ? minY : maxY, z = near ? minZ : maxZ, x = low ? minX : maxX, depth = near ? minY : maxY;
            tessellator.addVertex(minX, y, z);
            tessellator.addVertex(maxX, y, z);
            tessellator.addVertex(x, minY, z);
            tessellator.addVertex(x, maxY, z);
            tessellator.addVertex(x, depth, minZ);
            tessellator.addVertex(x, depth, maxZ);
        }
    }

    private static void box(FloatArrayList lines, double minX, double minY, double minZ, double maxX, double maxY,
        double maxZ) {
        for (int i = 0; i < 4; i++) {
            boolean low = (i & 1) == 0, near = (i >> 1) == 0;
            line(lines, minX, low ? minY : maxY, near ? minZ : maxZ, maxX, low ? minY : maxY, near ? minZ : maxZ);
            line(lines, low ? minX : maxX, minY, near ? minZ : maxZ, low ? minX : maxX, maxY, near ? minZ : maxZ);
            line(lines, low ? minX : maxX, near ? minY : maxY, minZ, low ? minX : maxX, near ? minY : maxY, maxZ);
        }
    }

    private static void line(FloatArrayList lines, double x1, double y1, double z1, double x2, double y2, double z2) {
        lines.add((float) x1);
        lines.add((float) y1);
        lines.add((float) z1);
        lines.add((float) x2);
        lines.add((float) y2);
        lines.add((float) z2);
    }

    /** Offset along {@code coordinate} of an edge or cell that is {@code first}, {@code second} steps off the axis. */
    private static int offset(int axis, int coordinate, int first, int second) {
        if (coordinate == axis) return 0;
        return coordinate == (axis + 1) % 3 ? first : second;
    }

    private static void edge(FloatArrayList lines, LongSet blocks, int x, int y, int z, int axis) {
        int count = 0, shiftFirst = 0, shiftSecond = 0;
        boolean low = false, high = false;
        for (int i = 0; i < 4; i++) {
            int first = (i & 1) - 1, second = (i >> 1) - 1;
            boolean filled = blocks.contains(
                pack(
                    x + offset(axis, 0, first, second),
                    y + offset(axis, 1, first, second),
                    z + offset(axis, 2, first, second)));
            if (i == 0) low = filled;
            if (i == 3) high = filled;
            if (filled) {
                count++;
            } else {
                shiftFirst += first == 0 ? 1 : -1;
                shiftSecond += second == 0 ? 1 : -1;
            }
        }
        boolean diagonal = count == 2 && low == high;
        if (count != 1 && count != 3 && !diagonal) return;
        double liftFirst = Integer.signum(shiftFirst) * LIFT, liftSecond = Integer.signum(shiftSecond) * LIFT;
        double startX = x + lift(axis, 0, liftFirst, liftSecond), startY = y + lift(axis, 1, liftFirst, liftSecond),
            startZ = z + lift(axis, 2, liftFirst, liftSecond);
        line(
            lines,
            startX,
            startY,
            startZ,
            startX + (axis == 0 ? 1 : 0),
            startY + (axis == 1 ? 1 : 0),
            startZ + (axis == 2 ? 1 : 0));
    }

    private static double lift(int axis, int coordinate, double first, double second) {
        if (coordinate == axis) return 0;
        return coordinate == (axis + 1) % 3 ? first : second;
    }
}
