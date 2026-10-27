package com.xyp.gtneverreset.qa;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;
import com.xyp.gtneverreset.common.packet.WirelessMonitorRequestPacket;
import com.xyp.gtneverreset.common.packet.WirelessMonitorSnapshotPacket;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.utils.world.WirelessMonitorService;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "wirelessmonitorserverqa",
    name = "Wireless Monitor Server QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class WirelessMonitorServerChecks {

    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.worldServerForDimension(0) == null) return;
        finished = true;
        String result = "PASS";
        try {
            var player = FakePlayerFactory
                .get(server.worldServerForDimension(0), new GameProfile(new UUID(11, 12), "WirelessHudQA"));
            WirelessMonitorService.enqueue(player, 1);
            new WirelessMonitorService().onServerTick(event);
            var pending = WirelessMonitorService.class.getDeclaredField("PENDING");
            pending.setAccessible(true);
            if (!((Map<?, ?>) pending.get(null)).isEmpty()) throw new AssertionError("offline requests not discarded");
            WirelessMonitorService.enqueue(player, 2);
            WirelessMonitorService.reset();
            if (!((Map<?, ?>) pending.get(null)).isEmpty()) throw new AssertionError("session reset");
            if (MainConfig.wirelessMonitor.refreshTicks != 100) throw new AssertionError("config default");
            var buffer = Unpooled.buffer();
            try {
                new WirelessMonitorRequestPacket(41).toBytes(buffer);
                if (buffer.readableBytes() != 8) throw new AssertionError("request exposes more than a token");
                var request = new WirelessMonitorRequestPacket();
                request.fromBytes(buffer);
                if (request.requestId != 41) throw new AssertionError("token changed");
                buffer.clear();
                new WirelessMonitorSnapshotPacket(1, 2, new UUID(3, 4), BigInteger.TEN.pow(1000)).toBytes(buffer);
                var snapshot = new WirelessMonitorSnapshotPacket();
                snapshot.fromBytes(buffer);
                if (!snapshot.energy.equals(BigInteger.TEN.pow(1000))) throw new AssertionError("balance changed");
            } finally {
                buffer.release();
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        }
        try {
            Files.writeString(
                new File(System.getProperty("gtnr.wirelessMonitor.server.output"), "result.txt").toPath(),
                result);
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        FMLCommonHandler.instance()
            .exitJava(result.equals("PASS") ? 0 : 1, true);
    }
}
