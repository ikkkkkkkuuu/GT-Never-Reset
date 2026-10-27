package com.xyp.gtneverreset.client.script.radialmenu;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ChunkCoords {

    public static final int SIZE = 16;

    private ChunkCoords() {}

    public static int chunkOf(int world) {
        return world >> 4;
    }

    public static int offsetOf(int world) {
        return world & 15;
    }

    public static int toWorld(int chunk, int offset) {
        return chunk * SIZE + offset;
    }
}
