package com.xyp.gtneverreset.client.wireless;

import static com.xyp.gtneverreset.common.gui.WirelessPowerCardGui.*;
import static net.minecraft.client.gui.Gui.drawRect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.wireless.WirelessCardGui.Layout;
import com.xyp.gtneverreset.common.gui.WirelessPowerCardGui.Session;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Colors;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Icon;
import com.xyp.gtnrlib.gui.widget.CycleDropdown;

import appeng.core.localization.ColorUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gtneioreplugin.util.DimensionHelper;

@SideOnly(Side.CLIENT)
public final class WirelessPowerCardPanel extends ModularPanel {

    private static final int ROW_TOP = 91, ROW_HEIGHT = 24;
    private static final String NAME = "wireless_power_card", DIMENSION_MENU = NAME + "_dimension";
    private static final PowerState[] STATES = PowerState.values();
    private final List<ButtonWidget<?>> chips = new ArrayList<>();
    private final CycleDropdown<String> dimension;
    private final ToggleButton merge;
    private NBTTagCompound lastFilterView;
    private final String themeId = ScreenThemeConfig.wirelessPowerCard.themeId;
    private final AE2Theme.Variant variant = AE2Theme.variant(themeId);
    private final int scrollWidth = AE2Theme.scrollbarWidth(variant);
    private final WirelessCardScrollbar scrollbar;
    private final Session session;
    private final boolean details;
    private final IPanelHandler detailsPanel;
    private NBTTagCompound snapshot = new NBTTagCompound();
    private boolean closing;
    private final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    private final Map<String, Portrait> portraits = new HashMap<>();
    private final Map<Integer, ItemStack> icons = new HashMap<>();
    private final ButtonWidget<?> back, refresh, sourceLink;
    private final ToggleButton automatic;
    private final ButtonWidget<?>[] select = new ButtonWidget<?>[MAX_ROWS], sourceRows = new ButtonWidget<?>[MAX_ROWS],
        remove = new ButtonWidget<?>[MAX_ROWS], teleport = new ButtonWidget<?>[MAX_ROWS],
        copy = new ButtonWidget<?>[MAX_ROWS], highlight = new ButtonWidget<?>[MAX_ROWS],
        deviceRows = new ButtonWidget<?>[MAX_ROWS];
    private final ToggleButton[] power = new ToggleButton[MAX_ROWS];
    private final TextFieldWidget search;
    private String searchText = "";
    private int footerLeft = 10;
    private int xSize = 320, ySize = 232, visibleRows = MIN_ROWS, requestedRows;

    public WirelessPowerCardPanel(Session session) {
        this(session, false);
    }

