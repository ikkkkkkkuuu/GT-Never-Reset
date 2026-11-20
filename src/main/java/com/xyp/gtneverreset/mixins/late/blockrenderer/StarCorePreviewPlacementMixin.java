package com.xyp.gtneverreset.mixins.late.blockrenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtneverreset.common.machine.multiblock.StarCoreStripper;

import blockrenderer6343.integration.gregtech.GTGuiMultiblockHandler;
import blockrenderer6343.integration.nei.GuiMultiblockHandler;

@Mixin(value = GTGuiMultiblockHandler.class, remap = false)
public abstract class StarCorePreviewPlacementMixin extends GuiMultiblockHandler {

    @ModifyExpressionValue(
        method = "placeMultiblock",
        at = @At(value = "FIELD", target = "Lcom/gtnewhorizon/gtnhlib/blockpos/BlockPos;y:I"),
        require = 1)
    private int gtneverreset$previewControllerHeight(int original) {
        return renderingController instanceof StarCoreStripper ? StarCoreStripper.PREVIEW_CONTROLLER_Y : original;
    }
}
