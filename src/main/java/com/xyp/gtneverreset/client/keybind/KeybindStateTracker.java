package com.xyp.gtneverreset.client.keybind;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.settings.KeyBinding;

import com.xyp.gtneverreset.utils.gui.radialmenu.Modifiers;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class KeybindStateTracker {

    private static final Map<KeyBinding, Held> HELD = new HashMap<>();

    public static boolean activate(KeyBinding binding, Mode mode, int holdTicks, Modifiers modifiers) {
        if (binding == null) {
            return false;
        }
        if (mode == Mode.Toggle && HELD.containsKey(binding)) {
            stop(binding);
            return true;
        }

        ModifierKeys.hold(modifiers);
        KeyInjector.press(binding);
        KeyInjector.fireInputEvent();

        Held held = new Held();
        held.mode = mode;
        held.remainingTicks = mode == Mode.Tap ? 1 : Math.max(1, holdTicks);
        held.modifiers = modifiers == null ? Modifiers.NONE : modifiers;
        HELD.put(binding, held);
        return true;
    }

    public static boolean isActive(KeyBinding binding) {
        return HELD.containsKey(binding);
    }

    public static void stop(KeyBinding binding) {
        Held held = HELD.remove(binding);
        if (held != null) {
            KeyInjector.release(binding);
            ModifierKeys.release(held.modifiers);
        }
    }

    public static void releaseAll() {
        for (Map.Entry<KeyBinding, Held> entry : HELD.entrySet()) {
            KeyInjector.release(entry.getKey());
            ModifierKeys.release(entry.getValue().modifiers);
        }
        HELD.clear();
    }

    public static void onClientTick() {
        if (HELD.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<KeyBinding, Held>> iterator = HELD.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<KeyBinding, Held> entry = iterator.next();
            KeyBinding binding = entry.getKey();
            Held held = entry.getValue();

            if (held.mode == Mode.Toggle) {
                KeyInjector.hold(binding);
                ModifierKeys.hold(held.modifiers);
                continue;
            }

            if (--held.remainingTicks <= 0) {
                KeyInjector.release(binding);
                ModifierKeys.release(held.modifiers);
                iterator.remove();
            } else {
                KeyInjector.hold(binding);

                ModifierKeys.hold(held.modifiers);
            }
        }
    }

    public enum Mode {
        Tap,

        Toggle,

        Hold
    }

    private static final class Held {

        Mode mode;
        int remainingTicks;

        Modifiers modifiers = Modifiers.NONE;
    }
}
