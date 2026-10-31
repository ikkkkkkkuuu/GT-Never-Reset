package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.wireless.WirelessAutoConnect;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class ToggleWirelessCardPacket implements IMessage {

    private boolean heldOnly;
    private int slot;
    private boolean visualisation;
    private boolean backwards;

    public ToggleWirelessCardPacket() {}

    public ToggleWirelessCardPacket(boolean heldOnly, int slot, boolean visualisation, boolean backwards) {
        this.heldOnly = heldOnly;
        this.slot = slot;
        this.visualisation = visualisation;
        this.backwards = backwards;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        if (buffer.readableBytes() != 4) throw new IllegalArgumentException("Invalid wireless card payload length");
        heldOnly = buffer.readBoolean();
        slot = buffer.readByte();
        visualisation = buffer.readBoolean();
        backwards = buffer.readBoolean();
        if (heldOnly && (!visualisation || slot < 0 || slot > 8) || !heldOnly && slot != 0
            || !visualisation && backwards) throw new IllegalArgumentException("Invalid wireless card action");
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(heldOnly);
        buffer.writeByte(slot);
        buffer.writeBoolean(visualisation);
        buffer.writeBoolean(backwards);
    }

    public void handle(EntityPlayerMP player) {
        if (visualisation) WirelessAutoConnect.cycleVisualisation(player, heldOnly, slot, backwards);
        else WirelessAutoConnect.toggle(player);
    }
}
