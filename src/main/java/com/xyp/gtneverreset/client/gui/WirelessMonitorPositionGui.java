package com.xyp.gtneverreset.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.xyp.gtneverreset.client.gui.WirelessMonitorHud.Layout;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only drag editor. Moving updates the live HUD, and every exit path saves the position.
 * Opening it while the HUD is disabled shows a preview without enabling network polling.
 */
@SideOnly(Side.CLIENT)
public final class WirelessMonitorPositionGui extends GuiScreen {

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(
            new GuiButton(
                2,
                width / 2 - 50,
                72,
                100,
                20,
                // #tr gui.gtneverreset.wireless_position.settings
                // # HUD settings
                // # zh_CN HUD 设置
                StatCollector.translateToLocal("gui.gtneverreset.wireless_position.settings")));
        buttonList.add(
            new GuiButton(
                0,
                width / 2 - 102,
                48,
                100,
                20,
                // #tr gui.gtneverreset.wireless_position.reset
                // # Reset position
                // # zh_CN 恢复默认位置
                StatCollector.translateToLocal("gui.gtneverreset.wireless_position.reset")));
        buttonList.add(
            new GuiButton(
                1,
                width / 2 + 2,
                48,
                100,
                20,
                // #tr gui.gtneverreset.wireless_position.done
                // # Save and close
                // # zh_CN 保存并退出
                StatCollector.translateToLocal("gui.gtneverreset.wireless_position.done")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x18000000);
        drawCenteredString(
            fontRendererObj,
            // #tr gui.gtneverreset.wireless_position.title
            // # Wireless HUD position
            // # zh_CN 调整无线电网 HUD 位置
            StatCollector.translateToLocal("gui.gtneverreset.wireless_position.title"),
            width / 2,
            12,
            0xFFFFFF);
        drawCenteredString(
            fontRendererObj,
            // #tr gui.gtneverreset.wireless_position.hint
            // # Drag the HUD with left mouse; press the edit key or Esc to save.
            // # zh_CN 左键拖动文字区域；再次按编辑键或 Esc 保存退出。
            StatCollector.translateToLocal("gui.gtneverreset.wireless_position.hint"),
            width / 2,
            29,
            0xDDDDDD);
        Layout layout = WirelessMonitorHud.INSTANCE.layout(width, height);
        int left = (int) (layout.x - 2 * layout.scale) - 2;
        int top = (int) (layout.y - 2 * layout.scale) - 2;
        int right = layout.x + layout.width + 2;
        int bottom = layout.y + layout.height + 2;
        int color = dragging || layout.contains(mouseX, mouseY) ? 0xFF55FFFF : 0xFFAAAAAA;
        drawRect(left, top, right, top + 1, color);
        drawRect(left, bottom - 1, right, bottom, color);
        drawRect(left, top, left + 1, bottom, color);
        drawRect(right - 1, top, right, bottom, color);
        WirelessMonitorHud.INSTANCE.draw(layout);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (mc.currentScreen != this || button != 0) return;
        Layout layout = WirelessMonitorHud.INSTANCE.layout(width, height);
        if (layout.contains(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - layout.x;
            dragOffsetY = mouseY - layout.y;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (!dragging || button != 0) return;
        Layout before = WirelessMonitorHud.INSTANCE.layout(width, height);
        MainConfig.wirelessMonitor.xOffset = mouseX - dragOffsetX - 4;
        MainConfig.wirelessMonitor.yOffset = height - 42 - before.height - (mouseY - dragOffsetY);
        // Store the clamped position too, so dragging past an edge does not leave a hidden offset behind.
        Layout layout = WirelessMonitorHud.INSTANCE.layout(width, height);
        MainConfig.wirelessMonitor.xOffset = layout.x - 4;
        MainConfig.wirelessMonitor.yOffset = height - 42 - layout.height - layout.y;
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (button == 0) dragging = false;
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            dragging = false;
            MainConfig.wirelessMonitor.xOffset = 0;
            MainConfig.wirelessMonitor.yOffset = 0;
        } else if (button.id == 1) mc.displayGuiScreen(null);
        else if (button.id == 2) mc.displayGuiScreen(new WirelessMonitorSettingsGui());
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == WirelessMonitorHud.MOVE.getKeyCode()) {
            mc.displayGuiScreen(null);
        } else super.keyTyped(character, keyCode);
    }

    @Override
    public void onGuiClosed() {
        dragging = false;
        ConfigLoader.save();
        while (WirelessMonitorHud.MOVE.isPressed()) {}
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
