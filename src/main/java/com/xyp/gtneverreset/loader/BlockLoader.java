package com.xyp.gtneverreset.loader;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.block.blocks.BlockChaosCapacity;
import com.xyp.gtneverreset.common.block.blocks.BlockChaosCasing;
import com.xyp.gtneverreset.common.block.blocks.BlockEddingtonCasing;
import com.xyp.gtneverreset.common.block.blocks.BlockFluxConnector;
import com.xyp.gtneverreset.common.block.blocks.BlockOverloadedProvider;
import com.xyp.gtneverreset.common.block.blocks.BlockPackagedProvider;
import com.xyp.gtneverreset.common.block.blocks.BlockStockIOInterface;
import com.xyp.gtneverreset.common.block.blocks.item.ItemBlockChaosCasing;
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

    public static BlockChaosCasing chaosCasing01;
    public static BlockChaosCasing chaosCasing02;
    public static BlockChaosCasing chaosGlass;
    public static BlockChaosCasing chaosPhotonUpgrade;
    public static BlockChaosCasing chaosEnergyMatrix;
    public static BlockChaosCasing chaosFieldGenerator;
    public static BlockChaosCapacity chaosCapacity;
    public static BlockChaosCasing chaosMultiUseCore;

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
        registerChaosBlocks();
    }

    private static void registerChaosBlocks() {
        int page = gregtech.api.enums.Textures.BlockIcons.casingTexturePages.length - 1;
        while (page > 0 && gregtech.api.enums.Textures.BlockIcons.casingTexturePages[page] != null) page--;
        if (page == 0) throw new IllegalStateException("No casing texture page available for Bottle in Chaos");
        gregtech.api.util.GTUtility.addTexturePage((byte) page);
        int base = page << 7;
        chaosCasing01 = new BlockChaosCasing("chaos_casing_01", "MetaBlockCasing01", base + 0, false, 1, 2);
        GameRegistry.registerBlock(chaosCasing01, ItemBlockChaosCasing.class, "chaos_casing_01");
        chaosCasing02 = new BlockChaosCasing("chaos_casing_02", "MetaBlockCasing02", base + 16, false, 2, 4, 7, 8);
        GameRegistry.registerBlock(chaosCasing02, ItemBlockChaosCasing.class, "chaos_casing_02");
        chaosGlass = new BlockChaosCasing("chaos_glass", "MetaBlockGlass01", base + 32, true, 0);
        GameRegistry.registerBlock(chaosGlass, ItemBlockChaosCasing.class, "chaos_glass");
        chaosPhotonUpgrade = new BlockChaosCasing(
            "chaos_photon_upgrade",
            "PhotonControllerUpgrades",
            base + 48,
            false,
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13);
        GameRegistry.registerBlock(chaosPhotonUpgrade, ItemBlockChaosCasing.class, "chaos_photon_upgrade");
        chaosEnergyMatrix = new BlockChaosCasing(
            "chaos_energy_matrix",
            "EnergySustainmentMatrix",
            base + 64,
            false,
            0,
            1,
            2,
            3,
            4);
        GameRegistry.registerBlock(chaosEnergyMatrix, ItemBlockChaosCasing.class, "chaos_energy_matrix");
        chaosFieldGenerator = new BlockChaosCasing(
            "chaos_field_generator",
            "StabilisationFieldGenerator",
            base + 80,
            false,
            0,
            1,
            2,
            3,
            4);
        GameRegistry.registerBlock(chaosFieldGenerator, ItemBlockChaosCasing.class, "chaos_field_generator");
        chaosMultiUseCore = new BlockChaosCasing("chaos_multi_use_core", "MultiUseCore", base + 96, false, 5, 6, 7, 8);
        GameRegistry.registerBlock(chaosMultiUseCore, ItemBlockChaosCasing.class, "chaos_multi_use_core");
        chaosCapacity = new BlockChaosCapacity();
        GameRegistry.registerBlock(chaosCapacity, ItemBlockChaosCasing.class, "chaos_capacity");
        chaosCapacity.registerStructureChannel();
        GTNRItemList.ChaosCapacityTier1.set(new ItemStack(chaosCapacity, 1, 0));
        GTNRItemList.ChaosCapacityTier2.set(new ItemStack(chaosCapacity, 1, 1));
        GTNRItemList.ChaosCapacityTier3.set(new ItemStack(chaosCapacity, 1, 2));
        GTNRItemList.ChaosCapacityTier4.set(new ItemStack(chaosCapacity, 1, 3));
        GTNRItemList.ChaosCapacityTier5.set(new ItemStack(chaosCapacity, 1, 4));

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
        public void preInit(cpw.mods.fml.common.event.FMLPreInitializationEvent event) {
            BlockLoader.registerBlocks();
        }
    }
}
