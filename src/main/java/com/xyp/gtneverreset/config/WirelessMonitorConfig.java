package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultDouble;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.Pattern;
import com.gtnewhorizon.gtnhlib.config.Config.RangeDouble;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;

@Comment("无线电网 HUD 的本地显示设置")
public class WirelessMonitorConfig {

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
