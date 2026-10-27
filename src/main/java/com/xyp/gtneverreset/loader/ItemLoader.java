package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.common.item.ItemAdvancedIOBus;
import com.xyp.gtneverreset.common.item.ItemStockIOInterface;
import com.xyp.gtneverreset.common.item.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.common.item.WildcardPatternItem;
import com.xyp.gtneverreset.common.item.WirelessFrequencyCard;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import appeng.api.AEApi;
import appeng.api.config.Upgrades;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ItemLoader {

    private ItemLoader() {}

    public static void registerItems() {
        ItemWirelessDualInterfaceTerminal terminal = new ItemWirelessDualInterfaceTerminal();
        GameRegistry.registerItem(terminal, ItemWirelessDualInterfaceTerminal.REGISTRY_NAME);
        GTNRItemList.WirelessDualInterfaceTerminal.set(terminal);
        AEApi.instance()
            .registries()
            .wireless()
            .registerWirelessHandler(terminal);
        terminal.setCreativeTab(GTNRCreativeTabs.ITEMS);
        WirelessFrequencyCard frequencyCard = new WirelessFrequencyCard();
        GameRegistry.registerItem(frequencyCard, WirelessFrequencyCard.REGISTRY_NAME);
        GTNRItemList.WirelessFrequencyCard.set(frequencyCard);
        frequencyCard.setCreativeTab(GTNRCreativeTabs.ITEMS);
        WildcardPatternItem wildcardPattern = new WildcardPatternItem();
        GameRegistry.registerItem(wildcardPattern, WildcardPatternItem.ITEM_NAME);
        GTNRItemList.WildcardPattern.set(wildcardPattern);
        wildcardPattern.setCreativeTab(GTNRCreativeTabs.ITEMS);
        ItemAdvancedIOBus advancedIOBus = new ItemAdvancedIOBus();
        GameRegistry.registerItem(advancedIOBus, ItemAdvancedIOBus.REGISTRY_NAME);
        GTNRItemList.AdvancedIOBus.set(advancedIOBus);
        advancedIOBus.setCreativeTab(GTNRCreativeTabs.ITEMS);
        Upgrades.SPEED.registerItem(GTNRItemList.AdvancedIOBus.get(1), 4);
        Upgrades.SUPERSPEED.registerItem(GTNRItemList.AdvancedIOBus.get(1), 4);
        Upgrades.SUPERLUMINALSPEED.registerItem(GTNRItemList.AdvancedIOBus.get(1), 4);
        Upgrades.CAPACITY.registerItem(GTNRItemList.AdvancedIOBus.get(1), 5);
        Upgrades.REDSTONE.registerItem(GTNRItemList.AdvancedIOBus.get(1), 1);
        ItemStockIOInterface stockInterface = new ItemStockIOInterface();
        GameRegistry.registerItem(stockInterface, ItemStockIOInterface.REGISTRY_NAME);
        GTNRItemList.StockIOInterfacePart.set(stockInterface);
        stockInterface.setCreativeTab(GTNRCreativeTabs.ITEMS);
    }
}
