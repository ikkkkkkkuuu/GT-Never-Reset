package com.xyp.gtneverreset.mixins.late.cropsnh;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

@Mixin(SeedStats.class)
public abstract class MixinSeedStats {

    @ModifyVariable(method = "<init>(BBBZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static byte gtneverreset$maxGrowth(byte original) {
        ConfigLoader.ensureLoaded();
        return MainConfig.cropsNH.enableMaxStats ? (byte) 31 : original;
    }

    @ModifyVariable(method = "<init>(BBBZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private static byte gtneverreset$maxGain(byte original) {
        ConfigLoader.ensureLoaded();
        return MainConfig.cropsNH.enableMaxStats ? (byte) 31 : original;
    }

    @ModifyVariable(method = "<init>(BBBZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 2, remap = false)
    private static byte gtneverreset$maxResistance(byte original) {
        ConfigLoader.ensureLoaded();
        return MainConfig.cropsNH.enableMaxStats ? (byte) 31 : original;
    }
}
