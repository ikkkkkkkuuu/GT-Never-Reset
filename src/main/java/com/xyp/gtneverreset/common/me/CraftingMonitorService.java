package com.xyp.gtneverreset.common.me;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.common.me.CraftingMachineMonitor.Provider;
import com.xyp.gtneverreset.common.me.CraftingMachineMonitor.Target;
import com.xyp.gtneverreset.common.packet.CraftingMonitorRequestPacket.Action;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Machine;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Position;
import com.xyp.gtneverreset.common.packet.CraftingMonitorSnapshotPacket.Result;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.parts.IPartHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.NamedDimensionalCoord;
import appeng.container.implementations.ContainerCraftingCPU;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public final class CraftingMonitorService {

    private static final int STATUS_INTERVAL = 4;
    private static final int ACTION_INTERVAL = 4;
    private static final int MAX_RECENT_VIEWS = 3;
    private static final int VIEW_LIFETIME = 400;
    private static final Map<ContainerCraftingCPU, Session> SESSIONS = new WeakHashMap<>();

    private CraftingMonitorService() {}

    public static void handle(EntityPlayerMP player, int windowId, int requestId, IAEStack<?> stack, Action action,
        int row) {
        if (!(player.openContainer instanceof ContainerCraftingCPU container) || container.windowId != windowId
            || !container.canInteractWith(player)) return;

        long now = player.worldObj.getTotalWorldTime();
        Session session = SESSIONS.computeIfAbsent(container, ignored -> new Session());
        if (!session.accept(action, now)) return;

        CraftingCPUCluster cpu = container.getCpu();
        IGrid grid = cpu == null ? null : cpu.getGrid();
        IGridNode terminalNode = container.getActionSource() instanceof PlayerSource source && source.via != null
            ? source.via.getActionableNode()
            : null;
        ISecurityGrid security = terminalNode == null || terminalNode.getGrid() != grid || grid == null ? null
            : grid.getCache(ISecurityGrid.class);
        if (security == null || !security.hasPermission(player, SecurityPermissions.CRAFT)) {
            reply(
                player,
                windowId,
                requestId,
                List.of(),
                action == Action.STATUS ? Result.STATUS : Result.DENIED,
                null);
            return;
        }

        if (action == Action.STATUS) {
            if (!cpu.isMaking(stack)) {
                reply(player, windowId, requestId, List.of(), Result.STATUS, null);
                return;
            }
            List<Row> rows = collectRows(
                player,
                cpu,
                grid,
                stack,
                security.hasPermission(player, SecurityPermissions.BUILD));
            session.views.addFirst(
                new View(
                    requestId,
                    now,
                    new WeakReference<>(cpu),
                    new WeakReference<>(cpu.getLastCraftingLink()),
                    stack,
                    rows));
            if (session.views.size() > MAX_RECENT_VIEWS) session.views.removeLast();
            reply(
                player,
                windowId,
                requestId,
                rows.stream()
                    .map(Row::display)
                    .toList(),
                Result.STATUS,
                null);
            return;
        }

        Row selected = session.getRow(cpu, stack, requestId, now, row);
        if (selected != null && cpu.isMaking(stack)) {
            if (action == Action.LOCATE_PROVIDER && selected.display()
                .provider() != null && hasCurrentProvider(cpu, grid, stack, selected)) {
                reply(
                    player,
                    windowId,
                    requestId,
                    List.of(),
                    Result.LOCATED,
                    selected.display()
                        .provider());
                return;
            }
            TileEntity tile = action == Action.OPEN_PROVIDER ? selected.provider()
                .get()
                : selected.machine()
                    .get();
            ForgeDirection side = action == Action.OPEN_PROVIDER ? selected.providerSide() : selected.machineSide();
            if (action != Action.LOCATE_PROVIDER && security.hasPermission(player, SecurityPermissions.BUILD)
                && providerOnGrid(
                    grid,
                    selected.provider()
                        .get(),
                    selected.providerSide())
                && hasCurrentRoute(cpu, grid, stack, selected, action == Action.OPEN_MACHINE)
                && CraftingMonitorGuiAccess.open(player, grid, tile, side)) {
                reply(player, windowId, requestId, List.of(), Result.OPENED, null);
                return;
            }
        }
        reply(player, windowId, requestId, List.of(), Result.DENIED, null);
    }

    private static List<Row> collectRows(EntityPlayerMP player, CraftingCPUCluster cpu, IGrid grid, IAEStack<?> stack,
        boolean mayConfigure) {
        var rows = new ArrayList<Row>();
        for (Target target : CraftingMachineMonitor.find(cpu, stack)) {
            TileEntity machine = target.machine();
            Position provider = positionOf(target.provider());
            boolean canConfigure = mayConfigure && providerOnGrid(grid, target.provider(), target.providerSide());
            Machine display = new Machine(
                machineName(machine),
                positionOf(machine),
                provider,
                target.progress(),
                target.duration(),
                target.parallel(),
                target.shutdown(),
                target.active(),
                canConfigure && CraftingMonitorGuiAccess.canOpen(player, machine, target.machineSide()),
                canConfigure && CraftingMonitorGuiAccess.canOpen(player, target.provider(), target.providerSide()));
            rows.add(
                new Row(
                    display,
                    new WeakReference<>(machine),
                    new WeakReference<>(target.provider()),
                    target.machineSide(),
                    target.providerSide(),
                    true));
            if (rows.size() >= CraftingMonitorSnapshotPacket.MAX_ROWS) return rows;
        }
        if (!rows.isEmpty()) return rows;

        List<NamedDimensionalCoord> planned = cpu.getProviders(stack);
        Map<Position, Provider> endpoints = planned.isEmpty() || !mayConfigure ? Map.of()
            : resolveScheduledProviders(grid, stack, planned);
        Set<Position> providers = new HashSet<>();
        for (NamedDimensionalCoord coordinate : planned) {
            Position position = positionOf(coordinate);
            if (!providers.add(position)) continue;
            Provider provider = endpoints.get(position);
            TileEntity tile = provider == null ? null : provider.tile();
            ForgeDirection side = provider == null ? null : provider.side();
            Machine display = new Machine(
                "",
                null,
                position,
                0,
                0,
                0,
                "",
                false,
                false,
                providerOnGrid(grid, tile, side) && CraftingMonitorGuiAccess.canOpen(player, tile, side));
            rows.add(new Row(display, new WeakReference<>(null), new WeakReference<>(tile), null, side, false));
            if (rows.size() >= CraftingMonitorSnapshotPacket.MAX_ROWS) return rows;
        }
        return rows;
    }

    private static boolean hasCurrentProvider(CraftingCPUCluster cpu, IGrid grid, IAEStack<?> stack, Row row) {
        if (row.dispatched()) {
            return providerOnGrid(
                grid,
                row.provider()
                    .get(),
                row.providerSide()) && hasCurrentRoute(cpu, grid, stack, row, false);
        }
        return cpu.getProviders(stack)
            .stream()
            .map(CraftingMonitorService::positionOf)
            .anyMatch(
                row.display()
                    .provider()::equals);
    }

    private static boolean hasCurrentRoute(CraftingCPUCluster cpu, IGrid grid, IAEStack<?> stack, Row row,
        boolean machineAction) {
        TileEntity provider = row.provider()
            .get();
        ForgeDirection providerSide = row.providerSide();
        if (row.dispatched()) {
            TileEntity machine = row.machine()
                .get();
            return CraftingMachineMonitor.find(cpu, stack)
                .stream()
                .anyMatch(
                    target -> target.provider() == provider && target.providerSide() == providerSide
                        && (!machineAction
                            || target.machine() == machine && target.machineSide() == row.machineSide()));
        }
        if (machineAction || provider == null) return false;
        Provider current = resolveScheduledProviders(grid, stack, cpu.getProviders(stack)).get(
            row.display()
                .provider());
        return current != null && current.tile() == provider && current.side() == providerSide;
    }

    private static boolean providerOnGrid(IGrid grid, TileEntity provider, ForgeDirection side) {
        if (provider == null || provider.isInvalid()) return false;
        World world = provider.getWorldObj();
        if (world == null || !world.getChunkProvider()
            .chunkExists(provider.xCoord >> 4, provider.zCoord >> 4)
            || world.getTileEntity(provider.xCoord, provider.yCoord, provider.zCoord) != provider) return false;
        ForgeDirection face = side == null ? ForgeDirection.UNKNOWN : side;
        Object host = provider instanceof IPartHost cable && face != ForgeDirection.UNKNOWN ? cable.getPart(face)
            : provider;
        IGridNode node = host instanceof IActionHost actionHost ? actionHost.getActionableNode()
            : host instanceof IGridHost gridHost ? gridHost.getGridNode(face) : null;
        return node != null && node.getGrid() == grid;
    }

    private static Map<Position, Provider> resolveScheduledProviders(IGrid grid, IAEStack<?> stack,
        List<NamedDimensionalCoord> planned) {
        if (!(grid.getCache(ICraftingGrid.class) instanceof CraftingGridCache crafting)) return Map.of();
        List<ICraftingPatternDetails> patterns = crafting.getCraftingMultiPatterns()
            .get(stack);
        if (patterns == null) return Map.of();

        Set<Position> positions = new HashSet<>();
        for (NamedDimensionalCoord coordinate : planned) {
            positions.add(positionOf(coordinate));
            if (positions.size() >= CraftingMonitorSnapshotPacket.MAX_ROWS) break;
        }
        Map<Position, Provider> endpoints = new LinkedHashMap<>();
        Set<Position> ambiguous = new HashSet<>();
        for (ICraftingPatternDetails pattern : patterns) {
            for (ICraftingMedium medium : crafting.getMediums(pattern)) {
                Provider provider = CraftingMachineMonitor.resolveProvider(medium);
                if (provider == null) continue;
                Position position = positionOf(provider.tile());
                if (!positions.contains(position) || ambiguous.contains(position)) continue;
                Provider previous = endpoints.putIfAbsent(position, provider);
                if (previous != null && !previous.equals(provider)) {
                    endpoints.remove(position);
                    ambiguous.add(position);
                }
            }
        }
        return endpoints;
    }

    private static Position positionOf(NamedDimensionalCoord coordinate) {
        return new Position(coordinate.getDimension(), coordinate.x, coordinate.y, coordinate.z);
    }

    private static Position positionOf(TileEntity tile) {
        World world = tile == null ? null : tile.getWorldObj();
        return world == null ? null : new Position(world.provider.dimensionId, tile.xCoord, tile.yCoord, tile.zCoord);
    }

    private static String machineName(TileEntity tile) {
        if (tile instanceof IGregTechTileEntity host && host.getMetaTileEntity() != null)
            return "gt.blockmachines." + host.getMetaTileEntity()
                .getMetaName() + ".name";
        return tile == null ? ""
            : tile.getBlockType()
                .getUnlocalizedName() + ".name";
    }

    private static void reply(EntityPlayerMP player, int windowId, int requestId, List<Machine> rows, Result result,
        Position position) {
        NetWorkHandler.sendTo(new CraftingMonitorSnapshotPacket(windowId, requestId, rows, result, position), player);
    }

    private static final class Session {

        private final Deque<View> views = new ArrayDeque<>();
        private long nextStatus = Long.MIN_VALUE;
        private long nextAction = Long.MIN_VALUE;

        private boolean accept(Action action, long now) {
            if (action == Action.STATUS) {
                if (now < nextStatus) return false;
                nextStatus = now + STATUS_INTERVAL;
            } else {
                if (now < nextAction) return false;
                nextAction = now + ACTION_INTERVAL;
            }
            return true;
        }

        private Row getRow(CraftingCPUCluster cpu, IAEStack<?> stack, int requestId, long now, int index) {
            for (View view : views) {
                if (view.requestId() == requestId && view.cpu()
                    .get() == cpu
                    && view.job()
                        .get() != null
                    && view.job()
                        .get() == cpu.getLastCraftingLink()
                    && view.stack()
                        .equals(stack)
                    && now - view.tick() < VIEW_LIFETIME
                    && index >= 0
                    && index < view.rows()
                        .size())
                    return view.rows()
                        .get(index);
            }
            return null;
        }
    }

    private record View(int requestId, long tick, WeakReference<CraftingCPUCluster> cpu,
        WeakReference<ICraftingLink> job, IAEStack<?> stack, List<Row> rows) {}

    private record Row(Machine display, WeakReference<TileEntity> machine, WeakReference<TileEntity> provider,
        ForgeDirection machineSide, ForgeDirection providerSide, boolean dispatched) {}
}
