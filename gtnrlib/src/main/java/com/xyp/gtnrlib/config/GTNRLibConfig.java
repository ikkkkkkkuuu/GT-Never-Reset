package com.xyp.gtnrlib.config;

import java.util.Collection;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultFloat;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultInt;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultString;
import com.gtnewhorizon.gtnhlib.config.Config.LangKey;
import com.gtnewhorizon.gtnhlib.config.Config.RangeFloat;
import com.gtnewhorizon.gtnhlib.config.Config.RangeInt;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.xyp.gtnrlib.GTNRLib;

@Config(
    modid = GTNRLib.MODID,
    category = GTNRLibConfig.CATEGORY,
    configSubDirectory = GTNRLib.CONFIG_DIRECTORY,
    filename = GTNRLib.MODID)
@Config.LangKeyPattern(pattern = "gtnrlib.gui.config.%cat.%field", fullyQualified = true)
@Comment("Client settings of the shared interface and HUD")
public class GTNRLibConfig {

    @Config.Ignore
    // #tr gtnrlib.gui.config.client
    // # Interface and HUD
    // # zh_CN 界面与 HUD
    // #tr gtnrlib.gui.config.client.tooltip
    // # Settings of the shared screens and the HUD on this client
    // # zh_CN 本机的通用界面和 HUD 设置
    public static final String CATEGORY = "client";
    @Config.Ignore
    private static final String THEME = "theme";
    @Config.Ignore
    public static final int DEFAULT_MAX_WORLD_MOTES = 1024;
    @Config.Ignore
    private static String[] themes = {};

    @Comment("Enable custom first-person hand and item animations. Disable to use vanilla rendering immediately.")
    // #tr gtnrlib.gui.config.client.enable_first_person_animations
    // # First-person hand animations
    // # zh_CN 第一人称手臂动画
    // #tr gtnrlib.gui.config.client.enable_first_person_animations.tooltip
    // # Disable all custom hand and item animations and use vanilla rendering. Takes effect immediately.
    // # zh_CN 关闭后停用所有自定义手臂和持物动画，恢复原版表现。立即生效。
    @LangKey("gtnrlib.gui.config.client.enable_first_person_animations")
    @DefaultBoolean(true)
    public static boolean enableFirstPersonAnimations = true;

    @Comment("Enable the built-in first-person poses and item actions")
    // #tr gtnrlib.gui.config.client.built_in_first_person
    // # Built-in first-person animations
    // # zh_CN 内置第一人称动画
    // #tr gtnrlib.gui.config.client.built_in_first_person.tooltip
    // # Animate hands and items using the built-in profiles. Applies on resource reload.
    // # zh_CN 使用内置姿势绘制双手和物品。重新加载资源时应用。
    @LangKey("gtnrlib.gui.config.client.built_in_first_person")
    @DefaultBoolean(true)
    public static boolean builtInFirstPerson = true;

    @Comment("Optional Punchy ZIP/folder or original HMI 5.1.1 (1.21.11) JAR in resourcepacks. Reload after changing.")
    // #tr gtnrlib.gui.config.client.animation_pack
    // # Animation-only pack
    // # zh_CN 仅导入动画的资源包
    // #tr gtnrlib.gui.config.client.animation_pack.tooltip
    // # Name in resourcepacks. Use a Punchy ZIP/folder or HMI 5.1.1 (1.21.11) JAR, then reload.
    // # zh_CN 填写 resourcepacks 中的 Punchy ZIP、目录名，或 HMI 5.1.1（1.21.11）原版 JAR 文件名。修改后重新加载资源。
    @LangKey("gtnrlib.gui.config.client.animation_pack")
    @DefaultString("")
    public static String animationPack = "";

    @Comment("Show every HUD element; arrange them with Edit HUD")
    // #tr gtnrlib.gui.config.client.enable_hud
    // # Show HUD
    // # zh_CN 显示 HUD
    // #tr gtnrlib.gui.config.client.enable_hud.tooltip
    // # Show every HUD element. Arrange them with Edit HUD.
    // # zh_CN 显示所有 HUD 元素，可在“编辑 HUD”中拖动每个元素
    @LangKey("gtnrlib.gui.config.client.enable_hud")
    @DefaultBoolean(true)
    public static boolean enableHud = true;

