package com.xyp.gtneverreset.client.radialmenu.action;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.client.radialmenu.ActionSpec;
import com.xyp.gtneverreset.client.radialmenu.ActionTypes;
import com.xyp.gtneverreset.client.radialmenu.script.ScriptHost;
import com.xyp.gtneverreset.config.ClientConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ScriptActionExecutor implements IActionExecutor {

    @Override
    public String typeId() {
        return ActionTypes.SCRIPT;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        if (!ClientConfig.radialMenu.enableScripts) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                // #tr radialmenu.script.disabled
                // # Scripts are disabled in the radial menu settings.
                // # zh_CN 快捷轮盘设置已禁用脚本。
                String message = StatCollector.translateToLocal("radialmenu.script.disabled");
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
            }
            return false;
        }
        return ScriptHost.start(spec);
    }
}
