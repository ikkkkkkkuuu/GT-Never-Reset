package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ObjLongConsumer;
import java.util.function.ToLongFunction;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.util.GTUtility;

/** Long totals with stable recipe views whose debits are settled before the next buffer access. */
public final class LongCircuitBuffer {

    private static final int MAX_TYPES_PER_CHANNEL = 64;

    private final Buffer<IAEItemStack, ItemStack> items;
    private final Buffer<IAEFluidStack, FluidStack> fluids;

    public LongCircuitBuffer(NBTTagCompound saved, Runnable changed, List<ItemStack> itemViews,
        List<FluidStack> fluidViews) {
        this(saved, changed, itemViews, fluidViews, false);
    }

    LongCircuitBuffer(NBTTagCompound saved, Runnable changed, List<ItemStack> itemViews, List<FluidStack> fluidViews,
        boolean wideFluidViews) {
        items = new Buffer<>(
            IAEItemStack::getItemStack,
            stack -> stack.stackSize,
            (stack, count) -> stack.stackSize = (int) count,
            Integer.MAX_VALUE,
            changed,
            itemViews);
        fluids = new Buffer<>(
            fluid -> wideFluidViews ? new MERecipeFluidStack(fluid.getFluidStack(), fluid.getStackSize(), null)
                : fluid.getFluidStack(),
            MERecipeFluidStack::getAmount,
            MERecipeFluidStack::setAmount,
            wideFluidViews ? Long.MAX_VALUE : Integer.MAX_VALUE,
            changed,
            fluidViews);
        if (saved == null) return;
        if (saved.hasKey("longItems") || saved.hasKey("longFluids")) {
            load(saved.getTagList("longItems", Constants.NBT.TAG_COMPOUND));
            load(saved.getTagList("longFluids", Constants.NBT.TAG_COMPOUND));
        } else {
            NBTTagList legacy = saved.getTagList("inventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < legacy.tagCount(); i++) {
                IAEItemStack item = AEItemStack.create(GTUtility.loadItem(legacy.getCompoundTagAt(i)));
                if (item != null && item.getStackSize() > 0) items.load(item);
            }
            legacy = saved.getTagList("fluidInventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < legacy.tagCount(); i++) {
                IAEFluidStack fluid = AEFluidStack.create(FluidStack.loadFluidStackFromNBT(legacy.getCompoundTagAt(i)));
                if (fluid != null && fluid.getStackSize() > 0) fluids.load(fluid);
            }
        }
    }

    /** Stages both channels together so a late type-limit or long overflow rejects the entire CPU delivery. */
    public boolean insert(MEInventoryCrafting table, ItemStack[] virtualMolds) {
        List<IAEItemStack> stagedItems = items.stacks();
        List<IAEFluidStack> stagedFluids = fluids.stacks();
        for (int i = 0; i < table.getSizeInventory(); i++) {
            IAEStack<?> stack = table.getAEStackInSlot(i);
            if (stack == null || stack.getStackSize() == 0) continue;
            if (stack.getStackSize() < 0 || CircuitPatternCodec.isCircuit(stack)
                || CircuitPatternCodec.isMold(stack, virtualMolds)) return false;
            if (stack instanceof IAEItemStack item) {
                if (!add(stagedItems, item)) return false;
            } else if (stack instanceof IAEFluidStack fluid) {
                if (!add(stagedFluids, fluid)) return false;
            } else return false;
        }
        items.replace(stagedItems);
        fluids.replace(stagedFluids);
        return true;
    }

    public List<ItemStack> itemViews() {
        return items.views();
    }

    public List<FluidStack> fluidViews() {
        return fluids.views();
    }

    public List<IAEItemStack> storedItems() {
        return items.stacks();
    }

    public List<IAEFluidStack> storedFluids() {
        return fluids.stacks();
    }

    public void write(NBTTagCompound tag) {
        tag.setTag("longItems", write(items.stacks()));
        tag.setTag("longFluids", write(fluids.stacks()));
        tag.removeTag("inventory");
        tag.removeTag("fluidInventory");
    }

