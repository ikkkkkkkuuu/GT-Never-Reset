package com.xyp.gtnrlib.gui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class ScrollablePanelScreen extends GuiScreen {

    /** How wide a tooltip is allowed to get before it wraps. About two thirds of a 320-wide dialog. */
    private static final int TOOLTIP_WIDTH = 200;

    protected static final int ID_PRIMARY = 90;
    protected static final int ID_SECONDARY = 91;
    protected static final int ID_DANGER = 92;

    /** Pixels per wheel notch. Small enough to read as movement rather than as a jump. */
    private static final int SCROLL_STEP = 12;

    /** Height of the title band: the title, a rule under it and a gap before the first row. */
    private static final int TITLE_BAND = 24;

    private boolean rebuildPending;

    /** How far the content is scrolled. Needed because a panel can be taller than a small screen. */
    protected int scrollOffset;

    private int contentHeight;

    /** Button id to tooltip text. Cleared with the controls, since ids are reassigned on every rebuild. */
    private final Map<Integer, String> tooltips = new HashMap<>();

    /** Ids of controls pinned to the footer, which never scroll out of view. */
    private final Set<Integer> footerIds = new HashSet<>();

    protected int panelLeft;
    protected int panelRight;
    protected int panelTop;
    protected int panelBottom;

    private int barY;

    protected abstract String titleKey();

    /** Arguments for the title's format string. Override when the title names what is being edited. */
    protected Object[] titleArgs() {
        return new Object[0];
    }

    protected int panelWidth() {
        return 320;
    }

    protected int panelHeightHint() {
        return -1;
    }

    /** Builds the button list and any text fields. Never call this from {@code actionPerformed}. */
    protected abstract void buildControls();

    /** Asks for the controls to be rebuilt before the next frame. */
    protected void requestRebuild() {
        rebuildPending = true;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int width = Math.min(panelWidth(), this.width - 2 * RadialUi.PAD);
        panelLeft = (this.width - width) / 2;
        panelRight = panelLeft + width;
        int hint = panelHeightHint();
        if (hint > 0) {
            int panelHeight = RadialUi.PAD + TITLE_BAND + hint + RadialUi.GAP + RadialUi.PAD;
            int total = panelHeight + RadialUi.GAP + RadialUi.ROW;
            panelTop = Math.max(RadialUi.PAD * 2, (this.height - total) / 2);
            panelBottom = panelTop + panelHeight;
            barY = panelBottom + RadialUi.GAP;
        } else {
            barY = RadialUi.bottomBarY(this.height);
            panelTop = RadialUi.PAD * 2;
            panelBottom = barY - RadialUi.GAP;
        }

        this.buttonList.clear();
        tooltips.clear();
        footerIds.clear();
        buildControls();
    }

    protected void tooltip(int buttonId, String text) {
        if (text != null && !text.isEmpty()) {
            tooltips.put(Integer.valueOf(buttonId), text);
        }
    }

    protected int viewportTop() {
        return panelTop + RadialUi.PAD + TITLE_BAND - RadialUi.GAP;
    }

    protected int viewportBottom() {
        int footer = footerHeight();
        return panelBottom - RadialUi.PAD - (footer > 0 ? footer + RadialUi.GAP : 0);
    }

    protected int contentTop() {
        return viewportTop() + RadialUi.GAP;
    }

    protected int scrolledTop() {
        return contentTop() - scrollOffset;
    }

    protected void setContentHeight(int height) {
        contentHeight = height;
    }

    /** Marks a control as part of the footer, so scrolling never hides it. */
    protected void markFooter(int buttonId) {
        footerIds.add(Integer.valueOf(buttonId));
    }

    private boolean isFooterButton(GuiButton button) {
        return footerIds.contains(Integer.valueOf(button.id));
    }

    /** Height reserved at the bottom of the panel for controls that do not scroll. */
    protected int footerHeight() {
        return 0;
    }

    protected boolean framedViewport() {
        return false;
    }

    protected int contentBottom() {
        return viewportBottom() - RadialUi.GAP;
    }

    protected int viewportHeight() {
        return contentBottom() - contentTop();
    }

    protected int maxScroll() {
        return Math.max(0, contentHeight - viewportHeight());
    }

    protected boolean isVisibleRow(int y) {
        return y + RadialUi.ROW > contentTop() && y < contentBottom();
    }

    protected int viewportLeft() {
        return panelLeft + RadialUi.PAD;
    }

    protected int viewportRight() {
        return panelRight - RadialUi.PAD;
    }

    protected int contentLeft() {
        return viewportLeft() + (framedViewport() ? RadialUi.GAP : 0);
    }

    protected int contentRight() {
        return viewportRight() - (framedViewport() ? RadialUi.GAP : 0);
    }

    protected int contentWidth() {
        return contentRight() - contentLeft();
    }

    protected void addBottomBar(String primaryKey, String dangerKey, String secondaryKey) {
        int count = 1 + (dangerKey == null ? 0 : 1) + (secondaryKey == null ? 0 : 1);
        int width = count == 1 ? 200 : (count == 2 ? 98 : 96);
        int total = count * width + (count - 1) * RadialUi.GAP;
        int x = this.width / 2 - total / 2;
        int y = barY;

        this.buttonList.add(new GuiButton(ID_PRIMARY, x, y, width, RadialUi.ROW, I18n.format(primaryKey)));
        markFooter(ID_PRIMARY);
        x += width + RadialUi.GAP;

        if (dangerKey != null) {
            this.buttonList.add(new GuiButton(ID_DANGER, x, y, width, RadialUi.ROW, I18n.format(dangerKey)));
            markFooter(ID_DANGER);
            x += width + RadialUi.GAP;
        }
        if (secondaryKey != null) {
            this.buttonList.add(new GuiButton(ID_SECONDARY, x, y, width, RadialUi.ROW, I18n.format(secondaryKey)));
            markFooter(ID_SECONDARY);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (rebuildPending) {
            rebuildPending = false;
            this.buttonList.clear();
            tooltips.clear();
            footerIds.clear();
            buildControls();
        }

        for (Object raw : this.buttonList) {
            GuiButton button = (GuiButton) raw;
            if (scrolls(button)) {
                button.visible = isVisibleRow(button.yPosition);
            }
        }

        this.drawDefaultBackground();
        RadialUi.panel(panelLeft, panelTop, panelRight, panelBottom);
        this.drawCenteredString(
            this.fontRendererObj,
            I18n.format(titleKey(), titleArgs()),
            this.width / 2,
            panelTop + RadialUi.PAD - 2,
            RadialUi.TEXT);

        // A rule under the title, so the edge of the scrolling area is unmistakably below it. Drawn to the same
        // edges as a frame, so the two line up on a screen that has both.
        Gui.drawRect(
            viewportLeft(),
            panelTop + RadialUi.PAD + 12,
            viewportRight(),
            panelTop + RadialUi.PAD + 13,
            0x40FFFFFF);

        if (framedViewport()) {
            RadialUi.list(viewportLeft(), viewportTop(), viewportRight(), viewportBottom());
        }

        boolean clipped = framedViewport();
        if (clipped) {
            beginClip();
        }
        drawContent(mouseX, mouseY, partialTicks);
        drawButtons(mouseX, mouseY, true);
        if (clipped) {
            endClip();
        }

        drawButtons(mouseX, mouseY, false);
        drawOverlay(mouseX, mouseY, partialTicks);
        drawScrollbar();
        drawTooltip(mouseX, mouseY);
    }

    private void drawButtons(int mouseX, int mouseY, boolean scrolling) {
        for (Object raw : this.buttonList) {
            GuiButton button = (GuiButton) raw;
            if (button.visible && scrolls(button) == scrolling) {
                button.drawButton(this.mc, mouseX, mouseY);
            }
        }
    }

    private boolean scrolls(GuiButton button) {
        return !isFooterButton(button);
    }

    private void beginClip() {
        RadialUi.beginClip(panelLeft, contentTop(), panelRight, contentBottom());
    }

    private void endClip() {
        RadialUi.endClip();
    }

    private void drawTooltip(int mouseX, int mouseY) {
        if (tooltips.isEmpty()) {
            return;
        }
        for (Object raw : this.buttonList) {
            GuiButton button = (GuiButton) raw;
            if (!button.visible || mouseX < button.xPosition
                || mouseX >= button.xPosition + button.width
                || mouseY < button.yPosition
                || mouseY >= button.yPosition + button.height) {
                continue;
            }
            String text = tooltips.get(Integer.valueOf(button.id));
            if (text != null) {
                @SuppressWarnings("unchecked")
                List<String> wrapped = this.fontRendererObj.listFormattedStringToWidth(text, TOOLTIP_WIDTH);
                drawHoveringText(allWhite(wrapped), mouseX, mouseY, this.fontRendererObj);
            }
            return;
        }
    }

    private static List<String> allWhite(List<String> lines) {
        List<String> coloured = new ArrayList<>(lines.size());
        for (String line : lines) {
            coloured.add(EnumChatFormatting.WHITE + line);
        }
        return coloured;
    }

    /** Drawn after the buttons, for anything that has to sit on top of one. */
    protected void drawOverlay(int mouseX, int mouseY, float partialTicks) {}

    /** Last chance to read the controls before scrolling rebuilds them. */
    protected void beforeScroll() {}

    protected boolean isInsideViewport(int mouseY) {
        return mouseY >= contentTop() && mouseY < contentBottom();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!isInsideViewport(mouseY)) {
            // Hidden buttons are skipped by the vanilla hit test; visibility is recomputed next frame anyway.
            for (Object raw : this.buttonList) {
                GuiButton button = (GuiButton) raw;
                if (scrolls(button)) {
                    button.visible = false;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected boolean isShowing() {
        return this.mc.currentScreen == this;
    }

    private void drawScrollbar() {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int trackTop = contentTop();
        int trackHeight = viewportHeight();
        int thumbHeight = Math.max(16, trackHeight * trackHeight / (trackHeight + max));
        int thumbTop = trackTop + (trackHeight - thumbHeight) * scrollOffset / max;

        int x = panelRight - 4;
        Gui.drawRect(x, trackTop, x + 2, trackTop + trackHeight, 0x40FFFFFF);
        Gui.drawRect(x, thumbTop, x + 2, thumbTop + thumbHeight, 0xC0FFFFFF);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || maxScroll() == 0) {
            return;
        }
        int next = Math.max(0, Math.min(maxScroll(), scrollOffset + (wheel > 0 ? -SCROLL_STEP : SCROLL_STEP)));
        if (next != scrollOffset) {
            // Scrolling rebuilds the controls, which recreates the text fields from the model - so whatever is typed
            // has to be read back first, or it is thrown away by the act of scrolling.
            beforeScroll();
            scrollOffset = next;
            requestRebuild();
        }
    }

    protected abstract void drawContent(int mouseX, int mouseY, float partialTicks);

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            onCancel();
            return;
        }
        if (!handleKey(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    /** @return true when the screen consumed the key */
    protected boolean handleKey(char typedChar, int keyCode) {
        return false;
    }

    /** Escape, and the secondary button unless a screen says otherwise. */
    protected void onCancel() {
        mc.displayGuiScreen(null);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
