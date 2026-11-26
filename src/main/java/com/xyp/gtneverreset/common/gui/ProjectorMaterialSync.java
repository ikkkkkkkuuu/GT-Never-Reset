package com.xyp.gtneverreset.common.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtneverreset.common.item.items.MultiblockProjector;
import com.xyp.gtneverreset.common.projector.ProjectorBuildMaterials;
import com.xyp.gtneverreset.common.projector.ProjectorMaterialCrafting;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorServer;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ProjectorMaterialSync extends SyncHandler<ProjectorMaterialSync> {

    private static final int STOCK = 0, CRAFT = 1, PAGE_SIZE = 24;
    private final PlayerInventoryGuiData data;
    private List<Material> displayed;
    private int[] counts;
    private boolean[] craftable;
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
            craftable = materials == null ? null : new boolean[materials.size()];
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
        syncToServer(STOCK, buf -> {
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
        return materials == displayed && counts != null ? counts[index] : -1;
    }

    @SideOnly(Side.CLIENT)
    public boolean isCraftable(List<Material> materials, int index) {
        return materials == displayed && craftable != null && craftable[index];
    }

    @SideOnly(Side.CLIENT)
    public void craft(ProjectorPlan plan) {
        if (isValid()) syncToServer(CRAFT, buf -> buf.writeNBTTagCompoundToBuffer(plan.writeToNBT()));
    }

    @Override
    public void readOnServer(int id, PacketBuffer buf) throws IOException {
        if (id == CRAFT) {
            NBTTagCompound draft = buf.readNBTTagCompoundFromBuffer();
            if (draft != null) ServerThreadUtil.addScheduledTask(() -> {
                EntityPlayerMP player = projectorUser();
                if (player != null) ProjectorServer.craftMaterials(player, draft);
            });
            return;
        }
        if (id != STOCK) return;
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
            EntityPlayerMP player = projectorUser();
            if (player == null) return;
            long tick = player.worldObj.getTotalWorldTime();
            if (lastQueryTick == tick) return;
            lastQueryTick = tick;
            int[] available = new int[length];
            for (int i = 0; i < length; i++)
                available[i] = ProjectorBuildMaterials.available(player, items[i], Integer.MAX_VALUE);
            boolean[] hasPattern = ProjectorMaterialCrafting.craftable(player, items);
            syncToClient(STOCK, reply -> {
                reply.writeInt(request);
                reply.writeByte(length);
                int patterns = 0;
                for (int i = 0; i < length; i++) {
                    reply.writeInt(available[i]);
                    if (hasPattern[i]) patterns |= 1 << i;
                }
                reply.writeInt(patterns);
            });
        });
    }

    private EntityPlayerMP projectorUser() {
        if (!isValid() || !(data.getPlayer() instanceof EntityPlayerMP player)) return null;
        ItemStack projector = data.getUsedItemStack();
        return projector != null && projector.getItem() instanceof MultiblockProjector ? player : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readOnClient(int id, PacketBuffer buf) {
        if (id != STOCK) return;
        int request = buf.readInt();
        int length = buf.readUnsignedByte();
        if (length == 0 || length > PAGE_SIZE) return;
        int[] available = new int[length];
        for (int i = 0; i < length; i++) available[i] = buf.readInt();
        int patterns = buf.readableBytes() >= Integer.BYTES ? buf.readInt() : 0;
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                if (!isValid() || !waiting || request != sequence || counts == null || offset + length > counts.length)
                    return;
                System.arraycopy(available, 0, counts, offset, length);
                for (int i = 0; i < length; i++) craftable[offset + i] = (patterns & 1 << i) != 0;
                offset += length;
                waiting = false;
                if (offset == counts.length) {
                    offset = 0;
                    refreshAt += 20;
                }
            });
    }
}
