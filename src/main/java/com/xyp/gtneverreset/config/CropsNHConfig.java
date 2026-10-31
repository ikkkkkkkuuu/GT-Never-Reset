package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;

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
    public static boolean enableInstantGrowth = true;
    @Comment("CropsNH 种子的生长、产量和抗性固定为 31")
    @DefaultBoolean(true)
    public static boolean enableMaxStats = true;
    @Comment("CropsNH 作物必定返回一粒种子，绕过抗性概率判定")
    @DefaultBoolean(true)
    public static boolean enableGuaranteedSeedDrop = true;
}
