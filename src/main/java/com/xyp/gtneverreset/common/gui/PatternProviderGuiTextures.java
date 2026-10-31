package com.xyp.gtneverreset.common.gui;

import com.cleanroommc.modularui.drawable.UITexture;
import com.xyp.gtnrlib.mod.ModList;

public final class PatternProviderGuiTextures {

    public static final UITexture PATTERN_SLOT = texture("ex_pattern_provider", 256, 256, 7, 41, 18, 18);
    public static final UITexture BUTTON = texture("states", 256, 256, 176, 128, 18, 20);
    public static final UITexture HOVER = texture("states", 256, 256, 212, 128, 18, 20);

    private PatternProviderGuiTextures() {}

    public static UITexture texture(String name, int w, int h, int x, int y, int width, int height) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NEVER_RESET, "gui/packaged/" + name)
            .imageSize(w, h)
            .subAreaXYWH(x, y, width, height)
            .build();
    }

    public static UITexture icon(String name) {
        return texture(name, 16, 16, 0, 0, 16, 16);
    }
}
