package com.xyp.gtneverreset.client.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.structure.VaultCasingConnections;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class VaultConnectedIcons {

    private static final Map<String, IIcon[]> icons = new HashMap<>();

    private VaultConnectedIcons() {}

    public static void register(IIconRegister register, String texture) {
        IIcon[] variants = new IIcon[256];
        for (int mask = 0; mask < variants.length; mask++) {
            if (VaultCasingConnections.normalizeMask(mask) == mask) {
                String path = texture.equals("sl_eddington") ? "eddington_" + mask : texture + "/" + mask;
                variants[mask] = register.registerIcon(ModList.GTNeverReset.getResourcePath("stellar/ctm/" + path));
            }
        }
        for (int mask = 0; mask < variants.length; mask++) {
            variants[mask] = variants[VaultCasingConnections.normalizeMask(mask)];
        }
        icons.put(texture, variants);
    }

    public static IIcon getIcon(String texture, int mask) {
        IIcon[] variants = icons.get(texture);
        return variants == null ? null : variants[mask & 255];
    }
}
