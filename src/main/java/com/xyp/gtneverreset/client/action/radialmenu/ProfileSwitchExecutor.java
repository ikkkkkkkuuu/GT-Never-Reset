package com.xyp.gtneverreset.client.action.radialmenu;

import com.xyp.gtneverreset.client.keybind.KeybindStateTracker;
import com.xyp.gtneverreset.client.profile.radialmenu.ProfileManager;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;

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

        KeybindStateTracker.releaseAll();

        if (target == null || target.trim()
            .isEmpty()) {
            return ProfileManager.cycle(1);
        }
        return ProfileManager.switchTo(target.trim());
    }
}
