package com.xyp.gtnrlib.client.render.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.xyp.gtnrlib.GTNRLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ProjectorArmModel {

    private static final float MODEL_UNIT = 1f / 16;
    // Center of the outer lens cap in forearm_terminal.py.
    private static final float LENS_X = .28f * MODEL_UNIT, LENS_Y = -3.55f * MODEL_UNIT, LENS_Z = -5.72f * MODEL_UNIT;
    private static final String[] OPTICAL_GROUPS = { "display", "display_marks", "status_lamps", "lens" };
    private static final ResourceLocation GEOMETRY = resource("models/animation/projector/forearm_projector.obj");
    private static final ResourceLocation SHELL = resource("textures/animation/projector/orrery_shell_proportions.png");
    private static final ResourceLocation OPTICS = resource("textures/animation/projector/orrery_materials.png");

    private static IModelCustom mesh;

    private ProjectorArmModel() {}

    public static void watchResources() {
        var resources = Minecraft.getMinecraft()
            .getResourceManager();
        if (resources instanceof IReloadableResourceManager reloadable)
            reloadable.registerReloadListener(manager -> mesh = null);
    }

    /** The shoulder pivot and live arm rotation have already been applied. */
    public static void renderOnArm(boolean armored, float projection, boolean localView) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(-MODEL_UNIT, 5.9f * MODEL_UNIT, 0);
            if (armored) GL11.glScalef(1.35f, 1, 1.35f);
            render();
            if (projection > .01f) {
                if (localView) ProjectorHologramRenderer.captureLens(LENS_X, LENS_Y, LENS_Z, projection);
                else ProjectorHologramRenderer.drawWorldProjection(LENS_X, LENS_Y, LENS_Z, projection);
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    public static void render() {
        if (mesh == null) mesh = AdvancedModelLoader.loadModel(GEOMETRY);
        var textures = Minecraft.getMinecraft()
            .getTextureManager();
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glColor4f(1, 1, 1, 1);
            textures.bindTexture(SHELL);
            mesh.renderAllExcept(OPTICAL_GROUPS);
            GL11.glDisable(GL11.GL_LIGHTING);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            textures.bindTexture(OPTICS);
            GL11.glColor4f(.28f, .54f, .55f, 1);
            mesh.renderPart("display");
            GL11.glColor4f(.66f, .94f, .92f, 1);
            mesh.renderPart("display_marks");
            GL11.glColor4f(1, 1, 1, 1);
            mesh.renderOnly("status_lamps", "lens");
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopAttrib();
        }
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(GTNRLib.MODID, path);
    }
}
