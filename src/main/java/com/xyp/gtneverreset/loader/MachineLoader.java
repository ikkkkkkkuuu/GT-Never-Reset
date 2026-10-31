package com.xyp.gtneverreset.loader;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.basicMachine.EndCrystalResonator;
import com.xyp.gtneverreset.common.machine.basicMachine.FragmentWorldCollector;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;
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
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.machine.multiblock.LargeBeeBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;
import com.xyp.gtneverreset.common.machine.multiblock.SingularityDataHub;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.enums.ModernMachineTextures;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtneverreset.utils.text.AnimatedText;
import com.xyp.gtnrlib.mod.ModList;

import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;

public final class MachineLoader {

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
        if (ModList.Forestry.isModLoaded()) {
            ItemStack beeCasing = ModsItemlist.ChiselYellowHempcrete.get(1);
            GTUtility.addTexturePage((byte) (LargeBeeBreeder.CASING_TEXTURE_ID >> 7));
            Textures.BlockIcons.setCasingTextureForId(
                LargeBeeBreeder.CASING_TEXTURE_ID,
                beeCasing != null && beeCasing.getItem() instanceof ItemBlock
                    ? TextureFactory.of(Block.getBlockFromItem(beeCasing.getItem()), beeCasing.getItemDamage())
                    : Textures.BlockIcons.getCasingTextureForId(10));
            // #tr gtnr.machine.large_bee_breeder.name
            // # Large Bee Breeder
            // # zh_CN 大型蜜蜂杂交机
            GTNRItemList.LargeBeeBreeder.set(
                new LargeBeeBreeder(
                    GTNRMachineID.LargeBeeBreeder.id,
                    "gtneverreset.large_bee_breeder",
                    StatCollector.translateToLocal("gtnr.machine.large_bee_breeder.name")));
            AnimatedTooltipHandler.addItemTooltip(GTNRItemList.LargeBeeBreeder.get(1), AnimatedText.GT_NEVER_RESET);
        }
    }

    public static void registerHatch() {
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
        GTNRItemList.EndCrystalResonatorLV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorLV.id,
                "gtneverreset.end_crystal_resonator.tier.1",
                VoltageIndex.LV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorLV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorMV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorMV.id,
                "gtneverreset.end_crystal_resonator.tier.2",
                VoltageIndex.MV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorMV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorHV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorHV.id,
                "gtneverreset.end_crystal_resonator.tier.3",
                VoltageIndex.HV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorHV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorEV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorEV.id,
                "gtneverreset.end_crystal_resonator.tier.4",
                VoltageIndex.EV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorEV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorIV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorIV.id,
                "gtneverreset.end_crystal_resonator.tier.5",
                VoltageIndex.IV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorIV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorLuV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorLuV.id,
                "gtneverreset.end_crystal_resonator.tier.6",
                VoltageIndex.LuV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorLuV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorZPM.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorZPM.id,
                "gtneverreset.end_crystal_resonator.tier.7",
                VoltageIndex.ZPM));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorZPM.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorUV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorUV.id,
                "gtneverreset.end_crystal_resonator.tier.8",
                VoltageIndex.UV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorUV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorUHV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorUHV.id,
                "gtneverreset.end_crystal_resonator.tier.9",
                VoltageIndex.UHV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorUHV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorUEV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorUEV.id,
                "gtneverreset.end_crystal_resonator.tier.10",
                VoltageIndex.UEV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorUEV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorUIV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorUIV.id,
                "gtneverreset.end_crystal_resonator.tier.11",
                VoltageIndex.UIV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorUIV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorUMV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorUMV.id,
                "gtneverreset.end_crystal_resonator.tier.12",
                VoltageIndex.UMV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorUMV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorUXV.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorUXV.id,
                "gtneverreset.end_crystal_resonator.tier.13",
                VoltageIndex.UXV));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorUXV.get(1), AnimatedText.GT_NEVER_RESET);
        GTNRItemList.EndCrystalResonatorMAX.set(
            new EndCrystalResonator(
                GTNRMachineID.EndCrystalResonatorMAX.id,
                "gtneverreset.end_crystal_resonator.tier.14",
                VoltageIndex.MAX));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.EndCrystalResonatorMAX.get(1), AnimatedText.GT_NEVER_RESET);
    }

    private static void registerModernFluidMachines() {
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.1.name
        // # Basic Mixer
        // # zh_CN 基础搅拌机
        GTNRItemList.ModernMixerLV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerLV.id,
                "gtneverreset.modern_mixer.tier.1",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.1.name"),
                VoltageIndex.LV,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerLV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.2.name
        // # Advanced Mixer
        // # zh_CN 进阶搅拌机
        GTNRItemList.ModernMixerMV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerMV.id,
                "gtneverreset.modern_mixer.tier.2",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.2.name"),
                VoltageIndex.MV,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerMV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.3.name
        // # Advanced Mixer II
        // # zh_CN 进阶搅拌机 II
        GTNRItemList.ModernMixerHV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerHV.id,
                "gtneverreset.modern_mixer.tier.3",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.3.name"),
                VoltageIndex.HV,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerHV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.4.name
        // # Advanced Mixer III
        // # zh_CN 进阶搅拌机 III
        GTNRItemList.ModernMixerEV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerEV.id,
                "gtneverreset.modern_mixer.tier.4",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.4.name"),
                VoltageIndex.EV,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerEV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.5.name
        // # Advanced Mixer IV
        // # zh_CN 进阶搅拌机 IV
        GTNRItemList.ModernMixerIV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerIV.id,
                "gtneverreset.modern_mixer.tier.5",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.5.name"),
                VoltageIndex.IV,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerIV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.6.name
        // # Elite Mixer
        // # zh_CN 精英搅拌机
        GTNRItemList.ModernMixerLuV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerLuV.id,
                "gtneverreset.modern_mixer.tier.6",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.6.name"),
                VoltageIndex.LuV,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerLuV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.7.name
        // # Elite Mixer II
        // # zh_CN 精英搅拌机 II
        GTNRItemList.ModernMixerZPM.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerZPM.id,
                "gtneverreset.modern_mixer.tier.7",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.7.name"),
                VoltageIndex.ZPM,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerZPM.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_mixer.tier.8.name
        // # Ultimate Matter Organizer
        // # zh_CN 终极物质组织者
        GTNRItemList.ModernMixerUV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernMixerUV.id,
                "gtneverreset.modern_mixer.tier.8",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_mixer.tier.8.name"),
                VoltageIndex.UV,
                GTNRRecipeMaps.MODERN_MIXER_RECIPES,
                6,
                1,
                2,
                1,
                ModernMachineTextures.MIXER,
                SoundResource.GTCEU_LOOP_MIXER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernMixerUV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.1.name
        // # Basic Electrolyzer
        // # zh_CN 基础电解机
        GTNRItemList.ModernElectrolyzerLV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerLV.id,
                "gtneverreset.modern_electrolyzer.tier.1",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.1.name"),
                VoltageIndex.LV,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerLV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.2.name
        // # Advanced Electrolyzer
        // # zh_CN 进阶电解机
        GTNRItemList.ModernElectrolyzerMV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerMV.id,
                "gtneverreset.modern_electrolyzer.tier.2",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.2.name"),
                VoltageIndex.MV,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerMV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.3.name
        // # Advanced Electrolyzer II
        // # zh_CN 进阶电解机 II
        GTNRItemList.ModernElectrolyzerHV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerHV.id,
                "gtneverreset.modern_electrolyzer.tier.3",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.3.name"),
                VoltageIndex.HV,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerHV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.4.name
        // # Advanced Electrolyzer III
        // # zh_CN 进阶电解机 III
        GTNRItemList.ModernElectrolyzerEV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerEV.id,
                "gtneverreset.modern_electrolyzer.tier.4",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.4.name"),
                VoltageIndex.EV,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerEV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.5.name
        // # Molecular Disintegrator E-4908
        // # zh_CN 分子分解者 E-4908
        GTNRItemList.ModernElectrolyzerIV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerIV.id,
                "gtneverreset.modern_electrolyzer.tier.5",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.5.name"),
                VoltageIndex.IV,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerIV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.6.name
        // # Elite Electrolyzer
        // # zh_CN 精英电解机
        GTNRItemList.ModernElectrolyzerLuV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerLuV.id,
                "gtneverreset.modern_electrolyzer.tier.6",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.6.name"),
                VoltageIndex.LuV,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerLuV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.7.name
        // # Elite Electrolyzer II
        // # zh_CN 精英电解机 II
        GTNRItemList.ModernElectrolyzerZPM.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerZPM.id,
                "gtneverreset.modern_electrolyzer.tier.7",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.7.name"),
                VoltageIndex.ZPM,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerZPM.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_electrolyzer.tier.8.name
        // # Ultimate Ionizer
        // # zh_CN 终极离子发生器
        GTNRItemList.ModernElectrolyzerUV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernElectrolyzerUV.id,
                "gtneverreset.modern_electrolyzer.tier.8",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_electrolyzer.tier.8.name"),
                VoltageIndex.UV,
                GTNRRecipeMaps.MODERN_ELECTROLYZER_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.ELECTROLYZER,
                SoundResource.GTCEU_LOOP_ELECTROLYZER));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernElectrolyzerUV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.1.name
        // # Basic Chemical Reactor
        // # zh_CN 基础化学反应釜
        GTNRItemList.ModernChemicalReactorLV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorLV.id,
                "gtneverreset.modern_chemical_reactor.tier.1",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.1.name"),
                VoltageIndex.LV,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernChemicalReactorLV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.2.name
        // # Advanced Chemical Reactor
        // # zh_CN 进阶化学反应釜
        GTNRItemList.ModernChemicalReactorMV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorMV.id,
                "gtneverreset.modern_chemical_reactor.tier.2",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.2.name"),
                VoltageIndex.MV,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernChemicalReactorMV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.3.name
        // # Advanced Chemical Reactor II
        // # zh_CN 进阶化学反应釜 II
        GTNRItemList.ModernChemicalReactorHV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorHV.id,
                "gtneverreset.modern_chemical_reactor.tier.3",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.3.name"),
                VoltageIndex.HV,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernChemicalReactorHV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.4.name
        // # Advanced Chemical Reactor III
        // # zh_CN 进阶化学反应釜 III
        GTNRItemList.ModernChemicalReactorEV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorEV.id,
                "gtneverreset.modern_chemical_reactor.tier.4",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.4.name"),
                VoltageIndex.EV,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernChemicalReactorEV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.5.name
        // # Advanced Chemical Reactor IV
        // # zh_CN 进阶化学反应釜 IV
        GTNRItemList.ModernChemicalReactorIV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorIV.id,
                "gtneverreset.modern_chemical_reactor.tier.5",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.5.name"),
                VoltageIndex.IV,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernChemicalReactorIV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.6.name
        // # Elite Chemical Reactor
        // # zh_CN 精英化学反应釜
        GTNRItemList.ModernChemicalReactorLuV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorLuV.id,
                "gtneverreset.modern_chemical_reactor.tier.6",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.6.name"),
                VoltageIndex.LuV,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler
            .addItemTooltip(GTNRItemList.ModernChemicalReactorLuV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.7.name
        // # Elite Chemical Reactor II
        // # zh_CN 精英化学反应釜 II
        GTNRItemList.ModernChemicalReactorZPM.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorZPM.id,
                "gtneverreset.modern_chemical_reactor.tier.7",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.7.name"),
                VoltageIndex.ZPM,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler
            .addItemTooltip(GTNRItemList.ModernChemicalReactorZPM.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.8.name
        // # Ultimate Chemical Performer
        // # zh_CN 终极化学反应执行者
        GTNRItemList.ModernChemicalReactorUV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernChemicalReactorUV.id,
                "gtneverreset.modern_chemical_reactor.tier.8",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_chemical_reactor.tier.8.name"),
                VoltageIndex.UV,
                GTNRRecipeMaps.MODERN_CHEMICAL_RECIPES,
                6,
                6,
                6,
                6,
                ModernMachineTextures.CHEMICAL_REACTOR,
                SoundResource.GTCEU_LOOP_CHEMICAL));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernChemicalReactorUV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.1.name
        // # Basic Centrifuge
        // # zh_CN 基础离心机
        GTNRItemList.ModernCentrifugeLV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeLV.id,
                "gtneverreset.modern_centrifuge.tier.1",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.1.name"),
                VoltageIndex.LV,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeLV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.2.name
        // # Advanced Centrifuge
        // # zh_CN 进阶离心机
        GTNRItemList.ModernCentrifugeMV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeMV.id,
                "gtneverreset.modern_centrifuge.tier.2",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.2.name"),
                VoltageIndex.MV,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeMV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.3.name
        // # Turbo Centrifuge
        // # zh_CN 涡轮离心机
        GTNRItemList.ModernCentrifugeHV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeHV.id,
                "gtneverreset.modern_centrifuge.tier.3",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.3.name"),
                VoltageIndex.HV,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeHV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.4.name
        // # Molecular Separator
        // # zh_CN 分子离心机
        GTNRItemList.ModernCentrifugeEV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeEV.id,
                "gtneverreset.modern_centrifuge.tier.4",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.4.name"),
                VoltageIndex.EV,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeEV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.5.name
        // # Molecular Cyclone
        // # zh_CN 旋风分子离心机
        GTNRItemList.ModernCentrifugeIV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeIV.id,
                "gtneverreset.modern_centrifuge.tier.5",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.5.name"),
                VoltageIndex.IV,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeIV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.6.name
        // # Elite Centrifuge
        // # zh_CN 精英离心机
        GTNRItemList.ModernCentrifugeLuV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeLuV.id,
                "gtneverreset.modern_centrifuge.tier.6",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.6.name"),
                VoltageIndex.LuV,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeLuV.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.7.name
        // # Elite Centrifuge II
        // # zh_CN 精英离心机 II
        GTNRItemList.ModernCentrifugeZPM.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeZPM.id,
                "gtneverreset.modern_centrifuge.tier.7",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.7.name"),
                VoltageIndex.ZPM,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeZPM.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gt.blockmachines.gtneverreset.modern_centrifuge.tier.8.name
        // # Ultimate Molecular Tornado
        // # zh_CN 终极分子龙卷风
        GTNRItemList.ModernCentrifugeUV.set(
            new ModernFluidMachine(
                GTNRMachineID.ModernCentrifugeUV.id,
                "gtneverreset.modern_centrifuge.tier.8",
                StatCollector.translateToLocal("gt.blockmachines.gtneverreset.modern_centrifuge.tier.8.name"),
                VoltageIndex.UV,
                GTNRRecipeMaps.MODERN_CENTRIFUGE_RECIPES,
                2,
                6,
                1,
                6,
                ModernMachineTextures.CENTRIFUGE,
                SoundResource.GTCEU_LOOP_CENTRIFUGE));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.ModernCentrifugeUV.get(1), AnimatedText.GT_NEVER_RESET);
    }

}
