package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.compat.backhand.BackhandAnimationCompat;

import cpw.mods.fml.common.Loader;

// Backhand overwrites right click at priority 1000 and moves the accepted block swing into its helper.
@Mixin(value = Minecraft.class, priority = 1001)
public abstract class MinecraftAttackMixin {

    @WrapOperation(
        method = "func_147121_ag()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;swingItem()V"),
        require = 0)
    private void gtnrlib$vanillaBlockUseSwing(EntityClientPlayerMP player, Operation<Void> vanilla) {
        gtnrlib$blockUseSwing(player, vanilla);
    }

    // The helper is added at runtime by Backhand. Its call to the vanilla swing still uses the refmap.
    @WrapOperation(
        method = "backhand$rightClickBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;swingItem()V",
            remap = true),
        remap = false,
        require = 0)
    private void gtnrlib$backhandBlockUseSwing(EntityClientPlayerMP player, Operation<Void> vanilla) {
        gtnrlib$blockUseSwing(player, vanilla);
    }

    @Unique
    private static void gtnrlib$blockUseSwing(EntityClientPlayerMP player, Operation<Void> vanilla) {
        var hand = Loader.isModLoaded("backhand") && BackhandAnimationCompat.selected(player) ? HmiFirstPerson.OFFHAND
            : HmiFirstPerson.INSTANCE;
        if (hand.enabled()) hand.duringBlockUseSwing(() -> vanilla.call(player));
        else vanilla.call(player);
    }

    @Inject(
        method = "func_147116_af",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;swingItem()V",
            shift = At.Shift.AFTER),
        require = 0)
    private void gtnrlib$attack(CallbackInfo ci) {
        if (!FirstPersonAnimations.INSTANCE.enabled()) return;
        var mc = Minecraft.getMinecraft();
        // swingItem can decline an early repeat or let the held item's onEntitySwing hook consume it.
        if (!mc.thePlayer.isSwingInProgress || mc.thePlayer.swingProgressInt != -1) return;
        var hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == MovingObjectType.BLOCK) {
            if (!mc.theWorld.isAirBlock(hit.blockX, hit.blockY, hit.blockZ))
                FirstPersonAnimations.INSTANCE.trigger("mining");
        } else FirstPersonAnimations.INSTANCE.trigger("attack");
    }
}
