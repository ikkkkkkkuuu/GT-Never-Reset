package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultDouble;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.Name;
import com.gtnewhorizon.gtnhlib.config.Config.Pattern;
import com.gtnewhorizon.gtnhlib.config.Config.RangeDouble;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.utils.enums.RevealAnimation;
import com.xyp.gtneverreset.utils.enums.SlotPlate;
import com.xyp.gtneverreset.utils.gui.radialmenu.AccentCoefficients;
import com.xyp.gtneverreset.utils.gui.radialmenu.Colors;
import com.xyp.gtneverreset.utils.gui.radialmenu.WheelColors;

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

    // #tr gtneverreset.gui.config.general.recipe_speed
    // # Global Recipe Speed
    // # zh_CN 全局配方加速
    @LangKey("gtneverreset.gui.config.general.recipe_speed")
    @Name("recipe_speed")
    public static final RecipeSpeed recipeSpeed = new RecipeSpeed();

    // #tr gtneverreset.gui.config.general.packaged_provider
    // # Packaged Pattern Provider
    // # zh_CN 封包样板供应器
    @LangKey("gtneverreset.gui.config.general.packaged_provider")
    @Name("packaged_provider")
    public static final PackagedProvider packagedProvider = new PackagedProvider();

    // #tr gtneverreset.gui.config.general.wireless_monitor
    // # Wireless network HUD
    // # zh_CN 无线电网 HUD
    @LangKey("gtneverreset.gui.config.general.wireless_monitor")
    @Name("wireless_monitor")
    public static final WirelessMonitor wirelessMonitor = new WirelessMonitor();

    private MainConfig() {}

    @Comment("无线电网 HUD 的本地显示设置")
    public static class WirelessMonitor {

        @Comment("显示无线电网 HUD；按键切换后保存")
        @DefaultBoolean(false)
        // #tr gtneverreset.gui.config.general.wireless_monitor.enabled
        // # Enabled
        // # zh_CN 启用
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.enabled")
        public boolean enabled = false;

        @Comment("使用科学计数法；超长数值始终缩短")
        @DefaultBoolean(false)
        // #tr gtneverreset.gui.config.general.wireless_monitor.scientific
        // # Scientific notation
        // # zh_CN 科学计数法
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.scientific")
        public boolean scientific = false;

        @Comment("水平偏移，正数向右")
        @DefaultInt(0)
        @RangeInt(min = -10000, max = 10000)
        // #tr gtneverreset.gui.config.general.wireless_monitor.xOffset
        // # Horizontal offset
        // # zh_CN 水平偏移
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.xOffset")
        public int xOffset = 0;

        @Comment("垂直偏移，正数向上")
        @DefaultInt(0)
        @RangeInt(min = -10000, max = 10000)
        // #tr gtneverreset.gui.config.general.wireless_monitor.yOffset
        // # Vertical offset
        // # zh_CN 垂直偏移
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.yOffset")
        public int yOffset = 0;

        @Comment("HUD 缩放")
        @DefaultDouble(1)
        @RangeDouble(min = 0.5, max = 3)
        // #tr gtneverreset.gui.config.general.wireless_monitor.scale
        // # Scale
        // # zh_CN 缩放
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.scale")
        public float scale = 1F;

        @Comment("0=文本；1=卡片；2=卡片加近五分钟电量曲线")
        @DefaultInt(0)
        @RangeInt(min = 0, max = 2)
        // #tr gtneverreset.gui.config.general.wireless_monitor.style
        // # Display style
        // # zh_CN 显示样式
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.style")
        public int style = 0;

        @Comment("卡片和曲线的宽度；屏幕不足时自动缩放")
        @DefaultInt(240)
        @RangeInt(min = 120, max = 600)
        // #tr gtneverreset.gui.config.general.wireless_monitor.width
        // # Panel width
        // # zh_CN 面板宽度
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.width")
        public int width = 240;

        @Comment("采样间隔，单位 tick；20 tick 为 1 秒")
        @DefaultInt(100)
        @RangeInt(min = 20, max = 200)
        // #tr gtneverreset.gui.config.general.wireless_monitor.refreshTicks
        // # Refresh interval
        // # zh_CN 刷新间隔
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.refreshTicks")
        public int refreshTicks = 100;

        @Comment("显示电网总电量")
        @DefaultBoolean(true)
        // #tr gtneverreset.gui.config.general.wireless_monitor.showEnergy
        // # Show energy
        // # zh_CN 显示电量
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.showEnergy")
        public boolean showEnergy = true;

        @Comment("显示最近两个采样之间的电网净 EU/t")
        @DefaultBoolean(true)
        // #tr gtneverreset.gui.config.general.wireless_monitor.showRealtime
        // # Show instant net power
        // # zh_CN 显示瞬时净功率
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.showRealtime")
        public boolean showRealtime = true;

        @Comment("显示近五分钟采样窗口的平均净 EU/t")
        @DefaultBoolean(true)
        // #tr gtneverreset.gui.config.general.wireless_monitor.showAverage
        // # Show average net power
        // # zh_CN 显示平均净功率
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.showAverage")
        public boolean showAverage = true;

        @Comment("在净 EU/t 后显示对应电压等级和安培数")
        @DefaultBoolean(true)
        // #tr gtneverreset.gui.config.general.wireless_monitor.showTier
        // # Show voltage tier
        // # zh_CN 显示电压等级
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.showTier")
        public boolean showTier = true;

        @Comment("启用彩虹文字渲染")
        @DefaultBoolean(true)
        // #tr gtneverreset.gui.config.general.wireless_monitor.animatedColors
        // # Rainbow text
        // # zh_CN 彩虹文字
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.animatedColors")
        public boolean animatedColors = true;

        @Comment("数值也使用彩虹；关闭后净变化保留红绿颜色")
        @DefaultBoolean(true)
        // #tr gtneverreset.gui.config.general.wireless_monitor.animateValues
        // # Animate values
        // # zh_CN 数值彩虹
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.animateValues")
        public boolean animateValues = true;

        @Comment("使用自定义颜色列表")
        @DefaultBoolean(false)
        // #tr gtneverreset.gui.config.general.wireless_monitor.customPalette
        // # Custom palette
        // # zh_CN 自定义配色
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.customPalette")
        public boolean customPalette = false;

        @Comment("HUD 文字粗体")
        @DefaultBoolean(false)
        // #tr gtneverreset.gui.config.general.wireless_monitor.bold
        // # Bold
        // # zh_CN 粗体
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.bold")
        public boolean bold = false;

        @Comment("HUD 文字斜体")
        @DefaultBoolean(false)
        // #tr gtneverreset.gui.config.general.wireless_monitor.italic
        // # Italic
        // # zh_CN 斜体
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.italic")
        public boolean italic = false;

        @Comment("逗号分隔的 RGB 颜色，最多八种")
        @DefaultString("#33CCFF,#FFAA33,#DD77FF")
        @Pattern("^#[0-9a-fA-F]{6}(,#[0-9a-fA-F]{6}){0,7}$")
        // #tr gtneverreset.gui.config.general.wireless_monitor.palette
        // # RGB palette
        // # zh_CN RGB 配色
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.palette")
        public String palette = "#33CCFF,#FFAA33,#DD77FF";

        @Comment("彩虹滚动速度，0 为静止")
        @DefaultDouble(1)
        @RangeDouble(min = 0, max = 10)
        // #tr gtneverreset.gui.config.general.wireless_monitor.speed
        // # Animation speed
        // # zh_CN 动画速度
        @LangKey("gtneverreset.gui.config.general.wireless_monitor.speed")
        public float speed = 1F;

    }

    public static class PackagedProvider {

        @Comment("奥术封包每单位基础源质转换的 Vis 点数；小数余量保留在供应器内")
        @DefaultInt(1)
        @RangeInt(min = 1, max = 1000)
        // #tr gtneverreset.gui.config.general.packaged_provider.arcane_vis_per_essentia
        // # Arcane Vis per essentia
        // # zh_CN 每单位源质转换的 Vis
        @LangKey("gtneverreset.gui.config.general.packaged_provider.arcane_vis_per_essentia")
        public int arcaneVisPerEssentia = 1;
    }

    @RequiresMcRestart
    @Comment("全局配方耗时设置；客户端与服务端应使用相同配置，修改后重启生效")
    public static class RecipeSpeed {

        @Comment("0=关闭；1=固定时长；2=原始时长乘以倍率。影响 GT 配方及熔炉等特殊处理")
        @DefaultInt(0)
        @RangeInt(min = 0, max = 2)
        // #tr gtneverreset.gui.config.general.recipe_speed.mode
        // # Recipe duration mode
        // # zh_CN 配方耗时模式
        @LangKey("gtneverreset.gui.config.general.recipe_speed.mode")
        public int mode = 0;

        @Comment("固定时长模式的耗时，单位 tick，20 tick 为 1 秒")
        @DefaultInt(1)
        @RangeInt(min = 1)
        // #tr gtneverreset.gui.config.general.recipe_speed.fixed_duration
        // # Fixed duration (ticks)
        // # zh_CN 固定耗时（tick）
        @LangKey("gtneverreset.gui.config.general.recipe_speed.fixed_duration")
        public int fixedDuration = 1;

        @Comment("倍率模式下乘以原始耗时，0.1 表示十分之一耗时，结果至少为 1 tick")
        @DefaultDouble(0.1)
        @RangeDouble(min = 0.0001, max = Float.MAX_VALUE)
        // #tr gtneverreset.gui.config.general.recipe_speed.multiplier
        // # Duration multiplier
        // # zh_CN 耗时倍率
        @LangKey("gtneverreset.gui.config.general.recipe_speed.multiplier")
        public float multiplier = 0.1F;

        @Comment("允许 GT 单方块机器每次自动输出全部流体，解除每次 1000 mB 上限；独立于加速模式")
        @DefaultBoolean(true)
        // #tr gtneverreset.gui.config.general.recipe_speed.full_fluid_output
        // # Full fluid auto-output
        // # zh_CN 全量自动输出流体
        @LangKey("gtneverreset.gui.config.general.recipe_speed.full_fluid_output")
        public boolean fullFluidOutput = true;
    }

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

        // #tr gtneverreset.gui.config.general.client.radial_menu
        // # Radial Menu
        // # zh_CN 轮盘菜单
        @LangKey("gtneverreset.gui.config.general.client.radial_menu")
        @Name("radial_menu")
        public final RadialMenu radialMenu = new RadialMenu();

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

    @Comment("仅影响本客户端的轮盘菜单")
    public static class RadialMenu {

        @Comment("轮盘外观")
        public final Wheel wheel = new Wheel();
        @Comment("轮盘颜色")
        public final RadialColors colors = new RadialColors();
        @Comment("主题色参数")
        public final Accent accent = new Accent();
        @Comment("轮盘动画")
        public final Animation animation = new Animation();

        @Config.Comment("松开轮盘键时执行选中动作；关闭后需点击执行。")
        @Config.DefaultBoolean(true)
        @Config.Order(1)
        public boolean releaseToSelect = true;

        @Config.Comment("使用鼠标滚轮选择项目，此模式下子菜单替换当前轮盘。")
        @Config.DefaultBoolean(false)
        @Config.Order(2)
        public boolean scrollToSelect = false;

        @Config.Comment("右键打开编辑器；关闭后使用 Shift 加左键编辑。")
        @Config.DefaultBoolean(false)
        @Config.Order(3)
        public boolean rightClickToEdit = false;

        @Config.Comment("悬停多久后展开外环子菜单，单位毫秒；零表示立即展开。")
        @Config.DefaultInt(150)
        @Config.RangeInt(min = 0, max = 2000)
        @Config.Order(4)
        public int inlineHoverDelayMs = 150;

        @Config.Comment("打开轮盘时继续处理游戏输入，允许玩家移动。")
        @Config.DefaultBoolean(true)
        @Config.Order(5)
        public boolean allowInputWhileOpen = true;

        @Config.Comment("打开轮盘时将鼠标移到屏幕中心。")
        @Config.DefaultBoolean(true)
        @Config.Order(6)
        public boolean centerCursorOnOpen = true;

        @Config.Comment("执行未绑定按键的动作时临时借用 F15，以兼容拒绝零键码的模组。")
        @Config.DefaultBoolean(true)
        @Config.Order(7)
        public boolean lendKeyCodeToUnbound = true;

        @Config.Comment("允许执行配置中的 Lua 脚本；脚本可以发送聊天和命令。")
        @Config.DefaultBoolean(true)
        @Config.Order(8)
        public boolean enableScripts = true;

        @Config.Comment("记录注入按键和潜行状态，仅供兼容性调试。")
        @Config.DefaultBoolean(false)
        @Config.Order(9)
        public boolean logKeyState = false;
    }

    public static class Wheel {

        @Config.Comment("轮盘外半径，单位为 GUI 像素。")
        @Config.DefaultInt(80)
        @Config.RangeInt(min = 32, max = 240)
        @Config.Order(1)
        public int outerRadius = 80;

        @Config.Comment("轮盘内半径；中心区域不选中任何项目。")
        @Config.DefaultInt(32)
        @Config.RangeInt(min = 8, max = 200)
        @Config.Order(2)
        public int innerRadius = 32;

        @Config.Comment("扇区间的视觉间距，单位为 GUI 像素；不改变点击区域。")
        @Config.DefaultFloat(0f)
        @Config.RangeFloat(min = 0f, max = 12f)
        @Config.Order(3)
        public float sectorGap = 0f;

        @Config.Comment("外环子菜单与父轮盘的间距，单位为 GUI 像素。")
        @Config.DefaultInt(2)
        @Config.RangeInt(min = 0, max = 32)
        @Config.Order(6)
        public int inlineRingSpacing = 2;

        @Config.Comment("轮盘边线宽度，单位为 GUI 像素。")
        @Config.DefaultInt(1)
        @Config.RangeInt(min = 1, max = 6)
        @Config.Order(4)
        public int borderWidth = 1;

        @Config.Comment("绘制轮盘边缘和扇区分隔线。")
        @Config.DefaultBoolean(true)
        @Config.Order(5)
        public boolean drawOutline = true;

        @Config.Comment("绘制选中扇区的边线。")
        @Config.DefaultBoolean(true)
        @Config.Order(7)
        public boolean drawHighlightOutline = true;

        @Config.Comment("使用一像素渐变平滑轮盘边缘。")
        @Config.DefaultBoolean(false)
        @Config.Order(8)
        public boolean smoothEdges = false;

        @Config.Comment("轮盘图标尺寸，单位为 GUI 像素；背景槽位同步缩放。")
        @Config.DefaultInt(16)
        @Config.RangeInt(min = 8, max = 48)
        @Config.Order(9)
        public int iconSize = 16;

        @Config.Comment("图标背景：NONE 无，SLOT 槽位，HOTBAR 选中高亮，SELECTED 全部高亮。")
        @Config.DefaultEnum("NONE")
        @Config.Order(10)
        public SlotPlate slotPlate = SlotPlate.NONE;

        @Config.Comment("在轮盘后绘制背景遮罩。")
        @Config.DefaultBoolean(false)
        @Config.Order(11)
        public boolean dimBackground = false;

        public int effectiveInnerRadius() {
            return Math.min(innerRadius, outerRadius - 8);
        }

        public int effectiveIconSize() {
            return Math.max(4, iconSize);
        }
    }

    public static class RadialColors {

        @Config.Comment("轮盘填充颜色，格式 0xAARRGGBB。")
        @Config.DefaultString("0x99101010")
        @Config.Order(1)
        public String ringColor = "0x99101010";

        @Config.Comment("轮盘边线颜色，格式 0xAARRGGBB；透明时隐藏。")
        @Config.DefaultString("0x60FFFFFF")
        @Config.Order(2)
        public String borderColor = "0x60FFFFFF";

        @Config.Comment("选中扇区的填充颜色，格式 0xAARRGGBB。")
        @Config.DefaultString("0xCC4A90D9")
        @Config.Order(3)
        public String highlightColor = "0xCC4A90D9";

        @Config.Comment("选中扇区的边线颜色，格式 0xAARRGGBB。")
        @Config.DefaultString("0xCCFFFFFF")
        @Config.Order(4)
        public String highlightBorderColor = "0xCCFFFFFF";

        @Config.Comment("轮盘背景遮罩颜色，格式 0xAARRGGBB；需启用背景遮罩。")
        @Config.DefaultString("0x80101010")
        @Config.Order(5)
        public String backgroundColor = "0x80101010";

        @Config.Comment("图标默认染色，格式 #RRGGBB；不改变物品图标。")
        @Config.DefaultString("#FFFFFF")
        @Config.Order(6)
        public String iconColor = "#FFFFFF";

        public WheelColors defaultColors() {
            return new WheelColors(
                Colors.parseArgb(ringColor, 0x99101010),
                Colors.parseArgb(highlightColor, 0xCC4A90D9),
                Colors.parseArgb(borderColor, 0x60FFFFFF),
                Colors.parseArgb(highlightBorderColor, 0xCCFFFFFF),
                Colors.parseArgb(backgroundColor, 0x80101010),
                Colors.parseArgb(iconColor, 0xFFFFFFFF));
        }
    }

    public static class Accent {

        @Config.Comment("主题色生成的轮盘透明度，范围 0 至 255。")
        @Config.DefaultInt(153)
        @Config.RangeInt(min = 0, max = 255)
        @Config.Order(1)
        public int ringAlpha = 153;

        @Config.Comment("主题色生成的轮盘亮度，单位为主题色亮度百分比。")
        @Config.DefaultInt(18)
        @Config.RangeInt(min = 0, max = 200)
        @Config.Order(2)
        public int ringBrightness = 18;

        @Config.Comment("主题色生成的边线透明度，范围 0 至 255。")
        @Config.DefaultInt(96)
        @Config.RangeInt(min = 0, max = 255)
        @Config.Order(3)
        public int borderAlpha = 96;

        @Config.Comment("主题色生成的边线亮度，单位为主题色亮度百分比。")
        @Config.DefaultInt(140)
        @Config.RangeInt(min = 0, max = 200)
        @Config.Order(4)
        public int borderBrightness = 140;

        @Config.Comment("主题色生成的高亮透明度，范围 0 至 255。")
        @Config.DefaultInt(204)
        @Config.RangeInt(min = 0, max = 255)
        @Config.Order(5)
        public int highlightAlpha = 204;

        @Config.Comment("主题色生成的高亮亮度，单位为主题色亮度百分比。")
        @Config.DefaultInt(100)
        @Config.RangeInt(min = 0, max = 200)
        @Config.Order(6)
        public int highlightBrightness = 100;

        @Config.Comment("主题色生成的高亮边线透明度，范围 0 至 255。")
        @Config.DefaultInt(204)
        @Config.RangeInt(min = 0, max = 255)
        @Config.Order(7)
        public int highlightBorderAlpha = 204;

        @Config.Comment("主题色生成的高亮边线亮度，单位为主题色亮度百分比。")
        @Config.DefaultInt(160)
        @Config.RangeInt(min = 0, max = 200)
        @Config.Order(8)
        public int highlightBorderBrightness = 160;

        @Config.Comment("主题色生成的背景透明度，范围 0 至 255。")
        @Config.DefaultInt(128)
        @Config.RangeInt(min = 0, max = 255)
        @Config.Order(9)
        public int backgroundAlpha = 128;

        @Config.Comment("主题色生成的背景亮度，单位为主题色亮度百分比。")
        @Config.DefaultInt(8)
        @Config.RangeInt(min = 0, max = 200)
        @Config.Order(10)
        public int backgroundBrightness = 8;

        public AccentCoefficients coefficients() {
            return new AccentCoefficients(
                new AccentCoefficients.Tone(ringAlpha, ringBrightness),
                new AccentCoefficients.Tone(highlightAlpha, highlightBrightness),
                new AccentCoefficients.Tone(borderAlpha, borderBrightness),
                new AccentCoefficients.Tone(highlightBorderAlpha, highlightBorderBrightness),
                new AccentCoefficients.Tone(backgroundAlpha, backgroundBrightness));
        }
    }

    public static class Animation {

        @Config.Comment("展开动画：NONE、FADE、ZOOM、STAGGER_CW、STAGGER_CCW 或 STAGGER_BOTH。")
        @Config.DefaultEnum("ZOOM")
        @Config.Order(1)
        public RevealAnimation revealAnimation = RevealAnimation.ZOOM;

        @Config.Comment("展开动画总时长，单位毫秒；零表示立即完成。")
        @Config.DefaultInt(180)
        @Config.RangeInt(min = 0, max = 2000)
        @Config.Order(2)
        public int revealDurationMs = 180;

        @Config.Comment("选中扇区向外移动的距离，单位为 GUI 像素。")
        @Config.DefaultInt(4)
        @Config.RangeInt(min = 0, max = 20)
        @Config.Order(3)
        public int hoverPush = 4;

        @Config.Comment("选中扇区移动时长，单位毫秒；零表示立即完成。")
        @Config.DefaultInt(120)
        @Config.RangeInt(min = 0, max = 2000)
        @Config.Order(4)
        public int hoverDurationMs = 120;
    }
}
