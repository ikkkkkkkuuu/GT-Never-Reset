// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtneverreset.common.packet.wireless;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.utils.machine.wireless.WirelessAutoConnect;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class OpenWirelessCardPacket implements IMessage {

    public OpenWirelessCardPacket() {}

    @Override
    public void fromBytes(ByteBuf buffer) {
        if (buffer.isReadable()) throw new IllegalArgumentException("Unexpected wireless card payload");
    }

    @Override
    public void toBytes(ByteBuf buffer) {}

    public void handle(EntityPlayerMP player) {
        WirelessAutoConnect.openGui(player);
    }
}
