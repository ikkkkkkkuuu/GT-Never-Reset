package com.xyp.gtnrlib.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.xyp.gtnrlib.compat.animation.ToolAnimationCompat;
import com.xyp.gtnrlib.mod.ModList;

@LateMixin
public class GTNRLibLateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        int v = Runtime.version()
            .feature();
        if (v >= 21) return "mixins.gtnrlib.late.j21.json";
        if (v >= 17) return "mixins.gtnrlib.late.j17.json";
        return "mixins.gtnrlib.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>(IMixins.getLateMixins(GTNRLibMixins.class, loadedMods));
        if (loadedMods.contains(ModList.ModIds.GREGTECH)) {
            if (!ToolAnimationCompat.legacyClass("mixins/late/gregtech/ToolMachineUseMixin"))
                mixins.add("gregtech.ToolMachineUseMixin");
            if (!ToolAnimationCompat.legacyClass("mixins/late/gregtech/WrenchToolUseMixin"))
                mixins.add("gregtech.WrenchToolUseMixin");
            mixins.add("gregtech.ScrewdriverMachineUseMixin");
            mixins.add("gregtech.ScrewdriverToolUseMixin");
        }
        return mixins;
    }
}
