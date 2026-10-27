package com.xyp.gtneverreset.client.action.radialmenu;

import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IActionExecutor {

    String typeId();

    boolean execute(ActionSpec spec);
}
