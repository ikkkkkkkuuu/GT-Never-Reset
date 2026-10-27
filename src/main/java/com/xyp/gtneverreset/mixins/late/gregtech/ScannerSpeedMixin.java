package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.common.tileentities.machines.basic.MTEScanner;

@Mixin(value = MTEScanner.class, remap = false)
public abstract class ScannerSpeedMixin {

    @ModifyExpressionValue(
        method = "checkRecipe",
        at = @At(value = "FIELD", target = "Lgregtech/api/util/GTScannerResult;duration:I"),
        require = 1,
        allow = 1)
    private int gtneverreset$modifyScannerDuration(int duration) {
        ConfigLoader.ensureLoaded();
        return RecipeSpeed.getModifiedRecipeDuration(duration);
    }
}
