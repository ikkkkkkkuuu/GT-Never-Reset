package com.xyp.gtneverreset.client.gui.radialmenu;

import com.xyp.gtnrlib.gui.widget.ScrollablePanelScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class RadialUiScreen extends ScrollablePanelScreen {

    @Override
    protected void onCancel() {
        RadialGuiStack.pop();
    }
}
