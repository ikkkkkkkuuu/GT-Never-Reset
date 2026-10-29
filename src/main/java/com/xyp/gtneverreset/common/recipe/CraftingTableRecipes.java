package com.xyp.gtneverreset.common.recipe;

import static appeng.items.tools.powered.ToolWirelessTerminal.infinityBoosterCard;
import static appeng.items.tools.powered.ToolWirelessTerminal.infinityEnergyCard;
import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_INTERFACE_TERM;
import static com.glodblock.github.loader.ItemAndBlockHolder.WIRELESS_PATTERN_TERM;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.xyp.gtneverreset.loader.ItemLoader;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.ModList;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;

import WayofTime.alchemicalWizardry.ModBlocks;
import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;
import ggfab.GGItemList;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import thaumcraft.common.config.ConfigBlocks;

public final class CraftingTableRecipes {

    public static void register() {
        GameRegistry.addRecipe(
            new ItemStack(ItemLoader.permanentMagnetRing),
            " I ",
            "IRI",
            " I ",
            'I',
            Items.iron_ingot,
            'R',
            Items.redstone);
        GameRegistry.addRecipe(
            new ItemStack(ItemLoader.harvestRing),
            " G ",
            "GWG",
            " G ",
            'G',
            Items.gold_ingot,
            'W',
            Items.wheat);
        GTModHandler.addCraftingRecipe(
            GTNRItemList.VeinMiningPickaxe.get(1),
            new Object[] { "AAA", " B ", " A ", 'A', new ItemStack(Blocks.planks, 1, 0), 'B',
                new ItemStack(Items.wooden_pickaxe, 1, 0) });

        registerPackagedRecipes();
        registerWirelessTerminalRecipes(GTNRItemList.WirelessDualInterfaceTerminal.get(1));
        registerWirelessFrequencyCardRecipes();
        registerWildcardPatternRecipes();
        registerAdvancedIORecipes();
        registerStockIOInterfaceRecipes();
        registerCompactAEMachineRecipes();
        registerStableEndCrystalRecipe();
        registerFragmentWorldCollectorRecipe();
    }

    /** Registers ordinary crafting upgrades without replacing any upstream or modpack recipe. */
    private static void registerPackagedRecipes() {
        var definitions = AEApi.instance()
            .definitions();
        var materials = definitions.materials();
        GameRegistry.addRecipe(
            GTNRItemList.BasicPackagedCore.get(1),
            "IFI",
            "FEF",
            "IFI",
            'I',
            Items.iron_ingot,
            'F',
            materials.fluixCrystal()
                .maybeStack(1)
                .get(),
            'E',
            materials.engProcessor()
                .maybeStack(1)
                .get());
        GameRegistry.addShapelessRecipe(
            GTNRItemList.WirelessPackagedPatternProvider.get(1),
            definitions.blocks()
                .iface()
                .maybeStack(1)
                .get(),
            GTNRItemList.BasicPackagedCore.get(1),
            materials.wireless()
                .maybeStack(1)
                .get());
        GameRegistry.addRecipe(
            GTNRItemList.ItemWirelessConnector.get(1),
            " W",
            "F ",
            'W',
            materials.wireless()
                .maybeStack(1)
                .get(),
            'F',
            materials.fluixCrystal()
                .maybeStack(1)
                .get());
        if (ModList.Thaumcraft.isModLoaded()) registerInfusion();
        if (ModList.BloodMagic.isModLoaded()) registerBloodAltar();
        GameRegistry.addShapelessRecipe(
            GTNRItemList.AssemblyLineCore.get(1),
            GTNRItemList.BasicPackagedCore.get(1),
            ItemList.Machine_Multi_Assemblyline.get(1));
        GameRegistry.addShapelessRecipe(
            GTNRItemList.AdvancedAssemblyLineCore.get(1),
            GTNRItemList.BasicPackagedCore.get(1),
            GGItemList.AdvAssLine.get(1));
        GameRegistry.addShapelessRecipe(
            GTNRItemList.CircuitAssemblyLineCore.get(1),
            GTNRItemList.BasicPackagedCore.get(1),
            ModsItemlist.CircuitAssemblyLine.get(1));
    }

