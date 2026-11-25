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
import java.util.Arrays;
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
import net.minecraft.entity.player.EntityPlayer;
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
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingHelper.BreedingStep;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingHelper.FoundationRequirement;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingHelper.MutationData;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingInputs;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingInputs.ParentPair;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingPlanner;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeBreedingTiers;
import com.xyp.gtneverreset.common.machine.multiblock.bee.BeeSeedBank;
import com.xyp.gtneverreset.common.machine.multiblock.bee.DronePool;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.config.BeeBreederConfig;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import forestry.api.apiculture.EnumBeeType;
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
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import lombok.Getter;

@IMetaTileEntity.SkipGenerateDescription

public class LargeBeeBreeder extends MTEExtendedPowerMultiBlockBase<LargeBeeBreeder>
    implements ISurvivalConstructable, MultiblockTooltipSource {

    public static final int CASING_TEXTURE_ID = (126 << 7) | 127;
    public static final int MODE_AUTOMATIC = 0;
    public static final int MODE_DRONE = 1;
    public static final int MODE_PRINCESS = 2;
    public static final int SEED_CYCLE_TICKS = 128;

    private static final long BASE_EUT_PER_BREEDING = 512L;
    private static final int BASE_VOLTAGE_TIER = VoltageIndex.HV;
    private static final int BASE_GLASS_TIER = VoltageIndex.HV;
    private static final int CHANCE_PER_GLASS_TIER = 2;
    private static final double FEMALE_RETENTION_CHANCE = 1D;
    private static final double DRONE_CONSUMPTION_CHANCE = 0.1D;

    private static final int TICKS_PER_BREEDING = 32 * 20;
    private static final int INPUT_SCAN_INTERVAL = 100;
    private static final int FOUNDATION_SCAN_INTERVAL = 200;
    private static final int BASE_PARALLEL_STEPS = 1;
    private static final int ROYAL_JELLY_PER_PARALLEL = 2;

    private static final byte OPERATION_NONE = 0;
    private static final byte OPERATION_BREEDING = 1;
    private static final byte OPERATION_OUTPUT = 2;
    private static final byte OPERATION_REFUND = 3;
    private static final byte OPERATION_SEED_PREPARATION = 4;
    private static final byte OPERATION_SEED_OUTPUT = 5;

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
    private BeeSeedBank seedBank = new BeeSeedBank();
    private final List<ItemStack> pendingBeeRefunds = new ArrayList<>();
    private byte activeOperation = OPERATION_NONE;
    private List<BreedingAttempt> activeBreedingBatch = new ArrayList<>();
    private ItemStack activeSeed;
    @Getter
    private int pendingPrincessOutputs = 0;

    private final List<ParameterModuleHatch> parameterModules = new ArrayList<>();
    private IStructureDefinition<LargeBeeBreeder> structureDefinition;
    private int mCountCasing;
    private int mGlassTier = -1;
    private BeeBreedingPlanner.Plan breedingPlan = BeeBreedingPlanner.Plan.empty("");
    private List<BeeBreedingHelper.BreedingStep> breedingChain = new ArrayList<>();
    private boolean breedingPlanDirty = true;
    @Getter
    private int availableFemaleCount;
    private Set<String> availableFemaleSpecies = Collections.emptySet();
    private Set<String> availableDroneSpecies = Collections.emptySet();
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
                    scanInputBuses(getStoredInputs());
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
        List<ItemStack> inputs = getStoredInputs();
        BeeBreedingInputs parents = scanInputBuses(inputs);
        refreshBreedingPlan();
        if (!pendingBeeRefunds.isEmpty()) {
            ItemStack[] output = { pendingBeeRefunds.get(0)
                .copy() };
            if (!hasOutputSpace(output)) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            activeOperation = OPERATION_REFUND;
            activeRequiredVoltageTier = VoltageIndex.ULV;
            mOutputItems = output;
            mMaxProgresstime = 1;
            mEfficiency = 10000;
            mEfficiencyIncrease = 10000;
            lEUt = 0;
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
        if (targetBeeSpecies.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        if (getBreedingVoltageTier() < BASE_VOLTAGE_TIER) return insufficientBreedingVoltage(BASE_VOLTAGE_TIER);

        if (seedBank.hasSpecies(targetBeeSpecies)) {
            if (pendingPrincessOutputs > 0) return prepareLegacyOutput(inputs);
            EnumBeeType type = getSeedOutputType();
            ItemStack seed = type == EnumBeeType.DRONE ? BeeBreedingHelper.createDrone(targetBeeSpecies)
                : BeeBreedingHelper.createPrincess(targetBeeSpecies);
            return seed == null ? unreachableTarget() : prepareSeedCycle(seed, false);
        }
        if (machineMode != MODE_AUTOMATIC) {
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_missing_seed
            // # The selected species has not been stored in the seed bank.
            // # zh_CN 所选蜂种尚未存入种库。
            return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_seed");
        }

        return prepareMutationBatch(inputs, parents);
    }

    private CheckRecipeResult prepareLegacyOutput(List<ItemStack> inputs) {
        int tier = getSpeciesVoltageTier(targetBeeSpecies);
        if (getBreedingVoltageTier() < tier) return insufficientBreedingVoltage(tier);
        long eut = getEuPerBreedingAttempt();
        if (getMaxInputPower() < eut) return CheckRecipeResultRegistry.insufficientPower(eut);
        ItemStack princess = BeeBreedingHelper.createPrincess(targetBeeSpecies);
        if (princess == null) return unreachableTarget();
        ItemStack drone = BeeBreedingHelper.createDrone(targetBeeSpecies);
        ItemStack[] output = drone == null ? new ItemStack[] { princess } : new ItemStack[] { princess, drone };
        if (!hasOutputSpace(output)) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
        mOutputItems = output;
        activeOperation = OPERATION_OUTPUT;
        prepareBreedingCycle(1, inputs);
        markBreedingChanged();
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private CheckRecipeResult prepareMutationBatch(List<ItemStack> inputs, BeeBreedingInputs parents) {
        List<BreedingStep> readySteps = breedingPlan.getReadySteps(seedBank.getSpecies(), Integer.MAX_VALUE);
        if (readySteps.isEmpty()) {
            if (breedingPlan.getMissingSpecies()
                .isEmpty()) return unreachableTarget();
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_missing_drone
            // # Species archive is missing a required parent!
            // # zh_CN 物种档案中缺少必要亲本！
            return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_drone");
        }

        int voltageTier = getBreedingVoltageTier();
        int requiredTier = readySteps.stream()
            .mapToInt(step -> getSpeciesVoltageTier(step.result))
            .min()
            .orElse(BASE_VOLTAGE_TIER);
        if (voltageTier < requiredTier) return insufficientBreedingVoltage(requiredTier);
        long eut = getEuPerBreedingAttempt();
        long power = getMaxInputPower();
        if (power < eut) return CheckRecipeResultRegistry.insufficientPower(eut);

        int limit = (int) Math.min(getMaxParallelBreedingSteps(), power / eut);
        List<BreedingAttempt> batch = new ArrayList<>();
        List<BreedingStep> blockedFoundations = new ArrayList<>();
        ItemStack[] outputs = null;
        boolean outputFull = false;
        ItemStack missingParent = null;
        for (BreedingStep planned : readySteps) {
            if (batch.size() >= limit) break;
            if (voltageTier < getSpeciesVoltageTier(planned.result)) continue;
            BreedingStep step = selectParentStep(planned);
            if (!hasFoundations(step)) {
                blockedFoundations.add(step);
                continue;
            }

            ParentPair pair = parents.findPair(step.parent1, step.parent2);
            if (pair == null) {
                if (missingParent == null) missingParent = createMissingParent(step, parents);
                continue;
            }

            boolean internalOffspring = !step.result.equals(targetBeeSpecies);
            ItemStack offspring = BeeBreedingHelper.createPrincess(step.result);
            if (offspring == null) continue;
            if (!internalOffspring && !hasOutputSpace(new ItemStack[] { offspring })) {
                outputFull = true;
                continue;
            }

            double chance = Math.min(100D, step.chance + getMutationChanceBonus());
            boolean succeeded = BeeBreedingHelper.tryMutation(chance);
            if (!succeeded) offspring = null;
            boolean consumeFemale = succeeded && !BeeBreedingHelper.tryMutation(FEMALE_RETENTION_CHANCE);
            boolean consumeDrone = succeeded && BeeBreedingHelper.tryMutation(DRONE_CONSUMPTION_CHANCE);
            batch.add(
                new BreedingAttempt(
                    step,
                    chance,
                    pair.femaleSpecies(),
                    offspring,
                    internalOffspring,
                    consumeFemale ? singleBee(pair.female()) : null,
                    consumeFemale && seedBank.ownsInput(pair.female()),
                    consumeDrone ? singleBee(pair.drone()) : null,
                    consumeDrone && seedBank.ownsInput(pair.drone()),
                    false));
            parents.reserve(pair, consumeFemale, consumeDrone);
            if (offspring != null && !internalOffspring) outputs = new ItemStack[] { offspring.copy() };
        }

        if (batch.isEmpty()) {
            if (outputFull) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            if (missingParent != null) return prepareSeedCycle(missingParent, true);
            updateMissingFoundations(blockedFoundations);
            if (!missingFoundations.isEmpty()) {
                // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_missing_foundation
                // # Missing mutation foundation. Waiting for input.
                // # zh_CN 缺少杂交基石，等待补充。
                return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_foundation");
            }
            return unreachableTarget();
        }

        parents.commit();
        seedBank.removeEmptyInputs();
        updateAvailableParents(parents);
        updateSlots();
        mOutputItems = outputs;
        activeOperation = OPERATION_BREEDING;
        activeBreedingBatch = batch;
        activeFoundationsChecked = true;
        allTasksBlocked = false;
        missingDroneSpecies = "";
        updateMissingFoundations(getActiveBreedingSteps());
        prepareBreedingCycle(batch.size(), inputs);
        markBaseTileDirty();
        markBreedingChanged();
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private BreedingStep selectParentStep(BreedingStep planned) {
        BreedingStep selected = planned;
        boolean selectedReady = hasFoundations(planned);
        for (MutationData mutation : BeeBreedingHelper.getMutationsForUID(planned.result)) {
            if (mutation.result.equals(mutation.parent1) || mutation.result.equals(mutation.parent2)
                || !seedBank.hasSpecies(mutation.parent1)
                || !seedBank.hasSpecies(mutation.parent2)
                || selectedReady && mutation.chance <= selected.chance) continue;
            BreedingStep step = new BreedingStep(
                mutation.parent1,
                mutation.parent2,
                mutation.result,
                mutation.chance,
                mutation.foundations);
            if (hasFoundations(step)) {
                selected = step;
                selectedReady = true;
            }
        }
        return selected;
    }

    private boolean hasFoundations(BreedingStep step) {
        if (!BeeBreederConfig.requireMutationFoundations) return true;
        for (FoundationRequirement foundation : step.foundations) if (findFoundation(foundation) == null) return false;
        return true;
    }

    private ItemStack createMissingParent(BreedingStep step, BeeBreedingInputs parents) {
        if (parents.hasFemale(step.parent1)) return seedBank.createParent(step.parent2, EnumBeeType.DRONE);
        if (parents.hasFemale(step.parent2)) return seedBank.createParent(step.parent1, EnumBeeType.DRONE);
        if (parents.hasDrone(step.parent1)) return seedBank.createParent(step.parent2, EnumBeeType.PRINCESS);
        return seedBank.createParent(step.parent1, EnumBeeType.PRINCESS);
    }

    private CheckRecipeResult prepareSeedCycle(ItemStack seed, boolean internal) {
        int tier = getSpeciesVoltageTier(BeeBreedingHelper.getBeeUID(seed));
        if (getBreedingVoltageTier() < tier) return insufficientBreedingVoltage(tier);
        long eut = getSeedEnergyCostPerTick();
        if (getMaxInputPower() < eut) return CheckRecipeResultRegistry.insufficientPower(eut);
        ItemStack[] outputs = internal ? null : new ItemStack[] { seed.copy() };
        if (outputs != null && !hasOutputSpace(outputs)) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;

        activeSeed = seed;
        activeOperation = internal ? OPERATION_SEED_PREPARATION : OPERATION_SEED_OUTPUT;
        activeRequiredVoltageTier = tier;
        mOutputItems = outputs;
        mMaxProgresstime = SEED_CYCLE_TICKS;
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        lEUt = -eut;
        markBaseTileDirty();
        markBreedingChanged();
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private boolean hasOutputSpace(ItemStack[] outputs) {
        return new ItemEjectionHelper(this).ejectItems(Arrays.asList(outputs), 1) == 1;
    }

    private CheckRecipeResult unreachableTarget() {
        allTasksBlocked = true;
        missingDroneSpecies = targetBeeSpecies;
        markBreedingChanged();
        // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_unreachable_target
        // # Current species archive cannot reach the target species!
        // # zh_CN 当前物种档案无法到达目标品种！
        return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_unreachable_target");
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
        if (activeOperation == OPERATION_REFUND) {
            activeRequiredVoltageTier = VoltageIndex.ULV;
        } else if (activeSeed != null) {
            activeRequiredVoltageTier = getSpeciesVoltageTier(getSeedOutputSpecies());
        } else if (activeOperation == OPERATION_OUTPUT) {
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
        if (mOutputItems != null && mProgresstime + 1 >= mMaxProgresstime && !hasOutputSpace(mOutputItems)) {
            checkRecipeResult = CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            return false;
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
            if (pendingPrincessOutputs > 0) pendingPrincessOutputs--;
        } else if (activeOperation == OPERATION_REFUND) {
            if (!pendingBeeRefunds.isEmpty()) pendingBeeRefunds.remove(0);
        } else if (activeOperation == OPERATION_SEED_PREPARATION) {
            seedBank.addBreedingInput(activeSeed);
        } else if (activeOperation == OPERATION_BREEDING) {
            for (BreedingAttempt attempt : activeBreedingBatch) {
                String result = attempt.step.result;
                ItemStack offspring = attempt.offspring;
                if (attempt.legacy && !seedBank.hasSpecies(result)
                    && BeeBreedingHelper.tryMutation(attempt.effectiveChance)) {
                    offspring = BeeBreedingHelper.createPrincess(result);
                }
                if (offspring == null) continue;
                archiveChanged |= retainSeed(result, offspring);
                if (attempt.internalOffspring) seedBank.addBreedingInput(offspring);
            }
        }

        clearActiveOperation();
        if (archiveChanged) invalidateBreedingPlan();
        markBreedingChanged();
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        if (activeOperation != OPERATION_NONE) {
            for (BreedingAttempt attempt : activeBreedingBatch) {
                refundParent(attempt.consumedFemale, attempt.femaleInternal);
                refundParent(attempt.consumedDrone, attempt.droneInternal);
            }
            clearActiveOperation();
            markBreedingChanged();
            markBaseTileDirty();
        }
        super.stopMachine(reason);
    }

    private void refundParent(ItemStack bee, boolean internal) {
        if (bee == null) return;
        if (internal) seedBank.addBreedingInput(bee);
        else pendingBeeRefunds.add(bee);
    }

    private void clearActiveOperation() {
        activeOperation = OPERATION_NONE;
        activeSeed = null;
        activeBreedingBatch.clear();
        activeFoundationsChecked = false;
        activeRequiredVoltageTier = BASE_VOLTAGE_TIER;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (player.worldObj.isRemote) return;
        setMachineMode(nextMachineMode());
        GTUtility.sendChatTrans(player, getMachineModeKey());
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
                    scheduleRecipeCheckImmediate();
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
            scheduleRecipeCheckImmediate();
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
                breedingPlan = BeeBreedingPlanner.plan(targetBeeSpecies, seedBank.getSpecies());
                breedingChain = new ArrayList<>(breedingPlan.getSteps());
                breedingPlanDirty = false;
                markBreedingChanged();
            }

            boolean targetUnlocked = seedBank.hasSpecies(targetBeeSpecies);
            boolean hasReadyStep = !breedingPlan.getReadySteps(seedBank.getSpecies(), 1)
                .isEmpty();
            allTasksBlocked = !targetUnlocked && (machineMode != MODE_AUTOMATIC || !hasReadyStep);
            missingDroneSpecies = allTasksBlocked ? breedingPlan.getFirstMissingSpecies() : "";
            if (allTasksBlocked && missingDroneSpecies.isEmpty()) {
                missingDroneSpecies = targetBeeSpecies;
            }
        }

        if (activeOperation == OPERATION_BREEDING) {
            updateMissingFoundations(getActiveBreedingSteps());
        } else if (machineMode == MODE_AUTOMATIC && !seedBank.hasSpecies(targetBeeSpecies)) {
            updateMissingFoundations(
                breedingPlan.getReadySteps(seedBank.getSpecies(), getMaxParallelBreedingSteps())
                    .stream()
                    .map(this::selectParentStep)
                    .toList());
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

    private BeeBreedingInputs scanInputBuses(List<ItemStack> inputs) {
        BeeBreedingInputs parents = new BeeBreedingInputs(inputs, getStockedInputSamples());
        boolean archiveChanged = false;
        for (Map.Entry<String, ItemStack> specimen : parents.getSpecimens()
            .entrySet()) {
            archiveChanged |= retainSeed(specimen.getKey(), specimen.getValue());
        }
        seedBank.indexInputs(parents);
        updateAvailableParents(parents);
        if (archiveChanged) {
            invalidateBreedingPlan();
            markBaseTileDirty();
            scheduleRecipeCheckImmediate();
        }
        if (isFoundationScanDue()) refreshFoundationInputs(inputs);
        return parents;
    }

    private Set<ItemStack> getStockedInputSamples() {
        Set<ItemStack> stocked = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var hatch : GTUtility.validMTEList(mInputBusses)) {
            if (!(hatch instanceof MTEHatchInputBusME bus)) continue;
            // Stocked stacks are snapshots until AE finishes withdrawing them. Only refundable inventory can be
            // reserved.
            for (int i = 0; i < MTEHatchInputBusME.SLOT_COUNT; i++) {
                ItemStack stack = bus.getStackInSlot(i);
                if (stack != null) stocked.add(stack);
            }
        }
        return stocked;
    }

    private boolean retainSeed(String species, ItemStack sample) {
        if (!seedBank.retain(species, sample)) return false;
        dronePool.unlockSpecies(species);
        return true;
    }

    private void updateAvailableParents(BeeBreedingInputs parents) {
        int females = parents.getFemaleCount();
        Set<String> femaleSpecies = parents.getFemaleSpecies();
        Set<String> droneSpecies = parents.getDroneSpecies();
        if (females == availableFemaleCount && availableFemaleSpecies.equals(femaleSpecies)
            && availableDroneSpecies.equals(droneSpecies)) return;
        availableFemaleCount = females;
        availableFemaleSpecies = femaleSpecies;
        availableDroneSpecies = droneSpecies;
        markBreedingChanged();
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
        return type == MachineModuleType.Thread ? 0 : 1;
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
        values.put("seedSeconds", SEED_CYCLE_TICKS / 20D);
        values.put("femaleRetentionChance", FEMALE_RETENTION_CHANCE);
        values.put("droneConsumptionChance", DRONE_CONSUMPTION_CHANCE);
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

    public boolean hasBreedingParents(BreedingStep step) {
        return seedBank.hasSpecies(step.parent1) && seedBank.hasSpecies(step.parent2);
    }

    public String getFemaleParentSpecies(BreedingStep step) {
        for (BreedingAttempt attempt : activeBreedingBatch) {
            if (attempt.step.result.equals(step.result) && !attempt.femaleSpecies.isEmpty())
                return attempt.femaleSpecies;
        }
        if (availableFemaleSpecies.contains(step.parent1)) return step.parent1;
        if (availableFemaleSpecies.contains(step.parent2) || availableDroneSpecies.contains(step.parent1))
            return step.parent2;
        return step.parent1;
    }

    public boolean isSeedPreparation() {
        return activeOperation == OPERATION_SEED_PREPARATION;
    }

    public boolean isSeedOutputCycle() {
        if (activeOperation != OPERATION_NONE) return activeOperation == OPERATION_SEED_OUTPUT;
        return !targetBeeSpecies.isEmpty()
            && (machineMode != MODE_AUTOMATIC || pendingPrincessOutputs == 0 && seedBank.hasSpecies(targetBeeSpecies));
    }

    public String getSeedOutputSpecies() {
        if (activeSeed == null) return targetBeeSpecies;
        String species = BeeBreedingHelper.getBeeUID(activeSeed);
        return species == null ? "" : species;
    }

    public EnumBeeType getSeedOutputType() {
        if (activeSeed != null) return BeeBreedingHelper.getBeeRoot()
            .getType(activeSeed);
        return machineMode == MODE_DRONE ? EnumBeeType.DRONE : EnumBeeType.PRINCESS;
    }

    public long getSeedEnergyCostPerTick() {
        return BASE_EUT_PER_BREEDING << getGlassUpgrades();
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
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        writeBeeState(tag, false);
        tag.setByte("beeBreederActiveOperation", activeOperation);
        NBTTagList attempts = new NBTTagList();
        for (BreedingAttempt attempt : activeBreedingBatch) attempts.appendTag(attempt.toNBT());
        tag.setTag("beeBreederActiveBatch", attempts);
        if (activeSeed != null) tag.setTag("beeActiveSeed", activeSeed.writeToNBT(new NBTTagCompound()));
        else tag.removeTag("beeActiveSeed");
    }

    private void writeBeeState(NBTTagCompound tag, boolean controllerItem) {
        tag.setString("targetBeeSpecies", targetBeeSpecies);
        tag.setInteger("pendingPrincessOutputs", pendingPrincessOutputs);
        tag.setTag("dronePool", dronePool.toNBT());
        tag.setInteger("beeSeedMode", machineMode);
        NBTTagCompound bankTag = seedBank.toNBT();
        NBTTagList refunds = new NBTTagList();
        for (ItemStack refund : pendingBeeRefunds) refunds.appendTag(refund.writeToNBT(new NBTTagCompound()));
        if (controllerItem) {
            NBTTagList internal = bankTag.getTagList("inputs", 10);
            for (BreedingAttempt attempt : activeBreedingBatch) {
                appendParentRefund(attempt.consumedFemale, attempt.femaleInternal ? internal : refunds);
                appendParentRefund(attempt.consumedDrone, attempt.droneInternal ? internal : refunds);
            }
            bankTag.setTag("inputs", internal);
        }
        tag.setTag("beeSeedBank", bankTag);
        tag.setTag("beePendingParentRefunds", refunds);
    }

    private static void appendParentRefund(ItemStack parent, NBTTagList refunds) {
        if (parent != null) refunds.appendTag(parent.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);

        targetBeeSpecies = tag.getString("targetBeeSpecies");
        pendingPrincessOutputs = Math.max(0, tag.getInteger("pendingPrincessOutputs"));
        machineMode = tag.getInteger("beeSeedMode");
        if (machineMode < MODE_AUTOMATIC || machineMode > MODE_PRINCESS) machineMode = MODE_AUTOMATIC;
        availableFemaleCount = 0;
        availableFemaleSpecies = Collections.emptySet();
        availableDroneSpecies = Collections.emptySet();
        foundationInputs.clear();
        foundationMatches.clear();
        lastFoundationScanTick = Long.MIN_VALUE;
        missingFoundations = Collections.emptyList();
        activeFoundationsChecked = false;

        dronePool = DronePool.fromNBT(tag.getCompoundTag("dronePool"));
        seedBank = BeeSeedBank.fromNBT(tag.getCompoundTag("beeSeedBank"));
        if (!tag.hasKey("beeSeedBank", 10)) {
            for (String species : dronePool.getAvailableSpecies()) {
                seedBank.retain(species, BeeBreedingHelper.createPrincess(species));
            }
        }
        for (String species : seedBank.getSpecies()) dronePool.unlockSpecies(species);

        pendingBeeRefunds.clear();
        NBTTagList refunds = tag.getTagList("beePendingParentRefunds", 10);
        for (int i = 0; i < refunds.tagCount(); i++) {
            ItemStack refund = ItemStack.loadItemStackFromNBT(refunds.getCompoundTagAt(i));
            if (refund != null && refund.stackSize > 0) pendingBeeRefunds.add(refund);
        }

        activeOperation = tag.getByte("beeBreederActiveOperation");
        activeSeed = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("beeActiveSeed"));
        activeBreedingBatch.clear();
        if (activeOperation == OPERATION_BREEDING) {
            NBTTagList attempts = tag.getTagList("beeBreederActiveBatch", 10);
            for (int i = 0; i < attempts.tagCount(); i++) {
                BreedingAttempt attempt = BreedingAttempt.fromNBT(attempts.getCompoundTagAt(i));
                if (attempt != null) activeBreedingBatch.add(attempt);
            }
        }
        validateLoadedOperation();

        breedingPlan = BeeBreedingPlanner.Plan.empty(targetBeeSpecies);
        breedingPlanDirty = true;
        breedingChain.clear();
        markBreedingChanged();
    }

    private void validateLoadedOperation() {
        if (activeOperation == OPERATION_NONE) return;
        boolean valid = mMaxProgresstime > 0 && switch (activeOperation) {
            case OPERATION_BREEDING -> !activeBreedingBatch.isEmpty() && activeBreedingBatch.stream()
                .allMatch(BreedingAttempt::isRegistered);
            case OPERATION_OUTPUT -> pendingPrincessOutputs > 0 && mOutputItems != null;
            case OPERATION_REFUND -> !pendingBeeRefunds.isEmpty() && mOutputItems != null;
            case OPERATION_SEED_PREPARATION, OPERATION_SEED_OUTPUT -> BeeBreedingHelper.isBee(activeSeed)
                && (activeOperation == OPERATION_SEED_PREPARATION || mOutputItems != null);
            default -> false;
        };
        if (valid) {
            updateActiveVoltageTier();
            return;
        }
        for (BreedingAttempt attempt : activeBreedingBatch) {
            refundParent(attempt.consumedFemale, attempt.femaleInternal);
            refundParent(attempt.consumedDrone, attempt.droneInternal);
        }
        clearActiveOperation();
        mOutputItems = null;
        mMaxProgresstime = 0;
        mProgresstime = 0;
        lEUt = 0;
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        writeBeeState(tag, true);
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
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return (machineMode + 1) % 3;
    }

    @Override
    public void setMachineMode(int mode) {
        if (mode < MODE_AUTOMATIC || mode > MODE_PRINCESS || mode == machineMode) return;
        super.setMachineMode(mode);
        markBaseTileDirty();
        markBreedingChanged();
    }

    @Override
    public String getMachineModeKey() {
        return switch (machineMode) {
            // #tr gtnr.machine.large_bee_breeder.mode.drone
            // # Export Drones
            // # zh_CN 导出雄蜂
            case MODE_DRONE -> "gtnr.machine.large_bee_breeder.mode.drone";
            // #tr gtnr.machine.large_bee_breeder.mode.princess
            // # Export Princesses
            // # zh_CN 导出公主蜂
            case MODE_PRINCESS -> "gtnr.machine.large_bee_breeder.mode.princess";
            // #tr gtnr.machine.large_bee_breeder.mode.automatic
            // # Automatic Breeding
            // # zh_CN 自动杂交
            default -> "gtnr.machine.large_bee_breeder.mode.automatic";
        };
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
    public boolean protectsExcessItem() {
        return true;
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

    private static ItemStack singleBee(ItemStack parent) {
        ItemStack bee = parent.copy();
        bee.stackSize = 1;
        return bee;
    }

    private record BreedingAttempt(BreedingStep step, double effectiveChance, String femaleSpecies, ItemStack offspring,
        boolean internalOffspring, ItemStack consumedFemale, boolean femaleInternal, ItemStack consumedDrone,
        boolean droneInternal, boolean legacy) {

        private boolean isRegistered() {
            return BeeBreedingHelper.findRegisteredStep(step.parent1, step.parent2, step.result, step.chance) != null;
        }

        private NBTTagCompound toNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("parent1", step.parent1);
            tag.setString("parent2", step.parent2);
            tag.setString("result", step.result);
            tag.setDouble("baseChance", step.chance);
            tag.setDouble("effectiveChance", effectiveChance);
            tag.setBoolean("legacy", legacy);
            tag.setString("femaleSpecies", femaleSpecies);
            tag.setBoolean("internalOffspring", internalOffspring);
            tag.setBoolean("femaleInternal", femaleInternal);
            tag.setBoolean("droneInternal", droneInternal);
            if (offspring != null) tag.setTag("offspring", offspring.writeToNBT(new NBTTagCompound()));
            if (consumedFemale != null) tag.setTag("consumedFemale", consumedFemale.writeToNBT(new NBTTagCompound()));
            if (consumedDrone != null) tag.setTag("consumedDrone", consumedDrone.writeToNBT(new NBTTagCompound()));
            return tag;
        }

        private static BreedingAttempt fromNBT(NBTTagCompound tag) {
            if (tag == null) return null;
            String parent1 = tag.getString("parent1");
            String parent2 = tag.getString("parent2");
            String result = tag.getString("result");
            if (parent1.isEmpty() || parent2.isEmpty() || result.isEmpty()) return null;
            double chance = tag.getDouble("baseChance");
            BreedingStep step = BeeBreedingHelper.findRegisteredStep(parent1, parent2, result, chance);
            if (step == null) step = new BreedingStep(parent1, parent2, result, chance);
            return new BreedingAttempt(
                step,
                tag.getDouble("effectiveChance"),
                tag.getString("femaleSpecies"),
                ItemStack.loadItemStackFromNBT(tag.getCompoundTag("offspring")),
                tag.getBoolean("internalOffspring"),
                ItemStack.loadItemStackFromNBT(tag.getCompoundTag("consumedFemale")),
                tag.getBoolean("femaleInternal"),
                ItemStack.loadItemStackFromNBT(tag.getCompoundTag("consumedDrone")),
                tag.getBoolean("droneInternal"),
                !tag.hasKey("legacy", 1) || tag.getBoolean("legacy"));
        }
    }

}