    private WirelessPowerCardPanel(Session session, boolean details) {
        super(details ? "wireless_power_details" : NAME);
        this.session = session;
        this.details = details;
        detailsPanel = details ? null
            : IPanelHandler.simple(this, (parent, player) -> new WirelessPowerCardPanel(session, true), true);
        if (details) child(ButtonWidget.panelCloseButton());
        scrollbar = new WirelessCardScrollbar(
            variant,
            this::view,
            this::scrollReady,
            offset -> session.request(SCROLL_TO, Integer.toString(offset)));
        child(scrollbar);
        // #tr power.gui.back
        // # Back
        // # zh_CN 返回
        back = button(() -> tr("power.gui.back"), details ? CLOSE : DEVICES, () -> "", () -> true);
        back.setEnabledIf(widget -> details || sources());
        // #tr power.gui.refresh
        // # Refresh
        // # zh_CN 刷新
        refresh = iconButton(
            Icon.REFRESH,
            () -> tr("power.gui.refresh"),
            () -> session.request(REFRESH, ""),
            () -> true);
        sourceLink = new CardButton().background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .onMousePressed(mouse -> {
                if (mouse == 0) session.request(SOURCES, "");
                return true;
            })
            .setEnabledIf(widget -> !details && !sources());
        sourceLink.tooltipBuilder(tooltip -> {
            // #tr power.gui.selected
            // # Power source: %s
            // # zh_CN 供电源：%s
            tooltip.addLine(tr("power.gui.selected", view().getString("sourceName")));
            tooltip.addLine(view().getString("balance") + " EU");
        })
            .tooltipAutoUpdate(true);
        child(sourceLink);
        automatic = toggle(
            () -> session.display()
                .getBoolean("auto"),
            AUTO,
            () -> "");
        automatic.setEnabledIf(widget -> !details && !sources());
        // #tr power.gui.search
        // # Search devices, sources or coordinates...
        // # zh_CN 搜索设备、电网或坐标…
        search = new TextFieldWidget() {

            @Override
            public boolean canBeSeen(IViewportStack stack) {
                return isEnabled() && super.canBeSeen(stack);
            }
        }.setMaxLength(64)
            .padding(2, 0)
            .hintText(tr("power.gui.search"))
            .autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> searchText, text -> {
                if (searchText.equals(text)) return;
                searchText = text;
                session.request(FILTER, text);
            }));
        search.setEnabledIf(widget -> !details && !sources());
        child(search);
        if (!details) {
            for (PowerState state : STATES) {
                ButtonWidget<?> chip = WirelessCardFilters.chip(
                    () -> stateLabel(state),
                    () -> stateCount(state),
                    () -> stateColor(state, variant == null ? null : variant.colors),
                    () -> state == PowerState.ALL || activeState() == state || stateCount(state) > 0,
                    () -> activeState() == state,
                    () -> {
                        session.request(STATE, state.name());
                        return true;
                    });
                chip.tooltipBuilder(tooltip -> {
                    tooltip.addLine(
                        state == PowerState.ALL ? tr("gtneverreset.wireless.gui.chip_all_tip")
                            : tr("gtneverreset.wireless.gui.chip_tip", stateLabel(state)));
                    // #tr power.gui.devices
                    // # Connected devices: %s
                    // # zh_CN 已接入设备：%s
                    tooltip.addLine(tr("power.gui.devices", stateCount(state)));
                })
                    .tooltipAutoUpdate(true);
                chips.add(chip);
                child(chip);
            }
            dimension = WirelessCardFilters
                .choice(
                    DIMENSION_MENU,
                    String.class,
                    () -> dimensionLabel(selectedDimension(), false),
                    id -> dimensionLabel(id, true),
                    variant)
                .maxVerticalMenuSize(120);
            dimension.value(new StringValue.Dynamic(this::selectedDimension, id -> session.request(DIMENSION, id)));
            child(dimension);
            merge = toggle(() -> view().getBoolean("mergeTypes"), MERGE_TYPES, () -> "");
        } else {
            dimension = null;
            merge = null;
        }
        for (int i = 0; i < MAX_ROWS; i++) {
            final int index = i;
            // #tr power.gui.select
            // # Select
            // # zh_CN 选择
            // #tr power.gui.selected_row
            // # Selected
            // # zh_CN 已选中
            select[i] = button(
                () -> tr(selected(index) ? "power.gui.selected_row" : "power.gui.select"),
                SELECT,
                () -> row(index).getString("id"),
                () -> !selected(index));
            select[i].setEnabledIf(widget -> sources() && hasRow(index));
            sourceRows[i] = new CardButton().background(IDrawable.EMPTY)
                .hoverBackground(IDrawable.EMPTY)
                .onMousePressed(mouse -> {
                    if (mouse == 0) session.request(SELECT, row(index).getString("id"));
                    return true;
                })
                .setEnabledIf(widget -> sources() && hasRow(index));
            sourceRows[i].tooltipBuilder(tooltip -> {
                tooltip.addLine(row(index).getString("name"));
                tooltip.addLine(row(index).getString("balance") + " EU");
            })
                .tooltipAutoUpdate(true);
            child(sourceRows[i]);
            // #tr power.gui.remove
            // # Unlink
            // # zh_CN 解绑
            remove[i] = iconButton(
                Icon.REMOVE,
                () -> tr("power.gui.remove"),
                () -> session.request(REMOVE, row(index).getString("id")),
                () -> true);
            remove[i].setEnabledIf(widget -> device(index));
            // #tr power.gui.teleport
            // # Teleport
            // # zh_CN 传送
            teleport[i] = iconButton(
                Icon.MOVE_TO,
                () -> tr("power.gui.teleport"),
                () -> session.request(TELEPORT, row(index).getString("id")),
                () -> row(index).getBoolean("loaded"));
            teleport[i].setEnabledIf(widget -> device(index));
            // #tr power.gui.copy
            // # Copy
            // # zh_CN 复制
            copy[i] = iconButton(Icon.COPY, () -> tr("power.gui.copy"), () -> copyPosition(index), () -> true);
            copy[i].setEnabledIf(widget -> !sources() && hasRow(index) && row(index).getInteger("typeCount") == 0);
            // #tr power.gui.highlight
            // # Highlight
            // # zh_CN 高亮
            highlight[i] = iconButton(
                Icon.VISIBLE,
                () -> tr("power.gui.highlight"),
                () -> session.request(HIGHLIGHT, row(index).getString("id")),
                () -> row(index).getBoolean("loaded"));
            highlight[i].setEnabledIf(widget -> device(index));
            power[i] = toggle(() -> !row(index).getBoolean("paused"), PAUSE, () -> row(index).getString("id"));
            power[i].setEnabledIf(widget -> device(index));
            deviceRows[i] = new CardButton().background(IDrawable.EMPTY)
                .hoverBackground(IDrawable.EMPTY)
                .onMousePressed(mouse -> {
                    var row = row(index);
                    if (mouse == 0 && row.getInteger("typeCount") > 1)
                        session.request(EXPAND_TYPE, row.getString("id"));
                    else if (mouse == 0 && row.getBoolean("group")) session.request(OPEN, row.getString("id"));
                    return true;
                })
                .setEnabledIf(widget -> !sources() && hasRow(index));
            deviceRows[i].tooltipBuilder(tooltip -> {
                NBTTagCompound row = row(index);
                tooltip.addLine(deviceName(row));
                if (row.getInteger("typeCount") > 1) {
                    tooltip.addLine(tr("power.gui.devices", row.getInteger("typeCount")));
                    return;
                }
                tooltip.addLine(position(row, true));
                tooltip.addLine(row.getString("sourceName"));
                if (row.hasKey("recipeEUt")) {
                    // #tr power.gui.recipe_usage
                    // # Recipe consumption: %s EU/t
                    // # zh_CN 配方耗电：%s EU/t
                    tooltip.addLine(tr("power.gui.recipe_usage", compact(Long.toString(row.getLong("recipeEUt")))));
                }
                // #tr power.gui.wireless_input
                // # Wireless input (1s avg): %s EU/t
                // # zh_CN 无线供电（近1秒平均）：%s EU/t
                tooltip.addLine(tr("power.gui.wireless_input", rate(row.getDouble("transfer"))));
                if (!row.getBoolean("loaded")) {
                    // #tr power.gui.unloaded_hint
                    // # This chunk is not loaded. The connection is kept and resumes when the chunk loads.
                    // # zh_CN 目标区块未加载；保留连接，区块加载后自动恢复供电。
                    tooltip.addLine(tr("power.gui.unloaded_hint"));
                }
            })
                .tooltipAutoUpdate(true);
            child(deviceRows[i]);
        }
        resizeLayout(332, 240);
    }

    public ModularScreen screen() {
        AE2Theme.useMenuTheme(themeId, GTNeverReset.MODID, NAME, DIMENSION_MENU);
        return new ModularScreen(GTNeverReset.MODID, this) {

            @Override
            public void onResize(int width, int height) {
                resizeLayout(width, height);
                for (ModularPanel panel : getPanelManager().getOpenPanels()) {
                    if (panel instanceof WirelessPowerCardPanel cardPanel && panel != WirelessPowerCardPanel.this)
                        cardPanel.resizeLayout(width, height);
                }
                super.onResize(width, height);
            }

            @Override
            public boolean onMousePressed(int button) {
                if (button == 1 && detailsPanel.isPanelOpen()) {
                    session.request(CLOSE, "");
                    return true;
                }
                if (button == 1 && sources()) {
                    session.request(DEVICES, "");
                    return true;
                }
                return super.onMousePressed(button);
            }
        }.useTheme(themeId);
    }

    private void resizeLayout(int width, int height) {
        Layout layout = Layout.forScreen(width, height);
        xSize = layout.width() - (details ? 24 : 0);
        ySize = layout.height() - (details ? ROW_TOP - 60 : 0);
        visibleRows = layout.rows();
        size(xSize, ySize);
        scrollbar.pos(xSize - 10 - scrollWidth, rowTop() - 2)
            .size(scrollWidth, visibleRows * ROW_HEIGHT + 2);
        // NEI keeps two shortcut buttons in the lower-left corner at large GUI scales.
        footerLeft = height - ySize < 40 ? Math.max(10, 46 - (width - xSize) / 2) : 10;
        back.pos(footerLeft, ySize - 27)
            .size(36, 20);
        refresh.pos(xSize - 30, ySize - 27)
            .size(20, 20);
        if (!details) {
            sourceLink.pos(10, 19)
                .size(statWidth(), 34);
            int autoWidth = Math.max(
                AE2Theme.switchWidth(tr(WirelessPowerCard.autoOnKey())),
                AE2Theme.switchWidth(tr(WirelessPowerCard.autoOffKey())));
            int mergeWidth = AE2Theme.switchWidth(tr("gtneverreset.wireless.gui.merge_types"));
            automatic.pos(xSize - 34 - autoWidth, ySize - 23)
                .size(autoWidth, AE2Theme.SWITCH_HEIGHT);
            merge.pos(xSize - 38 - autoWidth - mergeWidth, ySize - 23)
                .size(mergeWidth, AE2Theme.SWITCH_HEIGHT);
            search.pos(10, 74)
                .size(xSize - 124, 12);
            dimension.pos(xSize - 110, 73)
                .size(100, 14);
            layoutChips();
        }
        for (int i = 0; i < MAX_ROWS; i++) {
            int y = rowTop() + i * ROW_HEIGHT;
            select[i].pos(rowRight() - 67, y + 2)
                .size(64, 20);
            sourceRows[i].pos(10, y)
                .size(rowRight() - 85, ROW_HEIGHT);
            teleport[i].pos(rowRight() - 91, y + 4)
                .size(16, 16);
            copy[i].pos(rowRight() - 73, y + 4)
                .size(16, 16);
            highlight[i].pos(rowRight() - 55, y + 4)
                .size(16, 16);
            remove[i].pos(rowRight() - 37, y + 4)
                .size(16, 16);
            power[i].pos(rowRight() - 19, y + 4)
                .size(16, 16);
            deviceRows[i].pos(10, y)
                .size(stateX() - 10, ROW_HEIGHT);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!details) {
            if (session.display()
                .getBoolean("detail")) detailsPanel.openPanel();
            else detailsPanel.closePanel();
        }
        if (!details) {
            boolean showFilters = !sources() && !session.display()
                .getBoolean("detail");
            for (ButtonWidget<?> chip : chips) chip.setEnabled(showFilters);
            dimension.setEnabled(showFilters);
            merge.setEnabled(showFilters);
            if (!showFilters && dimension.isOpen()) dimension.closeMenu(false);
            if (showFilters && lastFilterView != view()) {
                lastFilterView = view();
                List<String> choices = new ArrayList<>();
                choices.add("");
                var options = view().getTagList("dimensions", 10);
                for (int i = 0; i < options.tagCount(); i++) choices.add(
                    Integer.toString(
                        options.getCompoundTagAt(i)
                            .getInteger("dimension")));
                dimension.choices(choices);
                for (int i = 0; i < visibleRows; i++) {
                    int end = row(i).getInteger("typeCount") > 1 ? rowRight() : stateX();
                    deviceRows[i].width(end - 10);
                }
                layoutChips();
                scheduleResize();
            }
        }
        if (!details && session.isValid() && requestedRows != visibleRows) {
            requestedRows = visibleRows;
            session.request(RESIZE, Integer.toString(visibleRows));
        }
    }

    @Override
    public boolean isDraggable() {
        return details;
    }

    @Override
    public void onOpen(ModularScreen screen) {
        closing = false;
        resizeLayout(screen.getScreenArea().width, screen.getScreenArea().height);
        super.onOpen(screen);
    }

    @Override
    public void closeIfOpen() {
        if (details && !closing
            && session.display()
                .getBoolean("detail")) {
            closing = true;
            session.request(CLOSE, "");
        }
        super.closeIfOpen();
    }

    @Override
    public boolean onKeyPressed(char character, int key) {
        if (!details && key == Keyboard.KEY_ESCAPE && dimension.isOpen()) {
            dimension.closeMenu(false);
            return true;
        }
        if (details && key == Keyboard.KEY_ESCAPE) {
            closeIfOpen();
            return true;
        }
        return super.onKeyPressed(character, key);
    }

    private int rowTop() {
        return details ? 60 : ROW_TOP;
    }

    private int rowRight() {
        return xSize - scrollWidth - 16;
    }

    private int stateX() {
        return rowRight() - 164;
    }

    private boolean scrollReady() {
        return session.isValid() && session.display()
            .getBoolean("detail") == details && view().getInteger("rowsPerPage") == visibleRows;
    }

    @Override
    public boolean onMouseScroll(UpOrDown direction, int amount) {
        if (super.onMouseScroll(direction, amount)) return true;
        int x = getContext().getAbsMouseX() - getArea().x;
        int y = getContext().getAbsMouseY() - getArea().y - rowTop();
        if (!scrollReady() || x < 8 || x >= xSize - 8 || y < 0 || y >= visibleRows * ROW_HEIGHT) return false;
        int steps = Math.max(1, Math.abs(amount) / 120);
        session.request(SCROLL_BY, Integer.toString(direction.isUp() ? -steps : steps));
        return true;
    }

    private NBTTagCompound view() {
        if (session.display()
            .getBoolean("detail") == details) snapshot = session.display();
        return snapshot;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            Platform.setupDrawFont();
            if (variant != null) {
                GL11.glColor4f(1, 1, 1, 1);
                variant.inset.draw(
                    8,
                    rowTop() - AE2Theme.INSET_TOP,
                    rowRight() - 7,
                    visibleRows * ROW_HEIGHT + AE2Theme.INSET_TOP + AE2Theme.INSET_EDGE);
            }
            int text = variant == null ? ColorUtils.guiTextColorGray.getColor() : variant.colors.text();
            if (details) {
                NBTTagCompound controller = view().getCompoundTag("controller");
                text(deviceName(controller), 10, 11, xSize - 40, text);
                text(position(controller, true), 10, 29, xSize - 20, text);
                // #tr power.gui.hatches
                // # Energy inputs: %s/%s loaded
                // # zh_CN 能源输入：%s/%s 已加载
                text(
                    tr("power.gui.hatches", controller.getInteger("online"), controller.getInteger("members")),
                    10,
                    47,
                    xSize - 20,
                    text);
            } else if (sources()) {
                // #tr power.gui.sources_title
                // # Wireless Power Card · Sources
                // # zh_CN 无线电网卡 · 电网源
                text(tr("power.gui.sources_title"), 10, 11, xSize - 20, text);
                // #tr power.gui.sources_hint
                // # Select a player's avatar or press Select to use their wireless power network.
                // # zh_CN 点击玩家头像或选择按钮，使用该玩家的无线电网。
                text(tr("power.gui.sources_hint"), 10, 29, xSize - 20, text);
                // #tr power.gui.sources_auto_hint
                // # Auto-connect uses the selected source; existing connections stay unchanged.
                // # zh_CN 自动连接使用当前选中的源；已有连接保持不变。
                text(tr("power.gui.sources_auto_hint"), 10, 47, xSize - 20, text);
            } else {
                drawOverview(text);
            }
            int mouseX = context.getAbsMouseX() - getArea().x;
            int mouseY = context.getAbsMouseY() - getArea().y - rowTop();
            int hovered = isBelowMouse() && mouseX >= 9 && mouseX < rowRight() && mouseY >= 0 ? mouseY / ROW_HEIGHT
                : -1;
            for (int i = 0; i < visibleRows && hasRow(i); i++)
                drawRow(i, i == hovered || row(i).getBoolean("expanded") || sources() && selected(i));
            if (view().getInteger("total") == 0) {
                // #tr power.gui.empty
                // # No matching devices. Carry this card when placing GT receivers, or right-click existing ones.
                // # zh_CN 暂无匹配的设备。携带卡片放置 GT 用电设备，或手持卡片右键已有设备。
                int y = rowTop() + 24;
                for (String line : font.listFormattedStringToWidth(tr("power.gui.empty"), xSize - 48)) {
                    font.drawString(line, (xSize - font.getStringWidth(line)) / 2, y, text);
                    y += 12;
                }
            }
            if (!details && !sources()) {
                boolean filtered = view().hasKey("dimension") || !view().getString("filter")
                    .isEmpty() || activeState() != PowerState.ALL;
                text(
                    tr(
                        // #tr power.gui.matched
                        // # Matching devices: %s
                        // # zh_CN 匹配设备：%s
                        filtered ? "power.gui.matched" : "power.gui.devices",
                        view().getInteger("matched")),
                    footerLeft,
                    ySize - 20,
                    merge.getArea().x - getArea().x - footerLeft - 4,
                    text);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private int statWidth() {
        return (xSize - 32) / 3;
    }

    private void drawOverview(int foreground) {
        NBTTagCompound data = view();
        text(tr("item.gtneverreset.wireless_power_card.name"), 10, 6, xSize - 20, foreground);
        int[] overall = data.getIntArray("overall");
        if (overall.length != STATES.length) return;
        Colors colors = variant == null ? null : variant.colors;
        int secondary = colors == null ? foreground : colors.secondary();
        int good = stateColor(PowerState.CONNECTED, colors), warning = stateColor(PowerState.OFFLINE, colors);
        int width = statWidth(), x = 14;
        int line = colors == null ? ColorUtils.craftingDiagnosticTerminalLine.getColor() : colors.line();
        // #tr power.gui.source_title
        // # Power source
        // # zh_CN 供电源
        text(tr("power.gui.source_title"), x, 23, width - 8, foreground);
        text(compact(data.getString("balance")) + " EU", x, 44, width - 8, good);
        text(data.getString("sourceName"), x, 34, width - 8, secondary);
        x += width + 6;
        drawRect(x - 8, 22, x - 7, 50, line);
        int total = data.getInteger("deviceCount"), connected = overall[PowerState.CONNECTED.ordinal()];
        drawStat(
            x,
            tr("gtneverreset.wireless.gui.module.devices"),
            connected + "/" + total,
            connected,
            total,
            good,
            foreground,
            line);
        text(stateLabel(PowerState.PAUSED) + " " + overall[PowerState.PAUSED.ordinal()], x, 44, width - 8, secondary);
        x += width + 6;
        drawRect(x - 8, 22, x - 7, 50, line);
        int offline = overall[PowerState.OFFLINE.ordinal()], empty = overall[PowerState.NO_POWER.ordinal()];
        // #tr power.gui.unavailable
        // # Unavailable
        // # zh_CN 供电异常
        drawStat(
            x,
            tr("power.gui.unavailable"),
            Integer.toString(offline + empty),
            offline + empty,
            total,
            warning,
            foreground,
            line);
        // #tr power.gui.unavailable_counts
        // # Offline %s · No power %s
        // # zh_CN 未加载 %s · 缺电 %s
        text(tr("power.gui.unavailable_counts", offline, empty), x, 44, width - 8, secondary);
    }

    private void drawStat(int x, String title, String value, int count, int total, int color, int foreground,
        int line) {
        int width = statWidth() - 8;
        int valueWidth = font.getStringWidth(value);
        text(title, x, 23, width - valueWidth - 3, foreground);
        text(value, x + width - valueWidth, 23, valueWidth, color);
        drawRect(x, 35, x + width, 39, line | 0xff000000);
        if (count > 0) drawRect(x, 35, x + (int) Math.round((double) width * count / total), 39, color | 0xff000000);
    }

    private void drawRow(int index, boolean highlighted) {
        NBTTagCompound row = row(index);
        int y = rowTop() + index * ROW_HEIGHT;
        Colors colors = variant == null ? null : (highlighted ? AE2Theme.Variant.LIGHT : variant).colors;
        int textColor = colors == null ? ColorUtils.guiTextColorGray.getColor() : colors.text();
        int secondary = colors == null ? textColor : colors.secondary();
        if (highlighted) {
            drawRect(9, y, rowRight(), y + ROW_HEIGHT - 1, variant == null ? 0x60ffffff : AE2Theme.HIGHLIGHT);
            drawRect(
                9,
                y + ROW_HEIGHT - 2,
                rowRight(),
                y + ROW_HEIGHT - 1,
                variant == null ? 0x40000000 : AE2Theme.HIGHLIGHT_SHADOW);
        }
        drawRect(
            9,
            y + ROW_HEIGHT - 1,
            rowRight(),
            y + ROW_HEIGHT,
            variant == null ? ColorUtils.craftingDiagnosticTerminalLine.getColor() : variant.colors.line());
        if (sources()) {
            portraits.computeIfAbsent(row.getString("id"), key -> new Portrait(key, row.getString("name")))
                .draw(12, y + 4, 16, 16);
            Platform.setupDrawFont();
            text(row.getString("name"), 34, y + 2, rowRight() - 117, textColor);
            text(compact(row.getString("balance")) + " EU", 34, y + 13, rowRight() - 117, secondary);
        } else {
            ItemStack icon = icon(row);
            int indent = row.getBoolean("indented") ? 12 : 0;
            boolean folder = row.getInteger("typeCount") > 1;
            boolean group = folder || row.getBoolean("group");
            int iconX = 12 + indent + (group ? 12 : 0);
            int textX = iconX + 20;
            if (indent > 0) {
                int edge = (colors == null ? secondary : colors.edge()) | 0xff000000;
                drawRect(15, y, 16, row.getBoolean("lastChild") ? y + 13 : y + ROW_HEIGHT, edge);
                drawRect(16, y + 12, 22, y + 13, edge);
            }
            if (group) WirelessCardPanel.drawArrow(variant, row.getBoolean("expanded"), 10 + indent, y + 4);
            if (icon != null) GuiDraw.drawItem(icon, iconX, y + 4, 16, 16, getContext().getCurrentDrawingZ());
            Platform.setupDrawFont();
            if (folder) {
                String count = "×" + row.getInteger("typeCount");
                text(deviceName(row), textX, y + 2, rowRight() - textX - font.getStringWidth(count) - 8, textColor);
                text(count, rowRight() - font.getStringWidth(count) - 4, y + 8, font.getStringWidth(count), secondary);
                text(
                    tr(
                        row.getBoolean("expanded") ? "gtneverreset.wireless.gui.group_collapse"
                            : "gtneverreset.wireless.gui.group_expand",
                        // #tr power.gui.group_size
                        // # %s devices
                        // # zh_CN %s 台设备
                        tr("power.gui.group_size", row.getInteger("typeCount"))),
                    textX,
                    y + 13,
                    rowRight() - textX - 32,
                    secondary);
                return;
            }
            String name = deviceName(row) + (group ? " [" + row.getInteger("members") + "]" : "");
            text(name, textX, y + 2, stateX() - textX - 4, textColor);
            text(position(row, true), textX, y + 13, stateX() - textX - 4, secondary);
            PowerState state = STATES[row.getInteger("state")];
            int color = stateColor(state, colors);
            text(stateLabel(state), stateX(), y + 8, 70, color);
            drawRect(stateX(), y + 19, stateX() + 36, y + 21, color | 0xff000000);
        }
    }

    private PowerState activeState() {
        return STATES[view().getInteger("state")];
    }

    private int stateCount(PowerState state) {
        int[] counts = view().getIntArray("stateCounts");
        return counts.length > state.ordinal() ? counts[state.ordinal()] : 0;
    }

    private void layoutChips() {
        WirelessCardFilters.layoutChips(chips, xSize, 56, i -> stateLabel(STATES[i]), i -> stateCount(STATES[i]));
    }

    private static String stateLabel(PowerState state) {
        return switch (state) {
            case ALL -> tr("gtneverreset.wireless.gui.filter.0");
            // #tr power.gui.connected
            // # Connected
            // # zh_CN 已连接
            case CONNECTED -> tr("power.gui.connected");
            // #tr power.gui.offline
            // # Chunk unloaded
            // # zh_CN 区块未加载
            case OFFLINE -> tr("power.gui.offline");
            // #tr power.gui.no_power
            // # No power
            // # zh_CN 电量不足
            case NO_POWER -> tr("power.gui.no_power");
            // #tr power.gui.paused
            // # Paused
            // # zh_CN 已暂停
            case PAUSED -> tr("power.gui.paused");
        };
    }

    private static int stateColor(PowerState state, Colors colors) {
        return switch (state) {
            case ALL -> colors == null ? ColorUtils.guiTextColorGray.getColor() : colors.text();
            case CONNECTED -> colors == null ? ColorUtils.craftConfirmPercent50.getColor() : colors.good();
            case OFFLINE -> colors == null ? ColorUtils.craftConfirmPercent25.getColor() : colors.warning();
            case NO_POWER, PAUSED -> colors == null ? ColorUtils.reshuffleStatusFailed.getColor() : colors.bad();
        };
    }

    private String selectedDimension() {
        return view().hasKey("dimension") ? Integer.toString(view().getInteger("dimension")) : "";
    }

    private String dimensionLabel(String id, boolean withCount) {
        if (id.isEmpty()) return tr("gtneverreset.wireless.gui.dimension_all");
        var options = view().getTagList("dimensions", 10);
        for (int i = 0; i < options.tagCount(); i++) {
            var option = options.getCompoundTagAt(i);
            int dim = option.getInteger("dimension");
            if (!id.equals(Integer.toString(dim))) continue;
            String name = localizedDimension(option.getString("name"), dim);
            return withCount ? tr("gtneverreset.wireless.gui.dimension_option", name, dim, option.getInteger("count"))
                : name.isEmpty() ? "D" + id : name;
        }
        return "D" + id;
    }

    private ItemStack icon(NBTTagCompound row) {
        return icons.computeIfAbsent(
            row.getInteger("metaId"),
            id -> id >= 0 && id < GregTechAPI.METATILEENTITIES.length && GregTechAPI.METATILEENTITIES[id] != null
                ? GregTechAPI.METATILEENTITIES[id].getStackForm(1)
                : null);
    }

    private String deviceName(NBTTagCompound row) {
        ItemStack icon = icon(row);
        return icon == null ? row.getString("name") : icon.getDisplayName();
    }

    private static String position(NBTTagCompound row, boolean localized) {
        String position = "D" + row.getInteger(
            "dimension") + "  " + row.getInteger("x") + ", " + row.getInteger("y") + ", " + row.getInteger("z");
        if (!localized) return position;
        String name = localizedDimension(row.getString("dimensionName"), row.getInteger("dimension"));
        return name.isEmpty() ? position : name + " " + position;
    }

    private static String localizedDimension(String name, int id) {
        if (name.equals("Personal World " + id) || name.equals("personalworld"))
            return tr("gtneverreset.wireless.gui.personal_world");
        return name.isEmpty() ? "" : DimensionHelper.getDimLocalizedName(name);
    }

    private void copyPosition(int index) {
        GuiScreen.setClipboardString(position(row(index), false));
        // #tr power.gui.copied
        // # Copied dimension and coordinates to the clipboard.
        // # zh_CN 已复制目标维度和坐标到剪贴板。
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentTranslation("power.gui.copied"));
    }

    private void text(String value, int x, int y, int width, int color) {
        font.drawString(font.trimStringToWidth(value, Math.max(0, width)), x, y, color);
    }

    private ButtonWidget<?> button(Supplier<String> label, int action, Supplier<String> target,
        BooleanSupplier available) {
        CardButton button = actionButton(() -> session.request(action, target.get()), available);
        IDrawable look = AE2Theme.button(button, available, () -> false);
        button.background(look)
            .hoverBackground(look)
            .overlay(AE2Theme.label(button, label, available, () -> false));
        return button;
    }

    private CardButton iconButton(Icon icon, Supplier<String> label, Runnable action, BooleanSupplier available) {
        CardButton button = actionButton(action, available);
        button.background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .overlay(
                icon == Icon.REFRESH ? AE2Theme.tabButton(button, () -> icon, available)
                    : AE2Theme.iconButton(button, () -> icon, available))
            .tooltipBuilder(tooltip -> tooltip.addLine(label.get()))
            .tooltipAutoUpdate(true);
        return button;
    }

    private CardButton actionButton(Runnable action, BooleanSupplier available) {
        CardButton button = new CardButton().onMousePressed(mouse -> {
            if (mouse == 0 && available.getAsBoolean()) action.run();
            return true;
        });
        child(button);
        return button;
    }

    private ToggleButton toggle(BooleanSupplier on, int action, Supplier<String> target) {
        ToggleButton button = new ToggleButton() {

            @Override
            public boolean canBeSeen(IViewportStack stack) {
                return isEnabled() && super.canBeSeen(stack);
            }

            @Override
            public @NotNull Result onMousePressed(int button) {
                return button == 0 ? super.onMousePressed(button) : Result.ACCEPT;
            }
        };
        IDrawable look = action != PAUSE ? AE2Theme.toggleSwitch(
            button,
            on,
            () -> action == MERGE_TYPES ? tr("gtneverreset.wireless.gui.merge_types")
                : tr(on.getAsBoolean() ? WirelessPowerCard.autoOnKey() : WirelessPowerCard.autoOffKey()),
            () -> true) : AE2Theme.iconButton(button, () -> on.getAsBoolean() ? Icon.PAUSE : Icon.PLAY, () -> true);
        button.value(new BoolValue.Dynamic(on, ignored -> session.request(action, target.get())))
            .background(false, IDrawable.EMPTY)
            .background(true, IDrawable.EMPTY)
            .hoverBackground(false, IDrawable.EMPTY)
            .hoverBackground(true, IDrawable.EMPTY)
            .overlay(false, look)
            .overlay(true, look);
        if (action == PAUSE) button.tooltipBuilder(tooltip -> {
            // #tr power.gui.pause
            // # Pause power supply
            // # zh_CN 暂停供电
            // #tr power.gui.resume
            // # Resume power supply
            // # zh_CN 恢复供电
            tooltip.addLine(tr(on.getAsBoolean() ? "power.gui.pause" : "power.gui.resume"));
        })
            .tooltipAutoUpdate(true);
        child(button);
        return button;
    }

    private NBTTagCompound row(int index) {
        return view().getTagList("rows", 10)
            .getCompoundTagAt(index);
    }

    private boolean sources() {
        return view().getBoolean("sources");
    }

    private boolean hasRow(int index) {
        return index < visibleRows && !row(index).hasNoTags();
    }

    private boolean device(int index) {
        return !sources() && hasRow(index)
            && !row(index).getBoolean("group")
            && row(index).getInteger("typeCount") == 0;
    }

    private boolean selected(int index) {
        return row(index).getString("id")
            .equals(
                session.display()
                    .getString("selected"));
    }

    private static String tr(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    private static String compact(String value) {
        return value.length() <= 10 ? value
            : value.substring(0, 1) + "." + value.substring(1, 4) + "e" + (value.length() - 1);
    }

    private static String rate(double value) {
        return value < 1_000_000_000 ? BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString() : String.format(Locale.ROOT, "%.3e", value);
    }

    private static final class CardButton extends ButtonWidget<CardButton> {

        @Override
        public boolean canBeSeen(IViewportStack stack) {
            // ModularUI draws a dragged panel with ignoreEnabled=true, including its hidden children.
            return isEnabled() && super.canBeSeen(stack);
        }
    }

    private static final class Portrait {

        private final GameProfile profile;
        private AbstractClientPlayer player;
        private ResourceLocation skin = SkinManager.field_152793_a, measuredSkin;
        private float logicalHeight = 64;

        private Portrait(String id, String name) {
            profile = new GameProfile(UUID.fromString(id), name);
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null && mc.theWorld.getPlayerEntityByName(name) instanceof AbstractClientPlayer online
                && online.getUniqueID()
                    .equals(profile.getId())) {
                player = online;
                return;
            }
            CompletableFuture.supplyAsync(
                () -> mc.func_152347_ac()
                    .fillProfileProperties(profile, true))
                .thenAccept(
                    resolved -> mc.func_152344_a(
                        () -> mc.func_152342_ad()
                            .func_152790_a(
                                resolved,
                                (type, texture) -> { if (type == MinecraftProfileTexture.Type.SKIN) skin = texture; },
                                true)))
                .exceptionally(failure -> null);
        }

        private void draw(int x, int y, int width, int height) {
            if (player != null) skin = player.getLocationSkin();
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(skin);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL11.GL_BLEND);
            if (!skin.equals(measuredSkin)) {
                int textureHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                int textureWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                logicalHeight = textureWidth > 0 ? 64f * textureHeight / textureWidth : 64;
                measuredSkin = skin;
            }
            Gui.func_152125_a(x, y, 8, 8, 8, 8, width, height, 64, logicalHeight);
            Gui.func_152125_a(x, y, 40, 8, 8, 8, width, height, 64, logicalHeight);
        }
    }
}
