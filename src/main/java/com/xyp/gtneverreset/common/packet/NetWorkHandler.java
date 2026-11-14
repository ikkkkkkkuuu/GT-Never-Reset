package com.xyp.gtneverreset.common.packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtnrlib.network.GregTechToolUsePacket;
import com.xyp.gtnrlib.network.PayloadHandler;
import com.xyp.gtnrlib.network.ServerboundPacket;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
        registerServerbound(SwitchGuisPacket.class);
        registerServerbound(TeleportToInterfacePacket.class);
        registerServerbound(ToggleInterfaceVisibilityPacket.class);
        registerServerbound(SendHeldItemToNetworkPacket.class);
        registerServerbound(TerminalItemRequestPacket.class);
        registerServerbound(OpenWirelessCardPacket.class);
        registerServerbound(ToggleWirelessCardPacket.class);
        CHANNEL
            .registerMessage(new PayloadHandler.Client<>(), WirelessHighlightPacket.class, nextPacketId++, Side.CLIENT);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            WirelessVisualisationPacket.class,
            nextPacketId++,
            Side.CLIENT);
        registerServerbound(QuantumCraftingModePacket.class);
        registerServerbound(RecursivePatternQueryPacket.class);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            QuantumCraftingAvailabilityPacket.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            QuantumCraftConfirmPacket.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            RecursivePatternResultPacket.class,
            nextPacketId++,
            Side.CLIENT);
        registerServerbound(ToolBeltSwapPacket.class);
        registerServerbound(ToolBeltResizePacket.class);
        CHANNEL.registerMessage(new PayloadHandler.Client<>(), ToolBeltSyncPacket.class, nextPacketId++, Side.CLIENT);
        registerServerbound(PackagedConnectorPacket.class);
        // ID 17 is reserved for PackagedConnectorPacket.
        nextPacketId = 18;
        CHANNEL.registerMessage(
            new PayloadHandler.Server<>(),
            WirelessMonitorRequestPacket.class,
            nextPacketId++,
            Side.SERVER);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            WirelessMonitorSnapshotPacket.class,
            nextPacketId++,
            Side.CLIENT);
        registerServerbound(VeinMiningSettingPacket.class);
        registerServerbound(TerminalMagnetPacket.class);
        // ID 21 is reserved for TerminalMagnetPacket.
        nextPacketId = 22;
        registerServerbound(QuestSkipPacket.class);
        registerServerbound(UniversalWandNetwork.OpenGuiMessage.class);
        registerServerbound(UniversalWandNetwork.ForceBedrockMessage.class);
        CHANNEL.registerMessage(
            new PayloadHandler.Server<>(),
            UniversalWandNetwork.ChainKeyMessage.class,
            nextPacketId++,
            Side.SERVER);
        registerServerbound(UniversalWandNetwork.ChainStartMessage.class);
        // IDs 27 through 31 belonged to the removed wand building packets.
        nextPacketId = 32;
        registerServerbound(UniversalWandNetwork.FlightSpeedMessage.class);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            UniversalWandNetwork.FlightSpeedSyncMessage.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            UniversalWandNetwork.ProtectionSyncMessage.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            UniversalWandNetwork.AccelerationSyncMessage.class,
            nextPacketId++,
            Side.CLIENT);
        registerServerbound(UniversalWandNetwork.AccelerationManagementMessage.class);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            UniversalWandNetwork.AccelerationNoticeMessage.class,
            nextPacketId++,
            Side.CLIENT);
        // Forge 1.7.10 dispatches these markers in the server's inbound packet loop. Keep the vanilla use
        // packet between them so its handled result is visible before granting travel.
        CHANNEL.registerMessage(
            new PayloadHandler.Server<>(),
            UniversalWandNetwork.TravelFallbackRequest.class,
            nextPacketId++,
            Side.SERVER);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            UniversalWandNetwork.TravelFallbackGrant.class,
            nextPacketId++,
            Side.CLIENT);
        CHANNEL.registerMessage(
            new PayloadHandler.Server<>(),
            UniversalWandNetwork.TravelFallbackFinish.class,
            nextPacketId++,
            Side.SERVER);
        CHANNEL.registerMessage(
            new PayloadHandler.Client<>(),
            UniversalWandNetwork.FeatureMessage.class,
            nextPacketId++,
            Side.CLIENT);
        // Older servers send these two acknowledgements on the original host channel.
        nextPacketId = GregTechToolUsePacket.register(CHANNEL, nextPacketId);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public static void sendToAllAround(IMessage packet, NetworkRegistry.TargetPoint target) {
        CHANNEL.sendToAllAround(packet, target);
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

    private static <T extends ServerboundPacket> void registerServerbound(Class<T> type) {
        var payloadHandler = new PayloadHandler.Server<T>();
        IMessageHandler<T, IMessage> handler = (message, context) -> {
            if (message.isInvalid()) return null;
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            var requestWorld = player.worldObj;
            SERVER_ACTIONS.add(() -> {
                if (!player.isDead && player.worldObj == requestWorld
                    && player.playerNetServerHandler.netManager.isChannelOpen())
                    payloadHandler.onMessage(message, context);
            });
            return null;
        };
        CHANNEL.registerMessage(handler, type, nextPacketId++, Side.SERVER);
    }
}
