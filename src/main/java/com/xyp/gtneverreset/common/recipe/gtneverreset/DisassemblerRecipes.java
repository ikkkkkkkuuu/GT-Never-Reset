package com.xyp.gtneverreset.common.recipe.gtneverreset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.GTRecipe_WithAlt;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTShapedRecipe;
import gregtech.api.util.GTShapelessRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GTUtility.ItemId;
import gregtech.common.blocks.ItemMachines;

public final class DisassemblerRecipes {

    public static void register() {
        Set<ItemId> blacklist = createBlacklist();
        Map<ItemId, List<Route>> assembler = assemblerRoutes(blacklist);
        Map<ItemId, List<Route>> crafting = craftingRoutes(blacklist, assembler.keySet());
        assembler.remove(ItemId.create(new ItemStack(Blocks.torch)));
        addRecipe(new ItemStack(Blocks.torch, 6), VoltageIndex.LV, new ItemStack(Items.stick));
        assembler.values()
            .forEach(DisassemblerRecipes::registerCommonParts);
        crafting.values()
            .forEach(DisassemblerRecipes::registerCommonParts);
        GTNeverReset.LOG.info(
            "registered {} disassembler recipes",
            GTNRRecipeMaps.DISASSEMBLER_RECIPES.getAllRecipes()
                .size());
    }

