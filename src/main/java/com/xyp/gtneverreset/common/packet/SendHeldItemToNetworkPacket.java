package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public final class SendHeldItemToNetworkPacket extends ServerboundPacket {

    private int slot;

    public SendHeldItemToNetworkPacket() {}

    public SendHeldItemToNetworkPacket(int slot) {
        this.slot = slot;
    }

    @Override
    protected void read(ByteBuf buffer) {
        slot = buffer.readInt();
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(slot);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        WirelessTerminalSession.storeHeldItem(player, slot);
    }
}
