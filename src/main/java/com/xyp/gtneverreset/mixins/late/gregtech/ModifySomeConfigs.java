package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import gregtech.GTMod;
import gregtech.loaders.preload.GTPreLoad;

@Mixin(value = GTPreLoad.class, remap = false)
public class ModifySomeConfigs {

    @Inject(
        method = "loadClientConfig",
        at = @At(value = "INVOKE", target = "Lgregtech/common/GTProxy;reloadNEICache()V", shift = At.Shift.BEFORE),
        require = 1)
    private static void gtneverreset$modifyClientConfig(CallbackInfo ci) {
        // GregTech preload can run before this mod's configuration registration.
        ConfigLoader.ensureLoaded();
        if (ClientConfig.enableAlwaysDisplayRecipeOwner) GTMod.gregtechproxy.mNEIRecipeOwner = true;
        if (ClientConfig.enableAlwaysDisplayWailaAverageNS) GTMod.gregtechproxy.wailaAverageNS = true;
        if (ClientConfig.enableAlwaysDisplayNEIOriginalVoltage) GTMod.gregtechproxy.mNEIOriginalVoltage = true;
    }
}
