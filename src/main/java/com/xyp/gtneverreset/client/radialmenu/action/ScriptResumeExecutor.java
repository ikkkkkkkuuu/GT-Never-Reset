package com.xyp.gtneverreset.client.radialmenu.action;

import com.xyp.gtneverreset.client.radialmenu.ActionSpec;
import com.xyp.gtneverreset.client.radialmenu.ActionTypes;
import com.xyp.gtneverreset.client.radialmenu.script.ScriptHost;

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
