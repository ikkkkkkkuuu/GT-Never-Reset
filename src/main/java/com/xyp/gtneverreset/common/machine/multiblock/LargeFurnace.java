package com.xyp.gtneverreset.common.machine.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofHatchAdder;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.multiMachineBase.GTNRMultiBlockBase;
import com.xyp.gtneverreset.common.machine.tooltip.StructureTooltipHooks;
import com.xyp.gtneverreset.utils.machine.RecipeThreadProcessingLogic;
import com.xyp.gtnrlib.mod.ModList;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;

@IMetaTileEntity.SkipGenerateDescription
public final class LargeFurnace extends GTNRMultiBlockBase<LargeFurnace> implements ISurvivalConstructable {

    private static final int BASE_PARALLELS = 64;
    private static final int BASE_THREADS = 6;
    private static final int CASING_TEXTURE_ID = 16;
    private static final String STRUCTURE_PIECE = "main";
    private static final IIconContainer FRONT = Textures.BlockIcons
        .custom(ModList.GTNeverReset.getResourceLocation(), "parallel/furnace_front");
    private static final IIconContainer ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNeverReset.getResourceLocation(), "parallel/furnace_active");

    public static int scaleForTier(int base, int tier) {
        return (int) Math.min(Integer.MAX_VALUE, (long) base << Math.min(30, Math.max(0, tier - 1)));
    }

    private IStructureDefinition<LargeFurnace> definition;

    public LargeFurnace(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public LargeFurnace(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeFurnace(mName);
    }

    @Override
    public IStructureDefinition<LargeFurnace> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<LargeFurnace>builder()
                .addShape(STRUCTURE_PIECE, transpose(LargeFurnaceStructure.shape()))
                .addElement(
                    'L',
                    ofChain(
                        StructureTooltipHooks.describeAsHatch(
                            ofHatchAdder(LargeFurnace::addParameterModuleToMachineList, CASING_TEXTURE_ID, 1),
                            ParameterModuleHatch.NAME_KEY),
                        buildHatchAdder(LargeFurnace.class)
                            .atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy), Maintenance)
                            .casingIndex(CASING_TEXTURE_ID)
                            .hint(1)
                            .buildAndChain(ofBlock(GregTechAPI.sBlockCasings2, 0))));
            ItemStack steel = Materials.Steel.getBlocks(1);
            builder.addElement('J', ofBlock(Block.getBlockFromItem(steel.getItem()), steel.getItemDamage()))
                .addElement('D', ofBlock(GregTechAPI.sBlockCasings12, 0))
                .addElement('E', ofBlock(GregTechAPI.sBlockCasings4, 15))
                .addElement('H', ofBlock(Blocks.lava, 0))
                .addElement('I', ofBlock(Blocks.lava, 0));
            for (char c : new char[] { 'B', 'C', 'G', 'K', 'M' }) {
                builder.addElement(c, ofBlock(Blocks.iron_bars, 0));
            }
            definition = builder.build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(
            STRUCTURE_PIECE,
            stack,
            hintsOnly,
            LargeFurnaceStructure.OFFSET_X,
            LargeFurnaceStructure.OFFSET_Y,
            LargeFurnaceStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE,
            stack,
            LargeFurnaceStructure.OFFSET_X,
            LargeFurnaceStructure.OFFSET_Y,
            LargeFurnaceStructure.OFFSET_Z,
            elementBudget,
            environment,
            false,
            true);
    }

    @Override
    protected void checkMachineStructure(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(
            STRUCTURE_PIECE,
            LargeFurnaceStructure.OFFSET_X,
            LargeFurnaceStructure.OFFSET_Y,
            LargeFurnaceStructure.OFFSET_Z,
            errors);
    }

    @Override
    protected void checkMachineHatches(List<StructureError> errors) {
        checkHatchMin(errors, InputBus, 1);
        checkHatchMin(errors, OutputBus, 1);
        checkHatchMin(errors, Energy.or(ExoticEnergy), 1);
        checkHatchMin(errors, Maintenance, 1);
        checkHatchMax(errors, Maintenance, 1);
    }

    @Override
    protected RecipeThreadProcessingLogic createRecipeThreadProcessingLogic() {
        return new FurnaceRecipeThreadProcessingLogic();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(CASING_TEXTURE_ID);
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(active ? ACTIVE : FRONT)
            .extFacing()
            .build() };
    }

    @Override
    public String getTooltipPath() {
        return "large-furnace";
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.furnaceRecipes;
    }

    @Override
    public int getMaxRecipeThreads() {
        return scaleForTier(BASE_THREADS, GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected int getBaseParallelRecipes() {
        return scaleForTier(BASE_PARALLELS, GTUtility.getTier(getMaxInputVoltage()));
    }

    @Override
    protected double getBaseSpeedMultiplier() {
        return 2;
    }

    @Override
    protected double getBaseEUtMultiplier() {
        return 0.8;
    }

}
