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
                    float[] hsv = Color.RGBtoHSB(pixel >> 16 & 255, pixel >> 8 & 255, pixel & 255, null);
                    float brightness = hsv[2] * 0.70F;
                    float hue = hsv[0] < 0.2F ? 0.32F : 0.38F;
                    if (hsv[0] >= 0.2F) brightness *= 0.58F;
                    image.setRGB(
                        x,
                        y,
                        (pixel & 0xFF000000) | (Color.HSBtoRGB(hue, Math.max(0.45F, hsv[1]), brightness) & 0xFFFFFF));
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
            GL11.glColor4f(0.06F, 0.19F, 0.10F, 1);
            for (int i = 0; i < 5; i++) {
                GL11.glPushMatrix();
                GL11.glTranslated(0, 0.22, -0.72 + i * 0.36);

                plate(0.24, 0.26, 0.10);
                GL11.glPopMatrix();
            }
            GL11.glDisable(GL11.GL_LIGHTING);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            GL11.glColor4f(0.35F, 1F, 0.06F, 1);
            for (int side : new int[] { -1, 1 }) {
                GL11.glPushMatrix();
                GL11.glTranslated(side * 0.11, 0.47, -1.57);

                plate(0.07, 0.025, 0.11);
                GL11.glPopMatrix();
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void plate(double x, double y, double z) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setNormal(0, 1, 0);
        t.addVertex(-x, y, -z);
        t.addVertex(-x, y, z);
        t.addVertex(x, y, z);
        t.addVertex(x, y, -z);
        t.setNormal(0, -1, 0);
        t.addVertex(-x, -y, z);
        t.addVertex(-x, -y, -z);
        t.addVertex(x, -y, -z);
        t.addVertex(x, -y, z);
        t.setNormal(1, 0, 0);
        t.addVertex(x, -y, -z);
        t.addVertex(x, y, -z);
        t.addVertex(x, y, z);
        t.addVertex(x, -y, z);
        t.setNormal(-1, 0, 0);
        t.addVertex(-x, -y, z);
        t.addVertex(-x, y, z);
        t.addVertex(-x, y, -z);
        t.addVertex(-x, -y, -z);
        t.setNormal(0, 0, 1);
        t.addVertex(x, -y, z);
        t.addVertex(x, y, z);
        t.addVertex(-x, y, z);
        t.addVertex(-x, -y, z);
        t.setNormal(0, 0, -1);
        t.addVertex(-x, -y, -z);
        t.addVertex(-x, y, -z);
        t.addVertex(x, y, -z);
        t.addVertex(x, -y, -z);
        t.draw();
    }
}
