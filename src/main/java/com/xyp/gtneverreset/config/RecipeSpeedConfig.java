package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultDouble;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.RangeDouble;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;

@RequiresMcRestart
@Comment("全局配方耗时设置；客户端与服务端应使用相同配置，修改后重启生效")
public class RecipeSpeedConfig {

    @Comment("0=关闭；1=固定时长；2=原始时长乘以倍率。影响 GT 配方及熔炉等特殊处理")
    @DefaultInt(0)
    @RangeInt(min = 0, max = 2)
    // #tr gtneverreset.gui.config.general.recipe_speed.mode
    // # Recipe duration mode
    // # zh_CN 配方耗时模式
    @LangKey("gtneverreset.gui.config.general.recipe_speed.mode")
    public int mode = 0;

    @Comment("固定时长模式的耗时，单位 tick，20 tick 为 1 秒")
    @DefaultInt(1)
    @RangeInt(min = 1)
    // #tr gtneverreset.gui.config.general.recipe_speed.fixed_duration
    // # Fixed duration (ticks)
    // # zh_CN 固定耗时（tick）
    @LangKey("gtneverreset.gui.config.general.recipe_speed.fixed_duration")
    public int fixedDuration = 1;

    @Comment("倍率模式下乘以原始耗时，0.1 表示十分之一耗时，结果至少为 1 tick")
    @DefaultDouble(0.1)
    @RangeDouble(min = 0.0001, max = Float.MAX_VALUE)
    // #tr gtneverreset.gui.config.general.recipe_speed.multiplier
    // # Duration multiplier
    // # zh_CN 耗时倍率
    @LangKey("gtneverreset.gui.config.general.recipe_speed.multiplier")
    public float multiplier = 0.1F;

    @Comment("允许 GT 单方块机器每次自动输出全部流体，解除每次 1000 mB 上限；独立于加速模式")
    @DefaultBoolean(true)
    // #tr gtneverreset.gui.config.general.recipe_speed.full_fluid_output
    // # Full fluid auto-output
    // # zh_CN 全量自动输出流体
    @LangKey("gtneverreset.gui.config.general.recipe_speed.full_fluid_output")
    public boolean fullFluidOutput = true;
}
