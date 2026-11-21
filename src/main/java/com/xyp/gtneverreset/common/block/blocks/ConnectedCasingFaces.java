package com.xyp.gtneverreset.common.block.blocks;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

final class ConnectedCasingFaces {

    interface Matcher {

        boolean matches(IBlockAccess world, int x, int y, int z, int side);
    }

    private ConnectedCasingFaces() {}

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int DOWN = 4;
    public static final int UP = 8;
    public static final int DOWN_LEFT = 16;
    public static final int DOWN_RIGHT = 32;
    public static final int UP_LEFT = 64;
    public static final int UP_RIGHT = 128;

    // Increasing U/V directions in vanilla RenderBlocks' unrotated face UVs.
    private static final ForgeDirection[] RIGHT_DIRECTIONS = { ForgeDirection.EAST, ForgeDirection.EAST,
        ForgeDirection.WEST, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.NORTH };
    private static final ForgeDirection[] DOWN_DIRECTIONS = { ForgeDirection.SOUTH, ForgeDirection.SOUTH,
        ForgeDirection.DOWN, ForgeDirection.DOWN, ForgeDirection.DOWN, ForgeDirection.DOWN };

    public static int getMask(IBlockAccess world, int x, int y, int z, int side, Matcher matcher) {
        if (world == null || side < 0 || side >= ForgeDirection.VALID_DIRECTIONS.length) return 0;
        ForgeDirection face = ForgeDirection.VALID_DIRECTIONS[side];
        if (!connects(world, x, y, z, face, matcher)) return 0;
        ForgeDirection right = RIGHT_DIRECTIONS[side];
        ForgeDirection down = DOWN_DIRECTIONS[side];
        int mask = 0;
        if (connects(world, x - right.offsetX, y - right.offsetY, z - right.offsetZ, face, matcher)) mask |= LEFT;
        if (connects(world, x + right.offsetX, y + right.offsetY, z + right.offsetZ, face, matcher)) mask |= RIGHT;
        if (connects(world, x + down.offsetX, y + down.offsetY, z + down.offsetZ, face, matcher)) mask |= DOWN;
        if (connects(world, x - down.offsetX, y - down.offsetY, z - down.offsetZ, face, matcher)) mask |= UP;
        if ((mask & (LEFT | DOWN)) == (LEFT | DOWN) && connects(
            world,
            x - right.offsetX + down.offsetX,
            y - right.offsetY + down.offsetY,
            z - right.offsetZ + down.offsetZ,
            face,
            matcher)) mask |= DOWN_LEFT;
        if ((mask & (RIGHT | DOWN)) == (RIGHT | DOWN) && connects(
            world,
            x + right.offsetX + down.offsetX,
            y + right.offsetY + down.offsetY,
            z + right.offsetZ + down.offsetZ,
            face,
            matcher)) mask |= DOWN_RIGHT;
        if ((mask & (LEFT | UP)) == (LEFT | UP) && connects(
            world,
            x - right.offsetX - down.offsetX,
            y - right.offsetY - down.offsetY,
            z - right.offsetZ - down.offsetZ,
            face,
            matcher)) mask |= UP_LEFT;
        if ((mask & (RIGHT | UP)) == (RIGHT | UP) && connects(
            world,
            x + right.offsetX - down.offsetX,
            y + right.offsetY - down.offsetY,
            z + right.offsetZ - down.offsetZ,
            face,
            matcher)) mask |= UP_RIGHT;
        return mask;
    }

    public static int normalizeMask(int mask) {
        int result = mask & 15;
        if ((result & (LEFT | DOWN)) == (LEFT | DOWN)) result |= mask & DOWN_LEFT;
        if ((result & (RIGHT | DOWN)) == (RIGHT | DOWN)) result |= mask & DOWN_RIGHT;
        if ((result & (LEFT | UP)) == (LEFT | UP)) result |= mask & UP_LEFT;
        if ((result & (RIGHT | UP)) == (RIGHT | UP)) result |= mask & UP_RIGHT;
        return result;
    }

    private static boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection face, Matcher matcher) {
        if (!matcher.matches(world, x, y, z, face.ordinal())) return false;
        int frontX = x + face.offsetX;
        int frontY = y + face.offsetY;
        int frontZ = z + face.offsetZ;
        Block cover = world.getBlock(frontX, frontY, frontZ);
        if (cover.isOpaqueCube() || world.isSideSolid(frontX, frontY, frontZ, face.getOpposite(), false)) return false;
        return true;
    }

}
