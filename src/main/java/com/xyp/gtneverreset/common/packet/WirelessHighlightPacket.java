package com.xyp.gtneverreset.common.packet;

import net.minecraft.client.Minecraft;

import com.xyp.gtneverreset.client.wireless.WirelessWorldHighlightRender;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class WirelessHighlightPacket extends ClientboundPacket {

    private int dimension, x, y, z;

    public WirelessHighlightPacket() {}

    public WirelessHighlightPacket(int dimension, int x, int y, int z) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    protected void read(ByteBuf buffer) {
        dimension = buffer.readInt();
        x = buffer.readInt();
        y = buffer.readInt();
        z = buffer.readInt();
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(dimension);
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection)
                WirelessWorldHighlightRender.afterTeleport(minecraft, dimension, x, y, z);
        });
    }

}
