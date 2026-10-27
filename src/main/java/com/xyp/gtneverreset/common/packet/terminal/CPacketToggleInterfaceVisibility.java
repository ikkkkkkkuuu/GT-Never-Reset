package com.xyp.gtneverreset.common.packet.terminal;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.container.ContainerQuickEncodingTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class CPacketToggleInterfaceVisibility implements IMessage {

    private long entryId;

    public CPacketToggleInterfaceVisibility() {}

    public CPacketToggleInterfaceVisibility(long entryId) {
        this.entryId = entryId;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entryId = buffer.readLong();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(entryId);
    }

    public void handle(EntityPlayerMP player) {
        if (player.openContainer instanceof ContainerQuickEncodingTerminal container) {
            container.toggleTrackedInterfaceVisibility(entryId);
        }
    }
}
