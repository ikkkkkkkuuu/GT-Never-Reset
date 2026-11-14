package com.xyp.gtneverreset.mixins.late.gregtech;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

import gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant;

@Mixin(value = MTEPurificationPlant.class, remap = false)
public abstract class PurificationPlantSpeedMixin {

    // Adjust the normal cycle after assignment so constant modifiers from other mods can coexist.
    // The debug branch skips this write; linked units receive the adjusted duration afterwards.
    @Inject(
        method = "startCycle",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/common/tileentities/machines/multi/purification/MTEPurificationPlant;mMaxProgresstime:I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 1,
            shift = At.Shift.AFTER),
        require = 1,
        allow = 1)
    private void gtneverreset$modifyPurificationCycleDuration(CallbackInfo ci) {
        ConfigLoader.ensureLoaded();
        MTEPurificationPlant plant = (MTEPurificationPlant) (Object) this;
        plant.mMaxProgresstime = RecipeSpeed.getModifiedRecipeDuration(plant.mMaxProgresstime);
    }
}
