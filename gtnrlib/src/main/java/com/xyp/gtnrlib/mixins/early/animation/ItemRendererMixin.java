package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.render.animation.FirstPersonPoseRenderer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Shadow
    private ItemStack itemToRender;

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
