package com.xyp.gtnrlib.gui.widget;

import com.cleanroommc.modularui.widget.scroll.ScrollArea;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface IPersistentScrollbar {

    @SideOnly(Side.CLIENT)
    void drawTrack(ScrollArea area);
}
