package com.xyp.gtneverreset.utils.machine.wireless;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public interface IWirelessTileData {

    String KEY = "GTNGWirelessLinks";
    String FREQUENCY_KEY = "GTNRWirelessFrequency";
    String[] KEYS = { KEY, FREQUENCY_KEY };

    NBTTagCompound gtnr$wirelessData();

    boolean gtnr$hasWirelessData();

    boolean gtnr$hasFrequencyData();

    static NBTTagCompound of(TileEntity tile) {
        return ((IWirelessTileData) tile).gtnr$wirelessData();
    }

    static NBTTagCompound load(NBTTagCompound saved) {
        NBTTagCompound data = null;
        for (String key : KEYS) {
            if (!saved.hasKey(key, 10)) continue;
            if (data == null) data = new NBTTagCompound();
            data.setTag(
                key,
                saved.getCompoundTag(key)
                    .copy());
        }
        return data;
    }

    static void save(NBTTagCompound data, NBTTagCompound out) {
        for (String key : KEYS) {
            if (data != null && data.hasKey(key, 10)) out.setTag(
                key,
                data.getCompoundTag(key)
                    .copy());
            else out.removeTag(key);
        }
    }
}
