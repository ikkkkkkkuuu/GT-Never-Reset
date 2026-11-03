package com.xyp.gtneverreset.config;

import java.util.Arrays;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(
    modid = "gtneverreset",
    category = "universal_wand",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.ITEM_FILE,
    categoryCulling = false)
public final class UniversalWandConfig {

    // #tr gtneverreset.config.universalwand.enableforcekill
    // # Force kill
    // # zh_CN 强制击杀
    @Config.LangKey("gtneverreset.config.universalwand.enableforcekill")
    @Config.Comment("Allow force killing.")
    @Config.DefaultBoolean(true)
    public static boolean enableForceKill = true;

    // #tr gtneverreset.config.universalwand.enablebeheading
    // # Beheading
    // # zh_CN 斩首
    @Config.LangKey("gtneverreset.config.universalwand.enablebeheading")
    @Config.Comment("Allow beheading.")
    @Config.DefaultBoolean(true)
    public static boolean enableBeheading = true;

    // #tr gtneverreset.config.universalwand.enablecapture
    // # Mob capture
    // # zh_CN 生物捕捉
    @Config.LangKey("gtneverreset.config.universalwand.enablecapture")
    @Config.Comment("Allow mob capture.")
    @Config.DefaultBoolean(true)
    public static boolean enableCapture = true;

    // #tr gtneverreset.config.universalwand.enableareaattack
    // # Area attack size
    // # zh_CN 范围攻击大小
    @Config.LangKey("gtneverreset.config.universalwand.enableareaattack")
    @Config.Comment("When disabled, lock area attacks to 1x1.")
    @Config.DefaultBoolean(true)
    public static boolean enableAreaAttack = true;

    // #tr gtneverreset.config.universalwand.enableplayerprotection
    // # Player protection
    // # zh_CN 玩家保护
    @Config.LangKey("gtneverreset.config.universalwand.enableplayerprotection")
    @Config.Comment("Allow player protection.")
    @Config.DefaultBoolean(true)
    public static boolean enablePlayerProtection = true;

    // #tr gtneverreset.config.universalwand.enablenoaggro
    // # Advanced invisibility
    // # zh_CN 高级隐身
    @Config.LangKey("gtneverreset.config.universalwand.enablenoaggro")
    @Config.Comment("Allow advanced invisibility.")
    @Config.DefaultBoolean(true)
    public static boolean enableNoAggro = true;

    // #tr gtneverreset.config.universalwand.enableteleport
    // # Teleport
    // # zh_CN 传送
    @Config.LangKey("gtneverreset.config.universalwand.enableteleport")
    @Config.Comment("Allow teleportation.")
    @Config.DefaultBoolean(true)
    public static boolean enableTeleport = true;

    // #tr gtneverreset.config.universalwand.enableacceleration
    // # Time acceleration
    // # zh_CN 时间加速
    @Config.LangKey("gtneverreset.config.universalwand.enableacceleration")
    @Config.Comment("Disabling blocks the acceleration menu and deletes all players' permanent targets in this world.")
    @Config.DefaultBoolean(true)
    public static boolean enableAcceleration = true;

    // #tr gtneverreset.config.universalwand.enablechain
    // # Chain mining
    // # zh_CN 连锁功能
    @Config.LangKey("gtneverreset.config.universalwand.enablechain")
    @Config.Comment("Disabling blocks the chain key, enhanced and small-ore chains, and locks radius and block limit.")
    @Config.DefaultBoolean(true)
    public static boolean enableChain = true;

    // #tr gtneverreset.config.universalwand.enablecrops
    // # Automatic harvesting
    // # zh_CN 全自动收菜
    @Config.LangKey("gtneverreset.config.universalwand.enablecrops")
    @Config.Comment("Disabling also blocks crop-stick support.")
    @Config.DefaultBoolean(true)
    public static boolean enableCrops = true;

    // #tr gtneverreset.config.universalwand.enablebedrock
    // # Force bedrock mining
    // # zh_CN 强制挖掘基岩
    @Config.LangKey("gtneverreset.config.universalwand.enablebedrock")
    @Config.Comment("Allow forced bedrock mining.")
    @Config.DefaultBoolean(true)
    public static boolean enableBedrock = true;

    // #tr gtneverreset.config.universalwand.enablecollection
    // # Item collection
    // # zh_CN 物品收集
    @Config.LangKey("gtneverreset.config.universalwand.enablecollection")
    @Config.Comment("Disabling also blocks AE storage priority.")
    @Config.DefaultBoolean(true)
    public static boolean enableCollection = true;

