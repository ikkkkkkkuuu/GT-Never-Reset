package com.xyp.gtneverreset.common.packet;

import net.minecraft.client.Minecraft;

import com.xyp.gtneverreset.client.wireless.WirelessWorldHighlightRender;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class WirelessHighlightPacket implements IMessage {

    private int dimension, x, y, z;

    public WirelessHighlightPacket() {}

    public WirelessHighlightPacket(int dimension, int x, int y, int z) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        dimension = buffer.readInt();
        x = buffer.readInt();
        y = buffer.readInt();
        z = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(dimension);
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
    }

    @SideOnly(Side.CLIENT)
    private void handleClient() {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection)
                WirelessWorldHighlightRender.afterTeleport(minecraft, dimension, x, y, z);
        });
    }

    public static final class Handler implements IMessageHandler<WirelessHighlightPacket, IMessage> {

        @Override
        public IMessage onMessage(WirelessHighlightPacket message, MessageContext context) {
            message.handleClient();
            return null;
        }
    }
}
