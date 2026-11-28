package com.xyp.gtneverreset.common.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.xyp.gtneverreset.client.gui.crafting.CraftingMonitorTooltip;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class CraftingMonitorCellProgressPacket extends ClientboundPacket {

    private int windowId;
    private int requestId;
    private List<Cell> cells;

    public CraftingMonitorCellProgressPacket() {}

    public CraftingMonitorCellProgressPacket(int windowId, int requestId, List<Cell> cells) {
        if (cells.size() > CraftingMonitorCellRequestPacket.MAX_CELLS)
            throw new IllegalArgumentException("Too many crafting cell samples");
        this.windowId = windowId;
        this.requestId = requestId;
        this.cells = List.copyOf(cells);
    }

    public int windowId() {
        return windowId;
    }

    public int requestId() {
        return requestId;
    }

    public List<Cell> cells() {
        return cells;
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(windowId);
        buffer.writeInt(requestId);
        buffer.writeByte(cells.size());
        for (Cell cell : cells) {
            buffer.writeByte(cell.index());
            buffer.writeLong(cell.completed());
            buffer.writeLong(cell.total());
        }
    }

    @Override
    protected void read(ByteBuf buffer) {
        windowId = buffer.readInt();
        requestId = buffer.readInt();
        int count = buffer.readUnsignedByte();
        if (count > CraftingMonitorCellRequestPacket.MAX_CELLS) {
            throw new IllegalArgumentException("Too many crafting cell samples");
        }
        List<Cell> rows = new ArrayList<>(count);
        long seen = 0;
        for (int i = 0; i < count; i++) {
            int index = buffer.readUnsignedByte();
            long completed = buffer.readLong();
            long total = buffer.readLong();
            if (index >= CraftingMonitorCellRequestPacket.MAX_CELLS || completed < 0 || total <= 0 || completed > total)
                throw new IllegalArgumentException("Invalid crafting cell sample");
            long bit = 1L << index;
            if ((seen & bit) != 0) throw new IllegalArgumentException("Duplicate crafting cell sample");
            seen |= bit;
            rows.add(new Cell(index, completed, total));
        }
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing crafting cell sample data");
        cells = List.copyOf(rows);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        CraftingMonitorTooltip.acceptCells(this);
    }

    public record Cell(int index, long completed, long total) {}
}
