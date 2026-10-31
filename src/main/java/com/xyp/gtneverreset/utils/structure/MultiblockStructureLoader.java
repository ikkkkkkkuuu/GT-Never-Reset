package com.xyp.gtneverreset.utils.structure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtnrlib.utils.StructureFileCodec;

/** Loads the GTNL MBS1 string-table format from this mod's packaged multiblock resources. */
public final class MultiblockStructureLoader {

    private static final Map<String, String[][]> STRUCTURES = new ConcurrentHashMap<>();

    /** Returns an independent array so callers can transpose or modify it without changing the cache. */
    public static String[][] load(String name) {
        if (!name.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid multiblock resource name: " + name);
        String[][] stored = STRUCTURES.computeIfAbsent(name, MultiblockStructureLoader::readResource);
        String[][] copy = new String[stored.length][];
        for (int layer = 0; layer < stored.length; layer++) copy[layer] = stored[layer].clone();
        return copy;
    }

    private static String[][] readResource(String name) {
        String path = "/assets/" + GTNeverReset.MODID + "/multiblock/" + name + ".mbs";
        try (InputStream resource = MultiblockStructureLoader.class.getResourceAsStream(path)) {
            if (resource == null) throw new IOException("Missing multiblock resource");
            return StructureFileCodec.decode(resource);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot load multiblock structure " + path, failure);
        }
    }

}
