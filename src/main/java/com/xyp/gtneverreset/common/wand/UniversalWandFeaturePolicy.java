package com.xyp.gtneverreset.common.wand;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.packet.UniversalWandNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/** The connected server owns feature availability; a client config cannot override it. */
public final class UniversalWandFeaturePolicy {

    private static volatile Long clientMask;
    private static volatile long serverMask = -1L;
    private long lastServerMask = Long.MIN_VALUE;

    public static boolean allowed(UniversalWandFeature feature) {
        return (mask() & (1L << feature.ordinal())) != 0;
    }

    static long mask() {
        Long remote = clientMask;
        return remote == null || FMLCommonHandler.instance()
            .getEffectiveSide()
            .isServer() ? serverMask : remote;
    }

    public static void receive(long mask) {
        clientMask = mask;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        clientMask = 0L;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clientMask = null;
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            serverMask = UniversalWandFeature.configuredMask();
            sanitize(player);
            UniversalWandNetwork.syncFeatures(player, serverMask);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        long mask = UniversalWandFeature.configuredMask();
        serverMask = mask;
        if (mask == lastServerMask) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) return;
        lastServerMask = mask;
        for (Object entry : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) entry;
            sanitize(player);
            UniversalWandNetwork.syncFeatures(player, mask);
        }
    }

    private static void sanitize(EntityPlayerMP player) {
        boolean changed = false;
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            changed |= ItemUniversalWand.sanitizeDisabledFeatures(stack);
        }
        if (changed) {
            player.inventory.markDirty();
            player.sendContainerToPlayer(player.inventoryContainer);
        }
    }
}
