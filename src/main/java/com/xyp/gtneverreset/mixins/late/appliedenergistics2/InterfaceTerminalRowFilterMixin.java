package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal.InterfacePatternRows;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal.InterfaceRowFilter;

import appeng.client.gui.implementations.GuiInterfaceTerminal;

@Mixin(value = GuiInterfaceTerminal.class, remap = false)
public abstract class InterfaceTerminalRowFilterMixin {

    @WrapOperation(
        method = { "drawSection", "drawEntry" },
        at = @At(
            value = "FIELD",
            target = "Lappeng/client/gui/implementations/GuiInterfaceTerminal$InterfaceTerminalEntry;rows:I"),
        require = 3)
    private int visibleRows(@Coerce Object entry, Operation<Integer> original) {
        if (!((Object) this instanceof InterfaceRowFilter)) return original.call(entry);
        return ((InterfacePatternRows) entry).gtneverreset$getVisibleRowCount();
    }

    @ModifyVariable(method = "drawEntry", at = @At("STORE"), name = "slotIdx", require = 2)
    private int actualSlot(int slot, @Coerce Object entry, int viewY, int titleBottom, int mouseX, int mouseY) {
        if (!((Object) this instanceof InterfaceRowFilter)) return slot;
        int actual = ((InterfacePatternRows) entry).gtneverreset$getActualSlot(slot);
        return actual < 0 ? Integer.MAX_VALUE : actual;
    }
}
