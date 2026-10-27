// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtneverreset.client.gui;

import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.APPLY_FILTER;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.BACK;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CLOSE_DETAILS;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.COPY_POSITION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CYCLE_FILTER;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CYCLE_VISUALISATION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.DELETE_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.HIGHLIGHT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.NEXT_PAGE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_DETAILS;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_SOURCES;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.PREVIOUS_PAGE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.REFRESH;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.RENAME;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.RENAME_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.SELECT_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TELEPORT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_AUTO;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_GROUPING;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_PAUSE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.isRowAction;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.rowIndex;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.type;
import static net.minecraft.client.gui.Gui.drawRect;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.core.mixins.early.minecraft.GuiScreenAccessor;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.github.bsideup.jabel.Desugar;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.event.WirelessCardInput;
import com.xyp.gtneverreset.client.gui.WirelessCardGui.Layout;
import com.xyp.gtneverreset.client.gui.WirelessCardGui.WirelessCardButton;
import com.xyp.gtneverreset.client.render.WirelessWorldHighlight;
import com.xyp.gtneverreset.container.WirelessCardContainer;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.machine.wireless.InterfaceNameLocalization;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardVisualisation;
import com.xyp.gtneverreset.utils.text.WirelessCardText;

import appeng.client.gui.ScreenColor;
import appeng.core.localization.ColorUtils;
import appeng.core.localization.GuiText;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gtneioreplugin.util.DimensionHelper;

@SideOnly(Side.CLIENT)
public class WirelessCardGui extends WirelessCardPanel {

    private final TextFieldWidget search, dimension;
    private final IPanelHandler detailsPanel, groupPanel, sourceRenamePanel;
    private long renameSourceId;
    private boolean showingDetails;
    private String searchText = "", dimensionText = "";

