package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
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

    // #tr gtneverreset.gui.config.general.gregtech.requireCleanroom
    // # Require a cleanroom
    // # zh_CN 要求超净间
    @Sync
    @Comment("默认免除 GT 配方的超净间要求和洁净度不足导致的产物损失。开启后使用 GT 原有的超净间判定。")
    @DefaultBoolean(false)
    public static boolean requireCleanroom = false;
}
