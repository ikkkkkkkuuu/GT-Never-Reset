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
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.UniversalJointFactoryGui;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;

@IMetaTileEntity.SkipGenerateDescription
public final class UniversalJointFactory extends GTNRMultiBlockBase<UniversalJointFactory>
    implements ISurvivalConstructable {

    private static final int BASE_PARALLELS = 64;
    private static final int BASE_THREADS = 32;
    private static final int THREADS_PER_TIER = 8;
    private static final int BASE_GLASS_TIER = VoltageIndex.HV;
    private static final double SPEED_PER_GLASS_TIER = 0.2;
    private static final double EU_MULTIPLIER_PER_GLASS_TIER = 0.9;
    private static final int CASING_TEXTURE_ID = 16;
    private static final String STRUCTURE_PIECE = "main";
    private static final int OFFSET_X = 24, OFFSET_Y = 15, OFFSET_Z = 0;
    public static final List<RecipeMap<?>> RECIPE_MAPS = List.of(
        RecipeMaps.benderRecipes,
        RecipeMaps.compressorRecipes,
        RecipeMaps.hammerRecipes,
        RecipeMaps.cutterRecipes,
        RecipeMaps.extruderRecipes,
        RecipeMaps.latheRecipes,
        RecipeMaps.wiremillRecipes,
        RecipeMaps.formingPressRecipes,
        RecipeMaps.polarizerRecipes,
        RecipeMaps.laserEngraverRecipes,
        RecipeMaps.fluidSolidifierRecipes,
        RecipeMaps.assemblerRecipes,
        RecipeMaps.circuitAssemblerRecipes,
        RecipeMaps.centrifugeRecipes,
        RecipeMaps.thermalCentrifugeRecipes,
        RecipeMaps.electrolyzerRecipes,
        RecipeMaps.sifterRecipes,
        RecipeMaps.maceratorRecipes,
        RecipeMaps.extractorRecipes,
        RecipeMaps.chemicalDehydratorRecipes,
        RecipeMaps.chemicalReactorRecipes,
        RecipeMaps.mixerRecipes,
        RecipeMaps.chemicalBathRecipes,
        RecipeMaps.oreWasherRecipes);

    public static int parallelsForTier(int tier) {
        int upgrades = (int) Math.min(30, Math.max(0L, (long) tier - VoltageIndex.LV));
        return (int) Math.min(Integer.MAX_VALUE, (long) BASE_PARALLELS << upgrades);
    }

    public static int threadsForTier(int tier) {
        long upgrades = Math.max(0L, (long) tier - VoltageIndex.LV);
        return (int) Math.min(Integer.MAX_VALUE, BASE_THREADS + upgrades * THREADS_PER_TIER);
    }

    private IStructureDefinition<UniversalJointFactory> definition;
    private RecipeMap<?> processingRecipeMap;

    public UniversalJointFactory(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public UniversalJointFactory(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new UniversalJointFactory(mName);
    }

    @Override
    public IStructureDefinition<UniversalJointFactory> getStructureDefinition() {
        if (definition == null) {
            var ports = ofChain(
                StructureTooltipHooks.describeAsHatch(
                    ofHatchAdder(UniversalJointFactory::addParameterModuleToMachineList, CASING_TEXTURE_ID, 1),
                    ParameterModuleHatch.NAME_KEY),
                buildHatchAdder(UniversalJointFactory.class)
                    .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                    .casingIndex(CASING_TEXTURE_ID)
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings2, 0)));
            definition = StructureDefinition.<UniversalJointFactory>builder()
                .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load("universal_joint_factory")))
                .addElement('X', ports)
                .addElement('Q', ports)
                .addElement('A', ofBlock(GregTechAPI.sBlockCasings2, 0))
                .addElement('B', ofBlock(GregTechAPI.sBlockCasings1, 2))
                .addElement('D', ofBlock(GregTechAPI.sBlockCasings2, 0))
                .addElement('E', ofBlock(GregTechAPI.sBlockCasings2, 3))
                .addElement('F', ofBlock(GregTechAPI.sBlockCasings2, 13))
                .addElement('G', ofBlock(GregTechAPI.sBlockCasings5, 0))
                .addElement(
                    'I',
                    chainAllGlasses(-1, (machine, tier) -> machine.mGlassTier = tier, machine -> machine.mGlassTier))
                .addElement('J', ofBlock(Blocks.glowstone, 0))
                .build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE,
            stack,
            OFFSET_X,
            OFFSET_Y,
            OFFSET_Z,
            elementBudget,
            environment,
            false,
            true);
    }

    @Override
    protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(STRUCTURE_PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMin(errors, InputBus.or(InputHatch), 1);
        checkHatchMin(errors, OutputBus.or(OutputHatch), 1);
        checkHatchMin(errors, Energy.or(ExoticEnergy), 1);
        checkHatchMin(errors, Maintenance, 1);
        checkHatchMax(errors, Maintenance, 1);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new UniversalJointFactoryProcessingLogic(this);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return processingRecipeMap == null ? RECIPE_MAPS.get(Math.floorMod(machineMode, RECIPE_MAPS.size()))
            : processingRecipeMap;
    }

    void setProcessingRecipeMap(RecipeMap<?> map) {
        processingRecipeMap = map;
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return RECIPE_MAPS;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(CASING_TEXTURE_ID);
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE)
            .extFacing()
            .build() };
    }

    @Override
    public String getTooltipPath() {
        return "universal-joint-factory";
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.machine.universal_joint_factory.type
        // # Universal processing / cross-recipe threads
        // # zh_CN 通用加工 / 跨配方线程
        return StatCollector.translateToLocal("gtnr.machine.universal_joint_factory.type");
    }

    @Override
    protected UniversalJointFactoryGui getGui() {
        return new UniversalJointFactoryGui(this);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return (getMachineMode() + 1) % RECIPE_MAPS.size();
    }

    @Override
    public void setMachineMode(int index) {
        if (index >= 0 && index < RECIPE_MAPS.size()) super.setMachineMode(index);
    }

    @Override
    public String getMachineModeKey() {
        return getRecipeMap().unlocalizedName;
    }

    @Override
    public int getMaxRecipeThreads() {
        if (getBaseMetaTileEntity() != null && mDualInputHatches.isEmpty()) return 1;
        return threadsForTier(GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected int getBaseParallelRecipes() {
        return parallelsForTier(GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected double getBaseSpeedMultiplier() {
        return 1 + SPEED_PER_GLASS_TIER * getGlassUpgrades();
    }

    @Override
    protected double getBaseEUtMultiplier() {
        return Math.pow(EU_MULTIPLIER_PER_GLASS_TIER, getGlassUpgrades());
    }

    private int getGlassUpgrades() {
        return Math.max(0, mGlassTier - BASE_GLASS_TIER);
    }
}
