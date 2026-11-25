package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.common.me.CraftingMonitorService;
import com.xyp.gtnrlib.network.ServerboundPacket;

import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public final class CraftingMonitorRequestPacket extends ServerboundPacket {

    private static final int MAX_REQUEST_BYTES = 8_192;

    private int windowId;
    private int requestId;
    private IAEStack<?> stack;
    private Action action;
    private int row;

    public CraftingMonitorRequestPacket() {}

    public CraftingMonitorRequestPacket(int windowId, int requestId, IAEStack<?> stack, Action action, int row) {
        this.windowId = windowId;
        this.requestId = requestId;
        this.stack = stack.copy();
        this.stack.setStackSize(1);
        this.action = action;
        this.row = row;
    }

    @Override
    protected void read(ByteBuf buffer) {
        if (buffer.readableBytes() > MAX_REQUEST_BYTES) throw new IllegalArgumentException("Monitor request too large");
        windowId = buffer.readInt();
        requestId = buffer.readInt();
        int actionId = buffer.readUnsignedByte();
        row = buffer.readByte();
        if (actionId >= Action.VALUES.length) throw new IllegalArgumentException("Invalid crafting monitor action");
        action = Action.VALUES[actionId];
        if (action != Action.STATUS && (row < 0 || row >= CraftingMonitorSnapshotPacket.MAX_ROWS))
            throw new IllegalArgumentException("Invalid crafting monitor row");
        NBTTagCompound tag = ByteBufUtils.readTag(buffer);
        stack = tag == null ? null : IAEStack.fromNBTGeneric(tag);
        if (stack == null || buffer.isReadable()) throw new IllegalArgumentException("Invalid crafting monitor stack");
        stack.setStackSize(1);
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(windowId);
        buffer.writeInt(requestId);
        buffer.writeByte(action.ordinal());
        buffer.writeByte(row);
        ByteBufUtils.writeTag(buffer, stack.toNBTGeneric());
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        CraftingMonitorService.handle(player, windowId, requestId, stack, action, row);
    }

    public enum Action {

        STATUS,
        LOCATE_PROVIDER,
        OPEN_PROVIDER,
        OPEN_MACHINE;

        private static final Action[] VALUES = values();
    }
}
