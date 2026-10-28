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
    public static IIconContainer FragmentCollectorFront;
    public static IIconContainer FragmentCollectorFrontActive;
    public static IIconContainer FragmentCollectorSide;
    public static IIconContainer FragmentCollectorTop;
    public static IIconContainer FragmentCollectorBottom;
    public static IIconContainer FragmentCollectorBack;

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
        FragmentCollectorFront = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "basicmachines/fragment_collector/overlay_front");
        FragmentCollectorFrontActive = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "basicmachines/fragment_collector/overlay_front_active");
        FragmentCollectorSide = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "basicmachines/fragment_collector/overlay_side");
        FragmentCollectorTop = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "basicmachines/fragment_collector/overlay_top");
        FragmentCollectorBottom = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "basicmachines/fragment_collector/overlay_bottom");
        FragmentCollectorBack = Textures.BlockIcons
            .custom(GTNeverReset.MODID, "basicmachines/fragment_collector/overlay_back");
    }
}
