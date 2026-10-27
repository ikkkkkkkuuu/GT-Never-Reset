package com.xyp.gtneverreset.common.block.blocks.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockStockIOInterface extends ItemBlock {

    public ItemBlockStockIOInterface(Block block) {
        super(block);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.stock_io_interface.direct
        // # Lets GT machines read recipe inputs and fuel directly from ME. Debits only actual use.
        // # zh_CN 让 GT 机器直接读取 ME 中的配方原料与燃料，只扣除实际消耗。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.direct"));
        // #tr tooltip.stock_io_interface.configure
        // # Attach to the machine, then right-click to mark resources and set their quantities.
        // # zh_CN 连接目标机器，右键标记资源并设置数量。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.configure"));
        // #tr tooltip.stock_io_interface.slots
        // # 900 item marks and 900 fluid marks. Supports reserves, fixed amounts and output recovery.
        // # zh_CN 物品与流体各 900 个标记，支持保留量、固定可用量与产物回收。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.slots"));
        // #tr tooltip.stock_io_interface.network
        // # Requires an active ME connection and a supported GT machine.
        // # zh_CN 需要有效的 ME 连接与受支持的 GT 机器。
        lines.add(StatCollector.translateToLocal("tooltip.stock_io_interface.network"));
    }
}
