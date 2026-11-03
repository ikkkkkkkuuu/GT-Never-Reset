package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.wand.UniversalWandUtilityHandler;

@Mixin(EntityPlayerMP.class)
public abstract class UniversalWandFlightMixin {

    @Inject(method = "sendPlayerAbilities", at = @At("HEAD"))
    private void gtneverreset$observeExternalFlight(CallbackInfo ci) {
        UniversalWandUtilityHandler.observeAbilitySync((EntityPlayerMP) (Object) this);
    }
}
