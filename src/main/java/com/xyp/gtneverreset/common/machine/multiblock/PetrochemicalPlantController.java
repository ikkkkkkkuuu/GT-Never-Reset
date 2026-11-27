package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.crafting.PetrochemicalFluidBuffer;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.common.recipe.gtneverreset.PetrochemicalPlantRecipes;
import com.xyp.gtneverreset.common.recipe.gtneverreset.PetrochemicalPlantRecipes.Step;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateDescription
public final class PetrochemicalPlantController extends GTNRMultiBlockBase<PetrochemicalPlantController>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 4, VERTICAL_OFFSET = 10, DEPTH_OFFSET = 0;
    private static final int RECIPE_THREADS = 5;
    private static final int CUPRONICKEL_CRACKING_DISCOUNT_PERCENT = 10;
    private static final Casings CASING = Casings.CleanStainlessSteelMachineCasing;
    private static final Casings REACTOR_CASING = Casings.ChemicallyInertMachineCasing;
    private static final String BUFFER_NBT_KEY = "petrochemicalIntermediates";

    private static IStructureDefinition<PetrochemicalPlantController> createStructure() {
        return StructureDefinition.<PetrochemicalPlantController>builder()
            .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load("petrochemical_plant")))
            .addElement('B', Casings.Firebricks.asElement())
            .addElement('S', CASING.asElement())
            .addElement('A', hatchOrCasing(CASING, 1))
            .addElement('C', REACTOR_CASING.asElement())
            .addElement('R', hatchOrCasing(REACTOR_CASING, 2))
            .addElement('P', Casings.SteelPipeCasing.asElement())
            .addElement('T', Casings.PTFEPipeCasing.asElement())
            .addElement('K', Casings.CupronickelCoilBlock.asElement())
            .addElement('#', isAir())
            .build();
    }

    private static IStructureElement<PetrochemicalPlantController> hatchOrCasing(Casings casing, int hint) {
        return buildHatchAdder(PetrochemicalPlantController.class)
            .atLeast(InputHatch, OutputHatch, OutputBus, Energy, Maintenance)
            .casingIndex(casing.getTextureId())
            .hint(hint)
            .buildAndChain(casing.asElement());
    }

    private final PetrochemicalFluidBuffer intermediates = new PetrochemicalFluidBuffer();

    public PetrochemicalPlantController(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private PetrochemicalPlantController(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new PetrochemicalPlantController(mName);
    }

    @Override
    public IStructureDefinition<PetrochemicalPlantController> getStructureDefinition() {
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
        checkHatchMin(errors, InputHatch, 1);
        checkHatchMin(errors, OutputHatch, 1);
        checkHatchRange(errors, Energy, 1, 2);
        checkHatchRange(errors, Maintenance, shouldCheckMaintenance() ? 1 : 0, 1);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new PetrochemicalProcessingLogic();
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
    protected boolean flushRecipeThreadOutputs(RecipeThreadTask task) {
        return task.getOutputs()
            .flush(this, intermediates);
    }

    @Override
    public ArrayList<FluidStack> getStoredFluidsForColor(Optional<Byte> color) {
        ArrayList<FluidStack> inputs = super.getStoredFluidsForColor(color);
        intermediates.prependInputs(inputs);
        return inputs;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE : OVERLAY_FRONT_DISTILLATION_TOWER)
            .extFacing()
            .build() };
    }

    @Override
    public ITexture getCasingTexture() {
        return CASING.getCasingTexture();
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.petrochemical_plant.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public String getTooltipPath() {
        return "petrochemical-plant";
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of("cracking_discount", CUPRONICKEL_CRACKING_DISCOUNT_PERCENT);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setTag(BUFFER_NBT_KEY, intermediates.save());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        intermediates.load(tag.getTagList(BUFFER_NBT_KEY, 10));
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setTag(BUFFER_NBT_KEY, intermediates.save());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey(BUFFER_NBT_KEY, NBT.TAG_LIST)) return;

        NBTTagList contents = tag.getTagList(BUFFER_NBT_KEY, NBT.TAG_COMPOUND);
        if (contents.tagCount() == 0) {
            // #tr gtnr.waila.petrochemical_plant.empty
            // # No buffered intermediates
            // # zh_CN 中间液缓冲为空
            lines.add(StatCollector.translateToLocal("gtnr.waila.petrochemical_plant.empty"));
            return;
        }

        // #tr gtnr.waila.petrochemical_plant.intermediates
        // # Buffered intermediates
        // # zh_CN 中间液储量
        lines.add(StatCollector.translateToLocal("gtnr.waila.petrochemical_plant.intermediates"));
        for (int i = 0; i < contents.tagCount(); i++) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(contents.getCompoundTagAt(i));
            if (fluid == null || fluid.amount <= 0) continue;
            // #tr gtnr.waila.petrochemical_plant.fluid_amount
            // # %s %s L
            // # zh_CN %s %s L
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.waila.petrochemical_plant.fluid_amount",
                    fluid.getLocalizedName(),
                    formatNumber(fluid.amount)));
        }
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction != ForgeDirection.UNKNOWN && direction.offsetY == 0
            && rotation == Rotation.NORMAL
            && flip == Flip.NONE;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNRRecipeMaps.PETROCHEMICAL_PLANT;
    }

    @Override
    public int getMaxRecipeThreads() {
        return RECIPE_THREADS;
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

    private final class PetrochemicalProcessingLogic extends RecipeThreadProcessingLogic {

        private int outputParallelLimit;

        @Override
        @Nonnull
        protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> map) {
            // Feed the downstream equipment first so a full upstream buffer can drain.
            return super.findRecipeMatches(map).sorted(Comparator.comparing(this::getStep));
        }

        @Override
        @Nonnull
        protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
            CheckRecipeResult result = super.validateRecipe(recipe);
            if (!result.wasSuccessful()) return result;
            int nativeParallelLimit = GTUtility
                .safeInt((long) (maxParallel * createOverclockCalculator(recipe).calculateMultiplierUnderOneTick()), 0);
            outputParallelLimit = Math
                .min(nativeParallelLimit, intermediates.limitParallels(recipe.mFluidOutputs, getRecipeThreadTasks()));
            if (outputParallelLimit > 0 && protectsExcessFluid()) {
                var externalFluids = Arrays.stream(recipe.mFluidOutputs)
                    .filter(stack -> stack != null && !intermediates.routes(stack))
                    .toList();
                if (!externalFluids.isEmpty()) {
                    outputParallelLimit = new FluidEjectionHelper(getOutputHatches(), true, true)
                        .ejectFluids(externalFluids, outputParallelLimit);
                }
            }
            return outputParallelLimit > 0 ? result : CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
        }

        @Override
        @Nonnull
        protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
            // Calculate at the native rate. The controller checks the shared power budget before committing inputs.
            return super.createParallelHelper(recipe).setAvailableEUt(availableVoltage * availableAmperage)
                .setMachine(PetrochemicalPlantController.this, protectsExcessItem(), false)
                .setEUtModifier(nativeEUtMultiplier(recipe))
                .setMaxParallelCalculator(
                    (candidate, max, fluids, items) -> candidate
                        .maxParallelCalculatedByInputs(Math.min(max, outputParallelLimit), fluids, items));
        }

        @Override
        @Nonnull
        protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
            OverclockCalculator calculator = super.createOverclockCalculator(recipe)
                .setEUtDiscount(nativeEUtMultiplier(recipe));
            if (getStep(recipe) == Step.DESULFURIZATION) {
                calculator.setDurationDecreasePerOC(4);
            }
            return calculator;
        }

        private double nativeEUtMultiplier(GTRecipe recipe) {
            return getStep(recipe) == Step.CRACKING ? 1.0 - CUPRONICKEL_CRACKING_DISCOUNT_PERCENT / 100.0 : 1.0;
        }

        private Step getStep(GTRecipe recipe) {
            return recipe.getMetadataOrDefault(PetrochemicalPlantRecipes.STEP, Step.SEPARATION);
        }
    }

    private static final class StructureHolder {

        private static final IStructureDefinition<PetrochemicalPlantController> DEFINITION = createStructure();
    }
}
