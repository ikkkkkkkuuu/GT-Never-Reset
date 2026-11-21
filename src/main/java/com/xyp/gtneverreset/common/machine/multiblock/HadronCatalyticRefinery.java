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

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.CrossRecipeMultiblockBase;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;

@IMetaTileEntity.SkipGenerateDescription
public final class HadronCatalyticRefinery extends CrossRecipeMultiblockBase<HadronCatalyticRefinery>
    implements ISurvivalConstructable {

    private static final Casings CASING = Casings.RobustTungstenSteelMachineCasing;
    private static final String STRUCTURE_PIECE = "main";
    private static final int HORIZONTAL_OFFSET = 8, VERTICAL_OFFSET = 22, DEPTH_OFFSET = 0;
    private static final List<RecipeMap<?>> RECIPE_MAPS = List.of(
        RecipeMaps.mixerNonCellRecipes,
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
        RecipeMaps.cannerRecipes);

    private static IStructureDefinition<HadronCatalyticRefinery> createStructure() {
        int texture = CASING.getTextureId();
        var ports = ofChain(
            StructureTooltipHooks.describeAsHatch(
                ofHatchAdder(HadronCatalyticRefinery::addParameterModuleToMachineList, texture, 1),
                ParameterModuleHatch.NAME_KEY),
            buildHatchAdder(HadronCatalyticRefinery.class)
                .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy.or(ExoticEnergy), Maintenance)
                .casingIndex(texture)
                .hint(1)
                .buildAndChain(CASING.asElement()));
        return StructureDefinition.<HadronCatalyticRefinery>builder()
            .addShape(STRUCTURE_PIECE, transpose(MultiblockStructureLoader.load("hadron_catalytic_refinery")))
            .addElement('X', ports)
            .addElement('Q', ports)
            .addElement('A', Casings.AssemblerMachineCasing.asElement())
            .addElement('B', Casings.ChemicalGradeGlass.asElement())
            .addElement('D', ofBlock(Blocks.iron_block, 0))
            .addElement('G', Casings.StableTitaniumMachineCasing.asElement())
            .addElement('I', CASING.asElement())
            .addElement('J', Casings.NichromeCoilBlock.asElement())
            .build();
    }

    public HadronCatalyticRefinery(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public HadronCatalyticRefinery(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new HadronCatalyticRefinery(mName);
    }

    @Override
    public IStructureDefinition<HadronCatalyticRefinery> getStructureDefinition() {
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
        checkPiece(STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMin(errors, InputBus.or(InputHatch), 1);
        checkHatchMin(errors, OutputBus.or(OutputHatch), 1);
        checkHatchRange(errors, Energy.or(ExoticEnergy), 1, 2);
        checkHatchMin(errors, Maintenance, 1);
        checkHatchMax(errors, Maintenance, 1);
    }

    @Override
    public List<RecipeMap<?>> getAvailableRecipeMaps() {
        return RECIPE_MAPS;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(CASING.getTextureId());
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER)
            .extFacing()
            .build() };
    }

    @Override
    public String getLocalNameKey() {
        return "gtnr.machine.hadron_catalytic_refinery.name";
    }

    @Override
    public String getTooltipPath() {
        return "hadron-catalytic-refinery";
    }

    @Override
    public String getTooltipMachineType() {
        // #tr gtnr.machine.hadron_catalytic_refinery.recipe_type
        // # Fluid processing / cross-recipe threads
        // # zh_CN 流体加工 / 跨配方线程
        return StatCollector.translateToLocal("gtnr.machine.hadron_catalytic_refinery.recipe_type");
    }

    @Override
    public int getMaxRecipeThreads() {
        return 32;
    }

    @Override
    protected double getBaseSpeedMultiplier() {
        return 3.0;
    }

    @Override
    protected double getBaseEUtMultiplier() {
        return 0.85;
    }

    private static final class StructureHolder {

        private static final IStructureDefinition<HadronCatalyticRefinery> DEFINITION = createStructure();
    }
}
