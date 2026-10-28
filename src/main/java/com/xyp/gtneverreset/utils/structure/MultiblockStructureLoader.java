package com.xyp.gtneverreset.utils.structure;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xyp.gtneverreset.GTNeverReset;

/** Loads the GTNL MBS1 string-table format from this mod's packaged multiblock resources. */
public final class MultiblockStructureLoader {

    private static final int MAGIC = 0x4D425331;
    private static final int MAX_FILE_BYTES = 16 * 1024 * 1024;
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
            byte[] bytes = resource.readNBytes(MAX_FILE_BYTES + 1);
            if (bytes.length > MAX_FILE_BYTES) throw new IOException("Multiblock resource exceeds size limit");
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return readBinary(input);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot load multiblock structure " + path, failure);
        }
    }

    private static String[][] readBinary(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC) throw new IOException("Expected MBS1 header");
        String[] strings = new String[readCount(input, Integer.BYTES, "string table")];
        for (int index = 0; index < strings.length; index++) {
            byte[] bytes = new byte[readCount(input, 1, "string length")];
            input.readFully(bytes);
            strings[index] = new String(bytes, StandardCharsets.UTF_8);
        }
        String[][] shape = new String[readCount(input, Integer.BYTES, "layer count")][];
        if (shape.length == 0) throw new IOException("Empty multiblock structure");
        for (int layer = 0; layer < shape.length; layer++) {
            String[] rows = new String[readCount(input, Integer.BYTES, "row count")];
            if (rows.length == 0) throw new IOException("Empty multiblock layer");
            for (int row = 0; row < rows.length; row++) {
                int index = input.readInt();
                if (index < 0 || index >= strings.length) throw new IOException("Invalid MBS1 string index: " + index);
                rows[row] = strings[index];
            }
            shape[layer] = rows;
        }
        if (input.read() != -1) throw new IOException("Trailing data in multiblock structure");
        return shape;
    }

    /** The input wraps the complete bounded byte array, so available() is an exact remaining byte count. */
    private static int readCount(DataInputStream input, int bytesPerEntry, String label) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > input.available() / bytesPerEntry) throw new IOException("Invalid MBS1 " + label);
        return count;
    }
}
