package com.xyp.gtneverreset.loader;

import java.io.File;

import net.minecraft.launchwrapper.Launch;

import com.xyp.gtneverreset.config.ConfigMigration;
import com.xyp.gtneverreset.config.ConfigRegistry;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

public final class ConfigLoader implements IContentLoader {

    private static boolean commonRegistered;
    private static boolean clientRegistered;
    private static boolean migrateLegacyClientConfig;

    @Override
    public String name() {
        return "config";
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        register(event.getModConfigurationDirectory());
    }

    public static void ensureLoaded() {
        register(new File(Launch.minecraftHome, "config"));
    }

    public static void register(File configDirectory) {
        if (!commonRegistered) {
            ConfigMigration.migrate(configDirectory, ConfigRegistry.common());
            ConfigRegistry.register(ConfigRegistry.common());
            commonRegistered = true;
        }
        if (FMLLaunchHandler.side()
            .isClient() && !clientRegistered) {
            File directory = new File(configDirectory, ConfigRegistry.DIRECTORY);
            migrateLegacyClientConfig = !new File(directory, "client.cfg").isFile()
                && !new File(directory, "GTNeverReset.cfg").isFile();
            ConfigMigration.migrate(configDirectory, ConfigRegistry.client());
            ConfigRegistry.register(ConfigRegistry.client());
            clientRegistered = true;
        }
    }

    public static boolean shouldMigrateLegacyClientConfig() {
        return migrateLegacyClientConfig;
    }

    public static void saveClient() {
        ConfigRegistry.save(ConfigRegistry.client());
        migrateLegacyClientConfig = false;
    }
}
