package com.xyp.gtneverreset.common.item.items;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.client.GTNRCreativeTabs;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class PermanentMagnetRing extends Item implements IBauble {

    private static final double RANGE = 64.0;
    private static final int INTERVAL = 10;
    private static final String LAST_SCAN = "gtneverreset.magnetLastScan";

    public PermanentMagnetRing() {
        // #tr item.gtneverreset.permanent_magnet_ring.name
        // # Permanent Magnet Ring
        // # zh_CN 永磁铁指环
        setUnlocalizedName("gtneverreset.permanent_magnet_ring");
        setTextureName("gtneverreset:permanent_magnet_ring");
        setMaxStackSize(1);
        setCreativeTab(GTNRCreativeTabs.ITEMS);
        GameRegistry.registerItem(this, "PermanentMagnetRing");
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer player) || player.worldObj.isRemote
            || player.isDead
            || player.isSneaking()
            || player.ticksExisted % INTERVAL != 0) return;
        long tick = player.worldObj.getTotalWorldTime();
        if (player.getEntityData()
            .hasKey(LAST_SCAN)
            && player.getEntityData()
                .getLong(LAST_SCAN) == tick)
            return;
        player.getEntityData()
            .setLong(LAST_SCAN, tick);
        List<EntityItem> items = player.worldObj
            .getEntitiesWithinAABB(EntityItem.class, player.boundingBox.expand(RANGE, RANGE, RANGE));
        for (EntityItem item : items) {
            if (item.isDead || item.delayBeforeCanPickup > 0
                || item.getDistanceSqToEntity(player) > RANGE * RANGE
                || item.getEntityData()
                    .getBoolean("PreventRemoteMovement"))
                continue;
            // Use vanilla pickup so ownership, Forge pickup events and inventory capacity remain authoritative.
            item.onCollideWithPlayer(player);
        }
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return wearer instanceof EntityPlayer;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        // #tr tooltip.gtnr.permanent_magnet_ring
        // # Equip in a Baubles ring slot: collects dropped items within 64 blocks. Sneak to pause. No energy required.
        // # zh_CN 装备于 Baubles 戒指栏：收集半径64格内的掉落物，潜行暂停，无需能量。
        lines.add(StatCollector.translateToLocal("tooltip.gtnr.permanent_magnet_ring"));
    }
}
