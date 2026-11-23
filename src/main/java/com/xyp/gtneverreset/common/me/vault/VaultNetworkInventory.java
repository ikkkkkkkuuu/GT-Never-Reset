package com.xyp.gtneverreset.common.me.vault;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.item.Item;

import com.xyp.gtneverreset.compat.ae2.TerminalInventorySort;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.registry.LanguageRegistry;

/** One viewer's sorted snapshot of the selected ME channel. */
public final class VaultNetworkInventory {

    private static final Comparator<IAEStack<?>> AMOUNT_ORDER = Comparator
        .comparingLong((IAEStack<?> stack) -> stack.getStackSize())
        .reversed();
    private static final Comparator<IAEStack<?>> FLUID_ORDER = Comparator.comparing(
        stack -> ((IAEFluidStack) stack).getFluid()
            .getName(),
        String.CASE_INSENSITIVE_ORDER);

    private final Map<IAEStack<?>, String> searchNames = new IdentityHashMap<>();
    private List<IAEStack<?>> inventory = new ArrayList<>();
    private List<IAEStack<?>> filtered = List.of();
    private InventoryListener<?> listener;
    private boolean inventoryDirty;
    private boolean orderDirty;
    private boolean filterDirty;
    private String query = "";
    private String language = "en_US";

    public boolean refresh(IMEMonitor<?> monitor, boolean poll) {
        boolean changed = monitor != (listener == null ? null : listener.monitor);
        if (changed) {
            stopObserving();
            if (monitor != null) observe(monitor);
        }
        if (!changed && (!poll || !inventoryDirty)) return false;

        inventoryDirty = false;
        searchNames.clear();
        inventory.clear();
        if (monitor != null) {
            for (Object entry : monitor.getStorageList()) {
                IAEStack<?> stack = (IAEStack<?>) entry;
                if (stack.getStackSize() > 0) inventory.add(stack.copy());
            }
        }
        orderDirty = !inventory.isEmpty();
        filterDirty = true;
        return true;
    }

    public boolean setSearch(String text, String locale) {
        String normalized = text.trim()
            .toLowerCase(Locale.ROOT);
        if (query.equals(normalized) && language.equals(locale)) return false;
        if (!language.equals(locale)) searchNames.clear();
        query = normalized;
        language = locale;
        filterDirty = true;
        return true;
    }

    public void invalidateOrder() {
        orderDirty = true;
    }

    public boolean isFiltering() {
        return !query.isEmpty();
    }

    public List<IAEStack<?>> entries(boolean fluids, boolean byAmount) {
        if (orderDirty) {
            if (!inventory.isEmpty()) {
                inventory.sort(byAmount ? AMOUNT_ORDER : fluids ? FLUID_ORDER : TerminalInventorySort.bundled());
            }
            orderDirty = false;
            filterDirty = true;
        }
        if (filterDirty) {
            filtered = isFiltering() ? inventory.stream()
                .filter(this::matches)
                .toList() : inventory;
            filterDirty = false;
        }
        return filtered;
    }

    public void stopObserving() {
        if (listener != null) {
            listener.monitor.removeListener(listener);
            listener = null;
        }
    }

    private <T extends IAEStack<T>> void observe(IMEMonitor<T> monitor) {
        var observer = new InventoryListener<>(monitor, () -> inventoryDirty = true);
        listener = observer;
        monitor.addListener(observer, monitor);
    }

    private boolean matches(IAEStack<?> stack) {
        return searchNames.computeIfAbsent(stack, this::searchName)
            .contains(query);
    }

    private String searchName(IAEStack<?> stack) {
        String id;
        String key;
        if (stack instanceof IAEItemStack item) {
            id = Item.itemRegistry.getNameForObject(item.getItem());
            key = item.getItemStack()
                .getUnlocalizedName() + ".name";
        } else if (stack instanceof IAEFluidStack fluid) {
            id = fluid.getFluid()
                .getName();
            key = fluid.getUnlocalizedName();
        } else {
            return "";
        }
        String translated = LanguageRegistry.instance()
            .getStringLocalization(key, language);
        return (stack.getDisplayName() + "\n" + id + "\n" + translated).toLowerCase(Locale.ROOT);
    }

    private record InventoryListener<T extends IAEStack<T>> (IMEMonitor<T> monitor, Runnable invalidate)
        implements IMEMonitorHandlerReceiver<T> {

        @Override
        public boolean isValid(Object token) {
            return token == monitor;
        }

        @Override
        public void postChange(IBaseMonitor<T> source, Iterable<T> changes, BaseActionSource actionSource) {
            invalidate.run();
        }

        @Override
        public void onListUpdate() {
            invalidate.run();
        }
    }
}
