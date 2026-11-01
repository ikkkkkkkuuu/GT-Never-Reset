package com.xyp.gtneverreset.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private static final Map<Class<?>, Boolean> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put(GregTechConfig.class, false);
        CATEGORIES.put(QuestsConfig.class, false);
        CATEGORIES.put(ToolsConfig.class, false);
        CATEGORIES.put(CropsNHConfig.class, false);
        CATEGORIES.put(SpiceOfLifeConfig.class, false);
        CATEGORIES.put(VeinMinerPickaxeConfig.class, false);
        CATEGORIES.put(ClientConfig.class, true);
        CATEGORIES.put(WirelessFrequencyConfig.class, false);
        CATEGORIES.put(QuantumCraftingConfig.class, false);
        CATEGORIES.put(RecipeSpeedConfig.class, false);
        CATEGORIES.put(PackagedProviderConfig.class, false);
        CATEGORIES.put(WirelessMonitorConfig.class, true);
    }

    public static Class<?>[] common() {
        return categories(false);
    }

    public static Class<?>[] client() {
        return categories(true);
    }

    public static Class<?>[] all() {
        return CATEGORIES.keySet()
            .toArray(Class<?>[]::new);
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

    private static Class<?>[] categories(boolean clientOnly) {
        return CATEGORIES.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == clientOnly)
            .map(Map.Entry::getKey)
            .toArray(Class<?>[]::new);
    }
}
