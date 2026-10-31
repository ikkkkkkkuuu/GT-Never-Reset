package com.xyp.gtneverreset.common.block.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.item.items.ItemWirelessConnector;
import com.xyp.gtnrlib.mod.ModList;

public final class BlockOverloadedProvider extends Block {

    public BlockOverloadedProvider() {
        super(Material.iron);
        // #tr tile.overloaded_wireless_pattern_provider.name
        // # Overloaded Wireless Pattern Provider
        // # zh_CN 过载无线样板供应器
        setBlockName("overloaded_wireless_pattern_provider");
        setBlockTextureName(ModList.GTNeverReset.getResourcePath("overloaded_provider"));
        setCreativeTab(GTNRCreativeTabs.BLOCKS);
        setHardness(3);
        setResistance(10);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityOverloadedProvider();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && placer instanceof EntityPlayer player
            && world.getTileEntity(x, y, z) instanceof TileEntityOverloadedProvider tile) {
            tile.setOwnerName(player.getCommandSenderName());
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player.getHeldItem() != null && player.getHeldItem()
            .getItem() instanceof ItemWirelessConnector) return false;
        if (world.getTileEntity(x, y, z) instanceof TileEntityOverloadedProvider tile
            && (world.isRemote || tile.canConfigure(player))) {
            if (!world.isRemote) TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
            return true;
        }
        return world.isRemote;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileEntityOverloadedProvider tile) {
            for (ItemStack stack : tile.takeDrops()) {

                if (stack != null) world.spawnEntityInWorld(new EntityItem(world, x + .5, y + .5, z + .5, stack));
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
