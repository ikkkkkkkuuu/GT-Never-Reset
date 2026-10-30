package com.xyp.gtneverreset.utils.machine;

import java.math.BigInteger;
import java.util.Random;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.xyp.gtneverreset.utils.world.WirelessPowerSavedData;

public final class WirelessPowerContracts {

    public static void main(String[] args) {
        require(WirelessPowerTransfer.offeredAmperes(32, 2, 1000, 63) == 1, "partial account balance");
        require(WirelessPowerTransfer.offeredAmperes(32, 2, 31, 1000) == 0, "cannot overfill the last packet");
        require(WirelessPowerTransfer.offeredAmperes(32, 2, 1000, 1000) == 2, "amperage limit");
        require(WirelessPowerTransfer.offeredAmperes(0, 2, 1000, 1000) == 0, "zero voltage");
        require(WirelessPowerTransfer.offeredAmperes(32, 2, 1000, -1) == 0, "no negative debit");
        Random random = new Random(74019);
        for (int i = 0; i < 10000; i++) {
            long voltage = 1 + (random.nextLong() & Long.MAX_VALUE) / 2;
            long limit = random.nextLong() & Long.MAX_VALUE;
            long room = random.nextLong() & Long.MAX_VALUE;
            long balance = random.nextLong() & Long.MAX_VALUE;
            long amperes = WirelessPowerTransfer.offeredAmperes(voltage, limit, room, balance);
            BigInteger transferred = BigInteger.valueOf(voltage)
                .multiply(BigInteger.valueOf(amperes));
            require(amperes >= 0 && amperes <= limit, "bounded packet count");
            require(
                transferred.compareTo(BigInteger.valueOf(Math.min(room, balance))) <= 0,
                "no overflow or energy creation");
            require(transferred.longValueExact() == voltage * amperes, "safe long multiplication");
        }
        persistence();
        System.out.println("Wireless power contracts passed: 10000 transfer cases and persistence/access checks");
    }

    private static void persistence() {
        UUID id = UUID.randomUUID(), owner = UUID.randomUUID(), source = UUID.randomUUID();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("id", id.toString());
        entry.setString("owner", owner.toString());
        entry.setString("source", source.toString());
        entry.setInteger("dimension", -1);
        entry.setInteger("x", -123);
        entry.setInteger("y", 50);
        entry.setInteger("z", 42);
        entry.setInteger("metaId", 99);
        NBTTagList entries = new NBTTagList();
        entries.appendTag(entry);
        entries.appendTag(new NBTTagCompound());
        NBTTagCompound saved = new NBTTagCompound();
        saved.setTag("connections", entries);
        WirelessPowerSavedData data = new WirelessPowerSavedData();
        data.readFromNBT(saved);
        require(
            data.connections(owner)
                .size() == 1,
            "skip malformed connections");
        data.toggle(id, source);
        require(
            !data.connection(id)
                .paused(),
            "other users cannot pause your devices");
        data.remove(id, source);
        require(data.connection(id) != null, "other users cannot unlink your devices");
        data.toggle(id, owner);
        NBTTagCompound roundTrip = new NBTTagCompound();
        data.writeToNBT(roundTrip);
        WirelessPowerSavedData restored = new WirelessPowerSavedData();
        restored.readFromNBT(roundTrip);
        var connection = restored.connection(id);
        require(
            connection.paused() && connection.source.equals(source) && connection.owner.equals(owner),
            "save account and pause");
        require(
            connection.dimension == -1 && connection.x == -123 && connection.y == 50 && connection.z == 42,
            "save cross-dimensional position");
        require(connection.metaId == 99, "save machine identity");
        require(WirelessPowerSavedData.uuid(new NBTTagCompound(), "id") == null, "missing UUID");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
