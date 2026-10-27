package com.xyp.gtneverreset.common.packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.packet.quantum.QuantumCraftConfirmPacket;
import com.xyp.gtneverreset.common.packet.quantum.QuantumCraftingAvailabilityPacket;
import com.xyp.gtneverreset.common.packet.quantum.QuantumCraftingModePacket;
import com.xyp.gtneverreset.common.packet.quantum.RecursivePatternQueryPacket;
import com.xyp.gtneverreset.common.packet.quantum.RecursivePatternResultPacket;
import com.xyp.gtneverreset.common.packet.terminal.CPacketSendHeldItemToNetwork;
import com.xyp.gtneverreset.common.packet.terminal.CPacketSwitchGuis;
import com.xyp.gtneverreset.common.packet.terminal.CPacketTeleportToInterface;
import com.xyp.gtneverreset.common.packet.terminal.CPacketToggleInterfaceVisibility;
import com.xyp.gtneverreset.common.packet.terminal.TerminalItemRequestPacket;
import com.xyp.gtneverreset.common.packet.wireless.OpenWirelessCardPacket;
import com.xyp.gtneverreset.common.packet.wireless.ToggleWirelessCardPacket;
import com.xyp.gtneverreset.common.packet.wireless.WirelessHighlightPacket;
import com.xyp.gtneverreset.common.packet.wireless.WirelessVisualisationPacket;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingMode;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class NetWorkHandler {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(GTNeverReset.MODID);
    private static final Queue<Runnable> SERVER_ACTIONS = new ConcurrentLinkedQueue<>();
    private static final NetWorkHandler INSTANCE = new NetWorkHandler();
    private static int nextPacketId;
    private static boolean initialized;

    private NetWorkHandler() {}

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;
        nextPacketId = 0;
        registerServerbound(CPacketSwitchGuis.class, CPacketSwitchGuis::handle);
        registerServerbound(CPacketTeleportToInterface.class, CPacketTeleportToInterface::handle);
        registerServerbound(CPacketToggleInterfaceVisibility.class, CPacketToggleInterfaceVisibility::handle);
        registerServerbound(CPacketSendHeldItemToNetwork.class, CPacketSendHeldItemToNetwork::handle);
        registerServerbound(TerminalItemRequestPacket.class, TerminalItemRequestPacket::handle);
        registerServerbound(OpenWirelessCardPacket.class, OpenWirelessCardPacket::handle);
        registerServerbound(ToggleWirelessCardPacket.class, ToggleWirelessCardPacket::handle);
        CHANNEL.registerMessage(
            WirelessHighlightPacket.Handler.class,
            WirelessHighlightPacket.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            WirelessVisualisationPacket.Handler.class,
            WirelessVisualisationPacket.class,
            nextPacketId++,
            Side.CLIENT);
        registerServerbound(QuantumCraftingModePacket.class, QuantumCraftingModePacket::handleServer);
        registerServerbound(RecursivePatternQueryPacket.class, RecursivePatternQueryPacket::handleServer);
        CHANNEL.registerMessage(
            QuantumCraftingAvailabilityPacket.Handler.class,
            QuantumCraftingAvailabilityPacket.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            QuantumCraftConfirmPacket.Handler.class,
            QuantumCraftConfirmPacket.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            RecursivePatternResultPacket.Handler.class,
            RecursivePatternResultPacket.class,
            nextPacketId++,
            Side.CLIENT);
        registerServerbound(ToolBeltSwapPacket.class, ToolBeltSwapPacket::handle);
        registerServerbound(ToolBeltResizePacket.class, ToolBeltResizePacket::handle);
        CHANNEL
            .registerMessage(ToolBeltSyncPacket.Handler.class, ToolBeltSyncPacket.class, nextPacketId++, Side.CLIENT);
        registerServerbound(PackagedConnectorPacket.class, PackagedConnectorPacket::handle);
        // ID 17 is reserved for PackagedConnectorPacket.
        nextPacketId = 18;
        CHANNEL.registerMessage(
            WirelessMonitorRequestPacket.Handler.class,
            WirelessMonitorRequestPacket.class,
            nextPacketId++,
            Side.SERVER);
        CHANNEL.registerMessage(
            WirelessMonitorSnapshotPacket.Handler.class,
            WirelessMonitorSnapshotPacket.class,
            nextPacketId++,
            Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public static void sendToServer(IMessage packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendTo(IMessage packet, EntityPlayerMP player) {
        CHANNEL.sendTo(packet, player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Runnable action;
        while ((action = SERVER_ACTIONS.poll()) != null) action.run();
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        QuantumCraftingMode.onPlayerLoggedOut(event.player);
    }

    private static <T extends IMessage> void registerServerbound(Class<T> type, BiConsumer<T, EntityPlayerMP> action) {
        IMessageHandler<T, IMessage> handler = (message, context) -> {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            var requestWorld = player.worldObj;
            SERVER_ACTIONS.add(() -> {
                if (!player.isDead && player.worldObj == requestWorld
                    && player.playerNetServerHandler.netManager.isChannelOpen()) action.accept(message, player);
            });
            return null;
        };
        CHANNEL.registerMessage(handler, type, nextPacketId++, Side.SERVER);
    }
}
