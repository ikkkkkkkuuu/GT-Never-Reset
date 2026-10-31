package com.xyp.gtneverreset.loader;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.factory.GuiManager;
import com.gtnewhorizon.gtnhlib.chat.ChatComponentCustomRegistry;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.Tags;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityPackagedProvider;
import com.xyp.gtneverreset.common.command.CommandWirelessFrequency;
import com.xyp.gtneverreset.common.gui.AdvancedIOGui;
import com.xyp.gtneverreset.common.gui.StockIOGui;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.world.ToolBeltEvents;
import com.xyp.gtneverreset.common.world.WirelessPowerEvents;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingCache;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingMode;
import com.xyp.gtneverreset.utils.crafting.quantum.hooks.ticking.TickHandler;
import com.xyp.gtneverreset.utils.machine.ChatComponentInterfaceNameSuffix;
import com.xyp.gtneverreset.utils.machine.flux.FluxTransferScheduler;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessAutoConnect;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager;
import com.xyp.gtneverreset.utils.world.WirelessMonitorService;

import appeng.api.AEApi;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class LifecycleLoader {

    private static Phase phase = Phase.NOT_STARTED;

    public static void preInit(FMLPreInitializationEvent event) {
        advance(Phase.NOT_STARTED, Phase.PRE_INIT);
        ConfigLoader.register(event.getModConfigurationDirectory());
        ItemLoader.registerItems();
        BlockLoader.registerBlocks();
        EntityLoader.registerEntities();
        NetWorkHandler.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new WirelessMonitorService());
        ToolBeltEvents toolBeltEvents = new ToolBeltEvents();
        MinecraftForge.EVENT_BUS.register(toolBeltEvents);
        FMLCommonHandler.instance()
            .bus()
            .register(toolBeltEvents);
        NetworkRegistry.INSTANCE.registerGuiHandler(GTNeverReset.INSTANCE, new WirelessTerminalGuiHandler());

        GTNeverReset.LOG.info("loading {} version {}", GTNeverReset.NAME, Tags.VERSION);
    }

    public static void init(FMLInitializationEvent event) {
        advance(Phase.PRE_INIT, Phase.INIT);
        GuiManager.registerFactory(AdvancedIOGui.FACTORY);
        GuiManager.registerFactory(StockIOGui.FACTORY);
        MachineLoader.registry();
        var eventBus = FMLCommonHandler.instance()
            .bus();
        MinecraftForge.EVENT_BUS.register(WirelessChannelManager.INSTANCE);
        eventBus.register(WirelessChannelManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WirelessAutoConnect.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WirelessPowerEvents.INSTANCE);
        eventBus.register(WirelessPowerEvents.INSTANCE);
    }

    public static void postInit(FMLPostInitializationEvent event) {
        advance(Phase.INIT, Phase.POST_INIT);
        var interfaceTerminal = AEApi.instance()
            .registries()
            .interfaceTerminal();
        interfaceTerminal.register(AssemblerMatrix.class);
        interfaceTerminal.register(SuperMTEHatchCraftingInputME.class);
        interfaceTerminal.register(CircuitMEPatternBuffer.class);
        interfaceTerminal.register(TileEntityOverloadedProvider.class);
        interfaceTerminal.register(TileEntityPackagedProvider.class);
        ChatComponentCustomRegistry.register(ChatComponentInterfaceNameSuffix::new);
        RecipeLoader.registerRecipes();
    }

    public static void loadComplete(FMLLoadCompleteEvent event) {
        advance(Phase.POST_INIT, Phase.LOAD_COMPLETE);
        RecipeLoader.registerLateRecipes();
    }

    public static void serverStarting(FMLServerStartingEvent event) {
        require(Phase.LOAD_COMPLETE);
        event.registerServerCommand(new CommandWirelessFrequency());
        WirelessMonitorService.reset();
        TickHandler.instance()
            .openSession();
    }

    public static void serverStopping(FMLServerStoppingEvent event) {
        TickHandler.instance()
            .closeSession();
        QuantumCraftingMode.clear();
        QuantumCraftingCache.clearAll();
    }

    public static void serverStopped(FMLServerStoppedEvent event) {
        WirelessPowerEvents.INSTANCE.clear();
        WirelessMonitorService.reset();
        FluxTransferScheduler.clear();
        WirelessChannelManager.clear();
    }

    @SideOnly(Side.CLIENT)
    public static void clientPreInit(File configDirectory) {
        require(Phase.PRE_INIT);
        ClientLoader.preInit(configDirectory);
    }

    @SideOnly(Side.CLIENT)
    public static void clientInit() {
        require(Phase.INIT);
        ClientLoader.init();
    }

    private static void advance(Phase expected, Phase next) {
        require(expected);
        phase = next;
    }

    private static void require(Phase expected) {
        if (phase != expected) throw new IllegalStateException("Expected loading phase " + expected + ", got " + phase);
    }

    private enum Phase {
        NOT_STARTED,
        PRE_INIT,
        INIT,
        POST_INIT,
        LOAD_COMPLETE
    }
}
