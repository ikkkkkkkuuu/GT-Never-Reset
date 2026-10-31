package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;

public class PackagedProviderConfig {

    @Comment("奥术封包每单位基础源质转换的 Vis 点数；小数余量保留在供应器内")
    @DefaultInt(1)
    @RangeInt(min = 1, max = 1000)
    // #tr gtneverreset.gui.config.general.packaged_provider.arcane_vis_per_essentia
    // # Arcane Vis per essentia
    // # zh_CN 每单位源质转换的 Vis
    @LangKey("gtneverreset.gui.config.general.packaged_provider.arcane_vis_per_essentia")
    public int arcaneVisPerEssentia = 1;
}
