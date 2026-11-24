package com.xyp.gtnrlib.network;

import net.minecraft.client.Minecraft;

import com.xyp.gtnrlib.compat.gtneverreset.ProjectorArmAnimation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class ProjectorArmStatePacket extends ClientboundPacket {

    private int entity, dimension;
    private boolean open;

    public ProjectorArmStatePacket() {}

    ProjectorArmStatePacket(int entity, int dimension, boolean open) {
        this.entity = entity;
        this.dimension = dimension;
        this.open = open;
    }

    @Override
    protected void read(ByteBuf buffer) {
        entity = buffer.readInt();
        dimension = buffer.readInt();
        open = buffer.readBoolean();
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(entity);
        buffer.writeInt(dimension);
        buffer.writeBoolean(open);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        ProjectorArmAnimation.receivePose(minecraft, entity, dimension, open);
    }
}
