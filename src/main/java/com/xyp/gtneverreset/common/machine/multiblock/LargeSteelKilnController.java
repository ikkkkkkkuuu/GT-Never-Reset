package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.GTMod;
import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.StructureChecker;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.pollution.Pollution;

@IMetaTileEntity.SkipGenerateDescription
public final class LargeSteelKilnController extends MTEEnhancedMultiBlockBase<LargeSteelKilnController>
    implements ISurvivalConstructable, MultiblockTooltipSource, ICasingTextureProvider {

    private static final String MAIN = "main";
    private static final String AUXILIARY = "auxiliary";
    private static final int MAIN_X = 2, MAIN_Y = 3, MAIN_Z = 0;
    private static final int AUXILIARY_Y = 2, AUXILIARY_Z = -1;
    private static final int[] AUXILIARY_X = { 5, -3 };
    private static final int[] SPEED_BY_AUXILIARY_COUNT = { 1, 8, 16 };

    private static IStructureDefinition<LargeSteelKilnController> createStructure() {
        int texture = Casings.Firebricks.getTextureId();
        return StructureDefinition.<LargeSteelKilnController>builder()
            .addShape(MAIN, transpose(MultiblockStructureLoader.load("large_steel_kiln")))
            .addShape(AUXILIARY, transpose(MultiblockStructureLoader.load("large_steel_kiln_auxiliary")))
            .addElement('B', Casings.Firebricks.asElement())
            .addElement('F', Casings.SteelFireboxCasing.asElement())
            .addElement(
                'I',
                buildHatchAdder(LargeSteelKilnController.class).atLeast(InputBus)
                    .casingIndex(texture)
                    .hint(1)
                    .build())
            .addElement(
                'O',
                buildHatchAdder(LargeSteelKilnController.class).atLeast(OutputBus)
                    .casingIndex(texture)
                    .hint(2)
                    .build())
            .build();
    }

    private static GTRecipe compactRecipeOutputs(GTRecipe recipe) {
        ItemStack[] outputs = recipe.mOutputs;
        if (outputs == null) return recipe;

        // The primitive blast furnace map can leave an empty slot before the chanced ash output.
        int outputCount = 0;
        for (ItemStack output : outputs) {
            if (output != null) outputCount++;
        }
        if (outputCount == outputs.length) return recipe;

        GTRecipe compacted = recipe.copyShallow();
        compacted.mOutputs = new ItemStack[outputCount];
        compacted.mOutputChances = new int[outputCount];
        for (int original = 0, target = 0; original < outputs.length; original++) {
            if (outputs[original] != null) {
                compacted.mOutputs[target] = outputs[original];
                compacted.mOutputChances[target++] = recipe.getOutputChance(original);
            }
        }
        return compacted;
    }

    public LargeSteelKilnController(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private LargeSteelKilnController(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeSteelKilnController(mName);
    }

    @Override
    public IStructureDefinition<LargeSteelKilnController> getStructureDefinition() {
        return StructureHolder.DEFINITION;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(MAIN, trigger, hintsOnly, MAIN_X, MAIN_Y, MAIN_Z);
        int auxiliaries = Math.min(AUXILIARY_X.length, trigger.stackSize - 1);
        for (int i = 0; i < auxiliaries; i++) {
            buildPiece(AUXILIARY, trigger, hintsOnly, AUXILIARY_X[i], AUXILIARY_Y, AUXILIARY_Z);
        }
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (!mMachine) {
            int built = survivalBuildPiece(
                MAIN,
                trigger,
                MAIN_X,
                MAIN_Y,
                MAIN_Z,
                elementBudget,
                environment,
                false,
                true);
            if (built >= 0) return built;
        }
        int auxiliaries = Math.min(AUXILIARY_X.length, trigger.stackSize - 1);
        for (int i = 0; i < auxiliaries; i++) {
            int built = survivalBuildPiece(
                AUXILIARY,
                trigger,
                AUXILIARY_X[i],
                AUXILIARY_Y,
                AUXILIARY_Z,
                elementBudget,
                environment,
                false);
            if (built >= 0) return built;
        }
        return -1;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(MAIN, MAIN_X, MAIN_Y, MAIN_Z, errors);
    }

    private int countBuiltAuxiliaries() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        int count = 0;
        for (int x : AUXILIARY_X) {
            var check = new StructureChecker<>(this, false, null);
            getStructureDefinition().iterate(
                AUXILIARY,
                tile.getWorld(),
                getExtendedFacing(),
                tile.getXCoord(),
                tile.getYCoord(),
                tile.getZCoord(),
                x,
                AUXILIARY_Y,
                AUXILIARY_Z,
                check);
            if (check.success) count++;
        }
        return count;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.primitiveBlastRecipes;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Override
            protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
                return super.createParallelHelper(compactRecipeOutputs(recipe));
            }

            @Override
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                return OverclockCalculator.ofNoOverclock(recipe);
            }

            @Override
            protected double calculateDuration(@Nonnull GTRecipe recipe, @Nonnull ParallelHelper helper,
                @Nonnull OverclockCalculator calculator) {
                return Math.max(1, calculator.getDuration() / SPEED_BY_AUXILIARY_COUNT[countBuiltAuxiliaries()]);
            }
        };
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return Textures.BlockIcons.createTextureWithCasing(
            this,
            side,
            facing,
            active,
            Textures.BlockIcons.MACHINE_CASING_BRICKEDBLASTFURNACE_INACTIVE,
            Textures.BlockIcons.MACHINE_CASING_BRICKEDBLASTFURNACE_INACTIVE_GLOW,
            Textures.BlockIcons.MACHINE_CASING_BRICKEDBLASTFURNACE_ACTIVE,
            Textures.BlockIcons.MACHINE_CASING_BRICKEDBLASTFURNACE_ACTIVE_GLOW);
    }

    @Override
    public ITexture getCasingTexture() {
        return TextureFactory.of(Textures.BlockIcons.MACHINE_CASING_DENSEBRICKS);
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.large_steel_kiln.name";
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
    }

    @Override
    public TooltipStats getTooltipStats() {
        return new TooltipStats(1, 1, Double.NaN, false);
    }

    @Override
    public List<TieredPart> getTooltipTieredParts() {
        return List.of();
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of(
            "one_auxiliary_speed",
            SPEED_BY_AUXILIARY_COUNT[1],
            "two_auxiliaries_speed",
            SPEED_BY_AUXILIARY_COUNT[2]);
    }

    @Override
    public String getTooltipPath() {
        return "large-steel-kiln";
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return GTMod.proxy.mPollutionPrimitveBlastFurnacePerSecond;
    }

    @Override
    public boolean polluteEnvironment(int pollution) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile.getTimer() % 20 == 0) Pollution.addPollution(tile, getPollutionPerSecond(getControllerSlot()));
        return true;
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction != ForgeDirection.UNKNOWN && direction.offsetY == 0
            && rotation == Rotation.NORMAL
            && flip == Flip.NONE;
    }

    @Override
    protected boolean canUseControllerSlotForRecipe() {
        return false;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    protected boolean requiresMuffler() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public VoidingMode getDefaultVoidingMode() {
        return VoidingMode.VOID_NONE;
    }

    private static final class StructureHolder {

        private static final IStructureDefinition<LargeSteelKilnController> DEFINITION = createStructure();
    }
}
