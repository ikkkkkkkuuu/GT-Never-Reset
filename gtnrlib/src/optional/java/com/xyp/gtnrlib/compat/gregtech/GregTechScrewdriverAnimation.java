package com.xyp.gtnrlib.compat.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.network.GregTechToolUsePacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.render.MetaGeneratedToolRenderer;

@SideOnly(Side.CLIENT)
public final class GregTechScrewdriverAnimation implements HmiItemBindings.HandheldTool {

    // Align GT's diagonal shaft with the wrist clip's Y axis, rotating about the handle in sprite coordinates.
    private static final Matrix4fc GRIP = new Matrix4f().translation(12f / 16, 4f / 16, .74f / 16)
        .rotateZ((float) Math.toRadians(10))
        .translate(-12f / 16, -4f / 16, 0);

    private GregTechScrewdriverAnimation() {}

    public static void register() {
        HmiItemBindings.registerTool(MetaGeneratedTool01.INSTANCE, new GregTechScrewdriverAnimation());
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return renderer.getClass() == MetaGeneratedToolRenderer.class
            && GregTechToolUsePacket.matches(stack, IDMetaTool01.SCREWDRIVER);
    }

    @Override
    public Matrix4fc spriteMount() {
        return GRIP;
    }

    @Override
    public boolean handlesBlockUseSwing() {
        return true;
    }
}
