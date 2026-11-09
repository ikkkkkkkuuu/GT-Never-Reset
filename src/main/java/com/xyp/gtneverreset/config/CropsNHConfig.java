package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;

// #tr gtneverreset.gui.config.general.cropsnh
// # CropsNH
// # zh_CN CropsNH 作物
@Config(
    modid = "gtneverreset",
    category = "cropsnh",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.GENERAL_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public class CropsNHConfig {

    @Comment("CropsNH 作物在生长判定时直接成熟")
    @DefaultBoolean(true)
    // #tr gtneverreset.gui.config.general.cropsnh.enableInstantGrowth
    // # Instant growth
    // # zh_CN 作物立即成熟
    @LangKey("gtneverreset.gui.config.general.cropsnh.enableInstantGrowth")
    public static boolean enableInstantGrowth = true;
    @Comment("CropsNH 作物每 tick 进行一次生长判定；关闭后恢复为每 256 tick 一次")
    @DefaultBoolean(true)
    // #tr gtneverreset.gui.config.general.cropsnh.enableFastGrowthTickRate
    // # Fast growth tick rate (256 -> 1)
    // # zh_CN 加快生长判定频率（256 -> 1）
    @LangKey("gtneverreset.gui.config.general.cropsnh.enableFastGrowthTickRate")
    public static boolean enableFastGrowthTickRate = true;
    @Comment("CropsNH 种子的生长、产量和抗性固定为 31")
    @DefaultBoolean(true)
    // #tr gtneverreset.gui.config.general.cropsnh.enableMaxStats
    // # Fixed max seed stats
    // # zh_CN 固定种子属性为最高值
    @LangKey("gtneverreset.gui.config.general.cropsnh.enableMaxStats")
    public static boolean enableMaxStats = true;
    @Comment("CropsNH 作物必定返回一粒种子，绕过抗性概率判定")
    @DefaultBoolean(true)
    // #tr gtneverreset.gui.config.general.cropsnh.enableGuaranteedSeedDrop
    // # Guaranteed seed drop
    // # zh_CN 必定掉落种子
    @LangKey("gtneverreset.gui.config.general.cropsnh.enableGuaranteedSeedDrop")
    public static boolean enableGuaranteedSeedDrop = true;
}
