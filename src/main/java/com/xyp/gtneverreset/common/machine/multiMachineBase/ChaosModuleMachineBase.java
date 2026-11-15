package com.xyp.gtneverreset.common.machine.multiMachineBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.crafting.chaos.processing.ChaosOverclockType;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.IGTHatchAdder;

public abstract class ChaosModuleMachineBase<T extends ChaosModuleMachineBase<T>> extends ChaosLongInputMachineBase<T> {

    protected static final StructureError one_hatch_each_module = StructureErrors
        // #tr gtnr.chaos.structure.one_module_each
        // # Install at most one controller of each parameter module type.
        // # zh_CN 每种参数模块控制器最多安装一个。
        .of("gtnr.chaos.structure.one_module_each");
    protected final Map<MachineModuleType, Collection<ParameterModuleHatch>> modularHatches = new EnumMap<>(
        MachineModuleType.class);
    protected ChaosOverclockType overclockType = ChaosOverclockType.NormalOverclock;

    protected ChaosModuleMachineBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected ChaosModuleMachineBase(String name) {
        super(name);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        repairMachine();
        modularHatches.clear();
        if (!checkMachineMM(tile, stack, errors)) return;
        if (!canMultiplyModularHatchType() && !checkSingleModularHatch()) {
            errors.add(one_hatch_each_module);
            return;
        }
        updateModuleParameters();
    }

    public abstract boolean checkMachineMM(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors);

    @Override
    public CheckRecipeResult checkProcessing() {
        updateModuleParameters();
        return checkProcessingMM();
    }

    public abstract CheckRecipeResult checkProcessingMM();

    private void updateModuleParameters() {
        maxParallel = 1;
        speedBonus = 1;
        euModifier = 1;
        overclockType = ChaosOverclockType.NormalOverclock;
        for (Collection<ParameterModuleHatch> modules : modularHatches.values()) {
            for (ParameterModuleHatch hatch : modules) {
                switch (hatch.getModuleType()) {
                    case Parallel -> maxParallel = (int) Math
                        .min(Integer.MAX_VALUE, (long) maxParallel + hatch.getParameter());
                    case Speed -> speedBonus /= 1f + hatch.getParameter();
                    case PowerConsumption -> euModifier *= (float) hatch.getPowerMultiplier();
                    case Overclock -> overclockType = ChaosOverclockType
                        .checkOverclockType(hatch.getTimeReduction(), hatch.getPowerIncrease());
                    default -> {}
                }
            }
        }
    }

    protected boolean checkSingleModularHatch() {
        return modularHatches.values()
            .stream()
            .allMatch(hatches -> hatches.size() <= 1);
    }

    public abstract Collection<MachineModuleType> getSupportedModularHatchTypes();

    protected abstract boolean canMultiplyModularHatchType();

    private boolean addModule(IGregTechTileEntity tile, int texture, MachineModuleType type) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ParameterModuleHatch hatch)
            || hatch.getModuleType() != type
            || !getSupportedModularHatchTypes().contains(type)) return false;
        hatch.updateTexture(texture);
        hatch.updateCraftingIcon(getMachineCraftingIcon());
        Collection<ParameterModuleHatch> modules = modularHatches.computeIfAbsent(type, key -> new ArrayList<>());
        if (!modules.contains(hatch)) modules.add(hatch);
        return true;
    }

    public enum ModuleElement implements IHatchElement<ChaosModuleMachineBase> {

        ParallelController(MachineModuleType.Parallel),
        SpeedController(MachineModuleType.Speed),
        PowerConsumptionController(MachineModuleType.PowerConsumption),
        OverclockController(MachineModuleType.Overclock);

        private final MachineModuleType type;

        ModuleElement(MachineModuleType type) {
            this.type = type;
        }

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return List.of(ParameterModuleHatch.class);
        }

        @Override
        public IGTHatchAdder<? super ChaosModuleMachineBase> adder() {
            return (machine, tile, texture) -> machine.addModule(tile, texture, type);
        }

        @Override
        public long count(ChaosModuleMachineBase machine) {
            Collection<?> modules = (Collection<?>) machine.modularHatches.get(type);
            return modules == null ? 0 : modules.size();
        }
    }
}
