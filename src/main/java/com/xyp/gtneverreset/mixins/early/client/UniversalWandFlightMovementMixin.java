package com.xyp.gtneverreset.mixins.early.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.wand.UniversalWandUtilityHandler;

@Mixin(EntityPlayerSP.class)
public abstract class UniversalWandFlightMovementMixin {

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void gtneverreset$dampenFlightInertia(CallbackInfo ci) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        if (!player.capabilities.isFlying || !UniversalWandUtilityHandler.shouldOverrideFlightSpeed(player)) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        GameSettings settings = minecraft.gameSettings;
        boolean inputActive = minecraft.currentScreen == null && minecraft.inGameHasFocus;
        // Preserve acceleration while moving; retain a short glide with extra drag after release.
        if (!inputActive
            || !(GameSettings.isKeyDown(settings.keyBindForward) || GameSettings.isKeyDown(settings.keyBindBack)
                || GameSettings.isKeyDown(settings.keyBindLeft)
                || GameSettings.isKeyDown(settings.keyBindRight))) {
            player.motionX *= 0.5D;
            player.motionZ *= 0.5D;
        }
        if (!inputActive
            || !(GameSettings.isKeyDown(settings.keyBindJump) || GameSettings.isKeyDown(settings.keyBindSneak))) {
            player.motionY *= 0.5D;
        }
    }
}
