package com.xyp.gtneverreset.client.gui.radialmenu;

import com.xyp.gtnrlib.client.gui.MultilineEditorScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialMultilineEditorGui extends MultilineEditorScreen {

    public RadialMultilineEditorGui(String titleKey, String initialText, Result result) {
        super(titleKey, initialText, result);
    }

    @Override
    protected String hintKey() {
        // #tr radialmenu.lines.hint
        // # Placeholders: {player} {dim} {x} {y} {z}
        // # zh_CN 占位符：{player} {dim} {x} {y} {z}
        return "radialmenu.lines.hint";
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.pop();
    }
}
