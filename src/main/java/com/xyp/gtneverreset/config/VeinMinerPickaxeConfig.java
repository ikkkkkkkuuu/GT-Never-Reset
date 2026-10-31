package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;

// #tr gtneverreset.gui.config.general.vein_miner_pickaxe
// # Vein mining pickaxe
// # zh_CN 连锁挖掘镐
@Config(
    modid = "gtneverreset",
    category = "vein_miner_pickaxe",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.ITEM_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public class VeinMinerPickaxeConfig {

    @Comment("单次连锁挖掘方块上限")
    @DefaultInt(327670)
    @RangeInt(min = 0)
    public static int maxAmount = 327670;
    @Comment("匹配方块之间的最大间隔")
    @DefaultInt(32)
    @RangeInt(min = 0, max = 32)
    public static int maxRange = 32;
}
