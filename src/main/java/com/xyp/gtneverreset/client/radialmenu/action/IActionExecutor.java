package com.xyp.gtneverreset.client.radialmenu.action;

import com.xyp.gtneverreset.client.radialmenu.ActionSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IActionExecutor {

    String typeId();

    boolean execute(ActionSpec spec);
}
