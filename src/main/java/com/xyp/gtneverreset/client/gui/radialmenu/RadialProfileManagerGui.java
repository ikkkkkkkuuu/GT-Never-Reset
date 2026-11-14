package com.xyp.gtneverreset.client.gui.radialmenu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Mouse;

import com.xyp.gtneverreset.client.radialmenu.profile.ProfileManager;
import com.xyp.gtneverreset.client.radialmenu.profile.ProfileStorage;
import com.xyp.gtnrlib.gui.widget.RadialUi;
import com.xyp.gtnrlib.gui.widget.RadialUiList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialProfileManagerGui extends RadialUiScreen {

    private static final int ID_ACTIVATE = 1;
    private static final int ID_RULES = 2;
    private static final int ID_COLORS = 7;
    private static final int ID_NEW = 3;
    private static final int ID_RENAME = 4;
    private static final int ID_DUPLICATE = 5;
    private static final int ID_DELETE = 6;
    private static final int ID_BACKUPS = 8;

    private static final int BUTTON_COLUMN = 104;

    private final List<String> profiles = new ArrayList<>();

    private String selected;

    private RadialUiList list;

    /** Kept across rebuilds, which make a fresh list, so selecting a row does not jump back to the top. */
    private int listScroll;

    /** Set when an operation fails, so a refused name or a failed delete is visible instead of silent. */
    private String errorKey;

    @Override
    protected String titleKey() {
        // #tr gtnr.radialmenu.profiles.title
        // # RadialMenu profiles
        // # zh_CN RadialMenu 配置
        return "gtnr.radialmenu.profiles.title";
    }

    @Override
    protected int panelWidth() {
        return 360;
    }

    /** A line for the error, reserved only while there is one - the list is worth more than a permanent blank. */
    @Override
    protected int footerHeight() {
        return errorKey == null ? 0 : 10;
    }

    @Override
    protected void buildControls() {
        profiles.clear();
        profiles.addAll(ProfileStorage.listProfileNames());
        if (selected == null || !profiles.contains(selected)) {
            selected = ProfileManager.activeName();
        }

        int listRight = viewportRight() - BUTTON_COLUMN - RadialUi.GAP;
        list = new RadialUiList(viewportLeft(), viewportTop(), listRight, viewportBottom(), 13);
        list.scrollTo(listScroll, profiles.size());

        int x = listRight + RadialUi.GAP;
        int y = viewportTop();
        boolean has = profiles.contains(selected);
        boolean isActive = has && selected.equals(ProfileManager.activeName());

        y = addAction(
            ID_ACTIVATE,
            // #tr gtnr.radialmenu.profiles.activate
            // # Activate
            // # zh_CN 启用
            "gtnr.radialmenu.profiles.activate",
            // #tr gtnr.radialmenu.profiles.activate.tooltip
            // # Use the selected profile
            // # zh_CN 启用选中的配置方案
            "gtnr.radialmenu.profiles.activate.tooltip",
            x,
            y,
            has && !isActive);
        y = addAction(
            ID_RULES,
            // #tr gtnr.radialmenu.profiles.rules
            // # Auto-bind rules
            // # zh_CN 自动绑定规则
            "gtnr.radialmenu.profiles.rules",
            // #tr gtnr.radialmenu.profiles.rules.tooltip
            // # Set when joining a server or world should activate this profile
            // # zh_CN 设置进入服务器或世界时自动启用此配置方案的条件
            "gtnr.radialmenu.profiles.rules.tooltip",
            x,
            y,
            has);
        y = addAction(
            ID_COLORS,
            // #tr gtnr.radialmenu.profiles.colors
            // # Colors
            // # zh_CN 颜色
            "gtnr.radialmenu.profiles.colors",
            // #tr gtnr.radialmenu.profiles.colors.tooltip
            // # Set ring and highlight colors for this profile
            // # zh_CN 设置此配置方案的轮盘和高亮颜色
            "gtnr.radialmenu.profiles.colors.tooltip",
            x,
            y,
            has);
        y += RadialUi.GAP;
        y = addAction(
            ID_NEW,
            // #tr gtnr.radialmenu.profiles.new
            // # New profile
            // # zh_CN 新建配置
            "gtnr.radialmenu.profiles.new",
            // #tr gtnr.radialmenu.profiles.new.tooltip
            // # Create an empty profile
            // # zh_CN 创建空白配置方案
            "gtnr.radialmenu.profiles.new.tooltip",
            x,
            y,
            true);
        y = addAction(
            ID_RENAME,
            // #tr gtnr.radialmenu.profiles.rename
            // # Rename
            // # zh_CN 重命名
            "gtnr.radialmenu.profiles.rename",
            // #tr gtnr.radialmenu.profiles.rename.tooltip
            // # Rename this profile and its file
            // # zh_CN 重命名此配置方案及其文件
            "gtnr.radialmenu.profiles.rename.tooltip",
            x,
            y,
            has);
        y = addAction(
            ID_DUPLICATE,
            // #tr gtnr.radialmenu.profiles.duplicate
            // # Duplicate
            // # zh_CN 复制
            "gtnr.radialmenu.profiles.duplicate",
            // #tr gtnr.radialmenu.profiles.duplicate.tooltip
            // # Copy this profile, including its rules and entries
            // # zh_CN 复制此配置方案，包含规则和全部条目
            "gtnr.radialmenu.profiles.duplicate.tooltip",
            x,
            y,
            has);
        y = addAction(
            ID_BACKUPS,
            // #tr gtnr.radialmenu.profiles.backups
            // # Backups...
            // # zh_CN 备份...
            "gtnr.radialmenu.profiles.backups",
            // #tr gtnr.radialmenu.profiles.backups.tooltip
            // # View versions backed up when changes were saved
            // # zh_CN 查看保存修改时备份的历史版本
            "gtnr.radialmenu.profiles.backups.tooltip",
            x,
            y,
            has);
        y += RadialUi.GAP;
        addAction(
            ID_DELETE,
            "gtnr.radialmenu.profiles.delete",
            // #tr gtnr.radialmenu.profiles.delete.tooltip
            // # Delete this profile. If active, another profile is selected automatically
            // # zh_CN 删除此配置方案；若正在使用，则自动切换到其他方案
            "gtnr.radialmenu.profiles.delete.tooltip",
            x,
            y,
            has);

        addBottomBar("gui.done", null, null);
    }

    private int addAction(int id, String labelKey, String tooltipKey, int x, int y, boolean enabled) {
        GuiButton button = new GuiButton(id, x, y, BUTTON_COLUMN, RadialUi.ROW, I18n.format(labelKey));
        button.enabled = enabled;
        this.buttonList.add(button);
        tooltip(id, describe(tooltipKey));
        return y + RadialUi.STEP;
    }

    private static String describe(String key) {
        String translated = I18n.format(key);
        return translated.equals(key) ? null : translated;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        errorKey = null;

        switch (button.id) {
            case ID_PRIMARY:
                RadialGuiStack.closeAll();
                return;

            case ID_ACTIVATE:
                if (!ProfileManager.switchTo(selected)) {
                    // #tr gtnr.radialmenu.profiles.error.load
                    // # Could not load that profile
                    // # zh_CN 无法加载该配置
                    errorKey = "gtnr.radialmenu.profiles.error.load";
                }
                requestRebuild();
                return;

            case ID_RULES:
                RadialGuiStack.push(new RadialAutoBindRulesGui(selected));
                return;

            case ID_COLORS:
                RadialGuiStack.push(new RadialProfileColorsGui(selected));
                return;

            case ID_NEW:
                RadialGuiStack
                    .push(new RadialTextPromptGui("gtnr.radialmenu.profiles.new", "", new RadialTextPromptGui.Result() {

                        @Override
                        public String onConfirm(String value) {
                            if (!ProfileManager.create(value)) {
                                // #tr gtnr.radialmenu.profiles.error.exists
                                // # A profile with that name already exists
                                // # zh_CN 已存在同名配置
                                return "gtnr.radialmenu.profiles.error.exists";
                            }
                            selected = ProfileStorage.sanitize(value);
                            return null;
                        }
                    }));
                return;

            case ID_RENAME:
                final String renameFrom = selected;
                RadialGuiStack.push(
                    new RadialTextPromptGui(
                        "gtnr.radialmenu.profiles.rename",
                        renameFrom,
                        new RadialTextPromptGui.Result() {

                            @Override
                            public String onConfirm(String value) {
                                if (!ProfileManager.rename(renameFrom, value)) {
                                    return "gtnr.radialmenu.profiles.error.exists";
                                }
                                selected = ProfileStorage.sanitize(value);
                                return null;
                            }
                        }));
                return;

            case ID_DUPLICATE:
                final String copyFrom = selected;
                RadialGuiStack.push(
                    new RadialTextPromptGui(
                        "gtnr.radialmenu.profiles.duplicate",
                        copyFrom + "-copy",
                        new RadialTextPromptGui.Result() {

                            @Override
                            public String onConfirm(String value) {
                                if (!ProfileManager.duplicate(copyFrom, value)) {
                                    return "gtnr.radialmenu.profiles.error.exists";
                                }
                                selected = ProfileStorage.sanitize(value);
                                return null;
                            }
                        }));
                return;

            case ID_BACKUPS:
                RadialGuiStack.push(new RadialProfileBackupsGui(selected));
                return;

            case ID_DELETE:
                // #tr gtnr.radialmenu.profiles.confirm_delete
                // # Delete this profile?
                // # zh_CN 删除此配置？
                RadialGuiStack.push(
                    new RadialConfirmGui(
                        "gtnr.radialmenu.profiles.confirm_delete",
                        selected,
                        new RadialConfirmGui.Result() {

                            @Override
                            public void onConfirmed() {
                                if (!ProfileManager.delete(selected)) {
                                    // #tr gtnr.radialmenu.profiles.error.delete
                                    // # Could not delete that profile
                                    // # zh_CN 无法删除该配置
                                    errorKey = "gtnr.radialmenu.profiles.error.delete";
                                }
                                selected = null;
                            }
                        }));
                return;

            default:
                break;
        }
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        list.drawFrame();

        String activeName = ProfileManager.activeName();
        int hovered = list.itemAt(mouseX, mouseY, profiles.size());

        for (int row = 0; row < list.rowsVisible() && row + list.firstRow() < profiles.size(); row++) {
            int index = row + list.firstRow();
            String name = profiles.get(index);

            list.drawRowBackground(row, name.equals(selected), index == hovered);

            boolean active = name.equals(activeName);
            String label = RadialUi.fit(name, list.textWidth() - (active ? 42 : 0));
            this.fontRendererObj.drawString(
                label,
                list.textLeft(),
                list.rowTop(row) + 3,
                active ? RadialUi.TEXT_ACTIVE : RadialUi.TEXT);

            if (active) {
                // #tr gtnr.radialmenu.profiles.active_marker
                // # (active)
                // # zh_CN (当前)
                String marker = I18n.format("gtnr.radialmenu.profiles.active_marker");
                this.fontRendererObj.drawString(
                    EnumChatFormatting.DARK_GREEN + marker,
                    list.right - 5 - this.fontRendererObj.getStringWidth(marker),
                    list.rowTop(row) + 3,
                    RadialUi.TEXT);
            }
        }

        list.drawScrollbar(profiles.size());

        if (profiles.isEmpty()) {
            this.drawCenteredString(
                this.fontRendererObj,
                // #tr gtnr.radialmenu.profiles.empty
                // # No profiles yet
                // # zh_CN 暂无配置
                EnumChatFormatting.GRAY + I18n.format("gtnr.radialmenu.profiles.empty"),
                (list.left + list.right) / 2,
                list.top + 8,
                RadialUi.TEXT);
        }

        if (errorKey != null) {
            this.drawCenteredString(
                this.fontRendererObj,
                EnumChatFormatting.RED + I18n.format(errorKey),
                this.width / 2,
                viewportBottom() + RadialUi.GAP,
                RadialUi.TEXT);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        list.scroll(Mouse.getEventDWheel(), profiles.size());
        listScroll = list.firstRow();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }
        int index = list.itemAt(mouseX, mouseY, profiles.size());
        if (index >= 0) {
            selected = profiles.get(index);
            errorKey = null;
            requestRebuild();
        }
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.closeAll();
    }
}
