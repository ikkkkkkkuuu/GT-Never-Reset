package com.xyp.gtneverreset.common.block.blocks.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtneverreset.common.block.blocks.BlockFluxConnector;
import com.xyp.gtneverreset.utils.machine.flux.FluxDropData;

/** Explains the port's GT EU direction, automatic owner binding and configurable packet limits. */
public final class ItemBlockFluxConnector extends ItemBlock {

    public ItemBlockFluxConnector(Block block) {
        super(block);
    }

    /** Reset legacy portable settings on the server so old and newly crafted stacks can merge. */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        super.onUpdate(stack, world, entity, slot, held);
        if (!world.isRemote && world.getTotalWorldTime() % 20 == 0) {
            FluxDropData.normalize(stack);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (((BlockFluxConnector) field_150939_a).plug) {
            // #tr flux.tooltip.plug
            // # Transfers incoming GT EU to the GTNH wireless energy network.
            // # zh_CN 将输入的 GT EU 存入 GTNH 无线电网。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.plug"));
        } else {
            // #tr flux.tooltip.point
            // # Supplies GT EU from the GTNH wireless energy network.
            // # zh_CN 从 GTNH 无线电网取电并输出 GT EU。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.point"));
        }
        // #tr flux.tooltip.owner
        // # Uses the placing player's GT wireless team balance.
        // # zh_CN 使用放置者所属 GT 无线团队的电量。
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.owner"));
        // #tr flux.tooltip.settings
        // # Detects voltage on placement, or uses 32 V if unknown. Starts at 1 A; right-click to edit.
        // # zh_CN 放置时识别电压，无法识别时采用 32 V；默认 1 A，右键调整。
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.settings"));
        // #tr flux.tooltip.rating
        // # Transfer limit: voltage x amperage EU/t.
        // # zh_CN 传输上限：电压 × 安培数 EU/t。
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.rating"));
        if (!((BlockFluxConnector) field_150939_a).plug) {
            // #tr flux.tooltip.overvoltage
            // # Output obeys GT overvoltage and cable amperage limits.
            // # zh_CN 输出仍受 GT 过压与线缆安培数限制。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.overvoltage"));
        }
    }
}
