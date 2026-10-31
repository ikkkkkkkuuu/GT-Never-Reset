package com.xyp.gtnrlib.mixins;

import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

public class GTNRLibEarlyMixins implements IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        int v = Runtime.version()
            .feature();
        if (v >= 21) return "mixins.gtnrlib.early.j21.json";
        if (v >= 17) return "mixins.gtnrlib.early.j17.json";
        return "mixins.gtnrlib.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(GTNRLibMixins.class, loadedCoreMods);
    }

}
