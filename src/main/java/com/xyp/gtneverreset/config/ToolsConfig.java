package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultDouble;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.RangeDouble;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

// #tr gtneverreset.gui.config.general.tools
// # Tools
// # zh_CN 工具
@Config(
    modid = "gtneverreset",
    category = "tools",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.ITEM_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public class ToolsConfig {

    @Sync
    @Comment("无线二合一接口终端的磁力范围（格）；范围越大，掉落物密集时的扫描开销越高")
    @DefaultInt(64)
    @RangeInt(min = 1, max = 64)
    public static int wirelessTerminalMagnetRange = 64;

    @Comment("GT 工具合成耐久消耗的除数；1 为原版，默认 10。消耗向上取整且至少为 1，不影响挖掘和攻击")
    @DefaultDouble(10.0)
    @RangeDouble(min = 1.0)
    public static double craftingDurabilityMultiplier = 10.0;
}
