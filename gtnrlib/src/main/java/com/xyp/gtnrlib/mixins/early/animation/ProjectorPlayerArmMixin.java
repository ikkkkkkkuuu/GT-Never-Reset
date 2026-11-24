package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnrlib.compat.gtneverreset.ProjectorArmAnimation;

@Mixin(ModelBiped.class)
public abstract class ProjectorPlayerArmMixin {

    @Shadow
    public ModelRenderer bipedRightArm;

    @ModifyExpressionValue(
        method = "setRotationAngles",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;heldItemRight:I"))
    private int gtnrlib$useUnheldPoseForWristComputer(int held, float walk, float stride, float age, float headYaw,
        float headPitch, float scale, Entity entity) {
        return entity instanceof EntityPlayer player && ProjectorArmAnimation.holdsProjector(player) ? 0 : held;
    }

    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void gtnrlib$raiseProjectorArm(float walk, float stride, float age, float headYaw, float headPitch,
        float scale, Entity entity, CallbackInfo callback) {
        if (entity instanceof EntityPlayer player) ProjectorArmAnimation.poseThirdPersonArm(player, bipedRightArm);
    }
}
