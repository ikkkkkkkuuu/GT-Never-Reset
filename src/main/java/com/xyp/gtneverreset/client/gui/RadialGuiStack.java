package com.xyp.gtneverreset.client.gui;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RadialGuiStack {

    private static final Deque<GuiScreen> STACK = new ArrayDeque<>();
    private static GuiScreen requested;

    private RadialGuiStack() {}

    public static void push(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) {
            STACK.push(mc.currentScreen);
        }
        mc.displayGuiScreen(screen);
    }

    public static void pop() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(STACK.isEmpty() ? null : STACK.pop());
    }

    public static void closeAll() {
        STACK.clear();
        Minecraft.getMinecraft()
            .displayGuiScreen(null);
    }

    public static boolean hasParent() {
        return !STACK.isEmpty();
    }

    public static void requestOpen(GuiScreen screen) {
        requested = screen;
    }

    public static void openRequested() {
        if (requested == null) {
            return;
        }
        GuiScreen screen = requested;
        requested = null;
        STACK.clear();
        Minecraft.getMinecraft()
            .displayGuiScreen(screen);
    }
}
