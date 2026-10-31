package com.xyp.gtnrlib;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.xyp.gtnrlib.client.GTNRLibClient;
import com.xyp.gtnrlib.command.CommandItemInfo;
import com.xyp.gtnrlib.command.CommandSuicide;
import com.xyp.gtnrlib.world.WorldListener;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = GTNRLib.MODID,
    name = GTNRLib.NAME,
    useMetadata = true,
    dependencies = "required-after:gtnhlib;required-after:modularui2;required-after:Baubles",
    acceptedMinecraftVersions = "1.7.10",
    guiFactory = "com.xyp.gtnrlib.config.GTNRLibGuiFactory")
public class GTNRLib {

    public static final String MODID = "gtnrlib", NAME = "GTNRLib", CONFIG_DIRECTORY = "GTNeverReset/GTNRLib";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide()
            .isClient()) GTNRLibClient.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        WorldListener listener = WorldListener.INSTANCE;
        MinecraftForge.EVENT_BUS.register(listener);
        LOG.info("GTNRLib initialized");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandItemInfo());
        event.registerServerCommand(new CommandSuicide());
    }
}
