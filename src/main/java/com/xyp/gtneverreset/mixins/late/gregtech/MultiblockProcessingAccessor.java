package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public interface MultiblockProcessingAccessor {

    @Invoker("setupProcessingLogic")
    void gtnr$setupProcessingLogic(ProcessingLogic logic);
}
