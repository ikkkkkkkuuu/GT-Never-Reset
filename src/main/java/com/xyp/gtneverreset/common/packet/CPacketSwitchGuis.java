package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.container.AEBaseContainer;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class CPacketSwitchGuis implements IMessage {

    private WirelessTerminalGuiType type;
    private boolean restoreSaved;

    public CPacketSwitchGuis() {}

    public CPacketSwitchGuis(WirelessTerminalGuiType type) {
        this.type = type;
    }

    public static CPacketSwitchGuis restoreLast() {
        CPacketSwitchGuis packet = new CPacketSwitchGuis();
        packet.restoreSaved = true;
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        type = WirelessTerminalGuiType.getByOrdinal(buffer.readByte());
        restoreSaved = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeByte(type == null ? -1 : type.ordinal());
        buffer.writeBoolean(restoreSaved);
    }

    public void handle(EntityPlayerMP player) {
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
