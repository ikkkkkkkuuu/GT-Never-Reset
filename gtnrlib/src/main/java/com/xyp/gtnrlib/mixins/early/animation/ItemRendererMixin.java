package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.render.animation.FirstPersonPoseRenderer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Shadow
    private ItemStack itemToRender;

    @ModifyExpressionValue(
        method = "renderItemInFirstPerson",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getSwingProgress(F)F"),
        require = 0)
    private float gtnrlib$visualSwing(float vanilla) {
        return FirstPersonAnimations.INSTANCE.replacesSwing(itemToRender) ? 0 : vanilla;
    }

    // Successful block placement resets the equipped height. Sample both interpolation endpoints at rest for that clip.
    @ModifyExpressionValue(
        method = "renderItemInFirstPerson",
        at = { @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemRenderer;prevEquippedProgress:F"),
            @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemRenderer;equippedProgress:F") },
        require = 0)
    private float gtnrlib$placementHeight(float vanilla) {
        return FirstPersonAnimations.INSTANCE.replacesPlacementDip(itemToRender) ? 1 : vanilla;
    }

    @Inject(
        method = "renderItemInFirstPerson",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;shouldRotateAroundWhenRendering()Z"),
        require = 0)
    private void gtnrlib$held(float partial, CallbackInfo ci) {
        FirstPersonPoseRenderer.held(itemToRender, partial);
    }

    @Inject(
        method = "renderItemInFirstPerson",
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;shouldRotateAroundWhenRendering()Z")),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            ordinal = 0,
            shift = At.Shift.AFTER),
        require = 0)
    private void gtnrlib$empty(float partial, CallbackInfo ci) {
        FirstPersonPoseRenderer.empty(partial);
    }
}
