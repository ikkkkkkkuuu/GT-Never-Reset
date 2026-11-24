package com.xyp.gtnrlib.client.fx.world;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class FxStructureDeployment {

    private static final int FINISH_GRACE_TICKS = 8;
    private static final int REVEAL_FADE_TICKS = 6;
    private static final double FLIGHT_APPROACH_TICKS = 22;
    private static final double FLIGHT_HOVER_TICKS = 26;
    private static final double FLIGHT_HEIGHT = 13;
    private static final DoubleBuffer PLANE = BufferUtils.createDoubleBuffer(4);
    private static final FloatBuffer COLOR = BufferUtils.createFloatBuffer(4);

    private final World world;
    private final StructureDeploymentStyle style;
    private final StructureDeployment bounds;
    private final FxSummoningDeployment summoning;
    private final boolean alongX;
    private final int min, max;
    private final long[] revealedAt;
    private long finishedTick = -1;
    private int finished;

    public FxStructureDeployment(World world, StructureDeploymentStyle style, StructureDeployment bounds) {
        this.world = world;
        this.style = style;
        this.bounds = bounds;
        summoning = style == StructureDeploymentStyle.SUMMON ? new FxSummoningDeployment(world, bounds) : null;
        alongX = bounds.bandsAlongX();
        min = bounds.bandMin();
        max = bounds.bandMax();
        revealedAt = style.segmented() ? new long[bounds.bandCount(style)] : new long[0];
        Arrays.fill(revealedAt, Long.MAX_VALUE);
    }

    public void impact() {
        if (summoning != null) summoning.impact();
    }

    public void progress(int finishedBands) {
        int count = Math.min(revealedAt.length, finishedBands);
        long age = age();
        while (finished < count) revealedAt[finished++] = age;
    }

    public void complete() {
        finishedTick = world.getTotalWorldTime();
    }

    /** Returns whether a summoning effect has a landing animation left to play. */
    public boolean cancel() {
        if (summoning == null || !summoning.hasImpact()) return false;
        complete();
        return true;
    }

    public boolean expired() {
        return finishedTick >= 0 && world.getTotalWorldTime() - finishedTick >= FINISH_GRACE_TICKS
            && (summoning == null || summoning.finished(age()));
    }

    public void close() {
        if (summoning != null) summoning.close();
    }

    public void tick(Minecraft minecraft) {
        if (summoning != null) summoning.tick(minecraft, age());
    }

    public static void shakeCamera(float partial) {
        FxStructureImpact.shakeCamera(partial);
    }

    public void render(Minecraft minecraft, float partial, double cameraX, double cameraY, double cameraZ,
        FxBlockMesh mesh, long compileBudgetNanos, Model model) {
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_POLYGON_BIT
                | GL11.GL_LINE_BIT
                | GL11.GL_TRANSFORM_BIT
                | GL11.GL_TEXTURE_BIT);
        try {
            if (mesh != null && compileBudgetNanos > 0) mesh.compile(compileBudgetNanos);
            double age = age() + partial;
            if (age < 0) return;
            if (model != null) prepareStructureRendering(minecraft);
            switch (style) {
                case SUMMON -> summoning.draw(age, cameraX, cameraY, cameraZ, model);
                case REVEAL -> {
                    if (model != null) drawReveal(age, cameraX, cameraZ, model);
                }
                case FLIGHT -> {
                    if (model != null) drawFlight(age, cameraX, cameraZ, model);
                }
                case NONE -> {}
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private long age() {
        return world.getTotalWorldTime() - bounds.startTick();
    }

    private static void prepareStructureRendering(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glDepthMask(true);
        GL11.glColor4f(1, 1, 1, 1);
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
