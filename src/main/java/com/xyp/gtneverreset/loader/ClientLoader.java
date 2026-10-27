package com.xyp.gtneverreset.loader;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.event.WirelessCardInput;
import com.xyp.gtneverreset.client.event.WirelessTerminalInputHandler;
import com.xyp.gtneverreset.client.gui.WirelessMonitorHud;
import com.xyp.gtneverreset.client.nei.NEIWirelessTerminalConfig;
import com.xyp.gtneverreset.client.nei.WildcardRecipeImport;
import com.xyp.gtneverreset.client.quantum.QuantumCraftingClientEvents;
import com.xyp.gtneverreset.client.quantum.RecursivePatternReport;
import com.xyp.gtneverreset.client.render.FluxConnectorRender;
import com.xyp.gtneverreset.client.render.PackagedCoreRenderer;
import com.xyp.gtneverreset.client.render.StableEndCrystalRender;
import com.xyp.gtneverreset.client.render.WirelessConnectorRenderer;
import com.xyp.gtneverreset.client.render.WirelessWorldHighlightRender;
import com.xyp.gtneverreset.client.text.EffectTextRenderer;
import com.xyp.gtneverreset.client.text.TextEffectRegistry;
import com.xyp.gtneverreset.client.text.effect.ShaderTextEffect;
import com.xyp.gtneverreset.common.block.blocks.BlockFluxConnector;
import com.xyp.gtneverreset.common.entity.StableEndCrystalEntity;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtneverreset.utils.text.effect.TextEffects;

import WayofTime.alchemicalWizardry.ModBlocks;
import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import ggfab.GGItemList;
import gregtech.api.enums.ItemList;
import thaumcraft.common.config.ConfigBlocks;

public final class ClientLoader {

    private ClientLoader() {}

    public static void preInit(File configDirectory) {
        NEIWirelessTerminalConfig.migrateLegacySettings(configDirectory);
        RadialMenuLoader.preInit();
    }

    public static void init() {
        registerPackagedCoreRenderers();
        MinecraftForge.EVENT_BUS.register(new WirelessConnectorRenderer());
        TextEffectRegistry.register(
            TextEffects.EXOTIC_RAINBOW.rendererId(),
            new ShaderTextEffect(
                new ResourceLocation(GTNeverReset.MODID, "shaders/text/exotic_rainbow.frag.glsl"),
                0xFF6B6B,
                0x7DC4E1,
                0xD3EB6C));
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(EffectTextRenderer.INSTANCE);
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
        if (ModList.Thaumcraft.isModLoaded()) {
            MinecraftForgeClient.registerItemRenderer(
                GTNRItemList.ThaumcraftCrucibleCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ConfigBlocks.blockMetalDevice, 1, 0)));
            MinecraftForgeClient.registerItemRenderer(
                GTNRItemList.ArcaneWorkbenchCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ConfigBlocks.blockTable, 1, 15)));
            MinecraftForgeClient.registerItemRenderer(
                GTNRItemList.ThaumcraftInfusionCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ConfigBlocks.blockStoneDevice, 1, 2)));
        }
        if (ModList.BloodMagic.isModLoaded()) {
            MinecraftForgeClient.registerItemRenderer(
                GTNRItemList.BloodAltarCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ModBlocks.blockAltar)));
        }
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

}
