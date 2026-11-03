package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;

@Mixin(Entity.class)
public abstract class UniversalWandEntityProtectionMixin {

    @Inject(method = "setDead", at = @At("HEAD"), cancellable = true)
    private void gtneverreset$remove(CallbackInfo ci) {
        if (UniversalWandProtectionHandler.isProtected((Entity) (Object) this)) ci.cancel();
    }

    @Inject(method = "setPosition", at = @At("HEAD"), cancellable = true)
    private void gtneverreset$position(double x, double y, double z, CallbackInfo ci) {
        if (UniversalWandProtectionHandler.isProtected((Entity) (Object) this)
            && !UniversalWandProtectionHandler.validPosition(x, y, z)) ci.cancel();
    }

    @Inject(method = { "setPositionAndRotation", "setLocationAndAngles" }, at = @At("HEAD"), cancellable = true)
    private void gtneverreset$location(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (UniversalWandProtectionHandler.isProtected((Entity) (Object) this)
            && !UniversalWandProtectionHandler.validPosition(x, y, z)) ci.cancel();
    }
}
