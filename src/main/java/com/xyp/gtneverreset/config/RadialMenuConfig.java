package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.xyp.gtneverreset.utils.enums.SlotPlate;
import com.xyp.gtnrlib.gui.wheel.AccentCoefficients;
import com.xyp.gtnrlib.gui.wheel.RevealAnimation;
import com.xyp.gtnrlib.gui.wheel.WheelColors;
import com.xyp.gtnrlib.utils.Colors;

@Comment("仅影响本客户端的轮盘菜单")
public class RadialMenuConfig {

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
