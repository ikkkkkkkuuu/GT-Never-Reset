package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.item.ToolBeltData;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public class ToolBeltResizePacket extends ServerboundPacket {

    private int requestedCount;

    public ToolBeltResizePacket() {}

    public ToolBeltResizePacket(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    @Override
    protected void read(ByteBuf buf) {
        requestedCount = buf.readInt();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeInt(requestedCount);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        ToolBeltData.resize(requestedCount, player);
    }

}
