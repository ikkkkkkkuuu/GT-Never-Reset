package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;

public class VeinMinerPickaxeConfig {

    @Comment("单次连锁挖掘方块上限")
    @DefaultInt(327670)
    @RangeInt(min = 0)
    public int maxAmount = 327670;
    @Comment("匹配方块之间的最大间隔")
    @DefaultInt(32)
    @RangeInt(min = 0, max = 32)
    public int maxRange = 32;
}
