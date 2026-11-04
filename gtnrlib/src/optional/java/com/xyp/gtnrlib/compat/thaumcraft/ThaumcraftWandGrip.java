package com.xyp.gtnrlib.compat.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.xyp.gtnrlib.client.animation.HmiItemBindings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.item.ItemWandRenderer;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;

@SideOnly(Side.CLIENT)
public final class ThaumcraftWandGrip implements HmiItemBindings.HandheldTool {

    // The original renderer pivots at model Y=1 while casting. After Forge centers it, that is Y=-0.1
    // for a wand or sceptre and Y=0.4 for a staff. Keep that point inside the fist in both poses.
    private static final Matrix4fc WAND = mount(-.1f, -24);
    private static final Matrix4fc STAFF = mount(.4f, -52);

    private static Matrix4fc mount(float pivotY, float pitch) {
        return new Matrix4f().translation(.1044f, -.1207f, .1062f)
            .rotateX((float) Math.toRadians(pitch))
            .rotateZ((float) Math.toRadians(9))
            .scale(.68f)
            .translate(0, -pivotY, 0);
    }

    private ThaumcraftWandGrip() {}

    public static void register() {
        HmiItemBindings.registerTool(ConfigItems.itemWandCasting, new ThaumcraftWandGrip());
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return renderer.getClass() == ItemWandRenderer.class;
    }

    @Override
    public Matrix4fc equippedMount(ItemStack stack) {
        return ((ItemWandCasting) stack.getItem()).isStaff(stack) ? STAFF : WAND;
    }

    /** Place the lower handle at the Y=1 pivot used by the mounted model. */
    public static float handleOffset(ItemStack stack) {
        return 1 - (((ItemWandCasting) stack.getItem()).isStaff(stack) ? 38f : 18f) / 16;
    }
}
