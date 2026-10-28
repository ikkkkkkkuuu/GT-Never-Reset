package com.xyp.gtneverreset.client.quantum;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.GTNeverReset;

import appeng.client.gui.ScreenColor;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.texture.ExtraBlockTextures;

public class QuantumCraftingModeButton extends GuiButton implements ITooltip {

    private static final ResourceLocation ICON = new ResourceLocation(
        GTNeverReset.MODID,
        "textures/gui/quantum_crafting_mode.png");

    private boolean active;

    public QuantumCraftingModeButton(int x, int y) {
        super(0, x, y, 16, 16, "");
    }

    public void setState(boolean active) {
        this.active = active;
    }

    public boolean getState() {
        return this.active;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!this.visible) return;
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        ScreenColor.setGuiColor();
        minecraft.renderEngine.bindTexture(ExtraBlockTextures.GuiTexture("guis/states.png"));
        this.drawTexturedModalRect(this.xPosition, this.yPosition, 256 - 16, 256 - 16, 16, 16);
        ScreenColor.resetGuiColor();

        // The icon sheet is 32x16: the active frame on the left, the inactive one on the right
        minecraft.renderEngine.bindTexture(ICON);
        final double u = this.active ? 0 : 0.5;
        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(this.xPosition, this.yPosition + 16, this.zLevel, u, 1);
        tessellator.addVertexWithUV(this.xPosition + 16, this.yPosition + 16, this.zLevel, u + 0.5, 1);
        tessellator.addVertexWithUV(this.xPosition + 16, this.yPosition, this.zLevel, u + 0.5, 0);
        tessellator.addVertexWithUV(this.xPosition, this.yPosition, this.zLevel, u, 0);
        tessellator.draw();
        this.mouseDragged(minecraft, mouseX, mouseY);
    }

    @Override
    public String getMessage() {
        final String hint = StatCollector
            // #tr gtnr.gui.quantum_crafting.mode.on
            // # On: handles cycles. Requires a Quantum CPU. Disables lite mode.
            // # zh_CN 开启：支持循环样板，任务需量子 CPU 执行，与轻量模式互斥。
            // #tr gtnr.gui.quantum_crafting.mode.off
            // # Off: plans with the AE2 algorithm.
            // # zh_CN 关闭：使用AE2算法计算合成计划。
            .translateToLocal(this.active ? "gtnr.gui.quantum_crafting.mode.on" : "gtnr.gui.quantum_crafting.mode.off");
        final List<String> lines = Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(hint, 180);
        // #tr gtnr.gui.quantum_crafting.mode
        // # Quantum Crafting Mode
        // # zh_CN 量子合成模式
        return StatCollector.translateToLocal("gtnr.gui.quantum_crafting.mode") + '\n' + String.join("\n", lines);
    }

    @Override
    public int xPos() {
        return this.xPosition;
    }

    @Override
    public int yPos() {
        return this.yPosition;
    }

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }
}
