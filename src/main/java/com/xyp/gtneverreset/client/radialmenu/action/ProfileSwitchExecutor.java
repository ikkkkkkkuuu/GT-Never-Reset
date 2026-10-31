package com.xyp.gtneverreset.client.radialmenu.action;

import static com.xyp.gtneverreset.client.radialmenu.RadialMenuInputHandler.KEYS;

import com.xyp.gtneverreset.client.radialmenu.ActionSpec;
import com.xyp.gtneverreset.client.radialmenu.ActionTypes;
import com.xyp.gtneverreset.client.radialmenu.profile.ProfileManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProfileSwitchExecutor implements IActionExecutor {

    @Override
    public String typeId() {
        return ActionTypes.PROFILE_SWITCH;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        String target = spec.getString(ActionTypes.PARAM_PROFILE, null);

        KEYS.releaseAll();

        if (target == null || target.trim()
            .isEmpty()) {
            return ProfileManager.cycle(1);
        }
        return ProfileManager.switchTo(target.trim());
    }
}
