// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtneverreset.client.quantum;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.quantum.QuantumCraftingModePacket;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

public final class QuantumCraftingClientConfig {

    private static volatile boolean syncScheduled;

    private QuantumCraftingClientConfig() {}

    public static boolean isQuantumCraftingMode() {
        return MainConfig.client.quantumCrafting.useQuantumCraftingMode;
    }

    public static void setQuantumCraftingMode(boolean enabled) {
        if (isQuantumCraftingMode() == enabled) return;
        MainConfig.client.quantumCrafting.useQuantumCraftingMode = enabled;
        ConfigLoader.save();
    }

    public static void scheduleSync() {
        syncScheduled = true;
    }

    public static void syncIfScheduled() {
        if (!syncScheduled) return;
        syncScheduled = false;
        if (isQuantumCraftingMode()) NetWorkHandler.sendToServer(new QuantumCraftingModePacket(true));
    }
}
