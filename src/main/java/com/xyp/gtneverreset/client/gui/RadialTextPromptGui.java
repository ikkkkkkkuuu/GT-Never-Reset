package com.xyp.gtneverreset.client.gui;

import com.xyp.gtnrlib.client.gui.TextPromptScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialTextPromptGui extends TextPromptScreen {

    public RadialTextPromptGui(String promptKey, String initialValue, Result result) {
        super(promptKey, initialValue, result);
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.pop();
    }
}
