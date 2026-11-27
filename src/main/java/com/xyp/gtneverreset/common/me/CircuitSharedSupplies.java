package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;

/** Shared filters, physical reserves and the transient ME offers for one controller recipe check. */
public final class CircuitSharedSupplies {

    public static final int SLOTS_PER_CHANNEL = 16;
    public static final int MAX_INTERVAL = 86400;
    private static final int DEFAULT_INTERVAL = 5;
    private static final String KEY = "circuitSharedSupplies";
    private final Runnable changed;
    private final Runnable configurationChanged;
    private final NetworkInputs<IAEItemStack, ItemStack> networkItems = new NetworkInputs<>(
        IAEItemStack::getItemStack,
        stack -> stack.stackSize);
    private final NetworkInputs<IAEFluidStack, FluidStack> networkFluids = new NetworkInputs<>(
        fluid -> fluid.getStackSize() > Integer.MAX_VALUE
            ? new MERecipeFluidStack(fluid.getFluidStack(), fluid.getStackSize(), null)
            : fluid.getFluidStack(),
        MERecipeFluidStack::getAmount);
    private IGrid offeredGrid;
    private boolean bufferedItems;
    private boolean bufferedFluids;
    private boolean checkingNetwork, queriedNetwork;
    private final IAEStack<?>[] targets = new IAEStack<?>[2 * SLOTS_PER_CHANNEL];
    private final List<ItemStack> items = new ArrayList<>();
    private final List<FluidStack> fluids = new ArrayList<>();
    private LongCircuitBuffer stock;
    private int interval = DEFAULT_INTERVAL;
    private int remainingTicks = interval * 20;
    private boolean onRecipeCheck;
    private long lastPullTick = Long.MIN_VALUE;
    private boolean exposed;

    public CircuitSharedSupplies(Runnable changed, Runnable configurationChanged) {
        this.changed = changed;
        this.configurationChanged = configurationChanged;
        stock = new LongCircuitBuffer(null, changed, items, fluids, true);
    }

    public IAEStack<?> target(int slot) {
        return targets[slot] == null ? null : targets[slot].copy();
    }

    public void setTarget(int slot, IAEStack<?> target) {
        if (!replaceTarget(slot, target)) return;
        rebuildTargets();
        configurationChanged.run();
        changed.run();
    }

    private boolean replaceTarget(int slot, IAEStack<?> target) {
        if (target != null) {
            if (target.isItem() != (slot < SLOTS_PER_CHANNEL)) return false;
            for (int i = 0; i < targets.length; i++) {
                if (i != slot && targets[i] != null && targets[i].isSameType(target)) return false;
            }
            long limit = target.isItem() ? Integer.MAX_VALUE : LongCircuitBuffer.maxPoweredAmount(target);
            target = target.copy()
                .setStackSize(Math.max(1, Math.min(limit, target.getStackSize())));
        }
        targets[slot] = target;
        return true;
    }

    private void rebuildTargets() {
        networkItems.targets.clear();
        networkFluids.targets.clear();
        for (IAEStack<?> target : targets) {
            if (target instanceof IAEItemStack item) networkItems.targets.add(item);
            else if (target instanceof IAEFluidStack fluid) networkFluids.targets.add(fluid);
        }
    }

    public boolean directItems() {
        return !bufferedItems;
    }

    public void setDirectItems(boolean direct) {
        bufferedItems = !direct;
        configurationChanged.run();
        changed.run();
    }

    public boolean directFluids() {
        return !bufferedFluids;
    }

    public void setDirectFluids(boolean direct) {
        bufferedFluids = !direct;
        configurationChanged.run();
        changed.run();
    }

    public void watchNetworkInputs(IStackWatcher watcher) {
        watcher.clear();
        if (directItems()) for (var item : networkItems.targets) watcher.add(item);
        if (directFluids()) for (var fluid : networkFluids.targets) watcher.add(fluid);
    }

    public int interval() {
        return interval;
    }

    public void setInterval(int seconds) {
        interval = Math.max(0, Math.min(MAX_INTERVAL, seconds));
        remainingTicks = interval * 20;
        changed.run();
    }

    public int remainingSeconds() {
        return hasWork() ? (remainingTicks + 19) / 20 : 0;
    }

    public boolean onRecipeCheck() {
        return onRecipeCheck;
    }

    public void setOnRecipeCheck(boolean enabled) {
        onRecipeCheck = enabled;
        changed.run();
    }

