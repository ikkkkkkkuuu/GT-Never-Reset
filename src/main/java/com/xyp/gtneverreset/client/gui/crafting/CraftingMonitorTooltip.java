package com.xyp.gtneverreset.client.gui.crafting;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.api.mixinHelper.CraftingMonitorScreen;
import com.xyp.gtneverreset.common.packet.CraftingMonitorCellProgressPacket;
import com.xyp.gtneverreset.common.packet.CraftingMonitorRequestPacket;
import com.xyp.gtneverreset.common.packet.CraftingMonitorRequestPacket.Action;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Machine;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Position;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Result;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;

import appeng.api.storage.data.IAEStack;
import appeng.api.util.NamedDimensionalCoord;
import appeng.client.gui.implementations.GuiCraftingCPU;
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.core.localization.PlayerMessages;
import appeng.util.ReadableNumberConverter;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcp.mobius.waila.overlay.tooltiprenderers.TTRenderBar;

@SideOnly(Side.CLIENT)
public final class CraftingMonitorTooltip {

    private static final long HOVER_DELAY_MS = 300;
    private static final long REFRESH_INTERVAL_MS = 500;
    private static final long REPLY_TIMEOUT_MS = 2000;
    private static final long PROGRESS_ANIMATION_MS = 180;
    private static final Variant SKIN = Variant.DARK_PURPLE;
    private static final AE2Look LOOK = AE2Look.of(SKIN);
    private static final int MAX_TOOLTIP_TARGETS = 2;
    private static final int MENU_WIDTH = 154;
    private static final int MENU_ROW_HEIGHT = 14;
    private static final int MENU_HEIGHT = MENU_ROW_HEIGHT * 5 + 6;
    private static int nextRequestId;

    private final CraftingMonitorCellOverlay cells = new CraftingMonitorCellOverlay();
    private IAEStack<?> hovered;
    private IAEStack<?> observed;
    private List<Machine> machines = List.of();
    private OutputProgress progress;
    private Card card;
    private long nextRefresh;
    private long replyDeadline;
    private int statusRequest;
    private int snapshotId;
    private int actionRequest;
    private Menu menu;

    public static void accept(CraftingMonitorSnapshotPacket packet) {
        Minecraft minecraft = Minecraft.getMinecraft();
        CraftingMonitorTooltip monitor = currentMonitor(minecraft, packet.windowId());
        if (monitor != null) monitor.apply(packet, minecraft);
    }

    public static void acceptCells(CraftingMonitorCellProgressPacket packet) {
        CraftingMonitorTooltip monitor = currentMonitor(Minecraft.getMinecraft(), packet.windowId());
        if (monitor != null) monitor.cells.apply(packet);
    }

    private static CraftingMonitorTooltip currentMonitor(Minecraft minecraft, int windowId) {
        if (minecraft.thePlayer == null || !(minecraft.currentScreen instanceof GuiCraftingCPU gui)
            || minecraft.thePlayer.openContainer != gui.inventorySlots
            || gui.inventorySlots.windowId != windowId
            || !(gui instanceof CraftingMonitorScreen screen)) return null;
        return screen.gtnr$craftingMonitor();
    }

    public void beginFrame() {
        hovered = null;
        card = null;
        cells.beginFrame();
    }

    public boolean drawCellProgress(IAEStack<?> stack, int x, int y, int width, int height, int stateColor) {
        return cells.draw(stack, x, y, width, height, stateColor);
    }

