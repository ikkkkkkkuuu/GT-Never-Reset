package com.xyp.gtneverreset.client.render;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

@SideOnly(Side.CLIENT)
public final class VaultConnectedTextureRender extends GTCopiedBlockTextureRender {

    private final Block block;

    public VaultConnectedTextureRender(Block block) {
        super(block, ForgeDirection.UNKNOWN.ordinal(), 0, Dyes._NULL.getRGBA());
        this.block = block;
    }

    @Override
    public IIcon getIcon(int side, @Nullable ISBRContext context) {
        if (context instanceof ISBRWorldContext world) {
            return block.getIcon(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(), side);
        }
        return block.getIcon(side, 0);
    }
}
