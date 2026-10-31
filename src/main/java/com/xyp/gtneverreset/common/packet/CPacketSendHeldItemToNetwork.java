package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class CPacketSendHeldItemToNetwork implements IMessage {

    private int slot;

    public CPacketSendHeldItemToNetwork() {}

    public CPacketSendHeldItemToNetwork(int slot) {
        this.slot = slot;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        slot = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(slot);
    }

    public void handle(EntityPlayerMP player) {
        WirelessTerminalSession.storeHeldItem(player, slot);
    }
}
