package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public final class ToggleInterfaceVisibilityPacket extends ServerboundPacket {

    private long entryId;

    public ToggleInterfaceVisibilityPacket() {}

    public ToggleInterfaceVisibilityPacket(long entryId) {
        this.entryId = entryId;
    }

    @Override
    protected void read(ByteBuf buffer) {
        entryId = buffer.readLong();
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeLong(entryId);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (player.openContainer instanceof QuickEncodingTerminalContainer container) {
            container.toggleTrackedInterfaceVisibility(entryId);
        }
    }
}
