package com.xyp.gtnrlib.mixins.early.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnrlib.client.animation.HmiFirstPerson;

@Mixin(EntityLivingBase.class)
public abstract class LocalSwingMixin {

    @Inject(
        method = "swingItem",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/EntityLivingBase;isSwingInProgress:Z",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER),
        require = 0)
    private void gtnrlib$acceptedSwing(CallbackInfo ci) {
        if (HmiFirstPerson.INSTANCE.enabled() && (Object) this == Minecraft.getMinecraft().thePlayer)
            HmiFirstPerson.INSTANCE.swing();
    }
}
