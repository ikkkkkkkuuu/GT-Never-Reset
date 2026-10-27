// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtneverreset.common.packet.quantum;

import com.xyp.gtneverreset.GTNeverReset;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class QuantumCraftConfirmPacket implements IMessage {

    private boolean quantumJob;

    public QuantumCraftConfirmPacket() {}

    public QuantumCraftConfirmPacket(boolean quantumJob) {
        this.quantumJob = quantumJob;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.quantumJob = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.quantumJob);
    }

    public static final class Handler implements IMessageHandler<QuantumCraftConfirmPacket, IMessage> {

        @Override
        public IMessage onMessage(QuantumCraftConfirmPacket message, MessageContext context) {
            GTNeverReset.proxy.receiveQuantumConfirm(message.quantumJob);
            return null;
        }
    }
}
