package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraftforge.client.event.DrawBlockHighlightEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import gregtech.client.BlockOverlayRenderer;

/** Reuses the fixed GT renderer for the wand's per-stack tool mode without reflection. */
@Mixin(value = BlockOverlayRenderer.class, remap = false)
public interface BlockOverlayRendererInvoker {

    @Invoker("drawGrid")
    static void gtneverreset$drawGrid(DrawBlockHighlightEvent event, boolean showCoverConnections, boolean wrench,
        boolean sneaking) {
        throw new AssertionError();
    }
}