    /** A network rejection leaves the exact long remainder here; no quantity is narrowed to an item entity. */
    public void refund(AENetworkProxy proxy, BaseActionSource source) throws GridAccessException {
        for (Entry<IAEItemStack, ItemStack> entry : items.entries) {
            refund(
                entry,
                proxy,
                proxy.getStorage()
                    .getItemInventory(),
                source);
        }
        for (Entry<IAEFluidStack, FluidStack> entry : fluids.entries) {
            refund(
                entry,
                proxy,
                proxy.getStorage()
                    .getFluidInventory(),
                source);
        }
    }

    /** Return excess stock first, then withdraw only each target's remaining deficit. */
    public boolean restock(AENetworkProxy proxy, BaseActionSource source, List<IAEItemStack> itemTargets,
        List<IAEFluidStack> fluidTargets) throws GridAccessException {
        var storage = proxy.getStorage();
        var energy = proxy.getEnergy();
        boolean changed = items.restock(energy, storage.getItemInventory(), source, itemTargets);
        return fluids.restock(energy, storage.getFluidInventory(), source, fluidTargets) || changed;
    }

    boolean canRecover(List<IAEItemStack> itemRequests, List<IAEFluidStack> fluidRequests) {
        return items.canRecover(itemRequests) && fluids.canRecover(fluidRequests);
    }

    void recoverItem(IAEItemStack remainder) {
        items.load(remainder);
    }

    void recoverFluid(IAEFluidStack remainder) {
        fluids.load(remainder);
    }

    public void clear() {
        items.clear();
        fluids.clear();
    }

    private void load(NBTTagList list) {
        for (int i = 0; i < list.tagCount(); i++) {
            IAEStack<?> stack = Platform.readStackNBT(list.getCompoundTagAt(i), true);
            if (stack == null || stack.getStackSize() <= 0) continue;
            if (stack instanceof IAEItemStack item) items.load(item);
            else if (stack instanceof IAEFluidStack fluid) fluids.load(fluid);
        }
    }

    private static <T extends IAEStack<T>> boolean add(List<T> stacks, T inserted) {
        for (T stored : stacks) {
            if (!stored.isSameType(inserted)) continue;
            if (inserted.getStackSize() > Long.MAX_VALUE - stored.getStackSize()) return false;
            stored.setStackSize(stored.getStackSize() + inserted.getStackSize());
            return true;
        }
        if (stacks.size() >= MAX_TYPES_PER_CHANNEL) return false;
        stacks.add(inserted.copy());
        return true;
    }

