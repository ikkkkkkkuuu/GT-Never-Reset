package com.xyp.gtneverreset.client.gui.crafting;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.api.mixinHelper.CraftingMonitorScreen;
import com.xyp.gtneverreset.common.packet.CraftingMonitorRequestPacket;
import com.xyp.gtneverreset.common.packet.CraftingMonitorRequestPacket.Action;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Machine;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Position;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Result;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;

import appeng.api.storage.data.IAEStack;
import appeng.api.util.NamedDimensionalCoord;
import appeng.client.gui.implementations.GuiCraftingCPU;
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.core.localization.PlayerMessages;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CraftingMonitorTooltip {

    private static final long HOVER_DELAY_MS = 300;
    private static final long REFRESH_INTERVAL_MS = 500;
    private static final long REPLY_TIMEOUT_MS = 2000;
    private static final int MAX_TOOLTIP_TARGETS = 2;
    private static final int BAR_WIDTH = 12;
    private static final String BAR_SEGMENT = "━";
    private static final int MENU_WIDTH = 154;
    private static final int MENU_ROW_HEIGHT = 14;
    private static final int MENU_HEIGHT = MENU_ROW_HEIGHT * 5 + 6;
    private static int nextRequestId;

    private IAEStack<?> hovered;
    private IAEStack<?> observed;
    private List<Machine> machines = List.of();
    private long nextRefresh;
    private long replyDeadline;
    private int statusRequest;
    private int snapshotId;
    private int actionRequest;
    private Menu menu;

    public static void accept(CraftingMonitorSnapshotPacket packet) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || !(minecraft.currentScreen instanceof GuiCraftingCPU gui)
            || minecraft.thePlayer.openContainer != gui.inventorySlots
            || gui.inventorySlots.windowId != packet.windowId()
            || !(gui instanceof CraftingMonitorScreen screen)) return;
        screen.gtnr$craftingMonitor()
            .apply(packet, minecraft);
    }

    public void beginFrame() {
        hovered = null;
    }

    public void append(IAEStack<?> stack, List<String> lines, int windowId) {
        hovered = stack;
        if (menu != null) return;
        long now = Minecraft.getSystemTime();
        if (observed == null || !stack.equals(observed)) {
            resetObservation();
            observed = stack.copy();
            nextRefresh = now + HOVER_DELAY_MS;
        }
        if (statusRequest != 0 && now >= replyDeadline) {
            statusRequest = 0;
            machines = List.of();
        }
        if (statusRequest == 0 && now >= nextRefresh) {
            statusRequest = send(windowId, stack, Action.STATUS, 0);
            replyDeadline = now + REPLY_TIMEOUT_MS;
            nextRefresh = now + REFRESH_INTERVAL_MS;
        }
        if (machines.isEmpty()) return;

        for (int index = 0; index < Math.min(machines.size(), MAX_TOOLTIP_TARGETS); index++) {
            appendMachine(lines, machines.get(index));
        }
        if (machines.size() > MAX_TOOLTIP_TARGETS) {
            // #tr gtnr.gui.crafting_monitor.additional
            // # %s more targets
            // # zh_CN 另有 %s 个目标
            lines.add(
                EnumChatFormatting.GRAY
                    + I18n.format("gtnr.gui.crafting_monitor.additional", machines.size() - MAX_TOOLTIP_TARGETS));
        }
        // #tr gtnr.gui.crafting_monitor.actions
        // # Right-click for machine and pattern actions
        // # zh_CN 右键查看机器与样板操作
        lines.add(EnumChatFormatting.GRAY + I18n.format("gtnr.gui.crafting_monitor.actions"));
    }

    private static void appendMachine(List<String> lines, Machine machine) {
        lines.add("");
        if (machine.machine() == null) {
            lines.add(EnumChatFormatting.AQUA + displayName(machine));
            if (machine.provider() != null) lines.add(formatPosition(machine.provider()));
            return;
        }
        lines.add(EnumChatFormatting.AQUA + displayName(machine) + "  " + stateText(machine));
        if (machine.active() && machine.duration() > 0) {
            int progress = Math.max(0, Math.min(machine.progress(), machine.duration()));
            int filled = (int) (((long) progress * BAR_WIDTH + machine.duration() / 2L) / machine.duration());
            int percent = (int) ((long) progress * 100 / machine.duration());
            lines.add(
                EnumChatFormatting.GREEN + BAR_SEGMENT.repeat(filled)
                    + EnumChatFormatting.DARK_GRAY
                    + BAR_SEGMENT.repeat(BAR_WIDTH - filled)
                    + EnumChatFormatting.WHITE
                    + "  "
                    + percent
                    + "%");
            // #tr gtnr.gui.crafting_monitor.progress
            // # %s / %s t · %s t left
            // # zh_CN %s / %s t · 剩余 %s t
            lines.add(
                I18n.format(
                    "gtnr.gui.crafting_monitor.progress",
                    progress,
                    machine.duration(),
                    machine.duration() - progress));
        }
        if (machine.active() && machine.parallel() > 0) {
            // #tr gtnr.gui.crafting_monitor.parallel
            // # Actual parallel: %s
            // # zh_CN 实际并行：%s
            lines.add(I18n.format("gtnr.gui.crafting_monitor.parallel", machine.parallel()));
        }
        if (!machine.active() && !machine.shutdown()
            .isEmpty()) {
            // #tr gtnr.gui.crafting_monitor.shutdown
            // # Stop reason: %s
            // # zh_CN 停机原因：%s
            lines.add(EnumChatFormatting.RED + I18n.format("gtnr.gui.crafting_monitor.shutdown", machine.shutdown()));
        }
        lines.add(formatPosition(machine.machine()));
    }

    private static String stateText(Machine machine) {
        if (machine.active()) {
            // #tr gtnr.gui.crafting_monitor.running
            // # Processing
            // # zh_CN 加工中
            return EnumChatFormatting.GREEN + I18n.format("gtnr.gui.crafting_monitor.running");
        }
        if (machine.duration() > 0 || !machine.shutdown()
            .isEmpty()) {
            // #tr gtnr.gui.crafting_monitor.waiting
            // # Waiting
            // # zh_CN 等待中
            return EnumChatFormatting.GRAY + I18n.format("gtnr.gui.crafting_monitor.waiting");
        }
        // #tr gtnr.gui.crafting_monitor.progress_unavailable
        // # Progress unavailable
        // # zh_CN 暂无加工进度
        return EnumChatFormatting.GRAY + I18n.format("gtnr.gui.crafting_monitor.progress_unavailable");
    }

    public void endFrame(int mouseX, int mouseY, FontRenderer font) {
        if (hovered == null && menu == null) resetObservation();
        if (menu != null) menu.draw(mouseX, mouseY, font);
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
        if (button != 1 || hovered == null || observed == null || !hovered.equals(observed) || machines.isEmpty()) {
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
        menu = null;
        hovered = null;
        actionRequest = 0;
    }

    private void apply(CraftingMonitorSnapshotPacket packet, Minecraft minecraft) {
        if (packet.result() == Result.STATUS && packet.requestId() == statusRequest) {
            statusRequest = 0;
            machines = packet.machines();
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
        return EnumChatFormatting.GRAY
            + I18n.format("gtnr.gui.crafting_monitor.position", pos.dimension(), pos.x(), pos.y(), pos.z());
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
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            Gui.drawRect(x, y, x + MENU_WIDTH, y + MENU_HEIGHT, 0xff5b456c);
            Gui.drawRect(x + 1, y + 1, x + MENU_WIDTH - 1, y + MENU_HEIGHT - 1, 0xff20182a);
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
            GL11.glPopAttrib();
        }

        private void drawRow(int index, String text, boolean enabled, int mouseX, int mouseY, FontRenderer font) {
            int top = y + 3 + index * MENU_ROW_HEIGHT;
            if (enabled && contains(mouseX, mouseY) && mouseY >= top && mouseY < top + MENU_ROW_HEIGHT) {
                Gui.drawRect(x + 2, top, x + MENU_WIDTH - 2, top + MENU_ROW_HEIGHT, 0xff59456d);
            }
            int color = enabled || index == 0 ? 0xffffff : 0x7c7584;
            font.drawStringWithShadow(font.trimStringToWidth(text, MENU_WIDTH - 12), x + 6, top + 3, color);
        }
    }
}
