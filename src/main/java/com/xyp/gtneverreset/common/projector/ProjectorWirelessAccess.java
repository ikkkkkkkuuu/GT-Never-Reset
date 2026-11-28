package com.xyp.gtneverreset.common.projector;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtneverreset.common.wireless.WirelessCardInventory;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileWireless;
import appeng.util.Platform;

final class ProjectorWirelessAccess {

    private static final double TERMINAL_START_POWER = 0.5;

    private ProjectorWirelessAccess() {}

    static Map<IGrid, Link> find(EntityPlayerMP player, SecurityPermissions permission) {
        var wireless = AEApi.instance()
            .registries()
            .wireless();
        Map<IGrid, Link> networks = new LinkedHashMap<>();
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
            if (grid == null || networks.containsKey(grid)) continue;
            ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            if (security == null || !security.hasPermission(player, permission)) continue;
            IActionHost access = activeAccess(host, grid, handler.hasInfinityRange(terminal));
            if (access != null) networks.put(grid, new Link(host, new PlayerSource(player, access)));
        }
        return networks;
    }

    private static IActionHost activeAccess(WirelessTerminalGuiObject host, IGrid grid, boolean unlimited) {
        var node = host.getActionableNode();
        if (node != null && node.isActive() && node.getGrid() == grid) return host;
        if (unlimited) {
            // An unlimited-range terminal may encounter an inactive wireless point before an active point.
            for (var candidate : grid.getMachines(TileWireless.class)) {
                if (candidate.isActive() && candidate.getMachine() instanceof TileWireless access && access.isActive())
                    return access;
            }
        }
        return null;
    }

    @Desugar
    record Link(WirelessTerminalGuiObject host, PlayerSource source) {}
}
