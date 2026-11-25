package com.xyp.gtneverreset.mixins.late.gregtech;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.common.tileentities.machines.IHatchWatcher;

@Mixin(value = MTEHatch.class, remap = false)
public interface CraftingMonitorHatchAccessor {

    @Accessor("watchers")
    List<IHatchWatcher> gtnr$getWatchers();
}
