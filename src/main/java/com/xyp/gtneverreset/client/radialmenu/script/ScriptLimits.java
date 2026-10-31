package com.xyp.gtneverreset.client.radialmenu.script;

import com.xyp.gtnrlib.gui.wheel.SlotLayout;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ScriptLimits {

    public final int instructionsPerResume;

    public final long nanosPerResume;

    public final int maxMenuItems;

    public static final ScriptLimits DEFAULT = new ScriptLimits(200_000, 20_000_000L, SlotLayout.MAX_SLOTS);

    public ScriptLimits(int instructionsPerResume, long nanosPerResume, int maxMenuItems) {
        this.instructionsPerResume = instructionsPerResume;
        this.nanosPerResume = nanosPerResume;
        this.maxMenuItems = maxMenuItems;
    }
}
