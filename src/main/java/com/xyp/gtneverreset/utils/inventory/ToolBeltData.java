package com.xyp.gtneverreset.utils.inventory;

import java.util.Arrays;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.ToolBeltSyncPacket;

import cpw.mods.fml.common.registry.GameRegistry;
import lombok.Getter;

public class ToolBeltData implements IExtendedEntityProperties {

    public static final String PROP_NAME = "ToolBeltData";
    public static final int DEFAULT_SLOT_COUNT = 10;
    public static final int MIN_SLOT_COUNT = 2;
    public static final int MAX_SLOT_COUNT = 24;
    public static final int SLOT_COUNT = DEFAULT_SLOT_COUNT;
    private static final String NBT_ITEMS = "Items";
    private static final String NBT_SLOT_COUNT = "SlotCount";

    @Getter
    private EntityLivingBase owner;
    private ItemStack[] items = new ItemStack[DEFAULT_SLOT_COUNT];

    public ToolBeltData(EntityLivingBase owner) {
        this.owner = owner;
    }

    public static ToolBeltData get(EntityLivingBase entity) {
        if (entity == null) return null;
        return (ToolBeltData) entity.getExtendedProperties(PROP_NAME);
    }

    public static void register(EntityLivingBase entity) {
        entity.registerExtendedProperties(PROP_NAME, new ToolBeltData(entity));
    }

    public int getSlotCount() {
        return items.length;
    }

