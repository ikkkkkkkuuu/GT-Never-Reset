package com.xyp.gtneverreset.compat.bloodmagic;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

import com.xyp.gtneverreset.api.packaged.PackagedCoreRegistry;
import com.xyp.gtneverreset.client.render.PackagedCoreRenderer;
import com.xyp.gtneverreset.common.item.items.ItemPackagedCore;
import com.xyp.gtneverreset.loader.ItemLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import WayofTime.alchemicalWizardry.ModBlocks;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class BloodMagicCompat {

    public static void registerPackagedCores() {

        // #tr item.blood_altar_packaged_core.name
        // # Blood Altar Packaged Core
        // # zh_CN 血魔法祭坛封包核心
        ItemLoader.bloodAltarCore = new ItemPackagedCore(
            "blood_altar",
            "blood_altar_packaged_core",
            GTNRItemList.BloodAltarCore);
        PackagedCoreRegistry.register("blood_altar", new BloodAltarAdapter());
    }

    @SideOnly(Side.CLIENT)
    public static void registerRenderers() {

        MinecraftForgeClient.registerItemRenderer(
            GTNRItemList.BloodAltarCore.getItem(),
            new PackagedCoreRenderer(() -> new ItemStack(ModBlocks.blockAltar)));
    }
}
