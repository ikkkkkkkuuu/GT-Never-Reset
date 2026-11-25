package com.xyp.gtneverreset.client.gui.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import com.xyp.gtneverreset.client.gui.crafting.CraftingMonitorTooltip.ProgressSample;
import com.xyp.gtneverreset.common.packet.CraftingMonitorCellProgressPacket;
import com.xyp.gtneverreset.common.packet.CraftingMonitorCellRequestPacket;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class CraftingMonitorCellOverlay {

    private static final long REFRESH_MS = 500;
    private static final long REPLY_TIMEOUT_MS = 2000;
    private static final Variant SKIN = Variant.DARK_PURPLE;
    private static int nextRequestId;

    private final Set<IAEStack<?>> visible = new LinkedHashSet<>();
    private final Map<IAEStack<?>, Sample> samples = new HashMap<>();
    private List<IAEStack<?>> requested = List.of();
    private int requestId;
    private int nextCell;
    private long nextRefresh;
    private long replyDeadline;

    void beginFrame() {
        visible.clear();
    }

    boolean draw(IAEStack<?> stack, int x, int y, int width, int height) {
        visible.add(stack);
        Sample sample = samples.get(stack);
        if (sample == null) return false;
        long now = Minecraft.getSystemTime();
        if (now >= sample.expiresAt()) return false;
        int filled = (int) (width * sample.fill()
            .valueAt(now));
        Gui.drawRect(x, y, x + width, y + height, SKIN.highlightShadow);
        if (filled > 0) Gui.drawRect(x, y, x + filled, y + height, SKIN.highlight);
        return true;
    }

    void endFrame(int windowId) {
        if (visible.isEmpty()) {
            clear();
            return;
        }
        samples.keySet()
            .retainAll(visible);
        long now = Minecraft.getSystemTime();
        if (now < nextRefresh || requestId != 0 && now < replyDeadline) return;
        int maxCells = CraftingMonitorCellRequestPacket.MAX_CELLS;
        var batch = new ArrayList<>(visible);
        if (batch.size() > maxCells) {
            int from = Math.floorMod(nextCell, batch.size());
            Collections.rotate(batch, -from);
            nextCell = from + maxCells;
            batch.subList(maxCells, batch.size())
                .clear();
        } else {
            nextCell = 0;
        }
        requested = batch;
        requestId = ++nextRequestId;
        if (requestId == 0) requestId = ++nextRequestId;
        replyDeadline = now + REPLY_TIMEOUT_MS;
        nextRefresh = now + REFRESH_MS;
        NetWorkHandler.sendToServer(new CraftingMonitorCellRequestPacket(windowId, requestId, requested));
    }

    void apply(CraftingMonitorCellProgressPacket packet) {
        if (requestId == 0 || packet.requestId() != requestId) return;
        long now = Minecraft.getSystemTime();
        int pages = (visible.size() + CraftingMonitorCellRequestPacket.MAX_CELLS - 1)
            / CraftingMonitorCellRequestPacket.MAX_CELLS;
        long expiresAt = now + REPLY_TIMEOUT_MS + pages * REFRESH_MS;
        Set<IAEStack<?>> received = new HashSet<>();
        for (var cell : packet.cells()) {
            if (cell.index() >= requested.size()) continue;
            IAEStack<?> stack = requested.get(cell.index());
            if (!visible.contains(stack)) continue;
            Sample previous = samples.get(stack);
            double fraction = (double) cell.progress() / cell.duration();
            double from = previous != null && previous.expiresAt() > now
                && previous.duration() == cell.duration()
                && previous.progress() <= cell.progress()
                    ? previous.fill()
                        .valueAt(now)
                    : fraction;
            samples.put(
                stack,
                new Sample(cell.progress(), cell.duration(), new ProgressSample(from, fraction, now), expiresAt));
            received.add(stack);
        }
        for (IAEStack<?> stack : requested) {
            if (!received.contains(stack)) samples.remove(stack);
        }
        requested = List.of();
        requestId = 0;
    }

    void clear() {
        visible.clear();
        samples.clear();
        requested = List.of();
        requestId = 0;
        nextRefresh = 0;
        nextCell = 0;
    }

    private record Sample(int progress, int duration, ProgressSample fill, long expiresAt) {}
}
