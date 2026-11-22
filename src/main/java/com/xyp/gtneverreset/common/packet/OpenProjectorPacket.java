package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.projector.ProjectorServer;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public class OpenProjectorPacket extends ServerboundPacket {

    private int location;

    public OpenProjectorPacket() {}

    public OpenProjectorPacket(int location) {
        this.location = location;
    }

    @Override
    protected void read(ByteBuf buf) {
        location = buf.readInt();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeInt(location);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        ProjectorServer.openGui(player, location);
    }
}
