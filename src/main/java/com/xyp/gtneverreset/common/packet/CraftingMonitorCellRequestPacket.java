package com.xyp.gtneverreset.common.packet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.common.me.CraftingMonitorService;
import com.xyp.gtnrlib.network.ServerboundPacket;

import appeng.api.storage.data.IAEStack;
import io.netty.buffer.ByteBuf;

public final class CraftingMonitorCellRequestPacket extends ServerboundPacket {

    public static final int MAX_CELLS = 48;
    private static final int MAX_REQUEST_BYTES = 24 * 1024;
    private static final int MAX_STACK_BYTES = 8 * 1024;
    private static final long MAX_UNCOMPRESSED_BYTES = 2 * 1024 * 1024L;

    private int windowId;
    private int requestId;
    private List<IAEStack<?>> visible;

    public CraftingMonitorCellRequestPacket() {}

    public CraftingMonitorCellRequestPacket(int windowId, int requestId, List<IAEStack<?>> visible) {
        if (visible.size() > MAX_CELLS) throw new IllegalArgumentException("Too many crafting monitor cells");
        this.windowId = windowId;
        this.requestId = requestId;
        this.visible = new ArrayList<>(visible.size());
        for (IAEStack<?> stack : visible) {
            this.visible.add(
                stack == null ? null
                    : stack.copy()
                        .reset()
                        .setStackSize(1));
        }
    }

    @Override
    protected void write(ByteBuf buffer) {
        int limit = buffer.writerIndex() + MAX_REQUEST_BYTES;
        buffer.writeInt(windowId);
        buffer.writeInt(requestId);
        buffer.writeByte(visible.size());
        for (int i = 0; i < visible.size(); i++) {
            IAEStack<?> stack = visible.get(i);
            byte[] compressed = stack == null ? null : compress(stack.toNBTGeneric());
            // Keep later cell indices intact even when a stack does not fit the packet.
            int available = limit - buffer.writerIndex() - Short.BYTES * (visible.size() - i);
            if (compressed == null || compressed.length > MAX_STACK_BYTES || compressed.length > available) {
                buffer.writeShort(0);
            } else {
                buffer.writeShort(compressed.length);
                buffer.writeBytes(compressed);
            }
        }
    }

    @Override
    protected void read(ByteBuf buffer) {
        if (buffer.readableBytes() > MAX_REQUEST_BYTES)
            throw new IllegalArgumentException("Crafting cell request too large");
        windowId = buffer.readInt();
        requestId = buffer.readInt();
        int count = buffer.readUnsignedByte();
        if (count > MAX_CELLS) throw new IllegalArgumentException("Too many crafting monitor cells");
        List<IAEStack<?>> slots = new ArrayList<>(count);
        NBTSizeTracker size = new NBTSizeTracker(MAX_UNCOMPRESSED_BYTES);
        for (int i = 0; i < count; i++) {
            int length = buffer.readUnsignedShort();
            if (length > MAX_STACK_BYTES) throw new IllegalArgumentException("Crafting cell stack too large");
            if (length == 0) {
                slots.add(null);
                continue;
            }
            byte[] compressed = new byte[length];
            buffer.readBytes(compressed);
            IAEStack<?> stack;
            try {
                stack = IAEStack.fromNBTGeneric(CompressedStreamTools.func_152457_a(compressed, size));
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid crafting cell stack data", e);
            }
            if (stack == null) throw new IllegalArgumentException("Invalid crafting cell stack");
            slots.add(
                stack.reset()
                    .setStackSize(1));
        }
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing crafting cell request data");
        visible = Collections.unmodifiableList(slots);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        CraftingMonitorService.sampleCells(player, windowId, requestId, visible);
    }

    private static byte[] compress(NBTTagCompound tag) {
        try {
            return CompressedStreamTools.compress(tag);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not encode crafting cell stack", e);
        }
    }
}
