package com.xyp.gtneverreset.mixins.late.appliedenergistics2.compact;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;

import appeng.util.inv.MEInventoryCrafting;

@Mixin(value = MEInventoryCrafting.class, remap = false)
public class MixinInventoryCrafting implements LargeInventoryCrafting {

    @Unique
    private long gtnr$assembler = 1L;

    @Override
    public void gtnr$setAssemblerSize(long value) {
        gtnr$assembler = value;
    }

    @Override
    public long gtnr$getAssemblerSize() {
        return gtnr$assembler;
    }
}
