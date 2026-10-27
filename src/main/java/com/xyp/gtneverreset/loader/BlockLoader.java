package com.xyp.gtneverreset.loader;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.block.blocks.BlockEddingtonCasing;
import com.xyp.gtneverreset.common.block.blocks.BlockFluxConnector;
import com.xyp.gtneverreset.common.block.blocks.BlockStockIOInterface;
import com.xyp.gtneverreset.common.block.blocks.item.ItemBlockFluxConnector;
import com.xyp.gtneverreset.common.block.blocks.item.ItemBlockStockIOInterface;
import com.xyp.gtneverreset.common.block.blocks.tile.TileFluxPlug;
import com.xyp.gtneverreset.common.block.blocks.tile.TileFluxPoint;
import com.xyp.gtneverreset.common.block.blocks.tile.TileStockIOInterface;
import com.xyp.gtneverreset.common.world.FluxChunkLoading;
import com.xyp.gtneverreset.utils.enums.BlockIcons;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.machine.flux.FluxTransferScheduler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;

public final class BlockLoader {

    public static BlockEddingtonCasing eddingtonWallCasing;

    private BlockLoader() {}

    public static void registerBlocks() {
        BlockIcons.registerIcons();
        eddingtonWallCasing = new BlockEddingtonCasing();
        GameRegistry.registerBlock(eddingtonWallCasing, BlockEddingtonCasing.REGISTRY_NAME);
        GTNRItemList.EddingtonWallCasing.set(new ItemStack(eddingtonWallCasing));
        BlockStockIOInterface stockInterface = new BlockStockIOInterface();
        GameRegistry
            .registerBlock(stockInterface, ItemBlockStockIOInterface.class, BlockStockIOInterface.REGISTRY_NAME);
        GameRegistry.registerTileEntity(
            TileStockIOInterface.class,
            ModList.GTNeverReset.getResourcePath(BlockStockIOInterface.REGISTRY_NAME));
        GTNRItemList.StockIOInterface.set(new ItemStack(stockInterface));
        stockInterface.setCreativeTab(GTNRCreativeTabs.BLOCKS);
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
        GameRegistry.registerTileEntity(TileFluxPlug.class, ModList.GTNeverReset.getResourcePath("flux_plug"));
        GameRegistry.registerTileEntity(TileFluxPoint.class, ModList.GTNeverReset.getResourcePath("flux_point"));
        GTNRItemList.FluxPlug.set(new ItemStack(plug));
        GTNRItemList.FluxPoint.set(new ItemStack(point));
        FluxTransferScheduler scheduler = new FluxTransferScheduler();
        FMLCommonHandler.instance()
            .bus()
            .register(scheduler);
        MinecraftForge.EVENT_BUS.register(scheduler);
        ForgeChunkManager.setForcedChunkLoadingCallback(GTNeverReset.INSTANCE, new FluxChunkLoading());
    }
}
