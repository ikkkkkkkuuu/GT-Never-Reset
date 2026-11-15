package com.xyp.gtneverreset.common.block.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.render.TextureFactory;

public final class BlockChaosCasing extends Block {

    private final String textureDirectory;
    private final int[] variants;
    private final int textureBase;
    private final boolean glass;
    private final Map<Integer, ItemStack[]> breakDrops = new HashMap<>();
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockChaosCasing(String name, String textureDirectory, int textureBase, boolean glass, int... variants) {
        super(glass ? Material.glass : Material.iron);
        this.textureDirectory = textureDirectory;
        this.variants = variants.clone();
        this.textureBase = textureBase;
        this.glass = glass;
        setBlockName("gtneverreset." + name);
        setCreativeTab(GTNRCreativeTabs.BLOCKS);
        setHardness(9);
        setResistance(5);
        setHarvestLevel("wrench", 1);
        GregTechAPI.registerMachineBlock(this, -1);
        for (int meta : variants) {
            Textures.BlockIcons.setCasingTextureForId(getTextureIndex(meta), TextureFactory.of(this, meta));
        }
    }

    public void setBreakDrops(int meta, ItemStack[] drops) {
        ItemStack[] copy = new ItemStack[drops.length];
        for (int i = 0; i < drops.length; i++) copy[i] = drops[i].copy();
        breakDrops.put(meta, copy);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        ItemStack[] configured = breakDrops.get(meta);
        if (configured == null) return super.getDrops(world, x, y, z, meta, fortune);
        ArrayList<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : configured) result.add(stack.copy());
        return result;
    }

    public int getTextureIndex(int meta) {
        return textureBase + meta;
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public boolean isOpaqueCube() {
        return !glass;
    }

    @Override
    public int getRenderBlockPass() {
        return glass ? 1 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[16];
        for (int meta : variants) icons[meta] = register.registerIcon(
            GTNeverReset.MODID + ":"
                + (textureDirectory.equals("MultiUseCore") ? "MultiUseCore_Default" : textureDirectory + "/" + meta));
        blockIcon = icons[variants[0]];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return meta >= 0 && meta < icons.length && icons[meta] != null ? icons[meta] : blockIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> items) {
        for (int meta : variants) items.add(new ItemStack(this, 1, meta));
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        GregTechAPI.causeMachineUpdate(world, x, y, z);
    }
}
