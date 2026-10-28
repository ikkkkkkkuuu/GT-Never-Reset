package com.xyp.gtneverreset.client.action.radialmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.xyp.gtneverreset.client.gui.RadialMenuGui;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WithoutScreen {

    private static boolean armed;

    private static GuiScreen hiddenScreen;
    private static boolean hiddenFocus;

    public static boolean wheelIsUp() {
        return Minecraft.getMinecraft().currentScreen instanceof RadialMenuGui;
    }

    public static void runAction(ActionSpec spec) {
        Minecraft mc = Minecraft.getMinecraft();

        GuiScreen screen = mc.currentScreen;
        boolean hadFocus = mc.inGameHasFocus;

        mc.currentScreen = null;
        mc.inGameHasFocus = true;
        try {
            ActionExecutors.execute(spec);
        } finally {
            // Preserve screens opened by the action instead of restoring the wheel over them.
            if (mc.currentScreen == null) {
                mc.currentScreen = screen;
                mc.inGameHasFocus = hadFocus;
            }
        }
    }

    public static void armNextTick() {
        armed = true;
    }

    public static void beginTick() {
        if (!armed || hiddenScreen != null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof RadialMenuGui)) {
            armed = false;
            return;
        }
        hiddenScreen = mc.currentScreen;
        hiddenFocus = mc.inGameHasFocus;
        mc.currentScreen = null;
        mc.inGameHasFocus = true;
    }

    public static void endTick(boolean startPhase) {
        if (hiddenScreen == null) {
            return;
        }
        if (startPhase) {
            armed = false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) {
            mc.currentScreen = hiddenScreen;
            mc.inGameHasFocus = hiddenFocus;
        }
        hiddenScreen = null;
    }
}
