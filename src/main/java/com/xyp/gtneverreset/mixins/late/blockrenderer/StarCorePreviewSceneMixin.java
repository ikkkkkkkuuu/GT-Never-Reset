package com.xyp.gtneverreset.mixins.late.blockrenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.xyp.gtneverreset.common.machine.multiblock.StarCoreStripper;

import blockrenderer6343.client.renderer.ImmediateWorldSceneRenderer;
import blockrenderer6343.integration.nei.GuiMultiblockHandler;

@Mixin(value = GuiMultiblockHandler.class, remap = false)
public abstract class StarCorePreviewSceneMixin {

    @Shadow
    protected IConstructable renderingController;
    @Shadow
    protected ImmediateWorldSceneRenderer renderer;
    @Shadow
    protected float zoom;

    @Inject(
        method = "initializeSceneRenderer",
        at = @At(value = "INVOKE", target = "Lblockrenderer6343/integration/nei/GuiMultiblockHandler;updateCamera()V"),
        require = 1)
    private void gtneverreset$fitStarCorePreview(boolean resetCamera, CallbackInfo ci) {
        // At the renderer's 60 degree field of view the box diagonal fits its bounding sphere on screen.
        if (resetCamera && renderingController instanceof StarCoreStripper) zoom = renderer.world.getSize()
            .length();
    }

    @ModifyArg(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lblockrenderer6343/client/utils/BRUtil;projectMultiblock(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;I)V"),
        index = 2,
        require = 1)
    private int gtneverreset$blocksBelowPreviewController(int blocksBelow) {
        return renderingController instanceof StarCoreStripper
            ? blocksBelow + StarCoreStripper.PREVIEW_CONTROLLER_Y - GuiMultiblockHandler.MB_PLACE_POS.y
            : blocksBelow;
    }
}
