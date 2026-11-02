package com.xyp.gtneverreset.client.wireless;

import static codechicken.nei.NEIClientUtils.cropText;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.BACK;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.COPY_POSITION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CYCLE_VISUALISATION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.DELETE_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.HIGHLIGHT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.NEXT_PAGE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_DETAILS;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_ENTRANCES;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.PREVIOUS_PAGE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.REFRESH;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.RENAME_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TELEPORT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_PAUSE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.isRowAction;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.rowIndex;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.type;
import static net.minecraft.client.gui.Gui.drawRect;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.core.mixins.early.minecraft.GuiScreenAccessor;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.client.wireless.WirelessCardGui.Layout;
import com.xyp.gtneverreset.client.wireless.WirelessCardGui.WirelessCardButton;
import com.xyp.gtneverreset.common.gui.container.WirelessCardContainer;
import com.xyp.gtneverreset.common.wireless.WirelessCardText;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;

import appeng.core.localization.ColorUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
abstract class WirelessCardPanel extends ModularPanel {

    public static final int ROW_HEIGHT = 24, CONTENT_X = 34;
    final String themeId = ScreenThemeConfig.wirelessCard.themeId;
    final AE2Theme.Variant variant = AE2Theme.variant(themeId);
    private boolean highlighted;
    public final Minecraft mc = Minecraft.getMinecraft();
    public final FontRenderer fontRendererObj = mc.fontRenderer;
    public final WirelessCardContainer container;
    public final List<WirelessCardButton> buttons = new ArrayList<>();
    public NBTTagCompound snapshot = new NBTTagCompound();
    public int xSize = 320, ySize = 232, visibleRows = Layout.MIN_ROWS;

    WirelessCardPanel(String name, WirelessCardContainer container) {
        super(name);
        this.container = container;
        for (int id : new int[] { PREVIOUS_PAGE, NEXT_PAGE, REFRESH, BACK }) addButton(id);

    }

    abstract boolean isDetailView();

    public abstract int rowTop();

    abstract void resizePanel(Layout layout);

    abstract void layoutButton(WirelessCardButton button, Layout layout);

    abstract boolean updatePanel();

    abstract void drawHeader(NBTTagCompound data, int color);

