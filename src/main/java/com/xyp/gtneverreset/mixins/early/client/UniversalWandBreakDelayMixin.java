package com.xyp.gtneverreset.mixins.early.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;

@Mixin(PlayerControllerMP.class)
public abstract class UniversalWandBreakDelayMixin {

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private int blockHitDelay;

    @Unique
    private int gtneverreset$breakDelay(int original) {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.getItem() instanceof ItemUniversalWand ? ItemUniversalWand.getBreakCooldown(stack)
            : original;
    }

    @ModifyExpressionValue(
        method = { "clickBlock", "onPlayerDamageBlock" },
        at = @At(value = "CONSTANT", args = "intValue=5"))
    private int gtneverreset$heldBreakDelay(int original) {
        return gtneverreset$breakDelay(original);
    }

    @Inject(method = "onPlayerDestroyBlock", at = @At("RETURN"))
    private void gtneverreset$instantBreakDelay(int x, int y, int z, int side, CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ()) blockHitDelay = gtneverreset$breakDelay(blockHitDelay);
    }
}
