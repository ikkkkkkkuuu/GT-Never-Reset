package com.xyp.gtneverreset.loader;

import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.common.wireless.WirelessMonitorService;
import com.xyp.gtneverreset.common.world.ToolBeltEvents;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public final class WorldLoader implements IContentLoader {

    @Override
    public String name() {
        return "world";
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new WirelessMonitorService());
        ToolBeltEvents toolBeltEvents = new ToolBeltEvents();
        MinecraftForge.EVENT_BUS.register(toolBeltEvents);
        FMLCommonHandler.instance()
            .bus()
            .register(toolBeltEvents);
    }
}
