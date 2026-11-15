package com.xyp.gtneverreset.common.machine.multiblock.structure;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.controllerElement;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.hatchElement;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.threadElement;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.extractMTEs;
import static gregtech.api.util.GTStructureUtility.ofFrame;
import static gregtech.api.util.GTStructureUtility.ofGenericMTETiered;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtneverreset.common.block.blocks.BlockChaosCapacity;
import com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;

import bartworks.API.BorosilicateGlass;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicHull;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTStructureUtility.ProxyStructureElement;
import gregtech.api.util.HatchElementBuilder;
import gregtech.api.util.MultiblockTooltipBuilder;

public final class BottledChaosStandardStructure extends BottledChaosStructure {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int HORIZONTAL_OFFSET = 14;
    private static final int VERTICAL_OFFSET = 5;
    private static final int DEPTH_OFFSET = 12;
    private static final int CAPACITY_REGION_COUNT = 4;
    private static final int CAPACITY_REGION_BLOCK_COUNT = 24;
    private static final int CAPACITY_LEVEL_COUNT = 5;

    // spotless:off

    // a marks the immediate capacity enclosures and controller-shaft glass; A remains outer glass.
    private static final String[][] SHAPE = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader.load("bottle_in_chaos");
    // spotless:on

    private static IStructureDefinition<BottleInChaos> structureDefinition;
    private static MultiblockTooltipBuilder tooltip;
    private int generatorLevel = -1;

    public BottledChaosStandardStructure(BottleInChaos machine) {
        super(machine);
    }

    @Override
    public int getProcessingGroupSlot(int relativeLeft, int relativeBack) {
        // Top view, controller front upwards: right #1..5, rear #6..10, left #11..15.
        if (relativeLeft == -10 && relativeBack >= 0 && relativeBack <= 4) return relativeBack;
        if (relativeBack == 12 && relativeLeft >= -2 && relativeLeft <= 2) return relativeLeft + 7;
        if (relativeLeft == 10 && relativeBack >= 0 && relativeBack <= 4) return 14 - relativeBack;
        return -1;
    }

    @Override
    public int getProcessingGroupLimit() {
        return BottleInChaosMachineBase.THREAD_COUNT;
    }

    @Override
    public int getCredentialHatchLimit(int parallelTier) {
        return Math.min(getProcessingGroupLimit(), 1 + parallelTier * 2);
    }

    @Override
    public void reset() {
        generatorLevel = -1;
    }

