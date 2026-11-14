package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.item.ToolBeltData;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public class ToolBeltSwapPacket extends ServerboundPacket {

    private int swapWith;

    public ToolBeltSwapPacket() {}

    public ToolBeltSwapPacket(int swapWith) {
        this.swapWith = swapWith;
    }

    @Override
    protected void read(ByteBuf buf) {
        swapWith = buf.readInt();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeInt(swapWith);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        ToolBeltData.swapItem(swapWith, player);
    }

}
