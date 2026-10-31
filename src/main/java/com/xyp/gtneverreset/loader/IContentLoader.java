package com.xyp.gtneverreset.loader;

import java.io.File;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface IContentLoader {

    String name();

    default void preInit(FMLPreInitializationEvent event) {}

    default void init() {}

    default void postInit() {}

    default void loadComplete() {}

    default void serverStarting(FMLServerStartingEvent event) {}

    default void serverStopping() {}

    default void serverStopped() {}

    @SideOnly(Side.CLIENT)
    default void clientPreInit(File directory) {}

    @SideOnly(Side.CLIENT)
    default void clientInit() {}
}
