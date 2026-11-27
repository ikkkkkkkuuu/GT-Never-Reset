package com.xyp.gtnrlib.client.fx.world;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.shader.ShaderEffect;
import com.xyp.gtnrlib.utils.Vec3d;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class FxBackdropRefraction implements AutoCloseable {

    private static final ShaderEffect SHADER = new ShaderEffect(
        new ResourceLocation(GTNRLib.MODID, "shaders/fx/shape.vert.glsl"),
        new ResourceLocation(GTNRLib.MODID, "shaders/fx/backdrop_refraction.frag.glsl"));
    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final int SAMPLE_MARGIN = 2;

    private int colorTexture, depthTexture, textureWidth, textureHeight;

    static boolean prepareShader() {
        return SHADER.prepare();
    }

    boolean draw(Vec3d center, Vec3d right, Vec3d up, double radius, float strength) {
        if (!OpenGlHelper.shadersSupported || GL11.glGetInteger(GL13.GL_SAMPLE_BUFFERS) != 0
            || GL11.glGetInteger(GL11.GL_DEPTH_BITS) == 0) return false;

        Vec3d horizontal = right.scale(radius), vertical = up.scale(radius);
        Region region = project(center, horizontal, vertical);
        if (region == null || !SHADER.begin()) return false;

        int previousUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int colorUnit = OpenGlHelper.defaultTexUnit, depthUnit = colorUnit + 1;
        OpenGlHelper.setActiveTexture(colorUnit);
        int previousColor = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        OpenGlHelper.setActiveTexture(depthUnit);
        int previousDepth = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            if (!capture(region)) return false;
            SHADER.uniform1i("sceneColor", 0);
            SHADER.uniform1i("sceneDepth", 1);
            SHADER.uniform2f("centerPixel", (float) region.center.x, (float) region.center.y);
            SHADER.uniform2f("copyOrigin", region.x, region.y);
            SHADER.uniform2f("copySize", region.width, region.height);
            SHADER.uniform2f("textureSize", textureWidth, textureHeight);
            SHADER.uniform1f("strength", strength);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glBegin(GL11.GL_QUADS);
            corner(center, horizontal, vertical, -1, -1);
            corner(center, horizontal, vertical, 1, -1);
            corner(center, horizontal, vertical, 1, 1);
            corner(center, horizontal, vertical, -1, 1);
            GL11.glEnd();
            return true;
        } finally {
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(depthUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousDepth);
            OpenGlHelper.setActiveTexture(colorUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousColor);
            OpenGlHelper.setActiveTexture(previousUnit);
            SHADER.end();
        }
    }

    private boolean capture(Region region) {
        // Query the current framebuffer attachment instead of the legacy draw-buffer cache.
        int drawBuffer = GL11.glGetInteger(GL20.GL_DRAW_BUFFER0);
        if (drawBuffer == GL11.GL_NONE || drawBuffer == GL11.GL_FRONT_AND_BACK) return false;
        int limit = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (region.width > limit || region.height > limit) return false;
        int width = Math.min(limit, Math.max(textureWidth, textureExtent(region.width)));
        int height = Math.min(limit, Math.max(textureHeight, textureExtent(region.height)));
        boolean resize = width != textureWidth || height != textureHeight;
        if (colorTexture == 0) {
            colorTexture = GL11.glGenTextures();
            depthTexture = GL11.glGenTextures();
        }

        var capabilities = GLContext.getCapabilities();
        boolean separateTargets = capabilities.OpenGL30 || capabilities.GL_ARB_framebuffer_object;
        int read = separateTargets ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) : 0;
        int draw = separateTargets ? GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) : read;
        if (read != draw) ARBFramebufferObject.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, draw);
        int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        try {
            if (previousReadBuffer != drawBuffer) GL11.glReadBuffer(drawBuffer);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + 1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
            if (resize) allocate(GL14.GL_DEPTH_COMPONENT24, GL11.GL_DEPTH_COMPONENT, GL11.GL_NEAREST, width, height);
            copy(region);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
            if (resize) allocate(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_LINEAR, width, height);
            copy(region);
            textureWidth = width;
            textureHeight = height;
            return true;
        } finally {
            if (previousReadBuffer != drawBuffer) GL11.glReadBuffer(previousReadBuffer);
            if (read != draw) ARBFramebufferObject.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        }
    }

    private static void allocate(int internalFormat, int format, int filter, int width, int height) {
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            internalFormat,
            width,
            height,
            0,
            format,
            GL11.GL_UNSIGNED_BYTE,
            (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private static void copy(Region region) {
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, region.x, region.y, region.width, region.height);
    }

    private static int textureExtent(int size) {
        return Math.max(1, Integer.highestOneBit(size - 1) * 2);
    }

    private static Region project(Vec3d center, Vec3d horizontal, Vec3d vertical) {
        MODELVIEW.clear();
        PROJECTION.clear();
        VIEWPORT.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        int windowX = VIEWPORT.get(0), windowY = VIEWPORT.get(1);
        int windowWidth = VIEWPORT.get(2), windowHeight = VIEWPORT.get(3);
        Pixel midpoint = project(center, windowX, windowY, windowWidth, windowHeight);
        if (midpoint == null) return null;

        double minX = midpoint.x, maxX = midpoint.x, minY = midpoint.y, maxY = midpoint.y;
        for (int corner = 0; corner < 4; corner++) {
            Vec3d point = offset(center, horizontal, vertical, (corner & 1) == 0 ? -1 : 1, (corner & 2) == 0 ? -1 : 1);
            Pixel pixel = project(point, windowX, windowY, windowWidth, windowHeight);
            if (pixel == null) return null;
            minX = Math.min(minX, pixel.x);
            maxX = Math.max(maxX, pixel.x);
            minY = Math.min(minY, pixel.y);
            maxY = Math.max(maxY, pixel.y);
        }
        int x0 = (int) Math.max(windowX, Math.floor(minX - SAMPLE_MARGIN));
        int y0 = (int) Math.max(windowY, Math.floor(minY - SAMPLE_MARGIN));
        int x1 = (int) Math.min(windowX + windowWidth, Math.ceil(maxX + SAMPLE_MARGIN));
        int y1 = (int) Math.min(windowY + windowHeight, Math.ceil(maxY + SAMPLE_MARGIN));
        return x1 > x0 && y1 > y0 ? new Region(x0, y0, x1 - x0, y1 - y0, midpoint) : null;
    }

    private static Pixel project(Vec3d point, int x, int y, int width, int height) {
        double eyeX = transform(MODELVIEW, 0, point.x(), point.y(), point.z(), 1);
        double eyeY = transform(MODELVIEW, 1, point.x(), point.y(), point.z(), 1);
        double eyeZ = transform(MODELVIEW, 2, point.x(), point.y(), point.z(), 1);
        double eyeW = transform(MODELVIEW, 3, point.x(), point.y(), point.z(), 1);
        double w = transform(PROJECTION, 3, eyeX, eyeY, eyeZ, eyeW);
        if (!(w > 0)) return null;
        double clipX = transform(PROJECTION, 0, eyeX, eyeY, eyeZ, eyeW);
        double clipY = transform(PROJECTION, 1, eyeX, eyeY, eyeZ, eyeW);
        return new Pixel(x + (clipX / w + 1) * width * .5, y + (clipY / w + 1) * height * .5);
    }

    private static double transform(FloatBuffer matrix, int row, double x, double y, double z, double w) {
        return matrix.get(row) * x + matrix.get(4 + row) * y + matrix.get(8 + row) * z + matrix.get(12 + row) * w;
    }

    private static void corner(Vec3d center, Vec3d horizontal, Vec3d vertical, int x, int y) {
        Vec3d point = offset(center, horizontal, vertical, x, y);
        GL11.glTexCoord2f(x, y);
        GL11.glVertex3d(point.x(), point.y(), point.z());
    }

    private static Vec3d offset(Vec3d center, Vec3d horizontal, Vec3d vertical, int x, int y) {
        return center.add(horizontal.scale(x))
            .add(vertical.scale(y));
    }

    @Override
    public void close() {
        if (colorTexture == 0) return;
        GL11.glDeleteTextures(colorTexture);
        GL11.glDeleteTextures(depthTexture);
        colorTexture = depthTexture = textureWidth = textureHeight = 0;
    }

    private record Pixel(double x, double y) {}

    private record Region(int x, int y, int width, int height, Pixel center) {}
}
