package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.animation.HmiFirstPerson;

@Mixin(value = EntityPlayer.class, priority = 1001)
public abstract class OffhandSwingMixin {

    @Dynamic("Backhand adds the accepted offhand swing to EntityPlayer")
    @Inject(
        method = "swingOffItem",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/player/EntityPlayer;backhand$isOffHandSwingInProgress:Z",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER),
        remap = false,
        require = 0)
    private void gtnrlib$offhandSwing(CallbackInfo ci) {
        if ((Object) this == Minecraft.getMinecraft().thePlayer) HmiFirstPerson.OFFHAND.swing();
    }
}
