package com.xyp.gtnrlib.client.gui;

import net.minecraft.client.gui.GuiButton;

import com.xyp.gtnrlib.gui.widget.RadialUi;
import com.xyp.gtnrlib.gui.widget.ScrollablePanelScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ConfirmationScreen extends ScrollablePanelScreen {

    private final String questionKey;
    private final String subject;
    private final String confirmKey;
    private final Result result;

    public ConfirmationScreen(String questionKey, String subject, String confirmKey, Result result) {
        this.questionKey = questionKey;
        this.subject = subject;
        this.confirmKey = confirmKey;
        this.result = result;
    }

    @Override
    protected String titleKey() {
        return questionKey;
    }

    @Override
    protected int panelWidth() {
        return 260;
    }

    @Override
    protected int panelHeightHint() {
        return RadialUi.STEP * 2;
    }

    @Override
    protected void buildControls() {
        setContentHeight(RadialUi.STEP);
        // Cancel first: the destructive choice should not be the one under the cursor by default.
        addBottomBar("gui.cancel", null, confirmKey);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_SECONDARY) {
            result.onConfirmed();
        }
        onCancel();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        this.drawCenteredString(
            this.fontRendererObj,
            subject == null ? "" : subject,
            this.width / 2,
            contentTop() + RadialUi.GAP,
            0xFFFFFF80);
    }

    public interface Result {

        void onConfirmed();
    }
}