    abstract void confirm();

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
                    case OPEN_ENTRANCES -> button.pos(xSize - 106, y + 4)
                        .size(16, 16);
                    case TOGGLE_PAUSE -> button.pos(xSize - 28, y + 4)
                        .size(16, 16);
                    case OPEN_DETAILS -> button.pos(10, y)
                        .size(rowStateX() - 10, ROW_HEIGHT);
                    case RENAME_SOURCE -> button.pos(xSize - 180, y + 2)
                        .size(60, 20);
                    case DELETE_SOURCE -> button.pos(xSize - 52, y + 2)
                        .size(40, 20);
                    case COPY_POSITION -> button.pos(xSize - 52, y + 4)
                        .size(16, 16);
                    case HIGHLIGHT -> button.pos(xSize - 70, y + 4)
                        .size(16, 16);
                    case TELEPORT -> button.pos(xSize - 88, y + 4)
                        .size(16, 16);
                }
            } else switch (id) {
                case PREVIOUS_PAGE -> button.pos(10, ySize - 27)
                    .size(18, 20);
                case NEXT_PAGE -> button.pos(76, ySize - 27)
                    .size(18, 20);
                case REFRESH -> button.pos(xSize - 30, ySize - 27)
                    .size(20, 20);
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
            .navigate(id)) return;
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
        return new TextFieldWidget().setTooltipOverride(true)
            .value(new StringValue(""))
            .setMaxLength(max)
            .padding(2, 0);
    }

    int contentWidth() {
        return rowStateX() - CONTENT_X - 4;
    }

    void drawRowMetrics(NBTTagCompound data, NBTTagCompound row, int y) {
        int indent = row.getInteger("depth") * 8;
        drawMetrics(row, CONTENT_X + indent, y + 13, contentWidth() - indent, true);
        int missingColor = row.getBoolean("stable") && row.getInteger("missing") > 0 ? badColor() : secondary();
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

    private AE2Theme.Colors colors() {
        return (highlighted ? AE2Theme.Variant.LIGHT : variant).colors;
    }

    int foreground() {
        return variant == null ? ColorUtils.guiTextColorGray.getColor() : colors().text();
    }

    int secondary() {
        return variant == null ? ColorUtils.craftingTreeTask.getColor() : colors().secondary();
    }

    int goodColor() {
        return variant == null ? ColorUtils.craftConfirmPercent50.getColor() : colors().good();
    }

    int badColor() {
        return variant == null ? ColorUtils.reshuffleStatusFailed.getColor() : colors().bad();
    }

    int waitingColor() {
        return variant == null ? ColorUtils.craftConfirmPercent25.getColor() : colors().warning();
    }

    int stateColor(NBTTagCompound row) {
        if (row.getBoolean("paused") || row.getBoolean("conflicted")) return badColor();
        if (row.getBoolean("stable") && row.getInteger("missing") > 0) return badColor();
        if (!row.getBoolean("stable") || !row.getString("state")
            .equals("active")) return waitingColor();
        // A live entrance is not proof of power or active devices.
        return row.getInteger("devices") > 0 && row.getInteger("online") == row.getInteger("devices") ? goodColor()
            : foreground();
    }

    static String budgetText(NBTTagCompound data) {
        // #tr gtneverreset.wireless.gui.frequency_split
        // # Frequency split: take over one controller structure to reconnect.
        // # zh_CN 频率分裂：请接管一组控制器以恢复连接。
        if (data.getBoolean("frequencySplit")) return tr("frequency_split");
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
        int textColor = foreground();
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
        return xSize - 148;
    }

    void drawRowTitle(NBTTagCompound data, NBTTagCompound row, ItemStack icon, int y) {
        int textX = CONTENT_X + row.getInteger("depth") * 8;
        String name = (row.getBoolean("group") ? (row.getBoolean("expanded") ? "⌄ " : "> ") : "") + rowName(row, icon);
        String position = row.getString("positionLabel");
        String count = row.getBoolean("category") ? " [" + row.getInteger("items") + "]"
            : row.getBoolean("group") ? " [" + row.getInteger("clusters") + "]" : "";
        int width = contentWidth() - (textX - CONTENT_X) - fontRendererObj.getStringWidth(count);
        int nameColor = foreground();
        if (data.getBoolean("detail") || name.isEmpty()) {
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(position, width), textX, y, nameColor);
            return;
        }
        // Keep the device identifiable first; the row tooltip retains the full name and coordinates.
        String title = cropText(fontRendererObj, name, width);
        int positionWidth = width - fontRendererObj.getStringWidth(title) - fontRendererObj.getStringWidth(" ()");
        String suffix = !position.isEmpty() && positionWidth >= fontRendererObj.getStringWidth("...")
            ? " (" + cropText(fontRendererObj, position, positionWidth) + ")"
            : "";
        fontRendererObj.drawString(title, textX, y, nameColor);
        int nextX = textX + fontRendererObj.getStringWidth(title);
        if (!count.isEmpty()) {
            fontRendererObj.drawString(count, nextX, y, stateColor(row));
            nextX += fontRendererObj.getStringWidth(count);
        }
        fontRendererObj.drawString(suffix, nextX, y, secondary());
    }

    private void drawDeviceRow(NBTTagCompound row, ItemStack icon, int y) {
        String state = row.getString("deviceState");
        // #tr gtneverreset.wireless.gui.device_state.online
        // # Online
        // # zh_CN 在线
        // #tr gtneverreset.wireless.gui.device_state.offline
        // # Offline
        // # zh_CN 离线
        // #tr gtneverreset.wireless.gui.device_state.missing
        // # Missing channel
        // # zh_CN 缺少频道
        String status = tr("device_state." + state);
        int right = xSize - 130 - 4;
        int statusX = right - fontRendererObj.getStringWidth(status);
        int color = "online".equals(state) ? goodColor() : "missing".equals(state) ? badColor() : waitingColor();
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(rowName(row, icon), right - CONTENT_X),
            CONTENT_X,
            y + 2,
            foreground());
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(row.getString("positionLabel"), Math.max(0, statusX - CONTENT_X - 4)),
            CONTENT_X,
            y + 13,
            secondary());
        fontRendererObj.drawString(status, statusX, y + 13, color);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            if (variant != null) {
                GL11.glColor4f(1, 1, 1, 1);
                variant.inset.draw(
                    8,
                    rowTop() - AE2Theme.INSET_TOP,
                    xSize - 16,
                    visibleRows * ROW_HEIGHT + AE2Theme.INSET_TOP + AE2Theme.INSET_EDGE);
            }
            int mouseX = context.getAbsMouseX() - getArea().x, mouseY = context.getAbsMouseY() - getArea().y - rowTop();
            int hovered = isBelowMouse() && mouseX >= 9 && mouseX < xSize - 9 && mouseY >= 0 ? mouseY / ROW_HEIGHT : -1;
            drawForeground(hovered);
        } finally {
            highlighted = false;
            GL11.glPopAttrib();
        }
    }

    private void drawForeground(int hovered) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Platform.setupDrawFont();
        NBTTagCompound data = view();
        int textColor = foreground();
        drawHeader(data, textColor);
        if (!data.getBoolean("sourcesPage")) drawMetrics(data, 10, isDetailView() ? 47 : 37, xSize - 20, false);
        NBTTagList rows = data.getTagList("rows", 10);
        for (int index = 0; index < Math.min(visibleRows, rows.tagCount()); index++) {
            NBTTagCompound row = rows.getCompoundTagAt(index);
            int y = rowTop() + index * ROW_HEIGHT;
            ItemStack icon = ItemStack.loadItemStackFromNBT(row.getCompoundTag("icon"));
            highlighted = index == hovered || row.getBoolean("expanded");
            if (highlighted) {
                drawRect(9, y, xSize - 9, y + ROW_HEIGHT - 1, variant == null ? 0x60ffffff : AE2Theme.HIGHLIGHT);
                drawRect(9, y, xSize - 9, y + 1, variant == null ? 0x80ffffff : 0xffdaffff);
                drawRect(
                    9,
                    y + ROW_HEIGHT - 2,
                    xSize - 9,
                    y + ROW_HEIGHT - 1,
                    variant == null ? 0x40000000 : AE2Theme.HIGHLIGHT_SHADOW);
            }
            drawRect(
                9,
                y + ROW_HEIGHT - 1,
                xSize - 9,
                y + ROW_HEIGHT,
                variant == null ? ColorUtils.craftingDiagnosticTerminalLine.getColor() : variant.colors.line());
            textColor = foreground();
            int depth = row.getInteger("depth");
            int iconX = 12 + 8 * depth;
            if (icon != null) drawDeviceIcon(icon, iconX, y + 4);
            if (depth > 0) {
                int lineX = 9 + 8 * (depth - 1);
                int lineColor = textColor | 0xff000000;
                drawRect(
                    lineX,
                    y,
                    lineX + 1,
                    y + (row.getBoolean("treeLast") ? ROW_HEIGHT / 2 : ROW_HEIGHT),
                    lineColor);
                drawRect(lineX, y + ROW_HEIGHT / 2, lineX + 5, y + ROW_HEIGHT / 2 + 1, lineColor);
                if ((row.getInteger("treeLines") & 1) != 0) drawRect(9, y, 10, y + ROW_HEIGHT, lineColor);
            }
            if (row.getBoolean("group")) {
                int color = secondary() | 0xff000000;
                for (int step = 0; step < 4; step++) {
                    if (row.getBoolean("expanded"))
                        drawRect(xSize - 25 + step, y + 10 + step, xSize - 18 - step, y + 11 + step, color);
                    else {
                        drawRect(xSize - 24 + step, y + 8 + step, xSize - 22 + step, y + 9 + step, color);
                        drawRect(xSize - 24 + step, y + 15 - step, xSize - 22 + step, y + 16 - step, color);
                    }
                }
            }
            if (row.getBoolean("device")) {
                drawDeviceRow(row, icon, y);
                continue;
            }
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
        highlighted = false;
        textColor = foreground();
        if (rows.tagCount() == 0) drawEmptyMessage(data, textColor);
        String page = fontRendererObj
            .trimStringToWidth((data.getInteger("page") + 1) + "/" + Math.max(1, data.getInteger("pages")), 40);
        fontRendererObj.drawString(page, 52 - fontRendererObj.getStringWidth(page) / 2, ySize - 20, textColor);
        drawFooter(data, textColor);
    }

}
