package com.xyp.gtneverreset.client.radialmenu.render;

import static com.xyp.gtneverreset.client.radialmenu.render.IconRenderer.ATLAS;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import com.xyp.gtneverreset.GTNeverReset;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class IconResourceReloadHandler implements IResourceManagerReloadListener {

    public static void register() {
        IResourceManager manager = Minecraft.getMinecraft()
            .getResourceManager();

        if (manager instanceof SimpleReloadableResourceManager) {
            ((SimpleReloadableResourceManager) manager).registerReloadListener(new IconResourceReloadHandler());
        } else {
            GTNeverReset.LOG.warn("Resource manager is not reloadable; icon caches will not refresh on pack changes");
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        ATLAS.invalidate();
        IconRenderer.clearCache();
    }
}
