package com.xyp.gtneverreset.loader;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtneverreset.api.packaged.PackagedCoreRegistry;
import com.xyp.gtneverreset.common.item.items.HarvestRing;
import com.xyp.gtneverreset.common.item.items.ItemAdvancedIOBus;
import com.xyp.gtneverreset.common.item.items.ItemPackagedCore;
import com.xyp.gtneverreset.common.item.items.ItemStockIOInterface;
import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.item.items.ItemWirelessConnector;
import com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.common.item.items.PermanentMagnetRing;
import com.xyp.gtneverreset.common.item.items.StableEndCrystalItem;
import com.xyp.gtneverreset.common.item.items.VeinMiningPickaxe;
import com.xyp.gtneverreset.common.item.items.WildcardPatternItem;
import com.xyp.gtneverreset.common.item.items.WirelessFrequencyCard;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.common.machine.packaged.AssemblyLineAdapter;
import com.xyp.gtneverreset.common.machine.packaged.CircuitAssemblyLineAdapter;
import com.xyp.gtneverreset.compat.bloodmagic.BloodMagicCompat;
import com.xyp.gtneverreset.compat.thaumcraft.ThaumcraftCompat;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtnrlib.mod.ModList;

import appeng.api.AEApi;
import appeng.api.config.Upgrades;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ItemLoader {

    public static final PermanentMagnetRing permanentMagnetRing = new PermanentMagnetRing();
    public static final HarvestRing harvestRing = new HarvestRing();

    public static final ItemWirelessConnector wirelessConnector = new ItemWirelessConnector();
    public static final ItemPackagedCore basicPackagedCore = new ItemPackagedCore(false);

    // #tr item.assembly_line_packaged_core.name
    // # Assembly Line Packaged Core
    // # zh_CN 装配线封包核心
    public static final ItemPackagedCore assemblyLineCore = new ItemPackagedCore(
        "assembly_line",
        "assembly_line_packaged_core");
    // #tr item.advanced_assembly_line_packaged_core.name
    // # Advanced Assembly Line Packaged Core
    // # zh_CN 进阶装配线封包核心
    public static final ItemPackagedCore advancedAssemblyLineCore = new ItemPackagedCore(
        "advanced_assembly_line",
        "advanced_assembly_line_packaged_core");
    // #tr item.circuit_assembly_line_packaged_core.name
    // # Circuit Assembly Line Packaged Core
    // # zh_CN 电路装配线封包核心
    public static final ItemPackagedCore circuitAssemblyLineCore = new ItemPackagedCore(
        "circuit_assembly_line",
        "circuit_assembly_line_packaged_core");

    public static ItemPackagedCore bloodAltarCore;
    public static ItemPackagedCore thaumcraftCrucibleCore;
    public static ItemPackagedCore arcaneWorkbenchCore;
    public static ItemPackagedCore thaumcraftInfusionCore;

    public static final StableEndCrystalItem stableEndCrystal = new StableEndCrystalItem();
    public static final ItemWirelessDualInterfaceTerminal wirelessDualInterfaceTerminal = new ItemWirelessDualInterfaceTerminal();
    public static final WirelessFrequencyCard wirelessFrequencyCard = new WirelessFrequencyCard();
    public static final WirelessPowerCard wirelessPowerCard = new WirelessPowerCard();
    public static final WildcardPatternItem wildcardPattern = new WildcardPatternItem();
    public static final ItemAdvancedIOBus advancedIOBus = new ItemAdvancedIOBus();
    public static final ItemStockIOInterface stockIOInterface = new ItemStockIOInterface();

    public static final ItemUniversalWand universalWand = new ItemUniversalWand();

    public static void registerItems() {
        GameRegistry.registerItem(permanentMagnetRing, "PermanentMagnetRing");
        GameRegistry.registerItem(harvestRing, "HarvestRing");
        GameRegistry.registerItem(wirelessConnector, "packaged_wireless_connector");
        GTNRItemList.ItemWirelessConnector.set(wirelessConnector);
        GameRegistry.registerItem(basicPackagedCore, "basic_packaged_core");
        GTNRItemList.BasicPackagedCore.set(basicPackagedCore);
        GameRegistry.registerItem(assemblyLineCore, "assembly_line_packaged_core");
        GTNRItemList.AssemblyLineCore.set(assemblyLineCore);
        GameRegistry.registerItem(advancedAssemblyLineCore, "advanced_assembly_line_packaged_core");
        GTNRItemList.AdvancedAssemblyLineCore.set(advancedAssemblyLineCore);
        GameRegistry.registerItem(circuitAssemblyLineCore, "circuit_assembly_line_packaged_core");
        GTNRItemList.CircuitAssemblyLineCore.set(circuitAssemblyLineCore);
        GameRegistry.registerItem(stableEndCrystal, StableEndCrystalItem.REGISTRY_NAME);
        GTNRItemList.StableEndCrystal.set(stableEndCrystal);
        GameRegistry.registerItem(wirelessDualInterfaceTerminal, ItemWirelessDualInterfaceTerminal.REGISTRY_NAME);
        GTNRItemList.WirelessDualInterfaceTerminal.set(wirelessDualInterfaceTerminal);
        AEApi.instance()
            .registries()
            .wireless()
            .registerWirelessHandler(wirelessDualInterfaceTerminal);
        GameRegistry.registerItem(wirelessFrequencyCard, WirelessFrequencyCard.REGISTRY_NAME);
        GTNRItemList.WirelessFrequencyCard.set(wirelessFrequencyCard);
        GameRegistry.registerItem(wirelessPowerCard, "wireless_power_card");
        GTNRItemList.WirelessPowerCard.set(wirelessPowerCard);
        GameRegistry.registerItem(wildcardPattern, WildcardPatternItem.ITEM_NAME);
        GTNRItemList.WildcardPattern.set(wildcardPattern);
        GameRegistry.registerItem(advancedIOBus, ItemAdvancedIOBus.REGISTRY_NAME);
        GTNRItemList.AdvancedIOBus.set(advancedIOBus);
        Upgrades.SPEED.registerItem(new ItemStack(advancedIOBus), 4);
        Upgrades.SUPERSPEED.registerItem(new ItemStack(advancedIOBus), 4);
        Upgrades.SUPERLUMINALSPEED.registerItem(new ItemStack(advancedIOBus), 4);
        Upgrades.CAPACITY.registerItem(new ItemStack(advancedIOBus), 5);
        Upgrades.REDSTONE.registerItem(new ItemStack(advancedIOBus), 1);
        GameRegistry.registerItem(stockIOInterface, ItemStockIOInterface.REGISTRY_NAME);
        GTNRItemList.StockIOInterfacePart.set(stockIOInterface);
        GameRegistry.registerItem(universalWand, "universal_wand");
        var pickaxe = new VeinMiningPickaxe();
        MinecraftForge.EVENT_BUS.register(pickaxe);
        FMLCommonHandler.instance()
            .bus()
            .register(pickaxe);
        GameRegistry.registerItem(pickaxe, pickaxe.getUnlocalizedName());
        GTNRItemList.VeinMiningPickaxe.set(new ItemStack(pickaxe, 1));
        if (ModList.BloodMagic.isModLoaded()) BloodMagicCompat.registerPackagedCores();

        PackagedCoreRegistry.register("assembly_line", new AssemblyLineAdapter(false));
        PackagedCoreRegistry.register("advanced_assembly_line", new AssemblyLineAdapter(true));
        PackagedCoreRegistry.register("circuit_assembly_line", new CircuitAssemblyLineAdapter());

        if (ModList.Thaumcraft.isModLoaded()) ThaumcraftCompat.registerPackagedCores();
    }

    public static final class Card implements IContentLoader {

        @Override
        public void preInit(FMLPreInitializationEvent event) {
            ItemLoader.registerItems();
        }
    }
}
