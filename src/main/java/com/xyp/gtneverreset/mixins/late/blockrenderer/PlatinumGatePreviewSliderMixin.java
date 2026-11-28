package com.xyp.gtneverreset.mixins.late.blockrenderer;

import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.xyp.gtneverreset.common.machine.multiblock.PlatinumProcessingPlantController;

import blockrenderer6343.integration.nei.GuiMultiblockHandler;

@Mixin(value = GuiMultiblockHandler.class, remap = false)
public abstract class PlatinumGatePreviewSliderMixin {

    @Shadow
    protected IConstructable renderingController;

    @ModifyArgs(
        method = "initGui",
        at = @At(
            value = "INVOKE",
            target = "Lblockrenderer6343/client/utils/GuiSlider;<init>(Ljava/lang/String;IIIIIII)V",
            ordinal = 0),
        require = 1)
    private void gtneverreset$configureRingSlider(Args args) {
        if (renderingController instanceof PlatinumProcessingPlantController gate) {
            // #tr gtnr.nei.platinum_gate.ring_count
            // # Rings
            // # zh_CN 圈数
            args.set(0, I18n.format("gtnr.nei.platinum_gate.ring_count"));
            // The master slider selects rings. The named coil slider uses its own scanned range.
            args.set(7, gate.getMaxRingCount());
        }
    }
}
