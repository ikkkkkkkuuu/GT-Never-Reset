package com.xyp.gtneverreset.common.recipe.ae2;

import static appeng.items.tools.powered.ToolWirelessTerminal.infinityBoosterCard;
import static appeng.items.tools.powered.ToolWirelessTerminal.infinityEnergyCard;
import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_INTERFACE_TERM;
import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_PATTERN_TERM;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.utils.enums.ModsItemlist;

import cpw.mods.fml.common.registry.GameRegistry;

public final class WirelessTerminalRecipes {

    private WirelessTerminalRecipes() {}

    public static void register(ItemStack terminal) {
        GameRegistry.addShapelessRecipe(terminal.copy(), WIRELESS_INTERFACE_TERM, WIRELESS_PATTERN_TERM.stack());
        registerUpgrade(terminal, ModsItemlist.EnergyCard.get(1), infinityEnergyCard);
        registerUpgrade(terminal, ModsItemlist.InfinityBoosterCard.get(1), infinityBoosterCard);
        registerUpgrade(terminal, ModsItemlist.QuantumBridgeCard.get(1), infinityBoosterCard);
    }

    private static void registerUpgrade(ItemStack terminal, ItemStack card, String upgradeKey) {
        if (card != null && card.getItem() != null) {
            GameRegistry.addRecipe(new WirelessTerminalUpgradeRecipe(terminal, card, upgradeKey));
        }
    }
}
