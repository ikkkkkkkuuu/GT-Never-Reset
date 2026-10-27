package com.xyp.gtneverreset.utils.enums;

import com.xyp.gtneverreset.GTNeverReset;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;

public final class BlockIcons {

    public static IIconContainer OVERLAY_FRONT_ITEMVAULTPORTHATCH;
    public static IIconContainer OverlayFrontKerrCompressor;
    public static IIconContainer OverlayFrontKerrCompressorActive;
    public static IIconContainer OverlayFrontKerrCompressorGlow;
    public static IIconContainer OverlayFrontKerrCompressorActiveGlow;

    private BlockIcons() {}

    public static void registerIcons() {
        if (OVERLAY_FRONT_ITEMVAULTPORTHATCH != null) return;
        OVERLAY_FRONT_ITEMVAULTPORTHATCH = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "iconsets/OVERLAY_FRONT_ITEMVAULTPORTHATCH");
        OverlayFrontKerrCompressor = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR");
        OverlayFrontKerrCompressorActive = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_ACTIVE");
        OverlayFrontKerrCompressorGlow = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_GLOW");
        OverlayFrontKerrCompressorActiveGlow = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_ACTIVE_GLOW");
    }
}
