package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

@Sync
@Comment("无线频率卡")
// #tr gtneverreset.gui.config.general.wireless_frequency
// # Wireless Frequency Card
// # zh_CN 无线频率卡
@Config(
    modid = "gtneverreset",
    category = "wireless_frequency",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.GENERAL_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public class WirelessFrequencyConfig {

    @Comment("每个频率允许的运行入口上限；降低此值只会阻止新入口")
    @DefaultInt(4096)
    @RangeInt(min = 1)
    // #tr gtneverreset.gui.config.general.wireless_frequency.max_entrances
    // # Maximum entrances per frequency
    // # zh_CN 每个频率的入口上限
    @LangKey("gtneverreset.gui.config.general.wireless_frequency.max_entrances")
    public static int maxEntrances = 4096;
}
