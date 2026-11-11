package com.xyp.gtneverreset.common.machine.multiblock.crop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.api.ISoilList;
import com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement;
import com.gtnewhorizon.cropsnh.utility.CropsNHUtils;
import com.xyp.gtneverreset.config.CropBreederConfig;

public final class CropBreedingSubstrate {

    private final SubSoilRequirement extraSoil;
    private final ISoilList plantingSoil;

    private CropBreedingSubstrate(SubSoilRequirement extraSoil, ISoilList plantingSoil) {
        this.extraSoil = extraSoil;
        this.plantingSoil = plantingSoil;
    }

    public static List<CropBreedingSubstrate> forCrop(ICropCard crop) {
        return from(crop, crop.getGrowthRequirements());
    }

    public static List<CropBreedingSubstrate> forMutation(ICropMutation mutation) {
        return from(mutation.getOutput(), mutation.getRequirements());
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return false;
        return extraSoil != null ? extraSoil.isValidSubSoil(stack)
            : plantingSoil.isRegistered(CropsNHUtils.getBlockFromItem(stack), CropsNHUtils.getItemMeta(stack));
    }

    public List<ItemStack> alternatives() {
        return extraSoil != null ? extraSoil.getItemsForNEI()
            : plantingSoil.getNEIItemList()
                .collect(Collectors.toList());
    }

    public boolean isAutomaticallySatisfied() {
        for (ItemStack stack : alternatives()) {
            if (!matches(stack)) continue;
            Block block = CropsNHUtils.getBlockFromItem(stack);
            if (block == null || block == Blocks.air) continue;
            String name = Block.blockRegistry.getNameForObject(block);
            if (name != null && name.startsWith("minecraft:")) return true;
        }
        return false;
    }

    private static List<CropBreedingSubstrate> from(ICropCard crop, Iterable<?> requirements) {
        if (!CropBreederConfig.requireSubstrates) return Collections.emptyList();
        List<CropBreedingSubstrate> result = new ArrayList<>();
        for (Object requirement : requirements) {
            if (requirement instanceof SubSoilRequirement soil) result.add(new CropBreedingSubstrate(soil, null));
        }
        if (result.isEmpty()) result.add(new CropBreedingSubstrate(null, crop.getSoilTypes()));
        return result;
    }
}
