package com.xyp.gtneverreset.loader.machine;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix;
import com.xyp.gtneverreset.common.machine.multiblock.LargeBeeBreeder;
import com.xyp.gtneverreset.common.machine.multiblock.LargeFurnace;
import com.xyp.gtneverreset.common.machine.multiblock.LargeOreProcessor;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;
import com.xyp.gtneverreset.common.machine.multiblock.SingularityDataHub;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;
import com.xyp.gtneverreset.utils.text.AnimatedText;
import com.xyp.gtnrlib.mod.ModList;

import gregtech.api.enums.Textures;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;

public final class MultiblockLoader {

    public static void registry() {
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

}
