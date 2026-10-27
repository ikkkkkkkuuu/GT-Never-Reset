package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.utils.inventory.ToolBeltData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ToolBeltResizePacket implements IMessage {

    private int requestedCount;

    public ToolBeltResizePacket() {}

    public ToolBeltResizePacket(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        requestedCount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(requestedCount);
    }

    public void handle(EntityPlayerMP player) {
        ToolBeltData.resize(requestedCount, player);
    }

}