    public WirelessCardGui(WirelessCardContainer container) {
        super("wireless_card", container);
        search = textField(64).hintText(tr("search_placeholder"))
            .hintColor(ColorUtils.craftingTreeRequest.getColor());
        dimension = textField(12);
        search.autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> searchText, text -> {
                if (searchText.equals(text)) return;
                searchText = text;
                applyFilters(false, false);
            }));
        dimension.autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> dimensionText, text -> {
                if (dimensionText.equals(text)) return;
                dimensionText = text;
                applyFilters(false, false);
            }));
        child(search).child(dimension);
        groupPanel = IPanelHandler.simple(this, (parent, player) -> new WirelessCardGroupPanel(container), true);
        detailsPanel = IPanelHandler.simple(this, (parent, player) -> new WirelessCardDetailsPanel(container), true);
        sourceRenamePanel = IPanelHandler
            .simple(this, (parent, player) -> new WirelessCardRenameDialog(this, () -> renameSourceId), true);
        for (int id : new int[] { TOGGLE_AUTO, CYCLE_FILTER, CYCLE_VISUALISATION, TOGGLE_GROUPING, OPEN_SOURCES })
            addButton(id);
        addRowButtons(OPEN_DETAILS, RENAME_SOURCE, DELETE_SOURCE);
    }

    @Override
    boolean isDetailView() {
        return false;
    }

    @Override
    public int rowTop() {
        return 82;
    }

    @Override
    void confirm() {
        action(APPLY_FILTER);
    }

    @Override
    boolean editingText() {
        return search.isFocused() || dimension.isFocused();
    }

    public ModularScreen screen() {
        return new ModularScreen(GTNeverReset.MODID, this) {

            @Override
            public void onResize(int width, int height) {
                resizeLayout(width, height);
                for (ModularPanel panel : getPanelManager().getOpenPanels()) {
                    if (panel instanceof WirelessCardPanel cardPanel && panel != WirelessCardGui.this)
                        cardPanel.resizeLayout(width, height);
                }
                super.onResize(width, height);
            }

            @Override
            public boolean onMousePressed(int button) {
                ModularPanel previous = getPanelManager().getTopMostPanel();
                ModularPanel hovered = hoveredPanel();
                if (button == 1) {
                    if (hovered == null) return true;
                    if (hovered.getTopHovering() instanceof TextFieldWidget) super.onMousePressed(button);
                    else if (hovered instanceof WirelessCardRenameDialog dialog) dialog.closeIfOpen();
                    else if (hovered instanceof WirelessCardPanel panel && (panel.isDetailView() || panel.view()
                        .getBoolean("group")
                        || panel.view()
                            .getBoolean("sourcesPage")))
                        panel.action(BACK);
                    return true;
                }
                if (hovered instanceof WirelessCardPanel panel && button > 1
                    && WirelessCardInput.visualisationKey(button - 100)
                    && !(getContext().getFocusedWidget()
                        .getElement() instanceof TextFieldWidget)) {
                    panel.action(CYCLE_VISUALISATION);
                    return true;
                }
                boolean handled = super.onMousePressed(button);
                // The opening button clears focus after its callback returns.
                if (getPanelManager().getTopMostPanel() instanceof WirelessCardRenameDialog dialog
                    && previous != dialog) getContext().focus(dialog.name);
                return handled;
            }

            private ModularPanel hoveredPanel() {
                for (ModularPanel panel : getPanelManager().getOpenPanels()) {
                    if (panel.isBelowMouse()) return panel;
                    if (panel.disablePanelsBelow()) break;
                }
                return null;
            }

            @Override
            public boolean onMouseRelease(int button) {
                return super.onMouseRelease(button) || button == 1;
            }
        };
    }

    @Override
    void resizePanel(Layout layout) {
        xSize = layout.width();
        ySize = layout.height();
        container.clientView()
            .resize(layout.rows());
        int dx = xSize - 320;
        search.pos(10, 62)
            .size(124 + dx, 12);
        dimension.pos(148 + dx, 62)
            .size(34, 12);
    }

    @Override
    void layoutButton(WirelessCardButton control, Layout layout) {
        var button = control.widget;
        int dx = xSize - 320;
        switch (control.id) {
            case OPEN_SOURCES -> button.pos(8, 23)
                .size(xSize - 16, 12);
            case TOGGLE_AUTO -> button.pos(xSize - 28, 10)
                .size(16, 16);
            case TOGGLE_GROUPING -> button.pos(264 + dx, 59)
                .size(44, 20);
            case CYCLE_FILTER -> button.pos(188 + dx, 59)
                .size(72, 20);
            case CYCLE_VISUALISATION -> button.pos(layout.visualisationX(), ySize - 27)
                .size(layout.visualisationWidth(), 20);
        }
    }

    @Override
    boolean updatePanel() {
        boolean detail = container.display.getBoolean("detail");
        if (!detail) {
            if (container.clientView()
                .groupDisplay()
                .getBoolean("group")) groupPanel.openPanel();
            else groupPanel.closePanel();
        }
        if (detail != showingDetails) {
            showingDetails = detail;
            if (detail) detailsPanel.openPanel();
            else detailsPanel.closePanel();
        }
        boolean showFilters = !detail && !detailsPanel.isPanelOpen() && !view().getBoolean("sourcesPage");
        if (!showFilters && editingText()) getContext().removeFocus();
        search.setEnabled(showFilters);
        dimension.setEnabled(showFilters);
        return true;
    }

    @Override
    public void action(int id) {
        if (type(id) == RENAME_SOURCE) {
            if (!layoutReady() || !view().getBoolean("sourcesPage")) return;
            renameSourceId = view().getTagList("rows", 10)
                .getCompoundTagAt(rowIndex(id))
                .getLong("targetId");
            if (renameSourceId > 0) sourceRenamePanel.openPanel();
            return;
        }
        super.action(id);
    }

    @Override
    public void sendAction(int id) {
        if (type(id) == DELETE_SOURCE) {
            if (!view().getBoolean("sourcesPage")) return;
            long target = view().getTagList("rows", 10)
                .getCompoundTagAt(rowIndex(id))
                .getLong("targetId");
            container.sync.request(DELETE_SOURCE, target, "", 0);
            return;
        }
        if (view().getBoolean("sourcesPage") && type(id) == OPEN_DETAILS) {
            long target = view().getTagList("rows", 10)
                .getCompoundTagAt(rowIndex(id))
                .getLong("targetId");
            container.sync.request(SELECT_SOURCE, target, "", 0);
            container.clientView()
                .navigate(BACK, false);
            return;
        }
        if (id == APPLY_FILTER || id == CYCLE_FILTER) {
            applyFilters(true, id == CYCLE_FILTER);
            return;
        }
        super.sendAction(id);
    }

    private void applyFilters(boolean reportInvalid, boolean cycle) {
        int state = container.display.getInteger("filterState");
        if (cycle) state = (state + 1) % 5;
        if (!container.clientView()
            .filter(searchText, dimensionText, state) && reportInvalid) {
            // #tr gtneverreset.wireless.gui.filter_invalid
            // # Invalid filter: enter an integer dimension ID or leave it blank.
            // # zh_CN 筛选无效：维度应填写整数或留空。
            mc.thePlayer.addChatMessage(new ChatComponentTranslation("gtneverreset.wireless.gui.filter_invalid"));
        }
    }

    @Override
    void drawHeader(NBTTagCompound data, int textColor) {
        if (data.getBoolean("sourcesPage")) {
            fontRendererObj.drawString(tr("sources_title"), 10, 11, textColor);
            fontRendererObj
                .drawString(fontRendererObj.trimStringToWidth(tr("sources_hint"), xSize - 20), 10, 29, textColor);
            fontRendererObj
                .drawString(fontRendererObj.trimStringToWidth(tr("sources_auto_hint"), xSize - 20), 10, 47, textColor);
            return;
        }
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(tr("title"), xSize - 140), 10, 11, textColor);
        String autoLabel = fontRendererObj.trimStringToWidth(tr(data.getBoolean("auto") ? "auto_on" : "auto_off"), 94);
        fontRendererObj.drawString(
            autoLabel,
            xSize - 32 - fontRendererObj.getStringWidth(autoLabel),
            10 + (16 - fontRendererObj.FONT_HEIGHT) / 2,
            textColor);
        String sourceLabel = data.getString("sourceLabel");
        if (!data.getString("sourceName")
            .isEmpty()) sourceLabel = data.getString("sourceName") + " (" + sourceLabel + ")";
        String source = "> " + (data.getBoolean("bound") ? tr("source", sourceLabel) : tr("unbound"));
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(source, xSize - 20), 10, 25, textColor);
        boolean budgetKnown = data.getBoolean("sourceLoaded") && data.getBoolean("channels")
            && data.getBoolean("stable")
            && data.getInteger("used") >= 0
            && data.getInteger("capacity") > 0;
        int budgetColor = !budgetKnown ? textColor
            : data.getInteger("used") >= data.getInteger("capacity") ? badColor() : goodColor();
        fontRendererObj
            .drawString(fontRendererObj.trimStringToWidth(budgetText(data), xSize - 116), 10, 36, budgetColor);
        ratioBar(
            xSize - 96,
            37,
            84,
            6,
            budgetKnown ? data.getInteger("used") : -1,
            data.getInteger("capacity"),
            budgetColor,
            ColorUtils.craftingTreeTask.getColor());
        fontRendererObj.drawString("D", xSize - 182, 64, textColor);
    }

    @Override
    void drawFooter(NBTTagCompound data, int textColor) {
        if (data.getBoolean("sourcesPage")) return;
        if (!data.getBoolean("group")) {
            var layout = new Layout(xSize, ySize, visibleRows);
            fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(
                    tr(data.getBoolean("filtered") ? "matched" : "loaded", data.getInteger("total")),
                    layout.visualisationX() - layout.footerInfoX() - 4),
                layout.footerInfoX(),
                ySize - 20,
                textColor);
        }
    }

    @SideOnly(Side.CLIENT)
    private static class WirelessCardGroupPanel extends WirelessCardPanel {

        private boolean closing;

        private WirelessCardGroupPanel(WirelessCardContainer container) {
            super("wireless_card_group", container);
            child(ButtonWidget.panelCloseButton());
            addRowButtons(OPEN_DETAILS);
        }

        @Override
        boolean isDetailView() {
            return false;
        }

        @Override
        boolean isGroupView() {
            return true;
        }

        @Override
        public boolean isDraggable() {
            return true;
        }

        @Override
        public int rowTop() {
            return 60;
        }

        @Override
        public NBTTagCompound view() {
            NBTTagCompound data = container.clientView()
                .groupDisplay();
            return data.getBoolean("group") ? data : snapshot;
        }

        @Override
        public boolean layoutReady() {
            return !closing && container.clientView()
                .groupDisplay()
                .getBoolean("group") && super.layoutReady();
        }

        @Override
        public void onOpen(ModularScreen screen) {
            closing = false;
            resizeLayout(screen.getScreenArea().width, screen.getScreenArea().height);
            super.onOpen(screen);
        }

        @Override
        public void closeIfOpen() {
            if (!closing) {
                closing = true;
                container.clientView()
                    .closeGroup();
            }
            super.closeIfOpen();
        }

        @Override
        public boolean onKeyPressed(char character, int key) {
            var focused = getContext().getFocusedWidget()
                .getElement();
            if (focused != null && focused.getPanel() != this) return false;
            if (key == Keyboard.KEY_ESCAPE) {
                closeIfOpen();
                return true;
            }
            return isBelowMouse() && super.onKeyPressed(character, key);
        }

        @Override
        void confirm() {}

        @Override
        void resizePanel(Layout layout) {
            xSize = layout.width() - 24;
            ySize = layout.height() - 22;
        }

        @Override
        void layoutButton(WirelessCardButton button, Layout layout) {}

        @Override
        boolean updatePanel() {
            if (closing || !container.clientView()
                .groupDisplay()
                .getBoolean("group")) {
                super.closeIfOpen();
                return false;
            }
            return true;
        }

        @Override
        void drawHeader(NBTTagCompound data, int textColor) {
            ItemStack icon = ItemStack.loadItemStackFromNBT(data.getCompoundTag("groupIcon"));
            String title = icon == null ? tr("title") : icon.getDisplayName();
            if (data.getBoolean("sharedGroup")) title = tr("shared", title);
            if (data.getBoolean("typeGroup")) title = GuiText.Types.getLocal() + " > " + title;
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(title, xSize - 40), 10, 11, textColor);
            fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(data.getString("groupPositionLabel"), xSize - 20),
                10,
                29,
                textColor);
        }
    }

    @SideOnly(Side.CLIENT)
    private static class WirelessCardDetailsPanel extends WirelessCardPanel {

        private final IPanelHandler renamePanel;
        private boolean closing;

        private WirelessCardDetailsPanel(WirelessCardContainer container) {
            super("wireless_card_details", container);
            child(ButtonWidget.panelCloseButton());
            addButton(RENAME);
            renamePanel = IPanelHandler.simple(this, (parent, player) -> new WirelessCardRenameDialog(this), true);
        }

        @Override
        boolean isDetailView() {
            return true;
        }

        @Override
        public int rowTop() {
            return 60;
        }

        @Override
        public boolean disablePanelsBelow() {
            return true;
        }

        @Override
        public boolean layoutReady() {
            return !closing && super.layoutReady();
        }

        @Override
        void confirm() {
            action(RENAME);
        }

        @Override
        public void onOpen(ModularScreen screen) {
            closing = false;
            resizeLayout(screen.getScreenArea().width, screen.getScreenArea().height);
            super.onOpen(screen);
            getContext().removeFocus();
        }

        @Override
        public void closeIfOpen() {
            if (!closing) {
                closing = true;
                if (container.display.getBoolean("detail")) container.sync.request(CLOSE_DETAILS, 0, "", 0);
            }
            super.closeIfOpen();
        }

        @Override
        public boolean onKeyPressed(char character, int key) {
            if (key == Keyboard.KEY_ESCAPE) {
                closeIfOpen();
                return true;
            }
            return super.onKeyPressed(character, key);
        }

        @Override
        void resizePanel(Layout layout) {
            xSize = Math.max(320, layout.width() - 24);
            ySize = layout.height() - 22;
        }

        @Override
        void layoutButton(WirelessCardButton button, Layout layout) {
            if (button.id == RENAME) button.widget.pos(xSize - 76, 24)
                .size(64, 20);
        }

        @Override
        boolean updatePanel() {
            if (closing || !container.display.getBoolean("detail")) {
                closeIfOpen();
                return false;
            }
            return true;
        }

        @Override
        public void action(int id) {
            if (id == BACK) {
                closeIfOpen();
                return;
            }
            if (id == RENAME) {
                if (layoutReady() && container.display.getBoolean("renameable")) renamePanel.openPanel();
                return;
            }
            super.action(id);
        }

        @Override
        void drawHeader(NBTTagCompound data, int textColor) {
            fontRendererObj
                .drawString(fontRendererObj.trimStringToWidth(tr("detail_title"), xSize - 40), 10, 11, textColor);
            fontRendererObj
                .drawString(fontRendererObj.trimStringToWidth(data.getString("name"), xSize - 94), 10, 29, textColor);
        }

        @Override
        void drawRowMetrics(NBTTagCompound data, NBTTagCompound row, int y) {
            fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(entranceUsage(data, row), contentWidth()),
                CONTENT_X,
                y + 13,
                stateColor(row));
        }
    }

    @SideOnly(Side.CLIENT)
    public static class WirelessCardRenameDialog extends Dialog<Void> {

        public final TextFieldWidget name;
        private final WirelessCardPanel owner;
        private final LongSupplier sourceTarget;
        private final WirelessCardContainer container;
        private final Minecraft mc = Minecraft.getMinecraft();
        private String target;
        private long nameRevision;
        private boolean finished;
        private long sourceId;
        private String originalName;

        WirelessCardRenameDialog(WirelessCardDetailsPanel owner) {
            this(owner, null);
        }

        WirelessCardRenameDialog(WirelessCardPanel owner, LongSupplier sourceTarget) {
            super("wireless_card_rename");
            this.owner = owner;
            this.sourceTarget = sourceTarget;
            this.container = owner.container;
            this.name = owner.textField(32);
            size(260, 96);
            background(owner.getBackground());
            child(ButtonWidget.panelCloseButton());
            child(
                IKey.str(GuiText.Renamer.getLocal())
                    .asWidget()
                    .color(ColorUtils.guiTextColorGray.getColor())
                    .pos(10, 10)
                    .size(220, 18));
            child(
                name.pos(10, 34)
                    .size(240, 16));
            child(dialogButton("gui.cancel", this::closeIfOpen).pos(10, 64));
            child(dialogButton("gui.done", this::submit).pos(134, 64));
        }

        private ButtonWidget<?> dialogButton(String key, Runnable action) {
            return new ButtonWidget<>().size(116, 20)
                .background(WirelessCardButton.BUTTON_TEXTURES[1])
                .hoverBackground(WirelessCardButton.BUTTON_TEXTURES[2])
                .overlay(
                    IKey.lang(key)
                        .color(0xffe0e0e0)
                        .shadow(true))
                .onMousePressed(button -> {
                    if (button != 0 || finished) return false;
                    action.run();
                    return true;
                });
        }

        @Override
        public void onOpen(ModularScreen screen) {
            NBTTagCompound data = container.display;
            target = data.getString("detailTarget");
            nameRevision = data.getLong("nameRevision");
            finished = false;
            String text = data.getBoolean("multipleNames") ? "" : data.getString("name");
            if (sourceTarget != null) {
                sourceId = sourceTarget.getAsLong();
                NBTTagCompound entry = sourceEntry();
                target = entry.getString("sourceKey");
                originalName = entry.getString("name");
                text = originalName;
            }
            name.getStringValue()
                .setStringValue(text);
            name.setText(text);
            super.onOpen(screen);
            getContext().focus(name);
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (!isOpen() || isClosing()) return;
            if (finished) closeIfOpen();
            else validateTarget();
        }

        private boolean validateTarget() {
            NBTTagCompound data = container.display;
            if (sourceTarget != null) {
                NBTTagCompound entry = sourceEntry();
                if (data.getBoolean("sourcesPage") && !target.isEmpty()
                    && target.equals(entry.getString("sourceKey"))
                    && originalName.equals(entry.getString("name"))) return true;
                closeIfOpen();
                return false;
            }
            if (data.getBoolean("detail") && data.getBoolean("renameable")
                && target.equals(data.getString("detailTarget"))
                && nameRevision == data.getLong("nameRevision")) return true;
            closeIfOpen();
            // #tr gtneverreset.wireless.gui.stale
            // # Physical cluster changed. Retry after refreshing.
            // # zh_CN 物理簇已变化，请在刷新后重新操作。
            mc.thePlayer.addChatMessage(new ChatComponentTranslation("gtneverreset.wireless.gui.stale"));
            return false;
        }

        private NBTTagCompound sourceEntry() {
            var sources = container.display.getTagList("sources", 10);
            for (int i = 0; i < sources.tagCount(); i++) {
                NBTTagCompound entry = sources.getCompoundTagAt(i);
                if (entry.getLong("targetId") == sourceId) return entry;
            }
            return new NBTTagCompound();
        }

        private void submit() {
            if (finished || !owner.layoutReady() || !validateTarget()) return;
            if (sourceTarget != null) container.sync.request(RENAME_SOURCE, sourceId, name.getText(), 0);
            else container.sync.request(RENAME, container.display.getLong("detailId"), name.getText(), nameRevision);
            closeIfOpen();
        }

        @Override
        public boolean onKeyPressed(char character, int key) {
            if (finished) return true;
            if (key == Keyboard.KEY_ESCAPE) {
                closeIfOpen();
                return true;
            }
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                submit();
                return true;
            }
            return super.onKeyPressed(character, key);
        }

        @Override
        public void closeIfOpen() {
            // Retry closing after the opening animation, without allowing another submission.
            finished = true;
            super.closeIfOpen();
        }

        @Override
        public void onClose() {
            if (name.isFocused()) getContext().removeFocus();
            super.onClose();
        }
    }

    @SideOnly(Side.CLIENT)
    public static class WirelessCardButton {

        public static final UITexture[] BUTTON_TEXTURES = { buttonTexture(0), buttonTexture(1), buttonTexture(2) };
        private static final UITexture TOGGLE_BACKGROUND = toggleTexture(255), TOGGLE_ON = toggleTexture(129),
            TOGGLE_OFF = toggleTexture(128);
        private static final IDrawable[] TINTED_BUTTONS = { tinted(BUTTON_TEXTURES[0]), tinted(BUTTON_TEXTURES[1]),
            tinted(BUTTON_TEXTURES[2]) };
        public final int id;
        public final Widget<?> widget;
        private final WirelessCardPanel panel;
        private final FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRenderer;
        private boolean available, on;
        private String label = "";

        public WirelessCardButton(int id, WirelessCardPanel panel) {
            this.id = id;
            this.panel = panel;
            if (id == TOGGLE_AUTO || type(id) == TOGGLE_PAUSE) {
                widget = new ToggleButton() {

                    @Override
                    public boolean canBeSeen(IViewportStack stack) {
                        return isEnabled() && super.canBeSeen(stack);
                    }

                    @Override
                    public @NotNull Result onMousePressed(int button) {
                        return button == 0 && available ? super.onMousePressed(button) : Result.ACCEPT;
                    }
                }.value(new BoolValue.Dynamic(() -> on, ignored -> panel.action(id)))
                    .background(false, tinted(TOGGLE_BACKGROUND))
                    .background(true, tinted(TOGGLE_BACKGROUND))
                    .hoverBackground(false, tinted(TOGGLE_BACKGROUND))
                    .hoverBackground(true, tinted(TOGGLE_BACKGROUND))
                    .overlay(false, TOGGLE_OFF)
                    .overlay(true, TOGGLE_ON);
            } else if (id == CYCLE_FILTER) {
                widget = new CycleButtonWidget() {

                    @Override
                    public boolean canBeSeen(IViewportStack stack) {
                        return isEnabled() && super.canBeSeen(stack);
                    }

                    @Override
                    public @NotNull Result onMousePressed(int button) {
                        return button == 0 && available ? super.onMousePressed(button) : Result.ACCEPT;
                    }
                }.stateCount(5)
                    .value(
                        new IntValue.Dynamic(
                            () -> panel.view()
                                .getInteger("filterState"),
                            ignored -> panel.action(id)));
                styleTextButton();
            } else {
                widget = new ActionButton().onMousePressed(button -> {
                    if (button != 0 || !available) return false;
                    panel.action(id);
                    return true;
                });
                if (id == OPEN_SOURCES) widget.background(IDrawable.EMPTY)
                    .hoverBackground(IDrawable.EMPTY);
                else styleTextButton();
            }
            if (type(id) == OPEN_DETAILS || type(id) == RENAME_SOURCE) widget.tooltipBuilder(tooltip -> {
                NBTTagCompound row = panel.view()
                    .getTagList("rows", 10)
                    .getCompoundTagAt(rowIndex(id));
                String name = WirelessCardPanel
                    .rowName(row, ItemStack.loadItemStackFromNBT(row.getCompoundTag("icon")));
                String targetName = row.getString("targetName");
                tooltip.addLine(targetName.isEmpty() || targetName.equals(name) ? name : name + " - " + targetName);
                tooltip.addLine(row.getString("positionLabel"));
                if (row.getBoolean("group")) tooltip.addLine(tr("loaded", row.getInteger("clusters")));
                if (panel.view()
                    .getBoolean("sourcesPage"))
                    tooltip.addLine(tr(row.getBoolean("loaded") ? "source_loaded" : "source_unloaded"));
            })
                .tooltipAutoUpdate(true);
            if (id == OPEN_SOURCES) widget.tooltipBuilder(tooltip -> tooltip.addLine(tr("sources_open")));
            if (type(id) == DELETE_SOURCE) widget.tooltipBuilder(tooltip -> tooltip.addLine(tr("source_delete_hint")));
            if (id == TOGGLE_GROUPING) widget
                .tooltipBuilder(
                    tooltip -> tooltip.addLine(
                        GuiText.Types.getLocal() + ": "
                            + StatCollector.translateToLocal(
                                panel.view()
                                    .getBoolean("collapseTypes") ? "options.on" : "options.off")))
                .tooltipAutoUpdate(true);
        }

        private static class ActionButton extends ButtonWidget<ActionButton> {

            @Override
            public boolean canBeSeen(IViewportStack stack) {
                return isEnabled() && super.canBeSeen(stack);
            }
        }

        private void styleTextButton() {
            IDrawable background = (context, x, y, width, height, theme) -> {
                if (type(id) == OPEN_DETAILS && !panel.view()
                    .getBoolean("sourcesPage")) return;
                TINTED_BUTTONS[!available ? 0 : widget.isHovering() ? 2 : 1].draw(context, x, y, width, height, theme);
            };
            widget.background(background)
                .hoverBackground(background)
                .overlay(
                    IKey.dynamic(
                        () -> fontRendererObj.trimStringToWidth(label, Math.max(0, widget.getArea().width - 4)))
                        .color(() -> !available ? 0xffa0a0a0 : widget.isHovering() ? 0xffffffa0 : 0xffe0e0e0)
                        .shadow(true));
        }

        private static IDrawable tinted(UITexture texture) {
            return (context, x, y, width, height, theme) -> {
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                try {
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    ScreenColor.setGuiColor();
                    texture.draw(x, y, width, height);
                } finally {
                    GL11.glPopAttrib();
                }
            };
        }

        public void update(NBTTagCompound data, boolean ready, int visibleRows) {
            NBTTagList rows = data.getTagList("rows", 10);
            int rowCount = Math.min(visibleRows, rows.tagCount());
            int actionType = type(id);
            if (actionType == OPEN_DETAILS) {
                int y = panel.rowTop() + rowIndex(id) * WirelessCardPanel.ROW_HEIGHT;
                if (data.getBoolean("sourcesPage")) widget.pos(panel.xSize - 116, y + 2)
                    .size(60, 20);
                else widget.pos(10, y)
                    .size(panel.rowStateX() - 10, WirelessCardPanel.ROW_HEIGHT);
            }
            boolean visible = true;
            available = ready;
            if (isRowAction(id)) {
                int index = rowIndex(id);
                if (index >= rowCount) {
                    widget.setEnabled(false);
                    return;
                }
                NBTTagCompound row = rows.getCompoundTagAt(index);
                if (actionType == TOGGLE_PAUSE) on = !row.getBoolean("paused");
                if (actionType == TOGGLE_PAUSE || actionType == HIGHLIGHT || actionType == TELEPORT)
                    visible = !row.getBoolean("group");
                if (actionType == COPY_POSITION) visible = !row.getBoolean("category");
                if (actionType == OPEN_DETAILS) available &= !row.getBoolean("leaf");
                if (actionType == OPEN_DETAILS && data.getBoolean("sourcesPage"))
                    available &= !row.getBoolean("selected");
            }
            if (id == TOGGLE_AUTO) on = data.getBoolean("auto");
            if (id == BACK) visible = data.getBoolean("detail") || data.getBoolean("group");
            if (id == RENAME) {
                visible = data.getBoolean("detail");
                available &= data.getBoolean("renameable");
            }
            if (id == CYCLE_FILTER || id == TOGGLE_AUTO || id == CYCLE_VISUALISATION || id == TOGGLE_GROUPING)
                visible = !data.getBoolean("detail");
            if (id == TOGGLE_GROUPING) visible &= !data.getBoolean("typeGroup");
            if (actionType == OPEN_DETAILS) visible &= !data.getBoolean("detail");
            if (id == OPEN_SOURCES) visible = !data.getBoolean("detail") && !data.getBoolean("sourcesPage");
            if (actionType == RENAME_SOURCE || actionType == DELETE_SOURCE) visible = data.getBoolean("sourcesPage");
            if (data.getBoolean("sourcesPage")) {
                if (isRowAction(id))
                    visible = actionType == OPEN_DETAILS || actionType == RENAME_SOURCE || actionType == DELETE_SOURCE;
                if (id == BACK) visible = true;
                if (id == TOGGLE_AUTO || id == CYCLE_FILTER || id == CYCLE_VISUALISATION || id == TOGGLE_GROUPING)
                    visible = false;
            }
            widget.setEnabled(visible);
            if (!visible) return;
            if (id == PREVIOUS_PAGE) available &= data.getInteger("page") > 0;
            if (id == NEXT_PAGE) available &= data.getInteger("page") + 1 < data.getInteger("pages");
            label = switch (actionType) {
                case PREVIOUS_PAGE -> "<";
                case NEXT_PAGE -> ">";
                case REFRESH -> tr("refresh");
                case BACK -> tr("back_compact");
                case RENAME, RENAME_SOURCE -> StatCollector.translateToLocal("selectWorld.rename");
                case DELETE_SOURCE -> tr("source_delete");
                case CYCLE_FILTER -> tr("filter." + data.getInteger("filterState"));
                case TOGGLE_GROUPING -> GuiText.Types.getLocal() + (data.getBoolean("collapseTypes") ? " +" : " −");
                case TELEPORT -> tr("teleport");
                case HIGHLIGHT -> tr("highlight");
                case COPY_POSITION -> tr("copy");
                case OPEN_DETAILS -> data.getBoolean("sourcesPage") ? tr(
                    rows.getCompoundTagAt(rowIndex(id))
                        .getBoolean("selected") ? "source_selected" : "source_select")
                    : "";
                default -> "";
            };
            if (id == CYCLE_VISUALISATION) {
                int mode = data.getInteger("visualMode");
                String label = StatCollector
                    .translateToLocal(WirelessCardVisualisation.modeKey(mode) + (mode > 0 ? ".short" : ""));
                this.label = fontRendererObj.trimStringToWidth(
                    widget.getArea().width < 102 ? label : tr("visualisation_button", label),
                    widget.getArea().width - 8);
            }
        }

        private static UITexture buttonTexture(int state) {
            return UITexture.builder()
                .location(new ResourceLocation("minecraft", "textures/gui/widgets.png"))
                .imageSize(256, 256)
                .subAreaXYWH(0, 46 + state * 20, 200, 20)
                .adaptable(2)
                .build();
        }

        private static UITexture toggleTexture(int icon) {
            return UITexture.builder()
                .location(ModList.AE2.getResourceLocation("textures/guis/states.png"))
                .imageSize(256, 256)
                .subAreaXYWH(icon % 16 * 16, icon / 16 * 16, 16, 16)
                .build();
        }

        private static String tr(String key, Object... args) {
            return WirelessCardText.tr(key, args);
        }

    }

    @SideOnly(Side.CLIENT)
    public static class WirelessCardClientView {

        private final WirelessCardContainer container;
        private final SearchMatcher matcher = new SearchMatcher();
        private final Map<SortView, List<RowOrder>> orders = new LinkedHashMap<>(16, 0.75f, true);
        private NBTTagCompound source = new NBTTagCompound();
        private List<Row> roots = Collections.emptyList();
        private NBTTagCompound groupDisplay = new NBTTagCompound();
        private String query = "", machineKey = "", typeKey = "";
        private Integer dimension;
        private int state, page, groupPage, detailPage, machineParentPage;
        private int pageSize = Layout.MIN_ROWS;
        private byte[] incoming;
        private long incomingRevision = -1;
        private int received;
        private boolean sourcesPage;

        public WirelessCardClientView(WirelessCardContainer container) {
            this.container = container;
        }

        public void acceptChunk(long revision, int total, int offset, byte[] chunk) {
            if (revision < incomingRevision) return;
            if (offset == 0) {
                incoming = new byte[total];
                incomingRevision = revision;
                received = 0;
            }
            if (incoming == null || revision != incomingRevision || incoming.length != total || offset != received)
                return;
            System.arraycopy(chunk, 0, incoming, offset, chunk.length);
            received += chunk.length;
            if (received != total) return;
            byte[] complete = incoming;
            incoming = null;
            try {
                accept(CompressedStreamTools.readCompressed(new ByteArrayInputStream(complete)));
            } catch (IOException failure) {
                GTNeverReset.LOG.warn("Cannot decode wireless card view", failure);
            }
        }

        private void accept(NBTTagCompound data) {
            if (!source.getString("selectedSource")
                .equals(data.getString("selectedSource"))) {
                closeGroup();
                page = detailPage = 0;
            }
            if (data.getBoolean("detail") && (!source.getBoolean("detail") || !data.getString("detailTarget")
                .equals(source.getString("detailTarget")))) detailPage = 0;
            if (source.hasKey("session") && data.getBoolean("collapseTypes") != source.getBoolean("collapseTypes"))
                page = 0;
            localizeDimensions(data);
            source = data;
            List<Row> next = new ArrayList<>();
            NBTTagList rows = data.getTagList("rows", 10);
            for (int i = 0; i < rows.tagCount(); i++) {
                NBTTagCompound row = rows.getCompoundTagAt(i);
                String prefix = row.getBoolean("group") || !row.getString("typeKey")
                    .isEmpty() ? iconName(row, "icon") : "";
                next.add(readRow(row, prefix, row.getString("position")));
            }
            roots = Collections.unmodifiableList(new ArrayList<>(next));
            if (data.hasKey("highlight", 9)) {
                WirelessWorldHighlight.show(Minecraft.getMinecraft(), data.getTagList("highlight", 10));
                data.removeTag("highlight");
            }
            rebuild();
        }

        private Row readRow(NBTTagCompound data, String prefix, String position) {
            localizePosition(data, "position", source.getCompoundTag("dimensions"));
            localizeInterfaceNames(data);
            List<Row> children = new ArrayList<>();
            NBTTagList entries = data.getTagList("children", 10);
            for (int i = 0; i < entries.tagCount(); i++)
                children.add(readRow(entries.getCompoundTagAt(i), prefix, position));
            StringBuilder names = new StringBuilder(prefix);
            NBTTagList customNames = data.getTagList("customNames", 8);
            for (int i = 0; i < customNames.tagCount(); i++) names.append(' ')
                .append(customNames.getStringTagAt(i));
            names.append(' ')
                .append(iconName(data, "searchIcon"));
            NBTTagList labels = data.getTagList("searchLabels", 10);
            for (int i = 0; i < labels.tagCount(); i++) {
                NBTTagCompound label = labels.getCompoundTagAt(i);
                names.append(' ')
                    .append(label.getString("serverName"))
                    .append(' ')
                    .append(
                        InterfaceNameLocalization.localizeName(
                            label.getString("rawName"),
                            label.getString("suffix"),
                            ItemStack.loadItemStackFromNBT(label.getCompoundTag("icon"))));
            }
            NBTTagCompound display = copy(data);
            display.removeTag("children");
            display.removeTag("searchLabels");
            return new Row(
                display,
                Collections.unmodifiableList(new ArrayList<>(children)),
                names.toString()
                    .toLowerCase(Locale.ROOT),
                (position + " " + data.getString("position")).toLowerCase(Locale.ROOT));
        }

        private static String iconName(NBTTagCompound data, String key) {
            ItemStack icon = ItemStack.loadItemStackFromNBT(data.getCompoundTag(key));
            return data.getString(key.equals("searchIcon") ? "serverDeviceName" : "serverIconName") + " "
                + (icon == null ? "" : icon.getDisplayName());
        }

        public boolean filter(String text, String dimensionText, int state) {
            if (text.length() > 64 || dimensionText.length() > 12 || state < 0 || state > 4) return false;
            Integer nextDimension;
            try {
                nextDimension = dimensionText.trim()
                    .isEmpty() ? null : Integer.valueOf(dimensionText.trim());
            } catch (NumberFormatException incomplete) {
                return false;
            }
            String nextQuery = text.trim()
                .toLowerCase(Locale.ROOT);
            if (query.equals(nextQuery) && Objects.equals(dimension, nextDimension) && this.state == state) return true;
            query = nextQuery;
            dimension = nextDimension;
            this.state = state;
            page = groupPage = machineParentPage = 0;
            rebuild();
            return true;
        }

        public NBTTagCompound groupDisplay() {
            return groupDisplay;
        }

        public void closeGroup() {
            machineKey = "";
            typeKey = "";
            groupPage = machineParentPage = 0;
            groupDisplay = new NBTTagCompound();
        }

        public void resize(int rows) {
            if (!Layout.validRows(rows) || rows == pageSize) return;
            page = page * pageSize / rows;
            detailPage = detailPage * pageSize / rows;
            groupPage = groupPage * pageSize / rows;
            machineParentPage = machineParentPage * pageSize / rows;
            pageSize = rows;
            rebuild();
        }

        public boolean navigate(int action, boolean groupWindow) {
            if (action == OPEN_SOURCES) {
                closeGroup();
                sourcesPage = true;
                page = 0;
                rebuild();
                return true;
            }
            if (sourcesPage && action == BACK) {
                sourcesPage = false;
                page = 0;
                rebuild();
                return true;
            }
            boolean detail = source.getBoolean("detail");
            if (groupWindow && (detail || !groupDisplay.getBoolean("group"))) return true;
            if (action == PREVIOUS_PAGE || action == NEXT_PAGE) {
                int change = action == NEXT_PAGE ? 1 : -1;
                if (detail) detailPage += change;
                else if (groupWindow) groupPage += change;
                else page += change;
            } else if (action == BACK && !detail) {
                if (groupWindow && !machineKey.isEmpty() && !typeKey.isEmpty()) {
                    machineKey = "";
                    groupPage = machineParentPage;
                } else if (groupWindow) closeGroup();
            } else if (type(action) == OPEN_DETAILS && !detail) {
                NBTTagList rows = (groupWindow ? groupDisplay : container.display).getTagList("rows", 10);
                int index = rowIndex(action);
                if (index < 0 || index >= rows.tagCount()) return true;
                NBTTagCompound row = rows.getCompoundTagAt(index);
                if (row.getBoolean("leaf")) return true;
                if (!row.getBoolean("group")) return false;
                if (!groupWindow) closeGroup();
                if (row.getBoolean("category")) {
                    typeKey = row.getString("typeKey");
                } else {
                    machineParentPage = groupPage;
                    machineKey = row.getString("key");
                }
                groupPage = 0;
            } else return false;
            rebuild();
            return true;
        }

        public void rebuild() {
            if (!source.hasKey("session")) return;
            if (source.getBoolean("detail")) {
                container.display = buildListing(null, "", detailPage);
                detailPage = container.display.getInteger("page");
                return;
            }
            if (!typeKey.isEmpty() && roots.stream()
                .noneMatch(
                    row -> typeKey.equals(
                        row.data()
                            .getString("typeKey"))))
                closeGroup();
            Row machine = machineKey.isEmpty() ? null
                : roots.stream()
                    .filter(
                        row -> row.data()
                            .getBoolean("group")
                            && machineKey.equals(
                                row.data()
                                    .getString("key"))
                            && (typeKey.isEmpty() || typeKey.equals(
                                row.data()
                                    .getString("typeKey"))))
                    .findFirst()
                    .orElse(null);
            if (!machineKey.isEmpty() && machine == null) {
                machineKey = "";
                groupPage = machineParentPage;
                if (typeKey.isEmpty()) closeGroup();
            }
            container.display = buildListing(null, "", page);
            page = container.display.getInteger("page");
            if (machine != null || !typeKey.isEmpty()) {
                groupDisplay = buildListing(machine, typeKey, groupPage);
                groupPage = groupDisplay.getInteger("page");
            }
        }

        private NBTTagCompound buildListing(Row machine, String typeKey, int requestedPage) {
            if (sourcesPage) return buildSources(requestedPage);
            boolean detail = source.getBoolean("detail");
            NBTTagCompound data = copy(source);
            List<Row> rows = new ArrayList<>();
            if (detail) {
                rows.addAll(roots);
            } else {
                for (Row row : machine == null ? roots : Collections.singletonList(machine)) {
                    if (!typeKey.isEmpty() && !typeKey.equals(
                        row.data()
                            .getString("typeKey")))
                        continue;
                    Row matched = filtered(row);
                    if (matched == null) continue;
                    if (machine == null) rows.add(matched);
                    else rows.addAll(matched.children());
                }
                if (!data.getBoolean("initializing")) {
                    NBTTagCompound statistics = aggregate(new NBTTagCompound(), rows);
                    for (String key : Arrays.asList("devices", "online", "missing", "stable"))
                        data.setTag(key, statistics.getTag(key));
                    data.setInteger(
                        "total",
                        rows.stream()
                            .mapToInt(
                                row -> row.data()
                                    .getInteger("clusters"))
                            .sum());
                }
                if (data.getBoolean("collapseTypes") && typeKey.isEmpty() && machine == null) rows = collapse(rows);
                var sortView = new SortView(
                    machine == null ? ""
                        : machine.data()
                            .getString("key"),
                    typeKey,
                    data.getBoolean("collapseTypes"),
                    query,
                    dimension,
                    state);
                orders.put(sortView, sort(rows, orders.getOrDefault(sortView, Collections.emptyList())));
                if (orders.size() > 16) orders.remove(
                    orders.keySet()
                        .iterator()
                        .next());
                data.setBoolean("group", machine != null || !typeKey.isEmpty());
                data.setBoolean("typeGroup", !typeKey.isEmpty());
                if (machine != null) {
                    data.setTag(
                        "groupIcon",
                        machine.data()
                            .getCompoundTag("icon"));
                    data.setString(
                        "groupPosition",
                        machine.data()
                            .getString("position"));
                } else if (!typeKey.isEmpty()) {
                    Row first = roots.stream()
                        .filter(
                            row -> typeKey.equals(
                                row.data()
                                    .getString("typeKey")))
                        .findFirst()
                        .orElseThrow(NoSuchElementException::new);
                    data.setTag(
                        "groupIcon",
                        first.data()
                            .getCompoundTag("typeIcon"));
                    data.setString(
                        "groupPosition",
                        "D" + first.data()
                            .getInteger("dimension"));
                }
                data.setBoolean(
                    "sharedGroup",
                    !typeKey.isEmpty() && roots.stream()
                        .anyMatch(
                            row -> typeKey.equals(
                                row.data()
                                    .getString("typeKey"))
                                && row.data()
                                    .getBoolean("shared")));
                localizePosition(data, "groupPosition", data.getCompoundTag("dimensions"));
            }
            int pages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
            int currentPage = Math.max(0, Math.min(requestedPage, pages - 1));
            data.setInteger("page", currentPage);
            data.setInteger("pageSize", pageSize);
            data.setInteger("pages", pages);
            data.setInteger("filterState", state);
            data.setBoolean("filtered", !query.isEmpty() || dimension != null || state != 0);
            NBTTagList entries = new NBTTagList();
            for (Row row : rows.subList(currentPage * pageSize, Math.min(rows.size(), (currentPage + 1) * pageSize))) {
                if (row.data()
                    .getBoolean("category"))
                    localizePosition(row.data(), "position", data.getCompoundTag("dimensions"));
                entries.appendTag(row.data());
            }
            data.setTag("rows", entries);
            return data;
        }

        private NBTTagCompound buildSources(int requestedPage) {
            NBTTagCompound data = copy(source);
            NBTTagList saved = source.getTagList("sources", 10);
            int pages = Math.max(1, (saved.tagCount() + pageSize - 1) / pageSize);
            int currentPage = Math.max(0, Math.min(requestedPage, pages - 1));
            data.setBoolean("sourcesPage", true);
            data.setInteger("page", currentPage);
            data.setInteger("pages", pages);
            data.setInteger("pageSize", pageSize);
            NBTTagList entries = new NBTTagList();
            for (int i = currentPage * pageSize; i < Math.min(saved.tagCount(), (currentPage + 1) * pageSize); i++) {
                NBTTagCompound row = copy(saved.getCompoundTagAt(i));
                localizePosition(row, "position", source.getCompoundTag("dimensions"));
                entries.appendTag(row);
            }
            data.setTag("rows", entries);
            return data;
        }

        private Row filtered(Row row) {
            if (!row.children()
                .isEmpty()) {
                List<Row> children = row.children()
                    .stream()
                    .filter(this::matches)
                    .collect(Collectors.toList());
                return children.isEmpty() ? null
                    : new Row(aggregate(row.data(), children), children, row.names(), row.positions());
            }
            return matches(row) ? row : null;
        }

        private boolean matches(Row row) {
            NBTTagCompound data = row.data();
            if (dimension != null && dimension != data.getInteger("dimension")) return false;
            boolean stateMatches = switch (state) {
                case 1 -> data.getBoolean("paused");
                case 2 -> data.getBoolean("conflicted");
                case 3 -> !data.getBoolean("paused") && data.getString("state")
                    .equals("active");
                case 4 -> !data.getBoolean("paused") && data.getString("state")
                    .equals("waiting");
                default -> true;
            };
            return stateMatches && (query.isEmpty() || (row.names() + " " + row.positions()).contains(query)
                || matcher.contains(row.names(), query));
        }

        private static List<Row> collapse(List<Row> rows) {
            List<Row> result = new ArrayList<>();
            Map<String, List<Row>> types = new LinkedHashMap<>();
            for (Row row : rows) {
                String key = row.data()
                    .getString("typeKey");
                if (key.isEmpty()) result.add(row);
                else types.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(row);
            }
            types.forEach((key, children) -> {
                children.sort(
                    Comparator.comparing(
                        row -> row.data()
                            .getString("sortKey")));
                if (children.size() == 1) result.add(children.get(0));
                else {
                    NBTTagCompound category = aggregate(
                        children.get(0)
                            .data(),
                        children);
                    category.setBoolean("group", true);
                    category.setBoolean("category", true);
                    category.setString("key", "type:" + key);
                    category.setString("sortKey", key);
                    category.setString("name", "");
                    category.setTag("icon", category.getCompoundTag("typeIcon"));
                    category.removeTag("targetName");
                    category.setString("position", "D" + category.getInteger("dimension"));
                    category.setInteger("items", children.size());
                    category.removeTag("targetId");
                    result.add(new Row(category, Collections.unmodifiableList(new ArrayList<>(children)), "", ""));
                }
            });
            return result;
        }

        private static NBTTagCompound aggregate(NBTTagCompound template, List<Row> rows) {
            NBTTagCompound data = copy(template);
            int devices = 0, online = 0, missing = 0, entrances = 0, clusters = 0;
            boolean stable = true, paused = true, conflict = false, limited = false, active = true;
            for (Row row : rows) {
                NBTTagCompound child = row.data();
                devices += child.getInteger("devices");
                online += child.getInteger("online");
                missing += child.getInteger("missing");
                entrances += child.getInteger("entrances");
                clusters += child.getInteger("clusters");
                stable &= child.getBoolean("stable");
                paused &= child.getBoolean("paused");
                conflict |= child.getBoolean("conflicted");
                limited |= child.getString("state")
                    .equals("limited");
                active &= child.getString("state")
                    .equals("active");
            }
            data.setInteger("devices", devices);
            data.setInteger("online", online);
            data.setInteger("missing", missing);
            data.setInteger("entrances", entrances);
            data.setInteger("clusters", clusters);
            data.setBoolean("stable", stable);
            data.setBoolean("paused", paused);
            data.setBoolean("conflicted", conflict);
            data.setString(
                "state",
                conflict ? "conflict" : limited ? "limited" : paused ? "paused" : active ? "active" : "waiting");
            return data;
        }

        private static List<RowOrder> sort(List<Row> rows, List<RowOrder> previous) {
            rows.sort(
                Comparator.comparing(
                    (Row row) -> row.data()
                        .getBoolean("paused"))
                    .thenComparingInt(
                        row -> row.data()
                            .getBoolean("stable")
                            && row.data()
                                .getInteger("missing") > 0 ? 0 : 1)
                    .thenComparing(
                        Comparator.comparingInt(
                            (Row row) -> row.data()
                                .getInteger("online"))
                            .reversed())
                    .thenComparing(
                        row -> row.data()
                            .getString("sortKey")));
            boolean[] pinned = new boolean[rows.size()];
            if (!previous.isEmpty()) {
                Map<String, Row> remaining = new LinkedHashMap<>();
                for (Row row : rows) remaining.put(
                    row.data()
                        .getString("key"),
                    row);
                Row[] ordered = new Row[rows.size()];
                int lastSlot = ordered.length - 1;
                for (int i = previous.size() - 1; i >= 0; i--) {
                    RowOrder old = previous.get(i);
                    Row row = remaining.get(old.key());
                    if (row == null || !row.data()
                        .getBoolean("paused") || old.paused() && !old.pinned()) continue;
                    int slot = Math.min(i, lastSlot);
                    ordered[slot] = row;
                    pinned[slot] = true;
                    lastSlot = slot - 1;
                    remaining.remove(old.key());
                }
                var moving = remaining.values()
                    .iterator();
                for (int i = 0; i < ordered.length; i++) rows.set(i, ordered[i] == null ? moving.next() : ordered[i]);
            }
            List<RowOrder> order = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                NBTTagCompound row = rows.get(i)
                    .data();
                order.add(new RowOrder(row.getString("key"), row.getBoolean("paused"), pinned[i]));
            }
            return order;
        }

        private static NBTTagCompound copy(NBTTagCompound source) {
            NBTTagCompound copy = new NBTTagCompound();
            for (Object key : source.func_150296_c()) copy.setTag((String) key, source.getTag((String) key));
            return copy;
        }

        private static void localizeDimensions(NBTTagCompound data) {
            NBTTagCompound dimensions = data.getCompoundTag("dimensions");
            for (Object key : dimensions.func_150296_c()) {
                String id = (String) key;
                String name = dimensions.getString(id);
                if (name.equals("Personal World " + id) || name.equals("personalworld")) {
                    // #tr gtneverreset.wireless.gui.personal_world
                    // # Personal World
                    // # zh_CN 私人维度
                    dimensions
                        .setString(id, StatCollector.translateToLocal("gtneverreset.wireless.gui.personal_world"));
                } else if (!name.isEmpty()) dimensions.setString(id, DimensionHelper.getDimLocalizedName(name));
            }
            localizePosition(data, "source", dimensions);
            localizePosition(data, "groupPosition", dimensions);
            NBTTagList rows = data.getTagList("rows", 10);
            for (int i = 0; i < rows.tagCount(); i++)
                localizePosition(rows.getCompoundTagAt(i), "position", dimensions);
        }

        private static void localizePosition(NBTTagCompound data, String key, NBTTagCompound dimensions) {
            String position = data.getString(key);
            int end = position.indexOf(' ');
            String name = position.startsWith("D")
                ? dimensions.getString(position.substring(1, end < 0 ? position.length() : end))
                : "";
            // Keep the numeric form for coordinate copying and client-side filtering.
            data.setString(key + "Label", name.isEmpty() ? position : name + " " + position);
        }

        private static void localizeInterfaceNames(NBTTagCompound row) {
            NBTTagList entries = row.getTagList("interfaceNames", 10);
            if (entries.tagCount() == 0) return;
            List<String> names = new ArrayList<>();
            for (int i = 0; i < entries.tagCount(); i++) {
                NBTTagCompound entry = entries.getCompoundTagAt(i);
                names.add(
                    InterfaceNameLocalization.localizeName(
                        entry.getString("rawName"),
                        entry.getString("suffix"),
                        ItemStack.loadItemStackFromNBT(entry.getCompoundTag("icon"))));
            }
            int count = row.getInteger("interfaceNameCount");
            String targetName = String.join(" / ", names) + (count > entries.tagCount() ? " … [" + count + "]" : "");
            row.setString("targetName", targetName);
            if (row.getString("name")
                .isEmpty()) row.setString("name", targetName);
        }

        @Desugar
        private record Row(NBTTagCompound data, List<Row> children, String names, String positions) {}

        @Desugar
        private record RowOrder(String key, boolean paused, boolean pinned) {}

        @Desugar
        private record SortView(String machine, String type, boolean collapse, String query, Integer dimension,
            int state) {}

        private static class SearchMatcher {

            private Object api;
            private Method contains;

            SearchMatcher() {
                if (!Loader.isModLoaded("nech")) return;
                try {
                    Class<?> type = Class.forName("com.asdflj.nech.API");
                    api = type.getField("INSTANCE")
                        .get(null);
                    contains = type.getMethod("contains", String.class, String.class);
                } catch (ReflectiveOperationException | LinkageError failure) {
                    GTNeverReset.LOG.warn("Cannot initialize optional Rework search", failure);
                }
            }

            boolean contains(String text, String query) {
                if (contains == null) return false;
                try {
                    return (boolean) contains.invoke(api, text, query);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    contains = null;
                    GTNeverReset.LOG.warn("Rework search failed; using literal matching", failure);
                    return false;
                }
            }
        }
    }

    @Desugar
    public record Layout(int width, int height, int rows) {

        public static final int MIN_ROWS = 5;
        public static final int MAX_ROWS = 9;

        public static Layout forScreen(int width, int height) {
            int rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, (height - 8 - 112) / 24));
            return new Layout(Math.max(320, Math.min(440, width - 12)), 112 + rows * 24, rows);
        }

        public int visualisationWidth() {
            return Math.min(120, width - 254);
        }

        public int visualisationX() {
            return width - 80 - visualisationWidth();
        }

        public int footerInfoX() {
            return Math.min(142, visualisationX() - 76);
        }

        public static boolean validRows(int rows) {
            return rows >= MIN_ROWS && rows <= MAX_ROWS;
        }
    }
}

