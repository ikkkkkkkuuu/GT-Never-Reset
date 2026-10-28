package com.xyp.gtneverreset.mixins.late.structurelib;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.xyp.gtneverreset.utils.structure.StructureTooltipHooks;

@Mixin(targets = "com.gtnewhorizon.structurelib.structure.LazyStructureElement", remap = false)
public abstract class TooltipLazyStructureElementMixin {

    @Shadow
    private IStructureElement<Object> get(Object context) {
        throw new AssertionError();
    }

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtneverreset$describeDetachedElement(Object context, CallbackInfoReturnable<List<String>> cir) {
        if (StructureTooltipHooks.isRecording(context)) cir.setReturnValue(get(context).getDescription(context));
    }
}
