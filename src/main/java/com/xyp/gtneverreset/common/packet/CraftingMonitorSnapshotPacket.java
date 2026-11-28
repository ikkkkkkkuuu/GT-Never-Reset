package com.xyp.gtneverreset.common.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.xyp.gtneverreset.client.gui.crafting.CraftingMonitorTooltip;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class CraftingMonitorSnapshotPacket extends ClientboundPacket {

    public static final int MAX_ROWS = 8;

    private int windowId;
    private int requestId;
    private List<Machine> machines;
    private Result result;
    private Position position;
    private long completed;
    private long total;

    public CraftingMonitorSnapshotPacket() {}

    public CraftingMonitorSnapshotPacket(int windowId, int requestId, List<Machine> machines, Result result,
        Position position) {
        this(windowId, requestId, machines, result, position, 0, 0);
    }

    public CraftingMonitorSnapshotPacket(int windowId, int requestId, List<Machine> machines, Result result,
        Position position, long completed, long total) {
        if (completed < 0 || completed > total || total > 0 && result != Result.STATUS)
            throw new IllegalArgumentException("Invalid cumulative crafting progress");
        this.windowId = windowId;
        this.requestId = requestId;
        this.machines = List.copyOf(machines);
        this.result = result;
        this.position = position;
        this.completed = completed;
        this.total = total;
    }

    public int windowId() {
        return windowId;
    }

    public int requestId() {
        return requestId;
    }

    public List<Machine> machines() {
        return machines;
    }

    public Result result() {
        return result;
    }

    public Position position() {
        return position;
    }

    public long completed() {
        return completed;
    }

    public long total() {
        return total;
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(windowId);
        buffer.writeInt(requestId);
        buffer.writeByte(result.ordinal());
        writePosition(buffer, position);
        buffer.writeByte(machines.size());
        for (Machine machine : machines) {
            ByteBufUtils.writeUTF8String(buffer, machine.name());
            writePosition(buffer, machine.machine());
            writePosition(buffer, machine.provider());
            buffer.writeInt(machine.progress());
            buffer.writeInt(machine.duration());
            buffer.writeLong(machine.parallel());
            ByteBufUtils.writeUTF8String(buffer, machine.shutdown());
            buffer.writeBoolean(machine.active());
            buffer.writeBoolean(machine.canOpenMachine());
            buffer.writeBoolean(machine.canOpenProvider());
        }
        if (total > 0) {
            buffer.writeLong(completed);
            buffer.writeLong(total);
        }
    }

    @Override
    protected void read(ByteBuf buffer) {
        windowId = buffer.readInt();
        requestId = buffer.readInt();
        int resultId = buffer.readUnsignedByte();
        if (resultId >= Result.VALUES.length) throw new IllegalArgumentException("Invalid crafting monitor result");
        result = Result.VALUES[resultId];
        position = readPosition(buffer);
        int count = buffer.readUnsignedByte();
        if (count > MAX_ROWS) throw new IllegalArgumentException("Too many crafting monitor rows");
        List<Machine> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(
                new Machine(
                    ByteBufUtils.readUTF8String(buffer),
                    readPosition(buffer),
                    readPosition(buffer),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readLong(),
                    ByteBufUtils.readUTF8String(buffer),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));
        }
        machines = List.copyOf(rows);
        completed = 0;
        total = 0;
        if (result == Result.STATUS && buffer.readableBytes() == Long.BYTES * 2) {
            completed = buffer.readLong();
            total = buffer.readLong();
            if (completed < 0 || total <= 0 || completed > total)
                throw new IllegalArgumentException("Invalid cumulative crafting progress");
        }
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing crafting monitor data");
    }

    private static void writePosition(ByteBuf buffer, Position position) {
        buffer.writeBoolean(position != null);
        if (position == null) return;
        buffer.writeInt(position.dimension());
        buffer.writeInt(position.x());
        buffer.writeInt(position.y());
        buffer.writeInt(position.z());
    }

    private static Position readPosition(ByteBuf buffer) {
        return buffer.readBoolean()
            ? new Position(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt())
            : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        CraftingMonitorTooltip.accept(this);
    }

    public enum Result {

        STATUS,
        LOCATED,
        DENIED,
        OPENED;

        private static final Result[] VALUES = values();
    }

    public record Machine(String name, Position machine, Position provider, int progress, int duration, long parallel,
        String shutdown, boolean active, boolean canOpenMachine, boolean canOpenProvider) {}

    public record Position(int dimension, int x, int y, int z) {}
}
