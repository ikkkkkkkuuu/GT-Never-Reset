package com.xyp.gtneverreset.common.me;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;

import com.xyp.gtneverreset.config.GregTechConfig;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;

/** Server-side player-order acceleration shared by pattern assemblies and maximum-capacity ME outputs. */
public final class MEOutputAcceleration {

    private static final Map<IGrid, GridCheck> GRID_CHECKS = new WeakHashMap<>();

    private MEOutputAcceleration() {}

    public static boolean hasPlayerJob(AENetworkProxy proxy) {
        if (!proxy.isActive()) return false;
        try {
            IGrid grid = proxy.getGrid();
            int tick = MinecraftServer.getServer()
                .getTickCounter();
            GridCheck check = GRID_CHECKS.get(grid);
            if (check == null || check.tick != tick) {
                ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
                check = new GridCheck(tick, crafting != null && hasPlayerJob(crafting.getCpus()));
                GRID_CHECKS.put(grid, check);
            }
            return check.hasPlayerJob;
        } catch (GridAccessException ignored) {
            return false;
        }
    }

    static boolean hasPlayerJob(Iterable<? extends ICraftingCPU> cpus) {
        for (ICraftingCPU cpu : cpus) {
            if (!cpu.isBusy()) continue;
            String player = cpu.getSourcePlayer();
            if ((player != null && !player.isEmpty()) || cpu.getCurrentJobSource() instanceof PlayerSource) return true;
        }
        return false;
    }

    public static boolean hasRecentlyWorkingController(Iterable<?> watchers, IDualInputHatch input, int holdTicks) {
        for (Object watcher : watchers) {
            if (watcher instanceof MTEMultiBlockBase controller && controller.isValid()
                && controller.mDualInputHatches.contains(input)
                && isWithinHoldTime(controller.getTotalRuntimeInTicks(), controller.getLastWorkingTick(), holdTicks)) {
                return true;
            }
        }
        return false;
    }

    public static void flushOutput(MTEHatchOutputMEBase<?> provider, Iterable<?> watchers, MTEHatch output) {
        if (!GregTechConfig.patternMEOutputFastFlush || provider.getCachedAmount() <= 0
            || !hasRecentlyWorkingOutputController(watchers, output, GregTechConfig.patternMEOutputFastFlushHoldTicks)
            || !hasPlayerJob(provider.getProxy())) return;
        provider.flushCachedStack();
    }

    private static boolean hasRecentlyWorkingOutputController(Iterable<?> watchers, MTEHatch output, int holdTicks) {
        for (Object watcher : watchers) {
            if (watcher instanceof MTEMultiBlockBase controller && controller.isValid()
                && (controller.mOutputBusses.contains(output) || controller.mOutputHatches.contains(output))
                && isWithinHoldTime(controller.getTotalRuntimeInTicks(), controller.getLastWorkingTick(), holdTicks)) {
                return true;
            }
        }
        return false;
    }

    static boolean isWithinHoldTime(long runtime, long lastWorkingTick, int holdTicks) {
        return lastWorkingTick > 0 && runtime >= lastWorkingTick && runtime - lastWorkingTick <= holdTicks;
    }

    private static final class GridCheck {

        private final int tick;
        private final boolean hasPlayerJob;

        private GridCheck(int tick, boolean hasPlayerJob) {
            this.tick = tick;
            this.hasPlayerJob = hasPlayerJob;
        }
    }
}
