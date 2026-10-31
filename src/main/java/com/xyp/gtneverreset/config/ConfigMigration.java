package com.xyp.gtneverreset.config;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizon.gtnhlib.config.Config;

public final class ConfigMigration {

    public static void migrate(File configDirectory, Class<?>... types) {
        File directory = new File(configDirectory, ConfigRegistry.DIRECTORY);
        File legacyFile = new File(directory, "GTNeverReset.cfg");
        if (!legacyFile.isFile()) return;
        Configuration legacy = new Configuration(legacyFile);
        legacy.load();
        Map<File, Configuration> targets = new LinkedHashMap<>();
        for (Class<?> type : types) {
            Config definition = type.getAnnotation(Config.class);
            File file = new File(directory, definition.filename() + ".cfg");
            if (file.isFile() && !targets.containsKey(file)) continue;
            Configuration target = targets.computeIfAbsent(file, Configuration::new);
            String oldRoot = "general." + definition.category();
            for (String category : legacy.getCategoryNames()) {
                if (!category.equals(oldRoot) && !category.startsWith(oldRoot + ".")) continue;
                String newCategory = category.substring("general.".length());
                legacy.getCategory(category)
                    .forEach(
                        (name, property) -> target.getCategory(newCategory)
                            .put(name, property));
            }
        }
        targets.values()
            .forEach(Configuration::save);
    }
}
