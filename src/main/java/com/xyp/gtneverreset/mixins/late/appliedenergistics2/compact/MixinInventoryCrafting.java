package com.xyp.gtneverreset.mixins.late.appliedenergistics2.compact;

import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;

import appeng.util.inv.MEInventoryCrafting;

/** Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance. */
@Mixin(value = MEInventoryCrafting.class, remap = false)
public class MixinInventoryCrafting implements LargeInventoryCrafting {

    @Unique
    private long gtnr$assembler = 1L;

    @Intrinsic
    public void setAssemblerSize(long value) {
        gtnr$assembler = value;
    }

    @Intrinsic
    public long getAssemblerSize() {
        return gtnr$assembler;
    }
}
