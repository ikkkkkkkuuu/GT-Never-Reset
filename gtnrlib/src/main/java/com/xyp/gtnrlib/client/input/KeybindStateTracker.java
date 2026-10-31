package com.xyp.gtnrlib.client.input;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BooleanSupplier;

import net.minecraft.client.settings.KeyBinding;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class KeybindStateTracker {

    private final Map<KeyBinding, Held> heldBindings = new HashMap<>();

    private final BooleanSupplier lendUnbound;

    public KeybindStateTracker(BooleanSupplier lendUnbound) {
        this.lendUnbound = lendUnbound;
    }

    public boolean activate(KeyBinding binding, Mode mode, int holdTicks, KeyModifiers modifiers) {
        if (binding == null) {
            return false;
        }
        if (mode == Mode.Toggle && heldBindings.containsKey(binding)) {
            stop(binding);
            return true;
        }

        ModifierKeys.hold(modifiers);
        KeyInjector.press(binding, lendUnbound.getAsBoolean());
        KeyInjector.fireInputEvent();

        Held held = new Held();
        held.mode = mode;
        held.remainingTicks = mode == Mode.Tap ? 1 : Math.max(1, holdTicks);
        held.modifiers = modifiers == null ? KeyModifiers.NONE : modifiers;
        heldBindings.put(binding, held);
        return true;
    }

    public boolean isActive(KeyBinding binding) {
        return heldBindings.containsKey(binding);
    }

    public void stop(KeyBinding binding) {
        Held held = heldBindings.remove(binding);
        if (held != null) {
            KeyInjector.release(binding);
            ModifierKeys.release(held.modifiers);
        }
    }

    public void releaseAll() {
        for (Map.Entry<KeyBinding, Held> entry : heldBindings.entrySet()) {
            KeyInjector.release(entry.getKey());
            ModifierKeys.release(entry.getValue().modifiers);
        }
        heldBindings.clear();
    }

    public void onClientTick() {
        if (heldBindings.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<KeyBinding, Held>> iterator = heldBindings.entrySet()
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

        KeyModifiers modifiers = KeyModifiers.NONE;
    }
}
