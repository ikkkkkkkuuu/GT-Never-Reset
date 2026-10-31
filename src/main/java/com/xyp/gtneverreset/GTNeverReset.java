package com.xyp.gtneverreset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

@Mod(
    modid = GTNeverReset.MODID,
    version = Tags.VERSION,
    name = GTNeverReset.NAME,
    dependencies = "required-after:gtnrlib;required-after:" + ModList.ModIds.GREGTECH
        + ";required-after:"
        + ModList.ModIds.GTNH_LIB
        + ";required-after:"
        + ModList.ModIds.APPLIED_ENERGISTICS_2
        + ";required-after:"
        + ModList.ModIds.AE2_FLUID_CRAFT
        + ";required-after:"
        + ModList.ModIds.BAUBLES
        + ";",
    acceptedMinecraftVersions = "[1.7.10]",
    guiFactory = "com.xyp.gtneverreset.config.ConfigGuiFactory")
public class GTNeverReset {

    public static final String MODID = ModList.ModIds.GT_NEVER_RESET;
    public static final String NAME = ModList.Names.GT_NEVER_RESET;
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static GTNeverReset INSTANCE;

    @SidedProxy(clientSide = "com.xyp.gtneverreset.ClientProxy", serverSide = "com.xyp.gtneverreset.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }
}
