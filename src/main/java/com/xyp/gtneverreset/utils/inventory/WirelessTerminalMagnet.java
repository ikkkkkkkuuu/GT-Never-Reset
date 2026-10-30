package com.xyp.gtneverreset.utils.inventory;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import com.glodblock.github.common.Config;
import com.glodblock.github.inventory.item.WirelessMagnetCardFilterInventory;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.entity.EntityFloatingItem;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public final class WirelessTerminalMagnet {

    public static final String MODE_KEY = "gtnrMagnetMode";
    private static final String LAST_SCAN = "gtnrTerminalMagnetScan";
    private static final int SCAN_INTERVAL = 5;

    private WirelessTerminalMagnet() {}

    public static int getMode(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(MODE_KEY)) return 1;
        return Math.max(
            0,
            Math.min(
                2,
                stack.getTagCompound()
                    .getInteger(MODE_KEY)));
    }

    public static void tick(ItemStack stack, EntityPlayerMP player) {
        int mode = getMode(stack);
        if (mode == 0 || player.isDead || player.isSneaking() || player.ticksExisted % SCAN_INTERVAL != 0) return;
        long time = player.worldObj.getTotalWorldTime();
        if (player.getEntityData()
            .hasKey(LAST_SCAN)
            && player.getEntityData()
                .getLong(LAST_SCAN) == time)
            return;
        player.getEntityData()
            .setLong(LAST_SCAN, time);
        int slot = WirelessTerminalSession.findTerminalSlot(player, stack);
        if (slot < 0) return;
        double range = Math.max(0, Config.magnetRange);
        List<EntityItem> items = player.worldObj
            .getEntitiesWithinAABB(EntityItem.class, player.boundingBox.expand(range, range, range));
        var host = mode == 2 && !items.isEmpty() ? WirelessTerminalGuiHandler
            .buildHost(player, player.worldObj, slot, WirelessTerminalGuiType.WirelessCraftingTerminal) : null;
        if (host != null && (!host.rangeCheck() || host.getGrid() == null)) host = null;
        if (host != null) {
            ISecurityGrid security = host.getGrid()
                .getCache(ISecurityGrid.class);
            if (!security.hasPermission(player, SecurityPermissions.INJECT)) host = null;
        }
        WirelessMagnetCardFilterInventory filter = null;
        for (EntityItem entity : items) {
            if (entity.isDead || entity instanceof EntityFloatingItem
                || entity.delayBeforeCanPickup > 0
                || entity.getEntityData()
                    .getBoolean("PreventRemoteMovement")
                || player.worldObj.getClosestPlayerToEntity(entity, range) != player) continue;
            ItemStack dropped = entity.getEntityItem();
            if (dropped == null) continue;
            if (filter == null) {
                Platform.openNbtData(stack);
                filter = new WirelessMagnetCardFilterInventory(stack, slot, null, player);
            }
            if (!filter.isPassFilter(dropped)) continue;
            if (mode == 1 || host == null || host.getItemInventory() == null) {
                entity.onCollideWithPlayer(player);
                continue;
            }
            if (entity.func_145798_i() != null && !entity.func_145798_i()
                .equals(player.getCommandSenderName()) && entity.age < 5800) continue;
            if (MinecraftForge.EVENT_BUS.post(new EntityItemPickupEvent(player, entity))) continue;
            if (entity.isDead || entity.getEntityItem() == null) continue;
            dropped = entity.getEntityItem();
            int original = dropped.stackSize;
            var remainder = Platform.poweredInsert(
                host,
                host.getItemInventory(),
                AEItemStack.create(dropped.copy()),
                new PlayerSource(player, host));
            int left = remainder == null ? 0 : (int) remainder.getStackSize();
            if (left < original) {
                player.onItemPickup(entity, original - left);
                if (left == 0) entity.setDead();
                else {
                    ItemStack rest = dropped.copy();
                    rest.stackSize = left;
                    entity.setEntityItemStack(rest);
                }
            }
        }
        List<EntityXPOrb> orbs = player.worldObj
            .getEntitiesWithinAABB(EntityXPOrb.class, player.boundingBox.expand(range, range, range));
        for (EntityXPOrb orb : orbs) {
            if (!orb.isDead && player.worldObj.getClosestPlayerToEntity(orb, range) == player)
                orb.onCollideWithPlayer(player);
        }
        if (filter != null) WirelessTerminalSession.writeBackTerminal(player, slot, stack);
    }
}
