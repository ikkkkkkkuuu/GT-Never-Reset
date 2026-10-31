package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

@Sync
@Comment("量子合成计算")
public class QuantumCraftingConfig {

    @Comment("记录每次量子合成计算的步骤")
    @DefaultBoolean(false)
    public boolean enableCalculationLog = false;

    @Comment("记录量子合成计算的阶段耗时与计数")
    @DefaultBoolean(false)
    public boolean enableCalculationPerformanceLog = false;

    @Comment("缺料时尝试替代样板的时间上限（秒），0 表示不尝试")
    @DefaultInt(5)
    @RangeInt(min = 0)
    public int calculationFallbackSeconds = 5;

    @Comment("独立服务器单次量子计算的时间上限（秒），0 表示不限")
    @DefaultInt(30)
    @RangeInt(min = 0)
    public int calculationLimitSeconds = 30;
}