@SideOnly(Side.CLIENT)
abstract class WirelessCardPanel extends ModularPanel {

    public static final int ROW_HEIGHT = 24, CONTENT_X = 34;
    public final Minecraft mc = Minecraft.getMinecraft();
    public final FontRenderer fontRendererObj = mc.fontRenderer;
    public final WirelessCardContainer container;
    public final List<WirelessCardButton> buttons = new ArrayList<>();
    public NBTTagCompound snapshot = new NBTTagCompound();
    public int xSize = 320, ySize = 232, visibleRows = Layout.MIN_ROWS;

    WirelessCardPanel(String name, WirelessCardContainer container) {
        super(name);
        this.container = container;
        background(
            UITexture.builder()
                .location(ModList.AE2.getResourceLocation("textures/guis/wireless_network_manager.png"))
                .imageSize(256, 256)
                .subAreaXYWH(0, 0, 106, 232)
                .adaptable(4)
                .build());
        for (int id : new int[] { PREVIOUS_PAGE, NEXT_PAGE, REFRESH, BACK }) addButton(id);
        addRowButtons(TOGGLE_PAUSE, COPY_POSITION, HIGHLIGHT, TELEPORT);
    }

    abstract boolean isDetailView();

    public abstract int rowTop();

    abstract void resizePanel(Layout layout);

