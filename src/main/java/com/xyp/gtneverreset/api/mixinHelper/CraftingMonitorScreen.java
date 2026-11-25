package com.xyp.gtneverreset.api.mixinHelper;

import com.xyp.gtneverreset.client.gui.crafting.CraftingMonitorTooltip;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface CraftingMonitorScreen {

    CraftingMonitorTooltip gtnr$craftingMonitor();
}