    public String prepareCard(IAEStack<?> stack, int windowId, String nativeTooltip, List<String> lines,
        int amountLines, int width, int height, FontRenderer font) {
        if (menu != null) return "";
        if (stack == null) return nativeTooltip;
        hovered = stack;
        long now = Minecraft.getSystemTime();
        if (observed == null || !stack.equals(observed)) {
            resetObservation();
            observed = stack.copy();
            nextRefresh = now + HOVER_DELAY_MS;
        }
        if (statusRequest != 0 && now >= replyDeadline) {
            statusRequest = 0;
            machines = List.of();
            progress = null;
        }
        if (statusRequest == 0 && now >= nextRefresh) {
            statusRequest = send(windowId, stack, Action.STATUS, 0);
            replyDeadline = now + REPLY_TIMEOUT_MS;
            nextRefresh = now + REFRESH_INTERVAL_MS;
        }
        if (machines.isEmpty() && progress == null) return nativeTooltip;
        int titleEnd = nativeTooltip.indexOf('\n');
        String title = titleEnd < 0 ? nativeTooltip : nativeTooltip.substring(0, titleEnd);
        card = Card.create(
            title,
            lines.subList(0, amountLines),
            lines.subList(amountLines, lines.size()),
            width,
            height,
            font,
            machines,
            progress);
        return card == null ? nativeTooltip : "";
    }

