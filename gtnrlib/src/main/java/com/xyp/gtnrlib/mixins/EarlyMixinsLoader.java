package com.xyp.gtnrlib.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("com.xyp.gtnrlib.mixins.EarlyMixinsLoader")
public final class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return new GTNRLibEarlyMixins().getMixinConfig();
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        List<String> mixins = new ArrayList<>(new GTNRLibEarlyMixins().getMixins(loadedCoreMods));
        if (!FMLLaunchHandler.side()
            .isClient()) return mixins;
        if (loadedCoreMods.contains("com.gtnewhorizons.angelica.loading.AngelicaTweaker")) {
            boolean library = loadedCoreMods.contains("com.gtnllib.mixins.GTNLLibEarlyCoreMod")
                && present("com/gtnllib/mixins/early/texteffect/angelica/MixinFontBatchTextEffects.class");
            boolean legacy = loadedCoreMods.contains("com.science.gtnl.asm.GTNLEarlyCoreMod")
                && present("com/science/gtnl/mixins/early/texteffect/angelica/MixinFontBatchTextEffects.class");
            if (library) mixins.add("texteffect.gtnl.MixinLibraryTextEffectCapture");
            if (legacy) mixins.add("texteffect.gtnl.MixinTextEffectCapture");
            if (!library && !legacy) mixins.add("texteffect.angelica.MixinFontBatchAntialiasing");
        }
        return mixins;
    }

    private static boolean present(String path) {
        return Launch.classLoader.getResource(path) != null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
