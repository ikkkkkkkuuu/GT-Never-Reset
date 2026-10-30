package com.xyp.gtneverreset.utils.enums;

public enum WirelessTerminalGuiType {

    WirelessDualInterfaceTerminal,
    WirelessCraftingTerminal,
    WirelessMagnetFilter;

    public static WirelessTerminalGuiType getByOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }
}
