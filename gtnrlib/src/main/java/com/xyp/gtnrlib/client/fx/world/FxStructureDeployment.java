package com.xyp.gtnrlib.client.fx.world;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class FxStructureDeployment {

    private static final int REVEAL_FADE_TICKS = 6;
    private static final double FLIGHT_APPROACH_TICKS = 22;
    private static final double FLIGHT_HOVER_TICKS = 26;
    private static final double FLIGHT_HEIGHT = 13;
    private static final DoubleBuffer PLANE = BufferUtils.createDoubleBuffer(4);
    private static final FloatBuffer COLOR = BufferUtils.createFloatBuffer(4);

    private final StructureDeploymentStyle style;
    private final boolean alongX;
    private final int min, max;
    private final long[] revealedAt;
    private int finished;

    public FxStructureDeployment(StructureDeploymentStyle style, boolean alongX, int min, int max) {
        this.style = style;
        this.alongX = alongX;
        this.min = min;
        this.max = max;
        revealedAt = new long[style.bandCount(max - min)];
        Arrays.fill(revealedAt, Long.MAX_VALUE);
    }

    public void progress(int finishedBands, long age) {
        int count = Math.min(revealedAt.length, finishedBands);
        while (finished < count) revealedAt[finished++] = age;
    }

    public void draw(double age, double cameraX, double cameraZ, Model model) {
        if (style == StructureDeploymentStyle.REVEAL) drawReveal(age, cameraX, cameraZ, model);
        else if (style == StructureDeploymentStyle.FLIGHT) drawFlight(age, cameraX, cameraZ, model);
    }

    private void drawReveal(double age, double cameraX, double cameraZ, Model model) {
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1, -1);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_ALPHA, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
        if (age < StructureDeploymentStyle.REVEAL_OUTLINE_TICKS) {
            float alpha;
            if (age < StructureDeploymentStyle.REVEAL_FLASH_TICKS) {
                alpha = (float) (1 - age / StructureDeploymentStyle.REVEAL_FLASH_TICKS * 0.55);
            } else {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
                GL11.glLineWidth(1.5f);
                alpha = 0.85f;
            }
            white(alpha);
            model.draw(0, 0, 0);
            return;
        }
        for (int band = 0; band < revealedAt.length; band++) {
            double fade = band < finished ? clamp((age - revealedAt[band]) / REVEAL_FADE_TICKS) : 0;
            if (fade >= 1) continue;
            white((float) (1 - fade));
            drawBand(band, cameraX, cameraZ, 0, 0, 0, model);
        }
    }

    private void drawFlight(double age, double cameraX, double cameraZ, Model model) {
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1, 1);
        for (int band = finished; band < revealedAt.length; band++) {
            double local = age - band * StructureDeploymentStyle.FLIGHT_BAND_TICKS;
            if (local < 0) continue;
            double approach = 1 - clamp(local / FLIGHT_APPROACH_TICKS);
            double settle = 1 - clamp(
                (local - FLIGHT_HOVER_TICKS) / (StructureDeploymentStyle.FLIGHT_TRAVEL_TICKS - FLIGHT_HOVER_TICKS));
            double distance = (42 + Math.min(42, max - min) + band * 2) * approach * approach;
            double height = FLIGHT_HEIGHT * settle * settle + 7 * approach;
            double drift = (band - (revealedAt.length - 1) * 0.5) * 3 * approach;
            drawBand(band, cameraX, cameraZ, alongX ? drift : -distance, height, alongX ? -distance : drift, model);
        }
    }

    private void drawBand(int band, double cameraX, double cameraZ, double x, double y, double z, Model model) {
        double offset = alongX ? x - cameraX : z - cameraZ;
        double lower = offset + StructureDeploymentStyle.bandBoundary(band, min, max, revealedAt.length);
        double upper = offset + StructureDeploymentStyle.bandBoundary(band + 1, min, max, revealedAt.length);
        clip(GL11.GL_CLIP_PLANE0, 1, -lower);
        clip(GL11.GL_CLIP_PLANE1, -1, upper);
        try {
            model.draw(x, y, z);
        } finally {
            GL11.glDisable(GL11.GL_CLIP_PLANE1);
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
        }
    }

    private void clip(int plane, int direction, double distance) {
        PLANE.clear();
        PLANE.put(alongX ? direction : 0)
            .put(0)
            .put(alongX ? 0 : direction)
            .put(distance)
            .flip();
        GL11.glClipPlane(plane, PLANE);
        GL11.glEnable(plane);
    }

    private static void white(float opacity) {
        COLOR.clear();
        COLOR.put(1)
            .put(1)
            .put(1)
            .put(opacity)
            .flip();
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, COLOR);
    }

    private static double clamp(double progress) {
        return Math.max(0, Math.min(1, progress));
    }

    @FunctionalInterface
    public interface Model {

        void draw(double x, double y, double z);
    }
}
