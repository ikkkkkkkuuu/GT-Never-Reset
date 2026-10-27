package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceTerminalEntry", remap = false)
public interface InterfaceEntryViewportAccessor {

    @Accessor("rowSize")
    int getRowSize();
}
