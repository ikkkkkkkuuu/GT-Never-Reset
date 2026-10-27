package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtneverreset.config.MainConfig;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;

@Mixin(value = MTEBasicMachine.class, remap = false)
public class BasicMachineOutputMixin {

    @ModifyConstant(method = "onPostTick", constant = @Constant(intValue = 1000), remap = false)
    private int gtneverreset$modifyAutoOutputFluidAmount(int constant) {
        if (MainConfig.recipeSpeed.fullFluidOutput) {
            return Integer.MAX_VALUE;
        }
        return constant;
    }
}
