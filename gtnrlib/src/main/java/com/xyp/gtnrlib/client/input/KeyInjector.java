package com.xyp.gtnrlib.client.input;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.xyp.gtnrlib.mixins.early.input.KeyBindingAccessor;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class KeyInjector {

    private static final int LENT_KEY_CODE = Keyboard.KEY_F15;

    private static final Set<KeyBinding> LENT = Collections.newSetFromMap(new IdentityHashMap<KeyBinding, Boolean>());

    private static void lendKeyCode(KeyBinding binding, boolean lendUnbound) {
        if (lendUnbound && binding.getKeyCode() == 0 && LENT.add(binding)) {
            binding.setKeyCode(LENT_KEY_CODE);
        }
    }

    private static void returnKeyCode(KeyBinding binding) {
        if (LENT.remove(binding)) {
            binding.setKeyCode(0);
        }
    }

    private static KeyBindingAccessor access(KeyBinding binding) {
        return (KeyBindingAccessor) (Object) binding;
    }

    /** Unbound keys bypass vanilla's code lookup; count only the initial press. */
    public static void press(KeyBinding binding, boolean lendUnbound) {
        lendKeyCode(binding, lendUnbound);
        KeyBindingAccessor accessor = access(binding);
        accessor.gtnrlib$setPressed(true);
        accessor.gtnrlib$setPressTime(accessor.gtnrlib$getPressTime() + 1);
        holdKeyCode(binding);
    }

    public static void hold(KeyBinding binding) {
        access(binding).gtnrlib$setPressed(true);
        holdKeyCode(binding);
    }

    private static void holdKeyCode(KeyBinding binding) {
        KeyBuffer.set(binding.getKeyCode(), true);
    }

    public static void release(KeyBinding binding) {
        KeyBindingAccessor accessor = access(binding);
        accessor.gtnrlib$setPressed(false);
        accessor.gtnrlib$setPressTime(0);

        KeyBuffer.set(binding.getKeyCode(), false);
        returnKeyCode(binding);
    }

    public static boolean isHeld(KeyBinding binding) {
        return access(binding).gtnrlib$isPressed();
    }

    /** Hide the caller's screen first so listeners can open their own screens. */
    public static void fireInputEvent() {
        FMLCommonHandler.instance()
            .bus()
            .post(new InputEvent.KeyInputEvent());
    }

    public static void resyncHeldKeys() {
        for (KeyBinding binding : KeyBindingLookup.all()) {
            if (isPhysicallyDown(binding)) hold(binding);
        }
    }

    public static boolean isPhysicallyDown(int keyCode) {
        if (keyCode == 0) {
            return false;
        }
        if (keyCode > 0) {
            return keyCode < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(keyCode);
        }
        int button = keyCode + 100;
        return button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
    }

    public static boolean isPhysicallyDown(KeyBinding binding) {
        return isPhysicallyDown(binding.getKeyCode());
    }
}
