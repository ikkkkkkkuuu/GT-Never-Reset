package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.ItemRepo;

@Mixin(value = GuiMEMonitorable.class, remap = false)
public interface AccessorGuiMEMonitorable {

    @Accessor("repo")
    ItemRepo gtnr$getItemRepo();
}
