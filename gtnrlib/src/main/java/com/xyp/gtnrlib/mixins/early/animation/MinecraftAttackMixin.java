package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;

@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {

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
