package com.xyp.gtneverreset.common.machine.multiblock.crop;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.xyp.gtneverreset.config.CropBreederConfig;

public final class CropBreedingChance {

    private static final int[] DEFAULT_TIER_CHANCES = { 100, 80, 60, 40, 20, 10 };

    private CropBreedingChance() {}

    public static int percent(ICropCard crop) {
        if (crop == null) return 0;
        int[] chances = CropBreederConfig.tierChances;
        if (chances == null || chances.length == 0) chances = DEFAULT_TIER_CHANCES;
        int band = Math.min(chances.length - 1, Math.max(0, crop.getTier() - 1) / 3);
        int chance = Math.max(0, Math.min(100, chances[band]));
        if (CropBreederConfig.cropOverrides == null) return chance;
        for (String entry : CropBreederConfig.cropOverrides) {
            if (entry == null) continue;
            int separator = entry.lastIndexOf('=');
            if (separator < 0 || !entry.substring(0, separator)
                .trim()
                .equals(crop.getId())) continue;
            try {
                int override = Integer.parseInt(
                    entry.substring(separator + 1)
                        .trim());
                if (override >= 0 && override <= 100) chance = override;
            } catch (NumberFormatException ignored) {}
        }
        return chance;
    }

}
