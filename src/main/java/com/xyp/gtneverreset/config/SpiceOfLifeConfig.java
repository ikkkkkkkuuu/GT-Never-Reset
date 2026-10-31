package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

@Sync
// #tr gtneverreset.gui.config.general.spice_of_life
// # The Spice of Life
// # zh_CN 生活调味料
@Config(
    modid = "gtneverreset",
    category = "spice_of_life",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.GENERAL_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public class SpiceOfLifeConfig {

    @Comment("关闭食物收益递减：重复食用仍恢复原本的饱食度与饱和度，进食时间不再延长；关闭此选项恢复 Spice of Life 原本规则")
    @DefaultBoolean(true)
    public static boolean disableDiminishingReturns = true;
}
