package com.xyp.gtneverreset.client.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtnrlib.mod.ModList;

public final class QuickTerminalButtonTextures {

    private static final ResourceLocation LIGHT_STATES = new ResourceLocation(
        ModList.ModIds.GT_NEVER_RESET,
        "textures/gui/quick_terminal/states.png");
    private static final ResourceLocation DARK_STATES = new ResourceLocation(
        ModList.ModIds.GT_NEVER_RESET,
        "textures/gui/quick_terminal/states_dark.png");
    private static final ResourceLocation AE2_STATES = ModList.AE2.getResourceLocation("textures/guis/states.png");

    private static boolean resourcePackDark;
    private static boolean resolved;

    public static ResourceLocation states() {
        String theme = MainConfig.client.wirelessTerminal.buttonTheme;
        if ("light".equalsIgnoreCase(theme)) return LIGHT_STATES;
        if (!"auto".equalsIgnoreCase(theme)) return DARK_STATES;
        if (!resolved) refresh();
        return resourcePackDark ? DARK_STATES : LIGHT_STATES;
    }

    public static boolean isDark() {
        return states() == DARK_STATES;
    }

    public static void refresh() {
        if (!"auto".equalsIgnoreCase(MainConfig.client.wirelessTerminal.buttonTheme)) return;
        resourcePackDark = false;
        resolved = true;
        try (InputStream stream = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(AE2_STATES)
            .getInputStream()) {
            BufferedImage atlas = ImageIO.read(stream);
            if (atlas == null || atlas.getWidth() <= 245 || atlas.getHeight() <= 245) return;
            int color = atlas.getRGB(245, 245);
            int brightness = ((color >> 16 & 255) * 299 + (color >> 8 & 255) * 587 + (color & 255) * 114) / 1000;
            resourcePackDark = brightness < 80;
        } catch (IOException ignored) {}
    }
}
