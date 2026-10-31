package com.xyp.gtnrlib.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WorldTime {

    public static final long DAY = 24000L;

    public static final long DUSK = 12000L;

    public static long timeOfDay(long worldTime) {
        long time = worldTime % DAY;
        return time < 0 ? time + DAY : time;
    }

    public static long day(long worldTime) {
        return Math.floorDiv(worldTime, DAY);
    }

    public static boolean isDay(long worldTime) {
        return timeOfDay(worldTime) < DUSK;
    }
}
