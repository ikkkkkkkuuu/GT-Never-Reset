package com.xyp.gtnrlib.mixins;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

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
        return IMixins.getLateMixins(GTNRLibMixins.class, loadedMods);
    }
}
