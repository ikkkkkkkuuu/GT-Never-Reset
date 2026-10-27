package com.xyp.gtneverreset.client.keybind;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.mixins.early.minecraft.KeyBindingAccessor;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class KeyInjector {

    private static final int LENT_KEY_CODE = Keyboard.KEY_F15;

    private static final Set<KeyBinding> LENT = Collections.newSetFromMap(new IdentityHashMap<KeyBinding, Boolean>());

    private KeyInjector() {}

    private static void lendKeyCode(KeyBinding binding) {
        if (MainConfig.client.radialMenu.lendKeyCodeToUnbound && binding.getKeyCode() == 0 && LENT.add(binding)) {
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
    public static void press(KeyBinding binding) {
        lendKeyCode(binding);
        KeyBindingAccessor accessor = access(binding);
        accessor.radialmenu$setPressed(true);
        accessor.radialmenu$setPressTime(accessor.radialmenu$getPressTime() + 1);
        holdKeyCode(binding);
    }

    public static void hold(KeyBinding binding) {
        access(binding).radialmenu$setPressed(true);
        holdKeyCode(binding);
    }

    private static void holdKeyCode(KeyBinding binding) {
        KeyBuffer.set(binding.getKeyCode(), true);
    }

    public static void release(KeyBinding binding) {
        KeyBindingAccessor accessor = access(binding);
        accessor.radialmenu$setPressed(false);
        accessor.radialmenu$setPressTime(0);

        KeyBuffer.set(binding.getKeyCode(), false);
        returnKeyCode(binding);
    }

    public static boolean isHeld(KeyBinding binding) {
        return access(binding).radialmenu$isPressed();
    }

    /** Run with the wheel hidden so listeners can open their own screens. */
    public static void fireInputEvent() {
        FMLCommonHandler.instance()
            .bus()
            .post(new InputEvent.KeyInputEvent());
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
