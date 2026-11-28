package com.xyp.gtneverreset.common.projector;

import java.util.Collection;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.xyp.gtneverreset.common.wireless.WirelessCardInventory;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

final class ProjectorRemovedItems {

    private static final String RECOVERED = "gtneverresetProjectorRecovered";

    private ProjectorRemovedItems() {}

    static boolean pending(EntityPlayerMP player) {
        return player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
            .hasKey(RECOVERED);
    }

    static void save(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagList recovered = persisted.getTagList(RECOVERED, Constants.NBT.TAG_COMPOUND);
        NBTTagCompound entry = new NBTTagCompound();
        ItemStack unit = stack.copy();
        unit.stackSize = 1;
        unit.writeToNBT(entry);
        entry.setInteger("Amount", stack.stackSize);
        recovered.appendTag(entry);
        persisted.setTag(RECOVERED, recovered);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
    }

    static boolean deliver(EntityPlayerMP player, Collection<ProjectorWirelessAccess.Link> networks) {
        NBTTagCompound persisted = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagList recovered = persisted.getTagList(RECOVERED, Constants.NBT.TAG_COMPOUND);
        boolean blocked = false;
        for (int i = recovered.tagCount() - 1; i >= 0; i--) {
            NBTTagCompound entry = recovered.getCompoundTagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
            int amount = entry.getInteger("Amount");
            if (stack == null) continue;
            for (ProjectorWirelessAccess.Link network : networks) {
                if (amount <= 0) break;
                int rest = insert(network, stack, amount);
                if (rest != amount) {
                    amount = rest;
                    entry.setInteger("Amount", amount);
                    WirelessCardInventory.sync(
                        player,
                        network.host()
                            .getItemStack());
                }
            }
            if (amount > 0) {
                int rest = ProjectorBuildMaterials.returnToInventory(player, stack, amount);
                if (rest != amount) {
                    amount = rest;
                    entry.setInteger("Amount", amount);
                    player.inventory.markDirty();
                }
            }
            if (amount > 0) blocked = true;
            else recovered.removeTag(i);
        }
        if (recovered.tagCount() == 0) persisted.removeTag(RECOVERED);
        return blocked;
    }

    private static int insert(ProjectorWirelessAccess.Link network, ItemStack item, int count) {
        var host = network.host();
        var storage = host.getItemInventory();
        if (storage == null) return count;
        IAEItemStack request = AEItemStack.create(item);
        if (request == null) return count;
        request.setStackSize(count);
        IAEItemStack rest = Platform.poweredInsert(host, storage, request, network.source());
        return rest == null ? 0 : (int) Math.max(0, Math.min(count, rest.getStackSize()));
    }
}
