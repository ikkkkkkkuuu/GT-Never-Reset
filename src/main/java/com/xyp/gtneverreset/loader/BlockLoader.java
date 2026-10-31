package com.xyp.gtneverreset.loader;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.block.blocks.BlockEddingtonCasing;
import com.xyp.gtneverreset.common.block.blocks.BlockFluxConnector;
import com.xyp.gtneverreset.common.block.blocks.BlockOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.BlockPackagedProvider;
import com.xyp.gtneverreset.common.block.blocks.BlockStockIOInterface;
import com.xyp.gtneverreset.common.block.blocks.item.ItemBlockFluxConnector;
import com.xyp.gtneverreset.common.block.blocks.item.ItemBlockStockIOInterface;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityFluxPlug;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityFluxPoint;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityPackagedProvider;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityStockIOInterface;
import com.xyp.gtneverreset.common.flux.FluxTransferScheduler;
import com.xyp.gtneverreset.common.world.FluxChunkLoading;
import com.xyp.gtneverreset.utils.enums.BlockIcons;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;

public final class BlockLoader {

    public static BlockEddingtonCasing eddingtonWallCasing;

    public static void registerBlocks() {
        BlockIcons.registerIcons();
        eddingtonWallCasing = new BlockEddingtonCasing();
        GameRegistry.registerBlock(eddingtonWallCasing, BlockEddingtonCasing.REGISTRY_NAME);
        GTNRItemList.EddingtonWallCasing.set(new ItemStack(eddingtonWallCasing));
        BlockStockIOInterface stockInterface = new BlockStockIOInterface();
        GameRegistry
            .registerBlock(stockInterface, ItemBlockStockIOInterface.class, BlockStockIOInterface.REGISTRY_NAME);
        GameRegistry.registerTileEntity(
            TileEntityStockIOInterface.class,
            ModList.GTNeverReset.getResourcePath(BlockStockIOInterface.REGISTRY_NAME));
        GTNRItemList.StockIOInterface.set(new ItemStack(stockInterface));
        stockInterface.setCreativeTab(GTNRCreativeTabs.BLOCKS);
        BlockPackagedProvider packagedProvider = new BlockPackagedProvider();
        GameRegistry.registerBlock(packagedProvider, "wireless_packaged_pattern_provider");
        GameRegistry.registerTileEntity(
            TileEntityPackagedProvider.class,
            ModList.GTNeverReset.getResourcePath("wireless_packaged_pattern_provider"));
        GTNRItemList.WirelessPackagedPatternProvider.set(new ItemStack(packagedProvider));
        BlockOverloadedProvider overloadedProvider = new BlockOverloadedProvider();
        GameRegistry.registerBlock(overloadedProvider, "overloaded_wireless_pattern_provider");
        GameRegistry.registerTileEntity(
            TileEntityOverloadedProvider.class,
            ModList.GTNeverReset.getResourcePath("overloaded_wireless_pattern_provider"));
        GTNRItemList.OverloadedWirelessPatternProvider.set(new ItemStack(overloadedProvider));
        registerFluxBlocks();
    }

    private static void registerFluxBlocks() {
        // #tr tile.flux_plug.name
        // # Flux Plug
        // # zh_CN 通量插头
        BlockFluxConnector plug = new BlockFluxConnector(true, "flux_plug");
        // #tr tile.flux_point.name
        // # Flux Point
        // # zh_CN 通量点
        BlockFluxConnector point = new BlockFluxConnector(false, "flux_point");
        GameRegistry.registerBlock(plug, ItemBlockFluxConnector.class, "flux_plug");
        GameRegistry.registerBlock(point, ItemBlockFluxConnector.class, "flux_point");
        GameRegistry.registerTileEntity(TileEntityFluxPlug.class, ModList.GTNeverReset.getResourcePath("flux_plug"));
        GameRegistry.registerTileEntity(TileEntityFluxPoint.class, ModList.GTNeverReset.getResourcePath("flux_point"));
        GTNRItemList.FluxPlug.set(new ItemStack(plug));
        GTNRItemList.FluxPoint.set(new ItemStack(point));
        FluxTransferScheduler scheduler = new FluxTransferScheduler();
        FMLCommonHandler.instance()
            .bus()
            .register(scheduler);
        MinecraftForge.EVENT_BUS.register(scheduler);
        ForgeChunkManager.setForcedChunkLoadingCallback(GTNeverReset.INSTANCE, new FluxChunkLoading());
    }

    public static final class Card implements IContentLoader {

        @Override
        public String name() {
            return "block";
        }

        @Override
        public void preInit(cpw.mods.fml.common.event.FMLPreInitializationEvent event) {
            BlockLoader.registerBlocks();
        }
    }
}
