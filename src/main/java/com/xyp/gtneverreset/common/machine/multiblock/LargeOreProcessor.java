package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_FACTORY;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_FACTORY_ACTIVE;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.loader.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.machine.OreRecipeThreadProcessingLogic;
import com.xyp.gtneverreset.utils.machine.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;
import com.xyp.gtneverreset.utils.structure.StructureTooltipHooks;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;

@IMetaTileEntity.SkipGenerateDescription
public class LargeOreProcessor extends GTNRMultiBlockBase<LargeOreProcessor> implements ISurvivalConstructable {

    private static final int BASE_PARALLELS = 64;
    private static final int RECIPE_THREADS = 10;
    private static final String STRUCTURE_PIECE = "main";
    private static final String[][] STRUCTURE_SHAPE = MultiblockStructureLoader.load("large_ore_processor");
    private static final int HORIZONTAL_OFFSET = 1;
    private static final int VERTICAL_OFFSET = 2;
    private static final int DEPTH_OFFSET = 0;
    private static final int CASING_META = 0;

    private static int getCasingTextureID() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, CASING_META);
    }

    private int casingCount;

    public LargeOreProcessor(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public LargeOreProcessor(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeOreProcessor(mName);
    }

    @Override
    public IStructureDefinition<LargeOreProcessor> getStructureDefinition() {
        return StructureDefinition.<LargeOreProcessor>builder()
            .addShape(STRUCTURE_PIECE, transpose(STRUCTURE_SHAPE))
            .addElement(
                'A',
                ofChain(
                    StructureTooltipHooks.describeAsHatch(
                        ofHatchAdder(LargeOreProcessor::addParameterModuleToMachineList, getCasingTextureID(), 1),
                        ParameterModuleHatch.NAME_KEY),
                    buildHatchAdder(LargeOreProcessor.class).casingIndex(getCasingTextureID())
                        .hint(1)
                        .atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy))
                        .build(),
                    onElementPass(machine -> ++machine.casingCount, ofBlock(GregTechAPI.sBlockCasings2, CASING_META))))
            .build();
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
        casingCount = 0;
        checkPiece(STRUCTURE_PIECE, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkCasingMin(errors, casingCount, 1);
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, Energy.or(ExoticEnergy), 1);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new OreRecipeThreadProcessingLogic();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(getCasingTextureID());
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? OVERLAY_FRONT_ORE_FACTORY_ACTIVE : OVERLAY_FRONT_ORE_FACTORY)
            .extFacing()
            .build() };
    }

    @Override
    public String getTooltipPath() {
        return "large-ore-processor";
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNRRecipeMaps.ORE_PROCESSING_RECIPES;
    }

    @Override
    public int getMaxRecipeThreads() {
        return RECIPE_THREADS;
    }

    @Override
    protected int getBaseParallelRecipes() {
        return BASE_PARALLELS;
    }

    @Override
    protected boolean hasPerfectOverclock() {
        return true;
    }

    @Override
    protected boolean canUseControllerSlotForRecipe() {
        return false;
    }

}
