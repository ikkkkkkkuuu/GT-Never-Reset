package com.xyp.gtneverreset.compat.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.StackInfo;
import gregtech.api.enums.ItemList;
import gregtech.nei.GTNEIDefaultHandler;

final class TerminalRecipeIngredients {

    static List<Ingredient<?>> inputs(IRecipeHandler recipe, List<PositionedStack> recipeInputs, boolean fluidsFirst) {
        List<PositionedStack> stacks = new ArrayList<>(recipeInputs);
        String identifier = recipe.getOverlayIdentifier();
        if ("gt.recipe.scanner".equals(identifier) || "gt.recipe.fakeAssemblylineProcess".equals(identifier)) {
            for (int i = stacks.size() - 1; i >= 0; i--) {
                PositionedStack stack = stacks.get(i);
                if (stack != null && (ItemList.Tool_DataStick.isStackEqual(stack.item, false, true)
                    || ItemList.Tool_DataOrb.isStackEqual(stack.item, false, true))) {
                    stacks.remove(i);
                    break;
                }
            }
        }
        List<Ingredient<?>> ingredients = collect(
            stacks,
            isThaumcraft(identifier),
            TerminalRecipeIngredients::selectedInput);
        if (fluidsFirst) ingredients.sort(
            (first, second) -> Boolean
                .compare(!(first.getStack() instanceof FluidStack), !(second.getStack() instanceof FluidStack)));
        for (int i = 0; i < ingredients.size(); i++) ingredients.get(i)
            .setIndex(i);
        return ingredients;
    }

    static List<Ingredient<?>> outputs(IRecipeHandler recipe, int index, boolean includeOther) {
        List<PositionedStack> stacks = new ArrayList<>();
        PositionedStack result = recipe.getResultStack(index);
        if (result != null) stacks.add(result);
        if (includeOther && !isThaumcraft(recipe.getOverlayIdentifier())) {
            stacks.addAll(safeList(recipe.getOtherStacks(index)));
        }
        return collect(stacks, isThaumcraft(recipe.getOverlayIdentifier()), positioned -> positioned.item);
    }

    static List<ItemStack> inputAlternatives(PositionedStack positioned) {
        if (positioned == null || positioned.items == null || positioned.items.length == 0) return List.of();
        for (ItemStack stack : positioned.items) {
            if (stack == null || stack.getItem() == null) {
                List<ItemStack> valid = Arrays.stream(positioned.items)
                    .filter(item -> item != null && item.getItem() != null)
                    .toList();
                return valid.isEmpty() ? List.of()
                    : new PositionedStack(valid, positioned.relx, positioned.rely, false).getFilteredPermutations();
            }
        }
        return positioned.getFilteredPermutations();
    }

    static ItemStack selectedInput(PositionedStack positioned) {
        if (positioned == null) return null;
        // The handler can retain a rendered permutation that the current NEI filters hide.
        ItemStack displayed = positioned.item;
        ItemStack firstVisible = null;
        for (ItemStack alternative : inputAlternatives(positioned)) {
            ItemStack rendered = renderedInput(alternative);
            if (displayed != null && rendered.isItemEqual(displayed)
                && ItemStack.areItemStackTagsEqual(rendered, displayed)) return displayed;
            if (firstVisible == null) firstVisible = rendered;
        }
        return firstVisible == null ? displayed : firstVisible;
    }

    private static ItemStack renderedInput(ItemStack alternative) {
        if (alternative.getItemDamage() != OreDictionary.WILDCARD_VALUE || !alternative.getItem()
            .isRepairable()) {
            return alternative;
        }
        ItemStack rendered = alternative.copy();
        rendered.setItemDamage(0);
        return rendered;
    }

    static List<Ingredient<?>> compress(List<Ingredient<?>> ingredients) {
        List<Ingredient<?>> result = new ArrayList<>();
        for (Ingredient<?> ingredient : ingredients) {
            if (ingredient.getStack() instanceof ItemStack item) {
                if (item.stackSize == 0) continue;
                boolean merged = false;
                for (Ingredient<?> previous : result) {
                    if (previous.getStack() instanceof ItemStack existing && existing.isItemEqual(item)
                        && ItemStack.areItemStackTagsEqual(existing, item)
                        && existing.stackSize + item.stackSize <= existing.getMaxStackSize()) {
                        existing.stackSize += item.stackSize;
                        merged = true;
                        break;
                    }
                }
                if (merged) continue;
            }
            result.add(ingredient);
        }
        return result;
    }

    static List<Ingredient<?>> clearNull(List<Ingredient<?>> ingredients) {
        List<Ingredient<?>> result = new ArrayList<>();
        for (Ingredient<?> ingredient : ingredients) {
            if (ingredient == null || ingredient.getStack() instanceof ItemStack item && item.stackSize == 0) continue;
            ingredient.setIndex(result.size());
            result.add(ingredient);
        }
        return result;
    }

