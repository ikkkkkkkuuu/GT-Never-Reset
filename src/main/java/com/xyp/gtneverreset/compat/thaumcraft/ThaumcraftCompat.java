package com.xyp.gtneverreset.compat.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

import com.xyp.gtneverreset.api.packaged.PackagedCoreRegistry;
import com.xyp.gtneverreset.client.render.PackagedCoreRenderer;
import com.xyp.gtneverreset.common.item.items.ItemPackagedCore;
import com.xyp.gtneverreset.loader.ItemLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.common.config.ConfigBlocks;

public final class ThaumcraftCompat {

    public static void registerPackagedCores() {

        // #tr item.tc4_crucible_packaged_core.name
        // # Thaumcraft Crucible Packaged Core
        // # zh_CN 神秘时代坩埚封包核心
        ItemLoader.thaumcraftCrucibleCore = new ItemPackagedCore(
            "thaumcraft_crucible",
            "tc4_crucible_packaged_core",
            GTNRItemList.ThaumcraftCrucibleCore);
        // #tr item.arcane_workbench_packaged_core.name
        // # Arcane Workbench Packaged Core
        // # zh_CN 奥术工作台封包核心
        ItemLoader.arcaneWorkbenchCore = new ItemPackagedCore(
            "arcane_workbench",
            "arcane_workbench_packaged_core",
            GTNRItemList.ArcaneWorkbenchCore);
        ItemLoader.thaumcraftInfusionCore = new ItemPackagedCore(true);

        PackagedCoreRegistry.register("thaumcraft_crucible", new ThaumcraftCrucibleAdapter());
        PackagedCoreRegistry.register("arcane_workbench", new ArcaneWorkbenchAdapter());
        PackagedCoreRegistry.register("thaumcraft_infusion", new ThaumcraftInfusionAdapter());
    }

    @SideOnly(Side.CLIENT)
    public static void registerRenderers() {

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
}
