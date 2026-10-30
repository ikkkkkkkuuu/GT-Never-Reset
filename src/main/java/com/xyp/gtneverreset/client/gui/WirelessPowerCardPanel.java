package com.xyp.gtneverreset.client.gui;

import static com.xyp.gtneverreset.common.gui.WirelessPowerCardGui.*;
import static net.minecraft.client.gui.Gui.drawRect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
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
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
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
import com.xyp.gtneverreset.client.gui.WirelessCardGui.Layout;
import com.xyp.gtneverreset.common.gui.WirelessCardGuiTextures;
import com.xyp.gtneverreset.common.gui.WirelessPowerCardGui.Session;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;

import appeng.client.gui.ScreenColor;
import appeng.core.localization.ColorUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gtneioreplugin.util.DimensionHelper;

@SideOnly(Side.CLIENT)
public final class WirelessPowerCardPanel extends ModularPanel {

    private static final int ROW_TOP = 82, ROW_HEIGHT = 24;
    private final Session session;
    private final boolean details;
    private final IPanelHandler detailsPanel;
    private NBTTagCompound snapshot = new NBTTagCompound();
    private boolean closing;
    private final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    private final Map<String, Portrait> portraits = new HashMap<>();
    private final Map<Integer, ItemStack> icons = new HashMap<>();
    private final ButtonWidget<?> previous, next, back, refresh, sourceLink;
    private final ToggleButton automatic;
    private final ButtonWidget<?>[] select = new ButtonWidget<?>[MAX_ROWS], sourceRows = new ButtonWidget<?>[MAX_ROWS],
        remove = new ButtonWidget<?>[MAX_ROWS], teleport = new ButtonWidget<?>[MAX_ROWS],
        copy = new ButtonWidget<?>[MAX_ROWS], highlight = new ButtonWidget<?>[MAX_ROWS],
        deviceRows = new ButtonWidget<?>[MAX_ROWS];
    private final ToggleButton[] power = new ToggleButton[MAX_ROWS];
    private final TextFieldWidget search;
    private String searchText = "";
    private int xSize = 320, ySize = 232, visibleRows = MIN_ROWS, requestedRows;

    public WirelessPowerCardPanel(Session session) {
        this(session, false);
    }

