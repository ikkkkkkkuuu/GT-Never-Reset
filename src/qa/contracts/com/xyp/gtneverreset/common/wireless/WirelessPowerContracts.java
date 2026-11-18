package com.xyp.gtneverreset.common.wireless;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import gregtech.common.misc.GlobalVariableStorage;

public final class WirelessPowerContracts {

    public static void main(String[] args) {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        GlobalVariableStorage.GlobalEnergy.clear();
        GlobalVariableStorage.GlobalEnergy.put(null, BigInteger.ZERO);
        GlobalVariableStorage.GlobalEnergy.put(first, BigInteger.ONE);
        GlobalVariableStorage.GlobalEnergy.put(second, BigInteger.TEN);

        WirelessPowerSavedData data = new WirelessPowerSavedData("GTNR_WirelessPowerContracts");
        List<UUID> sources = data.sources();
        require(!sources.contains(null), "an ownerless global energy entry must not become a network source");
        require(sources.contains(first) && sources.contains(second), "real owners must remain listed");
        require(data.name(null).isEmpty(), "an ownerless account must render without a name");
        require(
            data.name(first)
                .equals(first.toString().substring(0, 8)),
            "an unknown player name must fall back to the UUID prefix");

        GlobalVariableStorage.GlobalEnergy.clear();
        System.out.println("Wireless power contracts passed (4 checks)");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
