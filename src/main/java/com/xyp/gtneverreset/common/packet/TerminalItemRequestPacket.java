package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;
import com.xyp.gtnrlib.network.ServerboundPacket;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public final class TerminalItemRequestPacket extends ServerboundPacket {

    private ItemStack stack;
    private boolean fullStack;
    private int targetSlot = -1;

    public TerminalItemRequestPacket() {}

    public TerminalItemRequestPacket(ItemStack stack, boolean fullStack) {
        this(stack, fullStack, -1);
    }

    public TerminalItemRequestPacket(ItemStack stack, boolean fullStack, int targetSlot) {
        this.stack = stack == null ? null : stack.copy();
        this.fullStack = fullStack;
        this.targetSlot = targetSlot;
    }

    @Override
    protected void read(ByteBuf buffer) {
        stack = ByteBufUtils.readItemStack(buffer);
        fullStack = buffer.readBoolean();
        targetSlot = buffer.isReadable() ? buffer.readByte() : -1;
    }

    @Override
    protected void write(ByteBuf buffer) {
        ByteBufUtils.writeItemStack(buffer, stack);
        buffer.writeBoolean(fullStack);
        buffer.writeByte(targetSlot);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        WirelessTerminalSession.requestItem(player, stack, fullStack, targetSlot);
    }
}
