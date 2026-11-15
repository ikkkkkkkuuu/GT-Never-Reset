package com.xyp.gtneverreset.config;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config.RequiresMcRestart
@Config(
    modid = "gtneverreset",
    category = "bottle_in_chaos",
    configSubDirectory = ConfigRegistry.DIRECTORY,
    filename = ConfigRegistry.MACHINE_FILE,
    categoryCulling = false)
public final class ChaosConfig {

    @Config.Sync
    @Config.Comment("Bottle in Chaos: BottledChaosChainedProcessingSeconds. Restart required.")
    @Config.DefaultInt(20)
    @Config.RangeInt(min = 1)
    public static int BottledChaosChainedProcessingSeconds = 20;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: BottledChaosCycleRecipes. Restart required.")
    @Config.DefaultBoolean(true)
    public static boolean BottledChaosCycleRecipes = true;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: BottledChaosMinimumOverclockSeconds. Restart required.")
    @Config.DefaultInt(5)
    @Config.RangeInt(min = 1)
    public static int BottledChaosMinimumOverclockSeconds = 5;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: BottledChaosOneStep. Restart required.")
    @Config.DefaultBoolean(false)
    public static boolean BottledChaosOneStep = false;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: CoilTier_EnablePerfectOverclockExtruderMode_MagneticDrivePressureFormer. Restart required.")
    @Config.DefaultInt(11)
    @Config.RangeInt(min = 1)
    public static int CoilTier_EnablePerfectOverclockExtruderMode_MagneticDrivePressureFormer = 11;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: DEFAULT_BATCH_MODE. Restart required.")
    @Config.DefaultBoolean(false)
    public static boolean DEFAULT_BATCH_MODE = false;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: EU_Multiplier_MagneticDrivePressureFormer. Restart required.")
    @Config.DefaultFloat(0.75F)
    public static float EU_Multiplier_MagneticDrivePressureFormer = 0.75F;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: EnableModularizedMachineSystem. Restart required.")
    @Config.DefaultBoolean(true)
    public static boolean EnableModularizedMachineSystem = true;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: Parallel_MagneticDrivePressureFormer. Restart required.")
    @Config.DefaultInt(1024)
    @Config.RangeInt(min = 1)
    public static int Parallel_MagneticDrivePressureFormer = 1024;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: SpeedUpMultiplier_Coil_MagneticDrivePressureFormer. Restart required.")
    @Config.DefaultInt(1)
    @Config.RangeInt(min = 1)
    public static int SpeedUpMultiplier_Coil_MagneticDrivePressureFormer = 1;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: SpeedUpMultiplier_ExtruderMode_MagneticDrivePressureFormer. Restart required.")
    @Config.DefaultInt(8)
    @Config.RangeInt(min = 1)
    public static int SpeedUpMultiplier_ExtruderMode_MagneticDrivePressureFormer = 8;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: SpeedUpMultiplier_OtherMode_MagneticDrivePressureFormer. Restart required.")
    @Config.DefaultInt(16)
    @Config.RangeInt(min = 1)
    public static int SpeedUpMultiplier_OtherMode_MagneticDrivePressureFormer = 16;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: multiplierOfMiracleDoorEUCostABSMode. Restart required.")
    @Config.DefaultInt(1)
    @Config.RangeInt(min = 1)
    public static int multiplierOfMiracleDoorEUCostABSMode = 1;
    @Config.Sync
    @Config.Comment("Bottle in Chaos: multiplierOfMiracleDoorEUCostEBFMode. Restart required.")
    @Config.DefaultInt(2)
    @Config.RangeInt(min = 1)
    public static int multiplierOfMiracleDoorEUCostEBFMode = 2;
    @Config.Sync
    @Config.Comment("Source module parameter: Parallel_LCRMode_IntensifyChemicalDistorter")
    @Config.DefaultInt(1024)
    public static int Parallel_LCRMode_IntensifyChemicalDistorter = 1024;
    @Config.Sync
    @Config.Comment("Source module parameter: Piece_EnablePerfectOverclock_HolySeparator")
    @Config.DefaultInt(16)
    public static int Piece_EnablePerfectOverclock_HolySeparator = 16;
    @Config.Sync
    @Config.Comment("Source module parameter: ParallelPerPiece_HolySeparator")
    @Config.DefaultInt(8)
    public static int ParallelPerPiece_HolySeparator = 8;
    @Config.Sync
    @Config.Comment("Source module parameter: SpeedBonus_MultiplyPerTier_HolySeparator")
    @Config.DefaultFloat(0.9F)
    public static float SpeedBonus_MultiplyPerTier_HolySeparator = 0.9F;
}
