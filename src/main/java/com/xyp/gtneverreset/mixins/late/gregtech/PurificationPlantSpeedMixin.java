package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant;

@Mixin(value = MTEPurificationPlant.class, remap = false)
public abstract class PurificationPlantSpeedMixin {

    @ModifyConstant(
        method = "startCycle",
        constant = @Constant(intValue = MTEPurificationPlant.CYCLE_TIME_TICKS),
        require = 1,
        allow = 1)
    private int gtneverreset$modifyPurificationCycleDuration(int duration) {
        ConfigLoader.ensureLoaded();
        return RecipeSpeed.getModifiedRecipeDuration(duration);
    }
}
