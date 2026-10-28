package com.xyp.gtneverreset.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("com.xyp.gtneverreset.loader.EarlyMixinsLoader")
public final class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtneverreset.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        List<String> mixins = new ArrayList<>(
            List.of(
                "wireless.MixinWirelessTileData",
                "gtnhlib.ServerThreadUtilMixin",
                "minecraft.VanillaFurnaceMixin"));
        if (FMLLaunchHandler.side()
            .isClient()) {
            mixins.add("texteffect.MixinFontRendererTextEffects");
            if (loadedCoreMods.contains("com.gtnewhorizons.angelica.loading.AngelicaTweaker")) {
                mixins.add("texteffect.angelica.MixinFontBatchTextEffects");
                // Early loading has coremod names, not Forge's completed mod list.
                boolean gtnlFontBatchPatch = loadedCoreMods.contains("com.science.gtnl.asm.GTNLEarlyCoreMod")
                    && Launch.classLoader.getResource(
                        "com/science/gtnl/mixins/early/texteffect/angelica/MixinFontBatchTextEffects.class") != null;
                mixins.add(
                    gtnlFontBatchPatch ? "texteffect.gtnl.MixinTextEffectCapture"
                        : "texteffect.angelica.MixinFontBatchAntialiasing");
            }
        }
        return mixins;
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
