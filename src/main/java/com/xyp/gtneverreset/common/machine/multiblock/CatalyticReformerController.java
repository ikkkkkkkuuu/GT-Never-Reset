package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchMuffler;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;

@IMetaTileEntity.SkipGenerateDescription
public final class CatalyticReformerController extends CrossRecipeMultiblockBase<CatalyticReformerController>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final Casings CASING = Casings.CleanStainlessSteelMachineCasing;
    private static final String STRUCTURE_NAME = "catalytic_reformer";
    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 1, VERTICAL_OFFSET = 1, DEPTH_OFFSET = 0;
    private static final int MIN_CASINGS = 24;
    private static final int MIN_ENERGY_HATCHES = 1;
    private static final int MAX_ENERGY_HATCHES = 2;
    private static final int BASE_RECIPE_THREADS = 8;
    private static final int THREADS_PER_VOLTAGE_TIER = 4;
    private static final int BASE_THREAD_VOLTAGE_TIER = VoltageIndex.HV;
    private static final List<RecipeMap<?>> RECIPE_MAPS = List.of(GTNRRecipeMaps.CATALYTIC_REFORMER);
    // #tr GT5U.gui.text.recipe_result.gtnr.catalytic_reformer.muffler_obstructed
    // # Muffler exhaust is obstructed
    // # zh_CN 消声仓排气口受阻
    private static final CheckRecipeResult MUFFLER_OBSTRUCTED = SimpleCheckRecipeResult
        .ofFailure("gtnr.catalytic_reformer.muffler_obstructed");

    private static IStructureDefinition<CatalyticReformerController> createStructure() {
        int texture = CASING.getTextureId();
        return StructureDefinition.<CatalyticReformerController>builder()
            .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load(STRUCTURE_NAME)))
            .addElement(
                'C',
                buildHatchAdder(CatalyticReformerController.class)
                    .atLeast(InputBus, InputHatch, OutputHatch, Energy, Maintenance)
                    .casingIndex(texture)
                    .hint(1)
                    .buildAndChain(onElementPass(machine -> machine.casingCount++, CASING.asElement())))
            .addElement('P', Casings.SteelPipeCasing.asElement())
            .addElement('F', ofFrame(Materials.StainlessSteel))
            .addElement(
                'O',
                buildHatchAdder(CatalyticReformerController.class).atLeast(Muffler)
                    .casingIndex(texture)
                    .hint(2)
                    .build())
            .addElement('#', isAir())
            .build();
    }

    private int casingCount;

    public CatalyticReformerController(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public CatalyticReformerController(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new CatalyticReformerController(mName);
    }

    @Override
    public IStructureDefinition<CatalyticReformerController> getStructureDefinition() {
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
        casingCount = 0;
        checkPiece(STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkCasingMin(errors, casingCount, MIN_CASINGS);
        checkHatchRange(errors, Energy, MIN_ENERGY_HATCHES, MAX_ENERGY_HATCHES);
        checkHatchRange(errors, Maintenance, shouldCheckMaintenance() ? 1 : 0, 1);
        checkOneMufflerHatch(errors);
    }

    @Override
    protected CheckRecipeResult doCheckRecipe() {
        return isMufflerOpen() ? super.doCheckRecipe() : MUFFLER_OBSTRUCTED;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (!isMufflerOpen()) {
            setCheckRecipeResult(MUFFLER_OBSTRUCTED);
            return false;
        }
        if (MUFFLER_OBSTRUCTED.equals(getCheckRecipeResult())) {
            setCheckRecipeResult(CheckRecipeResultRegistry.SUCCESSFUL);
        }
        return super.onRunningTick(stack);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return Textures.BlockIcons.createTextureWithCasing(
            this,
            side,
            facing,
            active,
            OVERLAY_FRONT_OIL_CRACKER,
            OVERLAY_FRONT_OIL_CRACKER_GLOW,
            OVERLAY_FRONT_OIL_CRACKER_ACTIVE,
            OVERLAY_FRONT_OIL_CRACKER_ACTIVE_GLOW);
    }

    @Override
    public ITexture getCasingTexture() {
        return CASING.getCasingTexture();
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.catalytic_reformer.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public String getTooltipPath() {
        return "catalytic-reformer";
    }

    @Override
    public List<TieredPart> getTooltipTieredParts() {
        return List.of(TieredPart.VOLTAGE);
    }

    @Override
    public TierRange getTooltipTierRange(TieredPart part) {
        if (part == TieredPart.VOLTAGE) return new TierRange(BASE_THREAD_VOLTAGE_TIER, VoltageIndex.UXV);
        return super.getTooltipTierRange(part);
    }

    @Override
    protected RecipeThreadTask createLegacyRecipeThread() {
        if (mMaxProgresstime <= 0) return null;
        return new RecipeThreadTask(
            0,
            Math.max(0L, -lEUt),
            mMaxProgresstime,
            Math.max(1, lastParallel),
            new RecipeThreadOutputs(mOutputItems, mOutputFluids),
            mProgresstime);
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return RECIPE_MAPS;
    }

    @Override
    public int getMaxRecipeThreads() {
        int tier = GTUtility.getTier(getMaxInputVoltage());
        return BASE_RECIPE_THREADS + THREADS_PER_VOLTAGE_TIER * Math.max(0, tier - BASE_THREAD_VOLTAGE_TIER);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return false;
    }

    @Override
    public boolean getDefaultBatchMode() {
        return false;
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return 0;
    }

    @Override
    protected boolean usesInstantThreadFill() {
        // Budget EU/t across free slots before spending spare power on overclocks.
        return false;
    }

    private boolean isMufflerOpen() {
        for (MTEHatchMuffler hatch : mMufflerHatches) {
            if (!hatch.isValid()) continue;
            IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
            return tile.getAirAtSide(tile.getFrontFacing());
        }
        return false;
    }

    private static final class StructureHolder {

        private static final IStructureDefinition<CatalyticReformerController> DEFINITION = createStructure();
    }
}
