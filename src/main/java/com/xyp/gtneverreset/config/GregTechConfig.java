package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

// #tr gtneverreset.gui.config.general.gregtech
// # GregTech
// # zh_CN GregTech
@Config(
    modid = "gtneverreset",
    category = "gregtech",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.MACHINE_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public final class GregTechConfig {

    @Sync
    @RequiresMcRestart
    @Comment("开启后，砖高炉控制器各面支持自动输入原料和输出产物；关闭恢复原版手动上下料")
    @DefaultBoolean(true)
    public static boolean enableBrickedBlastFurnaceAutomation = true;

    // #tr gtneverreset.gui.config.general.gregtech.enableModernFluidTankBoost
    // # Increase GTM fluid tank capacity
    // # zh_CN 扩大 GTM 小机器流体槽容量
    @Sync
    @Comment("开启后，GTM 小机器每个流体输入槽和输出槽的容量为原来的 100 倍。关闭后恢复原容量，已存入的流体仍可取出。")
    @DefaultBoolean(true)
    public static boolean enableModernFluidTankBoost = true;

    // #tr gtneverreset.gui.config.general.gregtech.requireCleanroom
    // # Require a cleanroom
    // # zh_CN 要求超净间
    @Sync
    @Comment("默认免除 GT 配方的超净间要求和洁净度不足导致的产物损失。开启后使用 GT 原有的超净间判定。")
    @DefaultBoolean(false)
    public static boolean requireCleanroom = false;

    // #tr gtneverreset.gui.config.general.gregtech.patternMEOutputFastFlush
    // # Quickly export pattern assembly and maximum-capacity ME outputs
    // # zh_CN 总成及最大容量 ME 输出快速回网
    @Sync
    @Comment("三种样板输入总成、最大容量 ME 输出总线和输出仓（含输出总成）关联的机器工作期间，仅当同一 ME 网络有玩家提交的合成任务时，每 tick 尝试输出产物。自动补货任务不触发；其他情况沿用 GT 原生周期。")
    @DefaultBoolean(true)
    public static boolean patternMEOutputFastFlush = true;

    // #tr gtneverreset.gui.config.general.gregtech.patternMEOutputFastFlushHoldTicks
    // # ME output fast export hold ticks
    // # zh_CN ME 输出快速回网停机延续时间
    @Sync
    @Comment("机器最后工作后继续允许快速回网的 tick 数，仍要求 ME 网络存在玩家合成任务。100 tick = 5 秒；0 表示仅工作当 tick。")
    @DefaultInt(100)
    @RangeInt(min = 0, max = 24000)
    public static int patternMEOutputFastFlushHoldTicks = 100;
}