    // #tr gtneverreset.config.universalwand.enablegttools
    // # GT tools
    // # zh_CN GT 工具
    @Config.LangKey("gtneverreset.config.universalwand.enablegttools")
    @Config.Comment("Disabling turns off both GT tools and locks the tool mode control.")
    @Config.DefaultBoolean(true)
    public static boolean enableGtTools = true;

    // #tr gtneverreset.config.universalwand.enableshears
    // # Shears
    // # zh_CN 剪刀
    @Config.LangKey("gtneverreset.config.universalwand.enableshears")
    @Config.Comment("Allow shears.")
    @Config.DefaultBoolean(true)
    public static boolean enableShears = true;

    // #tr gtneverreset.config.universalwand.enablehoe
    // # Hoe
    // # zh_CN 锄头
    @Config.LangKey("gtneverreset.config.universalwand.enablehoe")
    @Config.Comment("Allow hoe use.")
    @Config.DefaultBoolean(true)
    public static boolean enableHoe = true;

    // #tr gtneverreset.config.universalwand.enableflight
    // # Flight
    // # zh_CN 飞行
    @Config.LangKey("gtneverreset.config.universalwand.enableflight")
    @Config.Comment("Allow carried-wand flight, speed controls and reduced inertia. Disabling revokes wand-owned flight and restores the previous speed; potion effects are unaffected.")
    @Config.DefaultBoolean(true)
    public static boolean enableFlight = true;

    @Config.Comment("Automatic crop harvest attempts per player and tick.")
    @Config.DefaultInt(16)
    @Config.RangeInt(min = 1)
    public static int universalWandBlocksPerTick = 16;

    @Config.Comment("Exhaustion per mined block.")
    @Config.DefaultDouble(0.025)
    @Config.RangeDouble(min = 0, max = 40)
    public static double universalWandMiningExhaustion = 0.025;

    @Config.Comment("Deliver collected drops to the player.")
    @Config.DefaultBoolean(true)
    public static boolean universalWandDropToPlayer = true;

    @Config.Comment("Give a wand on first login.")
    @Config.DefaultBoolean(true)
    public static boolean universalWandGrantOnFirstJoin = true;

    @Config.Comment("Automatic crop search positions per player and tick.")
    @Config.DefaultInt(4096)
    @Config.RangeInt(min = 1, max = 65536)
    public static int universalWandSearchPositionsPerTick = 4096;

    @Config.Comment("Shared automatic crop search budget per tick.")
    @Config.DefaultInt(16384)
    @Config.RangeInt(min = 1, max = 65536)
    public static int universalWandGlobalSearchPositionsPerTick = 16384;

    @Config.Comment("Shared automatic crop harvest budget per tick.")
    @Config.DefaultInt(128)
    @Config.RangeInt(min = 1, max = 65536)
    public static int universalWandGlobalBlocksPerTick = 128;

    @Config.Comment("Maximum targets per area attack.")
    @Config.DefaultInt(64)
    @Config.RangeInt(min = 1)
    public static int universalWandAoeMaxTargets = 64;

    @Config.Sync
    @Config.Comment("Selectable positive acceleration rates. Up to 32 unique values are used.")
    @Config.DefaultIntList({ 2, 4, 16, 32, 64, 128, 256, 512, 1024 })
    public static int[] universalWandAccelerationRates = { 2, 4, 16, 32, 64, 128, 256, 512, 1024 };

    @Config.Comment("Potion ID and amplifier pairs applied while carrying the wand.")
    @Config.DefaultStringList({ "23,1", "10,6", "16,1", "12,1", "13,1", "11,6" })
    public static String[] universalWandEffects = { "23,1", "10,6", "16,1", "12,1", "13,1", "11,6" };

    public static int[] accelerationRates() {
        return Rates.get(universalWandAccelerationRates);
    }

    private static final class Rates {

        private static int[] source;
        private static int[] normalized;

        private static synchronized int[] get(int[] configured) {
            if (source != configured) {
                int[] rates = Arrays.stream(configured)
                    .filter(rate -> rate > 0)
                    .distinct()
                    .sorted()
                    .limit(32)
                    .toArray();
                normalized = rates.length > 0 ? rates : new int[] { 2, 4, 16, 32, 64, 128, 256, 512, 1024 };
                source = configured;
            }
            return normalized;
        }
    }
}
