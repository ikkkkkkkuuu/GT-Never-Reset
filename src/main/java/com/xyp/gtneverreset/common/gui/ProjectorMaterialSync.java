package com.xyp.gtneverreset.common.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.projector.ProjectorBuildMaterials;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ProjectorMaterialSync extends SyncHandler<ProjectorMaterialSync> {

    private static final int PAGE_SIZE = 24;
    private final PlayerInventoryGuiData data;
    private List<Material> displayed;
    private int[] counts;
    private int sequence, offset, refreshAt, timeoutAt;
    private boolean waiting;
    private long lastQueryTick = Long.MIN_VALUE;

    public ProjectorMaterialSync(PlayerInventoryGuiData data) {
        this.data = data;
    }

    @SideOnly(Side.CLIENT)
    public void update(List<Material> materials, int tick) {
        if (displayed != materials) {
            displayed = materials;
            counts = materials == null ? null : new int[materials.size()];
            if (counts != null) Arrays.fill(counts, -1);
            offset = 0;
            waiting = false;
            refreshAt = tick;
            sequence++;
        }
        if (materials == null || materials.isEmpty() || !isValid() || tick < refreshAt || waiting && tick < timeoutAt)
            return;
        int length = Math.min(PAGE_SIZE, materials.size() - offset);
        int request = ++sequence;
        waiting = true;
        timeoutAt = tick + 60;
        syncToServer(0, buf -> {
            buf.writeInt(request);
            buf.writeByte(length);
            for (int i = 0; i < length; i++) {
                var item = materials.get(offset + i)
                    .stack();
                buf.writeInt(Item.getIdFromItem(item.getItem()));
                buf.writeInt(item.getItemDamage());
                buf.writeNBTTagCompoundToBuffer(item.getTagCompound());
            }
        });
        refreshAt = tick + 1;
    }

    @SideOnly(Side.CLIENT)
    public int count(List<Material> materials, int index) {
        return materials == displayed ? counts[index] : -1;
    }

    @Override
    public void readOnServer(int id, PacketBuffer buf) throws IOException {
        if (id != 0) return;
        int request = buf.readInt();
        int length = buf.readUnsignedByte();
        if (length == 0 || length > PAGE_SIZE) return;
        ItemStack[] items = new ItemStack[length];
        for (int i = 0; i < length; i++) {
            var item = Item.getItemById(buf.readInt());
            if (item == null) return;
            items[i] = new ItemStack(item, 1, buf.readInt());
            items[i].setTagCompound(buf.readNBTTagCompoundFromBuffer());
        }
        ServerThreadUtil.addScheduledTask(() -> {
            if (!isValid() || !(data.getPlayer() instanceof EntityPlayerMP player)) return;
            ItemStack projector = data.getUsedItemStack();
            if (projector == null || !(projector.getItem() instanceof MultiblockProjector)) return;
            long tick = player.worldObj.getTotalWorldTime();
            if (lastQueryTick == tick) return;
            lastQueryTick = tick;
            int[] available = new int[length];
            for (int i = 0; i < length; i++)
                available[i] = ProjectorBuildMaterials.available(player, items[i], Integer.MAX_VALUE);
            syncToClient(0, reply -> {
                reply.writeInt(request);
                reply.writeByte(length);
                for (int n : available) reply.writeInt(n);
            });
        });
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readOnClient(int id, PacketBuffer buf) {
        if (id != 0) return;
        int request = buf.readInt();
        int length = buf.readUnsignedByte();
        if (length == 0 || length > PAGE_SIZE) return;
        int[] available = new int[length];
        for (int i = 0; i < length; i++) available[i] = buf.readInt();
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                if (!isValid() || !waiting || request != sequence || offset + length > counts.length) return;
                System.arraycopy(available, 0, counts, offset, length);
                offset += length;
                waiting = false;
                if (offset == counts.length) {
                    offset = 0;
                    refreshAt += 20;
                }
            });
    }
}
