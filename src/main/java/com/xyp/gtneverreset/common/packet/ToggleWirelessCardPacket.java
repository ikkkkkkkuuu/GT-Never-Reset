package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.wireless.WirelessAutoConnect;
import com.xyp.gtnrlib.network.ServerboundPacket;

import io.netty.buffer.ByteBuf;

public final class ToggleWirelessCardPacket extends ServerboundPacket {

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
    protected void read(ByteBuf buffer) {
        if (buffer.readableBytes() != 4) throw new IllegalArgumentException("Invalid wireless card payload length");
        heldOnly = buffer.readBoolean();
        slot = buffer.readByte();
        visualisation = buffer.readBoolean();
        backwards = buffer.readBoolean();
        if (heldOnly && (!visualisation || slot < 0 || slot > 8) || !heldOnly && slot != 0
            || !visualisation && backwards) throw new IllegalArgumentException("Invalid wireless card action");
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeBoolean(heldOnly);
        buffer.writeByte(slot);
        buffer.writeBoolean(visualisation);
        buffer.writeBoolean(backwards);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (visualisation) WirelessAutoConnect.cycleVisualisation(player, heldOnly, slot, backwards);
        else WirelessAutoConnect.toggle(player);
    }
}
