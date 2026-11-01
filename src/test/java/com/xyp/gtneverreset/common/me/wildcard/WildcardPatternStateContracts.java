package com.xyp.gtneverreset.common.me.wildcard;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.crafting.ICraftingPatternDetails;

public final class WildcardPatternStateContracts {

    public static void main(String[] args) throws Exception {
        Item item = new Item();
        var register = Item.itemRegistry.getClass()
            .getDeclaredMethod("addObjectRaw", int.class, String.class, Object.class);
        register.setAccessible(true);
        register.invoke(Item.itemRegistry, 32001, "gtnr_contract:wildcard_pattern", item);
        checkOccupiedSlot(item);
        checkPersistence(item);
        System.out.println("Wildcard active-pattern isolation, legacy NBT restoration and empty-slot contracts passed");
    }

    private static void checkOccupiedSlot(Item item) {
        List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(item, 64)));
        WildcardPatternSlotState state = state(items);
        ICraftingPatternDetails iron = details(pattern(item, "iron"));
        ICraftingPatternDetails copper = details(pattern(item, "copper"));
        ICraftingPatternDetails unmarked = details(new ItemStack(item));
        state.setActivePatternDetails(iron);
        require(state.canAcceptPattern(iron), "the active recipe accepts another batch");
        require(!state.canAcceptPattern(copper), "occupied slots reject a different generated recipe");
        require(!state.canAcceptPattern(unmarked), "occupied slots require a generated identity");
        require(state.getPatternDetails(unmarked) == iron, "occupied slots retain the active recipe");
        items.clear();
        require(state.canAcceptPattern(copper), "empty slots may change recipes");
        require(state.getPatternDetails(unmarked) == unmarked, "empty slots restore the base pattern");
        require(
            state.getActiveGeneratedPatternId()
                .isEmpty(),
            "empty slots release their active identity");
    }

    private static void checkPersistence(Item item) {
        List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(item, 128)));
        ItemStack pattern = pattern(item, "iron");
        ICraftingPatternDetails iron = details(pattern);
        WildcardPatternSlotState state = state(items);
        state.setActivePatternDetails(iron);
        pattern.getTagCompound()
            .setString(WildcardPatternGenerator.KEY_GENERATED_PATTERN_ID, "changed");
        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("hostInventory", "retained");
        require(state.writeToNBT(saved, true) == saved, "state augments the host compound");
        require("retained".equals(saved.getString("hostInventory")), "host inventory tags survive");
        require("iron".equals(saved.getString("WildcardActivePatternId")), "legacy identity key is preserved");
        ItemStack persistedPattern = ItemStack.loadItemStackFromNBT(saved.getCompoundTag("WildcardActivePattern"));
        require(
            "iron".equals(WildcardPatternGenerator.getGeneratedPatternId(persistedPattern)),
            "saved pattern owns a snapshot independent of the caller");
        ICraftingPatternDetails restoredIron = details(persistedPattern);
        WildcardPatternSlotState restored = state(items);
        restored.restoreActivePatternDetails(saved, List.of(restoredIron));
        require(restored.getPatternDetails(null) == restoredIron, "full legacy save restores its selected expansion");
        NBTTagCompound idOnly = new NBTTagCompound();
        idOnly.setString("WildcardActivePatternId", "iron");
        WildcardPatternSlotState legacy = state(items);
        legacy.restoreActivePatternDetails(idOnly, List.of(restoredIron));
        require(legacy.getPatternDetails(null) == restoredIron, "identity-only legacy saves still restore");
        WildcardPatternSlotState second = state(items);
        require(
            second.getActiveGeneratedPatternId()
                .isEmpty(),
            "each slot owns independent active state");
        items.clear();
        restored.writeToNBT(saved, false);
        require(
            !saved.hasKey("WildcardActivePattern") && !saved.hasKey("WildcardActivePatternId"),
            "empty saves remove stale active-pattern tags");
        require("retained".equals(saved.getString("hostInventory")), "clearing state preserves host tags");
        second.restoreActivePatternDetails(idOnly, List.of(restoredIron));
        require(
            second.getActiveGeneratedPatternId()
                .isEmpty(),
            "an empty slot does not revive an old recipe");
    }

    private static WildcardPatternSlotState state(List<ItemStack> items) {
        List<FluidStack> fluids = new ArrayList<>();
        return new WildcardPatternSlotState(null, () -> null, items, fluids, () -> items.isEmpty() && fluids.isEmpty());
    }

    private static ItemStack pattern(Item item, String id) {
        ItemStack stack = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(WildcardPatternGenerator.KEY_GENERATED_PATTERN_ID, id);
        stack.setTagCompound(tag);
        return stack;
    }

    private static ICraftingPatternDetails details(ItemStack pattern) {
        return (ICraftingPatternDetails) Proxy.newProxyInstance(
            WildcardPatternStateContracts.class.getClassLoader(),
            new Class<?>[] { ICraftingPatternDetails.class },
            (proxy, method, arguments) -> {
                if (method.getName()
                    .equals("getPattern")) return pattern;
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
