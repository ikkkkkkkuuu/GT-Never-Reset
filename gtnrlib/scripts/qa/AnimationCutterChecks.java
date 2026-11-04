package com.xyp.gtnrlib.qa;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.HmiItemBindings;

import gregtech.api.enums.Materials;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;

/** Checks the visible blade edges and source texture ownership independently of hand separation. */
final class AnimationCutterChecks implements IItemRenderer {

    private static final int GRID = 32;
    private final int[] coverage = new int[GRID * GRID];
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private final DoubleBuffer equation = BufferUtils.createDoubleBuffer(4);
    private final Matrix4f mount = new Matrix4f();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final int zoom = mc.displayHeight / 24;
    private final int x0 = (mc.displayWidth - 16 * zoom) / 2, y0 = (mc.displayHeight - 16 * zoom) / 2;
    private final HmiItemBindings.TwoHandedTool rig;
    private final IItemRenderer gt;

    private AnimationCutterChecks(HmiItemBindings.TwoHandedTool rig, IItemRenderer gt) {
        this.rig = rig;
        this.gt = gt;
    }

    static void check(AnimationClientChecks qa, ItemStack steel, HmiItemBindings.TwoHandedTool rig, IItemRenderer gt)
        throws Exception {
        var probe = new AnimationCutterChecks(rig, gt);
        var reference = probe.view(qa, "stock", steel, .125, true, false);
        int[] gaps = new int[6];
        double[] times = { 0, .10, .125, .15, .21, .30 };
        for (int i = 0; i < times.length; i++) {
            var pixels = probe.view(qa, "steel-" + i, steel, times[i], false, false);
            gaps[i] = probe.gap(pixels);
            if (i == 2 || i == 3) qa.require(
                Arrays.equals(reference, pixels),
                "closed steel cutter matches the single GT sprite pixel-for-pixel at " + times[i]);
            for (int x = 6; x < 10; x++) {
                int center = probe.index(x * probe.zoom + probe.zoom / 2, 9 * probe.zoom + probe.zoom / 2);
                if (pixels[center] == pixels[0] || pixels[center] != reference[center])
                    throw new AssertionError("pin changed at source column " + x + ", t=" + times[i]);
            }
        }
        qa.require(
            gaps[0] > gaps[1] && gaps[1] > 0
                && gaps[2] == 0
                && gaps[3] == 0
                && gaps[4] > 0
                && gaps[4] < gaps[5]
                && gaps[5] == gaps[0],
            "actual cutting-edge gap in pixels: " + Arrays.toString(gaps));
        var gold = MetaGeneratedTool01.INSTANCE
            .getToolWithStats(IDMetaTool01.WIRECUTTER.ID, 1, Materials.Gold, Materials.Wood, null);
        var goldStock = probe.view(qa, "gold-stock", gold, .125, true, false);
        qa.require(
            !Arrays.equals(goldStock, reference)
                && Arrays.equals(goldStock, probe.view(qa, "gold-closed", gold, .125, false, false)),
            "closed gold cutter retains its own GT material and overlay");
        var tiltedStock = probe.view(qa, "angled-stock", steel, .125, true, true);
        var tiltedClosed = probe.view(qa, "angled-closed", steel, .125, false, true);
        qa.require(Arrays.equals(tiltedStock, tiltedClosed), "closed steel cutter matches GT from an oblique view");
        for (double t : new double[] { .1249, .1501 }) {
            var adjacent = probe.view(qa, "angled-" + t, steel, t, false, true);
            int changed = 0, visible = 0;
            for (int i = 0; i < adjacent.length; i++) {
                if (tiltedStock[i] != tiltedStock[0]) visible++;
                if (adjacent[i] != tiltedStock[i]) changed++;
            }
            qa.require(
                changed * 100 < visible,
                "next to blade contact at " + t
                    + " seconds, changed pixels / visible tool: "
                    + changed
                    + "/"
                    + visible);
        }
    }

    private int[] view(AnimationClientChecks qa, String name, ItemStack stack, double time, boolean stock,
        boolean angled) throws Exception {
        return qa.draw(mc, "cutter-face-" + name, .5f, () -> {
            GL11.glDisable(GL11.GL_LIGHTING);
            // Read the clip equations actually installed for every GT draw and the stationary pin's model matrix.
            Arrays.fill(coverage, 0);
            GL11.glPushMatrix();
            try {
                rig.render(stack, this, time, left -> {});
            } finally {
                GL11.glPopMatrix();
            }
            if (!stock) qa.require(
                Arrays.stream(coverage)
                    .allMatch(n -> n == 1),
                "each of 1024 source samples belongs to exactly one cutter part at " + time);
            // The native and animated sprite must go through the same projection and model matrix rounding.
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, mc.displayWidth, 0, mc.displayHeight, -4, 4);
            GL11.glTranslatef(x0, y0, 0);
            GL11.glScalef(16 * zoom, 16 * zoom, 1);
            if (angled) {
                GL11.glTranslatef(.5f, .5f, 0);
                GL11.glRotatef(-38, 1, 0, 0);
                GL11.glRotatef(12, 0, 1, 0);
                GL11.glTranslatef(-.5f, -.5f, 0);
            }
            GL11.glMultMatrix(
                new Matrix4f(mount).invert()
                    .get(buffer));
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            if (stock) {
                GL11.glLoadMatrix(mount.get(buffer));
                gt.renderItem(ItemRenderType.EQUIPPED_FIRST_PERSON, stack);
            } else rig.render(stack, gt, time, left -> {});
        });
    }

    private int gap(int[] pixels) {
        int center = 8 * zoom, y = 12 * zoom + zoom / 2;
        int l = center - 1, r = center;
        while (l >= 0 && pixels[index(l, y)] == pixels[0]) l--;
        while (r < 16 * zoom && pixels[index(r, y)] == pixels[0]) r++;
        if (l < 0 || r == 16 * zoom) throw new AssertionError("missing jaw on the scan line");
        return r - l - 1;
    }

    private int index(int x, int y) {
        return (mc.displayHeight - 1 - y0 - y) * mc.displayWidth + x0 + x;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        mount.set(buffer);
        var planes = new double[2][4];
        for (int i = 0; i < planes.length; i++) {
            int plane = GL11.GL_CLIP_PLANE0 + i;
            if (!GL11.glIsEnabled(plane)) continue;
            GL11.glGetClipPlane(plane, equation);
            for (int j = 0; j < 4; j++) planes[i][j] = equation.get(j);
        }
        var eye = new Vector3f();
        for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) {
            mount.transformPosition(eye.set((x + .5f) / GRID, (y + .5f) / GRID, 0));
            boolean inside = true;
            for (var p : planes) if (p[0] * eye.x + p[1] * eye.y + p[2] * eye.z + p[3] < 0) inside = false;
            if (inside) coverage[y * GRID + x]++;
        }
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return false;
    }
}
