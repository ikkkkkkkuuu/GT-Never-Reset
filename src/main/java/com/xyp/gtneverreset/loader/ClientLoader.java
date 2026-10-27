package com.xyp.gtneverreset.loader;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.event.WirelessCardInput;
import com.xyp.gtneverreset.client.event.WirelessTerminalInputHandler;
import com.xyp.gtneverreset.client.nei.NEIWirelessTerminalConfig;
import com.xyp.gtneverreset.client.nei.WildcardRecipeImport;
import com.xyp.gtneverreset.client.quantum.QuantumCraftingClientEvents;
import com.xyp.gtneverreset.client.quantum.RecursivePatternReport;
import com.xyp.gtneverreset.client.render.FluxConnectorRender;
import com.xyp.gtneverreset.client.render.StableEndCrystalRender;
import com.xyp.gtneverreset.client.render.WirelessWorldHighlightRender;
import com.xyp.gtneverreset.common.block.blocks.BlockFluxConnector;
import com.xyp.gtneverreset.common.entity.StableEndCrystalEntity;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;

public final class ClientLoader {

    private ClientLoader() {}

    public static void preInit(File configDirectory) {
        NEIWirelessTerminalConfig.migrateLegacySettings(configDirectory);
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(StableEndCrystalEntity.class, new StableEndCrystalRender());
        BlockFluxConnector.renderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new FluxConnectorRender());
        AEApi.instance()
            .partHelper()
            .setItemBusRenderer((IPartItem) GTNRItemList.AdvancedIOBus.getItem());
        AEApi.instance()
            .partHelper()
            .setItemBusRenderer((IPartItem) GTNRItemList.StockIOInterfacePart.getItem());
        WirelessCardInput.register();
        MinecraftForge.EVENT_BUS.register(new WildcardRecipeImport());
        MinecraftForge.EVENT_BUS.register(WirelessWorldHighlightRender.INSTANCE);
        RecursivePatternReport.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new QuantumCraftingClientEvents());
        ClientRegistry.registerKeyBinding(WirelessTerminalInputHandler.OPEN_TERMINAL);
        ClientRegistry.registerKeyBinding(WirelessTerminalInputHandler.SEND_HELD_ITEM);
        FMLCommonHandler.instance()
            .bus()
            .register(new WirelessTerminalInputHandler());
    }
}
