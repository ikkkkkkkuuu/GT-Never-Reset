package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

@Sync
@Comment("无线频率卡")
public class WirelessFrequencyConfig {

    @Comment("每个频率允许的运行入口上限；降低此值只会阻止新入口")
    @DefaultInt(4096)
    @RangeInt(min = 1)
    // #tr gtneverreset.gui.config.general.wireless_frequency.max_entrances
    // # Maximum entrances per frequency
    // # zh_CN 每个频率的入口上限
    @LangKey("gtneverreset.gui.config.general.wireless_frequency.max_entrances")
    public int maxEntrances = 4096;
}
