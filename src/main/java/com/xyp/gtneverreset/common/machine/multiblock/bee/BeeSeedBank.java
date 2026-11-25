package com.xyp.gtneverreset.common.machine.multiblock.bee;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import forestry.api.apiculture.EnumBeeType;

public final class BeeSeedBank {

    private final Map<String, ItemStack> seeds = new LinkedHashMap<>();
    private final Map<ItemStack, BeeIdentity> breedingInputs = new LinkedHashMap<>();

    public boolean retain(String species, ItemStack specimen) {
        if (seeds.containsKey(species)) return false;
        ItemStack seed = BeeBreedingHelper.copyBeeAsType(specimen, EnumBeeType.PRINCESS);
        if (seed == null) return false;
        seeds.put(species, seed);
        return true;
    }

    public Set<String> getSpecies() {
        return Collections.unmodifiableSet(seeds.keySet());
    }

    public boolean hasSpecies(String species) {
        return seeds.containsKey(species);
    }

    public ItemStack createParent(String species, EnumBeeType type) {
        return BeeBreedingHelper.copyBeeAsType(seeds.get(species), type);
    }

    public void addBreedingInput(ItemStack bee) {
        String species = BeeBreedingHelper.getBeeUID(bee);
        if (species == null || species.isEmpty() || bee.stackSize <= 0) return;
        EnumBeeType type = BeeBreedingHelper.getBeeRoot()
            .getType(bee);
        if (BeeBreedingInputs.isParentType(type)) breedingInputs.put(bee.copy(), new BeeIdentity(species, type));
    }

    public void indexInputs(BeeBreedingInputs inputs) {
        breedingInputs.forEach((bee, identity) -> inputs.index(bee, identity.species(), identity.type()));
    }

    public boolean ownsInput(ItemStack bee) {
        return breedingInputs.containsKey(bee);
    }

    public void removeEmptyInputs() {
        breedingInputs.keySet()
            .removeIf(bee -> bee.stackSize <= 0);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList templates = new NBTTagList();
        for (ItemStack seed : seeds.values()) templates.appendTag(seed.writeToNBT(new NBTTagCompound()));
        tag.setTag("seeds", templates);
        NBTTagList inputs = new NBTTagList();
        for (ItemStack input : breedingInputs.keySet()) inputs.appendTag(input.writeToNBT(new NBTTagCompound()));
        tag.setTag("inputs", inputs);
        return tag;
    }

    public static BeeSeedBank fromNBT(NBTTagCompound tag) {
        BeeSeedBank bank = new BeeSeedBank();
        NBTTagList templates = tag.getTagList("seeds", 10);
        for (int i = 0; i < templates.tagCount(); i++) {
            ItemStack seed = ItemStack.loadItemStackFromNBT(templates.getCompoundTagAt(i));
            String species = BeeBreedingHelper.getBeeUID(seed);
            if (species != null && !species.isEmpty()) bank.retain(species, seed);
        }
        NBTTagList inputs = tag.getTagList("inputs", 10);
        for (int i = 0; i < inputs.tagCount(); i++) {
            bank.addBreedingInput(ItemStack.loadItemStackFromNBT(inputs.getCompoundTagAt(i)));
        }
        return bank;
    }

    private record BeeIdentity(String species, EnumBeeType type) {}
}
