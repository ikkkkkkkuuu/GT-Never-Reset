package com.xyp.gtneverreset.loader;

import com.cleanroommc.modularui.factory.GuiManager;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.gui.AdvancedIOGui;
import com.xyp.gtneverreset.common.gui.StockIOGui;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

public final class GuiLoader implements IContentLoader {

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(GTNeverReset.INSTANCE, new WirelessTerminalGuiHandler());
    }

    @Override
    public void init() {
        GuiManager.registerFactory(AdvancedIOGui.FACTORY);
        GuiManager.registerFactory(StockIOGui.FACTORY);
    }
}
