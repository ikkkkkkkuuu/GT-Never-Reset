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
public final class GregTechWrenchAnimation implements HmiItemBindings.HandheldTool {

    public static final GregTechWrenchAnimation INSTANCE = new GregTechWrenchAnimation();
    // GT's 16px wrench has a vertical, centered handle. Place its lower grip on the upstream handheld grip.
    private static final Matrix4fc GRIP = new Matrix4f().translation(12f / 16, 4f / 16, 0)
        .rotateZ((float) Math.toRadians(32))
        .translate(-8f / 16, -4f / 16, 0);

    private GregTechWrenchAnimation() {}

    public static void register() {
        HmiItemBindings.registerTool(MetaGeneratedTool01.INSTANCE, INSTANCE);
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return renderer.getClass() == MetaGeneratedToolRenderer.class
            && GregTechToolUsePacket.matches(stack, IDMetaTool01.WRENCH);
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
