package com.xyp.gtneverreset.loader;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.cropsnh.api.CropsNHCrops;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.farming.mutation.CropMutation;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.compat.cropsnh.ExplosiveFlowerCrop;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.common.Optional;
import gregtech.api.enums.Mods;

public final class CropLoader implements IContentLoader {

    @Override
    public void init() {
        if (ModList.CropsNH.isModLoaded() && Mods.IndustrialCraft2.isModLoaded()) registerCrops();
    }

    @Optional.Method(modid = ModList.ModIds.CROPSNH)
    private static void registerCrops() {
        ItemStack industrialTnt = ModsItemlist.IndustrialTnt.get(1);
        if (industrialTnt == null) {
            GTNeverReset.LOG.warn("skipping explosive flower because industrial TNT is unavailable");
            return;
        }
        CropRegistry.instance.register(new ExplosiveFlowerCrop(industrialTnt));
    }

    @Optional.Method(modid = ModList.ModIds.CROPSNH)
    public static void registerMutations() {
        ICropCard crop = CropRegistry.instance.get(ExplosiveFlowerCrop.ID);
        if (crop == null) return;
        new CropMutation(crop, CropsNHCrops.Creeperweed, CropsNHCrops.Blazereed).register();
    }
}
