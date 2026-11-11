package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config.Sync
@Config.RequiresMcRestart
// #tr gtneverreset.gui.config.bee_breeder
// # Large Bee Breeder
// # zh_CN 大型蜜蜂杂交机
@Config.LangKey("gtneverreset.gui.config.bee_breeder")
@Config(
    modid = "gtneverreset",
    category = "bee_breeder",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.MACHINE_FILE,
    categoryCulling = false)
public class BeeBreederConfig {

    @Config.Comment({ "Require mutation foundation blocks in input buses. The blocks are not consumed.",
        "When disabled, breeding ignores foundations and the breeding route hides them. Restart to apply." })
    @Config.DefaultBoolean(false)
    // #tr gtneverreset.gui.config.bee_breeder.require_mutation_foundations
    // # Require mutation foundations
    // # zh_CN 启用杂交基石需求
    @Config.LangKey("gtneverreset.gui.config.bee_breeder.require_mutation_foundations")
    public static boolean requireMutationFoundations = false;

    @Config.Comment({ "Optional automatic bee tier overrides: species UID=HV, EV, IV, LuV, ZPM, UV or UHV.",
        "Defaults use breeding depth, parent tiers, foundation materials and GT comb processing tiers.",
        "Each three breeding generations adds one tier. Overrides also affect descendants. Restart to apply." })
    @Config.DefaultStringList({})
    // #tr gtneverreset.gui.config.bee_breeder.species_voltage_overrides
    // # Species voltage overrides
    // # zh_CN 蜂种电压等级覆盖
    @Config.LangKey("gtneverreset.gui.config.bee_breeder.species_voltage_overrides")
    public static String[] speciesVoltageOverrides = {};
}
