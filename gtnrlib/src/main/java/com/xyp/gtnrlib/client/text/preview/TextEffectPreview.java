package com.xyp.gtnrlib.client.text.preview;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.text.EffectTextLayout;
import com.xyp.gtnrlib.client.text.TextEffectRegistry;
import com.xyp.gtnrlib.text.TextEffectColors;
import com.xyp.gtnrlib.text.TextEffectFormat;
import com.xyp.gtnrlib.text.TextEffectStyle;
import com.xyp.gtnrlib.text.TextEffects;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** Interactive rendering sample using the same entry points as ordinary GUI text. */
public class TextEffectPreview extends GuiScreen {

    private static final int PAGE_SIZE = 7;
    private List<Entry> entries = List.of();
    private GuiTextField sample;
    private boolean customPalette;
    private boolean bold;
    private boolean italic;
    private int page;

    @Override
    public void initGui() {
        List<Entry> registered = new ObjectArrayList<>();
        for (String identifier : TextEffectRegistry.identifiers()) {
            TextEffectStyle style = new TextEffectStyle(identifier, IntList.of(), 1);
            String alias = TextEffectFormat.aliasFor(identifier);
            String name = effectName(style);
            registered.add(new Entry(style, alias.isEmpty() ? name : "[" + alias + "] " + name));
        }
        entries = List.copyOf(registered);
        page = Math.min(page, pageCount() - 1);
        String previous = sample == null ? "Animated text / 动态文字" : sample.getText();
        sample = new GuiTextField(fontRendererObj, 18, 28, Math.max(80, width - 36), 18);
        sample.setMaxStringLength(160);
        sample.setText(previous);
        sample.setFocused(true);
        buttonList.clear();
        int buttonWidth = Math.min(115, Math.max(1, (width - 46) / 3));
        buttonList.add(
            new GuiButton(
                0,
                18,
                height - 25,
                buttonWidth,
                20,
                // #tr gtnrlib.text_effect.palette
                // # Change palette
                // # zh_CN 切换配色
                StatCollector.translateToLocal("gtnrlib.text_effect.palette")));
        buttonList.add(
            new GuiButton(
                1,
                23 + buttonWidth,
                height - 25,
                buttonWidth,
                20,
                // #tr gtnrlib.text_effect.bold
                // # Toggle bold
                // # zh_CN 切换粗体
                StatCollector.translateToLocal("gtnrlib.text_effect.bold")));
        buttonList.add(
            new GuiButton(
                4,
                28 + buttonWidth * 2,
                height - 25,
                buttonWidth,
                20,
                // #tr gtnrlib.text_effect.italic
                // # Toggle italic
                // # zh_CN 切换斜体
                StatCollector.translateToLocal("gtnrlib.text_effect.italic")));
        buttonList.add(new GuiButton(2, width - 70, 50, 24, 20, "<"));
        buttonList.add(new GuiButton(3, width - 42, 50, 24, 20, ">"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(
            fontRendererObj,
            // #tr gtnrlib.text_effect.title
            // # Animated text effects
            // # zh_CN 动态文字效果
            StatCollector.translateToLocal("gtnrlib.text_effect.title"),
            width / 2,
            10,
            TextEffectColors.rgb(TextEffectColors.effectWhite));
        sample.drawTextBox();
        fontRendererObj.drawString(
            (page + 1) + " / " + pageCount(),
            width - 112,
            56,
            TextEffectColors.rgb(TextEffectColors.mutedText));
        float rowHeight = Math.max(20, EffectTextLayout.fontHeight(fontRendererObj) * 2.8f);
        int count = Math.min(PAGE_SIZE, entries.size() - page * PAGE_SIZE);
        float scale = Math.min(1, Math.max(0.2f, (height - 118f) / (rowHeight * Math.max(1, count))));
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 77, 0);
            GL11.glScalef(scale, scale, 1);
            drawEffects(rowHeight, width / scale);
        } finally {
            GL11.glPopMatrix();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawEffects(float rowHeight, float availableWidth) {
        int labelWidth = 0;
        for (int i = page * PAGE_SIZE; i < Math.min(entries.size(), (page + 1) * PAGE_SIZE); i++) {
            labelWidth = Math.max(
                labelWidth,
                fontRendererObj.getStringWidth(
                    entries.get(i)
                        .label()));
        }
        int textX = (int) Math.min(labelWidth + 36, availableWidth * 0.6f);
        String value = (bold ? "\u00a7l" : "") + (italic ? "\u00a7o" : "") + sample.getText() + "\u00a7r";
        float y = 0;
        for (int i = page * PAGE_SIZE; i < Math.min(entries.size(), (page + 1) * PAGE_SIZE); i++) {
            Entry entry = entries.get(i);
            TextEffectStyle preset = entry.style();
            TextEffectStyle style = customPalette ? customPalette(preset) : preset;
            String name = fontRendererObj.trimStringToWidth(entry.label(), textX - 30);
            fontRendererObj.drawString(name, 18, (int) y, TextEffectColors.rgb(TextEffectColors.mutedText));
            String rendered = TextEffects.format(style) + value;
            fontRendererObj.drawStringWithShadow(
                fontRendererObj.trimStringToWidth(rendered, (int) availableWidth - textX - 20),
                textX,
                (int) y,
                TextEffectColors.rgb(TextEffectColors.effectWhite));
            y += rowHeight;
        }
        if (!entries.isEmpty() && y + 77 + 30 < height - 25) {
            String mixed = "Plain + " + TextEffects.apply(
                value,
                entries.get(page * PAGE_SIZE)
                    .style())
                + " + plain";
            fontRendererObj
                .drawSplitString(mixed, 18, (int) y, width - 36, TextEffectColors.rgb(TextEffectColors.effectWhite));
        }
    }

    private static TextEffectStyle customPalette(TextEffectStyle preset) {
        if (preset.rendererId()
            .equals(TextEffects.INFERNUM_CYAN_SPARK.rendererId())) {
            return preset.withColors(
                TextEffectColors.rgb(TextEffectColors.effectPrismaticCyan),
                TextEffectColors.rgb(TextEffectColors.effectMoltenGold),
                TextEffectColors.rgb(TextEffectColors.effectNebulaPink),
                TextEffectColors.rgb(TextEffectColors.effectGhostCyan));
        }
        return preset.withColors(
            TextEffectColors.rgb(TextEffectColors.effectPrismaticCyan),
            TextEffectColors.rgb(TextEffectColors.effectMoltenGold),
            TextEffectColors.rgb(TextEffectColors.effectNebulaPink));
    }

    private static String effectName(TextEffectStyle style) {
        String identifier = style.rendererId();
        String namespace = GTNRLib.MODID + ":";
        return identifier.startsWith(namespace) ? identifier.substring(namespace.length()) : identifier;
    }

    private int pageCount() {
        return Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public record Entry(TextEffectStyle style, String label) {}

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) customPalette = !customPalette;
        if (button.id == 1) bold = !bold;
        if (button.id == 4) italic = !italic;
        if (button.id == 2) page = Math.floorMod(page - 1, pageCount());
        if (button.id == 3) page = (page + 1) % pageCount();
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (!sample.textboxKeyTyped(character, keyCode)) super.keyTyped(character, keyCode);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        sample.mouseClicked(x, y, button);
    }

    @Override
    public void updateScreen() {
        sample.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
