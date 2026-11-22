package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.Name;
import com.gtnewhorizon.gtnhlib.config.Config.Pattern;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;

@Comment("仅影响本客户端，不与服务器同步")
// #tr gtneverreset.gui.config.general.client
// # Client
// # zh_CN 客户端
@Config(
    modid = "gtneverreset",
    category = "client",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.CLIENT_FILE,
    categoryCulling = false)
@Config.LangKeyPattern(pattern = "gtneverreset.gui.config.general.%cat.%field", fullyQualified = true)
public class ClientConfig {

    @Comment("强制 GregTech 的 NEI 配方页面显示配方所属模组；关闭时遵循 GregTech 配置")
    @DefaultBoolean(true)
    @RequiresMcRestart
    // #tr gtneverreset.gui.config.general.client.enableAlwaysDisplayRecipeOwner
    // # Always display recipe owner
    // # zh_CN 始终显示配方所属模组
    @LangKey("gtneverreset.gui.config.general.client.enableAlwaysDisplayRecipeOwner")
    public static boolean enableAlwaysDisplayRecipeOwner = true;

    @Comment("强制 GregTech 的 Waila 信息显示平均耗时(ns)；关闭时遵循 GregTech 配置")
    @DefaultBoolean(true)
    @RequiresMcRestart
    // #tr gtneverreset.gui.config.general.client.enableAlwaysDisplayWailaAverageNS
    // # Always display Waila average tick time
    // # zh_CN 始终显示 Waila 平均耗时
    @LangKey("gtneverreset.gui.config.general.client.enableAlwaysDisplayWailaAverageNS")
    public static boolean enableAlwaysDisplayWailaAverageNS = true;

    @Comment("强制 GregTech 的 NEI 配方页面显示原始电压等级；关闭时遵循 GregTech 配置")
    @DefaultBoolean(true)
    @RequiresMcRestart
    // #tr gtneverreset.gui.config.general.client.enableAlwaysDisplayNEIOriginalVoltage
    // # Always display NEI original voltage
    // # zh_CN 始终显示 NEI 原始电压
    @LangKey("gtneverreset.gui.config.general.client.enableAlwaysDisplayNEIOriginalVoltage")
    public static boolean enableAlwaysDisplayNEIOriginalVoltage = true;

    @Comment("在 Windows 上隐藏本客户端的任务栏按钮，游戏窗口仍可正常使用。默认关闭，保存设置后即时生效。")
    @DefaultBoolean(false)
    // #tr gtneverreset.gui.config.general.client.hide_windows_taskbar_button
    // # Hide Windows taskbar button
    // # zh_CN 隐藏 Windows 任务栏按钮
    @LangKey("gtneverreset.gui.config.general.client.hide_windows_taskbar_button")
    public static boolean hideWindowsTaskbarButton = false;

    // #tr gtneverreset.gui.config.general.client.radial_menu
    // # Radial Menu
    // # zh_CN 轮盘菜单
    @LangKey("gtneverreset.gui.config.general.client.radial_menu")
    @Name("radial_menu")
    public static final RadialMenuConfig radialMenu = new RadialMenuConfig();

    // #tr gtneverreset.gui.config.general.client.quantum_crafting
    // # Quantum Crafting
    // # zh_CN 量子合成
    @LangKey("gtneverreset.gui.config.general.client.quantum_crafting")
    @Name("quantum_crafting")
    public static final QuantumCrafting quantumCrafting = new QuantumCrafting();

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
    public static final WirelessTerminal wirelessTerminal = new WirelessTerminal();

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

