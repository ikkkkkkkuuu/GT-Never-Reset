package com.xyp.gtneverreset.api.packaged;

import net.minecraft.nbt.NBTTagCompound;

/** Implemented by the TC4 matrix mixin; stores a server-only, persistent direct-supply job reference. */
public interface InfusionSourceAccess {

    NBTTagCompound gtnr$getEssentiaSource();

    void gtnr$setEssentiaSource(NBTTagCompound source);
}
