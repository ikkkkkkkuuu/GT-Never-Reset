package com.xyp.gtnrlib.compat.backhand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations.EquippedStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.common.items.relics.ItemThaumometer;
import xonin.backhand.api.core.BackhandUtils;
import xonin.backhand.api.core.IBackhandPlayer;
import xonin.backhand.client.hooks.ItemRendererHooks;
import xonin.backhand.client.utils.BackhandRenderHelper;
import xonin.backhand.utils.BackhandConfig;
import xonin.backhand.utils.BackhandConfigClient;
import xonin.backhand.utils.Mods;

@SideOnly(Side.CLIENT)
public final class BackhandAnimationCompat {

    private BackhandAnimationCompat() {}

    public static void render(float partial) {
        ItemRendererHooks.renderOffhandReturn(partial);
    }

    public static boolean canAnimate(ItemRenderer renderer, ItemStack stack) {
        return renderer == BackhandRenderHelper.itemRenderer
            && BackhandUtils.isUsingOffhand(Minecraft.getMinecraft().thePlayer)
            && !twoHanded(stack);
    }

    private static boolean twoHanded(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof ItemMap
            || Mods.THAUMCRAFT.isLoaded() && stack.getItem() instanceof ItemThaumometer);
    }

    public static ItemStack heldItem(EntityPlayer player) {
        return BackhandUtils.getOffhandItem(player);
    }

    public static int slot(EntityPlayer player) {
        return BackhandUtils.getOffhandSlot(player);
    }

    public static boolean selected(EntityPlayer player) {
        return BackhandUtils.isUsingOffhand(player);
    }

    public static boolean ownsLeftArm(float partial) {
        var mc = Minecraft.getMinecraft();
        var main = ((EquippedStack) mc.entityRenderer.itemRenderer).gtnrlib$equippedStack();
        if (twoHanded(main)) return false;
        return ((EquippedStack) BackhandRenderHelper.itemRenderer).gtnrlib$equippedStack() != null
            || BackhandConfigClient.RenderEmptyOffhandAtRest
            || BackhandConfig.EmptyOffhand && ((IBackhandPlayer) mc.thePlayer).getOffSwingProgress(partial) != 0;
    }

    public static boolean usingOffhand() {
        return ((IBackhandPlayer) Minecraft.getMinecraft().thePlayer).isOffhandItemInUse();
    }
}
