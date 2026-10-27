// SPDX-License-Identifier: MIT
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
    private static final int BASE_PARALLEL_SPECIES = 2;

    private static final byte OPERATION_NONE = 0;
    private static final byte OPERATION_BREEDING = 1;
    private static final byte OPERATION_OUTPUT = 2;

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int HORIZONTAL_OFFSET = 7;
    private static final int VERTICAL_OFFSET = 8;
    private static final int DEPTH_OFFSET = 0;

    private static final String[][] SHAPE = transpose(
        new String[][] {
            { "               ", "               ", "               ", "      HHH      ", "    HHAAAHH    ",
                "    HAPLPAH    ", "   HAPAAAPAH   ", "   HALAAALAH   ", "   HAPAAAPAH   ", "    HAPLPAH    ",
                "    HHAAAHH    ", "      HHH      ", "               ", "               ", "               " },
            { "               ", "               ", "      GGG      ", "    GG   GG    ", "   G       G   ",
                "   G       G   ", "  G         G  ", "  G         G  ", "  G         G  ", "   G       G   ",
                "   G       G   ", "    GG   GG    ", "      GGG      ", "               ", "               " },
            { "               ", "      HHH      ", "   HHH   HHH   ", "  HG       GH  ", "  H         H  ",
                "  H         H  ", " H           H ", " H           H ", " H           H ", "  H         H  ",
                "  H         H  ", "  HG       GH  ", "   HHH   HHH   ", "      HHH      ", "               " },
            { "      GGG      ", "   GGG   GGG   ", "  G         G  ", " G           G ", " G           G ",
                " G           G ", "G             G", "G             G", "G             G", " G           G ",
                " G           G ", " G           G ", "  G         G  ", "   GGG   GGG   ", "      GGG      " },
            { "      AAA      ", "   OLA   ALO   ", "  P         P  ", " O           O ", " L           L ",
                " A           A ", "A             A", "A             A", "A             A", " A           A ",
                " L           L ", " O           O ", "  P         P  ", "   OLA   ALO   ", "      AAA      " },
            { "     AAAAA     ", "   NA     AO   ", "  P         P  ", " N           O ", " A           A ",
                "A             A", "A     III     A", "A     III     A", "A     III     A", "A             A",
                " A           A ", " N           N ", "  P         P  ", "   NA     AN   ", "     AAAAA     " },
            { "     AAAAA     ", "   NA     AO   ", "  P         P  ", " N           O ", " A           A ",
                "A             A", "A     JJJ     A", "A     JKJ     A", "A     JJJ     A", "A             A",
                " A           A ", " N           N ", "  P         P  ", "   NA     AN   ", "     AAAAA     " },
            { "      AAA      ", "   OLA   ALO   ", "  P         P  ", " O           O ", " L           L ",
                " A           A ", "A             A", "A             A", "A             A", " A           A ",
                " L           L ", " O           O ", "  P         P  ", "   OLA   ALO   ", "      AAA      " },
            { "      G~G      ", "   GGGBBBGGG   ", "  GBB     BBG  ", " GBB       BBG ", " GB         BG ",
                " G           G ", "GB           BG", "GB           BG", "GB           BG", " G           G ",
                " GB         BG ", " GBB       BBG ", "  GBB     BBG  ", "   GGGBBBGGG   ", "      GGG      " },
            { "      HHH      ", "    HHBBBHH    ", "  HHBBBBBBBHH  ", "  HBBB   BBBH  ", " HBB       BBH ",
                " HBB BBBBB BBH ", "HBB  BBBBBB BBH", "HBB BBBBBBB BBH", "HBB BBBBBB  BBH", " HB  BBBBB BBH ",
                " HBB   BB BBH  ", "  HBBB    BBH  ", "  HHBBBBBBBHH  ", "    HHBBBHH    ", "      HHH      " },
            { "               ", "     GGGGG     ", "   GGBBBBBGG   ", "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ",
                " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ", " GBBBBBBBBBBBG ",
                "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ", "   GGBBBBBGG   ", "     GGGGG     ", "               " },
            { "               ", "      HHH      ", "    HHBBBHH    ", "   HBBBBBBBH   ", "  HBBBBBBBBBH  ",
                "  HBBBBBBBBBH  ", " HBBBBBBBBBBBH ", " HBBBBBBBBBBBH ", " HBBBBBBBBBBBH ", "  HBBBBBBBBBH  ",
                "  HBBBBBBBBBH  ", "   HBBBBBBBH   ", "    HHBBBHH    ", "      HHH      ", "               " },
            { "               ", "               ", "      GGG      ", "    GGBBBGG    ", "   GBBBBBBBG   ",
                "   GBBBBBBBG   ", "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ", "  GBBBBBBBBBG  ", "   GBBBBBBBG   ",
                "   GBBBBBBBG   ", "    GGBBBGG    ", "      GGG      ", "               ", "               " },
            { "               ", "               ", "       H       ", "     HHBHH     ", "    HBBBBBH    ",
                "   HBBBBBBBH   ", "   HBBBBBBBH   ", "  HBBBBBBBBBH  ", "   HBBBBBBBH   ", "   HBBBBBBBH   ",
                "    HBBBBBH    ", "     HHBHH     ", "       H       ", "               ", "               " },
            { "               ", "               ", "               ", "       G       ", "     GGBGG     ",
                "    GBBBBBG    ", "    GBBBBBG    ", "   GBBBBBBBG   ", "    GBBBBBG    ", "    GBBBBBG    ",
                "     GGBGG     ", "       G       ", "               ", "               ", "               " },
            { "               ", "               ", "               ", "               ", "      HHH      ",
                "     HHHHH     ", "    HHBBBHH    ", "    HHBBBHH    ", "    HHBBBHH    ", "     HHHHH     ",
                "      HHH      ", "               ", "               ", "               ", "               " },
            { "               ", "               ", "               ", "               ", "               ",
                "               ", "      GGG      ", "      GHG      ", "      GGG      ", "               ",
                "               ", "               ", "               ", "               ", "               " } });

    private int mCountCasing;
    private boolean hasStainlessSteelGear;
    private int glassTier = -1;
    private IStructureDefinition<LargeBeeBreeder> structureDefinition;
    private final List<ParameterModuleHatch> parameterModules = new ArrayList<>();

    @Getter
    private String targetBeeSpecies = "";

    @Getter
    private DronePool dronePool = new DronePool();

    private BeeBreedingPlanner.Plan breedingPlan = BeeBreedingPlanner.Plan.empty("");
    private List<BeeBreedingHelper.BreedingStep> breedingChain = new ArrayList<>();
    private boolean breedingPlanDirty = true;

    private byte activeOperation = OPERATION_NONE;
    private List<BreedingAttempt> activeBreedingBatch = new ArrayList<>();

    @Getter
    private int pendingPrincessOutputs = 0;

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

    public String getMachineType() {
        // #tr gtnr.bee_breeder.type
        // # Bee Breeder
        // # zh_CN 养蜂机
        return StatCollector.translateToLocal("gtnr.bee_breeder.type");
    }

    @Override
    public void onValueUpdate(byte aValue) {}

    @Override
    public byte getUpdateData() {
        return 0;
    }

    @Override
    public IStructureDefinition<LargeBeeBreeder> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<LargeBeeBreeder>builder()
                .addShape(STRUCTURE_PIECE_MAIN, SHAPE)
                .addElement('A', chainAllGlasses(-1, (te, t) -> te.glassTier = t, te -> te.glassTier))
                .addElement('B', ofChain(ofBlockAnyMeta(Blocks.dirt, 0), ofBlock(Blocks.grass, 0)))
                .addElement(
                    'G',
                    ofChain(
                        StructureTooltipHooks.describeAsHatch(
                            ofHatchAdder(LargeBeeBreeder::addParameterModuleToMachineList, 10, 1),
                            // #tr gtnr.bee_breeder.modules
                            // # Parameter Module
                            // # zh_CN 参数模块
                            "gtnr.bee_breeder.modules"),
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
            if (entry.getValue() > 1) errors.add(
                StructureErrors.hatchCount(
                    ErrorType.TOO_MANY,
                    TranslatableText.lang(moduleTypeKey(entry.getKey())),
                    entry.getValue(),
                    1));
        }
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        for (ParameterModuleHatch module : parameterModules) module.setWorking(false);
        parameterModules.clear();
    }

    public boolean addParameterModuleToMachineList(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ParameterModuleHatch module)) return false;
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
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public int getMaxParallelRecipes() {
        return getMaxParallelBreedingSteps();
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

    private boolean checkStainlessSteelGear(ItemStack stack) {
        if (stack == null) return false;
        return stack.isItemEqual(GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.StainlessSteel, 1L));
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

    @Nonnull
    @Override
    public CheckRecipeResult checkProcessing() {
        scanInputBuses();
        activeOperation = OPERATION_NONE;
        activeBreedingBatch.clear();

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

        List<BeeBreedingHelper.BreedingStep> readySteps = breedingPlan
            .getReadySteps(dronePool.getAvailableSpecies(), getMaxParallelBreedingSteps());
        if (readySteps.isEmpty()) {
            allTasksBlocked = true;
            missingDroneSpecies = breedingPlan.getFirstMissingSpecies();
            if (missingDroneSpecies.isEmpty()) missingDroneSpecies = targetBeeSpecies;
            markBreedingChanged();
            // #tr GT5U.gui.text.recipe_result.gtnr_bee_breeder_missing_drone
            // # Species archive is missing a required parent!
            // # zh_CN 物种档案中缺少必要亲本！
            return SimpleCheckRecipeResult.ofFailure("gtnr_bee_breeder_missing_drone");
        }

        long eutPerSpecies = getEuPerBreedingAttempt();
        long affordableSpecies = getMaxInputEu() / eutPerSpecies;
        if (affordableSpecies < 1) return CheckRecipeResultRegistry.insufficientPower(eutPerSpecies);
        if (readySteps.size() > affordableSpecies) readySteps = readySteps.subList(0, (int) affordableSpecies);

        double chanceBonus = (hasStainlessSteelGear ? 2.0D : 0.0D) + Math.max(0, glassTier);
        List<BreedingAttempt> batch = new ArrayList<>(readySteps.size());
        for (BeeBreedingHelper.BreedingStep step : readySteps) {
            batch.add(new BreedingAttempt(step, step.chance + chanceBonus));
        }

        activeOperation = OPERATION_BREEDING;
        activeBreedingBatch = batch;
        allTasksBlocked = false;
        missingDroneSpecies = "";
        prepareBreedingCycle(getEnergyCostPerTick(batch.size()));
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private long getEnergyCostPerTick(int parallelAttempts) {
        long eutPerAttempt = getEuPerBreedingAttempt();
        return eutPerAttempt * Math.min(Math.max(0, parallelAttempts), Long.MAX_VALUE / eutPerAttempt);
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

    public int getMaxParallelBreedingSteps() {
        ParameterModuleHatch parallel = parameterModule(MachineModuleType.Parallel);
        long multiplier = parallel == null ? 1 : 1L + parallel.getParameter();
        long base = Math.min(Integer.MAX_VALUE, (long) BASE_PARALLEL_SPECIES << getVoltageUpgrades());
        return (int) Math.min(Integer.MAX_VALUE, base * multiplier);
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

    public int getActiveBreedingSpecies() {
        return mMaxProgresstime > 0 && activeOperation == OPERATION_BREEDING ? activeBreedingBatch.size() : 0;
    }

    public List<String> getActiveBreedingSpeciesUids() {
        if (getActiveBreedingSpecies() == 0) return Collections.emptyList();
        return activeBreedingBatch.stream()
            .map(attempt -> attempt.step.result)
            .toList();
    }

    private ParameterModuleHatch parameterModule(MachineModuleType type) {
        for (ParameterModuleHatch module : parameterModules) {
            if (module.getModuleType() == type) return module;
        }
        return null;
    }

    public int getMutationChanceBonus() {
        return (hasStainlessSteelGear ? 2 : 0) + Math.max(0, glassTier);
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

        updateChainDisplayInfo();
        if (previousBlocked != allTasksBlocked || !Objects.equals(previousMissing, missingDroneSpecies)
            || previousCompleted != chainCompletedSteps) {
            markBreedingChanged();
        }
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

    public List<BeeBreedingHelper.BreedingStep> getBreedingSteps() {
        return Collections.unmodifiableList(breedingChain);
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<LargeBeeBreeder> getGui() {
        return new LargeBeeBreederGui(this);
    }

    @Override
    public boolean doesBindPlayerInventory() {
        return true;
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

    private void markBaseTileDirty() {
        IGregTechTileEntity baseTile = getBaseMetaTileEntity();
        if (baseTile != null) baseTile.markDirty();
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
        return new TooltipStats(BASE_PARALLEL_SPECIES, Double.NaN, Double.NaN, false);
    }

    @Override
    public List<String> getTooltipFeatureLines() {
        return List.of(
            // #tr gtnr.bee_breeder.tooltip.breeding
            // # Automatically breeds bees through a planned mutation chain
            // # zh_CN 自动规划杂交链，培育目标蜂种
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.breeding"),
            // #tr gtnr.bee_breeder.tooltip.base
            // # LV base: %s seconds per attempt, up to %s distinct species at once
            // # zh_CN LV 基准：每次尝试耗时 %s 秒，同时最多杂交 %s 种蜂
            StatCollector.translateToLocalFormatted(
                "gtnr.bee_breeder.tooltip.base",
                TICKS_PER_BREEDING / 20D,
                BASE_PARALLEL_SPECIES),
            // #tr gtnr.bee_breeder.tooltip.voltage
            // # Each voltage tier halves the duration and doubles parallel species, with a minimum duration of 1 tick
            // # zh_CN 电压每升一级，耗时减半、并行蜂种数翻倍，耗时最低 1 tick
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.voltage"),
            // #tr gtnr.bee_breeder.tooltip.parallel
            // # Only distinct ready species in the current route run in parallel; both parents must already be archived
            // # zh_CN 并行只计算当前路线中亲本均已存档的不同蜂种，不会重复尝试同一种蜂
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.parallel"),
            // #tr gtnr.bee_breeder.tooltip.energy
            // # LV uses %s EU/t per species, doubling per voltage tier; a full base batch uses that tier's recipe power
            // # zh_CN LV 每个蜂种消耗 %s EU/t，升压后每级翻倍；满基础并行消耗当前档位配方功率
            StatCollector.translateToLocalFormatted("gtnr.bee_breeder.tooltip.energy", BASE_EUT_PER_BREEDING),
            // @formatter:off
            // #tr gtnr.bee_breeder.tooltip.modules
            // # Supports speed, parallel, power and overclock modules; changes apply to new cycles and power limits concurrency
            // # zh_CN 支持速度、并行、能耗和超频模块；模块参数对新一轮任务生效，实际并行受供电功率限制
            // @formatter:on
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.modules"),
            // @formatter:off
            // #tr gtnr.bee_breeder.tooltip.overclock
            // # Overclock modules replace the default time and power ratios; voltage-based parallelism still doubles per tier
            // # zh_CN 超频模块可替代默认的升压耗时和能耗倍率；升压并行仍按每级翻倍
            // @formatter:on
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.overclock"),
            // #tr gtnr.bee_breeder.tooltip.mutation
            // # Uses Forestry mutation chances
            // # zh_CN 采用林业杂交概率
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.mutation"),
            // #tr gtnr.bee_breeder.tooltip.glass
            // # Each glass tier adds 1 percentage point to mutation chance
            // # zh_CN 玻璃每升 1 级，杂交成功率增加 1 个百分点
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.glass"),
            "",
            // #tr gtnr.bee_breeder.tooltip.archive
            // # Input princesses or drones to permanently unlock their species
            // # zh_CN 投入公主蜂或雄蜂，永久解锁对应蜂种
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.archive"),
            // #tr gtnr.bee_breeder.tooltip.target
            // # Set the target in the GUI; reachable mutation steps run automatically
            // # zh_CN 在 GUI 设置目标，机器自动执行可达的杂交步骤
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.target"),
            // #tr gtnr.bee_breeder.tooltip.princess
            // # Each input princess requests one target princess
            // # zh_CN 每投入一只公主蜂，可输出一只目标公主蜂
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.princess"),
            // #tr gtnr.bee_breeder.tooltip.gear
            // # A Stainless Steel Gear in the controller adds 2 percentage points to mutation chance
            // # zh_CN 控制器槽放入不锈钢齿轮，杂交成功率增加 2 个百分点
            StatCollector.translateToLocal("gtnr.bee_breeder.tooltip.gear"));
    }

    @Override
    public List<String> getTooltipStructureNotes() {
        // #tr gtnr.multiblock.modules.limit
        // # At most one EU, one speed, one parallel and one overclock module per machine
        // # zh_CN 每台机器最多安装一个 EU 模块、一个速度模块、一个并行模块和一个超频模块
        return List.of(StatCollector.translateToLocal("gtnr.multiblock.modules.limit"));
    }

    private static String moduleTypeKey(MachineModuleType type) {
        return switch (type) {
            // #tr gtnr.module.type.parallel
            // # Parallel Module
            // # zh_CN 并行模块
            case Parallel -> "gtnr.module.type.parallel";
            // #tr gtnr.module.type.speed
            // # Speed Module
            // # zh_CN 速度模块
            case Speed -> "gtnr.module.type.speed";
            // #tr gtnr.module.type.eu
            // # EU Module
            // # zh_CN EU 模块
            case PowerConsumption -> "gtnr.module.type.eu";
            // #tr gtnr.module.type.overclock
            // # Overclock Module
            // # zh_CN 超频模块
            case Overclock -> "gtnr.module.type.overclock";
        };
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
        // # Per species: %s EU/t | Chance bonus: +%s%%
        // # zh_CN 每蜂种：%s EU/t | 成功率加成：+%s%%
        info[superInfo.length + 4] = StatCollector.translateToLocalFormatted(
            "gtnr.bee_breeder.energy",
            NumberFormatUtil.formatNumber(getEnergyCostPerTick(1)),
            getMutationChanceBonus());

        return info;
    }

    private void markBreedingChanged() {
        breedingRevision++;
    }

    private void invalidateBreedingPlan() {
        breedingPlanDirty = true;
        markBreedingChanged();
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
