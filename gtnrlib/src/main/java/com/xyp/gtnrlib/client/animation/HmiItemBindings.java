package com.xyp.gtnrlib.client.animation;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.xyp.gtnrlib.GTNRLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Client-thread bindings from legacy stacks to the item identities used by the local HMI release. */
@SideOnly(Side.CLIENT)
public final class HmiItemBindings {

    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Map<Item, Function<ItemStack, Profile>> bindings = new IdentityHashMap<>();

    /**
     * The optional variant selects a value in an upstream component selector, for example "blue" for axolotl buckets.
     */
    public record Profile(String itemId, String variant) {

        public Profile {
            Objects.requireNonNull(itemId);
            Objects.requireNonNull(variant);
            if (!ITEM_ID.matcher(itemId)
                .matches()) throw new IllegalArgumentException("Invalid namespaced HMI item id: " + itemId);
        }

        public Profile(String itemId) {
            this(itemId, "");
        }
    }

    /** A resolver can inspect damage and NBT and return null to use normal identification for this stack. */
    public static void register(Item item, Function<ItemStack, Profile> resolver) {
        if (bindings.putIfAbsent(Objects.requireNonNull(item), Objects.requireNonNull(resolver)) != null)
            throw new IllegalArgumentException("HMI item already bound: " + Item.itemRegistry.getNameForObject(item));
    }

    public static void unregister(Item item) {
        bindings.remove(item);
    }

    static Profile resolve(ItemStack stack) {
        if (stack == null) return null;
        var resolver = bindings.get(stack.getItem());
        if (resolver == null) return null;
        try {
            return resolver.apply(stack);
        } catch (RuntimeException ex) {
            bindings.remove(stack.getItem(), resolver);
            GTNRLib.LOG
                .error("Disabled failing HMI binding for {}", Item.itemRegistry.getNameForObject(stack.getItem()), ex);
            return null;
        }
    }

    private HmiItemBindings() {}
}