    public boolean resize(int count) {
        if (owner.worldObj.isRemote || count < MIN_SLOT_COUNT || count > MAX_SLOT_COUNT) return false;
        if (count == items.length) return true;
        for (int slot = count; slot < items.length; slot++) {
            if (items[slot] != null && items[slot].stackSize > 0) return false;
        }
        items = Arrays.copyOf(items, count);
        onContentsChanged();
        return true;
    }

    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= items.length) return null;
        return items[slot];
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.length) return;
        setStackInSlotSilent(slot, stack);
        onContentsChanged();
    }

    public void setStackInSlotSilent(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.length) return;
        items[slot] = stack == null || stack.stackSize <= 0 ? null : stack;
    }

    public void applySnapshot(ItemStack[] snapshot) {
        if (snapshot.length < MIN_SLOT_COUNT || snapshot.length > MAX_SLOT_COUNT) return;
        items = new ItemStack[snapshot.length];
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack stack = snapshot[slot];
            setStackInSlotSilent(slot, stack == null ? null : stack.copy());
        }
    }

    /** Returns the uninserted remainder without changing the caller's stack. */
    public ItemStack tryInsert(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return null;
        if (owner.worldObj.isRemote || !isItemStackAllowed(stack)) return stack;
        ItemStack remaining = stack.copy();
        boolean changed = false;

        for (ItemStack existing : items) {
            if (remaining.stackSize <= 0) break;
            if (existing == null || !existing.isItemEqual(remaining)
                || !ItemStack.areItemStackTagsEqual(existing, remaining)) continue;
            int amount = Math.min(remaining.stackSize, Math.max(0, existing.getMaxStackSize() - existing.stackSize));
            if (amount <= 0) continue;
            existing.stackSize += amount;
            remaining.stackSize -= amount;
            changed = true;
        }

        for (int slot = 0; slot < items.length && remaining.stackSize > 0; slot++) {
            if (items[slot] != null) continue;
            int amount = Math.min(remaining.stackSize, remaining.getMaxStackSize());
            if (amount <= 0) break;
            items[slot] = remaining.copy();
            items[slot].stackSize = amount;
            remaining.stackSize -= amount;
            changed = true;
        }
        if (changed) onContentsChanged();
        return remaining.stackSize > 0 ? remaining : null;
    }

    public void onContentsChanged() {
        if (!owner.worldObj.isRemote) {
            syncToTracking();
        }
    }

    public void syncToTracking() {
        syncToSelf();
    }

    public void syncToSelf() {
        if (!(owner instanceof EntityPlayerMP)) return;
        NetWorkHandler.sendTo(new ToolBeltSyncPacket(owner, this), (EntityPlayerMP) owner);
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger(NBT_SLOT_COUNT, items.length);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setByte("Slot", (byte) i);
                items[i].writeToNBT(slotTag);
                list.appendTag(slotTag);
            }
        }
        if (list.tagCount() > 0) {
            data.setTag(NBT_ITEMS, list);
        }
        compound.setTag(PROP_NAME, data);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        NBTTagCompound data = compound.getCompoundTag(PROP_NAME);
        int count = data.hasKey(NBT_SLOT_COUNT, Constants.NBT.TAG_ANY_NUMERIC)
            ? Math.max(MIN_SLOT_COUNT, Math.min(MAX_SLOT_COUNT, data.getInteger(NBT_SLOT_COUNT)))
            : DEFAULT_SLOT_COUNT;
        NBTTagList list = data.getTagList(NBT_ITEMS, Constants.NBT.TAG_COMPOUND);
        ItemStack[] restored = new ItemStack[MAX_SLOT_COUNT];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound slotTag = list.getCompoundTagAt(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot >= MAX_SLOT_COUNT) continue;
            ItemStack stack = ItemStack.loadItemStackFromNBT(slotTag);
            if (stack == null || stack.stackSize <= 0) continue;
            restored[slot] = stack;
            count = Math.max(count, slot + 1);
        }
        items = Arrays.copyOf(restored, count);
    }

    @Override
    public void init(Entity entity, World world) {
        if (entity instanceof EntityLivingBase) owner = (EntityLivingBase) entity;
    }

    public static boolean isItemStackAllowed(ItemStack stack) {
        return stack != null && stack.getItem() != null
            && stack.stackSize > 0
            && GameRegistry.findUniqueIdentifierFor(stack.getItem()) != null
            && stack.getMaxStackSize() > 0;
    }

    public static void swapItem(int swapWith, EntityPlayer player) {
        if (player.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(player);
        if (data == null) {
            ToolBeltData.register(player);
            data = ToolBeltData.get(player);
        }
        if (data == null) return;

        ItemStack inHand = player.getHeldItem();
        if (inHand != null && !ToolBeltData.isItemStackAllowed(inHand)) return;

        if (swapWith < 0) {
            if (swapWith != -1 || inHand == null) return;
            ItemStack remaining = data.tryInsert(inHand);
            if (remaining != null && remaining.stackSize == inHand.stackSize) return;
            player.inventory.setInventorySlotContents(player.inventory.currentItem, remaining);
        } else {
            if (swapWith >= data.getSlotCount()) return;
            ItemStack inSlot = data.getStackInSlot(swapWith);
            if (inHand == null) {
                if (inSlot == null) return;
                data.setStackInSlotSilent(swapWith, null);
                player.inventory.setInventorySlotContents(player.inventory.currentItem, inSlot);
            } else
                if (inSlot == null || inSlot.isItemEqual(inHand) && ItemStack.areItemStackTagsEqual(inSlot, inHand)) {
                    int existing = inSlot == null ? 0 : inSlot.stackSize;
                    int amount = Math.min(inHand.stackSize, Math.max(0, inHand.getMaxStackSize() - existing));
                    if (amount <= 0) return;
                    ItemStack inserted = inHand.copy();
                    inserted.stackSize = existing + amount;
                    data.setStackInSlotSilent(swapWith, inserted);
                    ItemStack remaining = inHand.copy();
                    remaining.stackSize -= amount;
                    player.inventory.setInventorySlotContents(
                        player.inventory.currentItem,
                        remaining.stackSize > 0 ? remaining : null);
                } else {
                    if (inHand.stackSize > inHand.getMaxStackSize()) return;
                    data.setStackInSlotSilent(swapWith, inHand.copy());
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, inSlot);
                }
            data.onContentsChanged();
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }

    public static void resize(int requestedCount, EntityPlayerMP player) {
        ToolBeltData data = ToolBeltData.get(player);
        if (data == null) {
            ToolBeltData.register(player);
            data = ToolBeltData.get(player);
        }
        if (data == null) return;
        int previousCount = data.getSlotCount();
        boolean accepted = data.resize(requestedCount);
        if (!accepted) {
            if (requestedCount < ToolBeltData.MIN_SLOT_COUNT || requestedCount > ToolBeltData.MAX_SLOT_COUNT) {
                // #tr chat.gtneverreset.toolbelt.resize_range
                // # Tool belt size must be between %s and %s slots.
                // # zh_CN 工具腰带槽位数必须在 %s 到 %s 之间。
                player.addChatMessage(
                    new ChatComponentTranslation(
                        "chat.gtneverreset.toolbelt.resize_range",
                        ToolBeltData.MIN_SLOT_COUNT,
                        ToolBeltData.MAX_SLOT_COUNT));
            } else {
                // #tr chat.gtneverreset.toolbelt.resize_occupied
                // # Remove items from the slots being removed before shrinking the tool belt.
                // # zh_CN 请先取出被缩减槽位中的物品，再缩小工具腰带。
                player.addChatMessage(new ChatComponentTranslation("chat.gtneverreset.toolbelt.resize_occupied"));
            }
        }
        if (!accepted || previousCount == requestedCount) data.syncToSelf();
    }

}
