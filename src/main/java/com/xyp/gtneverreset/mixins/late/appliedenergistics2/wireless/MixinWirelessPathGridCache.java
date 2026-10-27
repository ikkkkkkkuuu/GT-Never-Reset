// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtneverreset.mixins.late.appliedenergistics2.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.utils.machine.wireless.IWirelessPathingState;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.me.cache.PathGridCache;

@Mixin(value = PathGridCache.class, remap = false)
public abstract class MixinWirelessPathGridCache implements IWirelessPathingState {

    @Shadow
    private boolean updateNetwork;

    @Shadow
    private boolean recalculateControllerNextTick;

    @Unique
    private long gtnr$topologyRevision;

    @Override
    public boolean gtnr$isRepathPending() {
        return updateNetwork || recalculateControllerNextTick;
    }

    @Override
    public long gtnr$topologyRevision() {
        return gtnr$topologyRevision;
    }

    @Override
    public void gtnr$topologyChanged() {
        gtnr$topologyRevision++;
    }

    @Inject(method = { "addNode", "removeNode" }, at = @At("HEAD"))
    private void gtnr$membershipChanged(IGridNode node, IGridHost machine, CallbackInfo ci) {
        gtnr$topologyRevision++;
    }
}
