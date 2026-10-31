package com.xyp.gtneverreset.client.gui.radialmenu;

import com.xyp.gtnrlib.client.gui.ConfirmationScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialConfirmGui extends ConfirmationScreen {

    public RadialConfirmGui(String questionKey, String subject, Result result) {
        // #tr radialmenu.profiles.delete
        // # Delete
        // # zh_CN 删除
        super(questionKey, subject, "radialmenu.profiles.delete", result);
    }

    public RadialConfirmGui(String questionKey, String subject, String confirmKey, Result result) {
        super(questionKey, subject, confirmKey, result);
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.pop();
    }
}
