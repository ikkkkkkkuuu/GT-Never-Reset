package com.xyp.gtneverreset.loader.machine;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.basicMachine.EndCrystalResonator;
import com.xyp.gtneverreset.common.machine.basicMachine.FragmentWorldCollector;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.ModernMachineTextures;
import com.xyp.gtneverreset.utils.text.AnimatedText;

import gregtech.api.enums.SoundResource;
import gregtech.api.enums.VoltageIndex;

public final class BasicMachineLoader {

    public static void registry() {
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
