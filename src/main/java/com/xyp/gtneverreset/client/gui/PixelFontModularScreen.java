package com.xyp.gtneverreset.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.eigenraven.lwjgl3ify.api.InputEvents;

/** Keeps bitmap glyph measurement consistent across layout, drawing and input, with Unicode fallback. */
@SideOnly(Side.CLIENT)
public class PixelFontModularScreen extends ModularScreen {

    public PixelFontModularScreen(String owner, ModularPanel panel) {
        super(owner, panel);
    }

    @Override
    public void onResize(int width, int height) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onResize(width, height);
        }
    }

    @Override
    public void onUpdate() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onUpdate();
        }
    }

    @Override
    public void onFrameUpdate() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onFrameUpdate();
        }
    }

    @Override
    public void drawScreen() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.drawScreen();
        }
    }

    @Override
    public void drawForeground() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.drawForeground();
        }
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMousePressed(mouseButton);
        }
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMouseRelease(mouseButton);
        }
    }

    @Override
    public boolean onMouseDrag(int mouseButton, long timeSinceClick) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMouseDrag(mouseButton, timeSinceClick);
        }
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMouseScroll(scrollDirection, amount);
        }
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onKeyPressed(typedChar, keyCode);
        }
    }

    @Override
    public boolean onKeyRelease(char typedChar, int keyCode) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onKeyRelease(typedChar, keyCode);
        }
    }

    @Override
    @Optional.Method(modid = ModularUI.ModIds.LWJGL3IFY)
    public void onKeyEvent(InputEvents.KeyEvent event) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onKeyEvent(event);
        }
    }

    @Override
    @Optional.Method(modid = ModularUI.ModIds.LWJGL3IFY)
    public void onTextEvent(InputEvents.TextEvent event) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onTextEvent(event);
        }
    }

    /** Temporarily enables bitmap Latin glyphs; nested scopes restore their caller's state. */
    @SideOnly(Side.CLIENT)
    private static final class PixelFontScope implements AutoCloseable {

        private final FontRenderer font;
        private final boolean unicode;

        private PixelFontScope() {
            font = Minecraft.getMinecraft().fontRenderer;
            unicode = font.getUnicodeFlag();
            font.setUnicodeFlag(false);
        }

        @Override
        public void close() {
            font.setUnicodeFlag(unicode);
        }
    }
}
