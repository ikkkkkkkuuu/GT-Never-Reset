package com.xyp.gtneverreset.common.machine.multiblock.crop;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.reference.Names;

public final class ArchivedSeedStats extends SeedStats {

    public static final String ARCHIVE_MARKER = "gtneverresetArchiveSeed";

    public ArchivedSeedStats(byte growth, byte gain, byte resistance, boolean analyzed) {
        super(growth, gain, resistance, analyzed);
    }

    public static ArchivedSeedStats initial() {
        return new ArchivedSeedStats((byte) 1, (byte) 1, (byte) 1, true);
    }

    public static ArchivedSeedStats fromTag(NBTTagCompound tag) {
        return new ArchivedSeedStats(
            tag.getByte(Names.NBT.growth),
            tag.getByte(Names.NBT.gain),
            tag.getByte(Names.NBT.resistance),
            tag.getBoolean(Names.NBT.analyzed));
    }

    @Override
    public ArchivedSeedStats copy() {
        return new ArchivedSeedStats(getGrowth(), getGain(), getResistance(), isAnalyzed());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setBoolean(ARCHIVE_MARKER, true);
        return super.writeToNBT(tag);
    }
}
