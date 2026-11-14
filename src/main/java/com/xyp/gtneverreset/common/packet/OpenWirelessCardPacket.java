package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.wireless.WirelessAutoConnect;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public final class OpenWirelessCardPacket extends ServerboundPacket {

    public OpenWirelessCardPacket() {}

    @Override
    protected void read(ByteBuf buffer) {
        if (buffer.isReadable()) throw new IllegalArgumentException("Unexpected wireless card payload");
    }

    @Override
    protected void write(ByteBuf buffer) {}

    @Override
    public void handleServer(EntityPlayerMP player) {
        WirelessAutoConnect.openGui(player);
    }
}
