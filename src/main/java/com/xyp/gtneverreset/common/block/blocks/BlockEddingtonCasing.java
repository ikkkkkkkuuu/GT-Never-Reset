package com.xyp.gtneverreset.common.block.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.machine.storage.VaultCasingConnections;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.ITexture;

public final class BlockEddingtonCasing extends Block {

    public static final String REGISTRY_NAME = "eddington_wall_casing";
    private final ITexture casingTexture;
    @SideOnly(Side.CLIENT)
    private IIcon[] connectedIcons;

    public BlockEddingtonCasing() {
        super(Material.iron);
        // #tr tile.gtneverreset.eddington_wall_casing.name
        // # Eddington Wall Casing
        // # zh_CN 爱丁顿壁外壳
        setBlockName(ModList.GTNeverReset.getResourceLocation() + ".eddington_wall_casing");
        setBlockTextureName(ModList.GTNeverReset.getResourcePath("stellar/sl_eddington"));
        setHardness(5.0f);
        setResistance(10.0f);
        setStepSound(soundTypeMetal);
        setHarvestLevel("wrench", 2);
        setCreativeTab(GTNRCreativeTabs.BLOCKS);
        GregTechAPI.registerMachineBlock(this, 1);
        casingTexture = GTNeverReset.proxy.createVaultCasingTexture(this);
    }

    public ITexture getCasingTexture() {
        return casingTexture;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        super.breakBlock(world, x, y, z, block, metadata);
        GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public boolean canBeReplacedByLeaves(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        IIcon[] variants = new IIcon[256];
        for (int mask = 0; mask < variants.length; mask++) {
            if (VaultCasingConnections.normalizeMask(mask) == mask) {
                variants[mask] = register
                    .registerIcon(ModList.GTNeverReset.getResourcePath("stellar/ctm/eddington_" + mask));
            }
        }
        for (int mask = 0; mask < variants.length; mask++) {
            variants[mask] = variants[VaultCasingConnections.normalizeMask(mask)];
        }
        connectedIcons = variants;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        IIcon icon = connectedIcons == null ? null
            : connectedIcons[VaultCasingConnections.getMask(world, x, y, z, side) & 255];
        return icon == null ? blockIcon : icon;
    }
}
