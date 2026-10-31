package com.xyp.gtneverreset.common.wireless;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.misc.WirelessNetworkManager;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

public final class WirelessPowerTransfer {

    private static final BigInteger MAX_TRANSFER = BigInteger.valueOf(Long.MAX_VALUE);

    private WirelessPowerTransfer() {}

    public static boolean supports(BaseMetaTileEntity tile) {
        return tile.getMetaTileEntity() instanceof MetaTileEntity meta && !(meta instanceof MTEHatchWirelessMulti)
            && meta.isElectric()
            && (meta.isEnetInput() || meta instanceof MTEHatchEnergyMulti)
            && tile.getInputVoltage() > 0
            && tile.getInputAmperage() > 0
            && tile.getEUCapacity() > 0;
    }

    public static boolean connect(BaseMetaTileEntity tile, EntityPlayer player, UUID source) {
        if (player.worldObj.isRemote || !supports(tile)
            || !tile.isUseableByPlayer(player)
            || !player.canPlayerEdit(tile.xCoord, tile.yCoord, tile.zCoord, 1, player.getCurrentEquippedItem())
            || !WirelessPowerSavedData.sourceExists(source)) return false;
        WirelessPowerSavedData data = WirelessPowerSavedData.get();
        if (data == null) return false;
        var old = data.connection(((IWirelessPowerReceiver) tile).gtnr$powerConnection());
        if (old != null && !old.owner.equals(player.getUniqueID())) return false;
        data.remember(player.getUniqueID(), player.getCommandSenderName());
        data.connect(tile, player.getUniqueID(), source);
        return true;
    }

    public static void tick(BaseMetaTileEntity tile, UUID id) {
        if (tile.isClientSide() || tile.isInvalid()) return;
        WirelessPowerSavedData data = WirelessPowerSavedData.get();
        if (data == null) return;
        var connection = data.connection(id);
        if (connection == null || !connection.matches(tile)) {
            ((IWirelessPowerReceiver) tile).gtnr$setPowerConnection(null);
            tile.markDirty();
            return;
        }
        connection.observed(tile, 0);
        if (connection.paused() || !supports(tile)
            || !tile.canAcceptEnergyThisTick()
            || !WirelessPowerSavedData.sourceExists(connection.source)) return;
        long voltage = tile.getInputVoltage();
        long room = Math.max(0, tile.getEUCapacity() - tile.getStoredEU());
        if (room < voltage) return;
        long available = WirelessNetworkManager.getUserEU(connection.source)
            .min(MAX_TRANSFER)
            .longValue();
        long amperes = offeredAmperes(voltage, tile.getInputAmperage(), room, available);
        if (amperes == 0) return;
        long reserved = voltage * amperes;
        if (!WirelessNetworkManager.addEUToGlobalEnergyMap(connection.source, -reserved)) return;
        long accepted = 0;
        long before = tile.getStoredEU();
        try {
            // UNKNOWN is GT's internal input: retain its voltage, capacity and shared per-tick amperage checks.
            accepted = Math.max(0, Math.min(amperes, tile.injectEnergyUnits(ForgeDirection.UNKNOWN, voltage, amperes)))
                * voltage;
        } catch (RuntimeException failure) {
            accepted = Math.max(0, Math.min(reserved, tile.getStoredEU() - before));
            throw failure;
        } finally {
            if (accepted < reserved)
                WirelessNetworkManager.addEUToGlobalEnergyMap(connection.source, reserved - accepted);
            connection.observed(tile, accepted);
        }
    }

    public static NBTTagCompound copySettings(TileEntity tile) {
        if (tile == null || tile.getWorldObj() == null || tile.getWorldObj().isRemote) return null;
        WirelessPowerSavedData data = WirelessPowerSavedData.get();
        var connection = data == null ? null : data.connectionAt(tile);
        if (connection == null) return null;
        NBTTagCompound settings = new NBTTagCompound();
        settings.setString("source", connection.source.toString());
        settings.setInteger("metaId", connection.metaId);
        settings.setBoolean("paused", connection.paused());
        return settings;
    }

    public static void pasteSettings(EntityPlayer player, TileEntity target, NBTTagCompound settings) {
        if (settings == null || player == null
            || player instanceof FakePlayer
            || !(target instanceof BaseMetaTileEntity tile)
            || tile.getWorldObj() == null
            || tile.getWorldObj().isRemote
            || tile.isInvalid()
            || player.worldObj != tile.getWorldObj()
            || tile.getMetaTileID() != settings.getInteger("metaId")
            || !supports(tile)
            || !tile.playerOwnsThis(player, false)
            || !player.canPlayerEdit(tile.xCoord, tile.yCoord, tile.zCoord, 1, player.getCurrentEquippedItem())
            || tile.getWorldObj()
                .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile)
            return;
        UUID source = WirelessPowerSavedData.uuid(settings, "source");
        if (!WirelessPowerSavedData.sourceExists(source)) return;
        WirelessPowerSavedData data = WirelessPowerSavedData.get();
        if (data == null) return;
        var old = data.connection(((IWirelessPowerReceiver) tile).gtnr$powerConnection());
        boolean paused = settings.getBoolean("paused");
        if (old != null && (!old.owner.equals(player.getUniqueID())
            || old.matches(tile) && old.source.equals(source) && old.paused() == paused)) return;
        data.remember(player.getUniqueID(), player.getCommandSenderName());
        var connection = data.connect(tile, player.getUniqueID(), source);
        if (paused) data.toggle(connection.id, connection.owner);
    }

    public static long offeredAmperes(long voltage, long limit, long room, long available) {
        if (voltage <= 0 || limit <= 0 || room <= 0 || available <= 0) return 0;
        return Math.min(limit, Math.min(room / voltage, available / voltage));
    }

    public static final class TransferRate {

        private static final int WINDOW_TICKS = 20;
        private final long[] samples = new long[WINDOW_TICKS];
        private long lastTick = Long.MIN_VALUE;

        public void record(long tick, long transferred) {
            if (lastTick == Long.MIN_VALUE || tick < lastTick || tick - lastTick >= WINDOW_TICKS) {
                Arrays.fill(samples, 0);
            } else {
                for (int offset = 1; offset <= tick - lastTick; offset++) samples[index(lastTick + offset)] = 0;
            }
            // The transfer path first records zero, then replaces it with the amount actually accepted this tick.
            samples[index(tick)] = Math.max(0, transferred);
            lastTick = tick;
        }

        public double average(long tick) {
            if (lastTick == Long.MIN_VALUE || tick < lastTick || tick - lastTick >= WINDOW_TICKS) return 0;
            double total = 0;
            for (int age = 0; age < WINDOW_TICKS - (tick - lastTick); age++) total += samples[index(lastTick - age)];
            return total / WINDOW_TICKS;
        }

        private static int index(long tick) {
            return (int) Math.floorMod(tick, (long) WINDOW_TICKS);
        }
    }
}
