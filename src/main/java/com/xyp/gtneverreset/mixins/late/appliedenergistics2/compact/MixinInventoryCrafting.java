// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtneverreset.mixins.late.appliedenergistics2.compact;

import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.xyp.gtneverreset.utils.machine.LargeInventoryCrafting;

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
