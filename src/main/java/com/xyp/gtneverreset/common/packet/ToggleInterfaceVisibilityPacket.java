package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class ToggleInterfaceVisibilityPacket implements IMessage {

    private long entryId;

    public ToggleInterfaceVisibilityPacket() {}

    public ToggleInterfaceVisibilityPacket(long entryId) {
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
        if (player.openContainer instanceof QuickEncodingTerminalContainer container) {
            container.toggleTrackedInterfaceVisibility(entryId);
        }
    }
}
