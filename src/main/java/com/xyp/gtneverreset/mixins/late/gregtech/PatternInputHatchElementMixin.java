package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputSlave;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(targets = "gregtech.api.enums.HatchElement$3", remap = false)
public abstract class PatternInputHatchElementMixin {

    @Inject(method = "mteClasses", at = @At("RETURN"), cancellable = true)
    private void gtneverreset$includePatternInputs(
        CallbackInfoReturnable<List<? extends Class<? extends IMetaTileEntity>>> cir) {
        List<Class<? extends IMetaTileEntity>> classes = new ArrayList<>(cir.getReturnValue());
        classes.add(SuperMTEHatchCraftingInputME.class);
        classes.add(SuperMTEHatchCraftingInputSlave.class);
        cir.setReturnValue(Collections.unmodifiableList(classes));
    }

    @Inject(
        method = "count(Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;)J",
        at = @At("RETURN"),
        cancellable = true)
    private void gtneverreset$countMirrors(MTEMultiBlockBase controller, CallbackInfoReturnable<Long> cir) {
        long mirrors = controller.mDualInputHatches.stream()
            .filter(hatch -> hatch instanceof SuperMTEHatchCraftingInputSlave && !hatch.supportsFluids())
            .count();
        cir.setReturnValue(cir.getReturnValue() + mirrors);
    }
}
