package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;

// #tr gtneverreset.gui.config.general.projector
// # Multiblock Projector
// # zh_CN 多方块投影仪
@Config(
    modid = "gtneverreset",
    category = "projector",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.MACHINE_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
@Sync
public class ProjectorConfig {

    @Comment("Allow players to generate structures with the multiblock projector.")
    @DefaultBoolean(true)
    // #tr gtneverreset.gui.config.general.projector.build_enabled
    // # Enable structure generation
    // # zh_CN 允许生成结构
    @Config.LangKey("gtneverreset.gui.config.general.projector.build_enabled")
    public static boolean buildEnabled = true;

    @Comment("Maximum distance from the projected controller, in blocks. Zero removes the limit.")
    @DefaultInt(64)
    @RangeInt(min = 0, max = 1024)
    // #tr gtneverreset.gui.config.general.projector.build_range
    // # Maximum generation distance
    // # zh_CN 最远生成距离
    @Config.LangKey("gtneverreset.gui.config.general.projector.build_range")
    public static int buildRange = 64;

    @Comment("Milliseconds per server tick shared by all projector generation jobs.")
    @DefaultInt(10)
    @RangeInt(min = 1, max = 40)
    // #tr gtneverreset.gui.config.general.projector.build_budget
    // # Generation budget per tick (ms)
    // # zh_CN 每刻生成预算（毫秒）
    @Config.LangKey("gtneverreset.gui.config.general.projector.build_budget")
    public static int buildBudgetMillis = 10;
}
