package com.xyp.gtneverreset.utils.machine.wireless;

import java.util.function.Supplier;

public interface WirelessInterfaceNameOverride {

    <T> T wirelessFrequency$withoutCustomName(Supplier<T> reader);
}
