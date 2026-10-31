package com.xyp.gtnrlib.client.render.icon;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class FileIconCache {

    private final Map<String, ResourceLocation> textures = new HashMap<>();

    private final Map<String, Boolean> failed = new HashMap<>();

    private final Supplier<File> directory;
    private final String texturePrefix;
    private final Logger logger;

    public FileIconCache(Supplier<File> directory, String texturePrefix, Logger logger) {
        this.directory = directory;
        this.texturePrefix = texturePrefix;
        this.logger = logger;
    }

    public ResourceLocation texture(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        ResourceLocation cached = textures.get(fileName);
        if (cached != null) {
            return cached;
        }
        if (failed.containsKey(fileName)) {
            return null;
        }

        File file = new File(directory.get(), fileName);
        if (!file.isFile()) {
            failed.put(fileName, Boolean.TRUE);
            logger.warn("Icon file not found: " + file.getAbsolutePath());
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IllegalArgumentException("not a readable image");
            }
            ResourceLocation location = Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation(texturePrefix, new DynamicTexture(image));
            textures.put(fileName, location);
            return location;
        } catch (Exception e) {
            failed.put(fileName, Boolean.TRUE);
            logger.error("Could not load icon " + fileName, e);
            return null;
        }
    }

    public List<String> listFiles() {
        List<String> names = new ArrayList<>();
        File[] files = directory.get()
            .listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName()
                    .toLowerCase(Locale.ROOT)
                    .endsWith(".png")) {
                    names.add(file.getName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    public void refresh() {
        for (ResourceLocation texture : textures.values()) {
            Minecraft.getMinecraft()
                .getTextureManager()
                .deleteTexture(texture);
        }
        textures.clear();
        failed.clear();
    }
}
