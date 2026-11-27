package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
import static gregtech.api.util.GTStructureUtility.activeCoils;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofCoil;
import static gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace.BASE_TIER_FOR_HEAT_INCREASE;
import static gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace.HEAT_PER_VOLTAGE_TIER;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.common.recipe.gtneverreset.PlatinumProcessingRecipes;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.misc.GTStructureChannels;

@IMetaTileEntity.SkipGenerateDescription
public final class PlatinumProcessingPlantController extends GTNRMultiBlockBase<PlatinumProcessingPlantController>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 12, VERTICAL_OFFSET = 21, DEPTH_OFFSET = 1;
    private static final Casings HULL = Casings.ChemicallyInertMachineCasing;

    private static IStructureDefinition<PlatinumProcessingPlantController> createStructure() {
        return StructureDefinition.<PlatinumProcessingPlantController>builder()
            .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load("platinum_processing_plant")))
            .addElement(
                'A',
                buildHatchAdder(PlatinumProcessingPlantController.class)
                    .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy, Maintenance)
                    .casingIndex(HULL.getTextureId())
                    .hint(1)
                    .buildAndChain(HULL.asElement()))
            .addElement('B', Casings.Firebricks.asElement())
            .addElement('C', HULL.asElement())
            .addElement('D', Casings.SolidSteelMachineCasing.asElement())
            .addElement(
                'K',
                GTStructureChannels.HEATING_COIL.use(
                    activeCoils(
                        ofCoil((machine, level) -> { machine.mCoilLevel = level; }, machine -> machine.mCoilLevel))))
            .addElement('S', Casings.CleanStainlessSteelMachineCasing.asElement())
            .addElement('#', isAir())
            .build();
    }

    public PlatinumProcessingPlantController(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private PlatinumProcessingPlantController(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new PlatinumProcessingPlantController(mName);
    }

    @Override
    public IStructureDefinition<PlatinumProcessingPlantController> getStructureDefinition() {
        return StructureHolder.DEFINITION;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, stack, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE,
            stack,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            environment,
            false,
            true);
    }

    @Override
    protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, InputHatch, 1);
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, OutputHatch, 1);
        checkHatchRange(errors, Energy, 1, 2);
        checkHatchRange(errors, Maintenance, shouldCheckMaintenance() ? 1 : 0, 1);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new PlatinumProcessingLogic();
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        setProcessingLogicPower(logic);
        logic.setBatchSize(1);
        logic.setRecipeLocking(this, false);
    }

    @Override
    protected boolean usesInstantThreadFill() {
        return true;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE : OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR)
            .extFacing()
            .build() };
    }

    @Override
    public ITexture getCasingTexture() {
        return HULL.getCasingTexture();
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.platinum_processing_plant.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public String getTooltipPath() {
        return "platinum-processing-plant";
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction != ForgeDirection.UNKNOWN && direction.offsetY == 0
            && rotation == Rotation.NORMAL
            && flip == Flip.NONE;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNRRecipeMaps.PLATINUM_PROCESSING;
    }

    @Override
    public int getMaxRecipeThreads() {
        return PlatinumProcessingRecipes.STATION_COUNT;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return 0;
    }

    private int getFurnaceHeat() {
        return (int) mCoilLevel.getHeat()
            + HEAT_PER_VOLTAGE_TIER * (GTUtility.getTier(getMaxInputVoltage()) - BASE_TIER_FOR_HEAT_INCREASE);
    }

    private final class PlatinumProcessingLogic extends RecipeThreadProcessingLogic {

        private int selectedStation;

        @Override
        @Nonnull
        protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
            int station = recipe.getMetadataOrDefault(PlatinumProcessingRecipes.STATION, -1);
            if (station < 0 || station >= PlatinumProcessingRecipes.STATION_COUNT
                || getRecipeThreadTasks().stream()
                    .anyMatch(task -> task.getStation() == station)) {
                return CheckRecipeResultRegistry.NO_RECIPE;
            }
            if (source(recipe) == RecipeMaps.blastFurnaceRecipes && requiredHeat(recipe) > getFurnaceHeat()) {
                return CheckRecipeResultRegistry.insufficientHeat(requiredHeat(recipe));
            }
            var result = super.validateRecipe(recipe);
            if (!result.wasSuccessful()) return result;
            selectedStation = station;
            return result;
        }

        @Override
        protected int getThreadStation() {
            return selectedStation;
        }

        @Override
        @Nonnull
        protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
            return super.createParallelHelper(recipe).setAvailableEUt(availableVoltage * availableAmperage);
        }

        @Override
        @Nonnull
        protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
            OverclockCalculator calculator = super.createOverclockCalculator(recipe);
            RecipeMap<?> map = source(recipe);
            if (map == RecipeMaps.multiblockChemicalReactorRecipes) {
                calculator.setDurationDecreasePerOC(4);
            } else if (map == RecipeMaps.blastFurnaceRecipes) {
                calculator.setRecipeHeat(requiredHeat(recipe))
                    .setMachineHeat(getFurnaceHeat())
                    .setHeatOC(true)
                    .setHeatDiscount(true);
            }
            return calculator;
        }

        private RecipeMap<?> source(GTRecipe recipe) {
            return recipe.getMetadata(PlatinumProcessingRecipes.SOURCE);
        }

        private int requiredHeat(GTRecipe recipe) {
            return recipe.getMetadataOrDefault(COIL_HEAT, recipe.mSpecialValue);
        }
    }

    private static final class StructureHolder {

        private static final IStructureDefinition<PlatinumProcessingPlantController> DEFINITION = createStructure();
    }
}
