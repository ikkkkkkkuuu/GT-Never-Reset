package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
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
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.GTStructureChannels;

@IMetaTileEntity.SkipGenerateDescription
public final class MegaElectricBlastFurnace extends MTEExtendedPowerMultiBlockBase<MegaElectricBlastFurnace>
    implements ICasingTextureProvider, ISurvivalConstructable, MultiblockTooltipSource {

    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 11, VERTICAL_OFFSET = 21, DEPTH_OFFSET = 0;
    private static final int MIN_CASINGS = 1_200;
    private static final int HEAT_PER_VOLTAGE_TIER = 1_000;
    private static final int HEAT_PER_RECIPE = 100;
    private static final int HEAT_PER_DISCOUNT = 450;
    private static final int PARALLELS_PER_KELVIN = 256;
    private static final Casings CASING = Casings.DimensionallyTranscendentCasing;
    private static final IHatchElement<? super MegaElectricBlastFurnace> ENERGY_INPUT = Energy.or(ExoticEnergy);

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
    private HeatingCoilLevel coilLevel = HeatingCoilLevel.None;

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
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        coilLevel = HeatingCoilLevel.None;
        if (checkPiece(STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) {
            collectTooltipStructureRequirements(errors);
        }
    }

    @Override
    public void collectTooltipStructureRequirements(List<StructureError> errors) {
        if (!StructureTooltipHooks.onCheckCasingMin(this, MIN_CASINGS)) {
            checkCasingMin(errors, casingCount, MIN_CASINGS);
        }
        if (!StructureTooltipHooks.onCheckHatches(this, 1, StructureTooltipHooks.UNLIMITED, ENERGY_INPUT)
            && ENERGY_INPUT.count(this) == 0) {
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Energy, 0, 1));
        }
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        if (!mMachine) return;
        int baseHeat = initialHeat(coilLevel, GTUtility.getTier(getMaxInputVoltage()));
        if (baseHeat > heatingCapacity) {
            heatingCapacity = baseHeat;
            markDirty();
        }
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return getMachineMode() == 1 ? RecipeMaps.alloyBlastSmelterRecipes : RecipeMaps.blastFurnaceRecipes;
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return List.of(RecipeMaps.blastFurnaceRecipes, RecipeMaps.alloyBlastSmelterRecipes);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new BlastFurnaceProcessingLogic().setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setAvailableVoltage(Math.max(0, getMaxInputEu()))
            .setAvailableAmperage(1)
            .setAmperageOC(true)
            .setMaxTierSkips(NO_TIER_SKIP);
    }

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        heatingCapacity = (int) Math.min(Integer.MAX_VALUE, (long) heatingCapacity + HEAT_PER_RECIPE);
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
        return new AutoMultiblockTooltipBuilder().addAutoTooltipWithRecipeMapFooter(this)
            .toolTipFinisher();
    }

    @Override
    public String getTooltipPath() {
        return "mega-electric-blast-furnace";
    }

    @Override
    public TooltipStats getTooltipStats() {
        return getTooltipStatsAtTier(TieredPart.COIL, 0);
    }

    @Override
    public List<TieredPart> getTooltipTieredParts() {
        return List.of(TieredPart.COIL, TieredPart.VOLTAGE);
    }

    @Override
    public TooltipStats getTooltipStatsAtTier(TieredPart part, int tier) {
        HeatingCoilLevel coils = HeatingCoilLevel.getFromTier((byte) (part == TieredPart.COIL ? tier : 0));
        int voltageTier = part == TieredPart.VOLTAGE ? tier : VoltageIndex.LV;
        return new TooltipStats(parallelLimit(coils, initialHeat(coils, voltageTier)), Double.NaN, Double.NaN, false);
    }

    @Override
    public HeatRules getTooltipHeatRules() {
        return new HeatRules(true, false, 1, true, HEAT_PER_VOLTAGE_TIER, VoltageIndex.LV, 0);
    }

    @Override
    public Map<String, Object> getTooltipVars() {
        return Map.of(
            "cycleHeat",
            HEAT_PER_RECIPE,
            "discountHeat",
            HEAT_PER_DISCOUNT,
            "parallelsPerKelvin",
            PARALLELS_PER_KELVIN);
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
    protected MTEMultiBlockBaseGui<?> getGui() {
        return super.getGui().withMachineModeIcons(
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_LPF_METAL);
    }

    @Override
    public String getMachineModeName() {
        return StatCollector.translateToLocal(getRecipeMap().unlocalizedName);
    }

    @Override
    public void setMachineMode(int index) {
        if (index >= 0 && index < getAvailableRecipeMaps().size()) super.setMachineMode(index);
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
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public VoidingMode getDefaultVoidingMode() {
        return VoidingMode.VOID_NONE;
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public int getMaxParallelRecipes() {
        return mMachine ? parallelLimit(coilLevel, heatingCapacity) : 0;
    }

    public int getHeatingCapacity() {
        return heatingCapacity;
    }

    private double getRecipeEnergyMultiplier(GTRecipe recipe) {
        if (recipe.mSpecialValue < HEAT_PER_DISCOUNT || recipe.mEUt <= 0) return 1;
        int steps = (heatingCapacity - recipe.mSpecialValue) / HEAT_PER_DISCOUNT;
        int discountedEUt = Math.max(1, recipe.mEUt >> Math.min(30, Math.max(0, steps)));
        return (double) discountedEUt / recipe.mEUt;
    }

    private static final class Structure {

        private static final IStructureDefinition<MegaElectricBlastFurnace> DEFINITION = StructureDefinition
            .<MegaElectricBlastFurnace>builder()
            .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load("mega_electric_blast_furnace")))
            .addElement(
                'A',
                buildHatchAdder(MegaElectricBlastFurnace.class)
                    .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, ENERGY_INPUT)
                    .casingIndex(CASING.getTextureId())
                    .hint(1)
                    .buildAndChain(onElementPass(machine -> ++machine.casingCount, CASING.asElement())))
            .addElement('C', Casings.DimensionalInjectionCasing.asElement())
            .addElement('D', Casings.OmniPurposeInfinityFusedGlass.asElement())
            .addElement('E', Casings.NeutroniumCasing.asElement())
            .addElement('F', Casings.DimensionalBridge.asElement())
            .addElement(
                'G',
                GTStructureChannels.HEATING_COIL.use(
                    activeCoils(
                        ofCoil((machine, level) -> { machine.coilLevel = level; }, machine -> machine.coilLevel))))
            .build();
    }

    private final class BlastFurnaceProcessingLogic extends ProcessingLogic {

        @Nonnull
        @Override
        protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
            return recipe.mSpecialValue <= heatingCapacity ? CheckRecipeResultRegistry.SUCCESSFUL
                : CheckRecipeResultRegistry.insufficientHeat(recipe.mSpecialValue);
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
