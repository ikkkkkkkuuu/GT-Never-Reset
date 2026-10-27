package com.xyp.gtneverreset.loader;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.basicMachine.FragmentWorldCollector;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternMirror;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
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
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.enums.ModernMachineTextures;
import com.xyp.gtneverreset.utils.text.AnimatedText;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.VoltageIndex;

public final class MachineLoader {

    public static final GTNRItemList[] STATIC_PARALLEL = { GTNRItemList.StaticParallelControllerT1,
        GTNRItemList.StaticParallelControllerT2, GTNRItemList.StaticParallelControllerT3,
        GTNRItemList.StaticParallelControllerT4, GTNRItemList.StaticParallelControllerT5,
        GTNRItemList.StaticParallelControllerT6, GTNRItemList.StaticParallelControllerT7,
        GTNRItemList.StaticParallelControllerT8 };

    public static final GTNRItemList[] DYNAMIC_PARALLEL = { GTNRItemList.DynamicParallelControllerT1,
        GTNRItemList.DynamicParallelControllerT2, GTNRItemList.DynamicParallelControllerT3,
        GTNRItemList.DynamicParallelControllerT4, GTNRItemList.DynamicParallelControllerT5,
        GTNRItemList.DynamicParallelControllerT6, GTNRItemList.DynamicParallelControllerT7,
        GTNRItemList.DynamicParallelControllerT8 };

    public static final GTNRItemList[] STATIC_SPEED = { GTNRItemList.StaticSpeedControllerT1,
        GTNRItemList.StaticSpeedControllerT2, GTNRItemList.StaticSpeedControllerT3,
        GTNRItemList.StaticSpeedControllerT4, GTNRItemList.StaticSpeedControllerT5,
        GTNRItemList.StaticSpeedControllerT6, GTNRItemList.StaticSpeedControllerT7,
        GTNRItemList.StaticSpeedControllerT8 };

    public static final GTNRItemList[] DYNAMIC_SPEED = { GTNRItemList.DynamicSpeedControllerT1,
        GTNRItemList.DynamicSpeedControllerT2, GTNRItemList.DynamicSpeedControllerT3,
        GTNRItemList.DynamicSpeedControllerT4, GTNRItemList.DynamicSpeedControllerT5,
        GTNRItemList.DynamicSpeedControllerT6, GTNRItemList.DynamicSpeedControllerT7,
        GTNRItemList.DynamicSpeedControllerT8 };

    public static final GTNRItemList[] POWER = { GTNRItemList.PowerConsumptionControllerT1,
        GTNRItemList.PowerConsumptionControllerT2, GTNRItemList.PowerConsumptionControllerT3,
        GTNRItemList.PowerConsumptionControllerT4, GTNRItemList.PowerConsumptionControllerT5,
        GTNRItemList.PowerConsumptionControllerT6, GTNRItemList.PowerConsumptionControllerT7,
        GTNRItemList.PowerConsumptionControllerT8 };

    public static final GTNRItemList[] OVERCLOCK = { GTNRItemList.LowSpeedPerfectOverclockController,
        GTNRItemList.PerfectOverclockController, GTNRItemList.SingularityPerfectOverclockController };

    public static final ItemStack[] MODERN_MIXERS = new ItemStack[VoltageIndex.UV + 1];
    public static final ItemStack[] MODERN_ELECTROLYZERS = new ItemStack[VoltageIndex.UV + 1];
    public static final ItemStack[] MODERN_CHEMICAL_REACTORS = new ItemStack[VoltageIndex.UV + 1];

    public static void registry() {
        registerHatch();
        registerMachines();
        registerBasicMachine();
    }

