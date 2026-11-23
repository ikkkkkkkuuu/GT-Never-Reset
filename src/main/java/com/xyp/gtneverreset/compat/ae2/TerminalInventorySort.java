package com.xyp.gtneverreset.compat.ae2;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.GTNeverReset;

import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class TerminalInventorySort implements Comparator<IAEStack<?>> {

    public static final String DEFAULT_TREE = "/assets/gtneverreset/terminal/InvTweaksTree.txt";
    private static final Comparator<String> REGISTRY_NAMES = Comparator.nullsFirst(Comparator.naturalOrder());
    private final Map<IAEStack<?>, SortKey> keys = new IdentityHashMap<>();
    private final ToIntFunction<ItemStack> itemOrder;
    private final int direction;

    /** Each comparator belongs to one sort invocation; mutable quantities and rules never survive a refresh. */
    @SideOnly(Side.CLIENT)
    public TerminalInventorySort(int direction) {
        this(direction, stack -> lookupOrder(ClientOrder.METHOD, stack));
    }

    private TerminalInventorySort(int direction, ToIntFunction<ItemStack> itemOrder) {
        this.direction = direction;
        this.itemOrder = itemOrder;
    }

    public static TerminalInventorySort bundled() {
        return new TerminalInventorySort(1, BundledOrder.RANK);
    }

    @Override
    public int compare(IAEStack<?> first, IAEStack<?> second) {
        SortKey a = keys.computeIfAbsent(first, stack -> new SortKey(stack, itemOrder));
        SortKey b = keys.computeIfAbsent(second, stack -> new SortKey(stack, itemOrder));
        return compareKeys(a, b) * direction;
    }

    private static int compareKeys(SortKey a, SortKey b) {
        if (a.stack == null) return b.stack == null ? 0 : 1;
        if (b.stack == null) return -1;
        if (a.order != b.order) return Integer.compare(a.order, b.order);
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
        return Long.compare(b.amount, a.amount);
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

    private static int lookupOrder(MethodHandle method, ItemStack stack) {
        try {
            return (int) method.invokeExact(stack);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("could not read AE2 inventory sorting rules", e);
        }
    }

    @SideOnly(Side.CLIENT)
    private static final class ClientOrder {

        private static final MethodHandle METHOD = findItemOrder();
    }

    private static final class BundledOrder {

        private static final ToIntFunction<ItemStack> RANK = load();

        private static ToIntFunction<ItemStack> load() {
            try (InputStream source = TerminalInventorySort.class.getResourceAsStream(DEFAULT_TREE)) {
                if (source == null) throw new IOException("missing bundled sorting tree");
                // The nested loader parses the same rules without initializing AE2's client-only file paths.
                Class<?> loader = Class.forName("appeng.util.InvTweakSortingModule$InvTweaksItemTreeLoader");
                Method load = loader.getDeclaredMethod("load", java.io.File.class);
                load.setAccessible(true);
                Path file = Files.createTempFile("gtnr-terminal-sort-", ".xml");
                Object tree;
                try {
                    Files.copy(source, file, StandardCopyOption.REPLACE_EXISTING);
                    tree = load.invoke(null, file.toFile());
                } finally {
                    Files.deleteIfExists(file);
                }
                Method rank = tree.getClass()
                    .getDeclaredMethod("getItemOrder", String.class, int.class);
                rank.setAccessible(true);
                MethodHandle method = MethodHandles.lookup()
                    .unreflect(rank)
                    .bindTo(tree);
                return stack -> rank(method, stack);
            } catch (IOException | ReflectiveOperationException | RuntimeException | LinkageError error) {
                GTNeverReset.LOG.warn("could not load bundled terminal sorting rules", error);
                return stack -> 0;
            }
        }

        private static int rank(MethodHandle method, ItemStack stack) {
            try {
                return (int) method
                    .invokeExact(Item.itemRegistry.getNameForObject(stack.getItem()), stack.getItemDamage());
            } catch (RuntimeException | Error error) {
                throw error;
            } catch (Throwable error) {
                throw new IllegalStateException("could not read bundled terminal sorting rules", error);
            }
        }
    }

    private static final class SortKey {

        private final ItemStack stack;
        private final long amount;
        private final int order;
        private final boolean named;
        private String registryName;
        private String displayName;
        private boolean enchantmentsRead;
        private int enchantmentCount;
        private int enchantmentId;
        private int enchantmentLevel;

        private SortKey(IAEStack<?> entry, ToIntFunction<ItemStack> itemOrder) {
            stack = entry.getItemStackForNEI();
            amount = entry.getStackSize();
            order = stack == null ? Integer.MAX_VALUE : itemOrder.applyAsInt(stack);
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
