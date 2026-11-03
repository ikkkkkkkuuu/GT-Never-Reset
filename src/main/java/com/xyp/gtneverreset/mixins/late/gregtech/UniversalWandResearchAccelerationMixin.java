package com.xyp.gtneverreset.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;

import com.xyp.gtneverreset.compat.gregtech.WandResearchAcceleration;

import tectech.thing.metaTileEntity.multi.MTEResearchStation;
import tectech.thing.metaTileEntity.multi.base.TTMultiblockBase;

@Mixin(value = MTEResearchStation.class, remap = false)
public abstract class UniversalWandResearchAccelerationMixin extends TTMultiblockBase
    implements WandResearchAcceleration {

    protected UniversalWandResearchAccelerationMixin(int id, String name, String regional) {
        super(id, name, regional);
    }

    @Override
    public void gtneverreset$accelerateResearch(int extra) {
        MTEResearchStation station = (MTEResearchStation) (Object) this;
        if (eAvailableData <= 0 || station.computationRemaining <= 0) return;
        long reduction = eAvailableData > Long.MAX_VALUE / extra ? Long.MAX_VALUE : eAvailableData * extra;
        station.computationRemaining = Math.max(0, station.computationRemaining - reduction);
    }
}
