package com.xyp.gtneverreset;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.factory.GuiManager;
import com.gtnewhorizon.gtnhlib.chat.ChatComponentCustomRegistry;
import com.xyp.gtneverreset.common.gui.AdvancedIOGui;
import com.xyp.gtneverreset.common.gui.StockIOGui;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.world.WirelessWorldListener;
import com.xyp.gtneverreset.loader.BlockLoader;
import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.loader.EntityLoader;
import com.xyp.gtneverreset.loader.ItemLoader;
import com.xyp.gtneverreset.loader.MachineLoader;
import com.xyp.gtneverreset.loader.RecipeLoader;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingCache;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingMode;
import com.xyp.gtneverreset.utils.crafting.quantum.hooks.ticking.TickHandler;
import com.xyp.gtneverreset.utils.machine.ChatComponentInterfaceNameSuffix;
import com.xyp.gtneverreset.utils.machine.flux.FluxTransferScheduler;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessAutoConnect;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager;

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
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ConfigLoader.register(event.getModConfigurationDirectory());
        ItemLoader.registerItems();
        BlockLoader.registerBlocks();
        EntityLoader.registerEntities();
        NetWorkHandler.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(GTNeverReset.INSTANCE, new WirelessTerminalGuiHandler());

        GTNeverReset.LOG.info("loading {} version {}", GTNeverReset.NAME, Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        GuiManager.registerFactory(AdvancedIOGui.FACTORY);
        GuiManager.registerFactory(StockIOGui.FACTORY);
        MachineLoader.registry();
        MinecraftForge.EVENT_BUS.register(WirelessChannelManager.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(WirelessChannelManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WirelessAutoConnect.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new WirelessWorldListener());
    }

    public void postInit(FMLPostInitializationEvent event) {
        var interfaceTerminal = AEApi.instance()
            .registries()
            .interfaceTerminal();
        interfaceTerminal.register(AssemblerMatrix.class);
        interfaceTerminal.register(SuperMTEHatchCraftingInputME.class);
        interfaceTerminal.register(CircuitMEPatternBuffer.class);
        ChatComponentCustomRegistry.register(ChatComponentInterfaceNameSuffix::new);
        RecipeLoader.registerRecipes();
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        RecipeLoader.registerLateRecipes();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        TickHandler.instance()
            .openSession();
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        TickHandler.instance()
            .closeSession();
        QuantumCraftingMode.clear();
        QuantumCraftingCache.clearAll();
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        FluxTransferScheduler.clear();
        WirelessChannelManager.clear();
    }

    public ITexture createVaultCasingTexture(Block block) {
        return TextureFactory.of(block, 0);
    }

    public Object createTerminalGui(int id, EntityPlayer player, World world, int slot) {
        return null;
    }

    public boolean preferOwnInterfaceNames() {
        return false;
    }
}
