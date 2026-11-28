package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.machine.hatch.FragmentDimensionHatch;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectionBackend;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectorRecipes;
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
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.tileentities.machines.IDualInputHatch;

@IMetaTileEntity.SkipGenerateDescription
public final class LargeFragmentWorldCollector extends CrossRecipeMultiblockBase<LargeFragmentWorldCollector>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String PIECE = "main";
    private static final int OFFSET_X = 1, OFFSET_Y = 3, OFFSET_Z = 0;
    private static final Casings CASING = Casings.StableTitaniumMachineCasing;
    private static final int RECIPE_THREADS = 64;
    private static final double SPEED_MULTIPLIER = 1.5;
    private static final int ENERGY_MULTIPLIER = 64;
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
        var ports = ofChain(
            StructureTooltipHooks.describeAsHatch(
                ofHatchAdder(LargeFragmentWorldCollector::addDimensionHatch, texture, 1),
                FragmentDimensionHatch.NAME_KEY),
            StructureTooltipHooks.describeAsHatch(
                ofHatchAdder(LargeFragmentWorldCollector::addParameterModuleToMachineList, texture, 1),
                ParameterModuleHatch.NAME_KEY),
            buildHatchAdder(LargeFragmentWorldCollector.class).atLeast(casingHatches)
                .casingIndex(texture)
                .hint(1)
                .buildAndChain(CASING.asElement()));
        return StructureDefinition.<LargeFragmentWorldCollector>builder()
            .addShape(PIECE, transpose(MultiblockStructureLoader.load("large_fragment_world_collector")))
            .addElement('X', ports)
            .addElement(
                'I',
                ofChain(
                    StructureTooltipHooks.describeAsHatch(
                        ofHatchAdder(LargeFragmentWorldCollector::addDimensionHatch, texture, 2),
                        FragmentDimensionHatch.NAME_KEY),
                    buildHatchAdder(LargeFragmentWorldCollector.class).anyOf(InputBus)
                        .exclusive()
                        .casingIndex(texture)
                        .hint(2)
                        .build()))
            .addElement(
                'O',
                buildHatchAdder(LargeFragmentWorldCollector.class).anyOf(OutputBus)
                    .exclusive()
                    .casingIndex(texture)
                    .hint(3)
                    .build())
            .build();
    }

    private FragmentDimensionHatch dimensionHatch;
    private List<GTRecipe> lastDimensionRecipes = List.of();
    private int nextDimensionRecipe;

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
    public void clearHatches() {
        super.clearHatches();
        dimensionHatch = null;
    }

    private boolean addDimensionHatch(IGregTechTileEntity tile, int texture) {
        if (dimensionHatch != null || !(tile.getMetaTileEntity() instanceof FragmentDimensionHatch hatch)) return false;
        hatch.updateTexture(texture);
        addIfSmartInput(hatch);
        dimensionHatch = hatch;
        return true;
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return List.of(GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new CollectionProcessingLogic();
    }

    @Override
    protected CheckRecipeResult doCheckRecipe() {
        if (dimensionHatch != null) return checkDimensionRecipes((CollectionProcessingLogic) processingLogic);
        CheckRecipeResult result = super.doCheckRecipe();
        // Reusable selectors leave buses stocked after a start. Rotate them to give each bus a turn.
        if (result.wasSuccessful() && mInputBusses.size() > 1) Collections.rotate(mInputBusses, -1);
        return result;
    }

    @Override
    protected List<IDualInputHatch> getRecipePatternInputs() {
        return dimensionHatch == null ? super.getRecipePatternInputs() : List.of();
    }

    private CheckRecipeResult checkDimensionRecipes(CollectionProcessingLogic logic) {
        if (!dimensionHatch.isValid()) return CheckRecipeResultRegistry.NO_RECIPE;
        ItemStack marker = dimensionHatch.getDimensionMarker();
        FragmentWorldCollectionBackend backend = GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getBackend();
        List<GTRecipe> recipes = backend.getDimensionRecipes(marker);
        if (recipes != lastDimensionRecipes) {
            lastDimensionRecipes = recipes;
            nextDimensionRecipe = 0;
        }
        if (recipes.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;

        List<ItemStack> available = mInputBusses.isEmpty() ? List.of() : getStoredInputs();
        ItemStack drill = null;
        int wear = Integer.MAX_VALUE;
        for (ItemStack input : available) {
            if (input.stackSize <= 0) continue;
            int candidateWear = backend.getDrillWear(input);
            if (candidateWear > 0 && candidateWear < wear) {
                drill = input;
                wear = candidateWear;
            }
        }

        CheckRecipeResult result = CheckRecipeResultRegistry.NO_RECIPE;
        for (int attempt = 0; attempt < recipes.size(); attempt++) {
            GTRecipe recipe = recipes.get(nextDimensionRecipe);
            nextDimensionRecipe = (nextDimensionRecipe + 1) % recipes.size();
            boolean fluidCollection = FragmentWorldCollectionBackend.isFluidCollection(recipe);
            if (fluidCollection != (drill != null)) continue;
            if (fluidCollection
                && !GTUtility.areStacksEqual(drill, recipe.mInputs[FragmentWorldCollectorRecipes.DRILL_INPUT_INDEX]))
                continue;

            List<ItemStack> inputs = FragmentWorldCollectionBackend.getDimensionRecipeInputs(recipe, marker, available);
            FluidStack[] fluids = recipe.mFluidInputs.length == 0 ? null : getStoredFluids().toArray(new FluidStack[0]);
            CheckRecipeResult attemptResult = logic.processDimensionRecipe(recipe, inputs, fluids);
            if (attemptResult.wasSuccessful()) return attemptResult;
            if (attemptResult != CheckRecipeResultRegistry.NO_RECIPE) result = attemptResult;
        }
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
    protected MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltip(this)
            .toolTipFinisher();
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
        return type == MachineModuleType.Overclock ? 0 : super.getParameterModuleLimit(type);
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

    private final class CollectionProcessingLogic extends CrossRecipeProcessingLogic {

        private GTRecipe dimensionRecipe;

        private CollectionProcessingLogic() {
            super(LargeFragmentWorldCollector.this);
        }

        private CheckRecipeResult processDimensionRecipe(GTRecipe recipe, List<ItemStack> inputs, FluidStack[] fluids) {
            dimensionRecipe = recipe;
            try {
                setRecipeLocking(LargeFragmentWorldCollector.this, false);
                setInputItems(inputs);
                setInputFluids(fluids);
                return process();
            } finally {
                dimensionRecipe = null;
            }
        }

        @Override
        @Nonnull
        protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> map) {
            return dimensionRecipe == null ? super.findRecipeMatches(map) : Stream.of(dimensionRecipe);
        }

        @Override
        @Nonnull
        protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
            return super.createOverclockCalculator(recipe).setNoOverclock(true);
        }

        @Override
        @Nonnull
        protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
            if (!GTNRRecipeMaps.FRAGMENT_WORLD_COLLECTION_RECIPES.getBackend()
                .matchesCollectorInputs(recipe, inputItems, inputFluids)) return CheckRecipeResultRegistry.NO_RECIPE;
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
            FragmentWorldCollectionBackend
                .consumeDrillHeads(recipe, inputItems, getCurrentParallels(), getBaseMetaTileEntity()::getRandomNumber);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
    }
}
