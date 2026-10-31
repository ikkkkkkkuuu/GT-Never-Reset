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
        for (Class<?> type : ConfigRegistry.all()) {
            Config definition = type.getAnnotation(Config.class);
            collect(type, null, "general." + definition.category(), "", entries);
        }
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
                if (!semantic(expected).equals(semantic(entries.get(key)))) throw new AssertionError(
                    "Configuration changed: " + key + "\nExpected: " + expected + "\nActual: " + entries.get(key));
                count++;
            }
            if (count < 50) throw new AssertionError("Incomplete configuration baseline");
            migration();
            System.out.println("Preserved " + count + " configuration defaults and constraints; migration passed");
        }
    }

    private static String semantic(String entry) {
        if (entry == null) return "<missing>";
        String result = entry.substring(0, entry.indexOf(" | "));
        var matcher = java.util.regex.Pattern.compile(
            "@com\\.gtnewhorizon\\.gtnhlib\\.config\\.Config\\.(?:Default\\w+|Range\\w+|Pattern|Sync|RequiresMcRestart|RequiresWorldRestart)\\([^)]*\\)")
            .matcher(entry);
        List<String> values = new ArrayList<>();
        while (matcher.find()) values.add(matcher.group());
        java.util.Collections.sort(values);
        return result + values;
    }

    private static void migration() throws Exception {
        Path temp = Files.createTempDirectory("gtnr-config-migration");
        var minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        Object previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temp.toFile());
        try {
            Path directory = Files.createDirectories(temp.resolve(ConfigRegistry.DIRECTORY));
            Path legacyFile = directory.resolve("GTNeverReset.cfg");
            net.minecraftforge.common.config.Configuration legacy = new net.minecraftforge.common.config.Configuration(
                legacyFile.toFile());
            legacy.get("general.tools", "wirelessTerminalMagnetRange", 64)
                .set(37);
            legacy.get("general.quantum_crafting", "calculationLimitSeconds", 30)
                .set(123);
            legacy.get("general.recipe_speed", "fixedDuration", 1)
                .set(17);
            legacy.get("general.client.wireless_terminal", "buttonTheme", "auto")
                .set("dark");
            legacy.get("general.client.radial_menu.wheel", "outerRadius", 100)
                .set(146);
            legacy.get("general.wireless_monitor", "xOffset", 0)
                .set(91);
            legacy.save();
            byte[] before = Files.readAllBytes(legacyFile);
            ConfigMigration.migrate(temp.toFile(), ConfigRegistry.common());
            if (Files.exists(directory.resolve("client.cfg")))
                throw new AssertionError("Common registration created client settings");
            ConfigMigration.migrate(temp.toFile(), ConfigRegistry.client());
            net.minecraftforge.common.config.Configuration client = new net.minecraftforge.common.config.Configuration(
                directory.resolve("client.cfg")
                    .toFile());
            client.load();
            if (!"dark".equals(
                client.get("client.wireless_terminal", "buttonTheme", "auto")
                    .getString())
                || client.get("client.radial_menu.wheel", "outerRadius", 0)
                    .getInt() != 146
                || client.get("wireless_monitor", "xOffset", 0)
                    .getInt() != 91)
                throw new AssertionError("Client values lost");
            for (String[] entry : new String[][] { { "item", "tools", "wirelessTerminalMagnetRange", "37" },
                { "machine", "quantum_crafting", "calculationLimitSeconds", "123" },
                { "recipe", "recipe_speed", "fixedDuration", "17" } }) {
                net.minecraftforge.common.config.Configuration file = new net.minecraftforge.common.config.Configuration(
                    directory.resolve(entry[0] + ".cfg")
                        .toFile());
                file.load();
                if (!entry[3].equals(
                    file.get(entry[1], entry[2], 0)
                        .getString()))
                    throw new AssertionError("Lost " + entry[2]);
            }
            client.get("wireless_monitor", "xOffset", 0)
                .set(12);
            client.save();
            ConfigMigration.migrate(temp.toFile(), ConfigRegistry.all());
            client.load();
            if (client.get("wireless_monitor", "xOffset", 0)
                .getInt() != 12) throw new AssertionError("Existing settings overwritten");
            if (!Arrays.equals(before, Files.readAllBytes(legacyFile)))
                throw new AssertionError("Legacy file modified");
        } finally {
            minecraftHome.set(null, previousHome);
            try (var paths = Files.walk(temp)) {
                for (Path item : paths.sorted(java.util.Comparator.reverseOrder())
                    .toList()) Files.delete(item);
            }
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
