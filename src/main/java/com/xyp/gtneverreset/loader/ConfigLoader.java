package com.xyp.gtneverreset.loader;

import java.io.File;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.xyp.gtneverreset.config.MainConfig;

public final class ConfigLoader {

    private static boolean registered;
    private static boolean migrateLegacyClientConfig;

    public static void ensureLoaded() {
        if (!registered) register(new File(Launch.minecraftHome, "config"));
    }

    public static void register(File configDirectory) {
        if (registered) return;
        Config definition = MainConfig.class.getAnnotation(Config.class);
        File configFile = new File(
            new File(configDirectory, definition.configSubDirectory()),
            definition.filename() + ".cfg");
        migrateLegacyClientConfig = !configFile.isFile();
        try {
            ConfigurationManager.registerConfig(MainConfig.class);
            registered = true;
        } catch (ConfigException exception) {
            throw new IllegalStateException("failed to register configuration", exception);
        }
    }

    public static boolean shouldMigrateLegacyClientConfig() {
        return migrateLegacyClientConfig;
    }

    public static void save() {
        ConfigurationManager.save(MainConfig.class);
        migrateLegacyClientConfig = false;
    }
}
