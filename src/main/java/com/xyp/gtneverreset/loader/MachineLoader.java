package com.xyp.gtneverreset.loader;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternMirror;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtneverreset.common.machine.hatch.VaultPortHatch;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;
import com.xyp.gtneverreset.common.machine.multiblock.SingularityDataHub;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.text.AnimatedText;

public final class MachineLoader {

    private MachineLoader() {}

    public static void registerMachines() {
        ModuleHatchLoader.register();
        // #tr gtnr.machine.large_ore_processor.name
        // # Large Ore Processor
        // # zh_CN 大型矿石处理机
        GTNRItemList.LargeOreProcessor.set(
            new LargeOreProcessor(
                GTNRMachineID.LargeOreProcessor.id,
                "gtneverreset.large_ore_processor",
                StatCollector.translateToLocal("gtnr.machine.large_ore_processor.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.LargeOreProcessor.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.machine.large_furnace.name
        // # Large Furnace
        // # zh_CN 大型熔炉
        GTNRItemList.LargeFurnace.set(
            new LargeFurnace(
                GTNRMachineID.LargeFurnace.id,
                "gtneverreset.large_furnace",
                StatCollector.translateToLocal("gtnr.machine.large_furnace.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.LargeFurnace.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.QuantumComputer.name
        // # Quantum Computer
        // # zh_CN 量子计算机
        GTNRItemList.QuantumComputer.set(
            new QuantumComputer(
                GTNRMachineID.QuantumComputer.id,
                "gtneverreset.quantum_computer",
                StatCollector.translateToLocal("gtnr.QuantumComputer.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.QuantumComputer.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.AssemblerMatrix.name
        // # Assembler Matrix
        // # zh_CN 装配矩阵
        GTNRItemList.AssemblerMatrix.set(
            new AssemblerMatrix(
                GTNRMachineID.AssemblerMatrix.id,
                "gtneverreset.assembler_matrix",
                StatCollector.translateToLocal("gtnr.AssemblerMatrix.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.AssemblerMatrix.get(1), AnimatedText.GT_NEVER_RESET);
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

        // #tr gtnr.machine.singularity_data_hub.name
        // # Vault
        // # zh_CN 保险库
        GTNRItemList.SingularityDataHub.set(
            new SingularityDataHub(
                GTNRMachineID.SingularityDataHub.id,
                "gtneverreset.singularity_data_hub",
                StatCollector.translateToLocal("gtnr.machine.singularity_data_hub.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.SingularityDataHub.get(1), AnimatedText.GT_NEVER_RESET);

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
