package com.xyp.gtneverreset.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import com.xyp.gtneverreset.client.profile.radialmenu.ProfileManager;
import com.xyp.gtneverreset.client.profile.radialmenu.ProfileStorage;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;
import com.xyp.gtneverreset.utils.gui.radialmenu.Colors;
import com.xyp.gtneverreset.utils.gui.radialmenu.MenuStyle;
import com.xyp.gtneverreset.utils.gui.radialmenu.Profile;
import com.xyp.gtneverreset.utils.gui.radialmenu.WheelColors;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialProfileColorsGui extends RadialUiScreen {

    private static final int ID_PICK_BASE = 10;
    private static final int ID_CLEAR_BASE = 20;
    private static final int ID_ACCENT = 30;

    private static final int CLEAR_WIDTH = 20;

    private final String profileName;

    private Profile profile;

    private final String[] colors = new String[Swatch.values().length];

    private int accentTop;

    public RadialProfileColorsGui(String profileName) {
        this.profileName = profileName;
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.profileColors.title
        // # Colors: %s
        // # zh_CN 颜色：%s
        return "radialmenu.profileColors.title";
    }

    @Override
    protected Object[] titleArgs() {
        return new Object[] { profileName };
    }

    @Override
    protected int panelWidth() {
        return 320;
    }

    @Override
    protected int panelHeightHint() {
        // The accent, the gap that keeps it apart from what it fills in, then the colours themselves.
        return 14 + RadialUi.STEP + RadialUi.GAP + RadialUi.STEP * (Swatch.values().length - 1) + RadialUi.ROW;
    }

    @Override
    protected void buildControls() {
        if (profile == null) {
            profile = ProfileStorage.loadProfile(profileName);
            if (profile == null) {
                RadialGuiStack.pop();
                return;
            }
            MenuStyle style = profile.style;
            colors[Swatch.Ring.ordinal()] = orEmpty(style == null ? null : style.ringColor);
            colors[Swatch.Highlight.ordinal()] = orEmpty(style == null ? null : style.highlightColor);
            colors[Swatch.Border.ordinal()] = orEmpty(style == null ? null : style.borderColor);
            colors[Swatch.HighlightBorder.ordinal()] = orEmpty(style == null ? null : style.highlightBorderColor);
            colors[Swatch.Background.ordinal()] = orEmpty(style == null ? null : style.backgroundColor);
            colors[Swatch.Icon.ordinal()] = orEmpty(style == null ? null : style.iconColor);
        }

        int controlLeft = contentLeft() + RadialUi.LABEL_WIDTH + RadialUi.GAP;
        int clearLeft = contentRight() - CLEAR_WIDTH;
        int controlWidth = clearLeft - RadialUi.GAP - controlLeft;
        int y = contentTop() + 14;

        accentTop = y;
        this.buttonList.add(
            new GuiButton(
                ID_ACCENT,
                controlLeft,
                y,
                controlWidth,
                RadialUi.ROW,
                I18n.format("radialmenu.editor.pickAccent")));
        // #tr radialmenu.profileColors.accent.tip
        // # Generate the colors below from one accent, then adjust each color
        // # zh_CN 用强调色生成下方配色，再分别调整
        tooltip(ID_ACCENT, I18n.format("radialmenu.profileColors.accent.tip"));

        y += RadialUi.STEP + RadialUi.GAP;

        for (final Swatch swatch : Swatch.values()) {
            final int index = swatch.ordinal();
            boolean editable = RadialFieldControls.colorEnabled(swatch.key);

            RadialUiColorButton pick = new RadialUiColorButton(
                ID_PICK_BASE + index,
                controlLeft,
                y,
                controlWidth,
                new RadialUiColorButton.Value() {

                    @Override
                    public String get() {
                        return colors[index];
                    }
                },
                Colors.parseArgb(inherited(swatch), 0xFF000000),
                // #tr radialmenu.profileColors.default
                // # From mod config
                // # zh_CN 来自模组配置
                I18n.format("radialmenu.profileColors.default"));
            // Shown but not editable when its part of the wheel is switched off in the config. The value stays, and
            // an accent still writes into it, so turning that part back on brings back the colour chosen for it.
            pick.enabled = editable;
            this.buttonList.add(pick);
            tooltip(
                ID_PICK_BASE + index,
                editable ? I18n.format(swatch.tooltipKey) : RadialFieldControls.colorOffTip(swatch.key));

            // One colour at a time: clearing all of them to put a single one back was a poor trade.
            GuiButton clear = new RadialUiIconButton(
                ID_CLEAR_BASE + index,
                clearLeft,
                y,
                CLEAR_WIDTH,
                RadialUi.ROW,
                RadialUiIconButton.Icon.Cross);
            clear.enabled = !colors[index].isEmpty();
            this.buttonList.add(clear);
            tooltip(ID_CLEAR_BASE + index, I18n.format("radialmenu.profileColors.clear.tip"));

            y += RadialUi.STEP;
        }

        // #tr radialmenu.profileColors.reset
        // # Reset to defaults
        // # zh_CN 重置为默认
        addBottomBar("radialmenu.editor.save", "radialmenu.profileColors.reset", "gui.cancel");
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String inherited(Swatch swatch) {
        switch (swatch) {
            case Highlight:
                return MainConfig.client.radialMenu.colors.highlightColor;
            case Icon:
                return MainConfig.client.radialMenu.colors.iconColor;
            case Border:
                return MainConfig.client.radialMenu.colors.borderColor;
            case HighlightBorder:
                return MainConfig.client.radialMenu.colors.highlightBorderColor;
            case Background:
                return MainConfig.client.radialMenu.colors.backgroundColor;
            case Ring:
            default:
                return MainConfig.client.radialMenu.colors.ringColor;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case ID_PRIMARY:
                save();
                return;
            case ID_SECONDARY:
                onCancel();
                return;
            case ID_DANGER:
                // With nothing of its own, the profile shows the config's colours - that is the whole reset.
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = "";
                }
                requestRebuild();
                return;
            case ID_ACCENT:
                pickAccent();
                return;
            default:
                break;
        }

        Swatch[] swatches = Swatch.values();

        int clearIndex = button.id - ID_CLEAR_BASE;
        if (clearIndex >= 0 && clearIndex < swatches.length) {
            colors[clearIndex] = "";
            requestRebuild();
            return;
        }

        int pickIndex = button.id - ID_PICK_BASE;
        if (pickIndex >= 0 && pickIndex < swatches.length) {
            pick(swatches[pickIndex]);
        }
    }

    private void pick(Swatch swatch) {
        final int index = swatch.ordinal();
        RadialGuiStack.push(
            new RadialColorPickerGui(
                effective(colors[index], inherited(swatch)),
                swatch.alpha,
                new RadialColorPickerGui.Result() {

                    @Override
                    public void onColorPicked(String hex) {
                        colors[index] = hex;
                        requestRebuild();
                    }
                }));
    }

    private void pickAccent() {
        RadialGuiStack.push(new RadialColorPickerGui(accentStart(), false, new RadialColorPickerGui.Result() {

            @Override
            public void onColorPicked(String hex) {
                WheelColors derived = RadialFieldControls.derive(hex);
                colors[Swatch.Ring.ordinal()] = hex(derived.ring);
                colors[Swatch.Highlight.ordinal()] = hex(derived.highlight);
                colors[Swatch.Border.ordinal()] = hex(derived.border);
                colors[Swatch.HighlightBorder.ordinal()] = hex(derived.highlightBorder);
                colors[Swatch.Background.ordinal()] = hex(derived.background);
                requestRebuild();
            }
        }));
    }

    private String accentStart() {
        String highlight = colors[Swatch.Highlight.ordinal()];
        int shown = Colors
            .over(highlight, Colors.parseArgb(MainConfig.client.radialMenu.colors.highlightColor, 0xFF4A90D9));
        return String.format("#%06X", Integer.valueOf(shown & 0x00FFFFFF));
    }

    private static String hex(int argb) {
        return String.format("#%08X", Integer.valueOf(argb));
    }

    /** Opens the picker on what the profile actually shows, not on a blank, so editing starts from what is there. */
    private static String effective(String override, String inherited) {
        return String.format("#%08X", Integer.valueOf(Colors.over(override, Colors.parseArgb(inherited, 0xFF000000))));
    }

    private void save() {
        profile.style = MenuStyle.of(
            colors[Swatch.Ring.ordinal()],
            colors[Swatch.Highlight.ordinal()],
            colors[Swatch.Icon.ordinal()],
            colors[Swatch.Border.ordinal()],
            colors[Swatch.HighlightBorder.ordinal()],
            colors[Swatch.Background.ordinal()]);
        ProfileStorage.saveProfile(profile);

        // The edited profile may be the one loaded in memory; reload so the wheel shows it straight away.
        if (profileName.equals(ProfileManager.activeName())) {
            ProfileManager.switchTo(profileName);
        }
        RadialGuiStack.pop();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        int left = contentLeft();
        // #tr radialmenu.profileColors.section
        // # Profile colors
        // # zh_CN 配置颜色
        RadialUi.sectionHeader(I18n.format("radialmenu.profileColors.section"), left, contentTop(), contentRight());

        // #tr radialmenu.profileColors.accent
        // # Accent
        // # zh_CN 强调色
        RadialUi.rowLabel(I18n.format("radialmenu.profileColors.accent"), left, accentTop);

        int y = accentTop + RadialUi.STEP + RadialUi.GAP;
        for (Swatch swatch : Swatch.values()) {
            RadialUi.rowLabel(I18n.format(swatch.labelKey), left, y);
            y += RadialUi.STEP;
        }
    }

    private enum Swatch {

        Ring(
            // #tr radialmenu.profileColors.ring
            // # Ring fill
            // # zh_CN 环形填充色
            "radialmenu.profileColors.ring",
            // #tr radialmenu.profileColors.ring.tip
            // # Ring color for this profile. Submenu colors override it
            // # zh_CN 配置方案的轮盘颜色；子菜单可单独设置
            "radialmenu.profileColors.ring.tip", true, ActionTypes.PARAM_RING_COLOR),
        Border(
            // #tr radialmenu.profileColors.border
            // # Ring outline
            // # zh_CN 环形边框
            "radialmenu.profileColors.border",
            // #tr radialmenu.profileColors.border.tip
            // # Divider and edge color. Submenu colors override it
            // # zh_CN 分隔线和边缘颜色；子菜单可单独设置
            "radialmenu.profileColors.border.tip", true, ActionTypes.PARAM_BORDER_COLOR),
        Highlight(
            // #tr radialmenu.profileColors.highlight
            // # Highlight fill
            // # zh_CN 高亮填充色
            "radialmenu.profileColors.highlight",
            // #tr radialmenu.profileColors.highlight.tip
            // # Hovered sector color. Submenu colors override it
            // # zh_CN 悬停扇区颜色；子菜单可单独设置
            "radialmenu.profileColors.highlight.tip", true, ActionTypes.PARAM_HIGHLIGHT_COLOR),
        HighlightBorder(
            // #tr radialmenu.profileColors.highlightBorder
            // # Highlight outline
            // # zh_CN 高亮边框
            "radialmenu.profileColors.highlightBorder",
            // #tr radialmenu.profileColors.highlightBorder.tip
            // # Outline color of the hovered sector
            // # zh_CN 悬停扇区的边框颜色
            "radialmenu.profileColors.highlightBorder.tip", true, ActionTypes.PARAM_HIGHLIGHT_BORDER_COLOR),
        Background(
            // #tr radialmenu.profileColors.background
            // # Background
            // # zh_CN 背景
            "radialmenu.profileColors.background",
            // #tr radialmenu.profileColors.background.tip
            // # Screen tint behind the wheel when its background is enabled
            // # zh_CN 启用轮盘背景时的屏幕蒙层颜色
            "radialmenu.profileColors.background.tip", true, ActionTypes.PARAM_BACKGROUND_COLOR),
        Icon(
            // #tr radialmenu.profileColors.icon
            // # Sprite icons
            // # zh_CN 精灵图标
            "radialmenu.profileColors.icon",
            // #tr radialmenu.profileColors.icon.tip
            // # Tint sprite icons without their own color. Items and custom PNGs keep their colors
            // # zh_CN 为未单独设色的精灵图标着色；物品和自定义 PNG 保持原色
            "radialmenu.profileColors.icon.tip", false, ActionTypes.PARAM_ICON_COLOR);

        private final String labelKey;
        private final String tooltipKey;

        /** Whether the picker offers opacity. A tint multiplies a texture, so transparency would only dim it. */
        private final boolean alpha;

        private final String key;

        Swatch(String labelKey, String tooltipKey, boolean alpha, String key) {
            this.labelKey = labelKey;
            this.tooltipKey = tooltipKey;
            this.alpha = alpha;
            this.key = key;
        }

    }
}
