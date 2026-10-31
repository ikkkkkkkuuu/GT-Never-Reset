package com.xyp.gtneverreset.config;

import java.util.HashSet;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

public final class ConfigRegistry {

    public static final String DIRECTORY = "GTNeverReset";
    public static final String GENERAL_FILE = "general";
    public static final String ITEM_FILE = "item";
    public static final String MACHINE_FILE = "machine";
    public static final String RECIPE_FILE = "recipe";
    public static final String CLIENT_FILE = "client";
    private static final Set<Class<?>> REGISTERED = new HashSet<>();

    public static Class<?>[] common() {
        return new Class<?>[] { GregTechConfig.class, QuestsConfig.class, ToolsConfig.class, CropsNHConfig.class,
            SpiceOfLifeConfig.class, VeinMinerPickaxeConfig.class, WirelessFrequencyConfig.class,
            QuantumCraftingConfig.class, RecipeSpeedConfig.class, PackagedProviderConfig.class };
    }

    public static Class<?>[] client() {
        return new Class<?>[] { ClientConfig.class, WirelessMonitorConfig.class };
    }

    public static Class<?>[] all() {
        return new Class<?>[] { GregTechConfig.class, QuestsConfig.class, ToolsConfig.class, CropsNHConfig.class,
            SpiceOfLifeConfig.class, VeinMinerPickaxeConfig.class, ClientConfig.class, WirelessFrequencyConfig.class,
            QuantumCraftingConfig.class, RecipeSpeedConfig.class, PackagedProviderConfig.class,
            WirelessMonitorConfig.class };
    }

    public static void register(Class<?>... types) {
        for (Class<?> type : types) {
            if (REGISTERED.contains(type)) continue;
            try {
                ConfigurationManager.registerConfig(type);
                REGISTERED.add(type);
            } catch (ConfigException exception) {
                throw new IllegalStateException("failed to register configuration " + type.getSimpleName(), exception);
            }
        }
    }

    public static void save(Class<?>... types) {
        for (Class<?> type : types) {
            if (!REGISTERED.contains(type))
                throw new IllegalStateException("configuration not registered: " + type.getName());
        }
        ConfigurationManager.save(types);
    }
}
