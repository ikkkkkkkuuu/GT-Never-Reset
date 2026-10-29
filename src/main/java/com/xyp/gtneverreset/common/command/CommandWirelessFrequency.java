package com.xyp.gtneverreset.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;

import com.xyp.gtneverreset.common.item.items.WirelessFrequencyCard;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardBinding;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessFrequencies;

public class CommandWirelessFrequency extends CommandBase {

    @Override
    public String getCommandName() {
        return "gtnr_wireless_me";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtnr_wireless_me owner <player>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 2 || !"owner".equals(args[0])) throw new WrongUsageException(getCommandUsage(sender));
        var player = getCommandSenderAsPlayer(sender);
        var card = player.getCurrentEquippedItem();
        // #tr gtneverreset.wireless.command.card_required
        // # Hold a bound wireless frequency card. Load its original controller if it has not been migrated yet.
        // # zh_CN 请手持已绑定的无线频率卡；尚未迁移的旧卡需要先加载原控制器。
        String required = "gtneverreset.wireless.command.card_required";
        if (card == null || !(card.getItem() instanceof WirelessFrequencyCard)) throw new CommandException(required);
        WirelessCardBinding.upgrade(card, player);
        var binding = WirelessCardBinding.read(card.getTagCompound());
        var owner = getPlayer(sender, args[1]);
        if (binding == null || !WirelessFrequencies.setCreator(binding.frequency(), owner.getUniqueID()))
            throw new CommandException(required);
        WirelessCardBinding.upgrade(card, player);
        // #tr gtneverreset.wireless.command.owner_set
        // # Frequency creator set to %s. Card holders still need AE build permission to take it over.
        // # zh_CN 已将频率创建者设为 %s；接管时仍须持有自己的卡并具有 AE 建造权限。
        sender.addChatMessage(
            new ChatComponentTranslation("gtneverreset.wireless.command.owner_set", owner.getCommandSenderName()));
    }
}
