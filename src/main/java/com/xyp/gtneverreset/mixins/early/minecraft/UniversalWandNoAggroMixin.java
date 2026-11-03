package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;

@Mixin(EntityLiving.class)
public abstract class UniversalWandNoAggroMixin {

    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void gtneverreset$target(EntityLivingBase target, CallbackInfo ci) {
        if (UniversalWandProtectionHandler.ignoresTarget(target)) ci.cancel();
    }
}
