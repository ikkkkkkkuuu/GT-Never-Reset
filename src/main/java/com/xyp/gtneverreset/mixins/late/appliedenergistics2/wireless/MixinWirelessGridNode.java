// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtneverreset.mixins.late.appliedenergistics2.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessClusterManager;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.me.GridNode;

@Mixin(value = GridNode.class, remap = false)
public abstract class MixinWirelessGridNode {

    @Inject(
        method = { "addConnection", "removeConnection" },
        at = @At(value = "INVOKE", target = "Lappeng/api/networking/IGridConnection;hasDirection()Z"))
    private void gtnr$connectionsChanged(IGridConnection connection, CallbackInfo ci) {
        WirelessChannelManager.connectionsChanged((IGridNode) this, connection);
    }

    @Inject(method = "updateState", at = @At("RETURN"))
    private void gtnr$nodeUpdated(CallbackInfo ci) {
        WirelessClusterManager.nodeChanged((IGridNode) this);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void gtnr$nodeDestroyed(CallbackInfo ci) {
        WirelessClusterManager.nodeChanged((IGridNode) this);
    }
}