    public static void registerMachines() {
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

        // #tr gtnr.machine.singularity_data_hub.name
        // # Vault
        // # zh_CN 保险库
        GTNRItemList.SingularityDataHub.set(
            new SingularityDataHub(
                GTNRMachineID.SingularityDataHub.id,
                "gtneverreset.singularity_data_hub",
                StatCollector.translateToLocal("gtnr.machine.singularity_data_hub.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.SingularityDataHub.get(1), AnimatedText.GT_NEVER_RESET);
    }

    public static void registerHatch() {
        registerModuleHatches(STATIC_PARALLEL, MachineModuleType.Parallel, false);
        registerModuleHatches(DYNAMIC_PARALLEL, MachineModuleType.Parallel, true);
        registerModuleHatches(STATIC_SPEED, MachineModuleType.Speed, false);
        registerModuleHatches(DYNAMIC_SPEED, MachineModuleType.Speed, true);
        registerModuleHatches(POWER, MachineModuleType.PowerConsumption, false);
        registerModuleHatches(OVERCLOCK, MachineModuleType.Overclock, false);

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

    public static void registerBasicMachine() {
        // #tr gtnr.fragment_world_collector.name
        // # Fragment World Collector
        // # zh_CN 碎片世界采集器
        GTNRItemList.FragmentWorldCollector.set(
            new FragmentWorldCollector(
                GTNRMachineID.FragmentWorldCollector.id,
                "gtneverreset.fragment_world_collector",
                StatCollector.translateToLocal("gtnr.fragment_world_collector.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.FragmentWorldCollector.get(1), AnimatedText.GT_NEVER_RESET);
        registerModernFluidMachines();
    }

    private static void registerModuleHatches(GTNRItemList[] entries, MachineModuleType type, boolean dynamic) {
        for (int i = 0; i < entries.length; i++) {
            GTNRItemList entry = entries[i];
            entry.set(
                new ParameterModuleHatch(GTNRMachineID.valueOf(entry.name()).id, entry.name(), type, i + 1, dynamic));
        }
    }

    private static void registerModernFluidMachines() {
        for (int tier = VoltageIndex.LV; tier <= VoltageIndex.UV; tier++) {
            int mixerTankCapacity = 4000 * (Math.min(tier, VoltageIndex.HV) + 1);
            // #tr gtnr.machine.modern_mixer.name
            // # %s Small Mixer
            // # zh_CN %s 小型搅拌机
            MODERN_MIXERS[tier] = new ModernFluidMachine(
                GTNRMachineID.ModernMixerLV.id + tier - VoltageIndex.LV,
                "gtneverreset.modern_mixer.tier." + tier,
                StatCollector.translateToLocalFormatted("gtnr.machine.modern_mixer.name", GTValues.VN[tier]),
                tier,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                mixerTankCapacity,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER).getStackForm(1);
            // #tr gtnr.machine.modern_electrolyzer.name
            // # %s Small Electrolyzer
            // # zh_CN %s 小型电解机
            MODERN_ELECTROLYZERS[tier] = new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerLV.id + tier - VoltageIndex.LV,
                "gtneverreset.modern_electrolyzer.tier." + tier,
                StatCollector.translateToLocalFormatted("gtnr.machine.modern_electrolyzer.name", GTValues.VN[tier]),
                tier,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                mixerTankCapacity * 4,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER).getStackForm(1);
            // #tr gtnr.machine.modern_chemical_reactor.name
            // # %s Small Chemical Reactor
            // # zh_CN %s 小型化学反应釜
            MODERN_CHEMICAL_REACTORS[tier] = new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorLV.id + tier - VoltageIndex.LV,
                "gtneverreset.modern_chemical_reactor.tier." + tier,
                StatCollector.translateToLocalFormatted("gtnr.machine.modern_chemical_reactor.name", GTValues.VN[tier]),
                tier,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                2,
                2,
                3,
                2,
                16000,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL).getStackForm(1);
            AnimatedTooltipHandler.addItemTooltip(MODERN_MIXERS[tier], AnimatedText.GT_NEVER_RESET);
            AnimatedTooltipHandler.addItemTooltip(MODERN_ELECTROLYZERS[tier], AnimatedText.GT_NEVER_RESET);
            AnimatedTooltipHandler.addItemTooltip(MODERN_CHEMICAL_REACTORS[tier], AnimatedText.GT_NEVER_RESET);
        }
    }

}
