package com.xyp.gtneverreset.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.xyp.gtneverreset.utils.enums.ModList;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public final class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtneverreset.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        if (loadedMods.contains(ModList.ModIds.CROPSNH)) {
            mixins.add("cropsnh.MixinSeedStats");
            mixins.add("cropsnh.MixinTileEntityCropSticks");
        }
        if (loadedMods.contains(ModList.ModIds.BLOOD_MAGIC)) {
            mixins.add("bloodmagic.MixinPackagedBloodAltar");
        }
        if (loadedMods.contains(ModList.ModIds.THAUMCRAFT)
            && loadedMods.contains(ModList.ModIds.APPLIED_ENERGISTICS_2)) {
            mixins.add("thaumcraft.MixinPackagedEssentiaHandler");
            mixins.add("thaumcraft.MixinPackagedInfusionSource");
            mixins.add("thaumcraft.MixinPackagedCrucible");
        }
        if (loadedMods.contains(ModList.ModIds.GREGTECH)) {
            if (FMLLaunchHandler.side()
                .isClient()) {
                mixins.add("gregtech.ModifySomeConfigs");
            }
            mixins.add("gregtech.AssemblyLineDataAccessMixin");
            mixins.addAll(
                Arrays.asList(
                    "gregtech.RecipeSpeedMixin",
                    "gregtech.SteamFurnaceSpeedMixin",
                    "gregtech.ScannerSpeedMixin",
                    "gregtech.AssemblyLineSpeedMixin",
                    "gregtech.PurificationPlantSpeedMixin",
                    "gregtech.ExtremeEntityCrusherSpeedMixin",
                    "gregtech.ThermalBoilerSpeedMixin",
                    "gregtech.FurnaceBackendMixin",
                    "gregtech.BasicMachineOutputMixin"));
            mixins.add("gregtech.OverclockCalculatorAccessorMixin");
            mixins.add("gregtech.AutoStructureTooltipMixin");
            mixins.add("gregtech.PatternInputHatchElementMixin");
            mixins.add("gregtech.PatternMEOutputMultiblockMixin");
            mixins.add("gregtech.PatternAssemblyControllerNameMixin");
            mixins.add("gregtech.wireless.MixinWirelessControllerVersion");
            mixins.add("gregtech.wireless.MixinWirelessHatchOwner");
        }
        if (loadedMods.contains(ModList.ModIds.STRUCTURE_LIB)) {
            mixins.add("structurelib.TooltipLazyStructureElementMixin");
        }
        if (loadedMods.contains(ModList.ModIds.APPLIED_ENERGISTICS_2)) {
            if (loadedMods.contains(ModList.ModIds.MATTER_MANIPULATOR)) {
                mixins.addAll(
                    Arrays.asList(
                        "matterManipulator.MixinPendingBlockWireless",
                        "matterManipulator.MixinPendingBuildWireless",
                        "matterManipulator.MixinPendingMoveWireless"));
            }
            mixins.addAll(
                Arrays.asList(
                    "appliedenergistics2.wireless.AccessorTileController",
                    "appliedenergistics2.wireless.MixinWirelessGridNode",
                    "appliedenergistics2.wireless.MixinWirelessInterfaceName",
                    "appliedenergistics2.wireless.MixinWirelessPartPlacement",
                    "appliedenergistics2.wireless.MixinWirelessPathGridCache",
                    "appliedenergistics2.wireless.MixinWirelessPathingCalculation",
                    "appliedenergistics2.wireless.MixinWirelessVisualiserTool",
                    "appliedenergistics2.DualityInterfaceMixin",
                    "appliedenergistics2.ItemEncodedPatternMixin",
                    "appliedenergistics2.PatternMultiplierHelperMixin",
                    "appliedenergistics2.MatrixWildcardPatternOptimizationMixin"));
            mixins.addAll(
                Arrays.asList(
                    "appliedenergistics2.quantum.AccessorOptimizePattern",
                    "appliedenergistics2.quantum.AccessorPacketCraftingTreeData",
                    "appliedenergistics2.quantum.MixinContainerCraftConfirm",
                    "appliedenergistics2.quantum.MixinContainerOptimizePatterns",
                    "appliedenergistics2.quantum.MixinCraftingCPUClusterQuantumJob",
                    "appliedenergistics2.quantum.MixinCraftingCPUStatus"));
            if (loadedMods.contains(ModList.ModIds.GREGTECH)) {
                mixins.addAll(
                    Arrays.asList(
                        "appliedenergistics2.compact.MixinCraftingGridCache",
                        "appliedenergistics2.compact.MixinCraftingCPUCluster",
                        "appliedenergistics2.compact.MixinInventoryCrafting",
                        "appliedenergistics2.compact.AccessorTaskProgress",
                        "appliedenergistics2.compact.AccessorSessionCraftCount",
                        "appliedenergistics2.compact.CompactCraftingEnergyMixin",
                        "appliedenergistics2.compact.MatrixInterfaceTerminalVisibilityMixin",
                        "appliedenergistics2.compact.MatrixPatternPersistenceMixin"));
                mixins.add("appliedenergistics2.LargeInterfaceTerminalRowsMixin");
                mixins.add("gregtech.MEOutputAssemblyMultiblockMixin");
                mixins.add("gregtech.MEOutputAssemblyHatchElementMixin");
                mixins.add("gregtech.BasicMachineStockIOMixin");
                mixins.add("gregtech.BasicGeneratorStockIOMixin");
                mixins.add("appliedenergistics2.MTEHatchCraftingInputMEMixin");
            }
        }
        if (!FMLLaunchHandler.side()
            .isClient() || !loadedMods.contains(ModList.ModIds.APPLIED_ENERGISTICS_2)) {
            return mixins;
        }
        mixins.addAll(
            Arrays.asList(
                "appliedenergistics2.wireless.MixinWirelessVisualiserRender",
                "appliedenergistics2.AccessorGuiScrollbar",
                "appliedenergistics2.AccessorContainerMEMonitorable",
                "appliedenergistics2.MixinGuiCraftingTerm",
                "appliedenergistics2.InterfaceEntryViewportAccessor",
                "appliedenergistics2.InterfaceTerminalViewportMixin",
                "appliedenergistics2.InterfaceEntryRowFilterMixin",
                "appliedenergistics2.InterfaceSectionRowFilterMixin",
                "appliedenergistics2.InterfaceTerminalRowFilterMixin",
                "appliedenergistics2.QuickTerminalButtonTextureMixin",
                "appliedenergistics2.QuickTerminalTabButtonTextureMixin",
                "appliedenergistics2.QuickTerminalTypeButtonTextureMixin"));
        mixins.addAll(
            Arrays.asList(
                "appliedenergistics2.quantum.MixinGuiCraftAmount",
                "appliedenergistics2.quantum.MixinGuiCraftConfirm",
                "appliedenergistics2.quantum.MixinGuiCraftingTree"));
        mixins.add("appliedenergistics2.LargeInterfaceTerminalEntryMixin");
        if (loadedMods.contains(ModList.ModIds.NOT_ENOUGH_ITEMS)) mixins.add("nei.TerminalPanelWidgetMixin");
        return mixins;
    }
}
