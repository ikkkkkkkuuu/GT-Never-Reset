package com.xyp.gtnrlib.client.fx.world;

import java.nio.DoubleBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.animation.StructureDeployment;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class FxSummoningDeployment {

    private static final double CIRCLE_DEGREES_PER_TICK = 2.4;
    private static final DoubleBuffer PORTAL_PLANE = BufferUtils.createDoubleBuffer(4);

    private final World world;
    private final StructureDeployment bounds;
    private FxStructureImpact impact;
    private boolean opened;

    FxSummoningDeployment(World world, StructureDeployment bounds) {
        this.world = world;
        this.bounds = bounds;
    }

    void impact() {
        if (impact == null) impact = new FxStructureImpact(world, bounds, world.getTotalWorldTime());
    }

    boolean hasImpact() {
        return impact != null;
    }

    void tick(Minecraft minecraft, long age) {
        if (!opened && age >= 0 && age < StructureDeployment.OPEN_TICKS) {
            opened = true;
            world.playSound(bounds.centerX(), bounds.openingY(), bounds.centerZ(), "random.orb", 0.65f, 0.45f, false);
        }
        if (impact != null) impact.tick(minecraft);
    }

    boolean finished(long age) {
        return age >= StructureDeployment.IMPACT_TICK + StructureDeployment.CLOSE_TICKS
            && (impact == null || impact.age() >= FxStructureImpact.TAIL_TICKS);
    }

    void close() {
        if (impact != null) impact.close();
    }

    void draw(double age, double cameraX, double cameraY, double cameraZ, FxStructureDeployment.Model model) {
        if (impact != null) age = Math.max(age, StructureDeployment.IMPACT_TICK);
        if (age >= StructureDeployment.OPEN_TICKS && model != null) drawModel(age, cameraY, model);
        float scale = bounds.circleScale(age);
        if (scale > 0) drawCircle(age, scale, cameraX, cameraY, cameraZ);
    }

    private void drawModel(double age, double cameraY, FxStructureDeployment.Model model) {
        if (age < StructureDeployment.IMPACT_TICK) {
            PORTAL_PLANE.clear();
            PORTAL_PLANE.put(0)
                .put(-1)
                .put(0)
                .put(bounds.openingY() - cameraY)
                .flip();
            GL11.glClipPlane(GL11.GL_CLIP_PLANE0, PORTAL_PLANE);
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        } else {
            // Real blocks have already been rendered by the terrain pass at the landing position.
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1, 1);
        }
        try {
            model.draw(0, bounds.verticalOffset(age), 0);
        } finally {
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }

    private void drawCircle(double age, float scale, double cameraX, double cameraY, double cameraZ) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(bounds.centerX() - cameraX, bounds.openingY() - cameraY, bounds.centerZ() - cameraZ);
            GL11.glScaled(bounds.radius() * scale, 1, bounds.radius() * scale);
            GL11.glRotated(age * CIRCLE_DEGREES_PER_TICK % 360, 0, 1, 0);
            FxSummoningCircle.draw();
        } finally {
            GL11.glPopMatrix();
        }
    }
}
