package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.render.animation.FirstPersonPoseRenderer;
import com.xyp.gtnrlib.client.render.animation.HmiPoseRenderer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin implements FirstPersonAnimations.EquippedStack {

    @ModifyExpressionValue(
        method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;getItemIcon(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/util/IIcon;",
            remap = true),
        remap = false,
        require = 0)
    private IIcon gtnrlib$bowStage(IIcon icon) {
        return HmiPoseRenderer.bowIcon(icon);
    }

    @Shadow
    private ItemStack itemToRender;

    @Shadow
    private float prevEquippedProgress;
    @Shadow
    private float equippedProgress;

    @Override
    public float gtnrlib$equipDip(float partial) {
        if (FirstPersonAnimations.INSTANCE.replacesPlacementDip(itemToRender)) return 0;
        return 1 - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partial);
    }

    @Override
    public ItemStack gtnrlib$equippedStack() {
        return itemToRender;
    }

    @ModifyExpressionValue(
        method = "renderItemInFirstPerson",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getSwingProgress(F)F"),
        require = 0)
    private float gtnrlib$visualSwing(float vanilla) {
        return FirstPersonAnimations.INSTANCE.replacesSwing(itemToRender) ? 0 : vanilla;
    }

    @Inject(
        method = "renderItemInFirstPerson",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V",
            ordinal = 0,
            shift = At.Shift.AFTER,
            remap = false),
        cancellable = true,
        require = 0)
    private void gtnrlib$hmi(float partial, CallbackInfo ci) {
        float dip = 1 - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partial);
        if (HmiPoseRenderer.render((ItemRenderer) (Object) this, itemToRender, partial, dip)) ci.cancel();
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