    @Config.Comment("Projector preview: outline connected hatch spots as one shape instead of one box per block")
    // #tr gtneverreset.gui.config.client.projector_merge_outlines
    // # Projector: Join Hatch Outlines
    // # zh_CN 投影仪：仓位描边连接内边
    // #tr gtneverreset.gui.config.client.projector_merge_outlines.tooltip
    // # In the projector preview, outline touching hatch spots as one shape instead of one box per block.
    // # zh_CN 投影仪预览里相连的仓位只描外轮廓，不再每格各描一个框。
    @Config.LangKey("gtneverreset.gui.config.client.projector_merge_outlines")
    @Config.DefaultBoolean(true)
    public static boolean projectorMergeOutlines = true;

    @Config.Comment("Projector preview: hatch spot outlines show through the blocks in front of them")
    // #tr gtneverreset.gui.config.client.projector_outlines_through_blocks
    // # Projector: Outlines Through Blocks
    // # zh_CN 投影仪：仓位描边透视
    // #tr gtneverreset.gui.config.client.projector_outlines_through_blocks.tooltip
    // # In the projector preview, show hatch spot outlines even behind blocks in front of them.
    // # zh_CN 投影仪预览里被前面方块挡住的仓位描边也显示出来。
    @Config.LangKey("gtneverreset.gui.config.client.projector_outlines_through_blocks")
    @Config.DefaultBoolean(false)
    public static boolean projectorOutlinesThroughBlocks = false;

    @Config.Comment("Projector preview: outline hatch spots on every page, not only on the hatch page")
    // #tr gtneverreset.gui.config.client.projector_hatch_outlines
    // # Projector: Hatch Outlines
    // # zh_CN 投影仪：仓室描边
    // #tr gtneverreset.gui.config.client.projector_hatch_outlines.tooltip
    // # In the projector preview, outline the hatch spots on every page, not only on the hatch page.
    // # zh_CN 投影仪预览里在每一页都描出仓位，不只在仓室页。
    @Config.LangKey("gtneverreset.gui.config.client.projector_hatch_outlines")
    @Config.DefaultBoolean(true)
    public static boolean projectorHatchOutlines = true;

    @Config.Comment("Projector: changing a hatch's model or tier also changes the other spots of the same kind")
    // #tr gtneverreset.gui.config.client.projector_same_kind
    // # Projector: Same Kind Together
    // # zh_CN 投影仪：同类仓位一起改
    // #tr gtneverreset.gui.config.client.projector_same_kind.tooltip
    // # Changing a hatch's model or tier also changes the other spots of the same kind.
    // # zh_CN 改一个仓的型号或等级时，同一种类的其他仓位一起改。
    @Config.LangKey("gtneverreset.gui.config.client.projector_same_kind")
    @Config.DefaultBoolean(true)
    public static boolean projectorSameKind = true;

    @Config.Comment("Apply hatch model and tier changes to all hatches of the same kind in the projector array")
    // #tr gtneverreset.gui.config.client.projector_batch_same_kind
    // # Projector: Edit matching hatches in the array
    // # zh_CN 投影仪：批量阵列同类仓位一起改
    // #tr gtneverreset.gui.config.client.projector_batch_same_kind.tooltip
    // # Enable to apply a model or tier change to every hatch of that kind in the array.
    // # zh_CN 开启后，切换型号或等级会同时修改阵列中已选的同类仓室。
    @Config.LangKey("gtneverreset.gui.config.client.projector_batch_same_kind")
    @Config.DefaultBoolean(false)
    public static boolean projectorBatchSameKind;

    @Config.Comment("Projector: choosing a casing changes its whole group of spots, not only the chosen spot")
    // #tr gtneverreset.gui.config.client.projector_same_casing
    // # Projector: Whole Casing Group
    // # zh_CN 投影仪：同组外壳一起换
    // #tr gtneverreset.gui.config.client.projector_same_casing.tooltip
    // # Choosing a casing changes its whole group of spots; when off, only the chosen spot changes.
    // # zh_CN 换外壳时整组外壳位一起换，关掉时只换选中的那一格。
    @Config.LangKey("gtneverreset.gui.config.client.projector_same_casing")
    @Config.DefaultBoolean(false)
    public static boolean projectorSameCasing = false;

}
