package com.xyp.gtneverreset.common.machine.multiblock.structure;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.hatchElement;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.threadElement;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosModuleMachineBase.ModuleElement.OverclockController;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosModuleMachineBase.ModuleElement.ParallelController;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosModuleMachineBase.ModuleElement.PowerConsumptionController;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosModuleMachineBase.ModuleElement.SpeedController;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

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
import gregtech.api.enums.Materials;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.HatchElementBuilder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.blocks.ItemMachines;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyTunnel;

public final class BottledChaosPrototypeStructure extends BottledChaosStructure {

    public static final String PREVIEW_CHANNEL = "bottledchaosprototype";

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final String STRUCTURE_PIECE_CAPACITY_BASE_EXTENSION = "capacity_base_extension";
    private static final String STRUCTURE_PIECE_CAPACITY_EXTENSION = "capacity_extension";
    private static final String STRUCTURE_PIECE_CAPACITY_BASE_TOP = "capacity_base_top";
    private static final String STRUCTURE_PIECE_CAPACITY_TOP = "capacity_top";
    private static final int HORIZONTAL_OFFSET = 7;
    private static final int VERTICAL_OFFSET = 4;
    private static final int DEPTH_OFFSET = 0;
    private static final int CAPACITY_HORIZONTAL_OFFSET = 3;
    private static final int CAPACITY_DEPTH_OFFSET = -4;
    private static final int CAPACITY_SEGMENT_HEIGHT = 4;
    private static final int MAX_EXTRA_SEGMENTS = 3;

    // spotless:off
    // Closed cabinets and a steel gantry join the controller to the enclosed capacity column.
    private static final String[][] SHAPE = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader.load("bottle_in_chaos_prototype");

    // The main piece owns the bridge's three blocks at the first capacity seam.
    private static final String[][] CAPACITY_BASE_EXTENSION = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader.load("bottle_in_chaos_prototype_capacity_base_extension");
    private static final String[][] CAPACITY_EXTENSION = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader.load("bottle_in_chaos_prototype_capacity_extension");
    private static final String[][] CAPACITY_BASE_TOP = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader.load("bottle_in_chaos_prototype_capacity_base_top");
    private static final String[][] CAPACITY_TOP = com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader.load("bottle_in_chaos_prototype_capacity_top");
    // spotless:on

    private static IStructureDefinition<BottleInChaos> structureDefinition;
    private static MultiblockTooltipBuilder tooltip;
    private int extraCapacitySegments;

    public BottledChaosPrototypeStructure(BottleInChaos machine) {
        super(machine);
    }

    @Override
    public int getProcessingGroupSlot(int relativeLeft, int relativeBack) {
        if (relativeBack != 0) return -1;
        if (relativeLeft >= -6 && relativeLeft <= -4) return relativeLeft + 6;
        return relativeLeft >= 4 && relativeLeft <= 5 ? relativeLeft - 1 : -1;
    }

    @Override
    public int getProcessingGroupLimit() {
        return 5;
    }

    @Override
    public int getCredentialHatchLimit(int parallelTier) {
        return Math.min(getProcessingGroupLimit(), 3 + parallelTier);
    }

    @Override
    public long getMaximumRecipeVoltage() {
        return GTValues.V[VoltageIndex.UV];
    }

    @Override
    public boolean isWirelessMode() {
        return false;
    }