    public void endFrame(int mouseX, int mouseY, int width, int height, int windowId, FontRenderer font) {
        cells.endFrame(windowId);
        if (hovered == null && menu == null) resetObservation();
        if (menu == null && card == null) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1, 1, 1, 1);
            if (menu != null) menu.draw(mouseX, mouseY, font);
            else card.draw(mouseX, mouseY, width, height, font);
        } finally {
            GL11.glPopAttrib();
        }
    }

    public boolean click(int mouseX, int mouseY, int button, int windowId, int left, int top, int width, int height) {
        if (menu != null) {
            Menu current = menu;
            if (button != 0 || !current.contains(mouseX, mouseY)) {
                closeMenu();
                return true;
            }
            int selection = (mouseY - current.y - 3) / MENU_ROW_HEIGHT;
            Machine target = current.machines.get(current.row);
            switch (selection) {
                case 0 -> current.row = (current.row + 1) % current.machines.size();
                case 1 -> select(Action.LOCATE_PROVIDER, target.provider() != null, windowId);
                case 2 -> select(Action.OPEN_MACHINE, target.canOpenMachine(), windowId);
                case 3 -> select(Action.OPEN_PROVIDER, target.canOpenProvider(), windowId);
                case 4 -> closeMenu();
                default -> {}
            }
            return true;
        }
        if (button != 1 || GuiScreen.isShiftKeyDown()
            || hovered == null
            || observed == null
            || !hovered.equals(observed)
            || machines.isEmpty()) {
            return false;
        }
        int x = Math.max(left, Math.min(mouseX, left + width - MENU_WIDTH));
        int y = Math.max(top, Math.min(mouseY, top + height - MENU_HEIGHT));
        menu = new Menu(x, y, observed, machines, snapshotId);
        return true;
    }

    public boolean keyPressed(int key) {
        if (menu == null || key != Keyboard.KEY_ESCAPE) return false;
        closeMenu();
        return true;
    }

    public void clear() {
        resetObservation();
        cells.clear();
        menu = null;
        card = null;
        hovered = null;
        actionRequest = 0;
    }

    private void apply(CraftingMonitorSnapshotPacket packet, Minecraft minecraft) {
        if (packet.result() == Result.STATUS && packet.requestId() == statusRequest) {
            statusRequest = 0;
            machines = packet.machines();
            progress = packet.total() > 0
                ? OutputProgress.sample(progress, packet.completed(), packet.total(), Minecraft.getSystemTime())
                : null;
            snapshotId = packet.requestId();
            return;
        }
        if (packet.requestId() != actionRequest) return;
        actionRequest = 0;
        if (packet.result() == Result.LOCATED && packet.position() != null) {
            Position pos = packet.position();
            NamedDimensionalCoord coord = new NamedDimensionalCoord(pos.x(), pos.y(), pos.z(), pos.dimension(), "");
            String[] messages = { PlayerMessages.MachineHighlightedNamed.getUnlocalized(),
                PlayerMessages.MachineInOtherDimNamed.getUnlocalized() };
            BlockPosHighlighter.highlightNamedBlocks(minecraft.thePlayer, Map.of(coord, messages), providerName());
            minecraft.thePlayer.closeScreen();
        } else if (packet.result() == Result.DENIED) {
            // #tr gtnr.gui.crafting_monitor.unavailable
            // # That crafting target is no longer available or cannot be accessed here.
            // # zh_CN 此合成目标已失效，或无法从当前位置访问。
            minecraft.thePlayer.addChatMessage(new ChatComponentTranslation("gtnr.gui.crafting_monitor.unavailable"));
        }
    }

    private void select(Action action, boolean enabled, int windowId) {
        if (!enabled) return;
        actionRequest = menu.snapshotId;
        NetWorkHandler
            .sendToServer(new CraftingMonitorRequestPacket(windowId, actionRequest, menu.stack, action, menu.row));
        closeMenu();
    }

    private void closeMenu() {
        menu = null;
        resetObservation();
    }

    private void resetObservation() {
        observed = null;
        machines = List.of();
        progress = null;
        statusRequest = 0;
        snapshotId = 0;
    }

    private static int send(int windowId, IAEStack<?> stack, Action action, int row) {
        int id = ++nextRequestId;
        if (id == 0) id = ++nextRequestId;
        NetWorkHandler.sendToServer(new CraftingMonitorRequestPacket(windowId, id, stack, action, row));
        return id;
    }

    private static String displayName(Machine machine) {
        return machine.name()
            .isEmpty() ? providerName() : I18n.format(machine.name());
    }

    private static String providerName() {
        // #tr gtnr.gui.crafting_monitor.provider
        // # Pattern provider
        // # zh_CN 样板供应器
        return I18n.format("gtnr.gui.crafting_monitor.provider");
    }

    private static String formatPosition(Position pos) {
        // #tr gtnr.gui.crafting_monitor.position
        // # Dim %s (%s, %s, %s)
        // # zh_CN 维度 %s（%s, %s, %s）
        return I18n.format("gtnr.gui.crafting_monitor.position", pos.dimension(), pos.x(), pos.y(), pos.z());
    }

    record OutputProgress(long completed, long total, double from, long receivedAt) {

        static OutputProgress sample(OutputProgress previous, long completed, long total, long now) {
            double from = previous != null && previous.total == total && previous.completed <= completed
                ? previous.valueAt(now)
                : fraction(completed, total);
            return new OutputProgress(completed, total, from, now);
        }

        private double fraction() {
            return fraction(completed, total);
        }

        private static double fraction(long completed, long total) {
            return completed == total ? 1 : Math.min(Math.nextDown(1.0), (double) completed / total);
        }

        double valueAt(long now) {
            double elapsed = Math.max(0, Math.min(1, (double) (now - receivedAt) / PROGRESS_ANIMATION_MS));
            return from + (fraction() - from) * elapsed;
        }
    }

    private record Card(String title, int width, int height, List<Line> lines) {

        private static final int MAX_WIDTH = 228;
        private static final int EDGE = 4;
        private static final int PADDING = 7;
        private static final int POINTER_GAP = 12;
        private static final int LINE_HEIGHT = 13;
        private static final int COLUMN_GAP = 8;
        private static final int BAR_HEIGHT = 12;
        private static final NumberFormat QUANTITIES = NumberFormat.getIntegerInstance();

        private static Card create(String title, List<String> amounts, List<String> details, int screenWidth,
            int screenHeight, FontRenderer font, List<Machine> machines, OutputProgress progress) {
            int width = Math.min(MAX_WIDTH, screenWidth - 2 * EDGE);
            int contentWidth = width - 2 * PADDING;
            int maxRows = (screenHeight - 2 * (EDGE + PADDING)) / LINE_HEIGHT;
            if (contentWidth <= 0 || maxRows <= 0) return null;

            List<Line> lines = new ArrayList<>();
            lines.add(new Text(title, SKIN.colors.fieldText()));
            for (String amount : amounts) lines.add(new Text(amount, LOOK.listText()));
            long linked = machines.stream()
                .map(Machine::machine)
                .filter(Objects::nonNull)
                .distinct()
                .count();
            if (linked > 0) {
                // #tr gtnr.gui.crafting_monitor.linked_machines
                // # Linked machines: %s
                // # zh_CN 关联机器：%s 台
                lines.add(new Text(I18n.format("gtnr.gui.crafting_monitor.linked_machines", linked), LOOK.listMuted()));
            }
            if (progress != null) {
                Machine running = machines.stream()
                    .filter(machine -> machine.active() && machine.duration() > 0)
                    .findFirst()
                    .orElse(null);
                appendProgress(lines, progress, running, contentWidth, font);
            }
            for (int index = 0; index < Math.min(machines.size(), MAX_TOOLTIP_TARGETS); index++) {
                lines.add(Divider.INSTANCE);
                appendMachine(lines, machines.get(index), contentWidth, font);
            }
            if (machines.size() > MAX_TOOLTIP_TARGETS) {
                // #tr gtnr.gui.crafting_monitor.additional
                // # %s more targets
                // # zh_CN 另有 %s 个目标
                lines.add(
                    new Text(
                        I18n.format("gtnr.gui.crafting_monitor.additional", machines.size() - MAX_TOOLTIP_TARGETS),
                        LOOK.listMuted()));
            }
            int bodyRows = machines.isEmpty() ? maxRows : maxRows - 1;
            for (String detail : details) {
                for (String line : font.listFormattedStringToWidth(detail, contentWidth)) {
                    lines.add(new Text(line, LOOK.listMuted()));
                    if (lines.size() > bodyRows) break;
                }
                if (lines.size() > bodyRows) break;
            }
            if (lines.size() > bodyRows) {
                if (GuiScreen.isShiftKeyDown()) return null;
                lines.subList(bodyRows, lines.size())
                    .clear();
            }
            if (!machines.isEmpty()) {
                // #tr gtnr.gui.crafting_monitor.actions
                // # Right-click for machine and pattern actions
                // # zh_CN 右键查看机器与样板操作
                lines.add(new Text(I18n.format("gtnr.gui.crafting_monitor.actions"), LOOK.listDetail()));
            }
            return new Card(title, width, 2 * PADDING + lines.size() * LINE_HEIGHT, lines);
        }

        private void draw(int mouseX, int mouseY, int screenWidth, int screenHeight, FontRenderer font) {
            int x = mouseX + POINTER_GAP;
            if (x + width + EDGE > screenWidth) x = mouseX - POINTER_GAP - width;
            x = Math.max(EDGE, Math.min(x, screenWidth - width - EDGE));
            int y = Math.max(EDGE, Math.min(mouseY - PADDING, screenHeight - height - EDGE));

            SKIN.panel.draw(x, y, width, height);
            for (int index = 0; index < lines.size(); index++) {
                lines.get(index)
                    .draw(x + PADDING, y + PADDING + index * LINE_HEIGHT, width - 2 * PADDING, font);
            }
        }

        private static void appendProgress(List<Line> lines, OutputProgress progress, Machine running, int width,
            FontRenderer font) {
            int percent = progress.completed() == progress.total() ? 100
                : Math.min(99, (int) (100 * progress.fraction()));
            // #tr gtnr.gui.crafting_monitor.total_percent
            // # Task progress: %s%%
            // # zh_CN 任务总进度：%s%%
            String heading = I18n.format("gtnr.gui.crafting_monitor.total_percent", percent);
            int available = running == null ? width - 4 : width - font.getStringWidth(heading) - COLUMN_GAP;
            String amount = QUANTITIES.format(progress.completed()) + " / " + QUANTITIES.format(progress.total());
            if (font.getStringWidth(amount) > available) {
                amount = ReadableNumberConverter.INSTANCE.toWideReadableForm(progress.completed()) + " / "
                    + ReadableNumberConverter.INSTANCE.toWideReadableForm(progress.total());
            }
            double fill = progress.valueAt(Minecraft.getSystemTime());
            if (running == null) {
                lines.add(new Text(heading, LOOK.listText()));
                lines.add(new Bar(amount, fill));
            } else {
                lines.add(new Text(heading, LOOK.listText(), amount, LOOK.listDetail()));
                lines.add(new Bar(formatRoundTime(running), fill));
            }
        }

        private static String formatRoundTime(Machine machine) {
            int duration = machine.duration();
            int progress = Math.max(0, Math.min(machine.progress(), duration));
            String time = duration < 20 ? I18n.format("GT5U.waila.machine.progress_tick", progress, duration)
                : I18n.format(
                    "GT5U.waila.machine.progress_second",
                    String.format(Locale.ROOT, "%.2f", progress / 20.0),
                    String.format(Locale.ROOT, "%.2f", duration / 20.0));
            // #tr gtnr.gui.crafting_monitor.round_time
            // # Cycle %s
            // # zh_CN 本轮 %s
            return I18n.format("gtnr.gui.crafting_monitor.round_time", time);
        }

        private static void appendMachine(List<Line> lines, Machine machine, int contentWidth, FontRenderer font) {
            if (machine.machine() == null) {
                lines.add(new Text(displayName(machine), LOOK.listText()));
                if (machine.provider() != null) {
                    lines.add(new Text(formatPosition(machine.provider()), LOOK.listMuted()));
                }
                return;
            }
            lines.add(
                new Text(
                    displayName(machine),
                    SKIN.colors.fieldText(),
                    stateText(machine),
                    machine.active() ? LOOK.good() : LOOK.listMuted()));
            if (machine.active() && machine.parallel() > 0) {
                // #tr gtnr.gui.crafting_monitor.parallel
                // # Actual parallel: %s
                // # zh_CN 实际并行：%s
                lines.add(
                    new Text(I18n.format("gtnr.gui.crafting_monitor.parallel", machine.parallel()), LOOK.listText()));
            }
            if (!machine.active() && !machine.shutdown()
                .isEmpty()) {
                // #tr gtnr.gui.crafting_monitor.shutdown
                // # Stop reason: %s
                // # zh_CN 停机原因：%s
                String reason = I18n.format("gtnr.gui.crafting_monitor.shutdown", machine.shutdown());
                for (String line : font.listFormattedStringToWidth(reason, contentWidth)) {
                    lines.add(new Text(line, LOOK.bad()));
                }
            }
            lines.add(new Text(formatPosition(machine.machine()), LOOK.listMuted()));
        }

        private static String stateText(Machine machine) {
            if (machine.active()) {
                // #tr gtnr.gui.crafting_monitor.running
                // # Processing
                // # zh_CN 加工中
                return I18n.format("gtnr.gui.crafting_monitor.running");
            }
            if (machine.duration() > 0 || !machine.shutdown()
                .isEmpty()) {
                // #tr gtnr.gui.crafting_monitor.waiting
                // # Waiting
                // # zh_CN 等待中
                return I18n.format("gtnr.gui.crafting_monitor.waiting");
            }
            // #tr gtnr.gui.crafting_monitor.progress_unavailable
            // # Progress unavailable
            // # zh_CN 暂无加工进度
            return I18n.format("gtnr.gui.crafting_monitor.progress_unavailable");
        }

        private interface Line {

            void draw(int x, int y, int width, FontRenderer font);
        }

        private record Text(String left, int leftColor, String right, int rightColor) implements Line {

            private Text(String left, int color) {
                this(left, color, "", color);
            }

            @Override
            public void draw(int x, int y, int width, FontRenderer font) {
                if (right.isEmpty()) {
                    font.drawString(font.trimStringToWidth(left, width), x, y, leftColor, LOOK.listShadow());
                    return;
                }
                String rightText = font.trimStringToWidth(right, width);
                int rightWidth = font.getStringWidth(rightText);
                int leftWidth = Math.max(0, width - rightWidth - COLUMN_GAP);
                if (leftWidth > 0) {
                    font.drawString(font.trimStringToWidth(left, leftWidth), x, y, leftColor, LOOK.listShadow());
                }
                font.drawString(rightText, x + width - rightWidth, y, rightColor, LOOK.listShadow());
            }
        }

        private record Bar(String label, double fraction) implements Line {

            @Override
            public void draw(int x, int y, int width, FontRenderer font) {
                int top = y + (LINE_HEIGHT - BAR_HEIGHT) / 2;
                String text = font.trimStringToWidth(label, width - 4);
                if (Loader.isModLoaded("Waila")) {
                    NativeWailaBar.draw(x, top, width, text, fraction);
                    return;
                }
                LOOK.drawWell(x, top, width, BAR_HEIGHT);
                int filled = (int) ((width - 2) * fraction);
                if (filled > 0) Gui.drawRect(x + 1, top + 1, x + 1 + filled, top + BAR_HEIGHT - 1, SKIN.highlight);
                font.drawStringWithShadow(text, x + 2, top + 2, SKIN.colors.fieldText());
            }
        }

        private static final class NativeWailaBar {

            private static final TTRenderBar RENDERER = new TTRenderBar();
            private static final String COLOR_TOP = Integer.toString(SKIN.highlightTop);
            private static final String COLOR_BOTTOM = Integer.toString(SKIN.highlight);

            private static void draw(int x, int y, int width, String label, double fraction) {
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(x, y, 0);
                    RENDERER.setMaxLineWidth(width);
                    RENDERER.draw(new String[] { label, COLOR_TOP, COLOR_BOTTOM, Double.toString(fraction) }, null);
                } finally {
                    GL11.glPopMatrix();
                }
            }
        }

        private enum Divider implements Line {

            INSTANCE;

            @Override
            public void draw(int x, int y, int width, FontRenderer font) {
                int center = y + LINE_HEIGHT / 2;
                Gui.drawRect(x, center, x + width, center + 1, SKIN.colors.line());
            }
        }
    }

    private static final class Menu {

        private final int x;
        private final int y;
        private final IAEStack<?> stack;
        private final List<Machine> machines;
        private final int snapshotId;
        private int row;

        private Menu(int x, int y, IAEStack<?> stack, List<Machine> machines, int snapshotId) {
            this.x = x;
            this.y = y;
            this.stack = stack;
            this.machines = machines;
            this.snapshotId = snapshotId;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + MENU_WIDTH && mouseY >= y + 3 && mouseY < y + MENU_HEIGHT - 3;
        }

        private void draw(int mouseX, int mouseY, FontRenderer font) {
            Machine target = machines.get(row);
            SKIN.panel.draw(x, y, MENU_WIDTH, MENU_HEIGHT);
            String index = (row + 1) + " / " + machines.size() + "  ";
            drawRow(0, index + displayName(target), machines.size() > 1, mouseX, mouseY, font);
            // #tr gtnr.gui.crafting_monitor.locate_provider
            // # Locate pattern provider
            // # zh_CN 定位样板供应器
            drawRow(
                1,
                I18n.format("gtnr.gui.crafting_monitor.locate_provider"),
                target.provider() != null,
                mouseX,
                mouseY,
                font);
            // #tr gtnr.gui.crafting_monitor.open_machine
            // # Open machine
            // # zh_CN 打开机器
            drawRow(
                2,
                I18n.format("gtnr.gui.crafting_monitor.open_machine"),
                target.canOpenMachine(),
                mouseX,
                mouseY,
                font);
            // #tr gtnr.gui.crafting_monitor.open_provider
            // # Open pattern provider
            // # zh_CN 打开样板供应器
            drawRow(
                3,
                I18n.format("gtnr.gui.crafting_monitor.open_provider"),
                target.canOpenProvider(),
                mouseX,
                mouseY,
                font);
            // #tr gtnr.gui.crafting_monitor.close
            // # Close
            // # zh_CN 关闭
            drawRow(4, I18n.format("gtnr.gui.crafting_monitor.close"), true, mouseX, mouseY, font);
        }

        private void drawRow(int index, String text, boolean enabled, int mouseX, int mouseY, FontRenderer font) {
            int top = y + 3 + index * MENU_ROW_HEIGHT;
            boolean selected = enabled && contains(mouseX, mouseY) && mouseY >= top && mouseY < top + MENU_ROW_HEIGHT;
            if (selected) LOOK.drawSelectedRow(x + 2, top, MENU_WIDTH - 4, MENU_ROW_HEIGHT);
            int color = selected ? LOOK.selectedText() : enabled || index == 0 ? LOOK.listText() : LOOK.listMuted();
            font.drawString(font.trimStringToWidth(text, MENU_WIDTH - 12), x + 6, top + 3, color, LOOK.listShadow());
        }
    }
}
