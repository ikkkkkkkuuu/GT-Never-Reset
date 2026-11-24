package com.xyp.gtnrlib.animation;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.xyp.gtnrlib.network.GTNRLibNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Tracks the pose of the real server-side projector container for other viewers. */
public final class ProjectorArmSessions {

    private static final ProjectorArmSessions INSTANCE = new ProjectorArmSessions();
    private final Map<EntityPlayerMP, Session> sessions = new IdentityHashMap<>();

    private ProjectorArmSessions() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void opened(EntityPlayerMP player, Container previous) {
        Container current = player.openContainer;
        if (current == previous || current == player.inventoryContainer) return;
        var session = new Session(current, player.getEntityId(), player.dimension);
        Session old = INSTANCE.sessions.put(player, session);
        if (old != null && old.dimension != session.dimension)
            GTNRLibNetwork.broadcastProjectorPose(old.entity, old.dimension, false);
        GTNRLibNetwork.broadcastProjectorPose(session.entity, session.dimension, true);
    }

    public static void clear() {
        INSTANCE.sessions.clear();
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || sessions.isEmpty()) return;
        var iterator = sessions.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var player = entry.getKey();
            var session = entry.getValue();
            if (player.openContainer == session.container && player.dimension == session.dimension
                && !player.isDead
                && player.playerNetServerHandler != null
                && player.playerNetServerHandler.netManager.isChannelOpen()) continue;
            GTNRLibNetwork.broadcastProjectorPose(session.entity, session.dimension, false);
            iterator.remove();
        }
    }

    @SubscribeEvent
    public void tracking(PlayerEvent.StartTracking event) {
        if (event.entityPlayer instanceof EntityPlayerMP observer && event.target instanceof EntityPlayerMP target) {
            Session session = sessions.get(target);
            if (session != null) GTNRLibNetwork.sendProjectorPose(observer, session.entity, session.dimension);
        }
    }

    private record Session(Container container, int entity, int dimension) {}
}
