package com.xyp.gtneverreset.loader.machine;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternMirror;
import com.xyp.gtneverreset.common.machine.hatch.MEDataAccessHatch;
import com.xyp.gtneverreset.common.machine.hatch.MEOutputAssembly;
import com.xyp.gtneverreset.common.machine.hatch.MaxCapacityMEOutputBus;
import com.xyp.gtneverreset.common.machine.hatch.MaxCapacityMEOutputHatch;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputBus;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputHatch;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtneverreset.common.machine.hatch.VaultPortHatch;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.text.AnimatedText;

public final class HatchLoader {

    public static void registry() {
        // #tr gtnr.hatch.me_data_access.name
        // # Data Access Hatch (ME, IV)
        // # zh_CN 数据访问仓 (ME, IV)
        GTNRItemList.MEDataAccessHatch.set(
            new MEDataAccessHatch(
                GTNRMachineID.MEDataAccessHatch.id,
                "gtneverreset.me_data_access",
                StatCollector.translateToLocal("gtnr.hatch.me_data_access.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.MEDataAccessHatch.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.hatch.super_advanced_me_input_hatch.name
        // # Super Advanced Stocking Input Hatch (ME)
        // # zh_CN 超级 ME 输入仓
        GTNRItemList.SuperAdvancedMEInputHatch.set(
            new SuperAdvancedMEInputHatch(
                GTNRMachineID.SuperAdvancedMEInputHatch.id,
                "gtneverreset.super_advanced_me_input_hatch",
                StatCollector.translateToLocal("gtnr.hatch.super_advanced_me_input_hatch.name")));
        AnimatedTooltipHandler
            .addItemTooltip(GTNRItemList.SuperAdvancedMEInputHatch.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.hatch.super_advanced_me_input_bus.name
        // # Super Advanced Stocking Input Bus (ME)
        // # zh_CN 超级 ME 输入总线
        GTNRItemList.SuperAdvancedMEInputBus.set(
            new SuperAdvancedMEInputBus(
                GTNRMachineID.SuperAdvancedMEInputBus.id,
                "gtneverreset.super_advanced_me_input_bus",
                StatCollector.translateToLocal("gtnr.hatch.super_advanced_me_input_bus.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.SuperAdvancedMEInputBus.get(1), AnimatedText.GT_NEVER_RESET);

        // #tr gtnr.hatch.max_capacity_me_output_bus.name
        // # Max Capacity ME Output Bus
        // # zh_CN 最大容量 ME 输出总线
        GTNRItemList.MaxCapacityMEOutputBus.set(
            new MaxCapacityMEOutputBus(
                GTNRMachineID.MaxCapacityMEOutputBus.id,
                "gtneverreset.max_capacity_me_output_bus",
                StatCollector.translateToLocal("gtnr.hatch.max_capacity_me_output_bus.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.MaxCapacityMEOutputBus.get(1), AnimatedText.GT_NEVER_RESET);

        // #tr gtnr.hatch.max_capacity_me_output_hatch.name
        // # Max Capacity ME Output Hatch
        // # zh_CN 最大容量 ME 输出仓
        GTNRItemList.MaxCapacityMEOutputHatch.set(
            new MaxCapacityMEOutputHatch(
                GTNRMachineID.MaxCapacityMEOutputHatch.id,
                "gtneverreset.max_capacity_me_output_hatch",
                StatCollector.translateToLocal("gtnr.hatch.max_capacity_me_output_hatch.name")));
        AnimatedTooltipHandler
            .addItemTooltip(GTNRItemList.MaxCapacityMEOutputHatch.get(1), AnimatedText.GT_NEVER_RESET);

        // #tr gtnr.hatch.me_output_assembly.name
        // # ME Output Assembly
        // # zh_CN ME 输出总成
        GTNRItemList.MEOutputAssembly.set(
            new MEOutputAssembly(
                GTNRMachineID.MEOutputAssembly.id,
                "gtneverreset.me_output_assembly",
                StatCollector.translateToLocal("gtnr.hatch.me_output_assembly.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.MEOutputAssembly.get(1), AnimatedText.GT_NEVER_RESET);

        GTNRItemList.StaticParallelControllerT1.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT1.id,
                "StaticParallelControllerT1",
                MachineModuleType.Parallel,
                1,
                false));
        GTNRItemList.StaticParallelControllerT2.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT2.id,
                "StaticParallelControllerT2",
                MachineModuleType.Parallel,
                2,
                false));
        GTNRItemList.StaticParallelControllerT3.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT3.id,
                "StaticParallelControllerT3",
                MachineModuleType.Parallel,
                3,
                false));
        GTNRItemList.StaticParallelControllerT4.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT4.id,
                "StaticParallelControllerT4",
                MachineModuleType.Parallel,
                4,
                false));
        GTNRItemList.StaticParallelControllerT5.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT5.id,
                "StaticParallelControllerT5",
                MachineModuleType.Parallel,
                5,
                false));
        GTNRItemList.StaticParallelControllerT6.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT6.id,
                "StaticParallelControllerT6",
                MachineModuleType.Parallel,
                6,
                false));
        GTNRItemList.StaticParallelControllerT7.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT7.id,
                "StaticParallelControllerT7",
                MachineModuleType.Parallel,
                7,
                false));
        GTNRItemList.StaticParallelControllerT8.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticParallelControllerT8.id,
                "StaticParallelControllerT8",
                MachineModuleType.Parallel,
                8,
                false));
        GTNRItemList.DynamicParallelControllerT1.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT1.id,
                "DynamicParallelControllerT1",
                MachineModuleType.Parallel,
                1,
                true));
        GTNRItemList.DynamicParallelControllerT2.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT2.id,
                "DynamicParallelControllerT2",
                MachineModuleType.Parallel,
                2,
                true));
        GTNRItemList.DynamicParallelControllerT3.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT3.id,
                "DynamicParallelControllerT3",
                MachineModuleType.Parallel,
                3,
                true));
        GTNRItemList.DynamicParallelControllerT4.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT4.id,
                "DynamicParallelControllerT4",
                MachineModuleType.Parallel,
                4,
                true));
        GTNRItemList.DynamicParallelControllerT5.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT5.id,
                "DynamicParallelControllerT5",
                MachineModuleType.Parallel,
                5,
                true));
        GTNRItemList.DynamicParallelControllerT6.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT6.id,
                "DynamicParallelControllerT6",
                MachineModuleType.Parallel,
                6,
                true));
        GTNRItemList.DynamicParallelControllerT7.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT7.id,
                "DynamicParallelControllerT7",
                MachineModuleType.Parallel,
                7,
                true));
        GTNRItemList.DynamicParallelControllerT8.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicParallelControllerT8.id,
                "DynamicParallelControllerT8",
                MachineModuleType.Parallel,
                8,
                true));
        GTNRItemList.StaticSpeedControllerT1.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT1.id,
                "StaticSpeedControllerT1",
                MachineModuleType.Speed,
                1,
                false));
        GTNRItemList.StaticSpeedControllerT2.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT2.id,
                "StaticSpeedControllerT2",
                MachineModuleType.Speed,
                2,
                false));
        GTNRItemList.StaticSpeedControllerT3.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT3.id,
                "StaticSpeedControllerT3",
                MachineModuleType.Speed,
                3,
                false));
        GTNRItemList.StaticSpeedControllerT4.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT4.id,
                "StaticSpeedControllerT4",
                MachineModuleType.Speed,
                4,
                false));
        GTNRItemList.StaticSpeedControllerT5.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT5.id,
                "StaticSpeedControllerT5",
                MachineModuleType.Speed,
                5,
                false));
        GTNRItemList.StaticSpeedControllerT6.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT6.id,
                "StaticSpeedControllerT6",
                MachineModuleType.Speed,
                6,
                false));
        GTNRItemList.StaticSpeedControllerT7.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT7.id,
                "StaticSpeedControllerT7",
                MachineModuleType.Speed,
                7,
                false));
        GTNRItemList.StaticSpeedControllerT8.set(
            new ParameterModuleHatch(
                GTNRMachineID.StaticSpeedControllerT8.id,
                "StaticSpeedControllerT8",
                MachineModuleType.Speed,
                8,
                false));
        GTNRItemList.DynamicSpeedControllerT1.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT1.id,
                "DynamicSpeedControllerT1",
                MachineModuleType.Speed,
                1,
                true));
        GTNRItemList.DynamicSpeedControllerT2.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT2.id,
                "DynamicSpeedControllerT2",
                MachineModuleType.Speed,
                2,
                true));
        GTNRItemList.DynamicSpeedControllerT3.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT3.id,
                "DynamicSpeedControllerT3",
                MachineModuleType.Speed,
                3,
                true));
        GTNRItemList.DynamicSpeedControllerT4.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT4.id,
                "DynamicSpeedControllerT4",
                MachineModuleType.Speed,
                4,
                true));
        GTNRItemList.DynamicSpeedControllerT5.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT5.id,
                "DynamicSpeedControllerT5",
                MachineModuleType.Speed,
                5,
                true));
        GTNRItemList.DynamicSpeedControllerT6.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT6.id,
                "DynamicSpeedControllerT6",
                MachineModuleType.Speed,
                6,
                true));
        GTNRItemList.DynamicSpeedControllerT7.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT7.id,
                "DynamicSpeedControllerT7",
                MachineModuleType.Speed,
                7,
                true));
        GTNRItemList.DynamicSpeedControllerT8.set(
            new ParameterModuleHatch(
                GTNRMachineID.DynamicSpeedControllerT8.id,
                "DynamicSpeedControllerT8",
                MachineModuleType.Speed,
                8,
                true));
        GTNRItemList.PowerConsumptionControllerT1.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT1.id,
                "PowerConsumptionControllerT1",
                MachineModuleType.PowerConsumption,
                1,
                false));
        GTNRItemList.PowerConsumptionControllerT2.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT2.id,
                "PowerConsumptionControllerT2",
                MachineModuleType.PowerConsumption,
                2,
                false));
        GTNRItemList.PowerConsumptionControllerT3.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT3.id,
                "PowerConsumptionControllerT3",
                MachineModuleType.PowerConsumption,
                3,
                false));
        GTNRItemList.PowerConsumptionControllerT4.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT4.id,
                "PowerConsumptionControllerT4",
                MachineModuleType.PowerConsumption,
                4,
                false));
        GTNRItemList.PowerConsumptionControllerT5.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT5.id,
                "PowerConsumptionControllerT5",
                MachineModuleType.PowerConsumption,
                5,
                false));
        GTNRItemList.PowerConsumptionControllerT6.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT6.id,
                "PowerConsumptionControllerT6",
                MachineModuleType.PowerConsumption,
                6,
                false));
        GTNRItemList.PowerConsumptionControllerT7.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT7.id,
                "PowerConsumptionControllerT7",
                MachineModuleType.PowerConsumption,
                7,
                false));
        GTNRItemList.PowerConsumptionControllerT8.set(
            new ParameterModuleHatch(
                GTNRMachineID.PowerConsumptionControllerT8.id,
                "PowerConsumptionControllerT8",
                MachineModuleType.PowerConsumption,
                8,
                false));
        GTNRItemList.LowSpeedPerfectOverclockController.set(
            new ParameterModuleHatch(
                GTNRMachineID.LowSpeedPerfectOverclockController.id,
                "LowSpeedPerfectOverclockController",
                MachineModuleType.Overclock,
                1,
                false));
        GTNRItemList.PerfectOverclockController.set(
            new ParameterModuleHatch(
                GTNRMachineID.PerfectOverclockController.id,
                "PerfectOverclockController",
                MachineModuleType.Overclock,
                2,
                false));
        GTNRItemList.SingularityPerfectOverclockController.set(
            new ParameterModuleHatch(
                GTNRMachineID.SingularityPerfectOverclockController.id,
                "SingularityPerfectOverclockController",
                MachineModuleType.Overclock,
                3,
                false));

        // #tr gtnr.hatch.super_pattern.name
        // # Super Pattern Input Assembly (ME)
        // # zh_CN 超级样板输入总成 (ME)
        GTNRItemList.SuperMTEHatchCraftingInputME.set(
            new SuperMTEHatchCraftingInputME(
                GTNRMachineID.SuperCraftingInputME.id,
                "gtneverreset.super_pattern_input",
                StatCollector.translateToLocal("gtnr.hatch.super_pattern.name"),
                true));
        AnimatedTooltipHandler
            .addItemTooltip(GTNRItemList.SuperMTEHatchCraftingInputME.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.hatch.circuit_pattern.name
        // # Circuit Pattern Input Buffer (ME)
        // # zh_CN 电路样板输入总成 (ME)
        GTNRItemList.CircuitMEPatternBuffer.set(
            new CircuitMEPatternBuffer(
                GTNRMachineID.CircuitMEPatternBuffer.id,
                "gtneverreset.circuit_pattern_buffer",
                StatCollector.translateToLocal("gtnr.hatch.circuit_pattern.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.CircuitMEPatternBuffer.get(1), AnimatedText.GT_NEVER_RESET);

        // #tr gtnr.hatch.super_mirror.name
        // # Super Pattern Input Mirror (ME)
        // # zh_CN 超级样板输入镜像 (ME)
        GTNRItemList.SuperMTEHatchCraftingInputSlave.set(
            new SuperMTEHatchCraftingInputSlave(
                GTNRMachineID.SuperCraftingInputSlave.id,
                "gtneverreset.super_pattern_mirror",
                StatCollector.translateToLocal("gtnr.hatch.super_mirror.name")));
        AnimatedTooltipHandler
            .addItemTooltip(GTNRItemList.SuperMTEHatchCraftingInputSlave.get(1), AnimatedText.GT_NEVER_RESET);

        // #tr gtnr.hatch.circuit_mirror.name
        // # Circuit Pattern Input Mirror (ME)
        // # zh_CN 电路样板输入镜像 (ME)
        GTNRItemList.CircuitMEPatternMirror.set(
            new CircuitMEPatternMirror(
                GTNRMachineID.CircuitMEPatternMirror.id,
                "gtneverreset.circuit_pattern_mirror",
                StatCollector.translateToLocal("gtnr.hatch.circuit_mirror.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.CircuitMEPatternMirror.get(1), AnimatedText.GT_NEVER_RESET);

        // #tr gtnr.hatch.vault_port.name
        // # Vault Port Hatch
        // # zh_CN 保险库接口
        GTNRItemList.VaultPortHatch.set(
            new VaultPortHatch(
                GTNRMachineID.VaultPortHatch.id,
                "gtneverreset.vault_port",
                StatCollector.translateToLocal("gtnr.hatch.vault_port.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.VaultPortHatch.get(1), AnimatedText.GT_NEVER_RESET);
    }

}
