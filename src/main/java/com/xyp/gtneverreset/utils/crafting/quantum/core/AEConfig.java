package com.xyp.gtneverreset.utils.crafting.quantum.core;

import java.util.concurrent.TimeUnit;

import net.minecraft.server.MinecraftServer;

import com.xyp.gtneverreset.config.MainConfig;

public final class AEConfig {

    public static final Crafting CRAFTING = new Crafting();
    private static final AEConfig INSTANCE = new AEConfig();

    public static AEConfig instance() {
        return INSTANCE;
    }

    public long getCraftingFallbackNanos() {
        return TimeUnit.SECONDS.toNanos(Math.max(0, MainConfig.quantumCrafting.calculationFallbackSeconds));
    }

    public long getCalculationLimitNanos() {
        var server = MinecraftServer.getServer();
        if (server == null || !server.isDedicatedServer()) {
            return 0;
        }
        return TimeUnit.SECONDS.toNanos(Math.max(0, MainConfig.quantumCrafting.calculationLimitSeconds));
    }

    public boolean isCraftingPerformanceLogEnabled() {
        return MainConfig.quantumCrafting.enableCalculationPerformanceLog;
    }

    public static final class Crafting {

        public int craftingCalculationPausingInterval = 10000;
    }
}