    public boolean tick() {
        if (exposed) {
            stock.itemViews();
            stock.fluidViews();
            exposed = false;
        }
        if (!hasWork() || interval == 0 || --remainingTicks > 0) return false;
        remainingTicks = interval * 20;
        return true;
    }

    public boolean hasWork() {
        return (bufferedItems && !networkItems.targets.isEmpty())
            || (bufferedFluids && !networkFluids.targets.isEmpty())
            || !items.isEmpty()
            || !fluids.isEmpty();
    }

    public boolean pull(AENetworkProxy proxy, BaseActionSource source, long tick) throws GridAccessException {
        if (lastPullTick == tick) return false;
        lastPullTick = tick;
        return stock.restock(
            proxy,
            source,
            bufferedItems ? networkItems.targets : Collections.emptyList(),
            bufferedFluids ? networkFluids.targets : Collections.emptyList());
    }

    public List<ItemStack> itemInputs() {
        if (!items.isEmpty()) {
            exposed = true;
            stock.itemViews();
        }
        return items;
    }

    public List<FluidStack> fluidInputs() {
        if (!fluids.isEmpty()) {
            exposed = true;
            stock.fluidViews();
        }
        return fluids;
    }

    public boolean isCheckingNetwork() {
        return checkingNetwork;
    }

    public boolean beginNetworkCheck() {
        if (checkingNetwork || ((bufferedItems || networkItems.targets.isEmpty())
            && (bufferedFluids || networkFluids.targets.isEmpty()))) return false;
        checkingNetwork = true;
        return true;
    }

    public List<ItemStack> networkItemInputs(AENetworkProxy proxy, BaseActionSource source) {
        prepareNetworkInputs(proxy, source);
        return networkItems.views;
    }

    public List<FluidStack> networkFluidInputs(AENetworkProxy proxy, BaseActionSource source) {
        prepareNetworkInputs(proxy, source);
        return networkFluids.views;
    }

    /** Every slot and mirror in the check sees the same mutable stacks from both channels. */
    private void prepareNetworkInputs(AENetworkProxy proxy, BaseActionSource source) {
        if (!checkingNetwork || queriedNetwork) return;
        queriedNetwork = true;
        try {
            offeredGrid = proxy.getGrid();
            var storage = proxy.getStorage();
            if (directItems() && !networkItems.targets.isEmpty()) networkItems.offer(
                storage.getItemInventory(),
                items.isEmpty() ? Collections.emptyList() : stock.storedItems(),
                source);
            if (directFluids() && !networkFluids.targets.isEmpty()) networkFluids.offer(
                storage.getFluidInventory(),
                fluids.isEmpty() ? Collections.emptyList() : stock.storedFluids(),
                source);
        } catch (GridAccessException ignored) {}
    }

    public boolean endNetworkCheck(AENetworkProxy proxy, BaseActionSource source) {
        try {
            var itemRequests = networkItems.debits();
            var fluidRequests = networkFluids.debits();
            if (itemRequests.isEmpty() && fluidRequests.isEmpty()) return true;
            if (!proxy.isActive() || proxy.getGrid() != offeredGrid || !stock.canRecover(itemRequests, fluidRequests))
                return false;
            var storage = proxy.getStorage();
            var energy = proxy.getEnergy();
            var itemMonitor = storage.getItemInventory();
            var fluidMonitor = storage.getFluidInventory();
            if (!canExtract(energy, itemMonitor, itemRequests, source)
                || !canExtract(energy, fluidMonitor, fluidRequests, source)) return false;
            var paidItems = new ArrayList<IAEItemStack>(itemRequests.size());
            var paidFluids = new ArrayList<IAEFluidStack>(fluidRequests.size());
            if (extract(energy, itemMonitor, itemRequests, source, paidItems)
                && extract(energy, fluidMonitor, fluidRequests, source, paidFluids)) return true;
            returnPayment(itemMonitor, paidItems, source, stock::recoverItem);
            returnPayment(fluidMonitor, paidFluids, source, stock::recoverFluid);
            return false;
        } catch (GridAccessException ignored) {
            return false;
        } finally {
            networkItems.clearOffer();
            networkFluids.clearOffer();
            offeredGrid = null;
            checkingNetwork = false;
            queriedNetwork = false;
        }
    }

    private static <T extends IAEStack<T>> boolean canExtract(IEnergySource energy, IMEMonitor<T> monitor,
        List<T> requests, BaseActionSource source) {
        for (T request : requests) {
            T available = Platform.poweredExtraction(energy, monitor, request, source, Actionable.SIMULATE);
            if (available == null || available.getStackSize() != request.getStackSize()) return false;
        }
        return true;
    }

