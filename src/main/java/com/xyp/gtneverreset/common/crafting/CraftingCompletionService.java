package com.xyp.gtneverreset.common.crafting;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.packet.CraftingCompletionPacket;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.ContainerCraftingCPU;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.helpers.WirelessTerminalGuiObject;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Sends completed item identities once to the ordering player's terminal on the same AE grid. */
public final class CraftingCompletionService {

    public static final int MAX_ITEMS = 256;
    public static final CraftingCompletionService INSTANCE = new CraftingCompletionService();
    private static final int MAX_PENDING_PLAYERS = 1024;

    private final Map<String, List<Completion>> pending = new LinkedHashMap<>();
    private final List<GridIdentity> grids = new ArrayList<>();
    private final Map<EntityPlayerMP, Binding> bindings = new IdentityHashMap<>();
    private long nextGridId, sequence;

    private CraftingCompletionService() {}

    /** Called by the server-side crafting completion hook. */
    public static void completed(IGrid grid, String player, ItemStack item) {
        if (grid == null || player == null || player.isEmpty() || item == null || item.getItem() == null) return;
        INSTANCE.enqueue(grid, player.toLowerCase(Locale.ROOT), item);
    }

    public static void clear() {
        INSTANCE.pending.clear();
        INSTANCE.grids.clear();
        INSTANCE.bindings.clear();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP player)
            || pending.isEmpty() && bindings.isEmpty()) return;

        Binding previous = bindings.get(player);
        Container open = player.openContainer;
        if (!(open instanceof ContainerMEMonitorable terminal)) {
            if (previous != null && !isCraftingPage(open)) bindings.remove(player);
            return;
        }

        String owner = pending.isEmpty() ? null
            : player.getCommandSenderName()
                .toLowerCase(Locale.ROOT);
        List<Completion> queue = owner == null ? null : pending.get(owner);
        if (previous == null && queue == null) return;

        IGrid grid = terminalGrid(terminal);
        if (grid == null) return;

        List<ItemStack> items = queue == null ? List.of() : forGrid(queue, grid);
        if (queue != null && queue.isEmpty()) pending.remove(owner);
        if (items.isEmpty() && (previous == null || previous.matches(terminal, grid, player.dimension))) return;
        if (!terminal.canInteractWith(player)) return;

        long gridId = getGridId(grid);
        NetWorkHandler.sendTo(
            new CraftingCompletionPacket(player.dimension, terminal.windowId, gridId, ++sequence, items),
            player);
        if (!items.isEmpty() || previous != null && previous.grid()
            .get() == grid) {
            bindings.put(
                player,
                new Binding(
                    new WeakReference<>(terminal),
                    new WeakReference<>(grid),
                    terminal.windowId,
                    player.dimension));
        } else {
            bindings.remove(player);
        }
        if (queue != null) {
            queue.removeIf(
                entry -> entry.grid()
                    .get() == null
                    || entry.grid()
                        .get() == grid);
            if (queue.isEmpty()) pending.remove(owner);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        bindings.remove(event.player);
    }

    private void enqueue(IGrid grid, String owner, ItemStack item) {
        List<Completion> queue = pending.computeIfAbsent(owner, ignored -> new ArrayList<>());
        queue.removeIf(entry -> {
            IGrid origin = entry.grid()
                .get();
            return origin == null || origin == grid && entry.item()
                .isItemEqual(item) && ItemStack.areItemStackTagsEqual(entry.item(), item);
        });
        ItemStack identity = item.copy();
        identity.stackSize = 1;
        queue.add(new Completion(new WeakReference<>(grid), identity));
        if (queue.size() > MAX_ITEMS) queue.remove(0);
        if (pending.size() > MAX_PENDING_PLAYERS) pending.remove(
            pending.keySet()
                .iterator()
                .next());
    }

    private static List<ItemStack> forGrid(List<Completion> queue, IGrid grid) {
        List<ItemStack> items = null;
        for (Iterator<Completion> iterator = queue.iterator(); iterator.hasNext();) {
            Completion entry = iterator.next();
            IGrid origin = entry.grid()
                .get();
            if (origin == null) {
                iterator.remove();
            } else if (origin == grid) {
                if (items == null) items = new ArrayList<>();
                items.add(entry.item());
            }
        }
        return items == null ? List.of() : items;
    }

    private long getGridId(IGrid grid) {
        for (Iterator<GridIdentity> iterator = grids.iterator(); iterator.hasNext();) {
            GridIdentity identity = iterator.next();
            IGrid existingGrid = identity.grid()
                .get();
            if (existingGrid == grid) return identity.id();
            if (existingGrid == null) iterator.remove();
        }
        long id = ++nextGridId;
        grids.add(new GridIdentity(new WeakReference<>(grid), id));
        return id;
    }

    private static IGrid terminalGrid(ContainerMEMonitorable terminal) {
        IGridNode node = terminal.getNetworkNode();
        if (node != null) return node.getGrid();
        return terminal.getTarget() instanceof WirelessTerminalGuiObject wireless ? wireless.getGrid() : null;
    }

    private static boolean isCraftingPage(Container container) {
        return container instanceof ContainerCraftAmount || container instanceof ContainerCraftConfirm
            || container instanceof ContainerCraftingCPU;
    }

    private record GridIdentity(WeakReference<IGrid> grid, long id) {}

    private record Completion(WeakReference<IGrid> grid, ItemStack item) {}

    private record Binding(WeakReference<ContainerMEMonitorable> terminal, WeakReference<IGrid> grid, int windowId,
        int dimension) {

        private boolean matches(ContainerMEMonitorable other, IGrid otherGrid, int otherDimension) {
            return terminal.get() == other && grid.get() == otherGrid
                && windowId == other.windowId
                && dimension == otherDimension;
        }
    }
}
