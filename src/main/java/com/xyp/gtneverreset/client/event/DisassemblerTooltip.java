package com.xyp.gtneverreset.client.event;

import java.util.List;
import java.util.Locale;

import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.xyp.gtneverreset.common.machine.basicMachine.Disassembler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.GTValues;
import gregtech.api.util.tooltip.TooltipHelper;
import gregtech.common.blocks.ItemMachines;

@SideOnly(Side.CLIENT)
public final class DisassemblerTooltip {

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!(ItemMachines.getMetaTileEntity(event.itemStack) instanceof Disassembler machine)) return;
        long voltage = machine.maxEUInput();
        long capacity = machine.maxEUStore();
        List<String> lines = event.toolTip;
        int inputLine = lines.indexOf(
            StatCollector.translateToLocalFormatted("gt.tileentity.eup_in", TooltipHelper.voltageText(voltage)));
        if (inputLine < 0) return;

        // #tr gtnr.disassembler.voltage_input
        // # Voltage IN: {\\GREEN}%s (%s{\\RESET}{\\GREEN}){\\GRAY}
        // # zh_CN 输入电压：{\\GREEN}%s (%s{\\RESET}{\\GREEN}){\\GRAY}
        lines.set(
            inputLine,
            StatCollector.translateToLocalFormatted(
                "gtnr.disassembler.voltage_input",
                String.format(Locale.ROOT, "%,d", voltage),
                machine.getLegacyTierColor() + GTValues.VN[machine.mTier]));
        // #tr gtnr.disassembler.capacity
        // # Capacity: {\\BLUE}%s{\\GRAY} EU
        // # zh_CN 电能缓存：{\\BLUE}%s{\\GRAY} EU
        String legacyCapacity = StatCollector
            .translateToLocalFormatted("gtnr.disassembler.capacity", String.format(Locale.ROOT, "%,d", capacity));
        int capacityLine = lines.indexOf(
            StatCollector.translateToLocalFormatted("gt.tileentity.eup_store", TooltipHelper.euCapacityText(capacity)));
        if (capacityLine >= 0) lines.set(capacityLine, legacyCapacity);
        else lines.add(inputLine + 1, legacyCapacity);
    }
}
