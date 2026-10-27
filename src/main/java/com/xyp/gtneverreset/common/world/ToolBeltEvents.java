package com.xyp.gtneverreset.common.world;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.xyp.gtneverreset.utils.inventory.ToolBeltData;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class ToolBeltEvents {

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer) {
            ToolBeltData.register((EntityLivingBase) event.entity);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(player);
        if (data != null) {
            data.syncToSelf();
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        EntityPlayer player = event.player;
        if (player.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(player);
        if (data != null) {
            data.syncToSelf();
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(event.player);
        if (data != null) data.syncToSelf();
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        EntityLivingBase entity = event.entityLiving;
        if (entity.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(entity);
        if (data == null) return;
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.worldObj.getGameRules()
                .getGameRuleBooleanValue("keepInventory") || player.capabilities.isCreativeMode) return;
        }
        boolean changed = false;
        for (int i = 0; i < data.getSlotCount(); i++) {
            ItemStack stack = data.getStackInSlot(i);
            if (stack != null) {
                event.drops.add(new EntityItem(entity.worldObj, entity.posX, entity.posY, entity.posZ, stack.copy()));
                data.setStackInSlotSilent(i, null);
                changed = true;
            }
        }
        if (changed) data.onContentsChanged();
    }

    @SubscribeEvent
    public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        EntityPlayer oldPlayer = event.original;
        EntityPlayer newPlayer = event.entityPlayer;
        if (newPlayer.worldObj.isRemote) return;

        ToolBeltData oldData = ToolBeltData.get(oldPlayer);
        ToolBeltData newData = ToolBeltData.get(newPlayer);

        if (oldData != null && newData != null) {
            ItemStack[] snapshot = new ItemStack[oldData.getSlotCount()];
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i] = oldData.getStackInSlot(i);
            }
            newData.applySnapshot(snapshot);
        }
    }
}
