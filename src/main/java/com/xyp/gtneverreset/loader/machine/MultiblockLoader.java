package com.xyp.gtneverreset.loader.machine;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;
import com.xyp.gtneverreset.common.machine.multiblock.HadronCatalyticRefinery;
import com.xyp.gtneverreset.common.machine.multiblock.LargeBeeBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.LargeCropBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.common.machine.multiblock.LightHunterSpaceStation;
import com.xyp.gtneverreset.common.machine.multiblock.NexusSatelliteFactory;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;
import com.xyp.gtneverreset.common.machine.multiblock.SingularityDataHub;
import com.xyp.gtneverreset.common.machine.multiblock.StarCoreStripper;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.config.ChaosConfig;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.text.AnimatedText;
import com.xyp.gtnrlib.mod.ModList;

import gregtech.api.enums.Textures;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;

public final class MultiblockLoader {

    public static void registry() {
        GTNRItemList.LightHunterSpaceStation.set(
            new LightHunterSpaceStation(
                GTNRMachineID.LightHunterSpaceStation.id,
                "gtneverreset.light_hunter_station",
                StatCollector.translateToLocal("gtnr.machine.light_hunter.name")));
        registerSatellites();
        // #tr gtnr.machine.star_core_stripper.name
        // # Star Core Stripper
        // # zh_CN 星核剥离装置
        GTNRItemList.StarCoreStripper.set(
            new StarCoreStripper(
                GTNRMachineID.StarCoreStripper.id,
                "gtneverreset.star_core_stripper",
                StatCollector.translateToLocal("gtnr.machine.star_core_stripper.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.StarCoreStripper.get(1), AnimatedText.GT_NEVER_RESET);
        if (ChaosConfig.enableBottleInChaos) {
            // #tr gtnr.machine.bottle_in_chaos.name
            // # Bottle in Chaos
            // # zh_CN 混沌中瓶
            GTNRItemList.BottleInChaos.set(
                new BottleInChaos(
                    GTNRMachineID.BottleInChaos.id,
                    "gtneverreset.bottle_in_chaos",
                    StatCollector.translateToLocal("gtnr.machine.bottle_in_chaos.name")));
            AnimatedTooltipHandler.addItemTooltip(GTNRItemList.BottleInChaos.get(1), AnimatedText.GT_NEVER_RESET);
        }
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
        // #tr gtnr.machine.universal_joint_factory.name
        // # Universal Joint Factory
        // # zh_CN 通用联合工厂
        GTNRItemList.UniversalJointFactory.set(
            new UniversalJointFactory(
                GTNRMachineID.UniversalJointFactory.id,
                "gtneverreset.universal_joint_factory",
                StatCollector.translateToLocal("gtnr.machine.universal_joint_factory.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.UniversalJointFactory.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.machine.hadron_catalytic_refinery.name
        // # Hadron Catalytic Refinery
        // # zh_CN 强子流态催化精炼塔
        GTNRItemList.HadronCatalyticRefinery.set(
            new HadronCatalyticRefinery(
                GTNRMachineID.HadronCatalyticRefinery.id,
                "gtneverreset.hadron_catalytic_refinery",
                StatCollector.translateToLocal("gtnr.machine.hadron_catalytic_refinery.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.HadronCatalyticRefinery.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.machine.quantum_computer.name
        // # Quantum Computer
        // # zh_CN 量子计算机
        GTNRItemList.QuantumComputer.set(
            new QuantumComputer(
                GTNRMachineID.QuantumComputer.id,
                "gtneverreset.quantum_computer",
                StatCollector.translateToLocal("gtnr.machine.quantum_computer.name")));
        AnimatedTooltipHandler.addItemTooltip(GTNRItemList.QuantumComputer.get(1), AnimatedText.GT_NEVER_RESET);
        // #tr gtnr.machine.assembler_matrix.name
        // # Assembler Matrix
        // # zh_CN 装配矩阵
        GTNRItemList.AssemblerMatrix.set(
            new AssemblerMatrix(
                GTNRMachineID.AssemblerMatrix.id,
                "gtneverreset.assembler_matrix",
                StatCollector.translateToLocal("gtnr.machine.assembler_matrix.name")));
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
        if (ModList.CropsNH.isModLoaded()) {
            // #tr gtnr.machine.large_crop_breeder.name
            // # Large Crop Breeder
            // # zh_CN 大型作物杂交机
            GTNRItemList.LargeCropBreeder.set(
                new LargeCropBreeder(
                    GTNRMachineID.LargeCropBreeder.id,
                    "gtneverreset.large_crop_breeder",
                    StatCollector.translateToLocal("gtnr.machine.large_crop_breeder.name")));
            AnimatedTooltipHandler.addItemTooltip(GTNRItemList.LargeCropBreeder.get(1), AnimatedText.GT_NEVER_RESET);
        }
        if (ModList.Forestry.isModLoaded()) {
            ItemStack beeCasing = LargeBeeBreeder.getCasingMaterial()
                .get(1);
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

    private static void registerSatellites() {
        // #tr gtnr.machine.metal_forming_satellite_mk1.name
        // # Metal Forming Satellite Mk I
        // # zh_CN 金属成型卫星 Mk I
        registerSatellite(
            GTNRItemList.MetalFormingSatelliteMkI,
            GTNRMachineID.MetalFormingSatelliteMkI,
            "gtnr.machine.metal_forming_satellite_mk1.name",
            0,
            0);
        // #tr gtnr.machine.metal_forming_satellite_mk2.name
        // # Metal Forming Satellite Mk II
        // # zh_CN 金属成型卫星 Mk II
        registerSatellite(
            GTNRItemList.MetalFormingSatelliteMkII,
            GTNRMachineID.MetalFormingSatelliteMkII,
            "gtnr.machine.metal_forming_satellite_mk2.name",
            0,
            1);
        // #tr gtnr.machine.metal_forming_satellite_mk3.name
        // # Metal Forming Satellite Mk III
        // # zh_CN 金属成型卫星 Mk III
        registerSatellite(
            GTNRItemList.MetalFormingSatelliteMkIII,
            GTNRMachineID.MetalFormingSatelliteMkIII,
            "gtnr.machine.metal_forming_satellite_mk3.name",
            0,
            2);
        // #tr gtnr.machine.metal_forming_satellite_mk4.name
        // # Metal Forming Satellite Mk IV
        // # zh_CN 金属成型卫星 Mk IV
        registerSatellite(
            GTNRItemList.MetalFormingSatelliteMkIV,
            GTNRMachineID.MetalFormingSatelliteMkIV,
            "gtnr.machine.metal_forming_satellite_mk4.name",
            0,
            3);
        // #tr gtnr.machine.mineral_processing_satellite_mk1.name
        // # Mineral Processing Satellite Mk I
        // # zh_CN 矿物处理卫星 Mk I
        registerSatellite(
            GTNRItemList.MineralProcessingSatelliteMkI,
            GTNRMachineID.MineralProcessingSatelliteMkI,
            "gtnr.machine.mineral_processing_satellite_mk1.name",
            1,
            0);
        // #tr gtnr.machine.mineral_processing_satellite_mk2.name
        // # Mineral Processing Satellite Mk II
        // # zh_CN 矿物处理卫星 Mk II
        registerSatellite(
            GTNRItemList.MineralProcessingSatelliteMkII,
            GTNRMachineID.MineralProcessingSatelliteMkII,
            "gtnr.machine.mineral_processing_satellite_mk2.name",
            1,
            1);
        // #tr gtnr.machine.mineral_processing_satellite_mk3.name
        // # Mineral Processing Satellite Mk III
        // # zh_CN 矿物处理卫星 Mk III
        registerSatellite(
            GTNRItemList.MineralProcessingSatelliteMkIII,
            GTNRMachineID.MineralProcessingSatelliteMkIII,
            "gtnr.machine.mineral_processing_satellite_mk3.name",
            1,
            2);
        // #tr gtnr.machine.mineral_processing_satellite_mk4.name
        // # Mineral Processing Satellite Mk IV
        // # zh_CN 矿物处理卫星 Mk IV
        registerSatellite(
            GTNRItemList.MineralProcessingSatelliteMkIV,
            GTNRMachineID.MineralProcessingSatelliteMkIV,
            "gtnr.machine.mineral_processing_satellite_mk4.name",
            1,
            3);
        // #tr gtnr.machine.fluid_processing_satellite_mk1.name
        // # Fluid Processing Satellite Mk I
        // # zh_CN 流体处理卫星 Mk I
        registerSatellite(
            GTNRItemList.FluidProcessingSatelliteMkI,
            GTNRMachineID.FluidProcessingSatelliteMkI,
            "gtnr.machine.fluid_processing_satellite_mk1.name",
            2,
            0);
        // #tr gtnr.machine.fluid_processing_satellite_mk2.name
        // # Fluid Processing Satellite Mk II
        // # zh_CN 流体处理卫星 Mk II
        registerSatellite(
            GTNRItemList.FluidProcessingSatelliteMkII,
            GTNRMachineID.FluidProcessingSatelliteMkII,
            "gtnr.machine.fluid_processing_satellite_mk2.name",
            2,
            1);
        // #tr gtnr.machine.fluid_processing_satellite_mk3.name
        // # Fluid Processing Satellite Mk III
        // # zh_CN 流体处理卫星 Mk III
        registerSatellite(
            GTNRItemList.FluidProcessingSatelliteMkIII,
            GTNRMachineID.FluidProcessingSatelliteMkIII,
            "gtnr.machine.fluid_processing_satellite_mk3.name",
            2,
            2);
        // #tr gtnr.machine.fluid_processing_satellite_mk4.name
        // # Fluid Processing Satellite Mk IV
        // # zh_CN 流体处理卫星 Mk IV
        registerSatellite(
            GTNRItemList.FluidProcessingSatelliteMkIV,
            GTNRMachineID.FluidProcessingSatelliteMkIV,
            "gtnr.machine.fluid_processing_satellite_mk4.name",
            2,
            3);
        // #tr gtnr.machine.assembly_satellite_mk1.name
        // # Assembly Satellite Mk I
        // # zh_CN 组装卫星 Mk I
        registerSatellite(
            GTNRItemList.AssemblySatelliteMkI,
            GTNRMachineID.AssemblySatelliteMkI,
            "gtnr.machine.assembly_satellite_mk1.name",
            3,
            0);
        // #tr gtnr.machine.assembly_satellite_mk2.name
        // # Assembly Satellite Mk II
        // # zh_CN 组装卫星 Mk II
        registerSatellite(
            GTNRItemList.AssemblySatelliteMkII,
            GTNRMachineID.AssemblySatelliteMkII,
            "gtnr.machine.assembly_satellite_mk2.name",
            3,
            1);
        // #tr gtnr.machine.assembly_satellite_mk3.name
        // # Assembly Satellite Mk III
        // # zh_CN 组装卫星 Mk III
        registerSatellite(
            GTNRItemList.AssemblySatelliteMkIII,
            GTNRMachineID.AssemblySatelliteMkIII,
            "gtnr.machine.assembly_satellite_mk3.name",
            3,
            2);
        // #tr gtnr.machine.assembly_satellite_mk4.name
        // # Assembly Satellite Mk IV
        // # zh_CN 组装卫星 Mk IV
        registerSatellite(
            GTNRItemList.AssemblySatelliteMkIV,
            GTNRMachineID.AssemblySatelliteMkIV,
            "gtnr.machine.assembly_satellite_mk4.name",
            3,
            3);
    }

    private static void registerSatellite(GTNRItemList item, GTNRMachineID machine, String key, int category,
        int grade) {
        item.set(
            new NexusSatelliteFactory(
                machine.id,
                "gtneverreset." + machine.name(),
                StatCollector.translateToLocal(key),
                key,
                category,
                grade));
    }
}