    abstract void layoutButton(WirelessCardButton button, Layout layout);

    abstract boolean updatePanel();

    abstract void drawHeader(NBTTagCompound data, int color);

    abstract void confirm();

    boolean isGroupView() {
        return false;
    }

    boolean editingText() {
        return false;
    }

    void drawFooter(NBTTagCompound data, int color) {}

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    public boolean onKeyPressed(char character, int key) {
        if (WirelessCardInput.visualisationKey(key) && !editingText()) {
            action(CYCLE_VISUALISATION);
            return true;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            confirm();
            return true;
        }
        return super.onKeyPressed(character, key);
    }

    public NBTTagCompound view() {
        // Freeze the overview behind a modal detail panel while only the active view is synchronized.
        return container.display.getBoolean("detail") == isDetailView() ? container.display : snapshot;
    }

    public boolean layoutReady() {
        return container.display.getBoolean("detail") == isDetailView() && container.display.hasKey("session")
            && view().getInteger("pageSize") == visibleRows;
    }

    public void resizeLayout(int width, int height) {
        var layout = Layout.forScreen(width, height);
        visibleRows = layout.rows();
        resizePanel(layout);
        size(xSize, ySize);
        for (var control : buttons) {
            int id = control.id;
            var button = control.widget;
            if (isRowAction(id)) {
                int y = rowTop() + rowIndex(id) * ROW_HEIGHT;
                switch (type(id)) {
                    case TOGGLE_PAUSE -> button.pos(xSize - 28, y + 4)
                        .size(16, 16);
                    case OPEN_DETAILS -> button.pos(10, y)
                        .size(rowStateX() - 10, ROW_HEIGHT);
                    case RENAME_SOURCE -> button.pos(xSize - 180, y + 2)
                        .size(60, 20);
                    case DELETE_SOURCE -> button.pos(xSize - 52, y + 2)
                        .size(40, 20);
                    case COPY_POSITION -> button.pos(xSize - 96, y + 2)
                        .size(30, 20);
                    case HIGHLIGHT -> button.pos(xSize - 62, y + 2)
                        .size(30, 20);
                    case TELEPORT -> button.pos(xSize - 130, y + 2)
                        .size(30, 20);
                }
            } else switch (id) {
                case PREVIOUS_PAGE -> button.pos(10, ySize - 27)
                    .size(18, 20);
                case NEXT_PAGE -> button.pos(76, ySize - 27)
                    .size(18, 20);
                case REFRESH -> button.pos(xSize - 76, ySize - 27)
                    .size(64, 20);
                case BACK -> button.pos(layout.footerInfoX(), ySize - 27)
                    .size(36, 20);
                default -> layoutButton(control, layout);
            }
        }
    }

