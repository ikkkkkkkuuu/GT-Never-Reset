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

    @Config.Comment("Success percentages for crop tiers 1-3, 4-6, 7-9, etc. The last entry covers higher tiers. Independent of voltage. Restart required.")
    @Config.DefaultIntList({ 100, 80, 60, 40, 20, 10 })
    @Config.RangeInt(min = 0, max = 100)
    // #tr gtneverreset.gui.config.crop_breeder.tier_chances
    // # Success chances by crop tier band (%%)
    // # zh_CN 作物等级分档成功率（%%）
    @Config.LangKey("gtneverreset.gui.config.crop_breeder.tier_chances")
    public static int[] tierChances = { 100, 80, 60, 40, 20, 10 };

    @Config.Comment("Per-crop success chance overrides: crop_id=integer_percent (0-100). Use exact CropsNH registry IDs. Last valid entry wins; invalid entries are ignored. Restart required.")
    @Config.DefaultStringList({})
    // #tr gtneverreset.gui.config.crop_breeder.crop_overrides
    // # Per-crop success chance overrides
    // # zh_CN 按作物 ID 覆盖成功率
    @Config.LangKey("gtneverreset.gui.config.crop_breeder.crop_overrides")
    public static String[] cropOverrides = {};
}
