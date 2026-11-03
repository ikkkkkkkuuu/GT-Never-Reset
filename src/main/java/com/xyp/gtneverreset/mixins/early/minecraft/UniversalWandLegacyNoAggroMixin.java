package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;

@Mixin(EntityCreature.class)
public abstract class UniversalWandLegacyNoAggroMixin {

    @Shadow
    protected Entity entityToAttack;

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void gtneverreset$target(Entity target, CallbackInfo ci) {
        if (UniversalWandProtectionHandler.ignoresTarget(target)) ci.cancel();
    }

    @Redirect(
        method = "updateEntityActionState",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/EntityCreature;entityToAttack:Lnet/minecraft/entity/Entity;",
            opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void gtneverreset$legacyTarget(EntityCreature creature, Entity target) {
        entityToAttack = UniversalWandProtectionHandler.ignoresTarget(target) ? null : target;
    }
}
