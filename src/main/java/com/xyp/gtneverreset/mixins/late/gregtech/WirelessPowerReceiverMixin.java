package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.common.wireless.IWirelessPowerReceiver;
import com.xyp.gtneverreset.common.wireless.WirelessPowerSavedData;
import com.xyp.gtneverreset.common.wireless.WirelessPowerTransfer;

import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mixin(value = BaseMetaTileEntity.class, remap = false)
public abstract class WirelessPowerReceiverMixin implements IWirelessPowerReceiver {

    @Unique
    private UUID gtnr$powerConnection;

    @Override
    public UUID gtnr$powerConnection() {
        return gtnr$powerConnection;
    }

    @Override
    public void gtnr$setPowerConnection(UUID connection) {
        gtnr$powerConnection = connection;
    }

    @Inject(method = "doPostTick", at = @At("HEAD"))
    private void gtnr$supplyWirelessPower(CallbackInfo ci) {
        if (gtnr$powerConnection != null)
            WirelessPowerTransfer.tick((BaseMetaTileEntity) (Object) this, gtnr$powerConnection);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"), remap = true)
    private void gtnr$readWirelessPower(NBTTagCompound tag, CallbackInfo ci) {
        gtnr$powerConnection = WirelessPowerSavedData.uuid(tag, "GTNRWirelessPower");
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"), remap = true)
    private void gtnr$writeWirelessPower(NBTTagCompound tag, CallbackInfo ci) {
        if (gtnr$powerConnection != null) tag.setString("GTNRWirelessPower", gtnr$powerConnection.toString());
        else tag.removeTag("GTNRWirelessPower");
    }
}
