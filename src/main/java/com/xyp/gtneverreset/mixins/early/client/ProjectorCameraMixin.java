package com.xyp.gtneverreset.mixins.early.client;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.projector.ProjectorDeploymentRenderer;

@Mixin(EntityRenderer.class)
public abstract class ProjectorCameraMixin {

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void gtneverreset$shakeOnProjectorImpact(float partialTicks, CallbackInfo callback) {
        ProjectorDeploymentRenderer.shakeCamera(partialTicks);
    }
}