    private WirelessPowerCardPanel(Session session, boolean details) {
        super(details ? "wireless_power_details" : "wireless_power_card");
        this.session = session;
        this.details = details;
        detailsPanel = details ? null
            : IPanelHandler.simple(this, (parent, player) -> new WirelessPowerCardPanel(session, true), true);
        if (details) child(ButtonWidget.panelCloseButton());
        background(WirelessCardGuiTextures.BACKGROUND);
        previous = button(
            () -> "<",
            PREVIOUS,
            () -> "",
            () -> session.display()
                .getInteger("page") > 0);
        next = button(
            () -> ">",
            NEXT,
            () -> "",
            () -> session.display()
                .getInteger("page") + 1 < session.display()
                    .getInteger("pages"));
        // #tr power.gui.back
        // # Back
        // # zh_CN 返回
        back = button(() -> tr("power.gui.back"), details ? CLOSE : DEVICES, () -> "", () -> true);
        back.setEnabledIf(widget -> details || sources());
        // #tr power.gui.refresh
        // # Refresh
        // # zh_CN 刷新
        refresh = button(() -> tr("power.gui.refresh"), REFRESH, () -> "", () -> true);
        sourceLink = new CardButton().background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .onMousePressed(mouse -> {
                if (mouse == 0) session.request(SOURCES, "");
                return true;
            })
            .setEnabledIf(widget -> !details && !sources());
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
        }.background(
            new Rectangle().color(0xff000000),
            new Rectangle().color(0xffa0a0a0)
                .hollow(1))
            .setMaxLength(64)
            .setTextColor(ColorUtils.searchboxText.getColor())
            .padding(2, 0)
            .hintText(tr("power.gui.search"))
            .hintColor(ColorUtils.craftingTreeRequest.getColor())
            .autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> searchText, text -> {
                if (searchText.equals(text)) return;
                searchText = text;
                session.request(FILTER, text);
            }));
        search.setEnabledIf(widget -> !details && !sources());
        child(search);
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
            remove[i] = button(() -> tr("power.gui.remove"), REMOVE, () -> row(index).getString("id"), () -> true);
            remove[i].setEnabledIf(widget -> device(index));
            // #tr power.gui.teleport
            // # Teleport
            // # zh_CN 传送
            teleport[i] = button(
                () -> tr("power.gui.teleport"),
                TELEPORT,
                () -> row(index).getString("id"),
                () -> row(index).getBoolean("loaded"));
            teleport[i].setEnabledIf(widget -> device(index));
            // #tr power.gui.copy
            // # Copy
            // # zh_CN 复制
            copy[i] = button(() -> tr("power.gui.copy"), () -> copyPosition(index), () -> true);
            copy[i].setEnabledIf(widget -> !sources() && hasRow(index));
            // #tr power.gui.highlight
            // # Highlight
            // # zh_CN 高亮
            highlight[i] = button(
                () -> tr("power.gui.highlight"),
                HIGHLIGHT,
                () -> row(index).getString("id"),
                () -> row(index).getBoolean("loaded"));
            highlight[i].setEnabledIf(widget -> device(index));
            power[i] = toggle(() -> !row(index).getBoolean("paused"), PAUSE, () -> row(index).getString("id"));
            power[i].setEnabledIf(widget -> device(index));
            deviceRows[i] = new CardButton().background(IDrawable.EMPTY)
                .hoverBackground(IDrawable.EMPTY)
                .onMousePressed(mouse -> {
                    if (mouse == 0 && row(index).getBoolean("group")) session.request(OPEN, row(index).getString("id"));
                    return true;
                })
                .setEnabledIf(widget -> !sources() && hasRow(index));
            deviceRows[i].tooltipBuilder(tooltip -> {
                NBTTagCompound row = row(index);
                tooltip.addLine(deviceName(row));
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
        };
    }

    private void resizeLayout(int width, int height) {
        Layout layout = Layout.forScreen(width, height);
        xSize = layout.width() - (details ? 24 : 0);
        ySize = layout.height() - (details ? 22 : 0);
        visibleRows = layout.rows();
        size(xSize, ySize);
        previous.pos(10, ySize - 27)
            .size(18, 20);
        next.pos(76, ySize - 27)
            .size(18, 20);
        back.pos(layout.footerInfoX(), ySize - 27)
            .size(36, 20);
        refresh.pos(xSize - 76, ySize - 27)
            .size(64, 20);
        sourceLink.pos(8, 23)
            .size(xSize - 16, 12);
        automatic.pos(xSize - 28, 10)
            .size(16, 16);
        search.pos(10, 62)
            .size(xSize - 20, 12);
        for (int i = 0; i < MAX_ROWS; i++) {
            int y = rowTop() + i * ROW_HEIGHT;
            select[i].pos(xSize - 76, y + 2)
                .size(64, 20);
            sourceRows[i].pos(10, y)
                .size(xSize - 94, ROW_HEIGHT);
            teleport[i].pos(xSize - 164, y + 2)
                .size(30, 20);
            copy[i].pos(xSize - 130, y + 2)
                .size(30, 20);
            highlight[i].pos(xSize - 96, y + 2)
                .size(30, 20);
            remove[i].pos(xSize - 62, y + 2)
                .size(30, 20);
            power[i].pos(xSize - 28, y + 4)
                .size(16, 16);
            deviceRows[i].pos(10, y)
                .size(xSize - 220, ROW_HEIGHT);
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
        if (details && key == Keyboard.KEY_ESCAPE) {
            closeIfOpen();
            return true;
        }
        return super.onKeyPressed(character, key);
    }

    private int rowTop() {
        return details ? 60 : ROW_TOP;
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
            int text = ColorUtils.guiTextColorGray.getColor();
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
                text(tr("item.gtneverreset.wireless_power_card.name"), 10, 11, xSize - 140, text);
                String auto = tr(
                    view().getBoolean("auto") ? WirelessPowerCard.autoOnKey() : WirelessPowerCard.autoOffKey());
                text(auto, xSize - 32 - font.getStringWidth(auto), 14, 94, text);
                // #tr power.gui.selected
                // # Power source: %s
                // # zh_CN 供电源：%s
                text("> " + tr("power.gui.selected", view().getString("sourceName")), 10, 25, xSize - 20, text);
                text(
                    compact(view().getString("balance")) + " EU",
                    10,
                    36,
                    xSize - 20,
                    ColorUtils.craftConfirmPercent50.getColor());
                // #tr power.gui.devices
                // # Connected devices: %s
                // # zh_CN 已接入设备：%s
                text(tr("power.gui.devices", view().getInteger("total")), 10, 47, xSize - 20, text);
            }
            for (int i = 0; i < visibleRows && hasRow(i); i++) drawRow(i, text);
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
            String page = (view().getInteger("page") + 1) + "/" + Math.max(1, view().getInteger("pages"));
            font.drawString(page, 52 - font.getStringWidth(page) / 2, ySize - 20, text);
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void drawRow(int index, int textColor) {
        NBTTagCompound row = row(index);
        int y = rowTop() + index * ROW_HEIGHT;
        drawRect(
            8,
            y + ROW_HEIGHT - 1,
            xSize - 8,
            y + ROW_HEIGHT,
            ColorUtils.craftingDiagnosticTerminalLine.getColor());
        if (sources()) {
            portraits.computeIfAbsent(row.getString("id"), key -> new Portrait(key, row.getString("name")))
                .draw(12, y + 4, 16, 16);
            Platform.setupDrawFont();
            text(row.getString("name"), 34, y + 2, xSize - 126, textColor);
            text(compact(row.getString("balance")) + " EU", 34, y + 13, xSize - 126, textColor);
        } else {
            ItemStack icon = icon(row);
            if (icon != null) GuiDraw.drawItem(icon, 12, y + 4, 16, 16, getContext().getCurrentDrawingZ());
            Platform.setupDrawFont();
            String name = (row.getBoolean("group") ? "> " : "") + deviceName(row)
                + (row.getBoolean("group") ? " [" + row.getInteger("members") + "]" : "");
            text(name, 34, y + 2, xSize - 244, ColorUtils.guiTextColorBlack.getColor());
            text(position(row, true), 34, y + 13, xSize - 244, textColor);
            // #tr power.gui.paused
            // # Paused
            // # zh_CN 已暂停
            // #tr power.gui.offline
            // # Chunk unloaded
            // # zh_CN 区块未加载
            // #tr power.gui.connected
            // # Connected
            // # zh_CN 已连接
            // #tr power.gui.no_power
            // # No power
            // # zh_CN 电量不足
            String status = row.getBoolean("paused") ? tr("power.gui.paused")
                : !row.getBoolean("loaded") ? tr("power.gui.offline")
                    : !row.getBoolean("ready") ? tr("power.gui.no_power") : tr("power.gui.connected");
            int color = !row.getBoolean("paused") && row.getBoolean("loaded") && row.getBoolean("ready")
                ? ColorUtils.craftConfirmPercent50.getColor()
                : textColor;
            text(status, xSize - 206, y + 8, 40, color);
            drawRect(xSize - 206, y + 19, xSize - 170, y + 21, color | 0xff000000);
        }
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
        String name = row.getString("dimensionName");
        if (name.equals("Personal World " + row.getInteger("dimension")) || name.equals("personalworld"))
            name = tr("gtneverreset.wireless.gui.personal_world");
        else if (!name.isEmpty()) name = DimensionHelper.getDimLocalizedName(name);
        return name.isEmpty() ? position : name + " " + position;
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
        return button(label, () -> session.request(action, target.get()), available);
    }

    private ButtonWidget<?> button(Supplier<String> label, Runnable action, BooleanSupplier available) {
        ButtonWidget<?> button = new CardButton();
        IDrawable background = (context, x, y, width, height,
            theme) -> tinted(
                WirelessCardGuiTextures.BUTTONS[!available.getAsBoolean() ? 0 : button.isHovering() ? 2 : 1])
                    .draw(context, x, y, width, height, theme);
        button.background(background)
            .hoverBackground(background)
            .overlay(
                IKey.dynamic(() -> font.trimStringToWidth(label.get(), Math.max(0, button.getArea().width - 4)))
                    .color(() -> !available.getAsBoolean() ? 0xffa0a0a0 : button.isHovering() ? 0xffffffa0 : 0xffe0e0e0)
                    .shadow(true))
            .onMousePressed(mouse -> {
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
        }.value(new BoolValue.Dynamic(on, ignored -> session.request(action, target.get())))
            .background(false, tinted(WirelessCardGuiTextures.TOGGLE_BACKGROUND))
            .background(true, tinted(WirelessCardGuiTextures.TOGGLE_BACKGROUND))
            .hoverBackground(false, tinted(WirelessCardGuiTextures.TOGGLE_BACKGROUND))
            .hoverBackground(true, tinted(WirelessCardGuiTextures.TOGGLE_BACKGROUND))
            .overlay(false, WirelessCardGuiTextures.TOGGLE_OFF)
            .overlay(true, WirelessCardGuiTextures.TOGGLE_ON);
        child(button);
        return button;
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
        return !sources() && hasRow(index) && !row(index).getBoolean("group");
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
