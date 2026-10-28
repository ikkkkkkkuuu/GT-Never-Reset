package com.xyp.gtneverreset.mixins.late.appliedenergistics2.wireless;

import org.spongepowered.asm.mixin.Mixin;

import appeng.tile.networking.TileController;

@Mixin(value = TileController.class, remap = false)
public interface AccessorTileController {
}