    @Override
    public boolean checkStructurePieces(List<StructureError> errors) {
        return checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors);
    }

    @Override
    public StructureError getInvalidCapacityRegionError() {
        // #tr gtnr.machine.BottleInChaos.structure_error.invalid_capacity_region
        // # Leave each capacity region empty or fill it with capacity blocks of one tier.
        // # zh_CN 容量区须留空, 或填满同级容量方块.
        return StructureErrors.of("gtnr.machine.BottleInChaos.structure_error.invalid_capacity_region");
    }

    @Override
    public int getCapacityRegionCount() {
        return CAPACITY_REGION_COUNT;
    }

    @Override
    public int getCapacityRegionBlockCount() {
        return CAPACITY_REGION_BLOCK_COUNT;
    }

    @Override
    public int getCapacityLevelCount() {
        return CAPACITY_LEVEL_COUNT;
    }

    @Override
    public int getCapacityRegion(int x, int y, int z) {
        IGregTechTileEntity base = machine.getBaseMetaTileEntity();
        int dx = x - base.getXCoord();
        int dz = z - base.getZCoord();
        ForgeDirection left = machine.getExtendedFacing()
            .getRelativeLeftInWorld();
        ForgeDirection back = machine.getExtendedFacing()
            .getRelativeBackInWorld();
        // The four fixed columns remain independently tiered after sharing one structure symbol.
        return (dx * left.offsetX + dz * left.offsetZ > 0 ? 1 : 0)
            + (dx * back.offsetX + dz * back.offsetZ > 0 ? 2 : 0);
    }

    @Override
    public boolean isWirelessMode() {
        return generatorLevel >= 4 && machine.mEnergyHatches.isEmpty()
            && machine.getExoticEnergyHatches()
                .isEmpty();
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (machine.mMachine) return -1;
        // Preview placement must update hatch counts and textures without an incomplete full check clearing them.
        boolean preview = GregTechAPI.isDummyWorld(
            machine.getBaseMetaTileEntity()
                .getWorld());
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            environment,
            preview,
            !preview);
    }

    @Override
    public IStructureDefinition<BottleInChaos> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<BottleInChaos>builder()
                .addShape(STRUCTURE_PIECE_MAIN, transpose(SHAPE))
                // GuideNH retains only channels discovered from the registered controller's structure.
                .addChannel(BottledChaosPrototypeStructure.PREVIEW_CHANNEL, 1, 1)
                .addChannel(BlockChaosCapacity.STRUCTURE_CHANNEL, 1, getCapacityLevelCount() + 1)
                .addElement('A', ofBlock(BorosilicateGlass.getGlassBlock(), 0))
                .addElement('a', ofBlock(BorosilicateGlass.getGlassBlock(), 0))
                .addElement('B', Casings.TPVCoilBlock.asElement())
                .addElement('C', Casings.IVMachineCasing.asElement())
                .addElement('D', Casings.RobustTungstenSteelMachineCasing.asElement())
                .addElement('E', Casings.StableTitaniumMachineCasing.asElement())
                .addElement('F', Casings.FilterMachineCasing.asElement())
                .addElement(
                    'G',
                    withChannel(
                        BlockChaosCapacity.STRUCTURE_CHANNEL,
                        BottleInChaosMachineBase.<BottleInChaos>capacityElement()))
                .addElement('H', Casings.EVMachineCasing.asElement())
                .addElement('I', Casings.IronFence.asElement())
                .addElement('J', ofFrame(Materials.Iron))
                .addElement('K', ofFrame(Materials.TungstenSteel))
                .addElement('L', Casings.Firebricks.asElement())
                .addElement('M', Casings.FrostProofMachineCasing.asElement())
                .addElement('N', Casings.RobustTungstenSteelMachineCasing.asElement())
                .addElement('O', Casings.TungstensteelPipeCasing.asElement())
                .addElement('P', Casings.FrostProofMachineCasing.asElement())
                .addElement('Q', Casings.RobustTungstenSteelMachineCasing.asElement())
                .addElement('R', ofBlock(BorosilicateGlass.getGlassBlock(), 0))
                .addElement('S', withChannel("energymatrix", generatorCasingElement()))
                .addElement('T', withChannel("fieldgeneratortier", generatorHullElement()))
                .addElement('U', threadElement(0))
                .addElement('V', threadElement(1))
                .addElement('W', threadElement(2))
                .addElement('X', controllerElement())
                .addElement(
                    'Y',
                    hatchElement(
                        HatchElementBuilder.<BottleInChaos>builder()
                            .atLeast(InputBus, InputHatch, OutputBus, OutputHatch, Energy.or(ExoticEnergy)),
                        5))
                .build();
        }
        return structureDefinition;
    }

    @Override
    public long getMaximumRecipeVoltage() {
        return GTValues.V[generatorLevel >= 4 ? VoltageIndex.MAX : VoltageIndex.UHV + Math.max(0, generatorLevel)];
    }

    private static IStructureElement<BottleInChaos> generatorCasingElement() {
        return ofBlocksTiered(
            (candidate, meta) -> candidate == GregTechAPI.sBlockCasings1 && meta >= 4 && meta <= 8 ? meta - 4 : null,
            ImmutableList.of(
                Pair.of(GregTechAPI.sBlockCasings1, 4),
                Pair.of(GregTechAPI.sBlockCasings1, 5),
                Pair.of(GregTechAPI.sBlockCasings1, 6),
                Pair.of(GregTechAPI.sBlockCasings1, 7),
                Pair.of(GregTechAPI.sBlockCasings1, 8)),
            -1,
            (machine, level) -> ((BottledChaosStandardStructure) machine.getMachineStructure()).generatorLevel = level,
            machine -> ((BottledChaosStandardStructure) machine.getMachineStructure()).generatorLevel);
    }

    private static IStructureElement<BottleInChaos> generatorHullElement() {
        ItemStack[] hulls = { ItemList.Hull_EV.get(1), ItemList.Hull_IV.get(1), ItemList.Hull_LuV.get(1),
            ItemList.Hull_ZPM.get(1), ItemList.Hull_UV.get(1) };
        IStructureElement<BottleInChaos> element = ofGenericMTETiered(MTEBasicHull.class, (machine, hull, tier) -> {
            BottledChaosStandardStructure structure = (BottledChaosStandardStructure) machine.getMachineStructure();
            int level = tier - 1;
            if (structure.generatorLevel != -1 && structure.generatorLevel != level) return false;
            structure.generatorLevel = level;
            return true;
        }, extractMTEs(MTEBasicHull.class, hulls));
        return new ProxyStructureElement<BottleInChaos, IStructureElement<BottleInChaos>>(element) {

            private ItemStack selectedHull(ItemStack trigger) {
                return hulls[Math.max(0, Math.min(hulls.length - 1, trigger.stackSize - 1))];
            }

            @Override
            public boolean couldBeValid(BottleInChaos machine, World world, int x, int y, int z, ItemStack trigger) {
                return world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile
                    && tile.getMetaTileID() == selectedHull(trigger).getItemDamage();
            }

            @Override
            public BlocksToPlace getBlocksToPlace(BottleInChaos machine, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment environment) {
                return BlocksToPlace.create(selectedHull(trigger).copy());
            }

            @Override
            public PlaceResult survivalPlaceBlock(BottleInChaos machine, World world, int x, int y, int z,
                ItemStack trigger, AutoPlaceEnvironment environment) {
                if (couldBeValid(machine, world, x, y, z, trigger)) return PlaceResult.SKIP;
                return super.survivalPlaceBlock(machine, world, x, y, z, trigger, environment);
            }
        };
    }

    @Override
    public MultiblockTooltipBuilder getTooltip() {
        return new com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder().addAutoTooltip(machine)
            .toolTipFinisher();
    }
}
