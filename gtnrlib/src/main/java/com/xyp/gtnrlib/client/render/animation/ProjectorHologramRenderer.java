package com.xyp.gtnrlib.client.render.animation;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Draws the wrist-to-screen projection behind the existing interactive screen. */
@SideOnly(Side.CLIENT)
public final class ProjectorHologramRenderer {

    private static final int ATTRIBUTES = GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT
        | GL11.GL_DEPTH_BUFFER_BIT
        | GL11.GL_COLOR_BUFFER_BIT
        | GL11.GL_LINE_BIT;
    // Biped right-arm coordinates relative to its shoulder pivot.
    private static final float WRIST_X = -1f / 16, WRIST_Y = 8f / 16, WRIST_Z = -2.25f / 16;
    private static final FloatBuffer MODEL = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer LENS = BufferUtils.createFloatBuffer(3);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static float lensX, lensY, intensity;
    private static boolean lensVisible;

    private ProjectorHologramRenderer() {}

    public static void beginFrame() {
        lensVisible = false;
    }

    public static void renderFromWrist(float strength, boolean localView) {
        if (localView) captureLens(WRIST_X, WRIST_Y, WRIST_Z, strength);
        else drawWorldProjection(WRIST_X, WRIST_Y, WRIST_Z, strength);
    }

    private static void captureLens(float x, float y, float z, float strength) {
        MODEL.clear();
        PROJECTION.clear();
        VIEWPORT.clear();
        LENS.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        if (!GLU.gluProject(x, y, z, MODEL, PROJECTION, VIEWPORT, LENS) || LENS.get(2) <= 0
            || LENS.get(2) >= 1
            || VIEWPORT.get(2) <= 0
            || VIEWPORT.get(3) <= 0) return;
        lensX = (LENS.get(0) - VIEWPORT.get(0)) / VIEWPORT.get(2);
        lensY = 1 - (LENS.get(1) - VIEWPORT.get(1)) / VIEWPORT.get(3);
        intensity = strength;
        lensVisible = lensX > 0 && lensX < 1 && lensY > 0 && lensY < 1;
    }

    public static void drawBeam(int screenWidth, int screenHeight, int x, int y, int width, int height) {
        if (!lensVisible || width <= 0 || height <= 0) return;
        float sourceX = lensX * screenWidth;
        float sourceY = lensY * screenHeight;
        int right = x + width, bottom = y + height;
        float edgeX1, edgeY1, edgeX2, edgeY2;
        if (sourceY > bottom || sourceY < y) {
            edgeY1 = edgeY2 = sourceY > bottom ? bottom : y;
            float center = Math.min(right - width * .22f, Math.max(x + width * .22f, sourceX));
            edgeX1 = center - width * .21f;
            edgeX2 = center + width * .21f;
        } else if (sourceX > right || sourceX < x) {
            edgeX1 = edgeX2 = sourceX > right ? right : x;
            float center = Math.min(bottom - height * .20f, Math.max(y + height * .20f, sourceY));
            edgeY1 = center - height * .18f;
            edgeY2 = center + height * .18f;
        } else {
            // A panel can obscure the device completely at narrow screen sizes.
            return;
        }
        int alpha = (int) (111 * intensity);
        GL11.glPushAttrib(ATTRIBUTES);
        try {
            projectionState();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            var tess = Tessellator.instance;
            tess.startDrawing(GL11.GL_TRIANGLES);
            tess.setColorRGBA_I(0x63e6fb, alpha);
            tess.addVertex(sourceX, sourceY, 0);
            tess.setColorRGBA_I(0x38c9ed, (int) (28 * intensity));
            tess.addVertex(edgeX1, edgeY1, 0);
            tess.addVertex(edgeX2, edgeY2, 0);
            tess.draw();
            GL11.glLineWidth(1.5f);
            tess.startDrawing(GL11.GL_LINES);
            tess.setColorRGBA_I(0x6df3ff, alpha + alpha / 3);
            tess.addVertex(sourceX, sourceY, 0);
            tess.setColorRGBA_I(0x50d3f9, alpha / 2);
            tess.addVertex(edgeX2, edgeY2, 0);
            tess.setColorRGBA_I(0x6df3ff, alpha);
            tess.addVertex(sourceX, sourceY, 0);
            tess.setColorRGBA_I(0x50d3f9, alpha / 3);
            tess.addVertex(edgeX1, edgeY1, 0);
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void drawWorldProjection(float x, float y, float z, float strength) {
        float distance = .53f * strength;
        float front = z - distance;
        float width = .47f * strength, height = .24f * strength;
        float bottom = y - .13f, top = bottom + height;
        float left = x - width / 2, right = x + width / 2;
        GL11.glPushAttrib(ATTRIBUTES);
        try {
            projectionState();
            var tess = Tessellator.instance;
            tess.startDrawing(GL11.GL_TRIANGLES);
            tess.setColorRGBA_I(0x39d9ff, (int) (52 * strength));
            tess.addVertex(x, y, z);
            tess.setColorRGBA_I(0x72dff7, (int) (20 * strength));
            tess.addVertex(left, bottom, front);
            tess.addVertex(right, bottom, front);
            tess.draw();
            tess.startDrawingQuads();
            tess.setColorRGBA_I(0x63cadd, (int) (29 * strength));
            tess.addVertex(left, bottom, front);
            tess.addVertex(right, bottom, front);
            tess.addVertex(right, top, front);
            tess.addVertex(left, top, front);
            tess.draw();
            GL11.glLineWidth(1);
            tess.startDrawing(GL11.GL_LINE_LOOP);
            tess.setColorRGBA_I(0x62e6ff, (int) (177 * strength));
            tess.addVertex(left, bottom, front);
            tess.addVertex(right, bottom, front);
            tess.addVertex(right, top, front);
            tess.addVertex(left, top, front);
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void projectionState() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
    }
}
