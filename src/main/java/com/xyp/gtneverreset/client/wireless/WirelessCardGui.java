package com.xyp.gtneverreset.client.wireless;

import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.BACK;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CLOSE_DETAILS;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.COPY_POSITION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CYCLE_VISUALISATION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.DELETE_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.HIGHLIGHT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.NEXT_PAGE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_DETAILS;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_ENTRANCES;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_SOURCES;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.PREVIOUS_PAGE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.REFRESH;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.RENAME;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.RENAME_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.SELECT_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.SET_VISUALISATION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TELEPORT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_AUTO;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_GROUPING;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_PAUSE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.isRowAction;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.rowIndex;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.menu.MenuPanel;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.github.bsideup.jabel.Desugar;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.wireless.WirelessCardGui.Layout;
import com.xyp.gtneverreset.client.wireless.WirelessCardGui.WirelessCardButton;
import com.xyp.gtneverreset.common.gui.container.WirelessCardContainer;
import com.xyp.gtneverreset.common.me.InterfaceNameLocalization;
import com.xyp.gtneverreset.common.wireless.WirelessCardText;
import com.xyp.gtneverreset.common.wireless.WirelessCardVisualisation;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.widget.CycleDropdown;

import appeng.core.localization.GuiText;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gtneioreplugin.util.DimensionHelper;

@SideOnly(Side.CLIENT)
public class WirelessCardGui extends WirelessCardPanel {

    private static final String NAME = "wireless_card", DIMENSION_MENU = NAME + "_dimension",
        VISUALISATION_MENU = NAME + "_visualisation";
    private static final int[] CHIP_STATES = { 0, 3, 4, 5, 1, 2 };
    private static final int DIMENSION_WIDTH = 100, MENU_HEIGHT = 120, ALL_DIMENSIONS = Integer.MIN_VALUE;
    private final List<ButtonWidget<?>> chips = new ArrayList<>();
    private final TextFieldWidget search;
    private final CycleDropdown<Integer> dimension, visualisation;
    private Layout layout = Layout.forScreen(0, 0);
    private NBTTagCompound lastLayoutView;
    private final IPanelHandler detailsPanel, sourceRenamePanel;
    private long renameSourceId;
    private boolean showingDetails;
    private String searchText = "";

