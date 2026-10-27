package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.HeatRules;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TierRange;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TieredPart;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TooltipStats;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.utils.machine.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.MultiblockPerformanceReader;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary;
import com.xyp.gtneverreset.utils.structure.PerformanceSummary.BonusStat;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;

import net.minecraft.server.MinecraftServer;

import com.xyp.gtneverreset.GTNeverReset;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import gregtech.api.util.OverclockCalculator;

@Mod(modid = "multiblockperformanceqa", name = "Multiblock Performance QA", version = "1", dependencies = "after:" + GTNeverReset.MODID)
public final class MultiblockPerformanceChecks {

    private int checks;

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        if (!Boolean.getBoolean("gtnr.performanceTooltip.qa") || Boolean.getBoolean("gtnr.performanceTooltip.client.qa")) return;
        String result = "PASS";
        try {
            verify();
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        }
        try {
            File output = new File(System.getProperty("gtnr.performanceTooltip.qa.output"));
            output.mkdirs();
            Files.write(new File(output, "server-result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        MinecraftServer.getServer().initiateShutdown();
    }

    public void verify() {
        new StructureTooltipChecks().verify();
        verifyStaticStats();
        verifyVoltageAndRestore();
        verifyBonuses();
        verifyNonlinearAndThresholds();
        verifyHeatSnapshots();
        verifyHeatRendering();
        verifyGtHeatMath();
        System.out.println("MULTIBLOCK_PERFORMANCE_QA PASS: " + checks + " assertions plus structure regression");
    }

    public static List<MTEEnhancedMultiBlockBase<?>> previews() {
        return List.of(new TieredMachine(), new NonlinearMachine(false), new NonlinearMachine(true),
            new HeatMachine(HeatMode.RECIPE), new HeatMachine(HeatMode.FIXED), new HeatMachine(HeatMode.PERFECT),
            new HeatMachine(HeatMode.NO_SKIP), new HeatMachine(HeatMode.UNLIMITED));
    }

    private void verifyStaticStats() {
        var summary = MultiblockPerformanceReader.readPerformance(new StaticMachine());
        require(summary.stats().equals(new TooltipStats(16, 2, .8, false)), "legacy four-field static stats remain compatible");
        require(summary.tierBonuses().isEmpty(), "constant legacy stats do not invent a tier bonus");
        require(summary.perfectOverclockFrom().isEmpty(), "constant legacy stats do not invent a perfect-OC threshold");
        require(summary.heat() == null, "ordinary machines do not acquire heat rules");
        String text = information(new StaticMachine());
        require(!text.contains("NaN") && !text.contains("Infinity"), "legacy tooltip contains finite display values");
    }

    private void verifyVoltageAndRestore() {
        TieredMachine machine = new TieredMachine();
        long originalVoltage = machine.getMaxInputVoltage();
        long originalPower = machine.getMaxInputEu();
        TooltipStats probed = machine.getTooltipStatsAtTier(TieredPart.VOLTAGE, 3);
        require(probed.parallels() == 256, "tier formulas use the probed MV/HV voltage consistently");
        require(machine.probedVoltage == GTValues.V[3] && machine.probedPower == GTValues.V[3],
            "voltage and total EU probe both use the selected tier's GT voltage");
        require(machine.getMaxInputVoltage() == originalVoltage && machine.getMaxInputEu() == originalPower,
            "voltage and total EU return to their ordinary hatch-derived values");
        require(machine.current(TieredPart.COIL, 71) == 71 && machine.current(TieredPart.VOLTAGE, 73) == 73,
            "probe context is absent after a direct probe");
        machine.nested = true;
        machine.getTooltipStatsAtTier(TieredPart.VOLTAGE, 4);
        require(machine.nestedRestored, "nested coil probe restores the outer voltage probe");
        machine.nested = false;
        machine.failOnCoil = true;
        boolean failed = false;
        try { machine.getTooltipStatsAtTier(TieredPart.COIL, 3); }
        catch (IllegalStateException expected) { failed = true; }
        require(failed, "a failed formula probe propagates its exception");
        require(machine.current(TieredPart.COIL, 71) == 71 && machine.getMaxInputVoltage() == originalVoltage,
            "failed formula probe clears both component and voltage context");
        machine.failOnCoil = false;
        machine.runtimeVoltage = 7;
        machine.runtimeCoil = 4;
        MultiblockPerformanceReader.readPerformance(machine);
        require(machine.runtimeVoltage == 7 && machine.runtimeCoil == 4,
            "reader clones do not change a caller's component fields");
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        machine.setBaseMetaTileEntity(tile);
        boolean boundRejected = false;
        try { machine.getTooltipStatsAtTier(TieredPart.VOLTAGE, 2); }
        catch (IllegalArgumentException | IllegalStateException expected) { boundRejected = true; }
        finally { machine.setBaseMetaTileEntity(null); }
        require(boundRejected, "direct probing rejects a controller bound to a tile entity");
    }

    private void verifyBonuses() {
        PerformanceSummary summary = MultiblockPerformanceReader.readPerformance(new TieredMachine());
        require(hasBonus(summary, TieredPart.VOLTAGE, BonusStat.PARALLEL, true, 2), "voltage parallel doubling is detected");
        require(hasBonus(summary, TieredPart.VOLTAGE, BonusStat.SPEED, false, .25), "linear speed gain is detected");
        require(hasBonus(summary, TieredPart.COIL, BonusStat.DURATION, true, .5), "coil speed doubling is described as halved recipe time");
        require(hasBonus(summary, TieredPart.GLASS, BonusStat.PARALLEL, false, 4), "glass additive parallels are detected");
        require(hasBonus(summary, TieredPart.CASING, BonusStat.ENERGY, false, -.02), "casing additive EU discount is detected");
        require(hasBonus(summary, TieredPart.PARALLEL_CONTROLLER, BonusStat.DURATION, false, -.01), "parallel controller additive recipe-time coefficient is detected");
        require(hasBonus(summary, TieredPart.PARALLEL_CONTROLLER, BonusStat.ENERGY, true, .9), "parallel controller multiplicative EU reduction is detected");
        require(summary.tierBonuses().stream().anyMatch(bonus -> bonus.part() == TieredPart.HIGH_PRESSURE && bonus.stat() == BonusStat.PARALLEL
            && !bonus.perTier() && nearly(bonus.value(), 64)), "high pressure is a one-time switch rather than a repeated tier bonus");
        require(summary.perfectOverclockFrom().stream().anyMatch(threshold -> threshold.part() == TieredPart.VOLTAGE && threshold.tier() == 4),
            "perfect overclock voltage threshold is read from the formula");
        require(summary.perfectOverclockFrom().stream().anyMatch(threshold -> threshold.part() == TieredPart.COIL && threshold.tier() == 3),
            "perfect overclock coil threshold is read from the formula");
    }

    private void verifyNonlinearAndThresholds() {
        PerformanceSummary summary = MultiblockPerformanceReader.readPerformance(new NonlinearMachine(false));
        require(summary.tierBonuses().stream().noneMatch(bonus -> bonus.part() == TieredPart.VOLTAGE),
            "a formula that changes after the first four tiers is not falsely described by one step or factor");
        require(summary.perfectOverclockFrom().stream().anyMatch(threshold -> threshold.part() == TieredPart.VOLTAGE && threshold.tier() == 5),
            "perfect OC begins at the final sustained true suffix, not an earlier isolated tier");
        require(MultiblockPerformanceReader.readPerformance(new NonlinearMachine(true)).perfectOverclockFrom().isEmpty(),
            "an isolated perfect-OC tier with a false highest tier is not advertised as an all-higher-tiers threshold");
        String text = information(new NonlinearMachine(true));
        require(!text.contains("NaN") && !text.contains("Infinity"), "unknown performance numbers are omitted from text");
    }

    private void verifyHeatSnapshots() {
        HeatMachine recipe = new HeatMachine(HeatMode.RECIPE);
        HeatRules rules = recipe.getTooltipHeatRules();
        require(rules != null && rules.overclock() && rules.discount(), "actual custom processing calculator enables both heat rules");
        require(rules.usesRecipeHeat(), "two different recipe heat values reveal the real recipe heat dependency");
        require(nearly(rules.discountFactor(), .9), "custom calculator discount multiplier is captured rather than hardcoded to GT's .95");
        require(rules.heatPerVoltageTier() == 100 && rules.voltageHeatFrom() == 2,
            "machine-declared voltage heating formula is carried into the heat snapshot");
        require(recipe.seenRecipeHeats.stream().distinct().count() >= 2, "heat probing uses at least two distinct actual GTRecipe values");
        HeatRules fixed = new HeatMachine(HeatMode.FIXED).getTooltipHeatRules();
        require(fixed != null && !fixed.usesRecipeHeat() && fixed.freePerfectOverclocks() == 2,
            "fixed heat gives two free perfect overclocks and does not claim to read recipe heat");
        require(new HeatMachine(HeatMode.ORDINARY).getTooltipHeatRules() == null, "no heat flags means no heat tooltip rules");
        require(new HeatMachine(HeatMode.NO_SKIP).getTooltipTierSkips().equals(OptionalInt.of(0)), "zero tier skips are read from the real calculator");
        require(new HeatMachine(HeatMode.UNLIMITED).getTooltipTierSkips().equals(OptionalInt.of(Integer.MAX_VALUE)),
            "unlimited tier skips are read from the real calculator");
        require(new HeatMachine(HeatMode.ORDINARY).getTooltipTierSkips().isEmpty(), "GT's usual one-tier skip stays implicit");
        HeatMachine machine = new HeatMachine(HeatMode.RECIPE);
        int expected = (int) HeatingCoilLevel.getFromTier((byte) 0).getHeat() + 100;
        require(machine.getTooltipStatsAtTier(TieredPart.VOLTAGE, 3) != null && machine.heatAt(3) == expected,
            "the shared coil-and-voltage formula uses the selected voltage tier");
    }

    private void verifyHeatRendering() {
        String normal = information(new HeatMachine(HeatMode.RECIPE));
        require(normal.contains("900") && normal.contains("1800") && normal.contains("100"),
            "temperature discount, heat OC and voltage heating are rendered from real rules");
        String perfect = information(new HeatMachine(HeatMode.PERFECT));
        require(perfect.contains("900") && !perfect.contains("1800"),
            "already-perfect machines retain the heat discount but omit duplicate heat-OC explanation");
    }

    private static boolean hasBonus(PerformanceSummary summary, TieredPart part, BonusStat stat, boolean multiplicative, double value) {
        return summary.tierBonuses().stream().anyMatch(bonus -> bonus.part() == part && bonus.stat() == stat
            && bonus.multiplicative() == multiplicative && nearly(bonus.value(), value));
    }

    private static String information(MTEEnhancedMultiBlockBase<?> machine) {
        return String.join("\n", new AutoMultiblockTooltipBuilder().addAutoTooltip(machine).toolTipFinisher().getInformation());
    }
    private void verifyGtHeatMath() {
        require(nearly(discount(1800, 1800), 1), "no temperature discount at recipe heat");
        require(nearly(discount(2699, 1800), 1), "899 K does not give a discount");
        require(nearly(discount(2700, 1800), .95), "900 K gives the actual GT five percent discount");
        require(nearly(discount(3600, 1800), .95 * .95), "1800 K gives two actual GT discount steps");
        require(nearly(discount(900, 1800), 1 / .95), "GT below-recipe heat behavior is not silently rewritten");
        require(overclock(false, false, 3600).getDuration() == 256, "two regular GT overclocks divide 1024 ticks by four");
        require(overclock(true, false, 3599).getDuration() == 256, "1799 K gives no temperature overclock");
        require(overclock(true, false, 3600).getDuration() == 128, "1800 K upgrades one actual overclock to perfect");
        require(overclock(false, true, 3600).getDuration() == 64, "two always-perfect overclocks divide 1024 ticks by sixteen");
        require(overclock(true, true, 3600).getDuration() == 64, "heat does not add a second bonus to already-perfect overclocking");
    }

    private static double discount(int machineHeat, int recipeHeat) {
        return new OverclockCalculator().setMachineHeat(machineHeat).setRecipeHeat(recipeHeat).setHeatDiscount(true).calculateHeatDiscountMultiplier();
    }

    private static OverclockCalculator overclock(boolean heat, boolean perfect, int machineHeat) {
        OverclockCalculator calculator = new OverclockCalculator().setRecipeEUt(32).setEUt(512).setDuration(1024).setMachineHeat(machineHeat).setRecipeHeat(1800).setHeatOC(heat);
        if (perfect) calculator.enablePerfectOC();
        return calculator.calculate();
    }

    private static boolean nearly(double first, double second) {
        return Math.abs(first - second) < 1e-8;
    }

    private void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private abstract static class Fixture<T extends Fixture<T>> extends GTNRMultiBlockBase<T> {

        private Fixture(String name) { super(name); }

        @Override
        public IStructureDefinition<T> getStructureDefinition() {
            return StructureDefinition.<T>builder().addShape("main", new String[][] { { "A~A" } })
                .addElement('A', StructureUtility.ofBlock(Blocks.cobblestone, 0)).build();
        }

        @Override
        public void construct(ItemStack stack, boolean hints) { buildPiece("main", stack, hints, 1, 0, 0); }

        @Override
        protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
            throw new AssertionError("tooltip must not execute a world structure check");
        }

        @Override
        public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color, boolean active, boolean redstone) {
            return new ITexture[0];
        }
    }

