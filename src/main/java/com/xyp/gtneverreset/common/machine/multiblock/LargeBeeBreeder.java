package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.error;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.LargeBeeBreederGui;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingHelper;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingHelper.FoundationRequirement;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingPlanner;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingTiers;
import com.xyp.gtneverreset.common.machine.multiblock.bee.DronePool;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.config.BeeBreederConfig;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import forestry.api.apiculture.IAlleleBeeSpecies;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import lombok.Getter;

@IMetaTileEntity.SkipGenerateDescription

public class LargeBeeBreeder extends MTEExtendedPowerMultiBlockBase<LargeBeeBreeder>
    implements ISurvivalConstructable, MultiblockTooltipSource {

    public static final int CASING_TEXTURE_ID = (126 << 7) | 127;

    private static final long BASE_EUT_PER_BREEDING = 512L;
    private static final int BASE_VOLTAGE_TIER = VoltageIndex.HV;
    private static final int BASE_GLASS_TIER = VoltageIndex.HV;
    private static final int CHANCE_PER_GLASS_TIER = 2;

    private static final int TICKS_PER_BREEDING = 32 * 20;
    private static final int INPUT_SCAN_INTERVAL = 100;
    private static final int FOUNDATION_SCAN_INTERVAL = 200;
    private static final int BASE_PARALLEL_STEPS = 1;
    private static final int ROYAL_JELLY_PER_PARALLEL = 2;

    private static final byte OPERATION_NONE = 0;
    private static final byte OPERATION_BREEDING = 1;
    private static final byte OPERATION_OUTPUT = 2;

    private static final String STRUCTURE_NAME = "large_bee_breeder";
    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 9;
    private static final int VERTICAL_OFFSET = 9;
    private static final int DEPTH_OFFSET = 3;

    private static int getParallelForVoltageTier(int voltageTier) {
        int upgrades = Math.max(0, Math.min(voltageTier, VoltageIndex.MAX) - BASE_VOLTAGE_TIER);
        return BASE_PARALLEL_STEPS << upgrades;
    }

    public static int getSpeciesVoltageTier(String species) {
        return BeeBreedingTiers.getVoltageTier(species);
    }

    public static ModsItemlist getCasingMaterial() {
        return ModsItemlist.EtFuturumRequiemYellowConcrete;
    }

    @Getter
    private String targetBeeSpecies = "";
    @Getter
    private DronePool dronePool = new DronePool();
    private byte activeOperation = OPERATION_NONE;
    private List<BreedingAttempt> activeBreedingBatch = new ArrayList<>();
    @Getter
    private int pendingPrincessOutputs = 0;

    private final List<ParameterModuleHatch> parameterModules = new ArrayList<>();
    private IStructureDefinition<LargeBeeBreeder> structureDefinition;
    private int mCountCasing;
    private int mGlassTier = -1;
    private BeeBreedingPlanner.Plan breedingPlan = BeeBreedingPlanner.Plan.empty("");
    private List<BeeBreedingHelper.BreedingStep> breedingChain = new ArrayList<>();
    private boolean breedingPlanDirty = true;
    private final List<ItemStack> foundationInputs = new ArrayList<>();
    private final Map<String, ItemStack> foundationMatches = new HashMap<>();
    private long lastFoundationScanTick = Long.MIN_VALUE;
    private List<FoundationRequirement> missingFoundations = Collections.emptyList();
    private boolean activeFoundationsChecked;
    private int activeRequiredVoltageTier = BASE_VOLTAGE_TIER;
    @Getter
    private String missingDroneSpecies = "";
    @Getter
    private boolean allTasksBlocked = false;
    @Getter
    private int chainTotalSteps = 0;
    @Getter
    private int chainCompletedSteps = 0;
    @Getter
    private boolean targetInputValid = true;
    @Getter
    private int breedingRevision;

    public LargeBeeBreeder(String aName) {
        super(aName);
    }

    public LargeBeeBreeder(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new LargeBeeBreeder(this.mName);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide() && mMachine && isFoundationScanDue()) {
            startRecipeProcessing();
            try {
                refreshFoundationInputs(getStoredInputs());
            } finally {
                endRecipeProcessing();
            }
            refreshBreedingPlan();
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            for (ParameterModuleHatch module : parameterModules) module.setWorking(mMaxProgresstime > 0);
        }
        if (aBaseMetaTileEntity.isServerSide() && aTick % INPUT_SCAN_INTERVAL == 0) {
            if (mMachine) {
                startRecipeProcessing();
                try {
                    scanInputBuses();
                } finally {
                    endRecipeProcessing();
                }
            }
            refreshBreedingPlan();
        }
    }

    @Override
    public IStructureDefinition<LargeBeeBreeder> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<LargeBeeBreeder>builder()
                .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load(STRUCTURE_NAME)))
                .addElement('A', chainAllGlasses(-1, (te, t) -> te.mGlassTier = t, te -> te.mGlassTier))
                .addElement('B', ofStructureBlock(ModsItemlist.ChiselYellowHempcrete))
                .addElement('C', ofStructureBlock(ModsItemlist.ChiselBrownHempcrete))
                .addElement('D', ofStructureBlock(ModsItemlist.ChiselBlackHempcrete))
                .addElement('E', ofStructureBlock(ModsItemlist.ChiselOrangeWoolenClay))
                .addElement('F', ofStructureBlock(ModsItemlist.ChiselBrownWoolenClay))
                .addElement('G', ofStructureBlock(ModsItemlist.EtFuturumRequiemOrangeConcrete))
                .addElement(
                    'H',
                    ofChain(
                        StructureTooltipHooks.describeAsHatch(
                            ofHatchAdder(LargeBeeBreeder::addParameterModuleToMachineList, CASING_TEXTURE_ID, 1),
                            ParameterModuleHatch.NAME_KEY),
                        buildHatchAdder(LargeBeeBreeder.class)
                            .atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy), Maintenance)
                            .casingIndex(getCasingTextureID())
                            .hint(1)
                            .buildAndChain(),
                        onElementPass(x -> ++x.mCountCasing, ofStructureBlock(getCasingMaterial()))))
                .addElement('I', ofStructureBlock(ModsItemlist.EtFuturumRequiemBrownConcrete))
                .addElement('J', ofStructureBlock(ModsItemlist.ExtraUtilitiesLightBlueLapisCaelestis))
                .build();
        }
        return structureDefinition;
    }

    private IStructureElement<LargeBeeBreeder> ofStructureBlock(ModsItemlist material) {
        ItemStack stack = material.get(1);
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return error();
        return ofBlock(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage());
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        this.buildPiece(STRUCTURE_PIECE, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
        return this.survivalBuildPiece(
            STRUCTURE_PIECE,
            stackSize,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCountCasing = 0;
        mGlassTier = -1;
        if (!checkPiece(STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) return;
        collectTooltipStructureRequirements(errors);

        markBreedingChanged();
    }

    @Override
    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        if (!StructureTooltipHooks.onCheckCasingMin(this, 10)) checkCasingMin(errors, mCountCasing, 10);
        if (!StructureTooltipHooks.onCheckHatches(this, 1, Integer.MAX_VALUE, HatchElement.InputBus)
            && mInputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, InputBus, 0, 1));
        if (!StructureTooltipHooks.onCheckHatches(this, 1, Integer.MAX_VALUE, HatchElement.OutputBus)
            && mOutputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, OutputBus, 0, 1));
        int energyHatchCount = mEnergyHatches.size() + mExoticEnergyHatches.size();
        if (!StructureTooltipHooks
            .onCheckHatches(this, 1, Integer.MAX_VALUE, HatchElement.Energy, HatchElement.ExoticEnergy)
            && energyHatchCount < 1)
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Energy, energyHatchCount, 1));
        if (shouldCheckMaintenance()
            && !StructureTooltipHooks.onCheckHatches(this, 1, Integer.MAX_VALUE, HatchElement.Maintenance)
            && mMaintenanceHatches.isEmpty())
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Maintenance, 0, 1));
        Map<MachineModuleType, Integer> counts = new EnumMap<>(MachineModuleType.class);
        for (ParameterModuleHatch module : parameterModules) counts.merge(module.getModuleType(), 1, Integer::sum);
        for (var entry : counts.entrySet()) {
            int limit = getParameterModuleLimit(entry.getKey());
            if (entry.getValue() > limit) errors.add(
                StructureErrors.hatchCount(
                    ErrorType.TOO_MANY,
                    TranslatableText.lang(
                        entry.getKey()
                            .getTranslationKey()),
                    entry.getValue(),
                    limit));
        }
    }

    public int checkEnergyHatchTier() {
        int tier = VoltageIndex.ULV;
        for (MTEHatch hatch : GTUtility.validMTEList(mEnergyHatches)) {
            tier = Math.max(tier, (int) hatch.getInputTier());
        }
        for (MTEHatch hatch : GTUtility.validMTEList(mExoticEnergyHatches)) {
            tier = Math.max(tier, (int) hatch.getInputTier());
        }
        return tier;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Nonnull
    @Override
    public CheckRecipeResult checkProcessing() {
        List<ItemStack> inputs = scanInputBuses();
        activeOperation = OPERATION_NONE;
        activeBreedingBatch.clear();
        activeFoundationsChecked = false;

        if (targetBeeSpecies == null || targetBeeSpecies.isEmpty()) {
            refreshBreedingPlan();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        refreshBreedingPlan();
        if (pendingPrincessOutputs <= 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        if (getBreedingVoltageTier() < BASE_VOLTAGE_TIER) return insufficientBreedingVoltage(BASE_VOLTAGE_TIER);
        if (dronePool.hasDrone(targetBeeSpecies)) {
            int requiredTier = getSpeciesVoltageTier(targetBeeSpecies);
            if (getBreedingVoltageTier() < requiredTier) return insufficientBreedingVoltage(requiredTier);
        }

        long eutPerAttempt = getEuPerBreedingAttempt();
        if (dronePool.hasDrone(targetBeeSpecies)) {
            if (getMaxInputPower() < eutPerAttempt) return CheckRecipeResultRegistry.insufficientPower(eutPerAttempt);
            ItemStack princess = BeeBreedingHelper.createPrincess(targetBeeSpecies);
            if (princess == null) {
                allTasksBlocked = true;
                missingDroneSpecies = targetBeeSpecies;
                markBreedingChanged();

                // @formatter:off
                // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_unreachable_target
                // # Current species archive cannot reach the target species!
                // # zh_CN 当前物种档案无法到达目标品种！
                // @formatter:on
                return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_unreachable_target");
            }
            ItemStack drone = BeeBreedingHelper.createDrone(targetBeeSpecies);
            mOutputItems = drone == null ? new ItemStack[] { princess } : new ItemStack[] { princess, drone };
            activeOperation = OPERATION_OUTPUT;
            prepareBreedingCycle(1, inputs);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        List<BeeBreedingHelper.BreedingStep> readySteps = breedingPlan
            .getReadySteps(dronePool.getAvailableSpecies(), Integer.MAX_VALUE);
        if (readySteps.isEmpty()) {
            allTasksBlocked = true;
            missingDroneSpecies = breedingPlan.getFirstMissingSpecies();
            if (missingDroneSpecies.isEmpty()) missingDroneSpecies = targetBeeSpecies;
            markBreedingChanged();

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_missing_drone
            // # Species archive is missing a required parent!
            // # zh_CN 物种档案中缺少必要亲本！
            // @formatter:on
            return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_drone");
        }

        int requiredTier = readySteps.stream()
            .mapToInt(step -> getSpeciesVoltageTier(step.result))
            .min()
            .orElse(BASE_VOLTAGE_TIER);
        readySteps.removeIf(step -> getBreedingVoltageTier() < getSpeciesVoltageTier(step.result));
        if (readySteps.isEmpty()) return insufficientBreedingVoltage(requiredTier);
        if (getMaxInputPower() < eutPerAttempt) return CheckRecipeResultRegistry.insufficientPower(eutPerAttempt);

        int poweredParallels = (int) Math.min(
            Math.min(readySteps.size(), getMaxParallelBreedingSteps()),
            Math.max(0L, getMaxInputPower()) / eutPerAttempt);
        readySteps = readySteps.subList(0, poweredParallels);
        updateMissingFoundations(readySteps);
        if (!missingFoundations.isEmpty()) {
            allTasksBlocked = true;

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_missing_foundation
            // # Missing mutation foundation. Waiting for input.
            // # zh_CN 缺少杂交基石，等待补充。
            // @formatter:on
            return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_foundation");
        }

        double chanceBonus = getMutationChanceBonus();
        List<BreedingAttempt> batch = new ArrayList<>(readySteps.size());
        for (BeeBreedingHelper.BreedingStep step : readySteps) {
            batch.add(new BreedingAttempt(step, step.chance + chanceBonus));
        }

        activeOperation = OPERATION_BREEDING;
        activeBreedingBatch = batch;
        activeFoundationsChecked = true;
        allTasksBlocked = false;
        missingDroneSpecies = "";
        prepareBreedingCycle(batch.size(), inputs);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private CheckRecipeResult insufficientBreedingVoltage(int requiredTier) {
        allTasksBlocked = true;
        missingDroneSpecies = "";
        return SimpleCheckRecipeResult.ofFailure(switch (requiredTier) {

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_voltage_ev
            // # Insufficient voltage. Requires EV. Waiting for a power upgrade.
            // # zh_CN 电压不足，需要 EV，等待升级供电。
            // @formatter:on
            case VoltageIndex.EV -> "gtnr_bee_breeder_voltage_ev";

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_voltage_iv
            // # Insufficient voltage. Requires IV. Waiting for a power upgrade.
            // # zh_CN 电压不足，需要 IV，等待升级供电。
            // @formatter:on
            case VoltageIndex.IV -> "gtnr_bee_breeder_voltage_iv";

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_voltage_luv
            // # Insufficient voltage. Requires LuV. Waiting for a power upgrade.
            // # zh_CN 电压不足，需要 LuV，等待升级供电。
            // @formatter:on
            case VoltageIndex.LuV -> "gtnr_bee_breeder_voltage_luv";

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_voltage_zpm
            // # Insufficient voltage. Requires ZPM. Waiting for a power upgrade.
            // # zh_CN 电压不足，需要 ZPM，等待升级供电。
            // @formatter:on
            case VoltageIndex.ZPM -> "gtnr_bee_breeder_voltage_zpm";

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_voltage_uv
            // # Insufficient voltage. Requires UV. Waiting for a power upgrade.
            // # zh_CN 电压不足，需要 UV，等待升级供电。
            // @formatter:on
            case VoltageIndex.UV -> "gtnr_bee_breeder_voltage_uv";

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_voltage_uhv
            // # Insufficient voltage. Requires UHV. Waiting for a power upgrade.
            // # zh_CN 电压不足，需要 UHV，等待升级供电。
            // @formatter:on
            case VoltageIndex.UHV -> "gtnr_bee_breeder_voltage_uhv";

            // @formatter:off
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_voltage_hv
            // # Insufficient voltage. Requires HV. Waiting for a power upgrade.
            // # zh_CN 电压不足，需要 HV，等待升级供电。
            // @formatter:on
            default -> "gtnr_bee_breeder_voltage_hv";
        });
    }

    public long getEuPerBreedingAttempt() {
        return getEnergyCostPerTick(1);
    }

    private long getEnergyCostPerTick(int parallelAttempts) {
        return getEnergyCostPerTick(parallelAttempts, 0);
    }

    private long getEnergyCostPerTick(int parallelAttempts, int extraOverclocks) {
        long eutPerAttempt = createBreedingOverclockCalculator(extraOverclocks).getConsumption() << getGlassUpgrades();
        return eutPerAttempt * Math.max(0, parallelAttempts);
    }

    private OverclockCalculator createBreedingOverclockCalculator() {
        return createBreedingOverclockCalculator(0);
    }

    private OverclockCalculator createBreedingOverclockCalculator(int extraOverclocks) {
        int voltageUpgrades = Math.max(0, Math.min(getBreedingVoltageTier(), VoltageIndex.MAX) - BASE_VOLTAGE_TIER)
            + extraOverclocks;
        ParameterModuleHatch speed = parameterModule(MachineModuleType.Speed);
        ParameterModuleHatch power = parameterModule(MachineModuleType.PowerConsumption);
        ParameterModuleHatch overclock = parameterModule(MachineModuleType.Overclock);
        return new OverclockCalculator().setRecipeEUt(BASE_EUT_PER_BREEDING)
            .setEUt(BASE_EUT_PER_BREEDING << (2 * voltageUpgrades))
            .setDuration(TICKS_PER_BREEDING)
            .setMaxOverclocks(voltageUpgrades)
            .setEUtDiscount(power == null ? 1 : power.getPowerMultiplier())
            .setDurationModifier(speed == null ? 1 : 1D / speed.getParameter())
            .setEUtIncreasePerOC(overclock == null ? 4 : overclock.getPowerIncrease())
            .setDurationDecreasePerOC(overclock == null ? 2 : overclock.getTimeReduction())
            .calculate();
    }

    private void prepareBreedingCycle(long eut) {
        updateActiveVoltageTier();
        mMaxProgresstime = createBreedingOverclockCalculator().getDuration();
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        lEUt = -eut;
    }

    private void prepareBreedingCycle(int parallels, List<ItemStack> inputs) {
        long eut = getEnergyCostPerTick(parallels);
        prepareBreedingCycle(eut);
        if (mMaxProgresstime <= 1) return;
        long acceleratedEut = getEnergyCostPerTick(parallels, 1);
        if (acceleratedEut > Math.max(0L, getMaxInputPower())) return;

        ItemStack royalJelly = ModsItemlist.ForestryRoyalJelly.get(1);
        if (royalJelly == null) return;
        int required = parallels * ROYAL_JELLY_PER_PARALLEL;
        int available = 0;
        List<ItemStack> jellyStacks = new ArrayList<>();
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack input : inputs) {
            if (input == null || input.stackSize <= 0
                || !seen.add(input)
                || !GTUtility.areStacksEqual(input, royalJelly)) continue;
            jellyStacks.add(input);
            available += Math.min(input.stackSize, required - available);
            if (available == required) break;
        }
        if (available < required) return;

        for (ItemStack jelly : jellyStacks) {
            int consumed = Math.min(jelly.stackSize, required);
            jelly.stackSize -= consumed;
            required -= consumed;
            if (required == 0) break;
        }
        mMaxProgresstime = createBreedingOverclockCalculator(1).getDuration();
        lEUt = -acceleratedEut;
        updateSlots();
        markBaseTileDirty();
    }

    private void updateActiveVoltageTier() {
        activeRequiredVoltageTier = BASE_VOLTAGE_TIER;
        if (activeOperation == OPERATION_OUTPUT) {
            String outputSpecies = mOutputItems != null && mOutputItems.length > 0
                ? BeeBreedingHelper.getBeeUID(mOutputItems[0])
                : targetBeeSpecies;
            activeRequiredVoltageTier = getSpeciesVoltageTier(outputSpecies);
        }
        for (BreedingAttempt attempt : activeBreedingBatch) {
            activeRequiredVoltageTier = Math.max(activeRequiredVoltageTier, getSpeciesVoltageTier(attempt.step.result));
        }
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (getBreedingVoltageTier() < activeRequiredVoltageTier) {
            checkRecipeResult = insufficientBreedingVoltage(activeRequiredVoltageTier);
            return false;
        }
        if (activeOperation == OPERATION_BREEDING) {
            if (!activeFoundationsChecked) {
                List<BeeBreedingHelper.BreedingStep> active = new ArrayList<>(activeBreedingBatch.size());
                for (BreedingAttempt attempt : activeBreedingBatch) active.add(attempt.step);
                updateMissingFoundations(active);
                activeFoundationsChecked = true;
            }
            if (!missingFoundations.isEmpty()) {
                checkRecipeResult = SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_foundation");
                return false;
            }
        }
        checkRecipeResult = CheckRecipeResultRegistry.SUCCESSFUL;
        allTasksBlocked = false;
        return super.onRunningTick(stack);
    }

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        settleActiveOperation();
        markBaseTileDirty();
    }

    private void settleActiveOperation() {
        if (activeOperation == OPERATION_NONE) return;

        boolean archiveChanged = false;
        if (activeOperation == OPERATION_OUTPUT) {
            if (pendingPrincessOutputs > 0) {
                pendingPrincessOutputs--;
            }
        } else if (activeOperation == OPERATION_BREEDING) {
            for (BreedingAttempt attempt : activeBreedingBatch) {
                if (dronePool.hasDrone(attempt.step.result)) continue;
                if (BeeBreedingHelper.tryMutation(attempt.effectiveChance)) {
                    archiveChanged |= dronePool.unlockSpecies(attempt.step.result);
                }
            }
        }

        activeOperation = OPERATION_NONE;
        activeBreedingBatch.clear();
        if (archiveChanged) invalidateBreedingPlan();
        markBreedingChanged();
    }

    public void setTargetBeeSpecies(String species) {

        String candidate = species == null ? "" : species.trim();
        if (!candidate.isEmpty()) {
            IAlleleBeeSpecies resolved = BeeBreedingHelper.getSpeciesByUID(candidate);
            if (resolved == null) {
                resolved = BeeBreedingHelper.getSpeciesByName(candidate);
            }
            targetInputValid = resolved != null;
            if (resolved != null) {

                String resolvedUID = resolved.getUID();
                if (!resolvedUID.equals(targetBeeSpecies)) {
                    this.targetBeeSpecies = resolvedUID;
                    invalidateBreedingPlan();
                    markBaseTileDirty();
                }
                return;
            }
            markBreedingChanged();
            return;
        }
        targetInputValid = true;
        if (!targetBeeSpecies.isEmpty()) {
            this.targetBeeSpecies = "";
            invalidateBreedingPlan();
            markBaseTileDirty();
        }
    }

    private void refreshBreedingPlan() {
        boolean previousBlocked = allTasksBlocked;
        String previousMissing = missingDroneSpecies;
        int previousCompleted = chainCompletedSteps;

        if (targetBeeSpecies == null || targetBeeSpecies.isEmpty()) {
            if (breedingPlanDirty || !breedingChain.isEmpty()) markBreedingChanged();
            breedingPlan = BeeBreedingPlanner.Plan.empty("");
            breedingChain = new ArrayList<>();
            breedingPlanDirty = false;
            allTasksBlocked = false;
            missingDroneSpecies = "";
        } else {
            if (breedingPlanDirty) {
                breedingPlan = BeeBreedingPlanner.plan(targetBeeSpecies, dronePool.getAvailableSpecies());
                breedingChain = new ArrayList<>(breedingPlan.getSteps());
                breedingPlanDirty = false;
                markBreedingChanged();
            }

            boolean targetUnlocked = dronePool.hasDrone(targetBeeSpecies);
            boolean hasReadyStep = !breedingPlan.getReadySteps(dronePool.getAvailableSpecies(), 1)
                .isEmpty();
            allTasksBlocked = pendingPrincessOutputs > 0 && !targetUnlocked && !hasReadyStep;
            missingDroneSpecies = allTasksBlocked ? breedingPlan.getFirstMissingSpecies() : "";
            if (allTasksBlocked && missingDroneSpecies.isEmpty()) {
                missingDroneSpecies = targetBeeSpecies;
            }
        }

        if (activeOperation == OPERATION_BREEDING) {
            updateMissingFoundations(getActiveBreedingSteps());
        } else if (pendingPrincessOutputs > 0 && !dronePool.hasDrone(targetBeeSpecies)) {
            updateMissingFoundations(
                breedingPlan.getReadySteps(dronePool.getAvailableSpecies(), getMaxParallelBreedingSteps()));
        } else {
            updateMissingFoundations(Collections.emptyList());
        }
        allTasksBlocked |= !missingFoundations.isEmpty();
        updateChainDisplayInfo();
        if (previousBlocked != allTasksBlocked || !Objects.equals(previousMissing, missingDroneSpecies)
            || previousCompleted != chainCompletedSteps) {
            markBreedingChanged();
        }
    }

    private void invalidateBreedingPlan() {
        breedingPlanDirty = true;
        markBreedingChanged();
    }

    private List<ItemStack> scanInputBuses() {
        ArrayList<ItemStack> inputs = getStoredInputs();
        boolean consumed = false;
        boolean archiveChanged = false;

        for (ItemStack stack : inputs) {
            if (stack == null || stack.stackSize <= 0) continue;
            boolean princess = BeeBreedingHelper.isPrincess(stack);
            if (!princess && !BeeBreedingHelper.isDrone(stack)) continue;
            String uid = BeeBreedingHelper.getBeeUID(stack);
            if (uid == null || uid.isEmpty()) continue;

            archiveChanged |= dronePool.unlockSpecies(uid);
            if (princess) {
                long newPending = (long) pendingPrincessOutputs + stack.stackSize;
                pendingPrincessOutputs = (int) Math.min(Integer.MAX_VALUE, newPending);
            }
            stack.stackSize = 0;
            consumed = true;
        }

        if (consumed) {
            updateSlots();
            markBaseTileDirty();
            if (archiveChanged) invalidateBreedingPlan();
            else markBreedingChanged();
        }
        if (isFoundationScanDue()) refreshFoundationInputs(inputs);
        return inputs;
    }

    private boolean isFoundationScanDue() {
        return BeeBreederConfig.requireMutationFoundations && (lastFoundationScanTick == Long.MIN_VALUE
            || mTotalRunTime - lastFoundationScanTick >= FOUNDATION_SCAN_INTERVAL);
    }

    private void refreshFoundationInputs(List<ItemStack> inputs) {
        foundationInputs.clear();
        for (ItemStack input : inputs) {
            if (input == null || input.stackSize <= 0) continue;
            ItemStack copy = input.copy();
            copy.stackSize = 1;
            foundationInputs.add(copy);
        }
        foundationMatches.clear();
        lastFoundationScanTick = mTotalRunTime;
        activeFoundationsChecked = false;
        markBreedingChanged();
    }

    public ItemStack findFoundation(FoundationRequirement requirement) {
        String key = requirement.key();
        if (!foundationMatches.containsKey(key)) {
            ItemStack match = null;
            for (ItemStack input : foundationInputs) {
                if (requirement.matches(input)) {
                    match = input;
                    break;
                }
            }
            foundationMatches.put(key, match);
        }
        return foundationMatches.get(key);
    }

    private void updateMissingFoundations(List<BeeBreedingHelper.BreedingStep> steps) {
        Map<String, FoundationRequirement> missing = new LinkedHashMap<>();
        if (BeeBreederConfig.requireMutationFoundations) {
            for (BeeBreedingHelper.BreedingStep step : steps) {
                for (FoundationRequirement requirement : step.foundations) {
                    if (findFoundation(requirement) == null) missing.putIfAbsent(requirement.key(), requirement);
                }
            }
        }
        List<FoundationRequirement> next = new ArrayList<>(missing.values());
        if (!missingFoundations.equals(next)) {
            missingFoundations = next;
            markBreedingChanged();
        }
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        if (side == aFacing) {
            if (aActive) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()) };
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        for (ParameterModuleHatch module : parameterModules) {
            module.setWorking(false);
        }
        clearCraftingIcons(parameterModules);
        parameterModules.clear();
    }

    @Override
    public boolean addToMachineList(IGregTechTileEntity tile, int casingIndex) {
        return addParameterModuleToMachineList(tile, casingIndex) || super.addToMachineList(tile, casingIndex);
    }

    public boolean addParameterModuleToMachineList(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ParameterModuleHatch module)) return false;
        if (!parameterModules.contains(module)) {
            module.updateTexture(casingIndex);
            module.updateCraftingIcon(getMachineCraftingIcon(), getBaseMetaTileEntity());
            parameterModules.add(module);
        }
        return true;
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return 1;
    }

    private ParameterModuleHatch parameterModule(MachineModuleType type) {
        for (ParameterModuleHatch module : parameterModules) if (module.getModuleType() == type) return module;
        return null;
    }

    public long getMaxInputPower() {
        long eut = 0;
        for (MTEHatch hatch : GTUtility.validMTEList(mEnergyHatches)) {
            IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
            eut += tile.getInputVoltage() * tile.getInputAmperage();
        }
        for (MTEHatch hatch : GTUtility.validMTEList(mExoticEnergyHatches)) {
            IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
            eut += tile.getInputVoltage() * tile.getInputAmperage();
        }
        return eut;
    }

    public int getCasingTextureID() {
        return CASING_TEXTURE_ID;
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.large_bee_breeder.name";
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.machine.large_bee_breeder.recipe_type
        // # Bee Breeder
        // # zh_CN 养蜂机
        return StatCollector.translateToLocal("gtnr.machine.large_bee_breeder.recipe_type");
    }

    @Override
    public String getTooltipPath() {
        return "large-bee-breeder";
    }

    @Override
    public TooltipStats getTooltipStats() {
        return new TooltipStats(BASE_PARALLEL_STEPS, Double.NaN, Double.NaN, false);
    }

    @Override
    public TierRange getTooltipTierRange(TieredPart part) {
        return part == TieredPart.VOLTAGE ? new TierRange(BASE_VOLTAGE_TIER, part.maxTier)
            : MultiblockTooltipSource.super.getTooltipTierRange(part);
    }

    @Override
    public TooltipStats getTooltipStatsAtTier(TieredPart part, int tier) {
        if (part == TieredPart.VOLTAGE) {
            return new TooltipStats(getParallelForVoltageTier(tier), Double.NaN, Double.NaN, false);
        }
        return getTooltipStats();
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("attemptSeconds", TICKS_PER_BREEDING / 20D);
        values.put("baseEu", BASE_EUT_PER_BREEDING);
        values.put("glassChance", CHANCE_PER_GLASS_TIER);
        return values;
    }

    public List<FoundationRequirement> getMissingFoundations() {
        return Collections.unmodifiableList(missingFoundations);
    }

    @Override
    public String[] getInfoData() {
        String[] superInfo = super.getInfoData();
        String[] info = new String[superInfo.length + 5];
        System.arraycopy(superInfo, 0, info, 0, superInfo.length);

        info[superInfo.length] = StatCollector.translateToLocalFormatted(

        // @formatter:off
            // #tr gtnr.bee_breeder.current_target
            // # Target: %s
            // # zh_CN 当前目标：%s
            // @formatter:on
            "gtnr.bee_breeder.current_target",

            // @formatter:off
// #tr gtnr.bee_breeder.unset
// # Not selected
// # zh_CN 未选择
// @formatter:on
            targetBeeSpecies.isEmpty() ? StatCollector.translateToLocal("gtnr.bee_breeder.unset") : targetBeeSpecies);
        info[superInfo.length + 1] = StatCollector.translateToLocalFormatted(

        // @formatter:off
            // #tr gtnr.bee_breeder.archive_size
            // # Archived species: %s
            // # zh_CN 已解锁蜂种：%s
            // @formatter:on
            "gtnr.bee_breeder.archive_size",
            dronePool.getAvailableSpecies()
                .size());
        info[superInfo.length + 2] = StatCollector

        // @formatter:off
            // #tr gtnr.bee_breeder.chain_progress
            // # Route progress: %s/%s
            // # zh_CN 路线进度：%s/%s
            // @formatter:on
            .translateToLocalFormatted("gtnr.bee_breeder.chain_progress", countCompletedSteps(), breedingChain.size());
        info[superInfo.length + 3] = StatCollector

        // @formatter:off
            // #tr gtnr.bee_breeder.pending
            // # Princesses waiting: %s
            // # zh_CN 待转换公主蜂：%s
            // @formatter:on
            .translateToLocalFormatted("gtnr.bee_breeder.pending", pendingPrincessOutputs);
        info[superInfo.length + 4] = StatCollector.translateToLocalFormatted(

        // @formatter:off
            // #tr gtnr.bee_breeder.energy
            // # Per attempt: %s EU/t | Chance bonus: +%s%%
            // # zh_CN 每次尝试：%s EU/t | 成功率加成：+%s%%
            // @formatter:on
            "gtnr.bee_breeder.energy",
            NumberFormatUtil.formatNumber(getEnergyCostPerTick(1)),
            getMutationChanceBonus());

        return info;
    }

    public List<BeeBreedingHelper.BreedingStep> getBreedingSteps() {
        return Collections.unmodifiableList(breedingChain);
    }

    public List<BeeBreedingHelper.BreedingStep> getActiveBreedingSteps() {
        if (activeOperation != OPERATION_BREEDING) return Collections.emptyList();
        List<BeeBreedingHelper.BreedingStep> steps = new ArrayList<>(activeBreedingBatch.size());
        for (BreedingAttempt attempt : activeBreedingBatch) {
            steps.add(
                new BeeBreedingHelper.BreedingStep(
                    attempt.step.parent1,
                    attempt.step.parent2,
                    attempt.step.result,
                    attempt.effectiveChance,
                    attempt.step.foundations));
        }
        return steps;
    }

    private void updateChainDisplayInfo() {
        chainTotalSteps = breedingChain.size();
        chainCompletedSteps = countCompletedSteps();
    }

    private int countCompletedSteps() {
        int count = 0;
        for (BeeBreedingHelper.BreedingStep step : breedingChain) {
            if (dronePool.hasDrone(step.result)) count++;
        }
        return count;
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<LargeBeeBreeder> getGui() {
        return new LargeBeeBreederGui(this);
    }

    @Override
    public void onValueUpdate(byte value) {}

    @Override
    public byte getUpdateData() {
        return 0;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);

        aNBT.setString("targetBeeSpecies", targetBeeSpecies != null ? targetBeeSpecies : "");
        aNBT.setInteger("pendingPrincessOutputs", pendingPrincessOutputs);
        aNBT.setTag("dronePool", dronePool.toNBT());
        aNBT.setByte("beeBreederActiveOperation", activeOperation);
        NBTTagList attempts = new NBTTagList();
        for (BreedingAttempt attempt : activeBreedingBatch) {
            attempts.appendTag(attempt.toNBT());
        }
        aNBT.setTag("beeBreederActiveBatch", attempts);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);

        targetBeeSpecies = aNBT.getString("targetBeeSpecies");
        pendingPrincessOutputs = aNBT.getInteger("pendingPrincessOutputs");
        foundationInputs.clear();
        foundationMatches.clear();
        lastFoundationScanTick = Long.MIN_VALUE;
        missingFoundations = Collections.emptyList();
        activeFoundationsChecked = false;

        if (aNBT.hasKey("dronePool")) {
            dronePool = DronePool.fromNBT(aNBT.getCompoundTag("dronePool"));
        } else {
            dronePool = new DronePool();
        }

        activeOperation = aNBT.getByte("beeBreederActiveOperation");
        activeBreedingBatch = new ArrayList<>();
        NBTTagList attempts = aNBT.getTagList("beeBreederActiveBatch", 10);
        for (int i = 0; i < attempts.tagCount(); i++) {
            BreedingAttempt attempt = BreedingAttempt.fromNBT(attempts.getCompoundTagAt(i));
            if (attempt != null) activeBreedingBatch.add(attempt);
        }
        if (activeOperation != OPERATION_BREEDING) activeBreedingBatch.clear();
        updateActiveVoltageTier();

        breedingPlan = BeeBreedingPlanner.Plan.empty(targetBeeSpecies);
        breedingPlanDirty = true;
        breedingChain.clear();
        markBreedingChanged();
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        aNBT.setString("targetBeeSpecies", targetBeeSpecies != null ? targetBeeSpecies : "");
        aNBT.setInteger("pendingPrincessOutputs", pendingPrincessOutputs);
        aNBT.setTag("dronePool", dronePool.toNBT());
        super.setItemNBT(aNBT);
    }

    @Override
    public int getMaxParallelRecipes() {
        return getMaxParallelBreedingSteps();
    }

    public int getBreedingVoltageTier() {
        return checkEnergyHatchTier();
    }

    public int getMaxParallelBreedingSteps() {
        ParameterModuleHatch module = parameterModule(MachineModuleType.Parallel);
        long multiplier = module == null ? 1 : 1L + module.getParameter();
        return (int) Math.min(Integer.MAX_VALUE, getParallelForVoltageTier(getBreedingVoltageTier()) * multiplier);
    }

    private int getGlassUpgrades() {
        return Math.max(0, Math.min(mGlassTier, VoltageIndex.MAX) - BASE_GLASS_TIER);
    }

    public int getMutationChanceBonus() {
        return getGlassUpgrades() * CHANCE_PER_GLASS_TIER;
    }

    @Override
    public boolean doesBindPlayerInventory() {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    private void markBaseTileDirty() {
        IGregTechTileEntity baseTile = getBaseMetaTileEntity();
        if (baseTile != null) baseTile.markDirty();
    }

    private void markBreedingChanged() {
        breedingRevision++;
    }

    private static final class BreedingAttempt {

        private final BeeBreedingHelper.BreedingStep step;
        private final double effectiveChance;

        private BreedingAttempt(BeeBreedingHelper.BreedingStep step, double effectiveChance) {
            this.step = step;
            this.effectiveChance = effectiveChance;
        }

        private NBTTagCompound toNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("parent1", step.parent1);
            tag.setString("parent2", step.parent2);
            tag.setString("result", step.result);
            tag.setDouble("baseChance", step.chance);
            tag.setDouble("effectiveChance", effectiveChance);
            return tag;
        }

        private static BreedingAttempt fromNBT(NBTTagCompound tag) {
            if (tag == null) return null;
            String parent1 = tag.getString("parent1");
            String parent2 = tag.getString("parent2");
            String result = tag.getString("result");
            if (parent1.isEmpty() || parent2.isEmpty() || result.isEmpty()) return null;
            BeeBreedingHelper.BreedingStep step = BeeBreedingHelper
                .findRegisteredStep(parent1, parent2, result, tag.getDouble("baseChance"));
            if (step == null) return null;
            return new BreedingAttempt(step, tag.getDouble("effectiveChance"));
        }
    }

}
