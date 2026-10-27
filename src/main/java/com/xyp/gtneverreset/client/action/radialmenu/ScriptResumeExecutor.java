package com.xyp.gtneverreset.client.action.radialmenu;

import com.xyp.gtneverreset.client.script.radialmenu.ScriptHost;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ScriptResumeExecutor implements IActionExecutor {

    @Override
    public String typeId() {
        return ActionTypes.SCRIPT_RESUME;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        String token = spec.getString(ActionTypes.PARAM_TOKEN, null);
        int choice = spec.getInt(ActionTypes.PARAM_CHOICE, 0);
        if (token == null || choice <= 0) {
            return false;
        }

        ScriptHost.choose(token, choice);
        return true;
    }
}
