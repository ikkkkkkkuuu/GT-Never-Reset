package com.xyp.gtneverreset.loader;

import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.common.flux.FluxTransferScheduler;
import com.xyp.gtneverreset.common.wireless.WirelessAutoConnect;
import com.xyp.gtneverreset.common.wireless.WirelessChannelManager;
import com.xyp.gtneverreset.common.wireless.WirelessMonitorService;
import com.xyp.gtneverreset.common.world.WirelessPowerEvents;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public final class WirelessLoader implements IContentLoader {

    @Override
    public String name() {
        return "wireless";
    }

    @Override
    public void init() {
        var eventBus = FMLCommonHandler.instance()
            .bus();
        MinecraftForge.EVENT_BUS.register(WirelessChannelManager.INSTANCE);
        eventBus.register(WirelessChannelManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WirelessAutoConnect.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WirelessPowerEvents.INSTANCE);
        eventBus.register(WirelessPowerEvents.INSTANCE);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        WirelessMonitorService.reset();
    }

    @Override
    public void serverStopped() {
        WirelessPowerEvents.INSTANCE.clear();
        WirelessMonitorService.reset();
        FluxTransferScheduler.clear();
        WirelessChannelManager.clear();
    }
}
