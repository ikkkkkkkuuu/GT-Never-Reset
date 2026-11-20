package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectionBackend;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Mods;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;

@IMetaTileEntity.SkipGenerateDescription
public final class StarCoreStripper extends GTNRMultiBlockBase<StarCoreStripper> implements ISurvivalConstructable {

    private static final String PIECE = "main";
    private static final int OFFSET_X = 61, OFFSET_Y = 11, OFFSET_Z = 2;
    private static final Casings CASING = Casings.AdvancedIridiumPlatedMachineCasing;

    private static IStructureDefinition<StarCoreStripper> createStructure() {
        int texture = CASING.getTextureId();
        var ports = ofChain(
            StructureTooltipHooks.describeAsHatch(
                ofHatchAdder(StarCoreStripper::addParameterModuleToMachineList, texture, 1),
                MachineModuleType.Parallel.getTranslationKey()),
            buildHatchAdder(StarCoreStripper.class)
                .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Maintenance, Energy.or(ExoticEnergy))
                .casingIndex(texture)
                .hint(1)
                .buildAndChain(CASING.asElement()));
        return StructureDefinition.<StarCoreStripper>builder()
            .addShape(PIECE, transpose(MultiblockStructureLoader.load("star_core_stripper")))
            .addElement('X', ports)
            .addElement('A', CASING.asElement())
            .addElement('B', Casings.FusionMachineCasingMKIII.asElement())
            .addElement('D', Casings.FusionCoilBlock.asElement())
            .addElement('E', Casings.RadiantNaquadahAlloyCasing.asElement())
            .addElement('F', Casings.TungstensteelPipeCasing.asElement())
            .addElement('G', Casings.ElectronPermeableNeutroniumCoatedGlass.asElement())
            .addElement('I', lamp(0))
            .addElement('J', lamp(9))
            .addElement('K', lamp(1))
            .addElement('L', lamp(4))
            .build();
    }

    private static IStructureElement<StarCoreStripper> lamp(int color) {
        Block lamp = GameRegistry.findBlock(Mods.Chisel.ID, "antiBlock");
        return lamp == null ? ofBlock(Blocks.stained_glass, color) : ofBlock(lamp, 15 - color);
    }

    public StarCoreStripper(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private StarCoreStripper(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new StarCoreStripper(mName);
    }

    @Override
    public IStructureDefinition<StarCoreStripper> getStructureDefinition() {
        return StructureHolder.DEFINITION;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(PIECE, stack, OFFSET_X, OFFSET_Y, OFFSET_Z, elementBudget, environment, false, true);
    }

    @Override
    protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMax(errors, Maintenance, 1);
        checkHatchMax(errors, InputBus, 4);
        checkHatchMax(errors, OutputBus, 4);
        checkHatchMax(errors, InputHatch, 4);
        checkHatchMax(errors, OutputHatch, 4);
        int lasers = (int) mExoticEnergyHatches.stream()
            .filter(hatch -> hatch.getConnectionType() == MTEHatch.ConnectionType.LASER)
            .count();
        checkPowerLimit(errors, Energy, mEnergyHatches.size() + mExoticEnergyHatches.size() - lasers, 2);
        checkPowerLimit(errors, ExoticEnergy, lasers, 1);
    }

    private void checkPowerLimit(List<StructureError> errors, HatchElement element, int count, int max) {
        if (!StructureTooltipHooks.onCheckHatches(this, 0, max, element) && count > max)
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, element, count, max));
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES;
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new RecipeThreadProcessingLogic() {

            @Override
            @Nonnull
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                return super.createOverclockCalculator(recipe).setMaxOverclocks(0);
            }

            @Override
            @Nonnull
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                // Cached patterns and recipe locks must still supply the selected marker, circuit and drill.
                return GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getBackend()
                    .matchesCollectorInputs(recipe, inputItems, inputFluids) ? CheckRecipeResultRegistry.SUCCESSFUL
                        : CheckRecipeResultRegistry.NO_RECIPE;
            }

            @Override
            @Nonnull
            protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
                ParallelHelper helper = super.createParallelHelper(recipe);
                // The lock records consumed amounts; reusable selectors do not appear in that ledger.
                if (isRecipeLocked && getSingleRecipeCheck() != null) helper.setRecipeLocked(null, false);
                return helper;
            }

            @Override
            @Nonnull
            protected CheckRecipeResult onRecipeStart(@Nonnull GTRecipe recipe) {
                FragmentWorldCollectionBackend.consumeDrillHeads(
                    recipe,
                    inputItems,
                    getCurrentParallels(),
                    getBaseMetaTileEntity()::getRandomNumber);
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        };
    }

    @Override
    protected CheckRecipeResult doCheckRecipe() {
        CheckRecipeResult result = super.doCheckRecipe();
        // Non-consumed markers can keep the first bus stocked forever. Give the next bus the next thread.
        if (result.wasSuccessful() && mInputBusses.size() > 1) Collections.rotate(mInputBusses, -1);
        return result;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(CASING.getTextureId());
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_EXTRACTOR_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_EXTRACTOR)
            .extFacing()
            .build() };
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.star_core_stripper.name";
    }

    @Override
    public String getTooltipPath() {
        return "star-core-stripper";
    }

    @Override
    public int getMaxRecipeThreads() {
        return 128;
    }

    @Override
    protected boolean usesInstantThreadFill() {
        return true;
    }

    @Override
    protected double getBaseSpeedMultiplier() {
        return 4;
    }

    @Override
    protected double getBaseEUtMultiplier() {
        return 256;
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return type == MachineModuleType.Parallel ? 1 : 0;
    }

    @Override
    protected boolean canUseControllerSlotForRecipe() {
        return false;
    }

    private static final class StructureHolder {

        private static final IStructureDefinition<StarCoreStripper> DEFINITION = createStructure();
    }
}
