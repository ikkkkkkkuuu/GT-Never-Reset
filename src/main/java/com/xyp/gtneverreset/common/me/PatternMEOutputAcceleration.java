package com.xyp.gtneverreset.common.me;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.IDualInputHatch;

/** Server-side eligibility shared by pattern assemblies and their output caches. */
public final class PatternMEOutputAcceleration {

    private static final Map<IGrid, GridCheck> GRID_CHECKS = new WeakHashMap<>();

    private PatternMEOutputAcceleration() {}

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
