package com.xyp.gtneverreset.common.machine.multiblock.crop;

import com.gtnewhorizon.cropsnh.api.ICropCard;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.VoltageIndex;

public final class CropBreedingRecipeRules {

    private CropBreedingRecipeRules() {}

    public static int breedingGrade(ICropCard crop) {
        return Math.max(1, crop.getMachineBreedingRecipeTier());
    }

    public static int requiredVoltageTier(ICropCard crop) {
        return Math.min(GTValues.V.length - 1, VoltageIndex.HV + breedingGrade(crop) - 1);
    }

    public static long requiredVoltage(ICropCard crop) {
        return GTValues.V[requiredVoltageTier(crop)];
    }

    public static int baseDuration(ICropCard crop) {
        return baseDuration(breedingGrade(crop));
    }

    public static int baseDuration(int grade) {
        return (int) Math.min(Integer.MAX_VALUE, 160L << Math.max(0, Math.min(30, grade - 1)));
    }
}
