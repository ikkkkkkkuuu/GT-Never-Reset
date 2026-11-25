package com.xyp.gtneverreset.common.machine.multiblock.bee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IBeeRoot;

public final class BeeBreedingInputs {

    private final Map<String, List<ItemStack>> drones = new HashMap<>();
    private final Map<String, List<ItemStack>> females = new HashMap<>();
    private final Map<String, ItemStack> specimens = new LinkedHashMap<>();
    private final Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ItemStack, Integer> reserved = new IdentityHashMap<>();
    private long femaleCount;

    public BeeBreedingInputs(List<ItemStack> inputs, Set<ItemStack> samplesOnly) {
        IBeeRoot root = BeeBreedingHelper.getBeeRoot();
        if (root == null) return;
        for (ItemStack stack : inputs) {
            if (stack == null || stack.stackSize <= 0 || !seen.add(stack) || !root.isMember(stack)) continue;
            EnumBeeType type = root.getType(stack);
            if (!isParentType(type)) continue;
            String species = BeeBreedingHelper.getBeeUID(stack);
            if (species == null || species.isEmpty()) continue;
            specimens.putIfAbsent(species, stack);
            if (!samplesOnly.contains(stack)) index(stack, species, type);
        }
    }

    public static boolean isParentType(EnumBeeType type) {
        return type == EnumBeeType.DRONE || type == EnumBeeType.PRINCESS || type == EnumBeeType.QUEEN;
    }

    void index(ItemStack stack, String species, EnumBeeType type) {
        Map<String, List<ItemStack>> inventory = type == EnumBeeType.DRONE ? drones : females;
        inventory.computeIfAbsent(species, ignored -> new ArrayList<>())
            .add(stack);
        if (type != EnumBeeType.DRONE) femaleCount += stack.stackSize;
    }

    public Map<String, ItemStack> getSpecimens() {
        return Collections.unmodifiableMap(specimens);
    }

    public int getFemaleCount() {
        return (int) Math.min(Integer.MAX_VALUE, femaleCount);
    }

    public Set<String> getFemaleSpecies() {
        return availableSpecies(females);
    }

    public Set<String> getDroneSpecies() {
        return availableSpecies(drones);
    }

    private Set<String> availableSpecies(Map<String, List<ItemStack>> inventory) {
        Set<String> species = new LinkedHashSet<>();
        inventory.forEach((uid, stacks) -> { if (findAvailable(stacks) != null) species.add(uid); });
        return species;
    }

    public boolean hasFemale(String species) {
        return findAvailable(females.get(species)) != null;
    }

    public boolean hasDrone(String species) {
        return findAvailable(drones.get(species)) != null;
    }

    public ParentPair findPair(String parent1, String parent2) {
        ParentPair pair = findOrientedPair(parent1, parent2);
        return pair == null ? findOrientedPair(parent2, parent1) : pair;
    }

    private ParentPair findOrientedPair(String femaleSpecies, String droneSpecies) {
        ItemStack drone = findAvailable(drones.get(droneSpecies));
        if (drone == null) return null;
        ItemStack female = findAvailable(females.get(femaleSpecies));
        return female == null ? null : new ParentPair(femaleSpecies, female, drone);
    }

    private ItemStack findAvailable(List<ItemStack> stacks) {
        if (stacks == null) return null;
        for (ItemStack stack : stacks) if (stack.stackSize > reserved.getOrDefault(stack, 0)) return stack;
        return null;
    }

    public void reserve(ParentPair pair, boolean consumeFemale, boolean consumeDrone) {
        if (consumeFemale) {
            reserved.merge(pair.female(), 1, Integer::sum);
            femaleCount--;
        }
        if (consumeDrone) reserved.merge(pair.drone(), 1, Integer::sum);
    }

    public void commit() {
        reserved.forEach((stack, count) -> stack.stackSize -= count);
        reserved.clear();
    }

    public record ParentPair(String femaleSpecies, ItemStack female, ItemStack drone) {}
}
