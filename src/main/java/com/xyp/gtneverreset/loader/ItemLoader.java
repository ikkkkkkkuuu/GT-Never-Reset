package com.xyp.gtneverreset.loader;

import com.xyp.gtneverreset.api.packaged.PackagedCoreRegistry;
import com.xyp.gtneverreset.common.item.items.ItemAdvancedIOBus;
import com.xyp.gtneverreset.common.item.items.ItemPackagedCore;
import com.xyp.gtneverreset.common.item.items.ItemStockIOInterface;
import com.xyp.gtneverreset.common.item.items.ItemWirelessConnector;
import com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.common.item.items.StableEndCrystalItem;
import com.xyp.gtneverreset.common.item.items.WildcardPatternItem;
import com.xyp.gtneverreset.common.item.items.WirelessFrequencyCard;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.machine.packaged.ArcaneWorkbenchAdapter;
import com.xyp.gtneverreset.utils.machine.packaged.AssemblyLineAdapter;
import com.xyp.gtneverreset.utils.machine.packaged.BloodAltarAdapter;
import com.xyp.gtneverreset.utils.machine.packaged.CircuitAssemblyLineAdapter;
import com.xyp.gtneverreset.utils.machine.packaged.ThaumcraftCrucibleAdapter;
import com.xyp.gtneverreset.utils.machine.packaged.ThaumcraftInfusionAdapter;

public final class ItemLoader {

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
    public static final WildcardPatternItem wildcardPattern = new WildcardPatternItem();
    public static final ItemAdvancedIOBus advancedIOBus = new ItemAdvancedIOBus();
    public static final ItemStockIOInterface stockIOInterface = new ItemStockIOInterface();

    public static void registerItems() {
        if (ModList.BloodMagic.isModLoaded()) {
            // #tr item.blood_altar_packaged_core.name
            // # Blood Altar Packaged Core
            // # zh_CN 血魔法祭坛封包核心
            bloodAltarCore = new ItemPackagedCore(
                "blood_altar",
                "blood_altar_packaged_core",
                GTNRItemList.BloodAltarCore);
            PackagedCoreRegistry.register("blood_altar", new BloodAltarAdapter());
        }

        PackagedCoreRegistry.register("assembly_line", new AssemblyLineAdapter(false));
        PackagedCoreRegistry.register("advanced_assembly_line", new AssemblyLineAdapter(true));
        PackagedCoreRegistry.register("circuit_assembly_line", new CircuitAssemblyLineAdapter());

        if (ModList.Thaumcraft.isModLoaded()) {
            // #tr item.tc4_crucible_packaged_core.name
            // # Thaumcraft Crucible Packaged Core
            // # zh_CN 神秘时代坩埚封包核心
            thaumcraftCrucibleCore = new ItemPackagedCore(
                "thaumcraft_crucible",
                "tc4_crucible_packaged_core",
                GTNRItemList.ThaumcraftCrucibleCore);
            // #tr item.arcane_workbench_packaged_core.name
            // # Arcane Workbench Packaged Core
            // # zh_CN 奥术工作台封包核心
            arcaneWorkbenchCore = new ItemPackagedCore(
                "arcane_workbench",
                "arcane_workbench_packaged_core",
                GTNRItemList.ArcaneWorkbenchCore);
            thaumcraftInfusionCore = new ItemPackagedCore(true);

            PackagedCoreRegistry.register("thaumcraft_crucible", new ThaumcraftCrucibleAdapter());
            PackagedCoreRegistry.register("arcane_workbench", new ArcaneWorkbenchAdapter());
            PackagedCoreRegistry.register("thaumcraft_infusion", new ThaumcraftInfusionAdapter());
        }
    }
}
