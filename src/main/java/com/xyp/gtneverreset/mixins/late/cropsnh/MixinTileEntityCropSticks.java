package com.xyp.gtneverreset.mixins.late.cropsnh;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.cropsnh.api.ISeedData;
import com.gtnewhorizon.cropsnh.items.ItemGenericSeed;
import com.gtnewhorizon.cropsnh.tileentity.TileEntityCropSticks;
import com.xyp.gtneverreset.config.CropsNHConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

@Mixin(TileEntityCropSticks.class)
public abstract class MixinTileEntityCropSticks {

    @Shadow(remap = false)
    private ISeedData seed;

    @Shadow(remap = false)
    private int growthProgress;

    @Shadow(remap = false)
    // This shadow writes the target object's state and must remain a field.
    @SuppressWarnings("FieldCanBeLocal")
    private boolean isDirty;

    @Shadow(remap = false)
    public abstract boolean hasCrop();

    @Shadow(remap = false)
    public abstract boolean hasWeed();

    @Shadow(remap = false)
    public abstract ItemStack getSeedStack();

    @ModifyConstant(method = "updateEntity", constant = @Constant(intValue = 256), remap = true)
    private int gtneverreset$modifyTickRateInUpdateEntity(int original) {
        return 1;
    }

    @Inject(method = "doGrowth", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtneverreset$instantGrowth(CallbackInfo ci) {
        ConfigLoader.ensureLoaded();
        if (!CropsNHConfig.enableInstantGrowth) return;
        if (!this.hasCrop()) return;
        int duration = this.seed.getCrop()
            .getGrowthDuration();
        if (this.growthProgress < duration) {
            this.growthProgress = duration;
            this.isDirty = true;
        }
        ci.cancel();
    }

    @Inject(method = "spawnWeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtneverreset$disableSpawnWeed(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "spreadWeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtneverreset$disableSpreadWeed(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getSeedDrop", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtneverreset$guaranteedSeedDrop(CallbackInfoReturnable<ItemStack> cir) {
        ConfigLoader.ensureLoaded();
        if (!CropsNHConfig.enableGuaranteedSeedDrop) return;
        if (this.hasCrop() && !this.hasWeed()) {
            ItemStack drop = this.getSeedStack();
            if (drop != null) {
                if (!(drop.getItem() instanceof ItemGenericSeed)) {
                    drop = this.seed.getCrop()
                        .getSeedItem(this.seed.getStats());
                }
                if (drop == null) return;
                drop = drop.copy();
                drop.stackSize = 1;
                cir.setReturnValue(drop);
            }
        }
    }
}
