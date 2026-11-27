package com.xyp.gtnrlib.client.fx.world;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.utils.Vec3d;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class FxDeploymentShaderWarmup {

    private static final int TARGET_SIZE = 16;

    private FxDeploymentShaderWarmup() {}

    static void prepare() {
        if (!OpenGlHelper.shadersSupported) return;
        long started = System.nanoTime();
        boolean plasma = FxSingularityDeployment.preparePlasmaShader();
        boolean refraction = FxBackdropRefraction.prepareShader();
        boolean drawn = false;
        if ((plasma || refraction) && OpenGlHelper.isFramebufferEnabled()) {
            try {
                drawn = drawOffscreen(plasma, refraction);
            } catch (RuntimeException failure) {
                GTNRLib.LOG.warn("deployment shaders could not be drawn offscreen", failure);
            }
        }
        GTNRLib.LOG.info(
            "prepared structure deployment shaders in {} ms (plasma: {}, refraction: {}, offscreen draw: {})",
            (System.nanoTime() - started) / 1_000_000,
            plasma,
            refraction,
            drawn);
    }

    private static boolean drawOffscreen(boolean plasma, boolean refraction) {
        var capabilities = GLContext.getCapabilities();
        boolean separate = capabilities.OpenGL30 || capabilities.GL_ARB_framebuffer_object;
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = separate ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) : draw;
        int renderbuffer = GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        Framebuffer target = null;
        try {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            target = new Framebuffer(TARGET_SIZE, TARGET_SIZE, true);
            target.bindFramebuffer(true);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(-1, 1, -1, 1, .1, 4);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glTranslated(0, 0, -2);
            FxSingularityDeployment.prepareEffects();
            boolean drewRefraction = false;
            if (refraction) {
                try (var lens = new FxBackdropRefraction()) {
                    drewRefraction = lens.draw(Vec3d.ZERO, new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), .82, .64f);
                }
            }
            if (plasma) FxSingularityDeployment.drawPlasmaSurface(22, .54);
            GL11.glFinish();
            return !refraction || drewRefraction;
        } finally {
            if (target != null) target.deleteFramebuffer();
            if (separate) {
                ARBFramebufferObject.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
                ARBFramebufferObject.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            } else {
                OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, draw);
            }
            OpenGlHelper.func_153176_h(OpenGlHelper.field_153199_f, renderbuffer);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(activeTexture);
            GL11.glMatrixMode(matrixMode);
        }
    }
}