    public static final class StaticMachine extends Fixture<StaticMachine> {
        public StaticMachine() { super("qa_static_performance"); }
        @Override public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) { return new StaticMachine(); }
        @Override public TooltipStats getTooltipStats() { return new TooltipStats(16, 2, .8, false); }
    }

    public static final class TieredMachine extends Fixture<TieredMachine> {
        private int runtimeVoltage = 1;
        private int runtimeCoil;
        private long probedVoltage;
        private long probedPower;
        private boolean failOnCoil;
        private boolean nested;
        private boolean insideNested;
        private boolean nestedRestored;

        public TieredMachine() { super("qa_tiered_performance"); }
        @Override public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) { return new TieredMachine(); }
        public int current(TieredPart part, int fallback) { return getTooltipTier(part, fallback); }
        @Override public TierRange getTooltipTierRange(TieredPart part) {
            return switch (part) { case VOLTAGE -> new TierRange(1, 6); case GLASS -> new TierRange(3, 6);
                case PARALLEL_CONTROLLER -> new TierRange(2, 5); case HIGH_PRESSURE -> new TierRange(0, 1); default -> new TierRange(0, 4); };
        }
        @Override public TooltipStats getTooltipStats() {
            int voltage = getTooltipTier(TieredPart.VOLTAGE, -1);
            if (voltage > 0) {
                probedVoltage = getMaxInputVoltage();
                probedPower = getMaxInputEu();
                if (probedVoltage != probedPower || probedVoltage != GTValues.V[voltage]) throw new AssertionError("probe power disagrees with voltage");
                if (nested && !insideNested) {
                    long before = getMaxInputVoltage();
                    insideNested = true;
                    try { getTooltipStatsAtTier(TieredPart.COIL, 1); }
                    finally { insideNested = false; }
                    nestedRestored = before == getMaxInputVoltage();
                }
            }
            if (failOnCoil && getTooltipTier(TieredPart.COIL, 0) == 3) throw new IllegalStateException("expected test probe failure");
            return super.getTooltipStats();
        }
        @Override protected int getBaseParallelRecipes() {
            int voltage = Math.max(1, getTooltipTier(TieredPart.VOLTAGE, runtimeVoltage));
            int parallels = (64 << Math.max(0, voltage - 1)) + 4 * getTooltipTier(TieredPart.GLASS, 0) + 64 * getTooltipTier(TieredPart.PARALLEL_CONTROLLER, 0);
            return getTooltipTier(TieredPart.HIGH_PRESSURE, 0) == 0 ? parallels : 2 * parallels;
        }
        @Override protected double getBaseSpeedMultiplier() {
            return (2 + .25 * (Math.max(1, getTooltipTier(TieredPart.VOLTAGE, runtimeVoltage)) - 1))
                * Math.pow(2, getTooltipTier(TieredPart.COIL, runtimeCoil)) / (1 - .02 * getTooltipTier(TieredPart.PARALLEL_CONTROLLER, 0));
        }
        @Override protected double getBaseEUtMultiplier() {
            return (.8 - .02 * getTooltipTier(TieredPart.CASING, 0)) * Math.pow(.9, getTooltipTier(TieredPart.PARALLEL_CONTROLLER, 0));
        }
        @Override protected boolean hasPerfectOverclock() {
            return getTooltipTier(TieredPart.VOLTAGE, runtimeVoltage) >= 4 || getTooltipTier(TieredPart.COIL, runtimeCoil) >= 3;
        }
    }

    public static final class NonlinearMachine extends Fixture<NonlinearMachine> {
        private final boolean islandOnly;
        public NonlinearMachine(boolean islandOnly) { super("qa_nonlinear_performance"); this.islandOnly = islandOnly; }
        @Override public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) { return new NonlinearMachine(islandOnly); }
        @Override public List<TieredPart> getTooltipTieredParts() { return List.of(TieredPart.VOLTAGE); }
        @Override public TierRange getTooltipTierRange(TieredPart part) { return new TierRange(1, 6); }
        @Override protected int getBaseParallelRecipes() {
            int tier = getTooltipTier(TieredPart.VOLTAGE, 1);
            return 8 + 2 * tier + (tier <= 4 ? 0 : tier * tier);
        }
        @Override protected double getBaseSpeedMultiplier() { return Double.NaN; }
        @Override protected double getBaseEUtMultiplier() { return Double.POSITIVE_INFINITY; }
        @Override protected boolean hasPerfectOverclock() {
            int tier = getTooltipTier(TieredPart.VOLTAGE, 1);
            return tier == 3 || !islandOnly && tier >= 5;
        }
    }

    private enum HeatMode { RECIPE, FIXED, PERFECT, NO_SKIP, UNLIMITED, ORDINARY }

    public static final class HeatMachine extends Fixture<HeatMachine> {
        private final HeatMode mode;
        private final List<Integer> seenRecipeHeats = new ArrayList<>();
        private boolean recordHeat;
        private int recordedHeat;
        private HeatMachine(HeatMode mode) { super("qa_heat_performance"); this.mode = mode; }
        @Override public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) { return new HeatMachine(mode); }
        @Override public List<TieredPart> getTooltipTieredParts() { return List.of(); }
        @Override protected boolean hasPerfectOverclock() { return mode == HeatMode.PERFECT; }
        @Override protected int getVoltageHeatPerTier() { return mode == HeatMode.RECIPE || mode == HeatMode.PERFECT ? 100 : 0; }
        @Override protected int getVoltageHeatFromTier() { return 2; }
        public int heatAt(int voltageTier) {
            recordHeat = true;
            try { getTooltipStatsAtTier(TieredPart.VOLTAGE, voltageTier); }
            finally { recordHeat = false; }
            return recordedHeat;
        }
        @Override public TooltipStats getTooltipStats() {
            if (recordHeat) recordedHeat = coilAndVoltageHeat(HeatingCoilLevel.None);
            return super.getTooltipStats();
        }
        @Override protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
            return new RecipeThreadProcessingLogic() {
                @Override protected OverclockCalculator createOverclockCalculator(GTRecipe recipe) {
                    seenRecipeHeats.add(recipe.mSpecialValue);
                    OverclockCalculator calculator = super.createOverclockCalculator(recipe);
                    if (mode == HeatMode.RECIPE || mode == HeatMode.PERFECT) {
                        calculator.setRecipeHeat(recipe.mSpecialValue).setMachineHeat(5400).setHeatOC(true).setHeatDiscount(true).setHeatDiscountMultiplier(.9);
                    } else if (mode == HeatMode.FIXED) {
                        calculator.setRecipeHeat(0).setMachineHeat(3600).setHeatOC(true);
                    }
                    if (mode == HeatMode.PERFECT) calculator.enablePerfectOC();
                    if (mode == HeatMode.NO_SKIP) calculator.setMaxTierSkips(0);
                    if (mode == HeatMode.UNLIMITED) calculator.setUnlimitedTierSkips();
                    return calculator;
                }
            };
        }
    }
}
