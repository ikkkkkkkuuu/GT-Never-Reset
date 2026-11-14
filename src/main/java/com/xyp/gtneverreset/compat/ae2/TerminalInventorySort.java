package com.xyp.gtneverreset.compat.ae2;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalInventorySort implements Comparator<IAEStack<?>> {

    private static final Comparator<String> REGISTRY_NAMES = Comparator.nullsFirst(Comparator.naturalOrder());
    private static final MethodHandle ITEM_ORDER = findItemOrder();
    private final Map<IAEStack<?>, SortKey> keys = new IdentityHashMap<>();
    private final int direction;

    /** Each comparator belongs to one sort invocation; mutable quantities and rules never survive a refresh. */
    public TerminalInventorySort(int direction) {
        this.direction = direction;
    }

    @Override
    public int compare(IAEStack<?> first, IAEStack<?> second) {
        SortKey a = keys.computeIfAbsent(first, SortKey::new);
        SortKey b = keys.computeIfAbsent(second, SortKey::new);
        return compareKeys(a, b) * direction;
    }

    private static int compareKeys(SortKey a, SortKey b) {
        if (b.stack == null) return -1;
        if (a.stack == null || a.order == -1) return 1;
        if (a.order != b.order) return a.order - b.order;
        if (a.stack.getItem() != b.stack.getItem()) {
            return REGISTRY_NAMES.compare(a.registryName(), b.registryName());
        }
        if (a.named || b.named) {
            if (!a.named) return -1;
            if (!b.named) return 1;
            int names = a.displayName()
                .compareTo(b.displayName());
            if (names != 0) return names;
        }
        a.readEnchantments();
        b.readEnchantments();
        if (a.enchantmentCount != b.enchantmentCount) return b.enchantmentCount - a.enchantmentCount;
        if (a.enchantmentId != b.enchantmentId) return b.enchantmentId - a.enchantmentId;
        if (a.enchantmentLevel != b.enchantmentLevel) return b.enchantmentLevel - a.enchantmentLevel;
        if (a.stack.getItemDamage() != b.stack.getItemDamage()) {
            return a.stack.isItemStackDamageable() ? b.stack.getItemDamage() - a.stack.getItemDamage()
                : a.stack.getItemDamage() - b.stack.getItemDamage();
        }
        return b.stack.stackSize - a.stack.stackSize;
    }

    private static MethodHandle findItemOrder() {
        try {
            // AE2's enclosing helper is private, so a static Mixin invoker cannot legally reference its class.
            Class<?> helper = Class.forName("appeng.util.InvTweakSortingModule$InvTweaks");
            Method method = helper.getDeclaredMethod("getItemOrder", ItemStack.class);
            method.setAccessible(true);
            return MethodHandles.lookup()
                .unreflect(method);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not access AE2 inventory sorting rules", e);
        }
    }

    private static int itemOrder(ItemStack stack) {
        try {
            return (int) ITEM_ORDER.invokeExact(stack);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("could not read AE2 inventory sorting rules", e);
        }
    }

    private static final class SortKey {

        private final ItemStack stack;
        private final int order;
        private final boolean named;
        private String registryName;
        private String displayName;
        private boolean enchantmentsRead;
        private int enchantmentCount;
        private int enchantmentId;
        private int enchantmentLevel;

        private SortKey(IAEStack<?> entry) {
            stack = entry.getItemStackForNEI();
            order = itemOrder(stack);
            named = stack != null && stack.hasDisplayName();
        }

        private String registryName() {
            if (registryName == null) registryName = Item.itemRegistry.getNameForObject(stack.getItem());
            return registryName;
        }

        private String displayName() {
            if (displayName == null) displayName = stack.getDisplayName();
            return displayName;
        }

        @SuppressWarnings("unchecked")
        private void readEnchantments() {
            if (enchantmentsRead) return;
            enchantmentsRead = true;
            Map<Integer, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
            enchantmentCount = enchantments.size();
            for (Map.Entry<Integer, Integer> enchantment : enchantments.entrySet()) {
                int level = enchantment.getValue();
                int id = enchantment.getKey();
                if (level > enchantmentLevel || level == enchantmentLevel && id > enchantmentId) {
                    enchantmentLevel = level;
                    enchantmentId = id;
                }
            }
        }
    }
}
