package com.xyp.gtneverreset.client.quantum;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.QuantumCraftingModePacket;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

public final class QuantumCraftingClientState {

    private static volatile boolean syncScheduled;

    public static boolean isQuantumCraftingMode() {
        return ClientConfig.quantumCrafting.useQuantumCraftingMode;
    }

    public static void setQuantumCraftingMode(boolean enabled) {
        if (isQuantumCraftingMode() == enabled) return;
        ClientConfig.quantumCrafting.useQuantumCraftingMode = enabled;
        ConfigLoader.saveClient();
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
