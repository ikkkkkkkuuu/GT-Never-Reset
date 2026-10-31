package com.xyp.gtneverreset.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.gtnewhorizon.gtnhlib.config.Config;

public final class ConfigurationContracts {

    public static void main(String[] args) throws Exception {
        Map<String, String> entries = new TreeMap<>();
        collect(MainConfig.class, null, "general", "", entries);
        if (args.length == 2 && args[0].equals("--record")) {
            List<String> lines = new ArrayList<>();
            entries.forEach((key, value) -> lines.add(key + "\t" + value));
            Files.write(Path.of(args[1]), lines, StandardCharsets.UTF_8);
            return;
        }
        try (var input = ConfigurationContracts.class.getResourceAsStream("/compat/configuration.tsv")) {
            if (input == null) throw new AssertionError("Missing configuration compatibility baseline");
            int count = 0;
            for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.isEmpty()) continue;
                int separator = line.indexOf('\t');
                String key = line.substring(0, separator);
                String expected = line.substring(separator + 1);
                if (!expected.equals(entries.get(key))) throw new AssertionError(
                    "Configuration changed: " + key + "\nExpected: " + expected + "\nActual: " + entries.get(key));
                count++;
            }
            if (count < 50) throw new AssertionError("Incomplete configuration baseline");
            System.out.println("Preserved " + count + " configuration paths, defaults and annotations");
        }
    }

    private static void collect(Class<?> type, Object instance, String prefix, String inherited,
        Map<String, String> entries) throws Exception {
        String category = inherited + annotations(type.getDeclaredAnnotations());
        for (Field field : type.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isPublic(field.getModifiers())) continue;
            Config.Name named = field.getAnnotation(Config.Name.class);
            String key = prefix + "." + (named == null ? field.getName() : named.value());
            String metadata = category + annotations(field.getDeclaredAnnotations());
            Object value = field.get(instance);
            if (field.getType()
                .isPrimitive() || value instanceof String
                || value instanceof Enum<?>) {
                entries.put(key, String.valueOf(value) + " | " + metadata);
            } else if (value != null) {
                collect(field.getType(), value, key, metadata, entries);
            }
        }
    }

    private static String annotations(Annotation[] values) {
        return Arrays.stream(values)
            .filter(
                value -> value.annotationType()
                    .getName()
                    .startsWith(Config.class.getName()))
            .map(Object::toString)
            .sorted()
            .reduce("", (a, b) -> a + b);
    }
}
