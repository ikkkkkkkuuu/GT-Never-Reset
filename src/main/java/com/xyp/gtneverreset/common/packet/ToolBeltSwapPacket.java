package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.item.ToolBeltData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ToolBeltSwapPacket implements IMessage {

    private int swapWith;

    public ToolBeltSwapPacket() {}

    public ToolBeltSwapPacket(int swapWith) {
        this.swapWith = swapWith;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        swapWith = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(swapWith);
    }

    public void handle(EntityPlayerMP player) {
        ToolBeltData.swapItem(swapWith, player);
    }

}
