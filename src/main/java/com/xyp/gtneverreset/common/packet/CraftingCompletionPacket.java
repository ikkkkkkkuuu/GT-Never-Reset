package com.xyp.gtneverreset.common.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.client.terminal.CraftingCompletionClient;
import com.xyp.gtneverreset.common.crafting.CraftingCompletionService;
import com.xyp.gtnrlib.mod.ModList;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class CraftingCompletionPacket extends ClientboundPacket {

    private int dimension;
    private int windowId;
    private long gridId;
    private long sequence;
    private List<ItemStack> items;

    public CraftingCompletionPacket() {}

    public CraftingCompletionPacket(int dimension, int windowId, long gridId, long sequence, List<ItemStack> items) {
        this.dimension = dimension;
        this.windowId = windowId;
        this.gridId = gridId;
        this.sequence = sequence;
        this.items = List.copyOf(items);
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(dimension);
        buffer.writeInt(windowId);
        buffer.writeLong(gridId);
        buffer.writeLong(sequence);
        buffer.writeShort(items.size());
        for (ItemStack item : items) ByteBufUtils.writeTag(buffer, item.writeToNBT(new NBTTagCompound()));
    }

    @Override
    protected void read(ByteBuf buffer) {
        dimension = buffer.readInt();
        windowId = buffer.readInt();
        gridId = buffer.readLong();
        sequence = buffer.readLong();
        int count = buffer.readUnsignedShort();
        if (count > CraftingCompletionService.MAX_ITEMS) throw new IllegalArgumentException("Too many completed items");
        items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            NBTTagCompound tag = ByteBufUtils.readTag(buffer);
            ItemStack item = tag == null ? null : ItemStack.loadItemStackFromNBT(tag);
            if (item != null) items.add(item);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        if (!ModList.AE2ConvenienceFeatures.isModLoaded()) {
            CraftingCompletionClient.receive(minecraft, dimension, windowId, gridId, sequence, items);
        }
    }
}
