package com.xyp.gtnrlib.gui.theme;

import org.jetbrains.annotations.Nullable;

public enum ScreenTheme {

    DEFAULT(null),
    AE2_LIGHT(AE2Theme.LIGHT_ID),
    AE2_DARK(AE2Theme.DARK_ID),
    AE2_DARK_PURPLE(AE2Theme.DARK_PURPLE_ID);

    public final @Nullable String themeId;

    ScreenTheme(@Nullable String themeId) {
        this.themeId = themeId;
    }
}