    @Override
    public boolean checkStructurePieces(List<StructureError> errors) {
        extraCapacitySegments = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) return false;
        for (int extra = 0; extra <= MAX_EXTRA_SEGMENTS; extra++) {
            List<StructureError> topErrors = new ArrayList<>();
            int topOffset = VERTICAL_OFFSET + extra * CAPACITY_SEGMENT_HEIGHT;
            if (checkPiece(
                getCapacityTopPiece(extra),
                CAPACITY_HORIZONTAL_OFFSET,
                topOffset,
                CAPACITY_DEPTH_OFFSET,
                topErrors)) return true;
            if (extra == MAX_EXTRA_SEGMENTS) {
                errors.addAll(topErrors);
                return false;
            }
            if (!checkPiece(
                getCapacityExtensionPiece(extra),
                CAPACITY_HORIZONTAL_OFFSET,
                topOffset + CAPACITY_SEGMENT_HEIGHT - 1,
                CAPACITY_DEPTH_OFFSET,
                errors)) return false;
            extraCapacitySegments++;
        }
        return false;
    }

    private static String getCapacityExtensionPiece(int extra) {
        return extra == 0 ? STRUCTURE_PIECE_CAPACITY_BASE_EXTENSION : STRUCTURE_PIECE_CAPACITY_EXTENSION;
    }

    private static String getCapacityTopPiece(int extra) {
        return extra == 0 ? STRUCTURE_PIECE_CAPACITY_BASE_TOP : STRUCTURE_PIECE_CAPACITY_TOP;
    }

    @Override
    public int getCapacityRegionCount() {
        return 1 + extraCapacitySegments;
    }

    @Override
    public int getCapacityRegionBlockCount() {
        return 48;
    }

    @Override
    public int getCapacityLevelCount() {
        return 2;
    }

    @Override
    public int getCapacityRegion(int x, int y, int z) {
        return Math.floorDiv(
            y - machine.getBaseMetaTileEntity()
                .getYCoord() - 1,
            CAPACITY_SEGMENT_HEIGHT);
    }

    @Override
    public StructureError getInvalidCapacityRegionError() {
        return StructureErrors.of("gtnr.machine.BottleInChaos.structure_error.invalid_capacity_region");
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
        int extensions = Math.min(MAX_EXTRA_SEGMENTS, Math.max(0, stackSize.stackSize - 1));
        for (int extra = 0; extra < extensions; extra++) {
            buildPiece(
                getCapacityExtensionPiece(extra),
                stackSize,
                hintsOnly,
                CAPACITY_HORIZONTAL_OFFSET,
                VERTICAL_OFFSET + (extra + 1) * CAPACITY_SEGMENT_HEIGHT - 1,
                CAPACITY_DEPTH_OFFSET);
        }
        buildPiece(
            getCapacityTopPiece(extensions),
            stackSize,
            hintsOnly,
            CAPACITY_HORIZONTAL_OFFSET,
            VERTICAL_OFFSET + extensions * CAPACITY_SEGMENT_HEIGHT,
            CAPACITY_DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment environment) {
        if (machine.mMachine) return -1;
        boolean preview = GregTechAPI.isDummyWorld(
            machine.getBaseMetaTileEntity()
                .getWorld());
        int built = survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            environment,
            preview,
            !preview);
        if (built >= 0) return built;
        int extensions = Math.min(MAX_EXTRA_SEGMENTS, Math.max(0, stackSize.stackSize - 1));
        for (int extra = 0; extra < extensions; extra++) {
            built = survivalBuildPiece(
                getCapacityExtensionPiece(extra),
                stackSize,
                CAPACITY_HORIZONTAL_OFFSET,
                VERTICAL_OFFSET + (extra + 1) * CAPACITY_SEGMENT_HEIGHT - 1,
                CAPACITY_DEPTH_OFFSET,
                elementBudget,
                environment,
                preview,
                !preview);
            if (built >= 0) return built;
        }
        return survivalBuildPiece(
            getCapacityTopPiece(extensions),
            stackSize,
            CAPACITY_HORIZONTAL_OFFSET,
            VERTICAL_OFFSET + extensions * CAPACITY_SEGMENT_HEIGHT,
            CAPACITY_DEPTH_OFFSET,
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
                .addShape(STRUCTURE_PIECE_CAPACITY_BASE_EXTENSION, transpose(CAPACITY_BASE_EXTENSION))
                .addShape(STRUCTURE_PIECE_CAPACITY_EXTENSION, transpose(CAPACITY_EXTENSION))
                .addShape(STRUCTURE_PIECE_CAPACITY_BASE_TOP, transpose(CAPACITY_BASE_TOP))
                .addShape(STRUCTURE_PIECE_CAPACITY_TOP, transpose(CAPACITY_TOP))
                .addChannel(BlockChaosCapacity.STRUCTURE_CHANNEL, 1, getCapacityLevelCount() + 1)
                .addElement('A', ofBlock(BorosilicateGlass.getGlassBlock(), 0))
                .addElement('B', ofFrame(Materials.TungstenSteel))
                .addElement('C', Casings.TPVCoilBlock.asElement())
                .addElement('D', Casings.RobustTungstenSteelMachineCasing.asElement())
                .addElement('E', Casings.StableTitaniumMachineCasing.asElement())
                .addElement('F', Casings.EVMachineCasing.asElement())
                .addElement('G', Casings.Firebricks.asElement())
                .addElement('H', Casings.RobustTungstenSteelMachineCasing.asElement())
                .addElement(
                    'I',
                    withChannel(
                        BlockChaosCapacity.STRUCTURE_CHANNEL,
                        BottleInChaosMachineBase.<BottleInChaos>capacityElement()))
                .addElement('J', threadElement(0, 3))
                .addElement('K', threadElement(1, 3))
                .addElement('L', threadElement(2, 3))
                .addElement('M', prototypeHatches())
                .addElement('R', ofFrame(Materials.Steel))
                .build();
        }
        return structureDefinition;
    }

    private static IStructureElement<BottleInChaos> prototypeHatches() {
        // Seven controller positions; the processing groups already provide dedicated inputs.
        Map<IHatchElement<? super BottleInChaos>, Integer> hatches = new LinkedHashMap<>();
        hatches.put(InputBus, 0);
        hatches.put(InputHatch, 0);
        hatches.put(OutputBus, 1);
        hatches.put(OutputHatch, 1);
        hatches.put(Energy.or(ExoticEnergy), 1);
        hatches.put(ParallelController, 1);
        hatches.put(SpeedController, 1);
        hatches.put(PowerConsumptionController, 1);
        hatches.put(OverclockController, 1);
        HatchElementBuilder<BottleInChaos> builder = HatchElementBuilder.<BottleInChaos>builder()
            .atLeast(hatches);
        var adder = builder.getAdder();
        return hatchElement(
            builder
                .adder(
                    (machine, tile, texture) -> tile != null && isSupportedHatch(tile.getMetaTileEntity())
                        && adder.apply(machine, tile, texture))
                .hatchItemFilterAnd(machine -> stack -> isSupportedHatch(ItemMachines.getMetaTileEntity(stack))),
            1);
    }

    private static boolean isSupportedHatch(IMetaTileEntity tile) {
        if (tile instanceof MTEHatchEnergyTunnel) return false;
        return !(tile instanceof MTEHatch hatch) || !(Energy.matchesHatch(tile) || ExoticEnergy.matchesHatch(tile))
            || hatch.maxWorkingAmperesIn() <= 64;
    }

    @Override
    public MultiblockTooltipBuilder getTooltip() {
        return new com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder().addAutoTooltip(machine)
            .toolTipFinisher();
    }
}
