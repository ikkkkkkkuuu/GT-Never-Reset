package com.xyp.gtnrlib.client.fx.world;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
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
    private static final int ARRIVAL_FLASH_TICKS = 9;
    private static final double WIREFRAME_FADE_IN_TICKS = 3;
    private static final double SOLID_FORM_FRACTION = .75;
    private static final int SINGULARITY_IMPACT_SHAKE_TICKS = 12;
    private static final double SINGULARITY_SHAKE_RANGE = 48;
    private static final double FLIGHT_APPROACH_TICKS = 22;
    private static final double FLIGHT_HOVER_TICKS = 26;
    private static final double FLIGHT_HEIGHT = 13;
    private static final DoubleBuffer PLANE = BufferUtils.createDoubleBuffer(4);
    private static final FloatBuffer COLOR = BufferUtils.createFloatBuffer(4);
    private static final Set<FxStructureDeployment> SINGULARITY_SHAKES = new LinkedHashSet<>();

    private final World world;
    private final StructureDeploymentStyle style;
    private final StructureDeployment bounds;
    private final FxSummoningDeployment summoning;
    private final FxSingularityDeployment singularity;
    private final boolean alongX;
    private final int min, max;
    private final long[] revealedAt;
    private long finishedTick = -1;
    private int finished;

    public FxStructureDeployment(World world, StructureDeploymentStyle style, StructureDeployment bounds) {
        this(world, style, bounds, null);
    }

    public FxStructureDeployment(World world, StructureDeploymentStyle style, StructureDeployment bounds,
        StructureDeployment.Launch launch) {
        this.world = world;
        this.style = style;
        this.bounds = bounds;
        summoning = style == StructureDeploymentStyle.SUMMON ? new FxSummoningDeployment(world, bounds) : null;
        singularity = style == StructureDeploymentStyle.SINGULARITY ? new FxSingularityDeployment(world, bounds, launch)
            : null;
        alongX = bounds.bandsAlongX();
        min = bounds.bandMin();
        max = bounds.bandMax();
        revealedAt = style.segmented() ? new long[bounds.bandCount(style)] : new long[0];
        Arrays.fill(revealedAt, Long.MAX_VALUE);
    }

    public void impact() {
        impact(world.getTotalWorldTime());
    }

    public void impact(long worldTick) {
        if (summoning != null) summoning.impact();
        if (singularity != null) singularity.impact(worldTick < 0 ? world.getTotalWorldTime() : worldTick);
    }

    public void progress(int finishedBands) {
        int count = Math.min(revealedAt.length, finishedBands);
        long age = age();
        while (finished < count) revealedAt[finished++] = age;
    }

    public void complete() {
        finishedTick = world.getTotalWorldTime();
    }

    /** Returns whether an effect has a confirmed impact animation left to play. */
    public boolean cancel() {
        if ((summoning == null || !summoning.hasImpact()) && (singularity == null || !singularity.hasImpact()))
            return false;
        complete();
        return true;
    }

    public boolean expired() {
        return finishedTick >= 0 && world.getTotalWorldTime() - finishedTick >= FINISH_GRACE_TICKS
            && (summoning == null || summoning.finished(age()))
            && (singularity == null || singularity.finished(age()));
    }

    public void close() {
        if (summoning != null) summoning.close();
        if (singularity != null) {
            SINGULARITY_SHAKES.remove(this);
            singularity.close();
        }
    }

    public void tick(Minecraft minecraft) {
        if (summoning != null) summoning.tick(minecraft, age());
        if (singularity != null) {
            SINGULARITY_SHAKES.add(this);
            singularity.tick(age());
        }
    }

    public static void shakeCamera(float partial) {
        FxStructureImpact.shakeCamera(partial);
        if (SINGULARITY_SHAKES.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity camera = minecraft.renderViewEntity;
        double strength = 0;
        var effects = SINGULARITY_SHAKES.iterator();
        while (effects.hasNext()) {
            FxStructureDeployment effect = effects.next();
            if (effect.world != minecraft.theWorld || effect.expired()) effects.remove();
            else if (camera != null) strength = Math.max(strength, effect.singularityShake(camera, partial));
        }
        if (strength <= 0) return;
        double phase = minecraft.theWorld.getTotalWorldTime() + partial;
        GL11.glRotatef((float) (strength * Math.sin(phase * 2.1)), 1, 0, 0);
        GL11.glRotatef((float) (strength * .65 * Math.cos(phase * 1.63)), 0, 0, 1);
        GL11.glRotatef((float) (strength * .36 * Math.sin(phase * 1.33)), 0, 1, 0);
    }

    private double singularityShake(Entity camera, float partial) {
        double age = age() + partial;
        double growth = shakePulse(
            age,
            StructureDeploymentStyle.SINGULARITY_CHARGE_TICKS,
            StructureDeploymentStyle.SINGULARITY_GROWTH_TICKS);
        double collapse = shakePulse(
            age,
            StructureDeploymentStyle.SINGULARITY_COLLAPSE_TICK,
            StructureDeploymentStyle.SINGULARITY_FORM_TICK);
        double impactAge = singularity.impactAge(age);
        double impact = impactAge >= 0 ? 1 - clamp(impactAge / SINGULARITY_IMPACT_SHAKE_TICKS) : 0;
        double amplitude = Math.max(1.2 * growth, Math.max(2.6 * collapse, 2.8 * impact * impact));
        if (amplitude == 0) return 0;

        double x = camera.prevPosX + (camera.posX - camera.prevPosX) * partial;
        double y = camera.prevPosY + (camera.posY - camera.prevPosY) * partial;
        double z = camera.prevPosZ + (camera.posZ - camera.prevPosZ) * partial;
        double dx = x - MathHelper.clamp_double(x, bounds.minX(), bounds.maxX());
        double dy = y - MathHelper.clamp_double(y, bounds.minY(), bounds.maxY());
        double dz = z - MathHelper.clamp_double(z, bounds.minZ(), bounds.maxZ());
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double range = Math.min(100, SINGULARITY_SHAKE_RANGE + bounds.radius() + (bounds.maxY() - bounds.minY()) * .35);
        return amplitude * (1 - clamp(distance / range));
    }

    private static double shakePulse(double age, int start, int end) {
        if (age <= start || age >= end) return 0;
        return Math.sin(Math.PI * (age - start) / (end - start));
    }

    public void render(Minecraft minecraft, float partial, double cameraX, double cameraY, double cameraZ,
        FxBlockMesh mesh, long compileBudgetNanos, Model model) {
        render(minecraft, partial, cameraX, cameraY, cameraZ, mesh, compileBudgetNanos, model, null);
    }

    public void render(Minecraft minecraft, float partial, double cameraX, double cameraY, double cameraZ,
        FxBlockMesh mesh, long compileBudgetNanos, Model model, Outline outline) {
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_POLYGON_BIT
                | GL11.GL_LINE_BIT
                | GL11.GL_POINT_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_TRANSFORM_BIT
                | GL11.GL_TEXTURE_BIT);
        try {
            if (mesh != null && compileBudgetNanos > 0) mesh.compile(compileBudgetNanos);
            double age = age() + partial;
            if (age < 0 && singularity == null) return;
            if (model != null && singularity == null) prepareStructureRendering(minecraft);
            switch (style) {
                case SUMMON -> summoning.draw(age, cameraX, cameraY, cameraZ, model);
                case REVEAL -> {
                    if (model != null) drawReveal(age, cameraX, cameraZ, model);
                }
                case FLIGHT -> {
                    if (model != null) drawFlight(age, cameraX, cameraZ, model);
                }
                case SINGULARITY -> {
                    singularity.draw(age, cameraX, cameraY, cameraZ);
                    if (model != null
                        && age >= StructureDeploymentStyle.SINGULARITY_FORM_TICK - WIREFRAME_FADE_IN_TICKS) {
                        double impactAge = singularity.impactAge(age);
                        if (impactAge < 0) drawCharging(minecraft, age, model, outline);
                        else drawArrival(minecraft, impactAge, model);
                    }
                    singularity.drawForegroundStar(age, cameraX, cameraY, cameraZ);
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
        // Machine face overlays share their depth with the casing beneath them.
        GL11.glDepthFunc(GL11.GL_LEQUAL);
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
        useTextureTint();
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
            textureTint(1, 1, 1, alpha);
            model.draw(0, 0, 0);
            return;
        }
        for (int band = 0; band < revealedAt.length; band++) {
            double fade = band < finished ? clamp((age - revealedAt[band]) / REVEAL_FADE_TICKS) : 0;
            if (fade >= 1) continue;
            textureTint(1, 1, 1, (float) (1 - fade));
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

    private static void drawCharging(Minecraft minecraft, double age, Model model, Outline outline) {
        double formAge = age - StructureDeploymentStyle.SINGULARITY_FORM_TICK;
        double edges = clamp(1 + formAge / WIREFRAME_FADE_IN_TICKS);
        double formDuration = StructureDeploymentStyle.SINGULARITY_IMPACT_TICK
            - StructureDeploymentStyle.SINGULARITY_FORM_TICK;
        double charge = clamp(formAge / (formDuration * SOLID_FORM_FRACTION));
        double formed = charge * (2 - charge);
        prepareStructureRendering(minecraft);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1, -2);
        GL11.glDepthMask(formed >= .85);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glAlphaFunc(GL11.GL_GREATER, .01f);
        useTextureTint();
        textureTint(
            (float) (.62 + .37 * formed),
            (float) (.84 + .16 * formed),
            1,
            (float) (.06 * edges + .94 * formed));
        model.draw(0, 0, 0);
        int opacity = (int) ((.75 - .35 * formed) * edges * 255);
        if (outline != null && opacity > 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDepthMask(false);
            GL11.glLineWidth(1.5f);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            outline.draw(0x99E3FF, opacity);
        }
    }

    private static void drawArrival(Minecraft minecraft, double age, Model model) {
        if (age < 0 || age >= ARRIVAL_FLASH_TICKS) return;
        prepareStructureRendering(minecraft);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1, -3);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glAlphaFunc(GL11.GL_GREATER, .01f);
        useTextureTint();
        double fade = 1 - age / ARRIVAL_FLASH_TICKS;
        textureTint(.62f, .83f, 1, (float) (.82 * fade * fade));
        model.draw(0, 0, 0);
    }

    private static void useTextureTint() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_ALPHA, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
    }

    private static void textureTint(float red, float green, float blue, float opacity) {
        COLOR.clear();
        COLOR.put(red)
            .put(green)
            .put(blue)
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

    @FunctionalInterface
    public interface Outline {

        void draw(int rgb, int alpha);
    }
}