    private static <T extends IAEStack<T>> boolean extract(IEnergySource energy, IMEMonitor<T> monitor,
        List<T> requests, BaseActionSource source, List<T> paid) {
        for (T request : requests) {
            T taken = Platform.poweredExtraction(energy, monitor, request, source);
            if (taken != null && taken.getStackSize() > 0) paid.add(taken);
            if (taken == null || taken.getStackSize() != request.getStackSize()) return false;
        }
        return true;
    }

    private static <T extends IAEStack<T>> void returnPayment(IMEMonitor<T> monitor, List<T> paid,
        BaseActionSource source, Consumer<T> recover) {
        for (T part : paid) {
            T rest = monitor.injectItems(part, Actionable.MODULATE, source);
            if (rest != null && rest.getStackSize() > 0) recover.accept(rest);
        }
    }

    public void refund(AENetworkProxy proxy, BaseActionSource source) throws GridAccessException {
        stock.refund(proxy, source);
    }

    public void clearStock() {
        stock.clear();
    }

    public void write(NBTTagCompound tag) {
        if (networkItems.targets.isEmpty() && !bufferedItems
            && networkFluids.targets.isEmpty()
            && !bufferedFluids
            && !hasWork()
            && interval == DEFAULT_INTERVAL
            && !onRecipeCheck) {
            tag.removeTag(KEY);
            return;
        }
        NBTTagCompound saved = new NBTTagCompound();
        NBTTagList filters = new NBTTagList();
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            targets[i].writeToNBTGeneric(entry);
            entry.setInteger("slot", i);
            filters.appendTag(entry);
        }
        saved.setTag("targets", filters);
        saved.setBoolean("bufferedItems", bufferedItems);
        saved.setBoolean("bufferedFluids", bufferedFluids);
        saved.setInteger("interval", interval);
        saved.setBoolean("onRecipeCheck", onRecipeCheck);
        stock.write(saved);
        tag.setTag(KEY, saved);
    }

    public void read(NBTTagCompound tag) {
        NBTTagCompound saved = tag.getCompoundTag(KEY);
        Arrays.fill(targets, null);
        bufferedItems = saved.getBoolean("bufferedItems");
        bufferedFluids = saved.getBoolean("bufferedFluids");
        NBTTagList filters = saved.getTagList("targets", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < filters.tagCount(); i++) {
            NBTTagCompound entry = filters.getCompoundTagAt(i);
            int slot = entry.getInteger("slot");
            if (slot >= 0 && slot < targets.length) replaceTarget(slot, Platform.readStackNBT(entry, true));
        }
        rebuildTargets();
        interval = saved.hasKey("interval") ? Math.max(0, Math.min(MAX_INTERVAL, saved.getInteger("interval")))
            : DEFAULT_INTERVAL;
        remainingTicks = interval * 20;
        onRecipeCheck = saved.getBoolean("onRecipeCheck");
        lastPullTick = Long.MIN_VALUE;
        exposed = false;
        stock.clear();
        stock = new LongCircuitBuffer(saved, changed, items, fluids, true);
        configurationChanged.run();
    }

    private static final class NetworkInputs<T extends IAEStack<T>, V> {

        private final List<T> targets = new ArrayList<>();
        private final List<T> original = new ArrayList<>();
        private final List<V> views = new ArrayList<>();
        private final Function<T, V> toView;
        private final ToLongFunction<V> count;

        NetworkInputs(Function<T, V> toView, ToLongFunction<V> count) {
            this.toView = toView;
            this.count = count;
        }

        void offer(IMEMonitor<T> monitor, List<T> localStock, BaseActionSource source) {
            for (T target : targets) {
                long limit = target.getStackSize();
                for (T local : localStock) {
                    if (target.isSameType(local)) limit -= local.getStackSize();
                }
                if (limit <= 0) continue;
                T available = monitor.extractItems(
                    target.copy()
                        .setStackSize(limit),
                    Actionable.SIMULATE,
                    source);
                if (available != null && available.getStackSize() > 0) {
                    original.add(available);
                    views.add(toView.apply(available));
                }
            }
        }

        List<T> debits() {
            if (views.isEmpty()) return Collections.emptyList();
            var result = new ArrayList<T>();
            for (int i = 0; i < views.size(); i++) {
                T offered = original.get(i);
                long used = offered.getStackSize() - Math.max(0, count.applyAsLong(views.get(i)));
                if (used > 0) result.add(
                    offered.copy()
                        .setStackSize(used));
            }
            return result;
        }

        void clearOffer() {
            original.clear();
            views.clear();
        }
    }
}
