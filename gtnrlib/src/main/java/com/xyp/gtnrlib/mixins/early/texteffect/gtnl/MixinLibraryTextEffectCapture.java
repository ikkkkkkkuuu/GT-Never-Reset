package com.xyp.gtnrlib.mixins.early.texteffect.gtnl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnrlib.client.text.EffectTextRenderer;

@Pseudo
@Mixin(targets = "com.gtnllib.client.text.EffectTextRenderer", remap = false)
public abstract class MixinLibraryTextEffectCapture {

    @Inject(method = "isCapturing", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gtnrlib$includeTextCapture(CallbackInfoReturnable<Boolean> cir) {
        if (EffectTextRenderer.isCapturing()) cir.setReturnValue(true);
    }
}