    private static NBTTagList write(List<? extends IAEStack<?>> stacks) {
        NBTTagList list = new NBTTagList();
        for (IAEStack<?> stack : stacks) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBTGeneric(tag);
            list.appendTag(tag);
        }
        return list;
    }

    static long maxPoweredAmount(IAEStack<?> stack) {
        // AE2 ceilDiv adds the divisor before subtracting one.
        return Long.MAX_VALUE - Math.max(1, stack.getAmountPerUnit());
    }

    private static <T extends IAEStack<T>, V> void refund(Entry<T, V> entry, AENetworkProxy proxy,
        IMEMonitor<T> inventory, BaseActionSource source) throws GridAccessException {
        entry.settle();
        long safeRequest = maxPoweredAmount(entry.stored);
        for (int attempt = 0; attempt < 2 && entry.stored.getStackSize() > 0; attempt++) {
            long sent = Math.min(entry.stored.getStackSize(), safeRequest);
            T rest = Platform.poweredInsert(
                proxy.getEnergy(),
                inventory,
                entry.stored.copy()
                    .setStackSize(sent),
                source);
            entry.refunded(sent, rest);
            if (rest != null && rest.getStackSize() > 0) break;
        }
    }

    private static final class Buffer<T extends IAEStack<T>, V> {

        private final List<Entry<T, V>> entries = new ArrayList<>();
        private final List<V> views;
        private final Function<T, V> view;
        private final ToLongFunction<V> count;
        private final ObjLongConsumer<V> setCount;
        private final long viewLimit;
        private final Runnable changed;

        Buffer(Function<T, V> view, ToLongFunction<V> count, ObjLongConsumer<V> setCount, long viewLimit,
            Runnable changed, List<V> views) {
            this.view = view;
            this.count = count;
            this.setCount = setCount;
            this.viewLimit = viewLimit;
            this.changed = changed;
            this.views = views;
        }

        private void settle() {
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry<T, V> entry = entries.get(i);
                entry.settle();
                if (entry.stored.getStackSize() == 0) {
                    entries.remove(i);
                    views.remove(i);
                }
            }
        }

        List<T> stacks() {
            settle();
            List<T> stacks = new ArrayList<>();
            for (Entry<T, V> entry : entries) stacks.add(entry.stored.copy());
            return stacks;
        }

        List<V> views() {
            settle();
            return views;
        }

        boolean restock(IEnergySource energy, IMEMonitor<T> inventory, BaseActionSource source, List<T> targets) {
            settle();
            boolean modified = false;
            for (Entry<T, V> entry : entries) {
                long limit = 0;
                for (T target : targets) {
                    if (entry.stored.isSameType(target)) limit = target.getStackSize();
                }
                long excess = entry.stored.getStackSize() - limit;
                if (excess <= 0) continue;
                long sent = Math.min(excess, maxPoweredAmount(entry.stored));
                T rejected = Platform.poweredInsert(
                    energy,
                    inventory,
                    entry.stored.copy()
                        .setStackSize(sent),
                    source);
                entry.refunded(sent, rejected);
                modified |= rejected == null || rejected.getStackSize() < sent;
            }
            settle();
            for (T target : targets) {
                long missing = target.getStackSize();
                boolean present = false;
                for (Entry<T, V> entry : entries) {
                    if (!entry.stored.isSameType(target)) continue;
                    missing -= entry.stored.getStackSize();
                    present = true;
                    break;
                }
                if (missing <= 0 || (!present && entries.size() >= MAX_TYPES_PER_CHANNEL)) continue;
                T received = Platform.poweredExtraction(
                    energy,
                    inventory,
                    target.copy()
                        .setStackSize(Math.min(missing, maxPoweredAmount(target))),
                    source);
                if (received != null && received.getStackSize() > 0) {
                    load(received);
                    modified = true;
                }
            }
            return modified;
        }

        boolean canRecover(List<T> requests) {
            if (requests.isEmpty() || entries.isEmpty()) return requests.size() <= MAX_TYPES_PER_CHANNEL;
            List<T> staged = stacks();
            for (T request : requests) if (!add(staged, request)) return false;
            return true;
        }

        void load(T stack) {
            List<T> staged = stacks();
            if (!add(staged, stack)) throw new IllegalArgumentException("Circuit buffer save exceeds capacity");
            replace(staged);
        }

        void replace(List<T> stacks) {
            // Staging preserves existing entry order and only appends new types.
            int existing = entries.size();
            for (int i = 0; i < existing; i++) {
                Entry<T, V> entry = entries.get(i);
                entry.stored.setStackSize(
                    stacks.get(i)
                        .getStackSize());
                entry.refresh();
            }
            for (int i = existing; i < stacks.size(); i++) {
                Entry<T, V> entry = new Entry<>(
                    stacks.get(i)
                        .copy(),
                    this);
                entries.add(entry);
                views.add(entry.view);
            }
            changed.run();
        }

        void clear() {
            entries.clear();
            views.clear();
        }
    }

    private static final class Entry<T extends IAEStack<T>, V> {

        private final T stored;
        private final V view;
        private final Buffer<T, V> owner;
        private long exposed;

        Entry(T stored, Buffer<T, V> owner) {
            this.stored = stored;
            this.owner = owner;
            view = owner.view.apply(
                stored.copy()
                    .setStackSize(1));
            refresh();
        }

        void refresh() {
            exposed = Math.min(owner.viewLimit, stored.getStackSize());
            owner.setCount.accept(view, exposed);
        }

        void settle() {
            long consumed = exposed - Math.max(0, owner.count.applyAsLong(view));
            if (consumed > 0) {
                stored.setStackSize(stored.getStackSize() - consumed);
                owner.changed.run();
            }
            refresh();
        }

        void refunded(long sent, T rest) {
            long before = stored.getStackSize();
            long rejected = rest == null ? 0 : Math.max(0, Math.min(sent, rest.getStackSize()));
            stored.setStackSize(before - (sent - rejected));
            refresh();
            if (stored.getStackSize() != before) owner.changed.run();
        }
    }
}
