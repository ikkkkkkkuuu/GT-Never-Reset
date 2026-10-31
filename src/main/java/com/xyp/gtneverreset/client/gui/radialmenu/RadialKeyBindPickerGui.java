package com.xyp.gtneverreset.client.gui.radialmenu;

import com.xyp.gtnrlib.client.gui.KeyBindingPickerScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialKeyBindPickerGui extends KeyBindingPickerScreen {

    public RadialKeyBindPickerGui(Callback callback) {
        super(callback);
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.pop();
    }
}
