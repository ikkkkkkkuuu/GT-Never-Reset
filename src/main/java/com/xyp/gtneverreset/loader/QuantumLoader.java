package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.common.crafting.quantum.ae2.QuantumCraftingCache;
import com.xyp.gtneverreset.common.crafting.quantum.ae2.QuantumCraftingMode;
import com.xyp.gtneverreset.common.crafting.quantum.hooks.ticking.TickHandler;

import cpw.mods.fml.common.event.FMLServerStartingEvent;

public final class QuantumLoader implements IContentLoader {

    @Override
    public String name() {
        return "quantum";
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        TickHandler.instance()
            .openSession();
    }

    @Override
    public void serverStopping() {
        TickHandler.instance()
            .closeSession();
        QuantumCraftingMode.clear();
        QuantumCraftingCache.clearAll();
    }
}
