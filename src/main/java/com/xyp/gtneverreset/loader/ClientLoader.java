package com.xyp.gtneverreset.loader;

import java.io.File;

import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.client.VeinMiningInputHandler;
import com.xyp.gtneverreset.client.event.WindowsTaskbarButton;
import com.xyp.gtneverreset.client.quantum.QuantumCraftingClientEvents;
import com.xyp.gtneverreset.client.quantum.RecursivePatternReport;
import com.xyp.gtneverreset.client.render.FluxConnectorRender;
import com.xyp.gtneverreset.client.render.PackagedCoreRenderer;
import com.xyp.gtneverreset.client.render.StableEndCrystalRender;
import com.xyp.gtneverreset.client.render.WirelessConnectorRenderer;
import com.xyp.gtneverreset.client.terminal.WirelessTerminalInputHandler;
import com.xyp.gtneverreset.client.wireless.WirelessCardInput;
import com.xyp.gtneverreset.client.wireless.WirelessMonitorHud;
import com.xyp.gtneverreset.client.wireless.WirelessWorldHighlightRender;
import com.xyp.gtneverreset.common.block.blocks.BlockFluxConnector;
import com.xyp.gtneverreset.common.entity.StableEndCrystalEntity;
import com.xyp.gtneverreset.compat.betterquesting.QuestSkipMenu;
import com.xyp.gtneverreset.compat.bloodmagic.BloodMagicCompat;
import com.xyp.gtneverreset.compat.nei.NEIWirelessTerminalConfig;
import com.xyp.gtneverreset.compat.nei.WildcardRecipeImport;
import com.xyp.gtneverreset.compat.thaumcraft.ThaumcraftCompat;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtnrlib.mod.ModList;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import betterquesting.api2.client.gui.context.QuestContextMenuRegistry;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import ggfab.GGItemList;
import gregtech.api.enums.ItemList;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class ClientLoader {

    public static void preInit(File configDirectory) {
        NEIWirelessTerminalConfig.migrateLegacySettings(configDirectory);
        RadialMenuLoader.preInit();
    }

    public static void init() {
        if (ModList.BetterQuesting.isModLoaded()) QuestContextMenuRegistry.register(new QuestSkipMenu());
        MinecraftForge.EVENT_BUS.register(new VeinMiningInputHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new WindowsTaskbarButton());
        registerPackagedCoreRenderers();
        MinecraftForge.EVENT_BUS.register(new WirelessConnectorRenderer());
        RadialMenuLoader.init();
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
        ClientRegistry.registerKeyBinding(WirelessMonitorHud.TOGGLE);
        ClientRegistry.registerKeyBinding(WirelessMonitorHud.CONFIGURE);
        ClientRegistry.registerKeyBinding(WirelessMonitorHud.MOVE);
        MinecraftForge.EVENT_BUS.register(WirelessMonitorHud.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(WirelessMonitorHud.INSTANCE);
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

    private static void registerPackagedCoreRenderers() {
        if (ModList.Thaumcraft.isModLoaded()) ThaumcraftCompat.registerRenderers();
        if (ModList.BloodMagic.isModLoaded()) BloodMagicCompat.registerRenderers();
        MinecraftForgeClient.registerItemRenderer(
            GTNRItemList.AssemblyLineCore.getItem(),
            new PackagedCoreRenderer(() -> ItemList.Machine_Multi_Assemblyline.get(1)));
        MinecraftForgeClient.registerItemRenderer(
            GTNRItemList.AdvancedAssemblyLineCore.getItem(),
            new PackagedCoreRenderer(() -> GGItemList.AdvAssLine.get(1)));
        MinecraftForgeClient.registerItemRenderer(
            GTNRItemList.CircuitAssemblyLineCore.getItem(),
            new PackagedCoreRenderer(() -> ModsItemlist.CircuitAssemblyLine.get(1)));
    }

    public static final class Card implements IContentLoader {

        @Override
        public String name() {
            return "client";
        }

        @Override
        @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public void clientPreInit(File directory) {
            ClientLoader.preInit(directory);
        }

        @Override
        @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public void clientInit() {
            ClientLoader.init();
        }
    }
}
