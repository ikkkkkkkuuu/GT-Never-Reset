package com.xyp.gtneverreset.mixins.late.spiceoflife;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.config.SpiceOfLifeConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

@Pseudo
@Mixin(targets = "squeek.spiceoflife.foodtracker.FoodModifier", remap = false)
public abstract class MixinFoodModifier {

    @Inject(
        method = "getFoodModifier(Lsqueek/spiceoflife/foodtracker/FoodHistory;Lnet/minecraft/item/ItemStack;)F",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private static void gtneverreset$fullFoodValue(CallbackInfoReturnable<Float> cir) {
        ConfigLoader.ensureLoaded();
        if (SpiceOfLifeConfig.disableDiminishingReturns) {
            cir.setReturnValue(1.0F);
        }
    }
}