    @Comment("Base scale of every HUD element; each element can be scaled further in the HUD editor")
    // #tr gtnrlib.gui.config.client.hud_scale
    // # HUD Scale
    // # zh_CN HUD 缩放
    // #tr gtnrlib.gui.config.client.hud_scale.tooltip
    // # Base size multiplier of every HUD element. Each element can be scaled further in Edit HUD.
    // # zh_CN 所有 HUD 元素的基础尺寸倍率，每个元素还可在“编辑 HUD”中单独缩放
    @LangKey("gtnrlib.gui.config.client.hud_scale")
    @DefaultFloat(1F)
    @RangeFloat(min = 0.5F, max = 3F)
    public static float hudScale = 1F;

    @Comment("Theme of the screens that follow the chosen theme; only registered themes can be chosen, an empty or unknown id keeps the default theme of each screen")
    // #tr gtnrlib.gui.config.client.theme
    // # Screen Theme
    // # zh_CN 界面主题
    // #tr gtnrlib.gui.config.client.theme.tooltip
    // # Theme of the screens that follow the chosen theme. Click to switch between the registered themes.
    // # zh_CN 跟随所选主题的界面使用的主题，点击在已注册的主题之间切换
    @LangKey("gtnrlib.gui.config.client.theme")
    @DefaultString("")
    public static String theme = "";

    @Comment("Most glowing dots, puffs and sparks shown in the world at once; when there are more, the oldest disappear first")
    // #tr gtnrlib.gui.config.client.max_world_motes
    // # Most World Dots
    // # zh_CN 世界光点上限
    // @formatter:off
    // #tr gtnrlib.gui.config.client.max_world_motes.tooltip
    // # Most glowing dots, puffs and sparks shown in the world at once. When there are more, the oldest disappear first.
    // # zh_CN 世界里的光点、光雾、火花同时最多显示多少个；超出时最早的先消失
    // @formatter:on
    @LangKey("gtnrlib.gui.config.client.max_world_motes")
    @DefaultInt(DEFAULT_MAX_WORLD_MOTES)
    @RangeInt(min = 128, max = 4096)
    public static int maxWorldMotes = DEFAULT_MAX_WORLD_MOTES;

    @Comment("Render animated text effects; when disabled, styled text is shown as ordinary text")
    // #tr gtnrlib.gui.config.general.client.enable_text_effects
    // # Animated Text Effects
    // # zh_CN 动态文字特效
    // #tr gtnrlib.gui.config.general.client.enable_text_effects.tooltip
    // # Render animated text effects. When disabled, styled text is shown as ordinary text.
    // # zh_CN 启用动态文字特效。关闭后，含特效的文字以普通文字显示
    @LangKey("gtnrlib.gui.config.general.client.enable_text_effects")
    @DefaultBoolean(true)
    public static boolean enableTextEffects = true;

    @Comment("Type marked text one character at a time when it appears")
    // #tr gtnrlib.gui.config.client.enable_typewriter
    // # Typewriter Text
    // # zh_CN 打字机文字
    // #tr gtnrlib.gui.config.client.enable_typewriter.tooltip
    // # Type marked text one character at a time. When disabled, show the full text immediately.
    // # zh_CN 标记的文字出现时逐字显示。关闭后立即显示全文。
    @LangKey("gtnrlib.gui.config.client.enable_typewriter")
    @DefaultBoolean(true)
    public static boolean enableTypewriter = true;

    public static void register() {
        try {
            ConfigurationManager.registerConfig(GTNRLibConfig.class);
        } catch (ConfigException e) {
            GTNRLib.LOG.error("Failed to load the GTNRLib client settings", e);
        }
    }

    public static String[] themes() {
        return themes.clone();
    }

    public static void listThemes(Collection<String> ids) {
        themes = ids.toArray(new String[0]);
        Configuration config = ConfigurationManager.getConfig(GTNRLibConfig.class);
        Property property = config == null ? null
            : config.getCategory(CATEGORY)
                .get(THEME);
        if (property != null) property.setValidValues(themes.clone());
    }
}
