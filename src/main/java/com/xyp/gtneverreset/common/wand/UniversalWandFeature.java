package com.xyp.gtneverreset.common.wand;

import java.util.function.BooleanSupplier;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.config.UniversalWandConfig;

/** Shared policy for config entries, item settings and their dependent GUI controls. */
public enum UniversalWandFeature {

    FORCE_KILL(0, () -> UniversalWandConfig.enableForceKill, "forceKill", "force_kill"),
    BEHEADING(1, () -> UniversalWandConfig.enableBeheading, "beheading", "beheading"),
    CAPTURE(2, () -> UniversalWandConfig.enableCapture, "spawnEgg", "spawn_egg"),
    AREA_ATTACK(3, () -> UniversalWandConfig.enableAreaAttack, "aoeSize", "aoe", "aoe_size"),
    PLAYER_PROTECTION(4, () -> UniversalWandConfig.enablePlayerProtection, "playerProtection", "player_protection"),
    NO_AGGRO(5, () -> UniversalWandConfig.enableNoAggro, "noAggro", "no_aggro"),
    TELEPORT(6, () -> UniversalWandConfig.enableTeleport, "teleportEnabled", "teleport"),
    ACCELERATION(7, () -> UniversalWandConfig.enableAcceleration, "timeAcceleration", "time_acceleration"),
    CHAIN(8, () -> UniversalWandConfig.enableChain, "enhancedChain", "enhanced_chain", "gt_small_chain", "radius",
        "max_count"),
    CROPS(9, () -> UniversalWandConfig.enableCrops, "cropHarvest", "crop_harvest", "crop_sticks"),
    BEDROCK(10, () -> UniversalWandConfig.enableBedrock, "forceBedrock", "force_bedrock"),
    COLLECTION(11, () -> UniversalWandConfig.enableCollection, "collectToInventory", "collect_items", "ae_priority"),
    // #tr gtneverreset.gui.universal_wand.gt_mode
    // # GT tool mode
    // # zh_CN GT 工具模式
    GT_TOOLS(13, () -> UniversalWandConfig.enableGtTools, "gtToolMode", "gt_mode"),
    SHEARS(14, () -> UniversalWandConfig.enableShears, "shearsEnabled", "shears"),
    HOE(15, () -> UniversalWandConfig.enableHoe, "hoeEnabled", "hoe"),
    FLIGHT(16, () -> UniversalWandConfig.enableFlight, "flightSpeedOverride", "flight", "flight_speed_override");

    private static final UniversalWandFeature[] FEATURES = values();
    private static final long ALL_MASK = java.util.Arrays.stream(FEATURES)
        .mapToLong(feature -> feature.mask)
        .reduce(0L, (a, b) -> a | b);
    final long mask;
    private final BooleanSupplier configured;
    private final String setting;
    private final String[] controls;

    UniversalWandFeature(int bit, BooleanSupplier configured, String setting, String... controls) {
        this.mask = 1L << bit;
        this.configured = configured;
        this.setting = setting;
        this.controls = controls;
    }

    static long configuredMask() {
        long mask = 0;
        for (UniversalWandFeature feature : FEATURES) {
            if (feature.configured.getAsBoolean()) mask |= feature.mask;
        }
        return mask;
    }

    public boolean allowed() {
        return UniversalWandFeaturePolicy.allowed(this);
    }

    public static UniversalWandFeature forControl(String key) {
        for (UniversalWandFeature feature : FEATURES)
            for (String control : feature.controls) if (control.equals(key)) return feature;
        return null;
    }

    public static UniversalWandFeature forSetting(String key) {
        for (UniversalWandFeature feature : FEATURES)
            if (feature.setting != null && feature.setting.equals(key)) return feature;
        switch (key) {
            case "gtSmallChain":
            case "enhancedChainRadius":
            case "maxMiningCount":
                return CHAIN;
            case "cropSticks":
                return CROPS;
            case "aeStoragePriority":
                return COLLECTION;
            case "accelerationRate":
            case "accelerationMode":
            case "accelerationGtMode":
                return ACCELERATION;
            default:
                return null;
        }
    }

    public static boolean canChange(String key) {
        UniversalWandFeature feature = forSetting(key);
        return feature == null || feature.allowed();
    }

    /** Operates on a settings copy; item update persists this result on the server. */
    public static void restrict(NBTTagCompound settings) {
        long mask = UniversalWandFeaturePolicy.mask();
        if ((mask & ALL_MASK) == ALL_MASK) return;
        for (UniversalWandFeature feature : FEATURES) {
            if ((mask & feature.mask) != 0 || feature.setting == null) continue;
            if (feature == AREA_ATTACK) settings.setInteger(feature.setting, 1);
            else if (feature == GT_TOOLS) settings.setInteger(feature.setting, 0);
            else settings.setBoolean(feature.setting, false);
            if (feature == CHAIN) settings.setBoolean("gtSmallChain", false);
            if (feature == CROPS) settings.setBoolean("cropSticks", false);
            if (feature == COLLECTION) settings.setBoolean("aeStoragePriority", false);
            if (feature == ACCELERATION) settings.setInteger("accelerationRate", 0);
        }
    }
}
