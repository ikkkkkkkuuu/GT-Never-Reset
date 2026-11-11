package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config.RequiresMcRestart
// #tr gtneverreset.gui.config.crop_breeder
// # Large Crop Breeder
// # zh_CN 大型作物杂交机
@Config.LangKey("gtneverreset.gui.config.crop_breeder")
@Config(
    modid = "gtneverreset",
    category = "crop_breeder",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.MACHINE_FILE,
    categoryCulling = false)
public class CropBreederConfig {

    @Config.Sync
    @Config.Comment({ "Require breeding substrates in input buses. Substrates are not consumed.",
        "When disabled, breeding ignores substrates and the route hides parent and offspring substrates.",
        "Catalysts, fertilizer and other breeding conditions still apply. Restart to apply." })
    @Config.DefaultBoolean(false)
    // #tr gtneverreset.gui.config.crop_breeder.require_substrates
    // # Require breeding substrates
    // # zh_CN 启用杂交底土需求
    @Config.LangKey("gtneverreset.gui.config.crop_breeder.require_substrates")
    public static boolean requireSubstrates = false;

    @Config.Comment("Success percentages for breeding grades 1, 2, 3, 4 and 5. Legacy entries beyond the fifth are ignored. Independent of machine voltage. Restart required.")
    @Config.DefaultIntList({ 100, 80, 60, 40, 20 })
    @Config.RangeInt(min = 0, max = 100)
    // #tr gtneverreset.gui.config.crop_breeder.tier_chances
    // # Default success chances by breeding grade (%%)
    // # zh_CN 杂交等级默认成功率（%%）
    @Config.LangKey("gtneverreset.gui.config.crop_breeder.tier_chances")
    public static int[] tierChances = { 100, 80, 60, 40, 20 };

    @Config.Comment("Per-crop success chance overrides: crop_id=integer_percent (0-100). Use exact CropsNH registry IDs. Last valid entry wins; invalid entries are ignored. Restart required.")
    @Config.DefaultStringList({})
    // #tr gtneverreset.gui.config.crop_breeder.crop_overrides
    // # Per-crop success chance overrides
    // # zh_CN 按作物 ID 覆盖成功率
    @Config.LangKey("gtneverreset.gui.config.crop_breeder.crop_overrides")
    public static String[] cropOverrides = {};
}
