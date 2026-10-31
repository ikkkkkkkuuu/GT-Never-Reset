package com.xyp.gtneverreset.common.block.blocks;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityStockIOInterface;
import com.xyp.gtneverreset.common.gui.StockIOGui;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class BlockStockIOInterface extends Block {

    public static final String REGISTRY_NAME = "stock_io_interface";

    public BlockStockIOInterface() {
        super(Material.iron);
        // #tr tile.gtneverreset.stock_io_interface.name
        // # ME Stock IO Interface
        // # zh_CN 库存 IO 接口 (ME)
        setBlockName("gtneverreset.stock_io_interface");
        setHardness(3.0f);
        setResistance(10.0f);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityStockIOInterface();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(ModList.AE2.getResourcePath("interface/BlockInterface_Purple"));
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!(world.getTileEntity(x, y, z) instanceof TileEntityStockIOInterface tile)) return false;
        if (!world.isRemote) StockIOGui.FACTORY.open(player, tile);
        return true;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        return ForgeDirection.getOrientation(side)
            .getOpposite()
            .ordinal();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileEntityStockIOInterface tile) {
            if (placer instanceof EntityPlayer player) tile.setOwnerName(player.getCommandSenderName());
            if (stack.hasTagCompound()) tile.getLogic()
                .readContents(stack.getTagCompound());
            tile.getLogic()
                .setTargetSide(ForgeDirection.getOrientation(world.getBlockMetadata(x, y, z)));
            tile.markDirty();
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        ItemStack portable = new ItemStack(this);
        if (world.getTileEntity(x, y, z) instanceof TileEntityStockIOInterface tile) {
            portable.setTagCompound(
                tile.getLogic()
                    .getRemovalContents());
        }
        drops.add(portable);
        return drops;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        super.harvestBlock(world, player, x, y, z, metadata);
        world.setBlockToAir(x, y, z);
    }
}
