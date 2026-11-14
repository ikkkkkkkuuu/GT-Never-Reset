package com.xyp.gtnrlib.compat.backhand;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemMap;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations.EquippedStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import xonin.backhand.api.core.IBackhandPlayer;
import xonin.backhand.client.hooks.ItemRendererHooks;
import xonin.backhand.client.utils.BackhandRenderHelper;
import xonin.backhand.utils.BackhandConfig;
import xonin.backhand.utils.BackhandConfigClient;

@SideOnly(Side.CLIENT)
public final class BackhandAnimationCompat {

    private BackhandAnimationCompat() {}

    public static void render(float partial) {
        ItemRendererHooks.renderOffhandReturn(partial);
    }

    public static boolean ownsLeftArm(float partial) {
        var mc = Minecraft.getMinecraft();
        var main = ((EquippedStack) mc.entityRenderer.itemRenderer).gtnrlib$equippedStack();
        if (main != null && main.getItem() instanceof ItemMap) return false;
        return ((EquippedStack) BackhandRenderHelper.itemRenderer).gtnrlib$equippedStack() != null
            || BackhandConfigClient.RenderEmptyOffhandAtRest
            || BackhandConfig.EmptyOffhand && ((IBackhandPlayer) mc.thePlayer).getOffSwingProgress(partial) != 0;
    }

    public static boolean usingOffhand() {
        return ((IBackhandPlayer) Minecraft.getMinecraft().thePlayer).isOffhandItemInUse();
    }
}
