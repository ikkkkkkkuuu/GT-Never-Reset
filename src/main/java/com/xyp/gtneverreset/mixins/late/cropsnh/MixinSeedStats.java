package com.xyp.gtneverreset.mixins.late.cropsnh;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.xyp.gtneverreset.common.machine.multiblock.crop.ArchivedSeedStats;
import com.xyp.gtneverreset.config.CropsNHConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

@Mixin(value = SeedStats.class, remap = false)
public abstract class MixinSeedStats {

    @Shadow
    @Final
    @Mutable
    private byte growth;
    @Shadow
    @Final
    @Mutable
    private byte gain;
    @Shadow
    @Final
    @Mutable
    private byte resistance;

    @Inject(method = "<init>(BBBZ)V", at = @At("RETURN"))
    private void gtneverreset$applyMaxStats(byte growth, byte gain, byte resistance, boolean analyzed,
        CallbackInfo ci) {
        if ((Object) this instanceof ArchivedSeedStats) return;
        ConfigLoader.ensureLoaded();
        if (CropsNHConfig.enableMaxStats) this.growth = this.gain = this.resistance = 31;
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"), cancellable = true)
    private static void gtneverreset$readArchive(NBTTagCompound tag, CallbackInfoReturnable<SeedStats> callback) {
        if (tag != null && tag.getBoolean(ArchivedSeedStats.ARCHIVE_MARKER)) {
            callback.setReturnValue(ArchivedSeedStats.fromTag(tag));
        }
    }
}
