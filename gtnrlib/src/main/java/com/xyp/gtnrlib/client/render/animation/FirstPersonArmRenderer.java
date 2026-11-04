package com.xyp.gtnrlib.client.render.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.compat.draconicevolution.DraconicArmorArms;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class FirstPersonArmRenderer {

    private static final int CHEST_PASS = 1;
    private static final float MODEL_UNIT = 1f / 16;
    private static final int ARM_ATTRIBUTES = GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT
        | GL11.GL_ENABLE_BIT
        | GL11.GL_COLOR_BUFFER_BIT
        | GL11.GL_DEPTH_BUFFER_BIT;
    private static ModelBiped fallbackSkin;
    private static ModelBiped defaultChestplate;

    static void render(boolean left, float shoulderRoll) {
        var mc = Minecraft.getMinecraft();
        var player = mc.thePlayer;
        var renderer = RenderManager.instance.getEntityRenderObject(player);
        ModelBiped model;
        if (renderer instanceof RenderPlayer playerRenderer) {
            model = playerRenderer.modelBipedMain;
        } else {
            if (fallbackSkin == null) fallbackSkin = new ModelBiped(0);
            model = fallbackSkin;
        }
        var arm = left ? model.bipedLeftArm : model.bipedRightArm;
        GL11.glPushMatrix();
        GL11.glPushAttrib(ARM_ATTRIBUTES);
        try {
            GL11.glTranslatef((left ? 5 : -5) * MODEL_UNIT, arm.rotationPointY * MODEL_UNIT, 0);
            GL11.glRotatef((float) Math.toDegrees(shoulderRoll), 0, 0, 1);
            mc.getTextureManager()
                .bindTexture(player.getLocationSkin());
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, .1f);
            drawAtShoulder(arm, MODEL_UNIT);
            renderChestplate(player, left);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    public static void renderOverVanillaArm(EntityPlayer player, RenderPlayer renderer) {
        if (player.getCurrentArmor(2) == null) return;
        var arm = renderer.modelBipedMain.bipedRightArm;
        GL11.glPushMatrix();
        GL11.glPushAttrib(ARM_ATTRIBUTES);
        try {
            GL11.glTranslatef(arm.offsetX, arm.offsetY, arm.offsetZ);
            arm.postRender(MODEL_UNIT);
            renderChestplate(player, false);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void renderChestplate(EntityPlayer player, boolean left) {
        ItemStack chest = player.getCurrentArmor(2);
        if (chest == null || !(chest.getItem() instanceof ItemArmor armor)) return;
        if (defaultChestplate == null) defaultChestplate = new ModelBiped(1);
        var model = ForgeHooksClient.getArmorModel(player, chest, CHEST_PASS, defaultChestplate);
        var arm = left ? model.bipedLeftArm : model.bipedRightArm;
        float scale = ModList.DraconicEvolution.isModLoaded() ? DraconicArmorArms.scale(model, MODEL_UNIT) : MODEL_UNIT;
        var textures = Minecraft.getMinecraft()
            .getTextureManager();
        textures.bindTexture(RenderBiped.getArmorResource(player, chest, CHEST_PASS, null));
        int color = armor.getColor(chest);
        GL11.glColor4f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
        drawAtShoulder(arm, scale);
        if (color != -1) {
            textures.bindTexture(RenderBiped.getArmorResource(player, chest, CHEST_PASS, "overlay"));
            GL11.glColor4f(1, 1, 1, 1);
            drawAtShoulder(arm, scale);
        }
    }

    // The animation already positioned the shoulder. Keep the live mesh, sleeve children and skin UVs.
    private static void drawAtShoulder(ModelRenderer part, float scale) {
        float x = part.rotationPointX, y = part.rotationPointY, z = part.rotationPointZ;
        float pitch = part.rotateAngleX, yaw = part.rotateAngleY, roll = part.rotateAngleZ;
        try {
            part.setRotationPoint(0, 0, 0);
            part.rotateAngleX = part.rotateAngleY = part.rotateAngleZ = 0;
            part.render(scale);
        } finally {
            part.setRotationPoint(x, y, z);
            part.rotateAngleX = pitch;
            part.rotateAngleY = yaw;
            part.rotateAngleZ = roll;
        }
    }

    private FirstPersonArmRenderer() {}
}