    private static Map<ItemId, List<Route>> assemblerRoutes(Set<ItemId> blacklist) {
        Map<ItemId, List<Route>> assembler = new HashMap<>();
        for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            if (!recipe.mEnabled || recipe.mFakeRecipe) continue;
            for (ItemStack product : recipe.mOutputs) {
                if (!canDisassemble(product, blacklist)) continue;
                // The old machine required the assembler EU/t to be strictly below its nominal voltage.
                Route route = new Route(product, GTUtility.getTier((long) recipe.mEUt + 1));
                assembler.computeIfAbsent(ItemId.create(product), ignored -> new ArrayList<>())
                    .add(route);
                // Leave an empty route for co-products so another route cannot refund the entire forward recipe.
                if (recipe.mOutputs.length != 1 || recipe.mFluidOutputs.length != 0
                    || recipe.getOutputChance(0) != 10000) continue;
                for (int slot = 0; slot < recipe.mInputs.length; slot++) {
                    ItemStack part = recipe.mInputs[slot];
                    if (part == null || part.stackSize <= 0 || recipe.getInputChance(slot) != 10000) continue;
                    if (part.isItemEqual(product)) {
                        route.parts.clear();
                        break;
                    }
                    ItemStack[] alternatives = recipe instanceof GTRecipe_WithAlt alt && alt.mOreDictAlt != null
                        && slot < alt.mOreDictAlt.length ? alt.mOreDictAlt[slot] : null;
                    route.add(
                        alternatives == null || alternatives.length == 0 ? canonical(part)
                            : commonIngredient(Arrays.asList(alternatives)),
                        part.stackSize);
                }
            }
        }
        return assembler;
    }

    private static Map<ItemId, List<Route>> craftingRoutes(Set<ItemId> blacklist, Set<ItemId> assemblerProducts) {
        Map<ItemId, List<Route>> crafting = new HashMap<>();
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            if (!(entry instanceof IRecipe recipe)) continue;
            Collection<?> slots;
            if (recipe.getClass() == GTShapedRecipe.class) slots = Arrays.asList(((GTShapedRecipe) recipe).getInput());
            else if (recipe.getClass() == GTShapelessRecipe.class) slots = ((GTShapelessRecipe) recipe).getInput();
            else continue;
            ItemStack product = recipe.getRecipeOutput();
            if (ItemMachines.getMetaTileEntity(product) == null || !canDisassemble(product, blacklist)
                || assemblerProducts.contains(ItemId.create(product))) continue;
            Route route = new Route(product, VoltageIndex.LV);
            for (Object slot : slots) {
                ItemStack part = slot instanceof ItemStack stack ? canonical(stack)
                    : slot instanceof List<?>alternatives ? commonIngredient(alternatives) : null;
                if (part != null && part.isItemEqual(product)) {
                    route.parts.clear();
                    break;
                }
                route.add(part, 1);
            }
            crafting.computeIfAbsent(ItemId.create(product), ignored -> new ArrayList<>())
                .add(route);
        }

        return crafting;
    }

    private static boolean canDisassemble(ItemStack stack, Set<ItemId> blacklist) {
        if (stack == null || stack.getItem() == null
            || stack.stackSize <= 0
            || stack.hasTagCompound()
            || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE
            || stack.getItem() instanceof MetaGeneratedTool
            || blacklist.contains(ItemId.createWithoutNBT(stack))) return false;
        ItemData data = GTOreDictUnificator.getAssociation(stack);
        if (data != null && data.mPrefix == OrePrefixes.circuit || RecipeMaps.unpackagerRecipes.containsInput(stack))
            return false;
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (circuitForOre(OreDictionary.getOreName(id)) != null) return false;
        }
        return true;
    }

    private static void registerCommonParts(List<Route> routes) {
        Route batch = routes.stream()
            .min(Comparator.comparingInt(route -> route.product.stackSize))
            .get();
        int size = batch.product.stackSize;
        int tier = routes.stream()
            .mapToInt(route -> route.tier)
            .min()
            .getAsInt();
        if (ItemMachines.getMetaTileEntity(batch.product) instanceof MTETieredMachineBlock machine)
            tier = Math.max(tier, machine.mTier);
        if (tier > VoltageIndex.UMV || size > batch.product.getMaxStackSize()) return;
        List<ItemStack> outputs = new ArrayList<>();
        for (Map.Entry<ItemId, Integer> part : batch.parts.entrySet()) {
            long count = part.getValue();
            for (Route route : routes) {
                count = Math
                    .min(count, (long) route.parts.getOrDefault(part.getKey(), 0) * size / route.product.stackSize);
            }
            ItemStack stack = part.getKey()
                .getItemStack();
            while (count > 0) {
                if (outputs.size() == 9 || stack.getMaxStackSize() <= 0) return;
                int amount = (int) Math.min(count, stack.getMaxStackSize());
                outputs.add(
                    part.getKey()
                        .getItemStack(amount));
                count -= amount;
            }
        }
        if (!outputs.isEmpty()) {
            outputs.sort(
                Comparator.comparing((ItemStack s) -> Item.itemRegistry.getNameForObject(s.getItem()))
                    .thenComparingInt(ItemStack::getItemDamage)
                    .thenComparingInt(s -> s.stackSize));
            addRecipe(batch.product.copy(), Math.max(tier, VoltageIndex.LV), outputs.toArray(new ItemStack[0]));
        }
    }

    private static void addRecipe(ItemStack product, int tier, ItemStack... parts) {
        GTRecipeBuilder.builder()
            .itemInputsUnified(product)
            .itemOutputs(parts)
            .nbtSensitive()
            .ignoreCollision()
            .eut(TierEU.RECIPE_LV)
            .duration(600)
            .specialValue(tier)
            .addTo(GTNRRecipeMaps.DISASSEMBLER_RECIPES);
    }

    private static ItemStack commonIngredient(Collection<?> alternatives) {
        ItemStack result = null;
        for (Object alternative : alternatives) {
            if (!(alternative instanceof ItemStack stack)) return null;
            ItemStack part = canonical(stack);
            if (part == null || (result != null && !result.isItemEqual(part))) return null;
            result = part;
        }
        return result;
    }

    private static ItemStack canonical(ItemStack stack) {
        if (stack == null || stack.getItem() == null
            || stack.hasTagCompound()
            || stack.getItem() instanceof MetaGeneratedTool
            || stack.getItem()
                .hasContainerItem(stack))
            return null;
        if (stack.getItem() == Item.getItemFromBlock(Blocks.trapped_chest)) return new ItemStack(Blocks.chest);
        for (int id : OreDictionary.getOreIDs(stack)) {
            String ore = OreDictionary.getOreName(id);
            ItemList circuit = circuitForOre(ore);
            if (circuit != null) return circuit.get(1);
            ItemStack replacement = switch (ore) {
                case "plankWood" -> new ItemStack(Blocks.planks);
                case "stoneCobble" -> new ItemStack(Blocks.cobblestone);
                case "gemDiamond" -> new ItemStack(Items.diamond);
                case "logWood" -> new ItemStack(Blocks.log);
                case "stickWood" -> new ItemStack(Items.stick);
                case "treeSapling" -> new ItemStack(Blocks.sapling);
                default -> null;
            };
            if (replacement != null) return replacement;
        }
        ItemData data = GTOreDictUnificator.getAssociation(stack);
        if (data != null && data.mMaterial != null) {
            Materials material = data.mMaterial.mMaterial;
            Materials base = baseMaterial(material);
            if (base != material) return GTOreDictUnificator.get(data.mPrefix, base, 1);
        }
        ItemStack part = GTOreDictUnificator.get(stack);
        if (part != null && part.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            if (part.getItem()
                .isDamageable()) return null;
            part.setItemDamage(0);
        }
        return part;
    }

    private static ItemList circuitForOre(String ore) {
        return switch (ore) {
            case "circuitPrimitive" -> ItemList.NandChip;
            case "circuitBasic" -> ItemList.Circuit_Microprocessor;
            case "circuitGood" -> ItemList.Circuit_Good;
            case "circuitAdvanced" -> ItemList.Circuit_Advanced;
            case "circuitData" -> ItemList.Circuit_Data;
            case "circuitElite" -> ItemList.Circuit_Elite;
            case "circuitMaster" -> ItemList.Circuit_Master;
            case "circuitUltimate" -> ItemList.Circuit_Quantummainframe;
            case "circuitSuperconductor" -> ItemList.Circuit_Crystalmainframe;
            case "circuitInfinite" -> ItemList.Circuit_Wetwaremainframe;
            case "circuitBio" -> ItemList.Circuit_Biomainframe;
            case "circuitOptical" -> ItemList.Circuit_OpticalMainframe;
            case "circuitExotic" -> ItemList.Circuit_ExoticMainframe;
            case "circuitCosmic" -> ItemList.Circuit_CosmicMainframe;
            case "circuitTranscendent" -> ItemList.Circuit_TranscendentMainframe;
            default -> null;
        };
    }

    private static Materials baseMaterial(Materials material) {
        if (material == Materials.SteelMagnetic) return Materials.Steel;
        if (material == Materials.IronMagnetic || material.mOreReRegistrations.contains(Materials.AnyIron))
            return Materials.Iron;
        if (material == Materials.NeodymiumMagnetic) return Materials.Neodymium;
        if (material == Materials.SamariumMagnetic) return Materials.Samarium;
        if (material == Materials.AnnealedCopper || material.mOreReRegistrations.contains(Materials.AnyCopper))
            return Materials.Copper;
        if (material.mOreReRegistrations.contains(Materials.AnyRubber)
            || material.mOreReRegistrations.contains(Materials.AnySyntheticRubber)) return Materials.Rubber;
        if (material.mOreReRegistrations.contains(Materials.AnyBronze)) return Materials.Bronze;
        return material;
    }

    private static Set<ItemId> createBlacklist() {
        Set<ItemId> excluded = new HashSet<>();
        for (ItemList entry : List.of(
            ItemList.Casing_Coil_Superconductor,
            ItemList.Casing_Fusion_Coil,
            ItemList.Circuit_Parts_Vacuum_Tube,
            ItemList.Schematic,
            ItemList.Transformer_MV_LV,
            ItemList.Transformer_HV_MV,
            ItemList.Transformer_EV_HV,
            ItemList.Transformer_IV_EV,
            ItemList.Automation_ChestBuffer_UEV,
            ItemList.Automation_ChestBuffer_UIV,
            ItemList.Automation_ChestBuffer_UMV,
            ItemList.CircuitAssemblerUEV,
            ItemList.CircuitAssemblerUIV,
            ItemList.CircuitAssemblerUMV,
            ItemList.CircuitAssemblerUXV,
            ItemList.CircuitAssemblerMAX)) {
            if (entry.hasBeenSet()) exclude(excluded, entry.get(1));
        }
        for (int tier = VoltageIndex.UEV; tier < ItemList.MACHINE_HULLS.length; tier++) {
            if (ItemList.MACHINE_HULLS[tier].hasBeenSet()) exclude(excluded, ItemList.MACHINE_HULLS[tier].get(1));
            if (ItemList.MACHINE_CASINGS[tier].hasBeenSet()) exclude(excluded, ItemList.MACHINE_CASINGS[tier].get(1));
        }
        exclude(excluded, Materials.Graphene.getDust(1));
        exclude(excluded, GTModHandler.getIC2Item("mixedMetalIngot", 1));
        exclude(excluded, Mods.IndustrialCraft2, "blockElectric", 3, 4, 5, 6);
        exclude(excluded, Mods.Railcraft, "track", 0, 736, 816);
        exclude(excluded, Mods.Railcraft, "machine.alpha", 14);
        exclude(excluded, Mods.AppliedEnergistics2, "item.ItemMultiPart", 36, 536);
        exclude(excluded, Mods.GTPlusPlus, "gtplusplus.blockcasings.3", 13);
        exclude(excluded, Mods.GTPlusPlusEverglades, "blockDarkWorldPortalFrame", 0);
        return excluded;
    }

    private static void exclude(Set<ItemId> excluded, Mods mod, String item, int... metas) {
        if (mod.isModLoaded()) for (int meta : metas) exclude(excluded, GTModHandler.getModItem(mod.ID, item, 1, meta));
    }

    private static void exclude(Set<ItemId> excluded, ItemStack stack) {
        if (stack != null) excluded.add(ItemId.createWithoutNBT(stack));
    }

    private static final class Route {

        final ItemStack product;
        final int tier;
        final Map<ItemId, Integer> parts = new HashMap<>();

        Route(ItemStack product, int tier) {
            this.product = product;
            this.tier = tier;
        }

        void add(ItemStack part, int count) {
            if (part != null)
                parts.merge(ItemId.create(part), count, (a, b) -> (int) Math.min((long) a + b, Integer.MAX_VALUE));
        }
    }
}
