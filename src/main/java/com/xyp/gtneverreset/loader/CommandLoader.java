package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.command.CommandWirelessFrequency;

import cpw.mods.fml.common.event.FMLServerStartingEvent;

public final class CommandLoader implements IContentLoader {

    @Override
    public String name() {
        return "command";
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWirelessFrequency());
    }
}
