package com.xyp.gtneverreset.client;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import tectech.thing.CustomItemList;

public final class GTNRCreativeTabs {

    // #tr itemGroup.gtneverreset
    // # GT-Never-Reset
    // # zh_CN GT-Never-Reset
    public static final CreativeTabs ITEMS = new CreativeTabs(GTNeverReset.MODID) {

        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return GTNRItemList.WirelessDualInterfaceTerminal.getItem();
        }
    };

    // #tr itemGroup.gtneverreset.blocks
    // # GT-Never-Reset - Blocks
    // # zh_CN GT-Never-Reset - 方块
    public static final CreativeTabs BLOCKS = new CreativeTabs(GTNeverReset.MODID + ".blocks") {

        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return CustomItemList.eM_Power.getItem();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack getIconItemStack() {
            return CustomItemList.eM_Power.get(1);
        }
    };

    // #tr itemGroup.gtneverreset.machines
    // # GT-Never-Reset - Machines
    // # zh_CN GT-Never-Reset - 机器
    public static final CreativeTabs MACHINES = new CreativeTabs(GTNeverReset.MODID + ".machines") {

        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return CustomItemList.Machine_Multi_EyeOfHarmony.getItem();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack getIconItemStack() {
            return CustomItemList.Machine_Multi_EyeOfHarmony.get(1);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void displayAllReleventItems(List<ItemStack> items) {
            super.displayAllReleventItems(items);
            // GregTech machines share one Item, so add only this addon's registered machine stacks.
            for (GTNRItemList entry : GTNRItemList.values()) {
                if (entry.hasBeenSet() && entry.getBlock() == GregTechAPI.sBlockMachines) {
                    items.add(entry.get(1));
                }
            }
        }
    };

}
