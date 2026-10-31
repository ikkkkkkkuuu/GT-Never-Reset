package com.xyp.gtnrlib.utils;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Client rendering helpers; numeric entry points delegate to the common-side Colors implementation. */
@SideOnly(Side.CLIENT)
public class ColorMath {

    public static float red(int rgb) {
        return Colors.red(rgb);
    }

    public static float green(int rgb) {
        return Colors.green(rgb);
    }

    public static float blue(int rgb) {
        return Colors.blue(rgb);
    }

    public static int mixWhite(int rgb, float k) {
        return Colors.mixWhite(rgb, k);
    }

    public static int scale(int rgb, float k) {
        return Colors.scaleRgb(rgb, k);
    }

    public static int lerp(int from, int to, float t) {
        return Colors.lerpRgb(from, to, t);
    }

    public static int over(int argb, int rgb) {
        return Colors.compositeRgb(argb, rgb);
    }

    public static double[] hsl(int rgb) {
        return Colors.hsl(rgb);
    }

    public static int hsl(double hue, double saturation, double lightness) {
        return Colors.hsl(hue, saturation, lightness);
    }

    public static double luminance(int rgb) {
        return Colors.luminance(rgb);
    }

    public static double contrast(int rgb, int other) {
        return Colors.contrast(rgb, other);
    }

    public static void tessellate(Tessellator tessellator, int rgb, float alpha) {
        tessellator.setColorRGBA_F(red(rgb), green(rgb), blue(rgb), MathUtils.clampUnit(alpha));
    }

    public static void tessellateArgb(Tessellator tessellator, int argb) {
        tessellator.setColorRGBA_I(argb & 0xFFFFFF, argb >>> 24);
    }

    public static void gl(int rgb, float alpha) {
        GL11.glColor4f(red(rgb), green(rgb), blue(rgb), alpha);
    }
}
