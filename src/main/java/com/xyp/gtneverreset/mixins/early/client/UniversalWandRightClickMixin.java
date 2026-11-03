package com.xyp.gtneverreset.mixins.early.client;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.wand.UniversalWandTravelFallback;

/** Bracket native right-click packets without cancelling or replaying any interaction. */
@Mixin(Minecraft.class)
public abstract class UniversalWandRightClickMixin {

    @Inject(method = "func_147121_ag", at = @At("HEAD"))
    private void gtneverreset$beginWandClick(CallbackInfo ci) {
        UniversalWandTravelFallback.beginClick();
    }

    @Inject(method = "func_147121_ag", at = @At("RETURN"))
    private void gtneverreset$finishWandClick(CallbackInfo ci) {
        UniversalWandTravelFallback.finishClick();
    }
}
