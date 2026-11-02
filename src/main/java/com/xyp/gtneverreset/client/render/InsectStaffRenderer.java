package com.xyp.gtneverreset.client.render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.item.items.InsectStaff;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class InsectStaffRenderer implements IItemRenderer, IResourceManagerReloadListener {

    private static final ResourceLocation MODEL = new ResourceLocation(
        "draconicevolution",
        "models/tools/DraconicStaffOfPower.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "draconicevolution",
        "textures/models/tools/DraconicStaffOfPower.png");
    private IModelCustom model;
    private ResourceLocation texture;

    public InsectStaffRenderer() {
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(this);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {
        model = AdvancedModelLoader.loadModel(MODEL);
        if (texture != null && !texture.equals(TEXTURE)) Minecraft.getMinecraft()
            .getTextureManager()
            .deleteTexture(texture);
        try (InputStream stream = resources.getResource(TEXTURE)
            .getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    image.setRGB(x, y, (pixel & 0xFF000000) | chitinColor(pixel & 0xFFFFFF));
                }
            }
            texture = Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation("gtnr_insect_staff", new DynamicTexture(image));
        } catch (IOException failure) {
            texture = TEXTURE;
            GTNeverReset.LOG.warn("could not tint insect staff texture", failure);
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type == ItemRenderType.ENTITY && helper == ItemRendererHelper.ENTITY_ROTATION;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        try {
            ((InsectStaff) stack.getItem()).tweakRender(type);
            GL11.glColor4f(1, 1, 1, 1);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(texture);
            model.renderAll();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            for (int i = 0; i < 5; i++) {
                double z = -0.65 + i * 0.30;
                shell(0, 0.22, z, 0.18, 0.245, 0.18, 0x78658d);
                shell(0, 0.22, z - 0.016, 0.15, 0.258, 0.15, 0x302739);
            }
            GL11.glDisable(GL11.GL_LIGHTING);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            shell(0, 0.24, -1.57, 0.14, 0.265, 0.19, 0xc7a1ff);
            shell(0, 0.24, -1.57, 0.065, 0.285, 0.12, 0xeee0ff);
            for (int side : new int[] { -1, 1 }) {
                for (double face : new double[] { 0.265, 0.172 }) {
                    vein(side * 0.40, -1.72, side * 0.62, -1.80, face, 0.013);
                    vein(side * 0.62, -1.80, side * 0.88, -1.62, face, 0.010);
                    vein(side * 0.88, -1.62, side * 1.18, -1.30, face, 0.007);
                }
            }
            for (int i = 0; i < 3; i++) {
                shell(0, 0.22, 1.0 + i * 0.16, 0.035, 0.32, 0.060, 0xb88aec);
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static int chitinColor(int rgb) {
        switch (rgb) {
            case 0xFF6600:
                return 0x393044;
            case 0xCC6600:
                return 0x241d30;
            case 0x440000:
                return 0x7954a0;
            case 0x000000:
                return 0x17131f;
            case 0x111111:
                return 0x272032;
            case 0x222222:
                return 0x40334f;
            case 0x663300:
                return 0x534164;
            case 0xD7DD89:
                return 0xa69ab9;
            case 0xFFD449:
                return 0xe3c9ff;
            case 0xFFCC33:
                return 0xbc8ded;
            case 0xFF9900:
                return 0x675079;
            case 0xCC9933:
                return 0xa180c2;
            case 0x663366:
                return 0x9470bd;
            default:
                float[] hsv = Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
                return Color.HSBtoRGB(0.75F, 0.30F + hsv[1] * 0.25F, 0.12F + hsv[2] * 0.55F) & 0xFFFFFF;
        }
    }

    private static void shell(double x, double y, double z, double width, double height, double length, int color) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int face : new int[] { -1, 1 }) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                double b = (i + 1) * Math.PI / 4;
                float shade = 0.72F + 0.28F * (float) ((Math.cos(a - 0.8) + 1) / 2);
                t.setColorOpaque_F(
                    (color >> 16 & 255) / 255F * shade,
                    (color >> 8 & 255) / 255F * shade,
                    (color & 255) / 255F * shade);
                t.setNormal(0, face, 0);
                t.addVertex(0, face * height, 0);
                t.addVertex(Math.cos(a) * width, face * height * 0.72, -face * Math.sin(a) * length);
                t.addVertex(Math.cos(b) * width, face * height * 0.72, -face * Math.sin(b) * length);
            }
        }
        t.draw();
        GL11.glPopMatrix();
    }

    private static void vein(double x1, double z1, double x2, double z2, double y, double width) {
        double length = Math.hypot(x2 - x1, z2 - z1);
        double dx = (z2 - z1) / length * width;
        double dz = -(x2 - x1) / length * width;
        GL11.glDisable(GL11.GL_CULL_FACE);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorOpaque(183, 140, 232);
        t.setNormal(0, 1, 0);
        t.addVertex(x1 - dx, y, z1 - dz);
        t.addVertex(x2 - dx, y, z2 - dz);
        t.addVertex(x2 + dx, y, z2 + dz);
        t.addVertex(x1 + dx, y, z1 + dz);
        t.draw();
    }
}
