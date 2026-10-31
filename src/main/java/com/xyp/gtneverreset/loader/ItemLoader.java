package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.api.packaged.PackagedCoreRegistry;
import com.xyp.gtneverreset.common.item.items.HarvestRing;
import com.xyp.gtneverreset.common.item.items.ItemAdvancedIOBus;
import com.xyp.gtneverreset.common.item.items.ItemPackagedCore;
import com.xyp.gtneverreset.common.item.items.ItemStockIOInterface;
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
        "assembly_line_packaged_core",
        GTNRItemList.AssemblyLineCore);
    // #tr item.advanced_assembly_line_packaged_core.name
    // # Advanced Assembly Line Packaged Core
    // # zh_CN 进阶装配线封包核心
    public static final ItemPackagedCore advancedAssemblyLineCore = new ItemPackagedCore(
        "advanced_assembly_line",
        "advanced_assembly_line_packaged_core",
        GTNRItemList.AdvancedAssemblyLineCore);
    // #tr item.circuit_assembly_line_packaged_core.name
    // # Circuit Assembly Line Packaged Core
    // # zh_CN 电路装配线封包核心
    public static final ItemPackagedCore circuitAssemblyLineCore = new ItemPackagedCore(
        "circuit_assembly_line",
        "circuit_assembly_line_packaged_core",
        GTNRItemList.CircuitAssemblyLineCore);

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

    public static void registerItems() {
        new VeinMiningPickaxe();
        if (ModList.BloodMagic.isModLoaded()) BloodMagicCompat.registerPackagedCores();

        PackagedCoreRegistry.register("assembly_line", new AssemblyLineAdapter(false));
        PackagedCoreRegistry.register("advanced_assembly_line", new AssemblyLineAdapter(true));
        PackagedCoreRegistry.register("circuit_assembly_line", new CircuitAssemblyLineAdapter());

        if (ModList.Thaumcraft.isModLoaded()) ThaumcraftCompat.registerPackagedCores();
    }

    public static final class Card implements IContentLoader {

        @Override
        public String name() {
            return "item";
        }

        @Override
        public void preInit(cpw.mods.fml.common.event.FMLPreInitializationEvent event) {
            ItemLoader.registerItems();
        }
    }
}
