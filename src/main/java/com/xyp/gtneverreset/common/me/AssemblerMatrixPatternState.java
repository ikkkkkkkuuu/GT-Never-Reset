package com.xyp.gtneverreset.common.me;

import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;

@Getter
public class AssemblerMatrixPatternState {

    private final Map<ItemStack, DireCraftingPatternDetails> patterns = new Reference2ObjectOpenHashMap<>();
    private final Set<IAEItemStack> possibleOutputs = new ObjectOpenHashSet<>();
    private int patternMultiply = 1;

    public void setPatternMultiply(int patternMultiply) {
        this.patternMultiply = Math.max(1, patternMultiply);
    }

    public DireCraftingPatternDetails decodePattern(ItemStack stack, World world) {
        if (!(stack.getItem() instanceof ICraftingPatternItem item)) return null;
        ICraftingPatternDetails pattern = item.getPatternForItem(stack, world);
        if (pattern != null && pattern.isCraftable()) pattern = new DireCraftingPatternDetails(pattern);
        if (!(pattern instanceof DireCraftingPatternDetails details)) return null;
        details.setMultiply(patternMultiply);
        return details;
    }

    public void rebuild(Iterable<ItemStack> inventory, World world) {
        clearPatternData();
        for (ItemStack stack : inventory) {
            DireCraftingPatternDetails details = decodePattern(stack, world);
            if (details != null) addPattern(stack, details);
        }
    }

    public void addPattern(ItemStack stack, DireCraftingPatternDetails details) {
        DireCraftingPatternDetails previous = patterns.put(stack, details);
        if (previous != null) {
            rebuildPossibleOutputs();
        } else {
            addPossibleOutputs(details);
        }
    }

    public boolean onPatternInventoryChanged(World world, ItemStack removedStack, ItemStack newStack) {
        boolean changed = false;
        if (removedStack != null) {
            DireCraftingPatternDetails removedPattern = patterns.remove(removedStack);
            if (removedPattern != null) {
                rebuildPossibleOutputs();
            }
            changed = true;
        }
        if (newStack != null) {
            DireCraftingPatternDetails details = decodePattern(newStack, world);
            if (details != null) {
                addPattern(newStack, details);
                changed = true;
            }
        }
        return changed;
    }

    public void clearPatternData() {
        patterns.clear();
        possibleOutputs.clear();
    }

    private void addPossibleOutputs(DireCraftingPatternDetails details) {
        for (IAEItemStack output : details.getCondensedOutputs()) {
            if (output != null) {
                possibleOutputs.add(output);
            }
        }
    }

    private void rebuildPossibleOutputs() {
        possibleOutputs.clear();
        for (DireCraftingPatternDetails details : patterns.values()) {
            addPossibleOutputs(details);
        }
    }

}
