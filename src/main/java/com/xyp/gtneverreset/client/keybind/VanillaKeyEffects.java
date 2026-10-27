package com.xyp.gtneverreset.client.keybind;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class VanillaKeyEffects {

    private VanillaKeyEffects() {}

    public static boolean tryRun(KeyBinding binding) {
        Minecraft mc = Minecraft.getMinecraft();
        GameSettings settings = mc.gameSettings;
        if (settings == null || binding == null) {
            return false;
        }

        if (binding == settings.keyBindTogglePerspective) {
            settings.thirdPersonView++;
            if (settings.thirdPersonView > 2) {
                settings.thirdPersonView = 0;
            }
            return true;
        }

        if (binding == settings.keyBindSmoothCamera) {
            settings.smoothCamera = !settings.smoothCamera;
            return true;
        }

        return false;
    }
}
