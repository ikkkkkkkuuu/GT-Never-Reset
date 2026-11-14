package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.me.InterfaceTeleportation;
import com.xyp.gtneverreset.common.me.InterfaceTerminalLocation;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class TeleportToInterfacePacket implements IMessage {

    private long entryId;

    public TeleportToInterfacePacket() {}

    public TeleportToInterfacePacket(long entryId) {
        this.entryId = entryId;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entryId = buffer.readLong();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(entryId);
    }

    public void handle(EntityPlayerMP player) {
        if (!(player.openContainer instanceof QuickEncodingTerminalContainer container)) return;
        InterfaceTerminalLocation target = container.getTrackedInterfaceLocation(entryId);
        if (target != null) InterfaceTeleportation.teleportNearInterface(player, target);
    }
}
