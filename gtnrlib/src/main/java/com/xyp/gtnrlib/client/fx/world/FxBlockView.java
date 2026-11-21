package com.xyp.gtnrlib.client.fx.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/**
 * A view of chosen blocks of a source world at their own positions, fully lit. Every other position reads as air, so
 * block renderers draw the faces that border it. {@link #shown} hides blocks without rebuilding the view.
 */
@SideOnly(Side.CLIENT)
public class FxBlockView implements IBlockAccess {

    private static final int FULL_BRIGHT = 15 << 20 | 15 << 4;

    public final Long2IntOpenHashMap cells = new Long2IntOpenHashMap();
    public World source;
    public boolean[] shown;

    public FxBlockView() {
        cells.defaultReturnValue(-1);
    }

    public int cellAt(int x, int y, int z) {
        int cell = cells.get(FxVoxelOutline.pack(x, y, z));
        return cell < 0 || shown != null && !shown[cell] ? -1 : cell;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return cellAt(x, y, z) < 0 ? Blocks.air : source.getBlock(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return cellAt(x, y, z) < 0 ? null : source.getTileEntity(x, y, z);
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) {
        return FULL_BRIGHT;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return cellAt(x, y, z) < 0 ? 0 : source.getBlockMetadata(x, y, z);
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return cellAt(x, y, z) < 0;
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.plains;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
        return cellAt(x, y, z) >= 0 && source.getBlock(x, y, z)
            .isSideSolid(this, x, y, z, side);
    }
}
