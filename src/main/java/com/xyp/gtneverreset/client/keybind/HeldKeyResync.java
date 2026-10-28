package com.xyp.gtneverreset.client.keybind;

import net.minecraft.client.settings.KeyBinding;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class HeldKeyResync {

    public static void resyncHeldKeys() {
        for (KeyBinding binding : KeyBindingLookup.all()) {
            if (KeyInjector.isPhysicallyDown(binding)) {
                KeyInjector.hold(binding);
            }
        }
    }
}