    public final void addButton(int id) {
        var button = new WirelessCardButton(id, this);
        buttons.add(button);
        child(button.widget);
    }

    public final void addRowButtons(int... actions) {
        for (int row = 0; row < Layout.MAX_ROWS; row++) for (int action : actions) addButton(action + row);
    }

    public void action(int id) {
        if (!layoutReady()) return;
        snapshot = view();
        if (type(id) == COPY_POSITION && rowIndex(id) < visibleRows) {
            var row = view().getTagList("rows", 10)
                .getCompoundTagAt(rowIndex(id));
            GuiScreen.setClipboardString(
                row.getString("position") + (row.getBoolean("group") ? "" : " / " + row.getString("side")));
            // #tr gtneverreset.wireless.gui.copied
            // # Copied dimension, coordinates and part side to the clipboard.
            // # zh_CN 已复制目标维度、坐标和部件方向到剪贴板。
            mc.thePlayer.addChatMessage(new ChatComponentTranslation("gtneverreset.wireless.gui.copied"));
            return;
        }
        sendAction(id);
    }

    public void sendAction(int id) {
        if (container.clientView()
            .navigate(id, isGroupView())) return;
        long target = isRowAction(id) ? view().getTagList("rows", 10)
            .getCompoundTagAt(rowIndex(id))
            .getLong("targetId") : 0;
        container.sync.request(type(id), target, "", 0);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (container.display.getBoolean("detail") == isDetailView()) snapshot = view();
        if (!updatePanel()) return;
        for (var button : buttons) button.update(view(), layoutReady(), visibleRows);
    }

