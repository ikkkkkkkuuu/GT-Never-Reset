package com.xyp.gtneverreset.mixins.late.enderio;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;

import crazypants.enderio.EnderIO;
import crazypants.enderio.api.tool.ITool;

@Pseudo
@Mixin(targets = "crazypants.enderio.tool.ToolUtil", remap = false)
public abstract class ToolUtilMixin {

    @Inject(method = "getEquippedToolImpl", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtneverreset$hideDisabledWand(EntityPlayer player, CallbackInfoReturnable<ITool> cir) {
        player = player == null ? EnderIO.proxy.getClientPlayer() : player;
        if (player == null) return;
        net.minecraft.item.ItemStack stack = player.getHeldItem();
        if (stack != null && stack.getItem() instanceof ItemUniversalWand
            && ItemUniversalWand.getGtToolMode(stack) == ItemUniversalWand.GtToolMode.OFF) {
            cir.setReturnValue(null);
        }
    }

}
