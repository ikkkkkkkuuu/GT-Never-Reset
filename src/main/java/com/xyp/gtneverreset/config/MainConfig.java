package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.Name;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;
import com.xyp.gtneverreset.GTNeverReset;

@Config(modid = GTNeverReset.MODID, configSubDirectory = "GTNeverReset", filename = "GTNeverReset")
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.%cat.%field", fullyQualified = true)
@Comment("常规设置")
public final class MainConfig {

    @Name("gregtech")
    public static final GregTechConfig gregTech = new GregTechConfig();

    @Name("quests")
    public static final QuestsConfig quests = new QuestsConfig();

    @Name("tools")
    public static final ToolsConfig tools = new ToolsConfig();

    @Name("cropsnh")
    public static final CropsNHConfig cropsNH = new CropsNHConfig();

    @Name("spice_of_life")
    public static final SpiceOfLifeConfig spiceOfLife = new SpiceOfLifeConfig();

    @Name("vein_miner_pickaxe")
    public static final VeinMinerPickaxeConfig veinMinerPickaxe = new VeinMinerPickaxeConfig();

    // #tr gtneverreset.gui.config.general.client
    // # Client
    // # zh_CN 客户端
    @LangKey("gtneverreset.gui.config.general.client")
    public static final ClientConfig client = new ClientConfig();

    // #tr gtneverreset.gui.config.general.wireless_frequency
    // # Wireless Frequency Card
    // # zh_CN 无线频率卡
    @LangKey("gtneverreset.gui.config.general.wireless_frequency")
    @Name("wireless_frequency")
    public static final WirelessFrequencyConfig wirelessFrequency = new WirelessFrequencyConfig();

    // #tr gtneverreset.gui.config.general.quantum_crafting
    // # Quantum Crafting
    // # zh_CN 量子合成
    @LangKey("gtneverreset.gui.config.general.quantum_crafting")
    @Name("quantum_crafting")
    public static final QuantumCraftingConfig quantumCrafting = new QuantumCraftingConfig();

    // #tr gtneverreset.gui.config.general.recipe_speed
    // # Global Recipe Speed
    // # zh_CN 全局配方加速
    @LangKey("gtneverreset.gui.config.general.recipe_speed")
    @Name("recipe_speed")
    public static final RecipeSpeedConfig recipeSpeed = new RecipeSpeedConfig();

    // #tr gtneverreset.gui.config.general.packaged_provider
    // # Packaged Pattern Provider
    // # zh_CN 封包样板供应器
    @LangKey("gtneverreset.gui.config.general.packaged_provider")
    @Name("packaged_provider")
    public static final PackagedProviderConfig packagedProvider = new PackagedProviderConfig();

    // #tr gtneverreset.gui.config.general.wireless_monitor
    // # Wireless network HUD
    // # zh_CN 无线电网 HUD
    @LangKey("gtneverreset.gui.config.general.wireless_monitor")
    @Name("wireless_monitor")
    public static final WirelessMonitorConfig wirelessMonitor = new WirelessMonitorConfig();

    public static final class GregTechConfig {

        @Sync
        @RequiresMcRestart
        @Comment("开启后，砖高炉控制器各面支持自动输入原料和输出产物；关闭恢复原版手动上下料")
        @DefaultBoolean(true)
        public boolean enableBrickedBlastFurnaceAutomation = true;
    }
}
