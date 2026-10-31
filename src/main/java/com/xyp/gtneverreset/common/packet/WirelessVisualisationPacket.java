package com.xyp.gtneverreset.common.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.client.wireless.WirelessCardInput;
import com.xyp.gtneverreset.common.wireless.WirelessCardBinding;
import com.xyp.gtneverreset.common.wireless.WirelessCardVisualisation;

import appeng.core.sync.AppEngPacket;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class WirelessVisualisationPacket implements IMessage {

    private NBTTagCompound bindingTag;
    private int dimension;
    private byte[] payload;

    public WirelessVisualisationPacket() {}

    public WirelessVisualisationPacket(NBTTagCompound tag, int dimension, AppEngPacket packet) {
        bindingTag = new NBTTagCompound();
        WirelessCardBinding binding = WirelessCardBinding.read(tag);
        if (binding == null) throw new IllegalArgumentException("Unbound wireless frequency card");
        binding.write(bindingTag);
        this.dimension = dimension;
        ByteBuf nativePayload = packet.getProxy()
            .payload();
        try {
            int size = nativePayload.readableBytes() - 4;
            if (size < 0 || size > WirelessCardVisualisation.MAX_PACKET_BYTES) {
                payload = new byte[0];
            } else {
                payload = new byte[size];
                nativePayload.getBytes(nativePayload.readerIndex() + 4, payload);
            }
        } finally {
            nativePayload.release();
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        bindingTag = ByteBufUtils.readTag(buffer);
        dimension = buffer.readInt();
        int size = buffer.readInt();
        if (WirelessCardBinding.read(bindingTag) == null || size < 0
            || size > WirelessCardVisualisation.MAX_PACKET_BYTES
            || size > buffer.readableBytes())
            throw new IllegalArgumentException("Invalid wireless visualisation snapshot");
        payload = new byte[size];
        buffer.readBytes(payload);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeTag(buffer, bindingTag);
        buffer.writeInt(dimension);
        buffer.writeInt(payload.length);
        buffer.writeBytes(payload);
    }

    @SideOnly(Side.CLIENT)
    private void handleClient() {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection) WirelessCardInput
                .receiveVisualisation(minecraft, WirelessCardBinding.read(bindingTag), dimension, payload);
        });
    }

    public static final class Handler implements IMessageHandler<WirelessVisualisationPacket, IMessage> {

        @Override
        public IMessage onMessage(WirelessVisualisationPacket message, MessageContext context) {
            message.handleClient();
            return null;
        }
    }
}
