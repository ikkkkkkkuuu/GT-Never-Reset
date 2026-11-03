package com.xyp.gtneverreset.mixins.early.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;

@Mixin(PlayerControllerMP.class)
public abstract class UniversalWandBlockReachMixin {

    @Inject(method = "getBlockReachDistance", at = @At("RETURN"), cancellable = true)
    private void gtneverreset$extendBlockReach(CallbackInfoReturnable<Float> cir) {
        if (ItemUniversalWand.hasReachBonus(Minecraft.getMinecraft().thePlayer))
            cir.setReturnValue(cir.getReturnValue() + (float) ItemUniversalWand.REACH_BONUS);
    }
}
