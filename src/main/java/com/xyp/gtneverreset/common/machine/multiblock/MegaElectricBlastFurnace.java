package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_GLOW;
import static gregtech.api.util.GTStructureUtility.activeCoils;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofCoil;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.ExoticEnergyInputHelper;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.misc.GTStructureChannels;

@IMetaTileEntity.SkipGenerateDescription
public final class MegaElectricBlastFurnace extends CrossRecipeMultiblockBase<MegaElectricBlastFurnace>
    implements ICasingTextureProvider, ISurvivalConstructable {

    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 11, VERTICAL_OFFSET = 21, DEPTH_OFFSET = 0;
    private static final int MIN_CASINGS = 1_200;
    private static final int HEAT_PER_VOLTAGE_TIER = 1_000;
    private static final int HEAT_PER_RECIPE = 100;
    private static final int HEAT_PER_DISCOUNT = 450;
    private static final int MIN_DISCOUNTED_EUT = 1;
    private static final int PARALLELS_PER_KELVIN = 256;
    private static final int BASE_THREADS = 96;
    private static final Casings CASING = Casings.DimensionallyTranscendentCasing;
    private static final List<RecipeMap<?>> RECIPE_MAPS = List
        .of(RecipeMaps.blastFurnaceRecipes, RecipeMaps.alloyBlastSmelterRecipes);

    private static int initialHeat(HeatingCoilLevel coils, int voltageTier) {
        return (int) coils.getHeat() + HEAT_PER_VOLTAGE_TIER * Math.max(0, voltageTier - VoltageIndex.LV);
    }

    private static int parallelLimit(HeatingCoilLevel coils, int heat) {
        if (coils == HeatingCoilLevel.None) return 0;
        long coilMultiplier = 1L << ((coils.getTier() + 1) / 2);
        return (int) Math.min(Integer.MAX_VALUE, PARALLELS_PER_KELVIN * coilMultiplier * heat);
    }

    private int heatingCapacity;

    private int casingCount;

    public MegaElectricBlastFurnace(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    private MegaElectricBlastFurnace(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MegaElectricBlastFurnace(mName);
    }

    @Override
    public IStructureDefinition<MegaElectricBlastFurnace> getStructureDefinition() {
        return Structure.DEFINITION;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, stack, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE,
            stack,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            budget,
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
        if (!StructureTooltipHooks.onCheckCasingMin(this, MIN_CASINGS)) {
            checkCasingMin(errors, casingCount, MIN_CASINGS);
        }
        checkHatchMin(errors, Energy.or(ExoticEnergy), 1);
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        if (!mMachine) return;
        int baseHeat = initialHeat(mCoilLevel, GTUtility.getTier(getMaxInputVoltage()));
        if (baseHeat > heatingCapacity) {
            heatingCapacity = baseHeat;
            markDirty();
        }
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return RECIPE_MAPS;
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new BlastFurnaceProcessingLogic();
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        logic.setAvailableVoltage(availableThreadPower())
            .setAvailableAmperage(1)
            .setAmperageOC(true)
            .setMaxTierSkips(NO_TIER_SKIP);
    }

    @Override
    protected void onRecipeThreadFinished() {
        heatingCapacity = (int) Math.min(Integer.MAX_VALUE, (long) heatingCapacity + HEAT_PER_RECIPE);
    }

    @Override
    public long getMaxInputVoltage() {
        return isTooltipProbeActive() ? super.getMaxInputVoltage()
            : ExoticEnergyInputHelper.getMaxInputVoltageMulti(getExoticAndNormalEnergyHatchList());
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return Textures.BlockIcons.createTextureWithCasing(
            this,
            side,
            facing,
            active,
            OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE,
            OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_GLOW,
            OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE,
            OVERLAY_FRONT_ELECTRIC_BLAST_FURNACE_ACTIVE_GLOW);
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
    public String getTooltipPath() {
        return "mega-electric-blast-furnace";
    }

    @Override
    public TooltipStats getTooltipStats() {
        return null;
    }

    @Override
    public HeatRules getTooltipHeatRules() {
        return null;
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.machine.mega_electric_blast_furnace.recipe_type
        // # Electric Blast Furnace / Alloy Blast Smelter
        // # zh_CN 工业高炉 / 合金冶炼炉
        return StatCollector.translateToLocal("gtnr.machine.mega_electric_blast_furnace.recipe_type");
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of(
            "voltageHeat",
            formatNumber(HEAT_PER_VOLTAGE_TIER),
            "cycleHeat",
            HEAT_PER_RECIPE,
            "discountHeat",
            HEAT_PER_DISCOUNT,
            "minEUt",
            MIN_DISCOUNTED_EUT,
            "perfectOverclockHeat",
            formatNumber(OverclockCalculator.HEAT_OVERCLOCK_THRESHOLD),
            "baseParallels",
            formatNumber(parallelLimit(HeatingCoilLevel.LV, initialHeat(HeatingCoilLevel.LV, VoltageIndex.LV))),
            "parallelsPerKelvin",
            PARALLELS_PER_KELVIN);
    }

    @Override
    public List<String> getTooltipStructureNotes() {
        return List.of(
            // #tr gtnr.machine.mega_electric_blast_furnace.structure.coils
            // # All heating coils must be the {\\RED}same tier{\\GRAY}.
            // # zh_CN 所有加热线圈都得是{\\RED}同一等级{\\GRAY}。
            StatCollector.translateToLocal("gtnr.machine.mega_electric_blast_furnace.structure.coils"),
            // #tr gtnr.machine.mega_electric_blast_furnace.structure.hatches
            // # Put an {\\AQUA}energy hatch{\\GRAY} in a main casing slot. No maintenance or muffler needed.
            // # zh_CN 主机壳位至少留一个给{\\AQUA}能源仓{\\GRAY}。无需维护仓或消声仓。
            StatCollector.translateToLocal("gtnr.machine.mega_electric_blast_furnace.structure.hatches"),
            // #tr gtnr.machine.mega_electric_blast_furnace.structure.outputs
            // # Use {\\AQUA}output buses{\\GRAY} for items and {\\AQUA}output hatches{\\GRAY} for fluids.
            // # zh_CN 物品走{\\AQUA}输出总线{\\GRAY}，流体走{\\AQUA}输出仓{\\GRAY}。
            StatCollector.translateToLocal("gtnr.machine.mega_electric_blast_furnace.structure.outputs"));
    }

    @Override
    public String[] getInfoData() {
        String[] info = super.getInfoData();
        String[] lines = Arrays.copyOf(info, info.length + 1);
        // #tr gtnr.machine.mega_electric_blast_furnace.heat
        // # Furnace heat: %s K
        // # zh_CN 炉温：%s K
        lines[info.length] = StatCollector
            .translateToLocalFormatted("gtnr.machine.mega_electric_blast_furnace.heat", formatNumber(heatingCapacity));
        return lines;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("heatingCapacity", heatingCapacity);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        heatingCapacity = Math.max(0, tag.getInteger("heatingCapacity"));
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
    public boolean getDefaultInputSeparationMode() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public int getMaxRecipeThreads() {
        return BASE_THREADS;
    }

    @Override
    public int getParameterModuleLimit(MachineModuleType type) {
        return type == MachineModuleType.Thread ? 1 : 0;
    }

    @Override
    protected int getBaseParallelRecipes() {
        return mMachine ? parallelLimit(mCoilLevel, heatingCapacity) : 0;
    }

    public int getHeatingCapacity() {
        return heatingCapacity;
    }

    private double getRecipeEnergyMultiplier(GTRecipe recipe) {
        if (recipe.mSpecialValue < HEAT_PER_DISCOUNT || recipe.mEUt <= 0) return 1;
        int steps = (heatingCapacity - recipe.mSpecialValue) / HEAT_PER_DISCOUNT;
        int discountedEUt = Math.max(MIN_DISCOUNTED_EUT, recipe.mEUt >> Math.min(30, Math.max(0, steps)));
        return (double) discountedEUt / recipe.mEUt;
    }

    private static final class Structure {

        private static final IStructureDefinition<MegaElectricBlastFurnace> DEFINITION = StructureDefinition
            .<MegaElectricBlastFurnace>builder()
            .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load("mega_electric_blast_furnace")))
            .addElement(
                'A',
                ofChain(
                    StructureTooltipHooks.describeAsHatch(
                        ofHatchAdder(
                            MegaElectricBlastFurnace::addParameterModuleToMachineList,
                            CASING.getTextureId(),
                            1),
                        ParameterModuleHatch.NAME_KEY),
                    buildHatchAdder(MegaElectricBlastFurnace.class)
                        .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy.or(ExoticEnergy))
                        .casingIndex(CASING.getTextureId())
                        .hint(1)
                        .buildAndChain(onElementPass(machine -> ++machine.casingCount, CASING.asElement()))))
            .addElement('C', Casings.DimensionalInjectionCasing.asElement())
            .addElement('D', Casings.OmniPurposeInfinityFusedGlass.asElement())
            .addElement('E', Casings.NeutroniumCasing.asElement())
            .addElement('F', Casings.DimensionalBridge.asElement())
            .addElement(
                'G',
                GTStructureChannels.HEATING_COIL.use(
                    activeCoils(
                        ofCoil((machine, level) -> { machine.mCoilLevel = level; }, machine -> machine.mCoilLevel))))
            .build();
    }

    private final class BlastFurnaceProcessingLogic extends CrossRecipeProcessingLogic {

        private BlastFurnaceProcessingLogic() {
            super(MegaElectricBlastFurnace.this);
        }

        @Nonnull
        @Override
        protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
            CheckRecipeResult result = super.validateRecipe(recipe);
            if (result.wasSuccessful() && recipe.mSpecialValue > heatingCapacity) {
                return CheckRecipeResultRegistry.insufficientHeat(recipe.mSpecialValue);
            }
            return result;
        }

        @Nonnull
        @Override
        protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
            return super.createParallelHelper(recipe).setEUtModifier(getRecipeEnergyMultiplier(recipe));
        }

        @Nonnull
        @Override
        protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
            return super.createOverclockCalculator(recipe).setEUtDiscount(getRecipeEnergyMultiplier(recipe))
                .setRecipeHeat(Math.max(0, recipe.mSpecialValue))
                .setMachineHeat(heatingCapacity)
                .setHeatOC(true);
        }
    }
}
