package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.projector.ProjectorPlacementUpdates;

@Mixin(PlayerManager.class)
public abstract class ProjectorPlayerManagerMixin {

    @Shadow
    @Final
    private WorldServer theWorldServer;

    @Inject(method = "markBlockForUpdate", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnr$holdProjectedBlock(int x, int y, int z, CallbackInfo ci) {
        if (ProjectorPlacementUpdates.isHeld(theWorldServer, x, y, z)) ci.cancel();
    }
}
