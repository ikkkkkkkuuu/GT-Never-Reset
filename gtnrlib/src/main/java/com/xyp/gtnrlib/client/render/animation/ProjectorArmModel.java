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
    public static void renderOnArm(boolean left, boolean armored) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef((left ? 1 : -1) * MODEL_UNIT, 5.9f * MODEL_UNIT, 0);
            if (armored) GL11.glScalef(1.35f, 1, 1.35f);
            render();
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
            mesh.renderPart("cuffs");
            mesh.renderPart("body");
            mesh.renderPart("panels");
            GL11.glDisable(GL11.GL_LIGHTING);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            textures.bindTexture(OPTICS);
            mesh.renderPart("optics");
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopAttrib();
        }
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(GTNRLib.MODID, path);
    }
}
