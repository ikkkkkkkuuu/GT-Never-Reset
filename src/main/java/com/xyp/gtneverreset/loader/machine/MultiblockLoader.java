package com.xyp.gtneverreset.loader.machine;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;
import com.xyp.gtneverreset.common.machine.multiblock.CatalyticReformerController;
import com.xyp.gtneverreset.common.machine.multiblock.HadronCatalyticRefinery;
import com.xyp.gtneverreset.common.machine.multiblock.LargeBeeBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.LargeCropBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.common.machine.multiblock.LightHunterSpaceStation;
import com.xyp.gtneverreset.common.machine.multiblock.MegaElectricBlastFurnace;
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
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;

public final class MultiblockLoader {

    public static void registry() {
        register(
            GTNRItemList.LightHunterSpaceStation,
            new LightHunterSpaceStation(
                GTNRMachineID.LightHunterSpaceStation.id,
                "gtneverreset.light_hunter_station",
                StatCollector.translateToLocal("gtnr.machine.light_hunter.name")));
        registerSatellites();
        // #tr gtnr.machine.star_core_stripper.name
        // # Star Core Stripper
        // # zh_CN 星核剥离装置
        register(
            GTNRItemList.StarCoreStripper,
            new StarCoreStripper(
                GTNRMachineID.StarCoreStripper.id,
                "gtneverreset.star_core_stripper",
                StatCollector.translateToLocal("gtnr.machine.star_core_stripper.name")));
        if (ChaosConfig.enableBottleInChaos) {
            // #tr gtnr.machine.bottle_in_chaos.name
            // # Bottle in Chaos
            // # zh_CN 混沌中瓶
            register(
                GTNRItemList.BottleInChaos,
                new BottleInChaos(
                    GTNRMachineID.BottleInChaos.id,
                    "gtneverreset.bottle_in_chaos",
                    StatCollector.translateToLocal("gtnr.machine.bottle_in_chaos.name")));
        }
        // #tr gtnr.machine.large_ore_processor.name
        // # Large Ore Processor
        // # zh_CN 大型矿石处理机
        register(
            GTNRItemList.LargeOreProcessor,
            new LargeOreProcessor(
                GTNRMachineID.LargeOreProcessor.id,
                "gtneverreset.large_ore_processor",
                StatCollector.translateToLocal("gtnr.machine.large_ore_processor.name")));
        // #tr gtnr.machine.large_furnace.name
        // # Large Furnace
        // # zh_CN 大型熔炉
        register(
            GTNRItemList.LargeFurnace,
            new LargeFurnace(
                GTNRMachineID.LargeFurnace.id,
                "gtneverreset.large_furnace",
                StatCollector.translateToLocal("gtnr.machine.large_furnace.name")));
        // #tr gtnr.machine.universal_joint_factory.name
        // # Universal Joint Factory
        // # zh_CN 通用联合工厂
        register(
            GTNRItemList.UniversalJointFactory,
            new UniversalJointFactory(
                GTNRMachineID.UniversalJointFactory.id,
                "gtneverreset.universal_joint_factory",
                StatCollector.translateToLocal("gtnr.machine.universal_joint_factory.name")));
        // #tr gtnr.machine.hadron_catalytic_refinery.name
        // # Hadron Catalytic Refinery
        // # zh_CN 强子流态催化精炼塔
        register(
            GTNRItemList.HadronCatalyticRefinery,
            new HadronCatalyticRefinery(
                GTNRMachineID.HadronCatalyticRefinery.id,
                "gtneverreset.hadron_catalytic_refinery",
                StatCollector.translateToLocal("gtnr.machine.hadron_catalytic_refinery.name")));
        // #tr gtnr.machine.catalytic_reformer.name
        // # Catalytic Reformer
        // # zh_CN 催化重整机
        register(
            GTNRItemList.CatalyticReformer,
            new CatalyticReformerController(
                GTNRMachineID.CatalyticReformer.id,
                "gtneverreset.catalytic_reformer",
                StatCollector.translateToLocal("gtnr.machine.catalytic_reformer.name")));
        // #tr gtnr.machine.quantum_computer.name
        // # Quantum Computer
        // # zh_CN 量子计算机
        register(
            GTNRItemList.QuantumComputer,
            new QuantumComputer(
                GTNRMachineID.QuantumComputer.id,
                "gtneverreset.quantum_computer",
                StatCollector.translateToLocal("gtnr.machine.quantum_computer.name")));
        // #tr gtnr.machine.assembler_matrix.name
        // # Assembler Matrix
        // # zh_CN 装配矩阵
        register(
            GTNRItemList.AssemblerMatrix,
            new AssemblerMatrix(
                GTNRMachineID.AssemblerMatrix.id,
                "gtneverreset.assembler_matrix",
                StatCollector.translateToLocal("gtnr.machine.assembler_matrix.name")));

        // #tr gtnr.machine.singularity_data_hub.name
        // # Vault
        // # zh_CN 保险库
        register(
            GTNRItemList.SingularityDataHub,
            new SingularityDataHub(
                GTNRMachineID.SingularityDataHub.id,
                "gtneverreset.singularity_data_hub",
                StatCollector.translateToLocal("gtnr.machine.singularity_data_hub.name")));
        if (ModList.CropsNH.isModLoaded()) {
            // #tr gtnr.machine.large_crop_breeder.name
            // # Large Crop Breeder
            // # zh_CN 大型作物杂交机
            register(
                GTNRItemList.LargeCropBreeder,
                new LargeCropBreeder(
                    GTNRMachineID.LargeCropBreeder.id,
                    "gtneverreset.large_crop_breeder",
                    StatCollector.translateToLocal("gtnr.machine.large_crop_breeder.name")));
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
            register(
                GTNRItemList.LargeBeeBreeder,
                new LargeBeeBreeder(
                    GTNRMachineID.LargeBeeBreeder.id,
                    "gtneverreset.large_bee_breeder",
                    StatCollector.translateToLocal("gtnr.machine.large_bee_breeder.name")));
        }
        // #tr gtnr.machine.mega_electric_blast_furnace.name
        // # Entrodynamically Phase Changer
        // # zh_CN 熵动力学相变发生器
        register(
            GTNRItemList.MegaElectricBlastFurnace,
            new MegaElectricBlastFurnace(
                GTNRMachineID.MegaElectricBlastFurnace.id,
                "gtneverreset.mega_electric_blast_furnace",
                StatCollector.translateToLocal("gtnr.machine.mega_electric_blast_furnace.name")));
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
        register(
            item,
            new NexusSatelliteFactory(
                machine.id,
                "gtneverreset." + machine.name(),
                StatCollector.translateToLocal(key),
                key,
                category,
                grade));
    }

    private static void register(GTNRItemList item, MTEEnhancedMultiBlockBase<?> machine) {
        item.set(machine);
        AnimatedTooltipHandler.addItemTooltip(item.get(1), AnimatedText.GT_NEVER_RESET);
    }
}
