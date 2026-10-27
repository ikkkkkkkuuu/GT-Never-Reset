package com.xyp.gtneverreset.client.gui;

import java.util.Locale;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WirelessMonitorSettingsGui extends GuiScreen {

    private static final String PREFIX = "gtneverreset.gui.config.general.wireless_monitor.";
    private int page;
    private GuiTextField palette;

    @Override
    public void initGui() {
        buttonList.clear();
        int left = width / 2 - 155;
        int top = 38;
        if (page == 0) {
            String[] fields = { "enabled", "style", "scientific", "scale", "width", "refreshTicks", "showEnergy",
                "showRealtime", "showAverage", "showTier" };
            for (int i = 0; i < fields.length; i++) {
                buttonList
                    .add(new GuiButton(i, left + i % 2 * 158, top + i / 2 * 23, 152, 20, label(fields[i], value(i))));
            }
        } else {
            String[] fields = { "animatedColors", "animateValues", "customPalette", "bold", "italic", "speed" };
            for (int i = 0; i < fields.length; i++) {
                buttonList.add(
                    new GuiButton(
                        i + 10,
                        left + i % 2 * 158,
                        top + i / 2 * 23,
                        152,
                        20,
                        label(fields[i], value(i + 10))));
            }
            palette = new GuiTextField(fontRendererObj, left, top + 85, 310, 18);
            palette.setMaxStringLength(63);
            palette.setText(MainConfig.wirelessMonitor.palette);
        }
        // #tr gui.gtneverreset.wireless_settings.page
        // # Display / Colors
        // # zh_CN 显示 / 配色
        buttonList.add(
            new GuiButton(
                20,
                left,
                height - 25,
                100,
                20,
                StatCollector.translateToLocal("gui.gtneverreset.wireless_settings.page")));
        // #tr gui.gtneverreset.wireless_settings.position
        // # Move HUD
        // # zh_CN 调整位置
        buttonList.add(
            new GuiButton(
                21,
                left + 105,
                height - 25,
                100,
                20,
                StatCollector.translateToLocal("gui.gtneverreset.wireless_settings.position")));
        buttonList.add(new GuiButton(22, left + 210, height - 25, 100, 20, StatCollector.translateToLocal("gui.done")));
    }

    private String label(String field, String value) {
        return StatCollector.translateToLocal(PREFIX + field) + ": " + value;
    }

    private String value(int id) {
        var c = MainConfig.wirelessMonitor;
        return switch (id) {
            case 0 -> onOff(c.enabled);
            case 1 -> styleName(c.style);
            case 2 -> onOff(c.scientific);
            case 3 -> String.format(Locale.ROOT, "%.1f", c.scale);
            case 4 -> Integer.toString(c.width);
            case 5 -> String.format(Locale.ROOT, "%.0fs", c.refreshTicks / 20F);
            case 6 -> onOff(c.showEnergy);
            case 7 -> onOff(c.showRealtime);
            case 8 -> onOff(c.showAverage);
            case 9 -> onOff(c.showTier);
            case 10 -> onOff(c.animatedColors);
            case 11 -> onOff(c.animateValues);
            case 12 -> onOff(c.customPalette);
            case 13 -> onOff(c.bold);
            case 14 -> onOff(c.italic);
            case 15 -> String.format(Locale.ROOT, "%.1f", c.speed);
            default -> "";
        };
    }

    private static String onOff(boolean enabled) {
        return StatCollector.translateToLocal(enabled ? "options.on" : "options.off");
    }

    private static String styleName(int style) {
        // #tr gui.gtneverreset.wireless_settings.text
        // # Text
        // # zh_CN 文本
        if (style == 0) return StatCollector.translateToLocal("gui.gtneverreset.wireless_settings.text");
        // #tr gui.gtneverreset.wireless_settings.card
        // # Card
        // # zh_CN 卡片
        if (style == 1) return StatCollector.translateToLocal("gui.gtneverreset.wireless_settings.card");
        // #tr gui.gtneverreset.wireless_settings.curve
        // # Curve
        // # zh_CN 曲线
        return StatCollector.translateToLocal("gui.gtneverreset.wireless_settings.curve");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        adjust(button.id, 1);
    }

    private void adjust(int id, int direction) {
        var c = MainConfig.wirelessMonitor;
        switch (id) {
            case 0 -> WirelessMonitorHud.INSTANCE.setEnabled(!c.enabled);
            case 1 -> c.style = Math.floorMod(c.style + direction, 3);
            case 2 -> c.scientific = !c.scientific;
            case 3 -> c.scale = Math.max(0.5F, Math.min(3, Math.round((c.scale + direction * 0.1F) * 10) / 10F));
            case 4 -> c.width = Math.max(120, Math.min(600, c.width + direction * 20));
            case 5 -> c.refreshTicks = Math.max(20, Math.min(200, c.refreshTicks + direction * 20));
            case 6 -> c.showEnergy = !c.showEnergy;
            case 7 -> c.showRealtime = !c.showRealtime;
            case 8 -> c.showAverage = !c.showAverage;
            case 9 -> c.showTier = !c.showTier;
            case 10 -> c.animatedColors = !c.animatedColors;
            case 11 -> c.animateValues = !c.animateValues;
            case 12 -> c.customPalette = !c.customPalette;
            case 13 -> c.bold = !c.bold;
            case 14 -> c.italic = !c.italic;
            case 15 -> c.speed = Math.max(0, Math.min(10, Math.round((c.speed + direction * 0.1F) * 10) / 10F));
            case 20 -> page = 1 - page;
            case 21 -> mc.displayGuiScreen(new WirelessMonitorPositionGui());
            case 22 -> mc.displayGuiScreen(null);
            default -> {}
        }
        if (mc.currentScreen == this) initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        // #tr gui.gtneverreset.wireless_settings.title
        // # Wireless network HUD
        // # zh_CN 无线电网 HUD
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("gui.gtneverreset.wireless_settings.title"),
            width / 2,
            10,
            0xFFFFFF);
        // #tr gui.gtneverreset.wireless_settings.hint
        // # Values: left-click +, right-click -. Rates show net grid change.
        // # zh_CN 数值左键增加、右键减少；功率表示整个电网的净变化。
        drawCenteredString(
            fontRendererObj,
            StatCollector.translateToLocal("gui.gtneverreset.wireless_settings.hint"),
            width / 2,
            24,
            0xAAAAAA);
        if (page == 1) {
            fontRendererObj
                .drawString(StatCollector.translateToLocal(PREFIX + "palette"), width / 2 - 155, 112, 0xFFFFFF);
            palette.drawTextBox();
        }
        int previewY = Math.max(160, height - 85);
        WirelessMonitorHud.INSTANCE.drawLine("§bEU/t: §a+3,200.00 §b(1.6A EV)", width / 2 - 110, previewY);
        WirelessMonitorHud.INSTANCE.drawLine("§bEU: §f29,500,080", width / 2 - 110, previewY + 14);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        if (button == 1) {
            for (Object entry : buttonList) {
                GuiButton control = (GuiButton) entry;
                if (control.mousePressed(mc, x, y)) {
                    adjust(control.id, -1);
                    return;
                }
            }
        }
        super.mouseClicked(x, y, button);
        if (page == 1 && palette != null) palette.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (page == 1 && palette.textboxKeyTyped(character, keyCode)) {
            String value = palette.getText();
            boolean valid = value.matches("#[0-9a-fA-F]{6}(,#[0-9a-fA-F]{6}){0,7}");
            palette.setTextColor(valid ? 0xFFFFFF : 0xFF5555);
            if (valid) MainConfig.wirelessMonitor.palette = value;
            return;
        }
        super.keyTyped(character, keyCode);
    }

    @Override
    public void updateScreen() {
        if (page == 1 && palette != null) palette.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        ConfigLoader.save();
        while (WirelessMonitorHud.CONFIGURE.isPressed()) {}
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
