package com.xyp.gtnrlib.client.render.icon;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SpriteAtlas {

    public final ResourceLocation texture;
    private final ResourceLocation catalogue;
    private final String namespace;
    private final Logger logger;

    private final Map<String, Sprite> byName = new HashMap<>();
    private final List<Sprite> ordered = new ArrayList<>();

    private int atlasWidth = 2048;
    private int atlasHeight = 1024;
    private int cell = 32;
    private boolean loaded;

    public SpriteAtlas(ResourceLocation texture, ResourceLocation catalogue, String namespace, Logger logger) {
        this.texture = texture;
        this.catalogue = catalogue;
        this.namespace = namespace;
        this.logger = logger;
    }

    public void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Reader reader = null;
        try {
            reader = new InputStreamReader(
                Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(catalogue)
                    .getInputStream(),
                Charset.forName("UTF-8"));

            Catalogue catalogue = new Gson().fromJson(reader, Catalogue.class);
            if (catalogue == null || catalogue.icons == null) {
                logger.error("Sprite catalogue is empty");
                return;
            }

            atlasWidth = catalogue.atlasWidth > 0 ? catalogue.atlasWidth : atlasWidth;
            atlasHeight = catalogue.atlasHeight > 0 ? catalogue.atlasHeight : atlasHeight;
            cell = catalogue.cell > 0 ? catalogue.cell : cell;

            for (Sprite sprite : catalogue.icons) {
                if (sprite != null && sprite.name != null) {
                    byName.put(sprite.name, sprite);
                    ordered.add(sprite);
                }
            }
            logger.info("Loaded " + ordered.size() + " sprite icons");
        } catch (Exception e) {
            logger.error("Could not read the sprite catalogue", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {

                }
            }
        }
    }

    public Sprite find(String id) {
        load();
        if (id == null) {
            return null;
        }
        String name = id.startsWith(namespace + ":") ? id.substring(namespace.length() + 1) : id;
        return byName.get(name);
    }

    public List<Sprite> all() {
        load();
        return Collections.unmodifiableList(ordered);
    }

    public List<Sprite> search(String query) {
        load();
        if (query == null || query.trim()
            .isEmpty()) {
            return all();
        }
        String needle = query.trim()
            .toLowerCase();
        List<Sprite> matches = new ArrayList<>();
        for (Sprite sprite : ordered) {
            if (sprite.name.contains(needle) || (sprite.tags != null && sprite.tags.contains(needle))) {
                matches.add(sprite);
            }
        }
        return matches;
    }

    public String qualify(String name) {
        return namespace + ":" + name;
    }

    public int cellSize() {
        load();
        return cell;
    }

    public float minU(Sprite sprite) {
        return sprite.u / (float) atlasWidth;
    }

    public float maxU(Sprite sprite) {
        return (sprite.u + cell) / (float) atlasWidth;
    }

    public float minV(Sprite sprite) {
        return sprite.v / (float) atlasHeight;
    }

    public float maxV(Sprite sprite) {
        return (sprite.v + cell) / (float) atlasHeight;
    }

    public void invalidate() {
        loaded = false;
        byName.clear();
        ordered.clear();
    }

    public static class Sprite {

        public String name;
        public int u;
        public int v;

        public String tags;
    }

    private static class Catalogue {

        int atlasWidth;
        int atlasHeight;
        int cell;
        List<Sprite> icons;
    }
}
