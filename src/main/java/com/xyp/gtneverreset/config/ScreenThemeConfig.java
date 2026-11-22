package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtnrlib.gui.theme.ScreenTheme;

// #tr gtneverreset.gui.config.general.screen_theme
// # Screen themes
// # zh_CN 界面主题
@Config(
    modid = GTNeverReset.MODID,
    category = "screen_theme",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.CLIENT_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public class ScreenThemeConfig {

    @Config.Comment("DEFAULT: ModularUI 2 default, AE2_LIGHT: AE2 light, AE2_DARK: AE2 dark, AE2_DARK_PURPLE: AE2 dark with purple highlights")
    @Config.DefaultEnum("AE2_LIGHT")
    // #tr gtneverreset.gui.config.general.screen_theme.wirelessCard
    // # Wireless frequency card
    // # zh_CN 无线频率卡
    @Config.LangKey("gtneverreset.gui.config.general.screen_theme.wirelessCard")
    public static ScreenTheme wirelessCard = ScreenTheme.AE2_LIGHT;

    @Config.Comment("DEFAULT: ModularUI 2 default, AE2_LIGHT: AE2 light, AE2_DARK: AE2 dark, AE2_DARK_PURPLE: AE2 dark with purple highlights")
    @Config.DefaultEnum("AE2_LIGHT")
    // #tr gtneverreset.gui.config.general.screen_theme.wirelessPowerCard
    // # Wireless power card
    // # zh_CN 无线电网卡
    @Config.LangKey("gtneverreset.gui.config.general.screen_theme.wirelessPowerCard")
    public static ScreenTheme wirelessPowerCard = ScreenTheme.AE2_LIGHT;

    @Config.Comment("DEFAULT: ModularUI 2 default, AE2_LIGHT: AE2 light, AE2_DARK: AE2 dark, AE2_DARK_PURPLE: AE2 dark with purple highlights")
    @Config.DefaultEnum("AE2_LIGHT")
    // #tr gtneverreset.gui.config.general.screen_theme.wildcardPattern
    // # Wildcard pattern
    // # zh_CN 通配符样板
    @Config.LangKey("gtneverreset.gui.config.general.screen_theme.wildcardPattern")
    public static ScreenTheme wildcardPattern = ScreenTheme.AE2_LIGHT;

    @Config.Comment("DEFAULT: ModularUI 2 default, AE2_LIGHT: AE2 light, AE2_DARK: AE2 dark, AE2_DARK_PURPLE: AE2 dark with purple highlights")
    @Config.DefaultEnum("AE2_LIGHT")
    // #tr gtneverreset.gui.config.general.screen_theme.advancedIOBus
    // # ME advanced IO bus
    // # zh_CN ME 高级 IO 总线
    @Config.LangKey("gtneverreset.gui.config.general.screen_theme.advancedIOBus")
    public static ScreenTheme advancedIOBus = ScreenTheme.AE2_LIGHT;

    @Config.Comment("DEFAULT: ModularUI 2 default, AE2_LIGHT: AE2 light, AE2_DARK: AE2 dark, AE2_DARK_PURPLE: AE2 dark with purple highlights")
    @Config.DefaultEnum("AE2_LIGHT")
    // #tr gtneverreset.gui.config.general.screen_theme.universalWand
    // # Useless Tools
    // # zh_CN 无用之杖
    @Config.LangKey("gtneverreset.gui.config.general.screen_theme.universalWand")
    public static ScreenTheme universalWand = ScreenTheme.AE2_LIGHT;

    @Config.Comment("Theme of the multiblock projector screen. DEFAULT: the ModularUI 2 default theme; AE2_LIGHT: the light AE2 1.21 theme; AE2_DARK: the dark AE2 1.21 theme; AE2_DARK_PURPLE: the dark AE2 1.21 theme with purple highlights")
    // #tr gtneverreset.gui.config.screen_theme.projector
    // # Multiblock Projector Theme
    // # zh_CN 多方块投影仪主题
    // #tr gtneverreset.gui.config.screen_theme.projector.tooltip
    // # Select the default ModularUI theme or a light, dark or purple AE2 theme.
    // # zh_CN 选择 ModularUI 默认主题，或 AE2 的亮色、暗色、紫色高亮主题。
    @Config.LangKey("gtneverreset.gui.config.screen_theme.projector")
    @Config.DefaultEnum("AE2_DARK")
    public static ScreenTheme projector = ScreenTheme.AE2_DARK;

}