    public static String tr(String key, Object... args) {
        return WirelessCardText.tr(key, args);
    }

    public TextFieldWidget textField(int max) {
        return new TextFieldWidget().background(
            new Rectangle().color(0xff000000),
            new Rectangle().color(0xffa0a0a0)
                .hollow(1))
            .setTooltipOverride(true)
            .value(new StringValue(""))
            .setMaxLength(max)
            .setTextColor(ColorUtils.searchboxText.getColor())
            .padding(2, 0);
    }

    int contentWidth() {
        return xSize - 212;
    }

    void drawRowMetrics(NBTTagCompound data, NBTTagCompound row, int y) {
        drawMetrics(row, CONTENT_X, y + 13, contentWidth(), true);
        int missingColor = row.getBoolean("stable") && row.getInteger("missing") > 0 ? badColor()
            : ColorUtils.craftingTreeTask.getColor();
        ratioBar(
            rowStateX(),
            y + 19,
            36,
            2,
            row.getBoolean("stable") ? row.getInteger("online") : -1,
            row.getInteger("devices"),
            goodColor(),
            missingColor);
    }

    static String entranceUsage(NBTTagCompound data, NBTTagCompound row) {
        if (!row.getBoolean("runtime")) return tr("no_runtime");
        if (!data.getBoolean("channels")) return tr("usage_disabled");
        return row.getInteger("usedChannels") < 0 ? tr("usage_waiting") : tr("usage", row.getInteger("usedChannels"));
    }

