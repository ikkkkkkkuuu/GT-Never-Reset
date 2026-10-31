package com.xyp.gtnrlib.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** Decodes MBS1 string-table structures without choosing an owning mod or resource path. */
public final class StructureFileCodec {

    private static final int MAGIC = 0x4D425331;
    private static final int MAX_FILE_BYTES = 16 * 1024 * 1024;

    private StructureFileCodec() {}

    /** Reads a bounded structure without closing the caller's stream or transposing its layers. */
    public static String[][] decode(InputStream resource) throws IOException {
        byte[] bytes = resource.readNBytes(MAX_FILE_BYTES + 1);
        if (bytes.length > MAX_FILE_BYTES) throw new IOException("Multiblock resource exceeds size limit");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return readBinary(input);
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

    public static String[][] readText(InputStream inputStream) throws IOException {
        List<String[]> rows = new ObjectArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(line.split(",", -1));
            }
        }
        return rows.toArray(new String[0][]);
    }

    public static String[][] readBinary(InputStream inputStream) throws IOException {
        try (InputStream stream = inputStream) {
            return decode(stream);
        }
    }

    public static void writeBinary(OutputStream outputStream, String[][] structure) throws IOException {
        try (DataOutputStream dataOutput = new DataOutputStream(outputStream)) {
            Object2IntLinkedOpenHashMap<String> stringIndexes = new Object2IntLinkedOpenHashMap<>();
            for (String[] row : structure) {
                for (String cell : row) {
                    if (!stringIndexes.containsKey(cell)) {
                        stringIndexes.put(cell, stringIndexes.size());
                    }
                }
            }

            dataOutput.writeInt(MAGIC);
            dataOutput.writeInt(stringIndexes.size());
            for (String cell : stringIndexes.keySet()) {
                byte[] bytes = cell.getBytes(StandardCharsets.UTF_8);
                dataOutput.writeInt(bytes.length);
                dataOutput.write(bytes);
            }
            dataOutput.writeInt(structure.length);
            for (String[] row : structure) {
                dataOutput.writeInt(row.length);
                for (String cell : row) {
                    dataOutput.writeInt(stringIndexes.getInt(cell));
                }
            }
            dataOutput.flush();
        }
    }

}
