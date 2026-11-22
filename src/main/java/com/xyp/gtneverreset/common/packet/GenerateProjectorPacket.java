package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.projector.ProjectorBuilder;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public class GenerateProjectorPacket extends ServerboundPacket {

    private int location;
    private final boolean batch;

    public GenerateProjectorPacket() {
        this(0);
    }

    public GenerateProjectorPacket(int location) {
        this(location, false);
    }

    protected GenerateProjectorPacket(int location, boolean batch) {
        this.location = location;
        this.batch = batch;
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
        ProjectorBuilder.request(player, location, batch);
    }
}
