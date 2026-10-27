package com.xyp.gtneverreset.client.render;

import net.minecraft.client.model.ModelEnderCrystal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class StableEndCrystalRender extends Render {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "textures/entity/endercrystal/endercrystal.png");
    private final ModelEnderCrystal model = new ModelEnderCrystal(0, false);

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        float rotation = entity.ticksExisted + partialTicks;
        float bob = MathHelper.sin(rotation * 0.2F) / 2 + 0.5F;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        bindTexture(TEXTURE);
        model.render(entity, 0, rotation * 3, (bob + bob * bob) * 0.2F, 0, 0, 0.0625F);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
