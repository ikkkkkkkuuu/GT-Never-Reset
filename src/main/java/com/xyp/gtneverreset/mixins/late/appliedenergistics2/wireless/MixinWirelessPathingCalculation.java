package com.xyp.gtneverreset.mixins.late.appliedenergistics2.wireless;

import java.util.Map;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xyp.gtneverreset.utils.machine.wireless.ChannelBudget;
import com.xyp.gtneverreset.utils.machine.wireless.IWirelessPathingAccess;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager;

import appeng.api.networking.IGrid;
import appeng.me.GridConnection;
import appeng.me.GridNode;
import appeng.me.pathfinding.PathingCalculation;

@Mixin(value = PathingCalculation.class, remap = false)
public abstract class MixinWirelessPathingCalculation implements IWirelessPathingAccess {

    @Unique
    private ChannelBudget gtnr$wirelessBudget;

    @Unique
    private IGrid gtnr$wirelessGrid;

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)"
                + "Ljava/lang/Object;"),
        require = 2,
        allow = 2)
    private Object gtnr$independentEntrance(Map<Object, Object> faces, Object direction,
        Function<Object, Object> factory, Operation<Object> original, @Local(name = "gc") GridConnection connection) {
        return WirelessChannelManager.isOwned(connection) ? factory.apply(direction)
            : original.call(faces, direction, factory);
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void gtnr$initializeBudget(IGrid grid, CallbackInfo ci) {
        gtnr$wirelessGrid = grid;
        WirelessChannelManager.invalidateAllocation(grid);
        if (WirelessChannelManager.channelsEnabled() && WirelessChannelManager.manages(grid)) {
            gtnr$wirelessBudget = new ChannelBudget(WirelessChannelManager.capacity(grid));
        }
    }

    @Inject(method = "tryUseChannel", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnr$checkBudget(GridNode start, CallbackInfoReturnable<Boolean> cir) {
        if (gtnr$wirelessBudget != null && !gtnr$wirelessBudget.hasRemaining()) cir.setReturnValue(false);
    }

    @Inject(method = "tryUseChannel", at = @At("RETURN"), require = 1)
    private void gtnr$countAcceptedChannel(GridNode start, CallbackInfoReturnable<Boolean> cir) {
        if (gtnr$wirelessBudget != null && cir.getReturnValueZ()) gtnr$wirelessBudget.recordAllocation();
    }

    @Inject(method = "compute", at = @At("RETURN"), require = 1)
    private void gtnr$publishAllocation(CallbackInfo ci) {
        WirelessChannelManager.recordAllocation(
            gtnr$wirelessGrid,
            gtnr$wirelessBudget == null ? WirelessChannelManager.capacity(gtnr$wirelessGrid)
                : gtnr$wirelessBudget.capacity(),
            gtnr$wirelessBudget == null ? ((PathingCalculation) (Object) this).getChannelsInUse()
                : gtnr$wirelessBudget.allocated());
    }
}
