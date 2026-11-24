package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.fx.world.FxStructureDeployment;

@Mixin(EntityRenderer.class)
public abstract class StructureDeploymentCameraMixin {

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void gtnrlib$shakeOnStructureImpact(float partialTicks, CallbackInfo callback) {
        FxStructureDeployment.shakeCamera(partialTicks);
    }
}
