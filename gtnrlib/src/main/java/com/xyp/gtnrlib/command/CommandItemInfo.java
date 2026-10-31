package com.xyp.gtnrlib.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

public class CommandItemInfo extends CommandBase {

    @Override
    public String getCommandName() {
        return "gtnr_iteminfo";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        // #tr gtnrlib.command.item_info.usage
        // # /gtnr_iteminfo - Display all information about the item in your hand
        // # zh_CN /gtnr_iteminfo - 显示手持物品的全部信息
        return "gtnrlib.command.item_info.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer player)) {
            // #tr gtnrlib.command.item_info.player_only
            // # This command can only be used by players.
            // # zh_CN 此指令只能由玩家使用。
            sender.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.player_only"));
            return;
        }

        ItemStack stack = player.getHeldItem();

        if (stack == null) {
            // #tr gtnrlib.command.item_info.no_item
            // # You are not holding any item.
            // # zh_CN 你没有手持任何物品。
            player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.no_item"));
            return;
        }

        Item item = stack.getItem();

        if (item == null) {
            player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.no_item"));
            return;
        }

        // #tr gtnrlib.command.item_info.header
        // # ===== Item Info =====
        // # zh_CN ===== 物品信息 =====
        player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.header"));
        player.addChatMessage(new ChatComponentText(stack.toString()));
        player.addChatMessage(
            // #tr gtnrlib.command.item_info.display_name
            // # Display Name: %s
            // # zh_CN 显示名称：%s
            new ChatComponentTranslation("gtnrlib.command.item_info.display_name", stack.getDisplayName()));
        player.addChatMessage(
            new ChatComponentTranslation(
                // #tr gtnrlib.command.item_info.item_stack_display_name
                // # ItemStack Display Name: %s
                // # zh_CN 物品堆显示名称：%s
                "gtnrlib.command.item_info.item_stack_display_name",
                item.getItemStackDisplayName(stack)));
        player.addChatMessage(
            new ChatComponentTranslation(
                // #tr gtnrlib.command.item_info.unlocalized_inefficient_name
                // # Unlocalized Inefficient Name: %s
                // # zh_CN 低效未本地化名称：%s
                "gtnrlib.command.item_info.unlocalized_inefficient_name",
                item.getUnlocalizedNameInefficiently(stack)));
        player.addChatMessage(
            // #tr gtnrlib.command.item_info.unlocalized_name
            // # Unlocalized Name: %s
            // # zh_CN 未本地化名称：%s
            new ChatComponentTranslation("gtnrlib.command.item_info.unlocalized_name", stack.getUnlocalizedName()));
        player.addChatMessage(
            new ChatComponentTranslation(
                // #tr gtnrlib.command.item_info.item_stack_unlocalized_name
                // # ItemStack Unlocalized Name: %s
                // # zh_CN 物品堆未本地化名称：%s
                "gtnrlib.command.item_info.item_stack_unlocalized_name",
                item.getUnlocalizedName(stack)));
        player.addChatMessage(
            new ChatComponentTranslation(
                // #tr gtnrlib.command.item_info.class
                // # Class: %s
                // # zh_CN 类：%s
                "gtnrlib.command.item_info.class",
                item.getClass()
                    .getName()));
        // #tr gtnrlib.command.item_info.id
        // # ID: %s
        // # zh_CN ID：%s
        player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.id", Item.getIdFromItem(item)));
        // #tr gtnrlib.command.item_info.stack_size
        // # Stack Size: %s
        // # zh_CN 堆叠数量：%s
        player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.stack_size", stack.stackSize));
        // #tr gtnrlib.command.item_info.damage
        // # Damage: %s
        // # zh_CN 损伤值：%s
        player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.damage", stack.getItemDamage()));
        // #tr gtnrlib.command.item_info.max_damage
        // # Max Damage: %s
        // # zh_CN 最大损伤值：%s
        player
            .addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.max_damage", stack.getMaxDamage()));

        NBTTagCompound nbt = stack.stackTagCompound;
        if (nbt != null) {
            // #tr gtnrlib.command.item_info.nbt_data
            // # NBT Data:
            // # zh_CN NBT 数据：
            player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.nbt_data"));
            for (String keyObj : nbt.func_150296_c()) {
                player.addChatMessage(
                    // #tr gtnrlib.command.item_info.nbt_entry
                    // # %s : %s
                    // # zh_CN %s：%s
                    new ChatComponentTranslation("gtnrlib.command.item_info.nbt_entry", keyObj, nbt.getTag(keyObj)));
            }
        } else {
            // #tr gtnrlib.command.item_info.no_nbt_data
            // # No NBT data.
            // # zh_CN 没有 NBT 数据。
            player.addChatMessage(new ChatComponentTranslation("gtnrlib.command.item_info.no_nbt_data"));
        }
    }

}
