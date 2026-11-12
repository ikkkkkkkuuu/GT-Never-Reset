package com.xyp.gtneverreset.mixins.late.cropsnh;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.cropsnh.loaders.MutationLoader;
import com.xyp.gtneverreset.loader.CropLoader;

@Mixin(value = MutationLoader.class, remap = false)
public abstract class MixinMutationLoader {

    @Inject(method = "postInit", at = @At("TAIL"), require = 1)
    private static void gtneverreset$registerMutations(CallbackInfo ci) {
        // Parent crops now exist, and CropsNH has not generated its machine recipe pages yet.
        CropLoader.registerMutations();
    }
}
