package com.xyp.gtneverreset.mixins.late.client;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;

/** The travel interface exposes virtual RF; it does not represent a rechargeable item. */
@Pseudo
@Mixin(targets = "com.caedis.duradisplay.overlay.OverlayDurabilityLike", remap = false)
public abstract class UniversalWandChargeOverlayMixin {

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true, require = 0)
    private void gtneverreset$hideVirtualCharge(ItemStack stack, CallbackInfoReturnable<Object> cir) {
        if (stack != null && stack.getItem() instanceof ItemUniversalWand
            && getClass().getName()
                .equals("com.caedis.duradisplay.overlay.OverlayCharge")) {
            cir.setReturnValue(null);
        }
    }
}
