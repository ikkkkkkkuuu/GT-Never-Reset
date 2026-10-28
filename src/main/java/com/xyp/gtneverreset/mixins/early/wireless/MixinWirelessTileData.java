package com.xyp.gtneverreset.mixins.early.wireless;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.utils.machine.wireless.IWirelessTileData;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessLinkPersistence;

@Mixin(TileEntity.class)
public class MixinWirelessTileData implements IWirelessTileData {

    @Unique
    private NBTTagCompound gtnr$wirelessData;

    @Override
    public NBTTagCompound gtnr$wirelessData() {
        if (gtnr$wirelessData == null) gtnr$wirelessData = new NBTTagCompound();
        return gtnr$wirelessData;
    }

    @Override
    public boolean gtnr$hasWirelessData() {
        return gtnr$wirelessData != null && gtnr$wirelessData.hasKey(KEY, 10);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void gtnr$readWireless(NBTTagCompound data, CallbackInfo ci) {
        gtnr$wirelessData = null;
        if (data.hasKey(KEY, 10)) gtnr$wirelessData().setTag(
            KEY,
            data.getCompoundTag(KEY)
                .copy());
    }

    @Inject(method = "validate", at = @At("RETURN"))
    private void gtnr$placedWireless(CallbackInfo ci) {
        if (gtnr$hasWirelessData()) WirelessLinkPersistence.placed((TileEntity) (Object) this);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void gtnr$writeWireless(NBTTagCompound data, CallbackInfo ci) {
        if (gtnr$wirelessData != null && gtnr$wirelessData.hasKey(KEY, 10)) {
            data.setTag(
                KEY,
                gtnr$wirelessData.getCompoundTag(KEY)
                    .copy());
        } else {
            data.removeTag(KEY);
        }
    }
}
