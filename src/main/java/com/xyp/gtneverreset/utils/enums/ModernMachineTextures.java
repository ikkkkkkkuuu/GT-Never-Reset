package com.xyp.gtneverreset.utils.enums;

import com.xyp.gtneverreset.GTNeverReset;

import gregtech.api.enums.Dyes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

public enum ModernMachineTextures {

    MIXER("mixer", true, true, false),
    ELECTROLYZER("electrolyzer", true, false, true),
    CHEMICAL_REACTOR("chemical_reactor", false, false, true);

    private static final ITexture[][] CASINGS = createCasings();

    private final ITexture[] overlays;

    ModernMachineTextures(String machineName, boolean hasSide, boolean hasTop, boolean emissive) {
        overlays = new ITexture[] { hasSide ? overlay(machineName, "side_active", emissive) : null,
            hasSide ? overlay(machineName, "side", emissive) : null, overlay(machineName, "front_active", emissive),
            overlay(machineName, "front", emissive), hasTop ? overlay(machineName, "top_active", false) : null,
            hasTop ? overlay(machineName, "top", false) : null, null, null };
    }

    /** The eight machine overlays in the order used by MTEBasicMachine; null faces retain its casing. */
    public ITexture[] getOverlays() {
        return overlays.clone();
    }

    /** GTM tier casings for LV through UV, with GregTech's color index (-1 for unpainted). */
    public static ITexture getCasing(int tier, int colorIndex) {
        return CASINGS[tier - 1][colorIndex + 1];
    }

    private static ITexture[][] createCasings() {
        String[] tiers = { "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv" };
        ITexture[][] casings = new ITexture[tiers.length][Dyes.VALUES.length + 1];
        for (int tier = 0; tier < tiers.length; tier++) {
            String path = "basicmachines/modern_casings/" + tiers[tier] + "/";
            IIconContainer bottom = Textures.BlockIcons.custom(GTNeverReset.MODID, path + "bottom");
            IIconContainer top = Textures.BlockIcons.custom(GTNeverReset.MODID, path + "top");
            IIconContainer side = Textures.BlockIcons.custom(GTNeverReset.MODID, path + "side");
            for (int color = -1; color < Dyes.VALUES.length; color++) {
                casings[tier][color + 1] = TextureFactory.of(bottom, top, side, Dyes.getModulation(color));
            }
        }
        return casings;
    }

    private static ITexture overlay(String machineName, String face, boolean emissive) {
        String path = "basicmachines/modern_" + machineName + "/overlay_" + face;
        ITexture texture = TextureFactory.of(Textures.BlockIcons.custom(GTNeverReset.MODID, path));
        if (!emissive) return texture;
        return TextureFactory.of(
            texture,
            TextureFactory.builder()
                .addIcon(Textures.BlockIcons.custom(GTNeverReset.MODID, path + "_emissive"))
                .glow()
                .build());
    }
}
