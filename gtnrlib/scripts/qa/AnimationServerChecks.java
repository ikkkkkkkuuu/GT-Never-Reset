package com.xyp.gtnrlib.qa;

import java.nio.file.Files;
import java.nio.file.Path;

import com.xyp.gtnrlib.api.animation.AnimationLibrary;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = "animationserverqa",
    name = "GTNRLib dedicated server check",
    version = "1",
    dependencies = "required-after:gtnrlib")
public class AnimationServerChecks {

    @Mod.EventHandler
    public void ready(FMLServerStartedEvent event) throws Exception {
        if (FMLCommonHandler.instance()
            .getSide() != Side.SERVER) throw new AssertionError("expected dedicated server");
        if (!new AnimationLibrary().reload(message -> { throw new AssertionError(message); }))
            throw new AssertionError("failed to load");
        boolean host = cpw.mods.fml.common.Loader.isModLoaded("gtneverreset");
        boolean gt = cpw.mods.fml.common.Loader.isModLoaded("gregtech");
        if (host != Boolean.getBoolean("gtnrlib.animation.qa.expectHost")
            || gt != Boolean.getBoolean("gtnrlib.animation.qa.toolMods"))
            throw new AssertionError("Unexpected mod list, GT-Never-Reset=" + host + ", GregTech=" + gt);
        Files.writeString(
            Path.of(System.getProperty("gtnrlib.animation.server.result")),
            "PASS dedicated server with GTNRLib, GT-Never-Reset=" + host + ", GregTech=" + gt);
        FMLCommonHandler.instance()
            .getMinecraftServerInstance()
            .initiateShutdown();
    }
}
