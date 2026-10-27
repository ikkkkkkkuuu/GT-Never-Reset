package com.xyp.gtneverreset.common.packet;

import java.math.BigInteger;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.xyp.gtneverreset.client.gui.WirelessMonitorHud;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/** Exact balance and server tick, with a request token to discard replies from an earlier HUD session. */
public final class WirelessMonitorSnapshotPacket implements IMessage {

    private static final int MAX_ENERGY_BYTES = 4096;
    public long requestId;
    public long tick;
    public UUID owner;
    public BigInteger energy;

    public WirelessMonitorSnapshotPacket() {}

    public WirelessMonitorSnapshotPacket(long requestId, long tick, UUID owner, BigInteger energy) {
        this.requestId = requestId;
        this.tick = tick;
        this.owner = owner;
        this.energy = energy;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        requestId = buffer.readLong();
        tick = buffer.readLong();
        owner = new UUID(buffer.readLong(), buffer.readLong());
        int size = buffer.readUnsignedShort();
        if (size == 0 || size > MAX_ENERGY_BYTES || size != buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid wireless energy size");
        }
        byte[] bytes = new byte[size];
        buffer.readBytes(bytes);
        energy = new BigInteger(bytes);
        if (energy.signum() < 0) throw new IllegalArgumentException("Negative wireless energy");
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        byte[] bytes = energy.toByteArray();
        if (bytes.length > MAX_ENERGY_BYTES || energy.signum() < 0) {
            throw new IllegalArgumentException("Invalid wireless energy");
        }
        buffer.writeLong(requestId);
        buffer.writeLong(tick);
        buffer.writeLong(owner.getMostSignificantBits());
        buffer.writeLong(owner.getLeastSignificantBits());
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    @SideOnly(Side.CLIENT)
    private void handleClient(MessageContext context) {
        Minecraft minecraft = Minecraft.getMinecraft();
        var connection = context.getClientHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection && minecraft.theWorld != null) {
                WirelessMonitorHud.INSTANCE.receive(this, connection);
            }
        });
    }

    public static final class Handler implements IMessageHandler<WirelessMonitorSnapshotPacket, IMessage> {

        @Override
        public IMessage onMessage(WirelessMonitorSnapshotPacket message, MessageContext context) {
            message.handleClient(context);
            return null;
        }
    }
}
