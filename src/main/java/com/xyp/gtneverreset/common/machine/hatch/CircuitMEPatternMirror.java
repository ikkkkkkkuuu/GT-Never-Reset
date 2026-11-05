package com.xyp.gtneverreset.common.machine.hatch;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

@IMetaTileEntity.SkipGenerateDescription
public class CircuitMEPatternMirror extends SuperMTEHatchCraftingInputSlave {

    public CircuitMEPatternMirror(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private CircuitMEPatternMirror(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new CircuitMEPatternMirror(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    protected boolean acceptsMaster(SuperMTEHatchCraftingInputME master) {
        return master instanceof CircuitMEPatternBuffer && !(master instanceof CrossRecipeMEPatternBuffer);
    }

    @Override
    public String[] getDescription() {
        String[] lines = super.getDescription();
        // #tr gtnr.tooltip.circuit_mirror.proxy
        // # Only links to Circuit Pattern Input Buffers (ME); cannot link to ordinary or cross-recipe assemblies
        // # zh_CN 仅可连接电路样板输入总成 (ME)，不可连接普通或跨配方类型总成
        lines[0] = StatCollector.translateToLocal("gtnr.tooltip.circuit_mirror.proxy");
        return lines;
    }
}
