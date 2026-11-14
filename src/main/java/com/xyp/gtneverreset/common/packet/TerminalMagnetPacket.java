package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.network.SPacketCustomButtonUpdate;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.common.gui.WirelessTerminalMagnetGui;
import com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;
import com.xyp.gtnrlib.network.ServerboundPacket;

import appeng.container.AEBaseContainer;
import appeng.container.interfaces.IContainerSubGui;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;

public final class TerminalMagnetPacket extends ServerboundPacket {

    private boolean filter;

    public TerminalMagnetPacket() {}

    public TerminalMagnetPacket(boolean filter) {
        this.filter = filter;
    }

    @Override
    protected void read(ByteBuf buf) {
        filter = buf.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeBoolean(filter);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (!(player.openContainer instanceof AEBaseContainer container)
            || !(container.getTarget() instanceof WirelessTerminalGuiObject host)
            || !(host.getItemStack()
                .getItem() instanceof ItemWirelessDualInterfaceTerminal)
            || !(host.getDataObject() instanceof WirelessTerminalMagnetGui data)) return;
        int nativeSlot = host.getInventorySlot();
        int slot = nativeSlot >= Platform.baublesSlotsOffset
            ? WirelessTerminalSession.BAUBLE_SLOT_OFFSET + nativeSlot - Platform.baublesSlotsOffset
            : nativeSlot;
        if (WirelessTerminalSession.getTerminalInSlot(player, slot) != host.getItemStack()) return;
        if (filter) {
            var primary = container.createPrimaryGui();
            WirelessTerminalGuiHandler.openGui(player, slot, WirelessTerminalGuiType.WirelessMagnetFilter);
            if (player.openContainer != container && player.openContainer instanceof IContainerSubGui sub)
                sub.setPrimaryGui(primary);
        } else {
            data.cycleMode();
            host.writeCustomButtonData();
            WirelessTerminalSession.writeBackTerminal(player, slot, host.getItemStack());
            FluidCraft.proxy.netHandler.sendTo(new SPacketCustomButtonUpdate(data), player);
        }
    }
}
