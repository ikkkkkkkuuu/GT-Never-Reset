package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectionBackend;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;

@IMetaTileEntity.SkipGenerateDescription
public final class LargeFragmentWorldCollector extends CrossRecipeMultiblockBase<LargeFragmentWorldCollector>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String PIECE = "main";
    private static final int OFFSET_X = 1, OFFSET_Y = 3, OFFSET_Z = 0;
    private static final Casings CASING = Casings.StableTitaniumMachineCasing;
    private static final int RECIPE_THREADS = 64;
    private static final int SPEED_MULTIPLIER = 4;
    private static final int ENERGY_MULTIPLIER = 256;
    private static final IHatchElement<MTEMultiBlockBase> ENERGY_INPUT = Energy.or(ExoticEnergy);
    private static final String FRONT_PATH = "large_fragment_world_collector/overlay_front";
    private static final IIconContainer FRONT = Textures.BlockIcons.custom(GTNeverReset.MODID, FRONT_PATH);
    private static final IIconContainer FRONT_EMISSIVE = Textures.BlockIcons
        .custom(GTNeverReset.MODID, FRONT_PATH + "_emissive");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(GTNeverReset.MODID, FRONT_PATH + "_active");
    private static final IIconContainer FRONT_ACTIVE_EMISSIVE = Textures.BlockIcons
        .custom(GTNeverReset.MODID, FRONT_PATH + "_active_emissive");
    private static final IIconContainer FRONT_PAUSED = Textures.BlockIcons
        .custom(GTNeverReset.MODID, FRONT_PATH + "_paused");
    private static final IIconContainer FRONT_PAUSED_EMISSIVE = Textures.BlockIcons
        .custom(GTNeverReset.MODID, FRONT_PATH + "_paused_emissive");

    private static IStructureDefinition<LargeFragmentWorldCollector> createStructure() {
        int texture = CASING.getTextureId();
        Map<IHatchElement<? super LargeFragmentWorldCollector>, Integer> casingHatches = new LinkedHashMap<>();
        casingHatches.put(ENERGY_INPUT, 1);
        casingHatches.put(InputBus, 0);
        casingHatches.put(InputHatch, 0);
        casingHatches.put(OutputHatch, 0);
        return StructureDefinition.<LargeFragmentWorldCollector>builder()
            .addShape(PIECE, transpose(MultiblockStructureLoader.load("large_fragment_world_collector")))
            .addElement(
                'X',
                buildHatchAdder(LargeFragmentWorldCollector.class).atLeast(casingHatches)
                    .casingIndex(texture)
                    .hint(1)
                    .buildAndChain(CASING.asElement()))
            .addElement(
                'I',
                buildHatchAdder(LargeFragmentWorldCollector.class).anyOf(InputBus)
                    .exclusive()
                    .casingIndex(texture)
                    .hint(2)
                    .build())
            .addElement(
                'O',
                buildHatchAdder(LargeFragmentWorldCollector.class).anyOf(OutputBus)
                    .exclusive()
                    .casingIndex(texture)
                    .hint(3)
                    .build())
            .build();
    }

    public LargeFragmentWorldCollector(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private LargeFragmentWorldCollector(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeFragmentWorldCollector(mName);
    }

    @Override
    public IStructureDefinition<LargeFragmentWorldCollector> getStructureDefinition() {
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
        checkHatchRange(errors, ENERGY_INPUT, 1, 1);
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return List.of(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new CrossRecipeProcessingLogic(this) {

            @Override
            @Nonnull
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                return super.createOverclockCalculator(recipe).setNoOverclock(true)
                    .setDuration(Math.max(1, recipe.mDuration / SPEED_MULTIPLIER))
                    .setDurationModifier(1);
            }

            @Override
            @Nonnull
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (!GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getBackend()
                    .matchesCollectorInputs(recipe, inputItems, inputFluids))
                    return CheckRecipeResultRegistry.NO_RECIPE;
                return super.validateRecipe(recipe);
            }

            @Override
            @Nonnull
            protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
                ParallelHelper helper = super.createParallelHelper(recipe);
                // A recipe lock's consumption ledger excludes reusable markers, circuits and drills.
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
        // Reusable selectors leave buses stocked after a start. Rotate them to give each bus a turn.
        if (result.wasSuccessful() && mInputBusses.size() > 1) Collections.rotate(mInputBusses, -1);
        return result;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        if (side != facing) return new ITexture[] { getCasingTexture() };
        IIconContainer overlay = active ? FRONT_ACTIVE : FRONT;
        IIconContainer emissive = active ? FRONT_ACTIVE_EMISSIVE : FRONT_EMISSIVE;
        if (tile != null && !tile.isAllowedToWork()) {
            overlay = FRONT_PAUSED;
            emissive = FRONT_PAUSED_EMISSIVE;
        }
        return new ITexture[] { getCasingTexture(), TextureFactory.builder()
            .addIcon(overlay)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(emissive)
                .extFacing()
                .glow()
                .build() };
    }

    @Override
    public ITexture getCasingTexture() {
        return CASING.getCasingTexture();
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.large_fragment_world_collector.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public String getTooltipPath() {
        return "large-fragment-world-collector";
    }

    @Override
    public int getMaxRecipeThreads() {
        return RECIPE_THREADS;
    }

    @Override
    protected double getBaseSpeedMultiplier() {
        return SPEED_MULTIPLIER;
    }

    @Override
    protected double getBaseEUtMultiplier() {
        return ENERGY_MULTIPLIER;
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return 0;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return false;
    }

    @Override
    protected boolean canUseControllerSlotForRecipe() {
        return false;
    }

    private static final class StructureHolder {

        private static final IStructureDefinition<LargeFragmentWorldCollector> DEFINITION = createStructure();
    }
}
