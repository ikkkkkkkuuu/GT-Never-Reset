package com.xyp.gtneverreset.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/** Temporarily enables bitmap Latin glyphs on the client thread; nested scopes restore their caller's state. */
public final class PixelFontScope implements AutoCloseable {

    private final FontRenderer font;
    private final boolean unicode;

    public PixelFontScope() {
        font = Minecraft.getMinecraft().fontRenderer;
        unicode = font.getUnicodeFlag();
        font.setUnicodeFlag(false);
    }

    @Override
    public void close() {
        font.setUnicodeFlag(unicode);
    }
}
