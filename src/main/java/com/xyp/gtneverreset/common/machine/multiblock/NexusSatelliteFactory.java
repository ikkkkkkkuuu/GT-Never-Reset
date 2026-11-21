package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTRecipeConstants.CLEANROOM;
import static gregtech.api.util.GTRecipeConstants.LOW_GRAVITY;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.math.BigInteger;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.NexusSatelliteFactoryGui;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.GTMod;
import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ICleanroom;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;

@IMetaTileEntity.SkipGenerateName
public final class NexusSatelliteFactory extends CrossRecipeMultiblockBase<NexusSatelliteFactory>
    implements ISurvivalConstructable {

    public static final long MIN_VOLTAGE = 2_000_000_000L;
    private static final int[] THREADS = { 32, 2_048, 131_072, 8_388_608 };
    private static final int[] PARALLELS = { 2_048, 131_072, 8_388_608, 2_147_483_643 };
    private static final double[] EU_MULTIPLIERS = { 0.9, 0.8, 0.7, 0.6 };
    private static final IIconContainer FRONT = Textures.BlockIcons
        .custom(GTNeverReset.MODID, "lighthunter/module_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(GTNeverReset.MODID, "lighthunter/module_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(GTNeverReset.MODID, "lighthunter/module_front_active_glow");
    private static final IIconContainer FRONT_IDLE_GLOW = Textures.BlockIcons
        .customOptional(GTNeverReset.MODID, "lighthunter/module_front_glow");
    private static IStructureDefinition<NexusSatelliteFactory> definition;
    private final int factoryType;
    private LightHunterSpaceStation host;
    private long nextHostScan;

    private final int grade;
    private final String nameKey;
    private long voltage = MIN_VOLTAGE;

    public NexusSatelliteFactory(int id, String name, String regionalName, String nameKey, int factoryType, int grade) {
        super(id, name, regionalName);
        this.nameKey = nameKey;
        this.factoryType = factoryType;
        this.grade = grade;
    }

    private NexusSatelliteFactory(String name, String nameKey, int factoryType, int grade) {
        super(name);
        this.nameKey = nameKey;
        this.factoryType = factoryType;
        this.grade = grade;
    }

    @Override
    public String getLocalNameKey() {
        return nameKey;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new NexusSatelliteFactory(mName, nameKey, factoryType, grade);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public IStructureDefinition<NexusSatelliteFactory> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<NexusSatelliteFactory>builder()
                .addShape("main", transpose(MultiblockStructureLoader.load("light_hunter_space_station_module")));
            for (char symbol : "ABCDEFGHIJKLMN".toCharArray()) {
                IStructureElement<NexusSatelliteFactory> element = LightHunterStructure.satellite(symbol);
                builder.addElement(symbol, symbol == 'E' ? GTStructureUtility.activeCoils(element) : element);
            }
            builder.addElement(
                'O',
                buildHatchAdder(NexusSatelliteFactory.class)
                    .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Maintenance)
                    .casingIndex(Casings.HighPowerCasing.getTextureId())
                    .hint(1)
                    .buildAndChain(LightHunterStructure.satellite('O')));
            definition = builder.build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 2, 10, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 2, 10, 0, budget, env, false, true);
    }

    @Override
    protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece("main", 2, 10, 0, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMin(errors, Maintenance, 1);
        checkHatchMax(errors, Maintenance, 1);
    }

    public LightHunterSpaceStation getHost() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || tile.getWorld() == null || !tile.isServerSide()) return null;
        if (host != null && host.acceptsModule(this)) return host;
        host = null;
        long tick = tile.getWorld()
            .getTotalWorldTime();
        if (tick < nextHostScan) return null;
        nextHostScan = tick + 20;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH,
            ForgeDirection.WEST, ForgeDirection.EAST })
            for (int side : new int[] { -9, 9 }) for (int distance = 41; distance <= 113; distance += 8) {
                int[] offset = LightHunterSpaceStation.moduleOffset(facing, distance, side);
                int x = tile.getXCoord() - offset[0], z = tile.getZCoord() - offset[2], y = tile.getYCoord();
                if (!tile.getWorld()
                    .blockExists(x, y, z)) continue;
                TileEntity candidate = tile.getWorld()
                    .getTileEntity(x, y, z);
                if (candidate instanceof IGregTechTileEntity gt
                    && gt.getMetaTileEntity() instanceof LightHunterSpaceStation station
                    && station.acceptsModule(this)) return host = station;
            }
        return null;
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return switch (factoryType) {
            case 1 -> List.of(
                GTNRRecipeMaps.LightHunterRockBreakingRecipes,
                RecipeMaps.oreWasherRecipes,
                RecipeMaps.centrifugeNonCellRecipes,
                RecipeMaps.electrolyzerNonCellRecipes,
                RecipeMaps.sifterRecipes,
                RecipeMaps.maceratorRecipes,
                RecipeMaps.chemicalDehydratorNonCellRecipes,
                RecipeMaps.thermalCentrifugeRecipes,
                RecipeMaps.electroMagneticSeparatorRecipes);
            case 2 -> List.of(
                RecipeMaps.chemicalDehydratorNonCellRecipes,
                RecipeMaps.autoclaveRecipes,
                RecipeMaps.extractorRecipes,
                RecipeMaps.fluidExtractionRecipes,
                RecipeMaps.brewingRecipes,
                RecipeMaps.fermentingRecipes,
                RecipeMaps.distilleryRecipes,
                RecipeMaps.distillationTowerRecipes,
                RecipeMaps.fluidHeaterRecipes,
                RecipeMaps.fluidSolidifierRecipes,
                RecipeMaps.chemicalBathRecipes);
            case 3 -> List.of(
                RecipeMaps.cannerRecipes,
                RecipeMaps.arcFurnaceRecipes,
                GTNRRecipeMaps.LightningProcessorRecipes,
                RecipeMaps.assemblerRecipes,
                GoodGeneratorRecipeMaps.preciseAssemblerRecipes,
                RecipeMaps.circuitAssemblerRecipes);
            default -> List.of(
                RecipeMaps.latheRecipes,
                RecipeMaps.benderRecipes,
                RecipeMaps.compressorRecipes,
                RecipeMaps.hammerRecipes,
                RecipeMaps.cutterRecipes,
                RecipeMaps.extruderRecipes,
                RecipeMaps.mixerNonCellRecipes,
                RecipeMaps.wiremillRecipes,
                RecipeMaps.formingPressRecipes,
                RecipeMaps.polarizerRecipes);
        };
    }

    @Override
    public boolean getDefaultBatchMode() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    protected CheckRecipeResult doCheckRecipe() {
        if (stationBudget().signum() <= 0) {
            // #tr GT5U.gui.text.recipe_result.gtnr_light_hunter_power
            // # Requires a powered Light Hunter station
            // # zh_CN 需要已供电的猎光号主站
            return SimpleCheckRecipeResult.ofFailure("gtnr_light_hunter_power");
        }
        return super.doCheckRecipe();
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new CrossRecipeProcessingLogic(this) {

            @Override
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                // Retain GT overclocking down to one tick without converting excess voltage into extra parallels.
                return super.createOverclockCalculator(recipe)
                    .setDurationUnderOneTickSupplier(() -> Math.max(1D, recipe.mDuration / getSpeedMultiplier()));
            }

            @Override
            @Nonnull
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                CheckRecipeResult patternCheck = super.validateRecipe(recipe);
                if (!patternCheck.wasSuccessful()) return patternCheck;
                if (recipe.mSpecialValue == -200 || recipe.mSpecialValue == -300
                    || recipe.getMetadataOrDefault(CLEANROOM, false)) {
                    ICleanroom room = cleanroomReference.getCleanroom();
                    if (room == null || !room.isValidCleanroom() || room.getCleanness() <= 0)
                        return SimpleCheckRecipeResult.ofFailure("cleanroom");
                }
                if (GTMod.proxy.mLowGravProcessing
                    && (recipe.mSpecialValue == -100 || recipe.mSpecialValue == -300
                        || recipe.getMetadataOrDefault(LOW_GRAVITY, false))
                    && !MTEBasicMachine
                        .isValidForLowGravity(recipe, getBaseMetaTileEntity().getWorld().provider.dimensionId)) {
                    // #tr GT5U.gui.text.recipe_result.gtnr_light_hunter_low_gravity
                    // # Requires a low-gravity environment
                    // # zh_CN 需要低重力环境
                    return SimpleCheckRecipeResult.ofFailure("gtnr_light_hunter_low_gravity");
                }
                if (getRecipeMap() == GoodGeneratorRecipeMaps.preciseAssemblerRecipes && recipe.mSpecialValue > 4)
                    return CheckRecipeResultRegistry.insufficientMachineTier(recipe.mSpecialValue);
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        };
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        LightHunterSpaceStation station = getHost();
        return station != null && station.supply(lEUt == -Long.MAX_VALUE ? activeDemand() : BigInteger.valueOf(-lEUt));
    }

    private BigInteger activeDemand() {
        long total = usedThreadPower();
        if (total < Long.MAX_VALUE) return BigInteger.valueOf(total);
        BigInteger demand = BigInteger.ZERO;
        int threads = getTotalRecipeThreads();
        for (var task : getRecipeThreadTasks()) {
            if (task.getThread() < threads && !task.isFinished())
                demand = demand.add(BigInteger.valueOf(task.getEUt()));
        }
        return demand;
    }

    @Override
    public long getMaxInputVoltage() {
        return voltage;
    }

    @Override
    public long getMachineVoltageLimit() {
        return voltage;
    }

    private BigInteger stationBudget() {
        LightHunterSpaceStation station = getHost();
        return station == null ? BigInteger.ZERO : station.availableSatellitePower();
    }

    @Override
    public long getMaxInputEu() {
        return isTooltipProbeActive() ? super.getMaxInputEu() : asPositiveLong(stationBudget());
    }

    @Override
    protected long remainingPower() {
        // GT calculates one recipe at a time using longs. All simultaneous threads share the grid's full balance.
        return asPositiveLong(stationBudget().subtract(activeDemand()));
    }

    @Override
    protected long availableThreadPower() {
        // A vacant thread consumes no share of the station's budget.
        int parallels = getMaxParallelRecipes();
        long maximum = voltage > Long.MAX_VALUE / parallels ? Long.MAX_VALUE : voltage * parallels;
        return Math.min(maximum, remainingPower());
    }

    private static long asPositiveLong(BigInteger amount) {
        return amount.signum() <= 0 ? 0
            : amount.min(BigInteger.valueOf(Long.MAX_VALUE))
                .longValue();
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return 0;
    }

    @Override
    protected ParameterModuleHatch parameterModule(MachineModuleType type) {
        LightHunterSpaceStation station = getHost();
        return station == null ? null : station.getSharedParameterHatch(type);
    }

    @Override
    protected int getThreadModuleBonus() {
        ParameterModuleHatch hatch = parameterModule(MachineModuleType.Thread);
        return hatch == null ? 0 : hatch.getParameter();
    }

    @Override
    public int getMaxRecipeThreads() {
        return THREADS[grade];
    }

    @Override
    protected int getBaseParallelRecipes() {
        return PARALLELS[grade];
    }

    @Override
    protected double getBaseEUtMultiplier() {
        return EU_MULTIPLIERS[grade];
    }

    public long getVoltage() {
        return voltage;
    }

    public void setVoltage(long value) {
        if (!mMachine || value < MIN_VOLTAGE) return;
        voltage = value;
        markDirty();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setLong("satelliteVoltage", voltage);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        voltage = Math.max(MIN_VOLTAGE, tag.getLong("satelliteVoltage"));
        machineMode = Math.floorMod(machineMode, getAvailableRecipeMaps().size());
    }

    @Override
    public void setMachineMode(int mode) {
        super.setMachineMode(Math.floorMod(mode, getAvailableRecipeMaps().size()));
        for (var hatch : mInputHatches) hatch.mRecipeMap = getRecipeMap();
        for (var bus : mInputBusses) bus.mRecipeMap = getRecipeMap();
        setSingleRecipeCheck(null);
    }

    @Override
    public String getMachineModeName() {
        return StatCollector.translateToLocal(getRecipeMap().unlocalizedName);
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (getBaseMetaTileEntity().isClientSide()) return;
        if (player.isSneaking()) super.onScrewdriverRightClick(side, player, x, y, z, tool);
        else {
            setMachineMode(nextMachineMode());
            GTUtility.sendChatTrans(player, "gt.interact.desc.mb.mode", getMachineModeName());
        }
    }

    @Override
    protected NexusSatelliteFactoryGui getGui() {
        return new NexusSatelliteFactoryGui(this);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new AutoMultiblockTooltipBuilder().addAutoTooltipWithRecipeMapFooter(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipPath() {
        return "nexus-satellite";
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(Casings.HighPowerCasing.getTextureId());
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? FRONT_ACTIVE : FRONT)
            .extFacing()
            .build(),
            TextureFactory.builder()
                .addIcon(active ? FRONT_GLOW : FRONT_IDLE_GLOW)
                .extFacing()
                .glow()
                .build() };
    }

}