    /** Kept separate so an installation without TC never resolves a TC item while registering common recipes. */
    private static void registerInfusion() {
        GameRegistry.addShapelessRecipe(
            GTNRItemList.ThaumcraftCrucibleCore.get(1),
            GTNRItemList.BasicPackagedCore.get(1),
            new ItemStack(ConfigBlocks.blockMetalDevice, 1, 0));
        GameRegistry.addShapelessRecipe(
            GTNRItemList.ArcaneWorkbenchCore.get(1),
            GTNRItemList.BasicPackagedCore.get(1),
            new ItemStack(ConfigBlocks.blockTable, 1, 15));
        GameRegistry.addShapelessRecipe(
            GTNRItemList.ThaumcraftInfusionCore.get(1),
            GTNRItemList.BasicPackagedCore.get(1),
            new ItemStack(ConfigBlocks.blockStoneDevice, 1, 2));
    }

    /** Requires the actual altar, preserving its existing progression gate. */
    private static void registerBloodAltar() {
        GameRegistry.addShapelessRecipe(
            GTNRItemList.BloodAltarCore.get(1),
            GTNRItemList.BasicPackagedCore.get(1),
            new ItemStack(ModBlocks.blockAltar));
    }

    private static void registerWirelessTerminalRecipes(ItemStack terminal) {
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

    private static void registerWirelessFrequencyCardRecipes() {
        var definitions = AEApi.instance()
            .definitions();
        GameRegistry.addShapelessRecipe(
            GTNRItemList.WirelessFrequencyCard.get(1),
            definitions.materials()
                .advCard()
                .maybeStack(1)
                .get(),
            definitions.items()
                .memoryCard()
                .maybeStack(1)
                .get(),
            definitions.materials()
                .wireless()
                .maybeStack(1)
                .get(),
            definitions.materials()
                .engProcessor()
                .maybeStack(1)
                .get());
    }

    private static void registerWildcardPatternRecipes() {
        GameRegistry.addRecipe(
            GTNRItemList.WildcardPattern.get(1),
            "B",
            'B',
            AEApi.instance()
                .definitions()
                .materials()
                .blankPattern()
                .maybeStack(1)
                .get());
    }

    private static void registerAdvancedIORecipes() {
        var definitions = AEApi.instance()
            .definitions();
        GameRegistry.addShapedRecipe(
            GTNRItemList.AdvancedIOBus.get(1),
            "ICI",
            "EPE",
            "ICI",
            'I',
            definitions.parts()
                .importBus()
                .maybeStack(1)
                .get(),
            'E',
            definitions.parts()
                .exportBus()
                .maybeStack(1)
                .get(),
            'C',
            definitions.materials()
                .calcProcessor()
                .maybeStack(1)
                .get(),
            'P',
            definitions.materials()
                .engProcessor()
                .maybeStack(1)
                .get());
    }

    private static void registerStockIOInterfaceRecipes() {
        GameRegistry.addShapelessRecipe(
            GTNRItemList.StockIOInterface.get(1),
            AEApi.instance()
                .definitions()
                .blocks()
                .iface()
                .maybeStack(1)
                .get(),
            GTNRItemList.AdvancedIOBus.get(1));
        GameRegistry.addRecipe(new Conversion(GTNRItemList.StockIOInterface, GTNRItemList.StockIOInterfacePart));
        GameRegistry.addRecipe(new Conversion(GTNRItemList.StockIOInterfacePart, GTNRItemList.StockIOInterface));
    }

    /** Form conversion preserves ghost settings and real unsettled refunds; ordinary crafting would discard them. */
    private static final class Conversion extends ShapelessRecipes {

        private Conversion(GTNRItemList input, GTNRItemList output) {
            super(output.get(1), Collections.singletonList(input.get(1)));
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            if (!matches(inventory, null)) return null;
            ItemStack result = super.getCraftingResult(inventory);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack source = inventory.getStackInSlot(slot);
                if (source != null && source.hasTagCompound()) {
                    result.setTagCompound(
                        (NBTTagCompound) source.getTagCompound()
                            .copy());
                    break;
                }
            }
            return result;
        }
    }

