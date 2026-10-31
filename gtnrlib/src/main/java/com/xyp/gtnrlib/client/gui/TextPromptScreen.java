package com.xyp.gtnrlib.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnrlib.gui.widget.RadialUi;
import com.xyp.gtnrlib.gui.widget.ScrollablePanelScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TextPromptScreen extends ScrollablePanelScreen {

    private final String promptKey;
    private final Result result;

    private GuiTextField input;
    private String text;
    private String errorKey;

    public TextPromptScreen(String promptKey, String initialValue, Result result) {
        this.promptKey = promptKey;
        this.result = result;
        this.text = initialValue == null ? "" : initialValue;
    }

    @Override
    protected String titleKey() {
        return promptKey;
    }

    @Override
    protected int panelWidth() {
        return 260;
    }

    @Override
    protected void buildControls() {
        int y = contentTop() + RadialUi.GAP;
        input = new GuiTextField(this.fontRendererObj, contentLeft() + 1, y + 3, contentWidth() - 2, 14);
        input.setMaxStringLength(48);
        input.setText(text);
        input.setFocused(true);
        input.setCursorPositionEnd();

        setContentHeight(RadialUi.STEP * 2);
        addBottomBar("gui.done", null, "gui.cancel");
    }

    @Override
    protected int panelHeightHint() {
        return RadialUi.GAP + RadialUi.STEP * 2;
    }

    private void confirm() {
        text = input.getText()
            .trim();
        if (text.isEmpty()) {
            // #tr gtnrlib.gui.editor.nameEmpty
            // # Name cannot be empty
            // # zh_CN 名称不能为空
            errorKey = "gtnrlib.gui.editor.nameEmpty";
            return;
        }
        errorKey = result.onConfirm(text);
        if (errorKey == null) {
            onCancel();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            confirm();
        } else if (button.id == ID_SECONDARY) {
            onCancel();
        }
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        input.drawTextBox();
        if (errorKey != null) {
            this.drawCenteredString(
                this.fontRendererObj,
                EnumChatFormatting.RED + I18n.format(errorKey),
                this.width / 2,
                input.yPosition + 22,
                RadialUi.TEXT);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (!isShowing()) {
            return;
        }
        input.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            confirm();
            return true;
        }
        if (input.textboxKeyTyped(typedChar, keyCode)) {
            errorKey = null;
            return true;
        }
        return false;
    }

    public interface Result {

        /** @return null when accepted, otherwise a translation key describing what is wrong */
        String onConfirm(String value);
    }
}
