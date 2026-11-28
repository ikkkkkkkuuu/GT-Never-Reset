package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksFlat;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
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
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.common.recipe.gtneverreset.PlatinumProcessingRecipes;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Mods;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.misc.GTStructureChannels;

@IMetaTileEntity.SkipGenerateDescription
public final class PlatinumProcessingPlantController extends GTNRMultiBlockBase<PlatinumProcessingPlantController>
    implements ISurvivalConstructable, ICasingTextureProvider {

    private static final String MAIN_RING = "main";
    private static final String RARE_EARTH_RING = "rare_earth";
    private static final String MONAZITE_RING = "monazite";
    private static final String NETHERITE_RING = "netherite";
    private static final String[] RING_PIECES = { MAIN_RING, RARE_EARTH_RING, MONAZITE_RING, NETHERITE_RING };
    private static final int HORIZONTAL_OFFSET = 12, VERTICAL_OFFSET = 21, DEPTH_OFFSET = 1;
    private static final int RING_SPACING = 7;
    private static final int RECIPE_THREADS = 10;
    private static final int PARALLELS_PER_THREAD = 2_048;
    private static final Casings HULL = Casings.ChemicallyInertMachineCasing;
    private static final Casings STEEL = Casings.SolidSteelMachineCasing;
    private static final Casings STAINLESS = Casings.CleanStainlessSteelMachineCasing;
    private static final Casings RARE_EARTH_HULL = Casings.RobustTungstenSteelMachineCasing;
    private static final Casings MONAZITE_HULL = Casings.VacuumCasing;
    private static final Casings NETHERITE_HULL = Casings.RadiantNaquadahAlloyCasing;
    private static final Casings[] REAR_HULLS = { RARE_EARTH_HULL, MONAZITE_HULL, NETHERITE_HULL };

    private int ringCount = 1;

    private static IStructureDefinition<PlatinumProcessingPlantController> createStructure() {
        IStructureElement<PlatinumProcessingPlantController> hull = ofBlocksFlat(
            Map.of(
                HULL.getBlock(),
                HULL.getBlockMeta(),
                STEEL.getBlock(),
                STEEL.getBlockMeta(),
                STAINLESS.getBlock(),
                STAINLESS.getBlockMeta()),
            HULL.getBlock(),
            HULL.getBlockMeta());
        var hatchBuilder = buildHatchAdder(PlatinumProcessingPlantController.class)
            .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy, Maintenance)
            .casingIndex(HULL.getTextureId())
            .hint(1);
        var hatchAdder = hatchBuilder.getAdder();
        return StructureDefinition.<PlatinumProcessingPlantController>builder()
            .addShape(MAIN_RING, transpose(MultiblockStructureLoader.load("platinum_processing_plant")))
            .addShape(RARE_EARTH_RING, transpose(MultiblockStructureLoader.load("platinum_gate_rare_earth_ring")))
            .addShape(MONAZITE_RING, transpose(MultiblockStructureLoader.load("platinum_gate_monazite_ring")))
            .addShape(NETHERITE_RING, transpose(MultiblockStructureLoader.load("platinum_gate_netherite_ring")))
            .addElement(
                'A',
                hatchBuilder
                    .adder(
                        (machine, hatch, texture) -> hatchAdder
                            .apply(machine, hatch, (short) casingBelow(machine.getBaseMetaTileEntity()).getTextureId()))
                    .buildAndChain(hull))
            .addElement('B', Casings.Firebricks.asElement())
            .addElement('C', hull)
            .addElement('D', STEEL.asElement())
            .addElement('F', MONAZITE_HULL.asElement())
            .addElement('H', RARE_EARTH_HULL.asElement())
            .addElement(
                'K',
                GTStructureChannels.HEATING_COIL.use(
                    activeCoils(
                        ofCoil((machine, level) -> { machine.mCoilLevel = level; }, machine -> machine.mCoilLevel))))
            .addElement('M', Muffler.newAny(MONAZITE_HULL.getTextureId(), 2))
            .addElement('N', NETHERITE_HULL.asElement())
            .addElement('S', STAINLESS.asElement())
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
        int rings = Math.clamp(stack.stackSize, 1, getMaxRingCount());
        ItemStack trigger = ringBuildTrigger(stack);
        for (int ring = hintsOnly || !mMachine ? 0 : ringCount; ring < rings; ring++) {
            buildPiece(
                RING_PIECES[ring],
                trigger,
                hintsOnly,
                HORIZONTAL_OFFSET,
                VERTICAL_OFFSET,
                DEPTH_OFFSET - ring * RING_SPACING);
        }
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment environment) {
        int rings = Math.clamp(stack.stackSize, 1, getMaxRingCount());
        ItemStack trigger = ringBuildTrigger(stack);
        for (int ring = mMachine ? ringCount : 0; ring < rings; ring++) {
            int built = survivalBuildPiece(
                RING_PIECES[ring],
                trigger,
                HORIZONTAL_OFFSET,
                VERTICAL_OFFSET,
                DEPTH_OFFSET - ring * RING_SPACING,
                elementBudget,
                environment,
                false,
                true);
            if (built >= 0) return built;
        }
        return -1;
    }

    private static ItemStack ringBuildTrigger(ItemStack stack) {
        if (stack.stackSize == 1 || ChannelDataAccessor.hasSubChannel(stack, GTStructureChannels.HEATING_COIL.get()))
            return stack;
        ItemStack trigger = stack.copy();
        trigger.stackSize = 1;
        return trigger;
    }

    @Override
    protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        ringCount = 1;
        if (!checkPiece(MAIN_RING, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) return;

        int lastRing = getMaxRingCount() - 1;
        while (lastRing > 0 && !hasRingAnchor(tile, lastRing)) lastRing--;
        for (int ring = 1; ring <= lastRing; ring++) {
            if (!hasRingAnchor(tile, ring)) {
                // #tr gtnr.structure.platinum_gate.requires_previous_rings
                // # Build the preceding rings before adding rear rings.
                // # zh_CN 请先建好前面的环，再向后扩建。
                errors.add(StructureErrors.of("gtnr.structure.platinum_gate.requires_previous_rings"));
                return;
            }
            if (!checkPiece(
                RING_PIECES[ring],
                HORIZONTAL_OFFSET,
                VERTICAL_OFFSET,
                DEPTH_OFFSET - ring * RING_SPACING,
                errors)) return;
            ringCount = ring + 1;
        }
    }

    private boolean hasRingAnchor(IGregTechTileEntity tile, int ring) {
        Casings casing = REAR_HULLS[ring - 1];
        ForgeDirection back = getExtendedFacing().getRelativeBackInWorld();
        int distance = ring * RING_SPACING;
        int x = tile.getXCoord() + distance * back.offsetX;
        int y = tile.getYCoord();
        int z = tile.getZCoord() + distance * back.offsetZ;
        World world = tile.getWorld();
        return world.blockExists(x, y, z) && world.getBlock(x, y, z) == casing.getBlock()
            && world.getBlockMetadata(x, y, z) == casing.getBlockMeta();
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, InputHatch, 1);
        checkHatchMin(errors, OutputBus, 1);
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
        ((PlatinumProcessingLogic) logic).limitPowerToThread();
        logic.setBatchSize(1);
        logic.setRecipeLocking(this, false);
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
        return casingBelow(getBaseMetaTileEntity()).getCasingTexture();
    }

    private static Casings casingBelow(IGregTechTileEntity tile) {
        if (tile == null || tile.getWorld() == null || tile.getYCoord() <= 0) return HULL;
        World world = tile.getWorld();
        int x = tile.getXCoord(), y = tile.getYCoord() - 1, z = tile.getZCoord();
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (block == STEEL.getBlock() && meta == STEEL.getBlockMeta()) return STEEL;
        if (block == STAINLESS.getBlock() && meta == STAINLESS.getBlockMeta()) return STAINLESS;
        return HULL;
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
    public byte getUpdateData() {
        return (byte) ringCount;
    }

    @Override
    public void onValueUpdate(byte value) {
        ringCount = Math.clamp(value, 1, getMaxRingCount());
    }

    public int getRingCount() {
        return ringCount;
    }

    public int getMaxRingCount() {
        return Mods.EtFuturumRequiem.isModLoaded() ? RING_PIECES.length : RING_PIECES.length - 1;
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
        return RECIPE_THREADS;
    }

    @Override
    protected int getBaseParallelRecipes() {
        return PARALLELS_PER_THREAD;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean getDefaultInputSeparationMode() {
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

        // #tr GT5U.gui.text.recipe_result.gtnr.platinum_gate.needs_rare_earth_ring
        // # Requires the rare-earth ring
        // # zh_CN 需要稀土处理环
        private static final CheckRecipeResult NEEDS_RARE_EARTH_RING = SimpleCheckRecipeResult
            .ofFailure("gtnr.platinum_gate.needs_rare_earth_ring");
        // #tr GT5U.gui.text.recipe_result.gtnr.platinum_gate.needs_monazite_ring
        // # Requires the monazite ring
        // # zh_CN 需要独居石处理环
        private static final CheckRecipeResult NEEDS_MONAZITE_RING = SimpleCheckRecipeResult
            .ofFailure("gtnr.platinum_gate.needs_monazite_ring");
        // #tr GT5U.gui.text.recipe_result.gtnr.platinum_gate.needs_netherite_ring
        // # Requires the netherite ring
        // # zh_CN 需要下界合金处理环
        private static final CheckRecipeResult NEEDS_NETHERITE_RING = SimpleCheckRecipeResult
            .ofFailure("gtnr.platinum_gate.needs_netherite_ring");
        // #tr GT5U.gui.text.recipe_result.gtnr.platinum_gate.digester_exhaust
        // # The digester needs an unobstructed muffler
        // # zh_CN 消化工序需要排气口畅通的消声仓
        private static final CheckRecipeResult DIGESTER_EXHAUST_BLOCKED = SimpleCheckRecipeResult
            .ofFailure("gtnr.platinum_gate.digester_exhaust");
        private int selectedStation;

        @Override
        @Nonnull
        protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
            int station = recipe.getMetadataOrDefault(PlatinumProcessingRecipes.STATION, -1);
            if (station < 0 || station >= PlatinumProcessingRecipes.STATION_COUNT) {
                return CheckRecipeResultRegistry.NO_RECIPE;
            }
            int needed = recipe.getMetadataOrDefault(PlatinumProcessingRecipes.RING_REQUIREMENT, 1);
            if (getRingCount() < needed) return switch (needed) {
                case 2 -> NEEDS_RARE_EARTH_RING;
                case 3 -> NEEDS_MONAZITE_RING;
                default -> NEEDS_NETHERITE_RING;
            };
            int furnaceHeat = recipe.getMetadataOrDefault(COIL_HEAT, 0);
            if (furnaceHeat > getFurnaceHeat()) return CheckRecipeResultRegistry.insufficientHeat(furnaceHeat);
            int digesterHeat = recipe.getMetadataOrDefault(PlatinumProcessingRecipes.DIGESTER_HEAT, 0);
            if (digesterHeat > mCoilLevel.getHeat()) {
                return CheckRecipeResultRegistry.insufficientHeat(digesterHeat);
            }
            if (digesterHeat > 0 && !hasClearDigesterExhaust()) return DIGESTER_EXHAUST_BLOCKED;
            var result = super.validateRecipe(recipe);
            if (!result.wasSuccessful()) return result;
            selectedStation = station;
            return result;
        }

        @Override
        protected int getThreadStation() {
            return selectedStation;
        }

        private void limitPowerToThread() {
            long power = availableThreadPower();
            long voltage = Math.min(availableVoltage, power);
            setAvailableVoltage(voltage);
            setAvailableAmperage(voltage == 0 ? 0 : Math.min(availableAmperage, power / voltage));
        }

        @Override
        @Nonnull
        protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
            return super.createParallelHelper(recipe).setAvailableEUt(availableVoltage * availableAmperage);
        }

        @Override
        @Nonnull
        protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
            // Keep the native base duration and prevent sub-tick overclocks from expanding the parallel limit.
            return super.createOverclockCalculator(recipe)
                .setDurationUnderOneTickSupplier(() -> recipe.mDuration * speedBoost);
        }

        private boolean hasClearDigesterExhaust() {
            for (var hatch : mMufflerHatches) {
                IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
                if (tile != null && tile.getAirAtSide(tile.getFrontFacing())) return true;
            }
            return false;
        }

    }

    private static final class StructureHolder {

        private static final IStructureDefinition<PlatinumProcessingPlantController> DEFINITION = createStructure();
    }
}