    private static void registerCompactAEMachineRecipes() {
        var definitions = AEApi.instance()
            .definitions();
        GameRegistry.addShapedRecipe(
            GTNRItemList.QuantumComputer.get(1),
            "IRI",
            "CMC",
            "IRI",
            'I',
            Items.iron_ingot,
            'R',
            Items.redstone,
            'C',
            definitions.materials()
                .fluixCrystal()
                .maybeStack(1)
                .get(),
            'M',
            definitions.blocks()
                .molecularAssembler()
                .maybeStack(1)
                .get());
        GameRegistry.addShapedRecipe(
            GTNRItemList.AssemblerMatrix.get(1),
            "IFI",
            "AMA",
            "IFI",
            'I',
            Items.iron_ingot,
            'F',
            definitions.materials()
                .fluixCrystal()
                .maybeStack(1)
                .get(),
            'A',
            definitions.blocks()
                .iface()
                .maybeStack(1)
                .get(),
            'M',
            definitions.blocks()
                .molecularAssembler()
                .maybeStack(1)
                .get());
    }

    private static void registerStableEndCrystalRecipe() {
        GameRegistry.addRecipe(
            GTNRItemList.StableEndCrystal.get(1),
            "GGG",
            "GEG",
            "GTG",
            'G',
            Blocks.glass,
            'E',
            Items.ender_eye,
            'T',
            Items.ghast_tear);
    }

    private static void registerFragmentWorldCollectorRecipe() {
        GTModHandler.addCraftingRecipe(
            GTNRItemList.FragmentWorldCollector.get(1),
            new Object[] { "WNW", "PCQ", "XNX", 'W', OrePrefixes.cableGt01.get(Materials.Lead), 'N', "logRubber", 'P',
                ItemList.Sensor_LV.get(1), 'Q', ItemList.Emitter_LV.get(1), 'C', ItemList.Hull_LV.get(1), 'X',
                new ItemStack(Blocks.anvil) });
    }

    private static final class WirelessTerminalUpgradeRecipe extends ShapelessRecipes {

        private final Item terminal;
        private final ItemStack card;
        private final String upgradeKey;

        public WirelessTerminalUpgradeRecipe(ItemStack terminal, ItemStack card, String upgradeKey) {
            super(install(terminal, upgradeKey), Arrays.asList(terminal.copy(), card.copy()));
            this.terminal = terminal.getItem();
            this.card = card.copy();
            this.upgradeKey = upgradeKey;
        }

        @Override
        public boolean matches(InventoryCrafting inventory, World world) {
            boolean foundTerminal = false;
            boolean foundCard = false;
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack == null) continue;
                if (stack.stackSize <= 0) return false;
                if (stack.getItem() == terminal && !foundTerminal) {
                    if (stack.hasTagCompound() && stack.getTagCompound()
                        .getBoolean(upgradeKey)) return false;
                    foundTerminal = true;
                } else if (card.isItemEqual(stack) && !foundCard) {
                    foundCard = true;
                } else {
                    return false;
                }
            }
            return foundTerminal && foundCard;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack != null && stack.getItem() == terminal) return install(stack, upgradeKey);
            }
            return null;
        }

        private static ItemStack install(ItemStack terminal, String upgradeKey) {
            ItemStack installed = terminal.copy();
            installed.stackSize = 1;
            if (!installed.hasTagCompound()) installed.setTagCompound(new NBTTagCompound());
            installed.getTagCompound()
                .setBoolean(upgradeKey, true);
            return installed;
        }
    }
}
