package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.wireless.WirelessMonitorService;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

/** Requests the sender's own network; clients cannot supply another player's UUID. */
public final class WirelessMonitorRequestPacket extends ServerboundPacket {

    public long requestId;

    public WirelessMonitorRequestPacket() {}

    public WirelessMonitorRequestPacket(long requestId) {
        this.requestId = requestId;
    }

    @Override
    protected void read(ByteBuf buffer) {
        requestId = buffer.readLong();
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing wireless monitor request data");
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeLong(requestId);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        WirelessMonitorService.enqueue(player, requestId);
    }
}
