package com.xyp.gtneverreset.common.projector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;
import com.xyp.gtneverreset.common.wireless.WirelessCardInventory;

import appeng.api.AEApi;
import appeng.api.config.CraftingAllow;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileWireless;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ProjectorMaterialCrafting {

    public static final ProjectorMaterialCrafting INSTANCE = new ProjectorMaterialCrafting();
    private static final int SIMULATION_ROUND_CELLS = 256;
    private static final long SIMULATION_BUDGET_NANOS = 2_000_000L;
    private static final int CPU_RETRY_TICKS = 10;
    private static final int MAX_SUBMISSION_ATTEMPTS = 2;
    private static final double TERMINAL_START_POWER = 0.5;
    private final Deque<Batch> batches = new ArrayDeque<>();
    private long tick;

    private ProjectorMaterialCrafting() {}

    public static boolean[] craftable(EntityPlayerMP player, ItemStack[] items) {
        var connections = connections(player);
        boolean[] result = new boolean[items.length];
        if (connections.isEmpty()) return result;
        for (int i = 0; i < items.length; i++) {
            IAEItemStack item = AEItemStack.create(items[i]);
            if (item == null) continue;
            for (Connection connection : connections.values()) {
                if (hasPattern(player, connection, item)) {
                    result[i] = true;
                    break;
                }
            }
        }
        return result;
    }

    public static void request(EntityPlayerMP player, ProjectorPlan plan) {
        if (INSTANCE.batches.stream()
            .anyMatch(batch -> batch.player == player)) {
            // #tr gtneverreset.projector.crafting.busy
            // # A projector crafting request is already being processed.
            // # zh_CN 已有投影材料合成请求正在处理。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.crafting.busy"));
            return;
        }
        if (connections(player).isEmpty()) {
            reportNoNetwork(player);
            return;
        }
        INSTANCE.batches.addLast(new Batch(player, plan));
        // #tr gtneverreset.projector.crafting.planning
        // # Checking the projection for materials to craft…
        // # zh_CN 正在检查投影方案中需要合成的材料……
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.crafting.planning"));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        Batch batch = batches.pollFirst();
        if (batch == null) return;
        try {
            if (batch.player.isDead) batch.cancelCalculation();
            else if (batch.advance(tick)) batches.addLast(batch);
        } catch (RuntimeException e) {
            GTNeverReset.LOG
                .error("could not process projector crafting request for machine {}", batch.plan.machine, e);
            batch.cancelCalculation();
            // #tr gtneverreset.projector.crafting.error
            // # The remaining projector crafting orders could not be processed.
            // # zh_CN 无法处理剩余的投影材料合成订单。
            batch.player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.crafting.error"));
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        batches.removeIf(batch -> {
            if (batch.player != event.player) return false;
            batch.cancelCalculation();
            return true;
        });
    }

    public void clear() {
        for (Batch batch : batches) batch.cancelCalculation();
        batches.clear();
    }

    private static Map<IGrid, Connection> connections(EntityPlayerMP player) {
        var wireless = AEApi.instance()
            .registries()
            .wireless();
        Map<IGrid, Connection> connections = new LinkedHashMap<>();
        for (ItemStack terminal : WirelessCardInventory.carried(player)) {
            var handler = wireless.getWirelessTerminalHandler(terminal);
            if (handler == null
                || !handler.hasInfinityPower(terminal) && !handler.hasPower(player, TERMINAL_START_POWER, terminal))
                continue;
            int location = WirelessCardInventory.location(player, terminal);
            int slot = location == 0 ? player.inventory.currentItem
                : location > 0 ? location - 1 : Platform.baublesSlotsOffset - location - 1;
            var host = new WirelessTerminalGuiObject(handler, terminal, player, player.worldObj, slot, 0, 0);
            IGrid grid = host.getGrid();
            if (grid == null || connections.containsKey(grid)) continue;
            ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
            if (crafting != null && security != null && security.hasPermission(player, SecurityPermissions.CRAFT)) {
                IActionHost access = activeAccess(host, grid, handler.hasInfinityRange(terminal));
                if (access != null) connections.put(grid, new Connection(crafting, new PlayerSource(player, access)));
            }
        }
        return connections;
    }

    private static IActionHost activeAccess(WirelessTerminalGuiObject host, IGrid grid, boolean unlimited) {
        var node = host.getActionableNode();
        if (node != null && node.isActive() && node.getGrid() == grid) return host;
        if (unlimited) {
            // AE's unlimited-range GUI host can select an inactive access point first.
            for (var candidate : grid.getMachines(TileWireless.class)) {
                if (candidate.isActive() && candidate.getMachine() instanceof TileWireless access && access.isActive())
                    return access;
            }
        }
        return null;
    }

    private static boolean hasPattern(EntityPlayerMP player, Connection connection, IAEItemStack item) {
        return !connection.crafting.getCraftingFor(item, null, 0, player.worldObj)
            .isEmpty();
    }

    private static int missing(EntityPlayerMP player, Material material, IAEItemStack item,
        Map<IGrid, Connection> connections) {
        int amount = material.count() - ProjectorBuildMaterials.available(player, material.stack(), material.count());
        if (amount <= 0) return 0;
        for (Connection connection : connections.values()) {
            for (var cpu : connection.crafting.getCpus()) {
                if (!cpu.isBusy() || !cpu.isCraftingLinkStandalone()) continue;
                var output = cpu.getFinalMultiOutput();
                if (item.equals(output)) {
                    amount -= (int) Math.max(0, Math.min(amount, output.getStackSize()));
                    if (amount == 0) return 0;
                }
            }
        }
        return amount;
    }

    private static void reportNoNetwork(EntityPlayerMP player) {
        // #tr gtneverreset.projector.crafting.no_network
        // # No powered wireless terminal can reach an ME network you may craft on.
        // # zh_CN 找不到有电且能访问有合成权限的 ME 网络的无线终端。
        player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.crafting.no_network"));
    }

    @Desugar
    private record Connection(ICraftingGrid crafting, PlayerSource source) {}

    @Desugar
    private record Calculation(Order order, IAEItemStack item, int amount, IGrid grid, Future<ICraftingJob> future) {}

    private static final class Order {

        private final Material material;
        private final Map<IGrid, Integer> attempts = new IdentityHashMap<>();

        Order(Material material) {
            this.material = material;
        }
    }

    private static final class Batch {

        private final EntityPlayerMP player;
        private final ProjectorPlan plan;
        private final Deque<Order> pending = new ArrayDeque<>();
        private ProjectorSimulation simulation;
        private Calculation calculation;
        private int submitted, failed, consecutiveWaits;
        private long retryAt;
        private boolean reportedWait;

        Batch(EntityPlayerMP player, ProjectorPlan plan) {
            this.player = player;
            this.plan = plan;
            this.simulation = new ProjectorSimulation(
                plan.batchProjection ? plan.batchSettings() : plan.settings(),
                SIMULATION_ROUND_CELLS);
        }

        boolean advance(long now) {
            if (now < retryAt) return true;
            if (simulation != null) {
                if (!simulation.step(SIMULATION_BUDGET_NANOS)) return true;
                for (Material material : ProjectorMaterialPlan.materials(simulation.result(), plan)) {
                    pending.addLast(new Order(material));
                }
                simulation = null;
            }
            if (calculation != null) {
                if (!calculation.future.isDone()) return true;
                finishCalculation(connections(player), now);
            } else if (!pending.isEmpty()) {
                var connections = connections(player);
                if (connections.isEmpty()) {
                    reportNoNetwork(player);
                    return false;
                }
                startCalculation(connections, now);
            }
            if (calculation != null || !pending.isEmpty()) return true;
            reportResult();
            return false;
        }

        private void startCalculation(Map<IGrid, Connection> connections, long now) {
            Order order = pending.removeFirst();
            Material material = order.material;
            IAEItemStack item = AEItemStack.create(material.stack());
            int amount = item == null ? 0 : missing(player, material, item, connections);
            if (amount == 0) {
                consecutiveWaits = 0;
                return;
            }
            boolean craftable = false;
            boolean busy = false;
            for (var entry : connections.entrySet()) {
                Connection connection = entry.getValue();
                if (!hasPattern(player, connection, item)) continue;
                craftable = true;
                IGrid grid = entry.getKey();
                if (order.attempts.getOrDefault(grid, 0) >= MAX_SUBMISSION_ATTEMPTS) continue;
                for (var cpu : connection.crafting.getCpus()) {
                    if (cpu.getCraftingAllowMode() == CraftingAllow.ONLY_NONPLAYER) continue;
                    if (cpu.isBusy()) {
                        busy = true;
                        continue;
                    }
                    try {
                        Future<ICraftingJob> job = connection.crafting.beginCraftingJob(
                            player.worldObj,
                            grid,
                            connection.source,
                            item.copy()
                                .setStackSize(amount),
                            null);
                        calculation = new Calculation(order, item, amount, grid, job);
                        consecutiveWaits = 0;
                        return;
                    } catch (RuntimeException e) {
                        GTNeverReset.LOG.error("could not calculate projector material {}", material.stack(), e);
                        order.attempts.put(grid, MAX_SUBMISSION_ATTEMPTS);
                        break;
                    }
                }
            }
            if (busy) waitForCpu(order, now);
            else {
                if (craftable) failed++;
                consecutiveWaits = 0;
            }
        }

        private void finishCalculation(Map<IGrid, Connection> connections, long now) {
            Calculation finished = calculation;
            calculation = null;
            Order order = finished.order;
            int amount = missing(player, order.material, finished.item, connections);
            if (amount == 0) return;
            Connection connection = connections.get(finished.grid);
            if (connection == null || amount != finished.amount || !hasPattern(player, connection, finished.item)) {
                pending.addLast(order);
                return;
            }
            try {
                ICraftingJob<?> job = finished.future.isCancelled() ? null : finished.future.get();
                if (job == null || job.isSimulation()) {
                    rejectGrid(order, finished.grid);
                    return;
                }
                boolean busy = false;
                for (var cpu : connection.crafting.getCpus()) {
                    if (cpu.getCraftingAllowMode() == CraftingAllow.ONLY_NONPLAYER
                        || cpu.getAvailableStorage() < job.getByteTotal()
                        || !job.supportsCPUCluster(cpu)) continue;
                    busy |= cpu.isBusy();
                    if (cpu.isBusy()) continue;
                    if (connection.crafting.submitJob(job, null, cpu, true, connection.source) != null) submitted++;
                    else {
                        order.attempts.merge(finished.grid, 1, Integer::sum);
                        pending.addLast(order);
                    }
                    return;
                }
                if (busy) waitForCpu(order, now);
                else rejectGrid(order, finished.grid);
            } catch (InterruptedException | ExecutionException | RuntimeException e) {
                GTNeverReset.LOG.error("could not submit projector material {}", order.material.stack(), e);
                rejectGrid(order, finished.grid);
            }
        }

        private void rejectGrid(Order order, IGrid grid) {
            order.attempts.put(grid, MAX_SUBMISSION_ATTEMPTS);
            pending.addLast(order);
        }

        private void waitForCpu(Order order, long now) {
            pending.addLast(order);
            if (++consecutiveWaits >= pending.size()) {
                consecutiveWaits = 0;
                retryAt = now + CPU_RETRY_TICKS;
            }
            if (reportedWait) return;
            reportedWait = true;
            // #tr gtneverreset.projector.crafting.waiting
            // # Waiting for an available ME crafting CPU. The remaining orders will be submitted automatically.
            // # zh_CN 正在等待空闲的 ME 合成 CPU，剩余订单会自动提交。
            player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.crafting.waiting"));
        }

        void cancelCalculation() {
            if (calculation != null) calculation.future.cancel(false);
        }

        private void reportResult() {
            if (submitted == 0 && failed == 0) {
                // #tr gtneverreset.projector.crafting.no_orders
                // # This projection needs no new ME crafting orders.
                // # zh_CN 当前投影方案没有需要新下单的材料。
                player.addChatMessage(new ChatComponentTranslation("gtneverreset.projector.crafting.no_orders"));
                return;
            }
            // #tr gtneverreset.projector.crafting.result
            // # Submitted %s ME crafting orders. %s could not be started.
            // # zh_CN 已提交 %s 项 ME 合成订单，%s 项未能下单。
            player.addChatMessage(
                new ChatComponentTranslation("gtneverreset.projector.crafting.result", submitted, failed));
        }
    }
}
