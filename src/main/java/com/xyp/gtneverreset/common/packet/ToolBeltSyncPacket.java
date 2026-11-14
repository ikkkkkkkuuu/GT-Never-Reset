package com.xyp.gtneverreset.common.packet;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.client.radialmenu.action.ToolBeltMenu;
import com.xyp.gtneverreset.common.item.ToolBeltData;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class ToolBeltSyncPacket extends ClientboundPacket {

    private int entityId;
    private ItemStack[] items;

    public ToolBeltSyncPacket() {}

    public ToolBeltSyncPacket(Entity entity, ToolBeltData data) {
        this.entityId = entity.getEntityId();
        this.items = new ItemStack[data.getSlotCount()];
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = data.getStackInSlot(i);
            this.items[i] = stack == null ? null : stack.copy();
        }
    }

    @Override
    protected void read(ByteBuf buf) {
        entityId = buf.readInt();
        int count = buf.readInt();
        if (count < 0 || count > ToolBeltData.MAX_SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid tool belt item count: " + count);
        }
        ItemStack[] restored = new ItemStack[ToolBeltData.MAX_SLOT_COUNT];
        int highestSlot = -1;
        for (int i = 0; i < count; i++) {
            int slot = buf.readInt();
            if (slot < 0 || slot >= restored.length) {
                throw new IllegalArgumentException("Invalid tool belt slot: " + slot);
            }
            restored[slot] = ByteBufUtils.readItemStack(buf);
            highestSlot = Math.max(highestSlot, slot);
        }
        int capacity = buf.isReadable() ? buf.readInt() : Math.max(ToolBeltData.DEFAULT_SLOT_COUNT, highestSlot + 1);
        if (capacity < ToolBeltData.MIN_SLOT_COUNT || capacity > ToolBeltData.MAX_SLOT_COUNT
            || highestSlot >= capacity) {
            throw new IllegalArgumentException("Invalid tool belt capacity: " + capacity);
        }
        items = Arrays.copyOf(restored, capacity);
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeInt(entityId);
        int count = 0;
        for (ItemStack item : items) {
            if (item != null) count++;
        }
        buf.writeInt(count);
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] == null) continue;
            buf.writeInt(slot);
            ByteBufUtils.writeItemStack(buf, items[slot]);
        }
        buf.writeInt(items.length);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        var connection = minecraft.getNetHandler();
        var world = minecraft.theWorld;
        minecraft.func_152344_a(() -> {
            if (world == null || minecraft.theWorld != world || minecraft.getNetHandler() != connection) return;
            Entity entity = minecraft.theWorld.getEntityByID(entityId);
            if (!(entity instanceof EntityLivingBase)) return;
            EntityLivingBase living = (EntityLivingBase) entity;
            ToolBeltData data = ToolBeltData.get(living);
            if (data == null) {
                ToolBeltData.register(living);
                data = ToolBeltData.get(living);
            }
            if (data == null) return;
            data.applySnapshot(items);
            if (entity == minecraft.thePlayer) ToolBeltMenu.onInventorySynced();
        });
    }

}
