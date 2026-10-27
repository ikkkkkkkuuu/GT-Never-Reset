package com.xyp.gtneverreset.common.packet.terminal;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.utils.inventory.WirelessTerminalSession;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class TerminalItemRequestPacket implements IMessage {

    private ItemStack stack;
    private boolean fullStack;

    public TerminalItemRequestPacket() {}

    public TerminalItemRequestPacket(ItemStack stack, boolean fullStack) {
        this.stack = stack == null ? null : stack.copy();
        this.fullStack = fullStack;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        stack = ByteBufUtils.readItemStack(buffer);
        fullStack = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeItemStack(buffer, stack);
        buffer.writeBoolean(fullStack);
    }

    public void handle(EntityPlayerMP player) {
        WirelessTerminalSession.requestItem(player, stack, fullStack);
    }
}