    static int goodColor() {
        return ColorUtils.craftConfirmPercent50.getColor();
    }

    static int badColor() {
        return ColorUtils.reshuffleStatusFailed.getColor();
    }

    static int waitingColor() {
        return ColorUtils.craftConfirmPercent25.getColor();
    }

    static int stateColor(NBTTagCompound row) {
        if (row.getBoolean("paused") || row.getBoolean("conflicted")) return badColor();
        if (row.getBoolean("stable") && row.getInteger("missing") > 0) return badColor();
        if (!row.getBoolean("stable") || !row.getString("state")
            .equals("active")) return waitingColor();
        // A live entrance is not proof of power or active devices.
        return row.getInteger("devices") > 0 && row.getInteger("online") == row.getInteger("devices") ? goodColor()
            : ColorUtils.guiTextColorGray.getColor();
    }

    static String budgetText(NBTTagCompound data) {
        return !data.getBoolean("bound") ? tr("bind_hint")
            : !data.getBoolean("sourceLoaded") ? tr("source_waiting")
                : !data.getBoolean("channels") ? tr("unlimited")
                    : tr(
                        "budget_compact",
                        data.getInteger("used") < 0 ? "?" : data.getInteger("used"),
                        data.getInteger("capacity"));
    }

    int textSegment(String text, int x, int y, int end, int color) {
        String fitted = fontRendererObj.trimStringToWidth(text, Math.max(0, end - x));
        fontRendererObj.drawString(fitted, x, y, color);
        return x + fontRendererObj.getStringWidth(fitted);
    }

