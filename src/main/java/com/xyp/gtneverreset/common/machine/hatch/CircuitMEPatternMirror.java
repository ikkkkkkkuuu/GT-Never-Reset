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
        return master instanceof CircuitMEPatternBuffer;
    }

    @Override
    public String[] getDescription() {
        String[] lines = super.getDescription();
        // #tr gtnr.tooltip.circuit_mirror.proxy
        // # Shares a Circuit Pattern Buffer (ME) with other multiblocks; only links to this hatch type
        // # zh_CN 让其他多方块机器共用电路样板总成 (ME)，仅可连接此类主仓
        lines[0] = StatCollector.translateToLocal("gtnr.tooltip.circuit_mirror.proxy");
        return lines;
    }
}
