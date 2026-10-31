package com.xyp.gtneverreset.common.crafting.quantum.core;

import java.util.concurrent.TimeUnit;

import net.minecraft.server.MinecraftServer;

import com.xyp.gtneverreset.config.QuantumCraftingConfig;

public final class AEConfig {

    public static final Crafting CRAFTING = new Crafting();
    private static final AEConfig INSTANCE = new AEConfig();

    public static AEConfig instance() {
        return INSTANCE;
    }

    public long getCraftingFallbackNanos() {
        return TimeUnit.SECONDS.toNanos(Math.max(0, QuantumCraftingConfig.calculationFallbackSeconds));
    }

    public long getCalculationLimitNanos() {
        var server = MinecraftServer.getServer();
        if (server == null || !server.isDedicatedServer()) {
            return 0;
        }
        return TimeUnit.SECONDS.toNanos(Math.max(0, QuantumCraftingConfig.calculationLimitSeconds));
    }

    public boolean isCraftingPerformanceLogEnabled() {
        return QuantumCraftingConfig.enableCalculationPerformanceLog;
    }

    public static final class Crafting {

        public int craftingCalculationPausingInterval = 10000;
    }
}
