// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtneverreset.utils.machine.wireless;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public interface IWirelessTileData {

    String KEY = "GTNGWirelessLinks";

    NBTTagCompound gtnr$wirelessData();

    boolean gtnr$hasWirelessData();

    static NBTTagCompound of(TileEntity tile) {
        return ((IWirelessTileData) tile).gtnr$wirelessData();
    }
}
