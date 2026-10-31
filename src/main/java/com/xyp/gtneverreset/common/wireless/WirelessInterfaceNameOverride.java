package com.xyp.gtneverreset.common.wireless;

import java.util.function.Supplier;

public interface WirelessInterfaceNameOverride {

    <T> T wirelessFrequency$withoutCustomName(Supplier<T> reader);
}
