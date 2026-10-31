package com.xyp.gtneverreset.common.wireless;

import java.util.Set;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

public interface WirelessHatchOwner {

    void wirelessFrequency$setController(MTEMultiBlockBase controller);

    Set<MTEMultiBlockBase> wirelessFrequency$getControllers();
}
