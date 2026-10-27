package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;
import com.xyp.gtneverreset.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "wildcardserverqa",
    name = "Wildcard Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NEVER_RESET)
public final class WildcardServerChecks {

    private static final GameProfile PROFILE = new GameProfile(
        UUID.fromString("72577873-70a0-4e38-8ab3-46cc3685ee8f"),
        "WildcardServerQA");

    private EntityPlayerMP player;
    private int ticks;
    private boolean prepared;
    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtnr.wildcard.server.qa")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null) return;
        WorldServer world = server.worldServerForDimension(0);
        if (world == null) return;
        try {
            if (!prepared) {
                WildcardCoreChecks.verify();
                world.getChunkProvider()
                    .loadChunk(0, 0);
                world.getChunkProvider()
                    .loadChunk(1, 0);
                player = FakePlayerFactory.get(world, PROFILE);
                player.setPosition(13, 12, 0);
                WildcardIntegrationChecks.prepare(player);
                prepared = true;
            } else if (++ticks >= 20) {
                WildcardIntegrationChecks.verify(player);
                finish(server, "PASS");
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(server, "FAIL");
        }
    }

    private void finish(MinecraftServer server, String result) {
        finished = true;
        try {
            File directory = new File(System.getProperty("gtnr.wildcard.server.qa.output"));
            directory.mkdirs();
            Files.write(new File(directory, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            System.out.println("WILDCARD_SERVER_QA " + result);
        } catch (Exception failure) {
            failure.printStackTrace();
        } finally {
            server.initiateShutdown();
        }
    }
}
