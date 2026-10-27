package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.Name;
import com.gtnewhorizon.gtnhlib.config.Config.Pattern;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;
import com.xyp.gtneverreset.GTNeverReset;

@Config(modid = GTNeverReset.MODID, configSubDirectory = "GTNeverReset", filename = "GTNeverReset")
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.%cat.%field", fullyQualified = true)
@Comment("常规设置")
public final class MainConfig {

    // #tr gtneverreset.gui.config.general.client
    // # Client
    // # zh_CN 客户端
    @LangKey("gtneverreset.gui.config.general.client")
    public static final Client client = new Client();

    // #tr gtneverreset.gui.config.general.wireless_frequency
    // # Wireless Frequency Card
    // # zh_CN 无线频率卡
    @LangKey("gtneverreset.gui.config.general.wireless_frequency")
    @Name("wireless_frequency")
    public static final WirelessFrequency wirelessFrequency = new WirelessFrequency();

    // #tr gtneverreset.gui.config.general.quantum_crafting
    // # Quantum Crafting
    // # zh_CN 量子合成
    @LangKey("gtneverreset.gui.config.general.quantum_crafting")
    @Name("quantum_crafting")
    public static final QuantumCrafting quantumCrafting = new QuantumCrafting();

    private MainConfig() {}

    @Sync
    @Comment("量子合成计算")
    public static class QuantumCrafting {

        @Comment("记录每次量子合成计算的步骤")
        @DefaultBoolean(false)
        public boolean enableCalculationLog = false;

        @Comment("记录量子合成计算的阶段耗时与计数")
        @DefaultBoolean(false)
        public boolean enableCalculationPerformanceLog = false;

        @Comment("缺料时尝试替代样板的时间上限（秒），0 表示不尝试")
        @DefaultInt(5)
        @RangeInt(min = 0)
        public int calculationFallbackSeconds = 5;

        @Comment("独立服务器单次量子计算的时间上限（秒），0 表示不限")
        @DefaultInt(30)
        @RangeInt(min = 0)
        public int calculationLimitSeconds = 30;
    }

    @Sync
    @Comment("无线频率卡")
    public static class WirelessFrequency {

        @Comment("每个频率允许的运行入口上限；降低此值只会阻止新入口")
        @DefaultInt(4096)
        @RangeInt(min = 1)
        // #tr gtneverreset.gui.config.general.wireless_frequency.max_entrances
        // # Maximum entrances per frequency
        // # zh_CN 每个频率的入口上限
        @LangKey("gtneverreset.gui.config.general.wireless_frequency.max_entrances")
        public int maxEntrances = 4096;
    }

    @Comment("仅影响本客户端，不与服务器同步")
    public static class Client {

        // #tr gtneverreset.gui.config.general.client.quantum_crafting
        // # Quantum Crafting
        // # zh_CN 量子合成
        @LangKey("gtneverreset.gui.config.general.client.quantum_crafting")
        @Name("quantum_crafting")
        public final QuantumCrafting quantumCrafting = new QuantumCrafting();

        @Comment("量子合成客户端偏好")
        public static class QuantumCrafting {

            @Comment("网络中有量子计算机时，使用量子模式规划玩家发起的合成请求")
            @DefaultBoolean(false)
            public boolean useQuantumCraftingMode = false;
        }

        // #tr gtneverreset.gui.config.general.client.wireless_terminal
        // # Wireless Dual Interface Terminal
        // # zh_CN 无线二合一接口终端
        @LangKey("gtneverreset.gui.config.general.client.wireless_terminal")
        @Name("wireless_terminal")
        public final WirelessTerminal wirelessTerminal = new WirelessTerminal();

        @Comment("无线二合一接口终端")
        public static class WirelessTerminal {

            @Comment("导入 NEI 配方时，自动将配方名称填入接口搜索框")
            @DefaultBoolean(false)
            // #tr nei.options.gtneverreset.dual_interface_terminal_fill_search_names
            // # Autofill interface search from NEI recipes
            // # zh_CN 用 NEI 配方自动填充接口搜索
            @LangKey("nei.options.gtneverreset.dual_interface_terminal_fill_search_names")
            public boolean fillSearchFromNeiRecipes = false;

            @Comment("在接口搜索中追加电路等不消耗物品的编号")
            @DefaultBoolean(true)
            // #tr nei.options.gtneverreset.dual_interface_terminal_append_circuit_damage
            // # Append non-consumed item numbers to interface search
            // # zh_CN 在接口搜索中追加不消耗物品编号
            @LangKey("nei.options.gtneverreset.dual_interface_terminal_append_circuit_damage")
            public boolean appendNonConsumableItemNumbers = true;

            @Comment("接口名称只显示样板总成自身的电路和不消耗物品编号，忽略其他关联接口的编号")
            @DefaultBoolean(false)
            // #tr gtneverreset.gui.config.general.client.wireless_terminal.prefer_own_interface_names
            // # Prefer assembly's own circuit and non-consumed item numbers
            // # zh_CN 接口名称优先使用样板总成自身的电路和不消耗物品编号
            @LangKey("gtneverreset.gui.config.general.client.wireless_terminal.prefer_own_interface_names")
            public boolean preferOwnInterfaceNames = false;

            @Comment("按钮主题：auto 自动匹配，light 浅色，dark 深色")
            @DefaultString("auto")
            @Pattern("(?i)auto|light|dark")
            // #tr gtneverreset.gui.config.general.client.wireless_terminal.button_theme
            // # Button theme
            // # zh_CN 按钮主题
            @LangKey("gtneverreset.gui.config.general.client.wireless_terminal.button_theme")
            public String buttonTheme = "auto";
        }
    }
}
