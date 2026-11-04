package com.xyp.gtnrlib.mixins.late.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.render.animation.HmiPoseRenderer;
import com.xyp.gtnrlib.compat.thaumcraft.ThaumcraftWandGrip;

import thaumcraft.client.renderers.item.ItemWandRenderer;

@Mixin(value = ItemWandRenderer.class, remap = false)
public abstract class WandHandleMixin {

    // A mounted wand already follows the casting wrist. Leave other Forge render contexts to Thaumcraft.
    @ModifyExpressionValue(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;getItemInUse()Lnet/minecraft/item/ItemStack;",
            remap = true))
    private ItemStack gtnrlib$useWristPose(ItemStack using, ItemRenderType type, ItemStack stack) {
        if (using != null && type == ItemRenderType.EQUIPPED_FIRST_PERSON
            && HmiPoseRenderer.renderingMountedStack(stack)
            && FirstPersonItemActions.INSTANCE.playing("use")) return null;
        return using;
    }

    @Inject(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/client/renderers/models/gear/ModelWand;render(Lnet/minecraft/item/ItemStack;)V"))
    private void gtnrlib$holdHandle(ItemRenderType type, ItemStack stack, Object[] data, CallbackInfo ci) {
        if (type == ItemRenderType.EQUIPPED_FIRST_PERSON && HmiPoseRenderer.renderingMountedStack(stack))
            GL11.glTranslatef(0, ThaumcraftWandGrip.handleOffset(stack), 0);
    }
}
