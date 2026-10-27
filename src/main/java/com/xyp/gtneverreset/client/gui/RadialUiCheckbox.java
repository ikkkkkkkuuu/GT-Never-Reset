package com.xyp.gtneverreset.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialUiCheckbox extends GuiButton {

    private static final int BOX = 11;

    public boolean checked;

    private final String label;

    public RadialUiCheckbox(int id, int x, int y, int width, String label, boolean checked) {
        super(id, x, y, width, RadialUi.ROW, label);
        this.label = label;
        this.checked = checked;
    }

    public void toggle() {
        checked = !checked;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        int boxTop = this.yPosition + (RadialUi.ROW - BOX) / 2;
        RadialUi.frame(
            this.xPosition,
            boxTop,
            this.xPosition + BOX,
            boxTop + BOX,
            0xFF101010,
            this.field_146123_n ? 0xFFFFFFFF : RadialUi.PANEL_BORDER);

        if (checked) {
            drawRect(this.xPosition + 3, boxTop + 3, this.xPosition + BOX - 3, boxTop + BOX - 3, RadialUi.TEXT_ACTIVE);
        }

        mc.fontRenderer.drawString(
            label,
            this.xPosition + BOX + 6,
            this.yPosition + (RadialUi.ROW - 8) / 2,
            this.enabled ? RadialUi.TEXT : RadialUi.TEXT_MUTED);
    }
}
