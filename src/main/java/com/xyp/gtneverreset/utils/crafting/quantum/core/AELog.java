package com.xyp.gtneverreset.utils.crafting.quantum.core;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.config.MainConfig;

public final class AELog {

    public static boolean isCraftingLogEnabled() {
        return MainConfig.quantumCrafting.enableCalculationLog;
    }

    public static void crafting(String message) {
        GTNeverReset.LOG.info("[QuantumCrafting] {}", message);
    }

    public static void craftingDebug(String message) {
        GTNeverReset.LOG.debug("[QuantumCrafting] {}", message);
    }

    public static void craftingPerformance(String format, Object... params) {
        GTNeverReset.LOG.info("[QuantumCrafting] {}", String.format(format, params));
    }

    public static void info(String format, Object... params) {
        GTNeverReset.LOG.info("[QuantumCrafting] {}", String.format(format, params));
    }

    public static void info(Throwable throwable, String message) {
        GTNeverReset.LOG.info("[QuantumCrafting] {}", message, throwable);
    }
}
