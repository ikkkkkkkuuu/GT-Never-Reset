package com.xyp.gtneverreset.common.projector;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.Constants;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.util.InventoryUtility;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public final class ProjectorBuildMaterials {

    private static final String ESCROW = "gtneverresetProjectorMaterials";
    public static final Refunds REFUNDS = new Refunds();

    private final EntityPlayerMP player;
    private final LongOpenHashSet missingPositions = new LongOpenHashSet();
    private final Map<ItemStack, Integer> required = new ItemStackMap<>(true);
    private final Map<ItemStack, NBTTagCompound> balances = new ItemStackMap<>(true);

    ProjectorBuildMaterials(EntityPlayerMP player) {
        this.player = player;
    }

    void require(int x, int y, int z, ItemStack item) {
        if (missingPositions.add(CoordinatePacker.pack(x, y, z))) required.merge(item, 1, Integer::sum);
    }

    boolean reserve() {
        if (required.isEmpty()) return true;
        NBTTagCompound persisted = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagList escrow = persisted.getTagList(ESCROW, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < escrow.tagCount(); i++) {
            NBTTagCompound entry = escrow.getCompoundTagAt(i);
            ItemStack item = ItemStack.loadItemStackFromNBT(entry);
            if (item != null) balances.put(item, entry);
        }
        boolean enough = true;
        for (var need : required.entrySet()) {
            ItemStack item = need.getKey();
            int amount = unreserved(item, need.getValue());
            int shortage = amount - available(player, item, amount);
            if (shortage > 0) {
                if (enough) reportShortage();
                reportItem(item, shortage);
                enough = false;
            }
        }
        if (!enough) return false;

        // Multiple terminals may expose the same grid, and simulated extraction need
        // not test the terminal's charge. Commit every reservation before any placement.
        player.getEntityData()
            .setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        persisted.setTag(ESCROW, escrow);
        for (var need : required.entrySet()) {
            ItemStack item = need.getKey();
            int amount = unreserved(item, need.getValue());
            if (amount == 0) continue;
            int actual = extract(player, item, amount, false);
            if (actual > 0) {
                NBTTagCompound entry = balances.get(item);
                if (entry == null) {
                    entry = new NBTTagCompound();
                    ItemStack unit = item.copy();
                    unit.stackSize = 1;
                    unit.writeToNBT(entry);
                    escrow.appendTag(entry);
                    balances.put(item, entry);
                }
                entry.setInteger("Remaining", entry.getInteger("Remaining") + actual);
            }
            if (actual != amount) {
                reportShortage();
                reportItem(item, amount - actual);
                return false;
            }
        }
        return true;
    }

    private int unreserved(ItemStack item, int amount) {
        NBTTagCompound balance = balances.get(item);
        return balance == null ? amount : Math.max(0, amount - balance.getInteger("Remaining"));
    }

    public static int available(EntityPlayerMP player, ItemStack item, int amount) {
        return extract(player, item, amount, true);
    }

    private static int extract(EntityPlayerMP player, ItemStack item, int amount, boolean simulate) {
        if (amount == 0) return 0;
        ItemStack request = item.copy();
        request.stackSize = amount;
        // Same exact-stack inventory and terminal extractors as IItemSource.fromPlayer.
        // takeAll only returns a boolean; here we need the actual amount even on a partial extraction.
        return InventoryUtility.takeFromInventory(player, request, simulate);
    }

    private void reportShortage() {
        // #tr gtneverreset.projector.build.missing
        // # Missing materials. No blocks placed. Restock and press Generate again.
        // # zh_CN 材料不足，本次未放置方块。补齐后再次点击生成。
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.build.missing"));
    }

    private void reportItem(ItemStack stack, int amount) {
        // #tr gtneverreset.projector.build.missing_item
        // # Missing %s × %s
        // # zh_CN 缺少 %s × %s
        player.addChatMessage(
            new ChatComponentTranslation("gtneverreset.projector.build.missing_item", stack.func_151000_E(), amount));
    }

    boolean canPlace(int x, int y, int z, ItemStack stack) {
        NBTTagCompound entry = balances.get(stack);
        return missingPositions.contains(CoordinatePacker.pack(x, y, z)) && entry != null
            && entry.getInteger("Remaining") > 0;
    }

    void placed(int x, int y, int z, ItemStack stack) {
        missingPositions.remove(CoordinatePacker.pack(x, y, z));
        NBTTagCompound entry = balances.get(stack);
        entry.setInteger("Remaining", entry.getInteger("Remaining") - 1);
    }

    static boolean refund(EntityPlayerMP player) {
        NBTTagCompound persisted = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagList entries = persisted.getTagList(ESCROW, Constants.NBT.TAG_COMPOUND);
        boolean dirty = false;
        for (int i = entries.tagCount() - 1; i >= 0; i--) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            int amount = entry.getInteger("Remaining");
            if (amount > 0) {
                ItemStack item = ItemStack.loadItemStackFromNBT(entry);
                if (item == null) continue;
                int left = returnToInventory(player, item, amount);
                if (left != amount) {
                    dirty = true;
                    entry.setInteger("Remaining", left);
                }
                if (left > 0) continue;
            }
            entries.removeTag(i);
        }
        if (entries.tagCount() == 0) persisted.removeTag(ESCROW);
        if (dirty) {
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }
        return entries.tagCount() > 0;
    }

    private static int returnToInventory(EntityPlayerMP player, ItemStack item, int amount) {
        ItemStack[] inventory = player.inventory.mainInventory;
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < inventory.length && amount > 0; i++) {
            ItemStack slot = inventory[i];
            if (pass == 0) {
                if (slot == null || !slot.isStackable()
                    || !item.isItemEqual(slot)
                    || !ItemStack.areItemStackTagsEqual(item, slot)) continue;
            } else {
                if (slot != null) continue;
                slot = item.copy();
                slot.stackSize = 0;
                inventory[i] = slot;
            }
            int moved = Math.min(
                amount,
                Math.max(
                    0,
                    Math.min(slot.getMaxStackSize(), player.inventory.getInventoryStackLimit()) - slot.stackSize));
            slot.stackSize += moved;
            amount -= moved;
        }
        return amount;
    }

    public static final class Refunds {

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP player)
                || player.isDead
                || player.ticksExisted % 20 != 0
                || !player.getEntityData()
                    .hasKey(EntityPlayer.PERSISTED_NBT_TAG))
                return;
            if (player.getEntityData()
                .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
                .hasKey(ESCROW) && !ProjectorBuilder.INSTANCE.building(player)) refund(player);
        }
    }
}