    public WirelessCardGui(WirelessCardContainer container) {
        super("wireless_card", container);
        search = textField(64).hintText(tr("search_placeholder"));
        search.autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> searchText, text -> {
                if (searchText.equals(text)) return;
                searchText = text;
                filter(clientView().dimension(), clientView().state());
            }));
        child(search);
        for (int index = 0; index < CHIP_STATES.length; index++) child(chip(index));
        dimension = WirelessCardFilters
            .choice(DIMENSION_MENU, Integer.class, this::dimensionLabel, this::dimensionOption, variant)
            .maxVerticalMenuSize(MENU_HEIGHT);
        dimension.value(
            new IntValue.Dynamic(
                () -> clientView().dimension() == null ? ALL_DIMENSIONS : clientView().dimension(),
                value -> filter(value == ALL_DIMENSIONS ? null : value, clientView().state())));
        visualisation = WirelessCardFilters
            .choice(VISUALISATION_MENU, Integer.class, this::visualisationLabel, this::visualisationOption, variant)
            .directionUp()
            .maxVerticalMenuSize(MENU_HEIGHT);
        visualisation.value(new IntValue.Dynamic(() -> view().getInteger("visualMode"), this::setVisualisation));
        List<Integer> modes = new ArrayList<>();
        for (int mode = 0; mode < WirelessCardVisualisation.MODE_COUNT; mode++) modes.add(mode);
        visualisation.choices(modes);
        child(dimension).child(visualisation);
        detailsPanel = IPanelHandler.simple(this, (parent, player) -> new WirelessCardDetailsPanel(container), true);
        sourceRenamePanel = IPanelHandler
            .simple(this, (parent, player) -> new WirelessCardRenameDialog(this, () -> renameSourceId), true);
        for (int id : new int[] { TOGGLE_AUTO, TOGGLE_GROUPING, OPEN_SOURCES }) addButton(id);
        addRowButtons(
            OPEN_DETAILS,
            OPEN_ENTRANCES,
            TOGGLE_PAUSE,
            COPY_POSITION,
            HIGHLIGHT,
            TELEPORT,
            RENAME_SOURCE,
            DELETE_SOURCE);
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
        filter(clientView().dimension(), clientView().state());
    }

    @Override
    boolean editingText() {
        return search.isFocused();
    }

    @Override
    public boolean onKeyPressed(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE && (dimension.isOpen() || visualisation.isOpen())) {
            dimension.closeMenu(false);
            visualisation.closeMenu(false);
            return true;
        }
        return super.onKeyPressed(character, key);
    }

    public ModularScreen screen() {
        AE2Theme.useMenuTheme(themeId, GTNeverReset.MODID, NAME, DIMENSION_MENU, VISUALISATION_MENU);
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
                if (button == 1 && !(hovered instanceof MenuPanel)) {
                    if (hovered == null) return true;
                    if (hovered.getTopHovering() instanceof TextFieldWidget) super.onMousePressed(button);
                    else if (hovered instanceof WirelessCardRenameDialog dialog) dialog.closeIfOpen();
                    else if (hovered instanceof WirelessCardPanel panel && (panel.isDetailView() || panel.view()
                        .getBoolean("sourcesPage"))) panel.action(BACK);
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
        }.useTheme(themeId);
    }

    @Override
    void resizePanel(Layout layout) {
        xSize = layout.width();
        ySize = layout.height();
        container.clientView()
            .resize(layout.rows());
        this.layout = layout;
        visualisation.deleteMenu();
        int searchWidth = xSize - 28 - DIMENSION_WIDTH - mergeWidth();
        search.pos(10, 65)
            .size(searchWidth, 12);
        dimension.pos(14 + searchWidth, 64)
            .size(DIMENSION_WIDTH, 14);
        visualisation.pos(layout.visualisationX(), ySize - 27)
            .size(layout.visualisationWidth(), 20);
        layoutChips();
    }

    @Override
    void layoutButton(WirelessCardButton control, Layout layout) {
        switch (control.id) {
            case OPEN_SOURCES -> control.widget.pos(8, 23)
                .size(xSize - 92, 12);
            case TOGGLE_AUTO -> control.widget.pos(xSize - 10 - autoWidth(), 12)
                .size(autoWidth(), AE2Theme.SWITCH_HEIGHT);
            case TOGGLE_GROUPING -> control.widget.pos(xSize - 10 - mergeWidth(), 65)
                .size(mergeWidth(), AE2Theme.SWITCH_HEIGHT);
        }
    }

    private WirelessCardClientView clientView() {
        return container.clientView();
    }

    private int chipColor(int index) {
        return switch (CHIP_STATES[index]) {
            case 3 -> goodColor();
            case 5, 2 -> badColor();
            case 1, 4 -> waitingColor();
            default -> foreground();
        };
    }

    private ButtonWidget<?> chip(int index) {
        ButtonWidget<?> chip = WirelessCardFilters.chip(
            () -> chipLabel(index),
            () -> chipCount(index),
            () -> chipColor(index),
            () -> chipAvailable(index),
            () -> clientView().state() == CHIP_STATES[index],
            () -> {
                if (!layoutReady()) return false;
                filter(clientView().dimension(), CHIP_STATES[index]);
                return true;
            })
            .tooltipBuilder(tooltip -> {
                tooltip.addLine(index == 0 ? tr("chip_all_tip") : tr("chip_tip", chipLabel(index)));
                tooltip.addLine(EnumChatFormatting.GRAY + tr("chip_count", chipCount(index)));
            })
            .tooltipAutoUpdate(true);
        chips.add(chip);
        return chip;
    }

    private static String chipLabel(int index) {
        return tr("filter." + CHIP_STATES[index]);
    }

    private int chipCount(int index) {
        int[] counts = view().getIntArray("stateCounts");
        return CHIP_STATES[index] < counts.length ? counts[CHIP_STATES[index]] : 0;
    }

    private boolean chipAvailable(int index) {
        return clientView().state() == CHIP_STATES[index] || chipCount(index) > 0;
    }

    private String dimensionLabel() {
        Integer id = clientView().dimension();
        return id == null ? tr("dimension_all") : dimensionName(id);
    }

    public String dimensionName(int id) {
        String name = clientView().dimensionName(id);
        return name.isEmpty() ? "D" + id : name;
    }

    private String dimensionOption(int value) {
        if (value == ALL_DIMENSIONS) return tr("dimension_all");
        int count = 0;
        for (int[] entry : clientView().dimensions()) if (entry[0] == value) count = entry[1];
        return tr("dimension_option", dimensionName(value), value, count);
    }

    private List<Integer> dimensionChoices() {
        List<Integer> choices = new ArrayList<>();
        choices.add(ALL_DIMENSIONS);
        for (int[] entry : clientView().dimensions()) choices.add(entry[0]);
        return choices;
    }

    private String visualisationLabel() {
        String name = visualisationName(view().getInteger("visualMode"), true);
        return layout.visualisationWidth() < 102 ? name : tr("visualisation_button", name);
    }

    private String visualisationOption(int mode) {
        String name = visualisationName(mode, false);
        return fontRendererObj.getStringWidth(name) <= layout.visualisationWidth() - 8 ? name
            : visualisationName(mode, true);
    }

    private static String visualisationName(int mode, boolean compact) {
        return StatCollector
            .translateToLocal(WirelessCardVisualisation.modeKey(mode) + (compact && mode > 0 ? ".short" : ""));
    }

    private void layoutChips() {
        WirelessCardFilters.layoutChips(chips, xSize, WirelessCardGui::chipLabel, this::chipCount);
    }

    private int autoWidth() {
        return Math.max(AE2Theme.switchWidth(tr("auto_on")), AE2Theme.switchWidth(tr("auto_off")));
    }

    private int mergeWidth() {
        return AE2Theme.switchWidth(tr("merge_types"));
    }

    @Override
    boolean updatePanel() {
        boolean detail = container.display.getBoolean("detail");
        if (detail != showingDetails) {
            showingDetails = detail;
            if (detail) detailsPanel.openPanel();
            else detailsPanel.closePanel();
        }
        boolean showFilters = !detail && !detailsPanel.isPanelOpen() && !view().getBoolean("sourcesPage");
        if (!showFilters && editingText()) getContext().removeFocus();
        search.setEnabled(showFilters);
        dimension.setEnabled(showFilters);
        visualisation.setEnabled(showFilters);
        for (ButtonWidget<?> chip : chips) chip.setEnabled(showFilters);
        if (!showFilters) {
            if (dimension.isOpen()) dimension.closeMenu(false);
            if (visualisation.isOpen()) visualisation.closeMenu(false);
        }
        if (view() != lastLayoutView) {
            lastLayoutView = view();
            dimension.choices(dimensionChoices());
            layoutChips();
            scheduleResize();
        }
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
                .navigate(BACK);
            return;
        }
        super.sendAction(id);
    }

    private void filter(Integer dimension, int state) {
        clientView().filter(searchText, dimension, state);
    }

    private void setVisualisation(int mode) {
        if (layoutReady()) container.sync.request(SET_VISUALISATION, mode, "", 0);
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
        boolean budgetKnown = data.getBoolean("sourceLoaded") && data.getBoolean("channels")
            && data.getBoolean("stable")
            && data.getInteger("used") >= 0
            && data.getInteger("capacity") > 0;
        int budgetColor = !budgetKnown ? textColor
            : data.getInteger("used") >= data.getInteger("capacity") ? badColor() : goodColor();
        String budget = fontRendererObj.trimStringToWidth(budgetText(data), xSize / 2);
        int budgetWidth = fontRendererObj.getStringWidth(budget);
        fontRendererObj.drawString(budget, xSize - 76 - budgetWidth, 25, budgetColor);
        ratioBar(
            xSize - 72,
            26,
            60,
            6,
            budgetKnown ? data.getInteger("used") : -1,
            data.getInteger("capacity"),
            budgetColor,
            secondary());
        String sourceLabel = data.getString("sourceLabel");
        if (!data.getString("sourceName")
            .isEmpty()) sourceLabel = data.getString("sourceName") + " (" + sourceLabel + ")";
        String source = data.getBoolean("bound") ? tr("source", sourceLabel) : tr("unbound");
        fontRendererObj
            .drawString(fontRendererObj.trimStringToWidth(source, xSize - 92 - budgetWidth), 10, 25, textColor);
    }

    @Override
    void drawFooter(NBTTagCompound data, int textColor) {
        if (data.getBoolean("sourcesPage")) return;
        var layout = new Layout(xSize, ySize, visibleRows);
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(
                tr(data.getBoolean("filtered") ? "matched" : "loaded", data.getInteger("total")),
                layout.visualisationX() - layout.footerInfoX() - 4),
            layout.footerInfoX(),
            ySize - 20,
            textColor);
    }

    @SideOnly(Side.CLIENT)
    private static class WirelessCardDetailsPanel extends WirelessCardPanel {

        private final IPanelHandler renamePanel;
        private boolean closing;

        private WirelessCardDetailsPanel(WirelessCardContainer container) {
            super("wireless_card_details", container);
            addRowButtons(TOGGLE_PAUSE, COPY_POSITION, HIGHLIGHT, TELEPORT);
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
            // #tr gtneverreset.wireless.gui.devices_title
            // # Channel devices
            // # zh_CN 频道设备
            fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(
                    tr(data.getBoolean("devicePage") ? "devices_title" : "detail_title"),
                    xSize - 40),
                10,
                11,
                textColor);
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
            child(ButtonWidget.panelCloseButton());
            child(
                IKey.str(GuiText.Renamer.getLocal())
                    .asWidget()
                    .color(owner.foreground())
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
                .overlay(IKey.lang(key))
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

        public final int id;
        public final Widget<?> widget;
        private final WirelessCardPanel panel;
        private final FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRenderer;
        private boolean available, on;
        private String label = "";

        public WirelessCardButton(int id, WirelessCardPanel panel) {
            this.id = id;
            this.panel = panel;
            if (id == TOGGLE_AUTO || id == TOGGLE_GROUPING || type(id) == TOGGLE_PAUSE) {
                ToggleButton toggle = new ToggleButton() {

                    @Override
                    public boolean canBeSeen(IViewportStack stack) {
                        return isEnabled() && super.canBeSeen(stack);
                    }

                    @Override
                    public @NotNull Result onMousePressed(int button) {
                        return button == 0 && available ? super.onMousePressed(button) : Result.ACCEPT;
                    }
                };
                IDrawable look = type(id) == TOGGLE_PAUSE
                    ? AE2Theme.iconButton(toggle, () -> on ? Icon.PAUSE : Icon.PLAY, () -> available)
                    : AE2Theme.toggleSwitch(
                        toggle,
                        () -> on,
                        () -> id == TOGGLE_AUTO ? tr(on ? "auto_on" : "auto_off") : tr("merge_types"),
                        () -> available);
                widget = toggle.value(new BoolValue.Dynamic(() -> on, ignored -> panel.action(id)))
                    .background(false, IDrawable.EMPTY)
                    .background(true, IDrawable.EMPTY)
                    .hoverBackground(false, IDrawable.EMPTY)
                    .hoverBackground(true, IDrawable.EMPTY)
                    .overlay(false, look)
                    .overlay(true, look);
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
            if (id == REFRESH) {
                widget.background(IDrawable.EMPTY)
                    .hoverBackground(IDrawable.EMPTY)
                    .overlay(AE2Theme.tabButton(widget, () -> Icon.REFRESH, () -> available))
                    .tooltipBuilder(tooltip -> tooltip.addLine(tr("refresh")));
                return;
            }
            Icon icon = switch (type(id)) {
                case OPEN_ENTRANCES -> Icon.MORE;
                case COPY_POSITION -> Icon.COPY;
                case HIGHLIGHT -> Icon.VISIBLE;
                case TELEPORT -> Icon.MOVE_TO;
                default -> null;
            };
            if (icon != null) {
                widget.background(IDrawable.EMPTY)
                    .hoverBackground(IDrawable.EMPTY)
                    .overlay(AE2Theme.iconButton(widget, () -> icon, () -> available))
                    .tooltipBuilder(tooltip -> tooltip.addLine(label))
                    .tooltipAutoUpdate(true);
                return;
            }
            IDrawable button = AE2Theme.button(widget, () -> available, () -> false);
            IDrawable background = (context, x, y, width, height, theme) -> {
                if (type(id) != OPEN_DETAILS || panel.view()
                    .getBoolean("sourcesPage")) button.draw(context, x, y, width, height, theme);
            };
            widget.background(background)
                .hoverBackground(background)
                .overlay(AE2Theme.label(widget, () -> label, () -> available, () -> false));
        }

        public void update(NBTTagCompound data, boolean ready, int visibleRows) {
            NBTTagList rows = data.getTagList("rows", 10);
            int rowCount = Math.min(visibleRows, rows.tagCount());
            int actionType = type(id);
            if (actionType == OPEN_DETAILS) {
                int y = panel.rowTop() + rowIndex(id) * WirelessCardPanel.ROW_HEIGHT;
                if (data.getBoolean("sourcesPage")) widget.pos(panel.xSize - 116, y + 2)
                    .size(60, 20);
                else {
                    var row = rows.getCompoundTagAt(rowIndex(id));
                    int end = row.getBoolean("group") ? panel.xSize - 10 : panel.rowStateX();
                    widget.pos(10, y)
                        .size(end - 10, WirelessCardPanel.ROW_HEIGHT);
                }
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
                if (actionType == OPEN_ENTRANCES) visible = !data.getBoolean("detail") && !row.getBoolean("group");
                if (actionType == TOGGLE_PAUSE && row.getBoolean("device")) visible = false;
                if (actionType == OPEN_DETAILS && data.getBoolean("sourcesPage"))
                    available &= !row.getBoolean("selected");
            }
            if (id == TOGGLE_GROUPING) on = data.getBoolean("collapseTypes");
            if (id == TOGGLE_AUTO) on = data.getBoolean("auto");
            if (id == BACK) visible = data.getBoolean("detail");
            if (id == RENAME) {
                visible = data.getBoolean("detail");
                available &= data.getBoolean("renameable");
            }
            if (id == TOGGLE_AUTO || id == TOGGLE_GROUPING) visible = !data.getBoolean("detail");
            if (actionType == OPEN_DETAILS) visible &= !data.getBoolean("detail");
            if (id == OPEN_SOURCES) visible = !data.getBoolean("detail") && !data.getBoolean("sourcesPage");
            if (actionType == RENAME_SOURCE || actionType == DELETE_SOURCE) visible = data.getBoolean("sourcesPage");
            if (data.getBoolean("sourcesPage")) {
                if (isRowAction(id))
                    visible = actionType == OPEN_DETAILS || actionType == RENAME_SOURCE || actionType == DELETE_SOURCE;
                if (id == BACK) visible = true;
                if (id == TOGGLE_AUTO || id == TOGGLE_GROUPING) visible = false;
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
                case OPEN_ENTRANCES -> tr("detail_title");
                case DELETE_SOURCE -> tr("source_delete");

                case TELEPORT -> tr("teleport");
                case HIGHLIGHT -> tr("highlight");
                case COPY_POSITION -> tr("copy");
                case OPEN_DETAILS -> data.getBoolean("sourcesPage") ? tr(
                    rows.getCompoundTagAt(rowIndex(id))
                        .getBoolean("selected") ? "source_selected" : "source_select")
                    : "";
                default -> "";
            };
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
        private final Set<String> expanded = new HashSet<>();
        private String query = "";
        private Integer dimension;
        private List<int[]> dimensions = Collections.emptyList();
        private int state, page, detailPage;
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
                expanded.clear();
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
            if (!data.getBoolean("detail")) dimensions = countDimensions(roots);
            if (data.hasKey("highlight", 9)) {
                WirelessWorldHighlightRender.show(Minecraft.getMinecraft(), data.getTagList("highlight", 10));
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

        public void filter(String text, Integer dimension, int state) {
            if (text.length() > 64 || state < 0 || state >= CHIP_STATES.length) return;
            String nextQuery = text.trim()
                .toLowerCase(Locale.ROOT);
            if (query.equals(nextQuery) && Objects.equals(this.dimension, dimension) && this.state == state) return;
            query = nextQuery;
            this.dimension = dimension;
            this.state = state;
            page = 0;
            rebuild();
        }

        public Integer dimension() {
            return dimension;
        }

        public int state() {
            return state;
        }

        public List<int[]> dimensions() {
            return dimensions;
        }

        public String dimensionName(int id) {
            return source.getCompoundTag("dimensions")
                .getString(Integer.toString(id));
        }

        public void resize(int rows) {
            if (!Layout.validRows(rows) || rows == pageSize) return;
            page = page * pageSize / rows;
            detailPage = detailPage * pageSize / rows;
            pageSize = rows;
            rebuild();
        }

        public boolean navigate(int action) {
            if (action == OPEN_SOURCES) {
                expanded.clear();
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
            if (action == PREVIOUS_PAGE || action == NEXT_PAGE) {
                int change = action == NEXT_PAGE ? 1 : -1;
                if (detail) detailPage += change;
                else page += change;
            } else if (type(action) == OPEN_DETAILS && !detail) {
                NBTTagList rows = container.display.getTagList("rows", 10);
                int index = rowIndex(action);
                if (index < 0 || index >= rows.tagCount()) return true;
                NBTTagCompound row = rows.getCompoundTagAt(index);
                if (!row.getBoolean("group")) return false;
                String key = row.getString("key");
                if (!expanded.remove(key)) expanded.add(key);
            } else return false;
            rebuild();
            return true;
        }

        public void rebuild() {
            if (!source.hasKey("session")) return;
            boolean detail = source.getBoolean("detail");
            container.display = buildListing(detail ? detailPage : page);
            if (detail) detailPage = container.display.getInteger("page");
            else page = container.display.getInteger("page");
        }

        private NBTTagCompound buildListing(int requestedPage) {
            if (sourcesPage) return buildSources(requestedPage);
            boolean detail = source.getBoolean("detail");
            NBTTagCompound data = copy(source);
            List<Shown> shown = new ArrayList<>();
            if (detail) {
                for (Row row : roots) shown.add(new Shown(row, 0, 0, false));
            } else {
                int[] counts = new int[CHIP_STATES.length];
                List<Row> rows = new ArrayList<>();
                for (Row row : roots) {
                    for (Row leaf : row.children()
                        .isEmpty() ? Collections.singletonList(row) : row.children()) {
                        if (!matchesPlace(leaf)) continue;
                        for (int s = 0; s < CHIP_STATES.length; s++) if (matchesState(leaf.data(), s)) counts[s]++;
                    }
                    Row matched = filtered(row);
                    if (matched != null) rows.add(matched);
                }
                data.setIntArray("stateCounts", counts);
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
                if (data.getBoolean("collapseTypes")) rows = collapse(rows);
                order(rows, "", data.getBoolean("collapseTypes"));
                flatten(rows, 0, 0, data.getBoolean("collapseTypes"), shown);
            }
            data.setInteger("rowCount", shown.size());
            int pages = Math.max(1, (shown.size() + pageSize - 1) / pageSize);
            int currentPage = Math.max(0, Math.min(requestedPage, pages - 1));
            data.setInteger("page", currentPage);
            data.setInteger("pageSize", pageSize);
            data.setInteger("pages", pages);
            data.setInteger("filterState", state);
            data.setBoolean("filtered", !query.isEmpty() || dimension != null || state != 0);
            NBTTagList entries = new NBTTagList();
            for (Shown entry : shown
                .subList(currentPage * pageSize, Math.min(shown.size(), (currentPage + 1) * pageSize))) {
                NBTTagCompound row = copy(
                    entry.row()
                        .data());
                if (row.getBoolean("category")) localizePosition(row, "position", data.getCompoundTag("dimensions"));
                row.setInteger("depth", entry.depth());
                row.setInteger("treeLines", entry.lines());
                row.setBoolean("treeLast", entry.last());
                row.setBoolean("expanded", row.getBoolean("group") && expanded.contains(row.getString("key")));
                entries.appendTag(row);
            }
            data.setTag("rows", entries);
            return data;
        }

        private void flatten(List<Row> rows, int depth, int lines, boolean collapse, List<Shown> shown) {
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                boolean last = i == rows.size() - 1;
                shown.add(new Shown(row, depth, lines, last));
                String key = row.data()
                    .getString("key");
                if (!row.data()
                    .getBoolean("group") || !expanded.contains(key)) continue;
                List<Row> children = new ArrayList<>(row.children());
                order(children, key, collapse);
                flatten(children, depth + 1, depth == 0 || last ? lines : lines | 1 << depth - 1, collapse, shown);
            }
        }

        private void order(List<Row> rows, String parent, boolean collapse) {
            var view = new SortView(parent, collapse, query, dimension, state);
            orders.put(view, sort(rows, orders.getOrDefault(view, Collections.emptyList())));
            if (orders.size() > 16) orders.remove(
                orders.keySet()
                    .iterator()
                    .next());
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
            return matchesState(row.data(), state) && matchesPlace(row);
        }

        private boolean matchesPlace(Row row) {
            if (dimension != null && dimension != row.data()
                .getInteger("dimension")) return false;
            return query.isEmpty() || (row.names() + " " + row.positions()).contains(query)
                || matcher.contains(row.names(), query);
        }

        private static boolean matchesState(NBTTagCompound data, int state) {
            boolean paused = data.getBoolean("paused");
            return switch (state) {
                case 1 -> paused;
                case 2 -> data.getBoolean("conflicted");
                case 3 -> !paused && data.getString("state")
                    .equals("active");
                case 4 -> !paused && data.getString("state")
                    .equals("waiting");
                case 5 -> !paused && data.getBoolean("stable") && data.getInteger("missing") > 0;
                default -> true;
            };
        }

        private static List<int[]> countDimensions(List<Row> rows) {
            Map<Integer, int[]> counts = new TreeMap<>();
            for (Row row : rows) for (Row leaf : row.children()
                .isEmpty() ? Collections.singletonList(row) : row.children()) {
                    int id = leaf.data()
                        .getInteger("dimension");
                    counts.computeIfAbsent(id, key -> new int[] { key, 0 })[1]++;
                }
            return new ArrayList<>(counts.values());
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
        private record Shown(Row row, int depth, int lines, boolean last) {}

        @Desugar
        private record RowOrder(String key, boolean paused, boolean pinned) {}

        @Desugar
        private record SortView(String parent, boolean collapse, String query, Integer dimension, int state) {}

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
            return new Layout(Math.max(320, Math.min(520, width - 12)), 112 + rows * 24, rows);
        }

        public int visualisationWidth() {
            return Math.max(66, Math.min(120, width - 250));
        }

        public int visualisationX() {
            return width - 34 - visualisationWidth();
        }

        public int footerInfoX() {
            return 100;
        }

        public static boolean validRows(int rows) {
            return rows >= MIN_ROWS && rows <= MAX_ROWS;
        }
    }
}
