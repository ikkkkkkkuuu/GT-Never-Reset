package com.xyp.gtneverreset.common.machine.hatch;

import java.util.List;
import java.util.Objects;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@IMetaTileEntity.SkipGenerateDescription
public final class CrossRecipeMEPatternMirror extends SuperMTEHatchCraftingInputSlave {

    public CrossRecipeMEPatternMirror(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private CrossRecipeMEPatternMirror(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new CrossRecipeMEPatternMirror(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    protected boolean acceptsMaster(SuperMTEHatchCraftingInputME master) {
        return master instanceof CrossRecipeMEPatternBuffer;
    }

    public List<String> getControllerRecipeMaps() {
        return getConnectedControllers().stream()
            .flatMap(
                controller -> controller.getAvailableRecipeMaps()
                    .stream())
            .filter(Objects::nonNull)
            .map(map -> map.unlocalizedName)
            .distinct()
            .toList();
    }

    List<MTEMultiBlockBase> getConnectedControllers() {
        return watchers.stream()
            .filter(
                watcher -> watcher instanceof MTEMultiBlockBase controller && controller.isValid()
                    && controller.mDualInputHatches.contains(this))
            .map(watcher -> (MTEMultiBlockBase) watcher)
            .toList();
    }

    @Override
    public String[] getDescription() {
        String[] lines = super.getDescription();
        // #tr gtnr.tooltip.cross_recipe_mirror.proxy
        // # Only links to a Super Cross-Recipe Pattern Input Buffer (ME); shares its patterns and buffers
        // # zh_CN 仅可连接超级跨配方类型样板输入总成 (ME)，共用其样板和缓存
        lines[0] = StatCollector.translateToLocal("gtnr.tooltip.cross_recipe_mirror.proxy");
        return lines;
    }
}
