package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.wireless.WirelessAutoConnect;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class OpenWirelessCardPacket implements IMessage {

    public OpenWirelessCardPacket() {}

    @Override
    public void fromBytes(ByteBuf buffer) {
        if (buffer.isReadable()) throw new IllegalArgumentException("Unexpected wireless card payload");
    }

    @Override
    public void toBytes(ByteBuf buffer) {}

    public void handle(EntityPlayerMP player) {
        WirelessAutoConnect.openGui(player);
    }
}
