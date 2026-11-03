package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.config.GregTechConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import gregtech.api.util.FakeCleanroom;

@Mixin(value = FakeCleanroom.class, remap = false)
public abstract class CleanroomRequirementMixin {

    @Inject(method = "isCleanroomBypassEnabled", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gtneverreset$applyCleanroomRequirement(CallbackInfoReturnable<Boolean> cir) {
        ConfigLoader.ensureLoaded();
        if (!GregTechConfig.requireCleanroom) {
            cir.setReturnValue(true);
        }
    }
}
