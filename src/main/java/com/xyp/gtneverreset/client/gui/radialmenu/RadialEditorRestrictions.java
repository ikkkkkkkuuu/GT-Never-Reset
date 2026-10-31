package com.xyp.gtneverreset.client.gui.radialmenu;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class RadialEditorRestrictions {

    static String builtInTip() {
        // #tr radialmenu.editor.builtIn.tip
        // # Store and retrieve items from the wheel. Edit the toolbelt entry separately
        // # zh_CN 在轮盘中存取物品；腰带入口可单独编辑
        return I18n.format("radialmenu.editor.builtIn.tip");
    }

    static String resizeTip() {
        // #tr radialmenu.editor.toolbelt.shared
        // # All profiles share the same items and slot count
        // # zh_CN 所有配置方案共用物品库存和槽位数量
        String shared = I18n.format("radialmenu.editor.toolbelt.shared");
        // #tr radialmenu.editor.toolbelt.shrink
        // # Empty the affected slots before reducing the slot count
        // # zh_CN 缩减槽位前，先清空将被移除的槽位
        return shared + "\n" + I18n.format("radialmenu.editor.toolbelt.shrink");
    }
}
