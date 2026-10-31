package com.xyp.gtnrlib.world;

import com.xyp.gtnrlib.utils.MathUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class Facing {

    private static final String[] COMPASS = { "south", "west", "north", "east" };

    public static double normalizeYaw(double yaw) {
        return MathUtils.normalizeDegrees(yaw);
    }

    public static String of(double yaw) {
        int quarter = (int) Math.floor(normalizeYaw(yaw) / 90.0 + 0.5) & 3;
        return COMPASS[quarter];
    }
}
