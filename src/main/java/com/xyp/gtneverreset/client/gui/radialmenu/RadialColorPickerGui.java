package com.xyp.gtneverreset.client.gui.radialmenu;

import com.xyp.gtnrlib.client.gui.ColorPickerScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialColorPickerGui extends ColorPickerScreen {

    public RadialColorPickerGui(String initialHex, Result result) {
        super(initialHex, result);
    }

    public RadialColorPickerGui(String initialHex, boolean withAlpha, Result result) {
        super(initialHex, withAlpha, result);
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.pop();
    }
}