    static String interfaceName(IRecipeHandler recipe, List<Ingredient<?>> ingredients) {
        StringBuilder name = new StringBuilder(recipe.getRecipeName());
        if (recipe instanceof GTNEIDefaultHandler && ClientConfig.wirelessTerminal.appendNonConsumableItemNumbers) {
            for (Ingredient<?> ingredient : ingredients) {
                if (ingredient.getStack() instanceof ItemStack item && item.stackSize == 0) {
                    name.append(' ')
                        .append(item.getItemDamage());
                }
            }
        }
        return name.toString();
    }

    static boolean isVirtualMold(ItemStack item) {
        for (ItemList entry : ItemList.values()) {
            if ((entry.name()
                .startsWith("Shape_")
                || entry.name()
                    .startsWith("Laser_Lens_"))
                && entry.hasBeenSet()
                && entry.isStackEqual(item, false, true)) return true;
        }
        for (int ore : OreDictionary.getOreIDs(item)) {
            String name = OreDictionary.getOreName(ore);
            if (name.startsWith("lens") || name.startsWith("nanite")) return true;
        }
        for (ModsItemlist entry : new ModsItemlist[] { ModsItemlist.CalculationPress, ModsItemlist.EngineeringPress,
            ModsItemlist.LogicPress, ModsItemlist.SiliconPress, ModsItemlist.SingleUseFileMold,
            ModsItemlist.SingleUseWrenchMold, ModsItemlist.SingleUseCrowbarMold, ModsItemlist.SingleUseWireCutterMold,
            ModsItemlist.SingleUseHardHammerMold, ModsItemlist.SingleUseSoftMalletMold,
            ModsItemlist.SingleUseScrewdriverMold, ModsItemlist.SingleUseSawMold, ModsItemlist.ReinforcedGlassLens,
            ModsItemlist.MysteriousCrystalLens, ModsItemlist.RadoxPolymerLens, ModsItemlist.ChromaticLens }) {
            ItemStack candidate = entry.get(1);
            if (candidate != null && candidate.isItemEqual(item) && ItemStack.areItemStackTagsEqual(candidate, item)) {
                return true;
            }
        }
        return false;
    }

    static boolean isArcane(String identifier) {
        return "thaumcraft.arcane.shaped".equals(identifier) || "thaumcraft.arcane.shapeless".equals(identifier)
            || "thaumcraft.wands".equals(identifier);
    }

    static NBTTagList arcaneLayout(List<PositionedStack> ingredients) {
        NBTTagCompound[] cells = new NBTTagCompound[9];
        for (PositionedStack ingredient : ingredients) {
            ItemStack selected = selectedInput(ingredient);
            if (selected == null || isAspectDisplay(selected)) continue;
            int x = ingredient.relx - 47;
            int y = ingredient.rely - 38;
            if (x < 0 || y < 0 || x % 28 != 0 || y % 27 != 0 || x / 28 > 2 || y / 27 > 2) {
                throw new IllegalArgumentException("invalid arcane recipe position");
            }
            int slot = x / 28 + y / 27 * 3;
            if (cells[slot] != null) throw new IllegalArgumentException("duplicate arcane recipe position");
            ItemStack item = selected.copy();
            item.stackSize = 1;
            cells[slot] = item.writeToNBT(new NBTTagCompound());
        }
        NBTTagList layout = new NBTTagList();
        for (NBTTagCompound cell : cells) layout.appendTag(cell == null ? new NBTTagCompound() : cell);
        return layout;
    }

    private static List<Ingredient<?>> collect(List<PositionedStack> stacks, boolean physicalItemsOnly,
        Function<PositionedStack, ItemStack> selectStack) {
        List<Ingredient<?>> result = new ArrayList<>();
        for (PositionedStack positioned : stacks) {
            if (positioned == null) continue;
            ItemStack selected = selectStack.apply(positioned);
            if (selected == null) continue;
            ItemStack item = selected.copy();
            if (physicalItemsOnly && isAspectDisplay(item)) continue;
            FluidStack fluid = StackInfo.isFluidDisplayItem(item) ? StackInfo.getFluid(item) : null;
            result.add(
                fluid == null ? new Ingredient<>(item, result.size()) : new Ingredient<>(fluid.copy(), result.size()));
        }
        return result;
    }

    private static boolean isThaumcraft(String identifier) {
        return identifier != null && identifier.startsWith("thaumcraft.");
    }

    private static boolean isAspectDisplay(ItemStack stack) {
        for (Class<?> type = stack.getItem()
            .getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if ("com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect".equals(name)
                || "com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect".equals(name)) return true;
        }
        return false;
    }

    private static List<PositionedStack> safeList(List<PositionedStack> stacks) {
        return stacks == null ? new ArrayList<>() : stacks;
    }

    static final class Ingredient<T> {

        private final T stack;
        private int index;

        Ingredient(T stack, int index) {
            this.stack = stack;
            this.index = index;
        }

        T getStack() {
            return stack;
        }

        int getIndex() {
            return index;
        }

        void setIndex(int index) {
            this.index = index;
        }
    }
}
