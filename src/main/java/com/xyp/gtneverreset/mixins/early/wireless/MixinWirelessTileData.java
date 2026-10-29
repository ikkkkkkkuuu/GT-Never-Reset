package com.xyp.gtneverreset.mixins.early.wireless;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.utils.machine.wireless.IWirelessTileData;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessFrequencies;
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

    @Override
    public boolean gtnr$hasFrequencyData() {
        return gtnr$wirelessData != null && gtnr$wirelessData.hasKey(FREQUENCY_KEY, 10);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void gtnr$readWireless(NBTTagCompound data, CallbackInfo ci) {
        gtnr$wirelessData = IWirelessTileData.load(data);
    }

    @Inject(method = "validate", at = @At("RETURN"))
    private void gtnr$placedWireless(CallbackInfo ci) {
        TileEntity tile = (TileEntity) (Object) this;
        if (gtnr$hasFrequencyData()) WirelessFrequencies.register(tile);
        else WirelessFrequencies.controllerChanged(tile);
        if (gtnr$hasWirelessData()) WirelessLinkPersistence.placed(tile);
    }

    @Inject(method = "invalidate", at = @At("RETURN"))
    private void gtnr$removedWireless(CallbackInfo ci) {
        WirelessFrequencies.controllerChanged((TileEntity) (Object) this);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void gtnr$writeWireless(NBTTagCompound data, CallbackInfo ci) {
        IWirelessTileData.save(gtnr$wirelessData, data);
    }
}
