package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.GregTechAPI.sBlockCasings1;
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
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.gui.modularui.LargeBeeBreederGui;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.machine.bee.BeeBreedingHelper;
import com.xyp.gtneverreset.utils.machine.bee.BeeBreedingPlanner;
import com.xyp.gtneverreset.utils.machine.bee.DronePool;
import com.xyp.gtneverreset.utils.structure.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;
import com.xyp.gtneverreset.utils.structure.StructureTooltipHooks;

import forestry.api.apiculture.IAlleleBeeSpecies;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import lombok.Getter;

@IMetaTileEntity.SkipGenerateDescription
public class LargeBeeBreeder extends MTEExtendedPowerMultiBlockBase<LargeBeeBreeder>
    implements ISurvivalConstructable, MultiblockTooltipSource {

    private static final long BASE_EUT_PER_BREEDING = 15L;
    private static final int TICKS_PER_BREEDING = 20 * 20;
    private static final byte OPERATION_NONE = 0;
    private static final byte OPERATION_BREEDING = 1;
    private static final byte OPERATION_OUTPUT = 2;
    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int HORIZONTAL_OFFSET = 7;
    private static final int VERTICAL_OFFSET = 8;
    private static final int DEPTH_OFFSET = 0;
    private static final String[][] SHAPE = MultiblockStructureLoader.load("large_bee_breeder");

    @Getter
    private String targetBeeSpecies = "";
    @Getter
    private DronePool dronePool = new DronePool();
    private byte activeOperation = OPERATION_NONE;
    private BreedingAttempt activeBreedingAttempt;
    @Getter
    private int pendingPrincessOutputs = 0;

    private int mCountCasing;
    private boolean hasStainlessSteelGear;
    private int glassTier = -1;
    private IStructureDefinition<LargeBeeBreeder> structureDefinition;
    private final List<ParameterModuleHatch> parameterModules = new ArrayList<>();
    private BeeBreedingPlanner.Plan breedingPlan = BeeBreedingPlanner.Plan.empty("");
    private List<BeeBreedingHelper.BreedingStep> breedingChain = new ArrayList<>();
    private boolean breedingPlanDirty = true;
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
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            for (ParameterModuleHatch module : parameterModules) module.setWorking(aBaseMetaTileEntity.isActive());
        }
        if (aBaseMetaTileEntity.isServerSide() && aTick % 20 == 0) {
            boolean gearBefore = hasStainlessSteelGear;
            hasStainlessSteelGear = checkStainlessSteelGear(getControllerSlot());
            if (gearBefore != hasStainlessSteelGear) markBreedingChanged();

            if (mMachine) scanInputBuses();
            refreshBreedingPlan();

        }
    }

    @Override
    public IStructureDefinition<LargeBeeBreeder> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<LargeBeeBreeder>builder()
                .addShape(STRUCTURE_PIECE_MAIN, transpose(SHAPE))
                .addElement('A', chainAllGlasses(-1, (te, t) -> te.glassTier = t, te -> te.glassTier))
                .addElement('B', ofChain(ofBlockAnyMeta(Blocks.dirt, 0), ofBlock(Blocks.grass, 0)))
                .addElement(
                    'G',
                    ofChain(
                        StructureTooltipHooks.describeAsHatch(
                            ofHatchAdder(LargeBeeBreeder::addParameterModuleToMachineList, 10, 1),
                            ParameterModuleHatch.NAME_KEY),
                        buildHatchAdder(LargeBeeBreeder.class)
                            .atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy), Maintenance)
                            .casingIndex(10)
                            .hint(1)
                            .buildAndChain(),
                        onElementPass(x -> ++x.mCountCasing, ofBlock(sBlockCasings1, 10))))
                .addElement('H', ofBlock(Blocks.planks, 0))
                .addElement('I', ofBlock(Blocks.wooden_slab, 0))
                .addElement('J', ofBlock(GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('K', ofBlock(GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('L', ofBlock(GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('N', ofBlock(GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('O', ofBlock(GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .addElement('P', ofBlock(GregTechAPI.sBlockFrames, (int) Materials.Bronze.mMetaItemSubID))
                .build();
        }
        return structureDefinition;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        this.buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
        return this.survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
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
        glassTier = -1;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) return;
        collectTooltipStructureRequirements(errors);
        hasStainlessSteelGear = checkStainlessSteelGear(getControllerSlot());

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
        Map<MachineModuleType, Integer> moduleCounts = new EnumMap<>(MachineModuleType.class);
        for (ParameterModuleHatch module : parameterModules)
            moduleCounts.merge(module.getModuleType(), 1, Integer::sum);
        for (var entry : moduleCounts.entrySet()) {
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

    private boolean checkStainlessSteelGear(ItemStack stack) {
        if (stack == null) return false;
        return stack.isItemEqual(GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.StainlessSteel, 1L));
    }

    @Nonnull
    @Override
    public CheckRecipeResult checkProcessing() {
        scanInputBuses();
        activeOperation = OPERATION_NONE;
        activeBreedingAttempt = null;

        if (targetBeeSpecies == null || targetBeeSpecies.isEmpty()) {
            refreshBreedingPlan();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        refreshBreedingPlan();
        if (pendingPrincessOutputs <= 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        if (getMaxInputVoltage() < GTValues.V[VoltageIndex.LV]) {
            return CheckRecipeResultRegistry.insufficientPower(GTValues.V[VoltageIndex.LV]);
        }

        if (dronePool.hasDrone(targetBeeSpecies)) {
            ItemStack princess = BeeBreedingHelper.createPrincess(targetBeeSpecies);
            if (princess == null) {
                allTasksBlocked = true;
                missingDroneSpecies = targetBeeSpecies;
                markBreedingChanged();
                // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_unreachable_target
                // # Current species archive cannot reach the target species!
                // # zh_CN 当前物种档案无法到达目标品种！
                return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_unreachable_target");
            }
            ItemStack drone = BeeBreedingHelper.createDrone(targetBeeSpecies);
            mOutputItems = drone == null ? new ItemStack[] { princess } : new ItemStack[] { princess, drone };
            activeOperation = OPERATION_OUTPUT;
            prepareBreedingCycle(0L);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        BeeBreedingHelper.BreedingStep nextStep = breedingPlan.getNextReadyStep(dronePool.getAvailableSpecies());
        if (nextStep == null) {
            allTasksBlocked = true;
            missingDroneSpecies = breedingPlan.getFirstMissingSpecies();
            if (missingDroneSpecies.isEmpty()) missingDroneSpecies = targetBeeSpecies;
            markBreedingChanged();
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_missing_drone
            // # Species archive is missing a required parent!
            // # zh_CN 物种档案中缺少必要亲本！
            return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_drone");
        }

        long eut = getEuPerBreedingAttempt();
        if (getMaxInputEu() < eut) return CheckRecipeResultRegistry.insufficientPower(eut);

        activeOperation = OPERATION_BREEDING;
        activeBreedingAttempt = new BreedingAttempt(nextStep, nextStep.chance + getMutationChanceBonus());
        allTasksBlocked = false;
        missingDroneSpecies = "";
        prepareBreedingCycle(eut);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    public List<BeeBreedingHelper.BreedingStep> getActiveBreedingSteps() {
        if (mMaxProgresstime <= 0 || activeOperation != OPERATION_BREEDING || activeBreedingAttempt == null)
            return Collections.emptyList();
        return Collections.singletonList(
            new BeeBreedingHelper.BreedingStep(
                activeBreedingAttempt.step.parent1,
                activeBreedingAttempt.step.parent2,
                activeBreedingAttempt.step.result,
                activeBreedingAttempt.effectiveChance));
    }

    private void prepareBreedingCycle(long eut) {
        mMaxProgresstime = getBreedingDurationTicks();
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        lEUt = -eut;
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
        } else if (activeOperation == OPERATION_BREEDING && activeBreedingAttempt != null) {
            if (!dronePool.hasDrone(activeBreedingAttempt.step.result)
                && BeeBreedingHelper.tryMutation(activeBreedingAttempt.effectiveChance)) {
                archiveChanged = dronePool.unlockSpecies(activeBreedingAttempt.step.result);
            }
        }

        activeOperation = OPERATION_NONE;
        activeBreedingAttempt = null;
        if (archiveChanged) invalidateBreedingPlan();
        markBreedingChanged();
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
            boolean hasReadyStep = breedingPlan.getNextReadyStep(dronePool.getAvailableSpecies()) != null;
            allTasksBlocked = pendingPrincessOutputs > 0 && !targetUnlocked && !hasReadyStep;
            missingDroneSpecies = allTasksBlocked ? breedingPlan.getFirstMissingSpecies() : "";
            if (allTasksBlocked && missingDroneSpecies.isEmpty()) {
                missingDroneSpecies = targetBeeSpecies;
            }
        }

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

    @Override
    public void clearHatches() {
        super.clearHatches();
        for (ParameterModuleHatch module : parameterModules) module.setWorking(false);
        parameterModules.clear();
    }

    public boolean addParameterModuleToMachineList(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ParameterModuleHatch module)) return false;
        if (getParameterModuleLimit(module.getModuleType()) <= 0) return false;
        if (!parameterModules.contains(module)) {
            module.updateTexture(casingIndex);
            module.updateCraftingIcon(getMachineCraftingIcon());
            parameterModules.add(module);
        }
        return true;
    }

    @Override
    public boolean addToMachineList(IGregTechTileEntity tile, int casingIndex) {
        return addParameterModuleToMachineList(tile, casingIndex) || super.addToMachineList(tile, casingIndex);
    }

    @Override
    public long getMaxInputVoltage() {
        long voltage = 0;
        for (var hatch : validMTEList(getExoticAndNormalEnergyHatchList())) {
            voltage = Math.max(
                voltage,
                hatch.getBaseMetaTileEntity()
                    .getInputVoltage());
        }
        return voltage;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        if (side == aFacing) {
            if (aActive) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(10), TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(10), TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_DISTILLATION_TOWER_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(10) };
    }

    private ParameterModuleHatch parameterModule(MachineModuleType type) {
        for (ParameterModuleHatch module : parameterModules) {
            if (module.getModuleType() == type) return module;
        }
        return null;
    }

    private void scanInputBuses() {
        ArrayList<ItemStack> inputs = getStoredInputs();
        List<ItemStack> toRemove = new ArrayList<>();

        for (ItemStack stack : inputs) {
            if (stack == null || stack.stackSize <= 0) continue;

            if (BeeBreedingHelper.isDrone(stack)) {
                dronePool.addDrone(stack);
                toRemove.add(stack);
            } else if (BeeBreedingHelper.isPrincess(stack)) {

                String uid = BeeBreedingHelper.getBeeUID(stack);
                if (uid != null) {
                    dronePool.unlockSpecies(uid);
                    long newPending = (long) pendingPrincessOutputs + Math.max(1, stack.stackSize);
                    pendingPrincessOutputs = (int) Math.min(Integer.MAX_VALUE, newPending);
                }
                toRemove.add(stack);
            }
        }

        for (ItemStack stack : toRemove) {
            stack.stackSize = 0;
        }
        if (!toRemove.isEmpty()) {
            updateSlots();
            markBaseTileDirty();
            invalidateBreedingPlan();
            markBreedingChanged();
        }

    }

    public String getMachineType() {
        // #tr gtnr.bee_breeder.type
        // # Bee Breeder
        // # zh_CN 养蜂机
        return StatCollector.translateToLocal("gtnr.bee_breeder.type");
    }

    private int countCompletedSteps() {
        int count = 0;
        for (BeeBreedingHelper.BreedingStep step : breedingChain) {
            if (dronePool.hasDrone(step.result)) count++;
        }
        return count;
    }

    private void updateChainDisplayInfo() {
        chainTotalSteps = breedingChain.size();
        chainCompletedSteps = countCompletedSteps();
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.bee_breeder.type
        // # Bee Breeder
        // # zh_CN 养蜂机
        return StatCollector.translateToLocal("gtnr.bee_breeder.type");
    }

    @Override
    public TooltipStats getTooltipStats() {
        return new TooltipStats(0, Double.NaN, Double.NaN, false);
    }

    @Override
    public String getTooltipPath() {
        return "large-bee-breeder";
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of("attemptSeconds", TICKS_PER_BREEDING / 20D, "baseEu", BASE_EUT_PER_BREEDING);
    }

    @Override
    public String[] getInfoData() {
        String[] superInfo = super.getInfoData();
        String[] info = new String[superInfo.length + 5];
        System.arraycopy(superInfo, 0, info, 0, superInfo.length);

        info[superInfo.length] = StatCollector.translateToLocalFormatted(
            "gtnr.bee_breeder.current_target",
            targetBeeSpecies.isEmpty() ? StatCollector.translateToLocal("gtnr.bee_breeder.unset") : targetBeeSpecies);
        // #tr gtnr.bee_breeder.archive_size
        // # Archived species: %s
        // # zh_CN 已解锁蜂种：%s
        info[superInfo.length + 1] = StatCollector.translateToLocalFormatted(
            "gtnr.bee_breeder.archive_size",
            dronePool.getAvailableSpecies()
                .size());
        // #tr gtnr.bee_breeder.chain_progress
        // # Route progress: %s/%s
        // # zh_CN 路线进度：%s/%s
        info[superInfo.length + 2] = StatCollector
            .translateToLocalFormatted("gtnr.bee_breeder.chain_progress", countCompletedSteps(), breedingChain.size());
        // #tr gtnr.bee_breeder.pending
        // # Princesses waiting: %s
        // # zh_CN 待转换公主蜂：%s
        info[superInfo.length + 3] = StatCollector
            .translateToLocalFormatted("gtnr.bee_breeder.pending", pendingPrincessOutputs);
        // #tr gtnr.bee_breeder.energy
        // # Per attempt: %s EU/t | Chance bonus: +%s%%
        // # zh_CN 每次尝试：%s EU/t | 成功率加成：+%s%%
        info[superInfo.length + 4] = StatCollector.translateToLocalFormatted(
            "gtnr.bee_breeder.energy",
            NumberFormatUtil.formatNumber(getEuPerBreedingAttempt()),
            getMutationChanceBonus());

        return info;
    }

    public List<BeeBreedingHelper.BreedingStep> getBreedingSteps() {
        return Collections.unmodifiableList(breedingChain);
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<LargeBeeBreeder> getGui() {
        return new LargeBeeBreederGui(this);
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

    @Override
    public void onValueUpdate(byte aValue) {}

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
        if (activeBreedingAttempt != null) attempts.appendTag(activeBreedingAttempt.toNBT());
        aNBT.setTag("beeBreederActiveBatch", attempts);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);

        targetBeeSpecies = aNBT.getString("targetBeeSpecies");
        pendingPrincessOutputs = aNBT.getInteger("pendingPrincessOutputs");

        if (aNBT.hasKey("dronePool")) {
            dronePool = DronePool.fromNBT(aNBT.getCompoundTag("dronePool"));
        } else {
            dronePool = new DronePool();
        }

        activeOperation = aNBT.getByte("beeBreederActiveOperation");
        activeBreedingAttempt = null;
        // Keep the existing save key, but resume only the first valid attempt from legacy batches.
        // Other unfinished species remain in the route and will be retried sequentially.
        NBTTagList attempts = aNBT.getTagList("beeBreederActiveBatch", 10);
        if (activeOperation == OPERATION_BREEDING) {
            for (int i = 0; i < attempts.tagCount() && activeBreedingAttempt == null; i++) {
                activeBreedingAttempt = BreedingAttempt.fromNBT(attempts.getCompoundTagAt(i));
            }
            if (activeBreedingAttempt != null && attempts.tagCount() > 1) lEUt /= attempts.tagCount();
        }

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
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public int getMaxParallelRecipes() {
        return 1;
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

    public long getEuPerBreedingAttempt() {
        ParameterModuleHatch overclock = parameterModule(MachineModuleType.Overclock);
        ParameterModuleHatch power = parameterModule(MachineModuleType.PowerConsumption);
        double increase = (overclock == null ? 4 : overclock.getPowerIncrease()) / 2D;
        return Math.max(
            1L,
            (long) Math.ceil(
                BASE_EUT_PER_BREEDING * Math.pow(increase, getVoltageUpgrades())
                    * (power == null ? 1 : power.getPowerMultiplier())));
    }

    public int getBreedingVoltageTier() {
        return Math.max(VoltageIndex.LV, Math.min(GTValues.V.length - 1, GTUtility.getTier(getMaxInputVoltage())));
    }

    private int getVoltageUpgrades() {
        return getBreedingVoltageTier() - VoltageIndex.LV;
    }

    public int getBreedingDurationTicks() {
        ParameterModuleHatch speed = parameterModule(MachineModuleType.Speed);
        ParameterModuleHatch overclock = parameterModule(MachineModuleType.Overclock);
        int timeReduction = overclock == null ? 2 : overclock.getTimeReduction();
        return Math.max(
            1,
            (int) Math.ceil(
                TICKS_PER_BREEDING / Math.pow(timeReduction, getVoltageUpgrades())
                    / (speed == null ? 1 : speed.getParameter())));
    }

    public int getMutationChanceBonus() {
        return (hasStainlessSteelGear ? 2 : 0) + Math.max(0, glassTier);
    }

    @Override
    public boolean doesBindPlayerInventory() {
        return true;
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return type == MachineModuleType.Parallel ? 0 : 1;
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
            BeeBreedingHelper.BreedingStep step = new BeeBreedingHelper.BreedingStep(
                parent1,
                parent2,
                result,
                tag.getDouble("baseChance"));
            return new BreedingAttempt(step, tag.getDouble("effectiveChance"));
        }
    }

}
