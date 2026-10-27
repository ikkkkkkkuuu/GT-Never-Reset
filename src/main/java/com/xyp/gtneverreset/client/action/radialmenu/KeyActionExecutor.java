package com.xyp.gtneverreset.client.action.radialmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.keybind.KeyBindingLookup;
import com.xyp.gtneverreset.client.keybind.KeybindStateTracker;
import com.xyp.gtneverreset.client.keybind.VanillaKeyEffects;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;
import com.xyp.gtneverreset.utils.gui.radialmenu.Modifiers;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class KeyActionExecutor implements IActionExecutor {

    private static final int SNEAK_LEAD_TICKS = 1;

    private static final int SNEAK_HOLD_TICKS = 4;

    @Override
    public String typeId() {
        return ActionTypes.KEYBIND;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        String description = spec.getString(ActionTypes.PARAM_BINDING, null);
        String category = spec.getString(ActionTypes.PARAM_CATEGORY, null);

        KeyBinding binding = KeyBindingLookup.find(description, category);
        if (binding == null) {
            GTNeverReset.LOG.warn("Keybinding '" + description + "' is not registered by any mod");
            return false;
        }

        if (VanillaKeyEffects.tryRun(binding)) {
            return true;
        }

        KeybindStateTracker.Mode mode = spec
            .getEnum(ActionTypes.PARAM_MODE, KeybindStateTracker.Mode.class, KeybindStateTracker.Mode.Tap);
        int holdTicks = spec.getInt(ActionTypes.PARAM_HOLD_TICKS, 20);

        if (spec.getBoolean(ActionTypes.PARAM_SNEAK, false)) {
            return sneakThenPress(spec, mode, holdTicks);
        }

        boolean activated = KeybindStateTracker.activate(binding, mode, holdTicks, Modifiers.of(spec));
        SneakDiagnostics.log("pressed " + description, binding);
        return activated;
    }

    /** Player movement updates must observe sneaking before the synthetic key event. */
    private boolean sneakThenPress(ActionSpec spec, KeybindStateTracker.Mode mode, int holdTicks) {
        KeyBinding sneak = Minecraft.getMinecraft().gameSettings.keyBindSneak;

        int sneakTicks = mode == KeybindStateTracker.Mode.Hold ? Math.max(SNEAK_HOLD_TICKS, holdTicks)
            : SNEAK_HOLD_TICKS;
        KeybindStateTracker.activate(sneak, KeybindStateTracker.Mode.Hold, sneakTicks, Modifiers.NONE);
        SneakDiagnostics.log("sneak started", sneak);

        ActionSpec pressOnly = spec.copy();
        pressOnly.set(ActionTypes.PARAM_SNEAK, "false");
        DelayedActions.scheduleAction(spec, pressOnly, SNEAK_LEAD_TICKS);
        return true;
    }
}
