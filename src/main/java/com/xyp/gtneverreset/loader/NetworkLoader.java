package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public final class NetworkLoader implements IContentLoader {

    @Override
    public String name() {
        return "network";
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        NetWorkHandler.register();
    }
}
