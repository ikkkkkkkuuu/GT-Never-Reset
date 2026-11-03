package com.xyp.gtneverreset.common.wand;

import java.util.function.BooleanSupplier;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.config.UniversalWandConfig;

/** Shared policy for config entries, item settings and their dependent GUI controls. */
public enum UniversalWandFeature {

    FORCE_KILL(() -> UniversalWandConfig.enableForceKill, "forceKill", "force_kill"),
    BEHEADING(() -> UniversalWandConfig.enableBeheading, "beheading", "beheading"),
    CAPTURE(() -> UniversalWandConfig.enableCapture, "spawnEgg", "spawn_egg"),
    AREA_ATTACK(() -> UniversalWandConfig.enableAreaAttack, "aoeSize", "aoe", "aoe_size"),
    PLAYER_PROTECTION(() -> UniversalWandConfig.enablePlayerProtection, "playerProtection", "player_protection"),
    NO_AGGRO(() -> UniversalWandConfig.enableNoAggro, "noAggro", "no_aggro"),
    TELEPORT(() -> UniversalWandConfig.enableTeleport, "teleportEnabled", "teleport"),
    ACCELERATION(() -> UniversalWandConfig.enableAcceleration, "timeAcceleration", "time_acceleration"),
    CHAIN(() -> UniversalWandConfig.enableChain, "enhancedChain", "enhanced_chain", "gt_small_chain", "radius",
        "max_count"),
    CROPS(() -> UniversalWandConfig.enableCrops, "cropHarvest", "crop_harvest", "crop_sticks"),
    BEDROCK(() -> UniversalWandConfig.enableBedrock, "forceBedrock", "force_bedrock"),
    COLLECTION(() -> UniversalWandConfig.enableCollection, "collectToInventory", "collect_items", "ae_priority"),
    BUILDING(() -> UniversalWandConfig.enableBuilding, "buildingEnabled", "building", "building_ae", "building_normal",
        "building_angel", "building_destruction"),
    // #tr gtneverreset.gui.universal_wand.gt_mode
    // # GT tool mode
    // # zh_CN GT 工具模式
    GT_TOOLS(() -> UniversalWandConfig.enableGtTools, "gtToolMode", "gt_mode", "gt_wrench", "gt_cutter", "gt_off"),
    SHEARS(() -> UniversalWandConfig.enableShears, "shearsEnabled", "shears"),
    HOE(() -> UniversalWandConfig.enableHoe, "hoeEnabled", "hoe"),
    FLIGHT(() -> UniversalWandConfig.enableFlight, "flightSpeedOverride", "flight", "flight_speed_override");

    private static final long ALL_MASK = (1L << values().length) - 1;

    private static final UniversalWandFeature[] FEATURES = values();
    private final BooleanSupplier configured;
    private final String setting;
    private final String[] controls;

    UniversalWandFeature(BooleanSupplier configured, String setting, String... controls) {
        this.configured = configured;
        this.setting = setting;
        this.controls = controls;
    }

    static long configuredMask() {
        long mask = 0;
        for (UniversalWandFeature feature : FEATURES) {
            if (feature.configured.getAsBoolean()) mask |= 1L << feature.ordinal();
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
            case "buildingAeEnabled":
            case "buildingCore":
                return BUILDING;
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
            if ((mask & (1L << feature.ordinal())) != 0 || feature.setting == null) continue;
            if (feature == AREA_ATTACK) settings.setInteger(feature.setting, 1);
            else if (feature == GT_TOOLS) settings.setInteger(feature.setting, 0);
            else settings.setBoolean(feature.setting, false);
            if (feature == CHAIN) settings.setBoolean("gtSmallChain", false);
            if (feature == CROPS) settings.setBoolean("cropSticks", false);
            if (feature == COLLECTION) settings.setBoolean("aeStoragePriority", false);
            if (feature == BUILDING) settings.setBoolean("buildingAeEnabled", false);
            if (feature == ACCELERATION) settings.setInteger("accelerationRate", 0);
        }
    }
}