    void drawMetrics(NBTTagCompound data, int x, int y, int width, boolean compact) {
        int end = x + width;
        String entrances = "  " + tr(
            compact ? "entrances_compact" : "entrances",
            compact ? data.getInteger("entrances")
                : data.getInteger("entrances") + " / " + data.getInteger("entranceLimit"));
        int metricsEnd = compact ? end : Math.max(x, end - fontRendererObj.getStringWidth(entrances));
        int textColor = ColorUtils.guiTextColorGray.getColor();
        int online = data.getInteger("online"), devices = data.getInteger("devices");
        int onlineColor = data.getBoolean("paused") ? badColor()
            : !data.getBoolean("stable") ? waitingColor() : online == devices && devices > 0 ? goodColor() : textColor;
        x = textSegment(
            tr(compact ? "online_compact" : "online_metric", online, devices),
            x,
            y,
            metricsEnd,
            onlineColor);
        int missingColor = !data.getBoolean("stable") ? waitingColor()
            : data.getInteger("missing") > 0 ? badColor() : textColor;
        x = textSegment(
            "  " + tr(
                compact ? "missing_compact" : "missing_metric",
                data.getBoolean("stable") ? data.getInteger("missing") : "?"),
            x,
            y,
            metricsEnd,
            missingColor);
        textSegment(entrances, x, y, end, textColor);
    }

    void ratioBar(int x, int y, int width, int height, int value, int total, int fill, int remainder) {
        drawRect(x, y, x + width, y + height, remainder | 0xFF000000);
        if (value < 0 || total <= 0) return;
        int filled = (int) Math.round(width * Math.max(0.0, Math.min(1.0, (double) value / total)));
        if (filled > 0) drawRect(x, y, x + filled, y + height, fill | 0xFF000000);
    }

    void drawDeviceIcon(ItemStack icon, int x, int y) {
        float brightnessX = OpenGlHelper.lastBrightnessX;
        float brightnessY = OpenGlHelper.lastBrightnessY;
        var itemRender = GuiScreenAccessor.getItemRender();
        float renderZ = itemRender.zLevel;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glColor4f(1, 1, 1, 1);
            // Isolate custom item renderers while retaining MUI's item layer and Forge glint cleanup.
            GuiDraw.drawItem(icon, x, y, 16, 16, getContext().getCurrentDrawingZ());
        } finally {
            itemRender.zLevel = renderZ;
            GL11.glPopAttrib();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        }
    }

    void drawEmptyMessage(NBTTagCompound data, int color) {
        List<String> lines = fontRendererObj.listFormattedStringToWidth(
            tr(
                data.getBoolean("sourcesPage") ? "sources_hint"
                    : data.getBoolean("initializing") ? "initializing"
                        : data.getBoolean("filtered") ? "no_matches" : "empty"),
            Math.min(300, xSize - 48));
        int lineHeight = fontRendererObj.FONT_HEIGHT + 3;
        int y = rowTop() + (visibleRows * ROW_HEIGHT - lines.size() * lineHeight) / 2;
        for (String line : lines) {
            fontRendererObj.drawString(line, (xSize - fontRendererObj.getStringWidth(line)) / 2, y, color);
            y += lineHeight;
        }
    }

    public static String rowName(NBTTagCompound row, ItemStack icon) {
        String name = row.getString("name");
        if (name.isEmpty()) name = icon == null ? "" : icon.getDisplayName();
        return row.getBoolean("shared") ? tr("shared", name) : name;
    }

    public int rowStateX() {
        return xSize - 172;
    }

    void drawRowTitle(NBTTagCompound data, NBTTagCompound row, ItemStack icon, int y) {
        String name = (row.getBoolean("group") ? "> " : "") + rowName(row, icon);
        String position = row.getString("positionLabel");
        String count = row.getBoolean("category") ? " [" + row.getInteger("items") + "]"
            : row.getBoolean("group") ? " [" + row.getInteger("clusters") + "]" : "";
        int width = contentWidth() - fontRendererObj.getStringWidth(count);
        int nameColor = ColorUtils.guiTextColorBlack.getColor();
        if (data.getBoolean("detail") || name.isEmpty()) {
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(position, width), CONTENT_X, y, nameColor);
            return;
        }
        int nameReserve = Math.min(24, fontRendererObj.getStringWidth(name));
        String suffix = " ("
            + fontRendererObj
                .trimStringToWidth(position, Math.max(0, width - nameReserve - fontRendererObj.getStringWidth(" ()")))
            + ")";
        String title = fontRendererObj
            .trimStringToWidth(name, Math.max(0, width - fontRendererObj.getStringWidth(suffix)));
        fontRendererObj.drawString(title, CONTENT_X, y, nameColor);
        fontRendererObj.drawString(
            suffix,
            CONTENT_X + fontRendererObj.getStringWidth(title),
            y,
            ColorUtils.craftingTreeTask.getColor());
        if (!count.isEmpty()) fontRendererObj.drawString(
            count,
            CONTENT_X + fontRendererObj.getStringWidth(title) + fontRendererObj.getStringWidth(suffix),
            y,
            stateColor(row));
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            int count = Math.min(
                visibleRows,
                view().getTagList("rows", 10)
                    .tagCount());
            for (int row = 0; row < count; row++) {
                int y = rowTop() + ROW_HEIGHT - 1 + row * ROW_HEIGHT;
                drawRect(8, y, xSize - 8, y + 1, ColorUtils.craftingDiagnosticTerminalLine.getColor());
            }
            drawForeground();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void drawForeground() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Platform.setupDrawFont();
        NBTTagCompound data = view();
        int textColor = ColorUtils.guiTextColorGray.getColor();
        drawHeader(data, textColor);
        if (!data.getBoolean("sourcesPage")) drawMetrics(data, 10, 47, xSize - 20, false);
        NBTTagList rows = data.getTagList("rows", 10);
        for (int index = 0; index < Math.min(visibleRows, rows.tagCount()); index++) {
            NBTTagCompound row = rows.getCompoundTagAt(index);
            int y = rowTop() + index * ROW_HEIGHT;
            ItemStack icon = ItemStack.loadItemStackFromNBT(row.getCompoundTag("icon"));
            if (icon != null) drawDeviceIcon(icon, 12, y + 4);
            if (data.getBoolean("sourcesPage")) {
                String name = row.getString("name");
                String position = row.getString("positionLabel");
                fontRendererObj.drawString(
                    fontRendererObj.trimStringToWidth(name.isEmpty() ? position : name, xSize - 224),
                    CONTENT_X,
                    y + 2,
                    textColor);
                fontRendererObj
                    .drawString(
                        fontRendererObj.trimStringToWidth(
                            name.isEmpty() ? tr(row.getBoolean("loaded") ? "source_loaded" : "source_unloaded")
                                : position,
                            xSize - 224),
                        CONTENT_X,
                        y + 13,
                        textColor);
                continue;
            }
            drawRowTitle(data, row, icon, y + 2);
            String state = tr("short_state." + row.getString("state"));
            fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(state, 36),
                rowStateX(),
                y + (ROW_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2,
                stateColor(row));
            drawRowMetrics(data, row, y);
        }
        if (rows.tagCount() == 0) drawEmptyMessage(data, textColor);
        String page = fontRendererObj
            .trimStringToWidth((data.getInteger("page") + 1) + "/" + Math.max(1, data.getInteger("pages")), 40);
        fontRendererObj.drawString(page, 52 - fontRendererObj.getStringWidth(page) / 2, ySize - 20, textColor);
        drawFooter(data, textColor);
    }

}
