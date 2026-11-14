package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;
import com.xyp.gtnrlib.network.ServerboundPacket;

import appeng.container.AEBaseContainer;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;

public final class SwitchGuisPacket extends ServerboundPacket {

    private WirelessTerminalGuiType type;
    private boolean restoreSaved;

    public SwitchGuisPacket() {}

    public SwitchGuisPacket(WirelessTerminalGuiType type) {
        this.type = type;
    }

    public static SwitchGuisPacket restoreLast() {
        SwitchGuisPacket packet = new SwitchGuisPacket();
        packet.restoreSaved = true;
        return packet;
    }

    @Override
    protected void read(ByteBuf buffer) {
        type = WirelessTerminalGuiType.getByOrdinal(buffer.readByte());
        restoreSaved = buffer.readBoolean();
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeByte(type == null ? -1 : type.ordinal());
        buffer.writeBoolean(restoreSaved);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (!restoreSaved && type == null) return;
        int slot = -1;
        if (!restoreSaved && player.openContainer instanceof AEBaseContainer container
            && container.getTarget() instanceof WirelessTerminalGuiObject host) {
            int nativeSlot = host.getInventorySlot();
            slot = nativeSlot >= Platform.baublesSlotsOffset
                ? WirelessTerminalSession.BAUBLE_SLOT_OFFSET + nativeSlot - Platform.baublesSlotsOffset
                : nativeSlot;
        } else if (restoreSaved) {
            slot = WirelessTerminalSession.findDualInterfaceTerminal(player);
        }
        if (slot >= 0) WirelessTerminalSession.open(player, slot, restoreSaved ? null : type);
    }
}
