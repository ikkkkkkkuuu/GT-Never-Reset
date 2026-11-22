package com.xyp.gtneverreset.common.me;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;

/** One physical reserve per hatch. Pattern inventories expose the same live stacks to GregTech. */
public final class CircuitSharedSupplies {

    public static final int SLOTS_PER_CHANNEL = 16;
    public static final int MAX_INTERVAL = 86400;
    private static final int DEFAULT_INTERVAL = 5;
    private static final String KEY = "circuitSharedSupplies";
    private final Runnable changed;
    private final IAEStack<?>[] targets = new IAEStack<?>[2 * SLOTS_PER_CHANNEL];
    private final List<ItemStack> items = new ArrayList<>();
    private final List<FluidStack> fluids = new ArrayList<>();
    private LongCircuitBuffer stock;
    private int interval = DEFAULT_INTERVAL;
    private int remainingTicks = interval * 20;
    private boolean onRecipeCheck;
    private long lastPullTick = Long.MIN_VALUE;
    private boolean configured;
    private boolean exposed;

    public CircuitSharedSupplies(Runnable changed) {
        this.changed = changed;
        stock = new LongCircuitBuffer(null, changed, items, fluids);
    }

    public IAEStack<?> target(int slot) {
        return targets[slot] == null ? null : targets[slot].copy();
    }

    public void setTarget(int slot, IAEStack<?> target) {
        if (target != null) {
            if (target.isItem() != (slot < SLOTS_PER_CHANNEL)) return;
            for (int i = 0; i < targets.length; i++) {
                if (i != slot && targets[i] != null && targets[i].isSameType(target)) return;
            }
            target = target.copy()
                .setStackSize(Math.max(1, Math.min(Integer.MAX_VALUE, target.getStackSize())));
        }
        targets[slot] = target;
        configured = Arrays.stream(targets)
            .anyMatch(t -> t != null);
        changed.run();
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
        return (remainingTicks + 19) / 20;
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
        return configured || !items.isEmpty() || !fluids.isEmpty();
    }

    public boolean pull(AENetworkProxy proxy, BaseActionSource source, long tick) throws GridAccessException {
        if (lastPullTick == tick) return false;
        lastPullTick = tick;
        List<IAEItemStack> itemTargets = new ArrayList<>();
        List<IAEFluidStack> fluidTargets = new ArrayList<>();
        for (IAEStack<?> target : targets) {
            if (target instanceof IAEItemStack item) itemTargets.add(item);
            else if (target instanceof IAEFluidStack fluid) fluidTargets.add(fluid);
        }
        return stock.restock(proxy, source, itemTargets, fluidTargets);
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

    public void refund(AENetworkProxy proxy, BaseActionSource source) throws GridAccessException {
        stock.refund(proxy, source);
    }

    public void clearStock() {
        stock.clear();
    }

    public void write(NBTTagCompound tag) {
        if (!hasWork() && interval == DEFAULT_INTERVAL && !onRecipeCheck) {
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
        saved.setInteger("interval", interval);
        saved.setBoolean("onRecipeCheck", onRecipeCheck);
        stock.write(saved);
        tag.setTag(KEY, saved);
    }

    public void read(NBTTagCompound tag) {
        NBTTagCompound saved = tag.getCompoundTag(KEY);
        Arrays.fill(targets, null);
        configured = false;
        NBTTagList filters = saved.getTagList("targets", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < filters.tagCount(); i++) {
            NBTTagCompound entry = filters.getCompoundTagAt(i);
            int slot = entry.getInteger("slot");
            if (slot >= 0 && slot < targets.length) setTarget(slot, Platform.readStackNBT(entry, true));
        }
        interval = saved.hasKey("interval") ? Math.max(0, Math.min(MAX_INTERVAL, saved.getInteger("interval")))
            : DEFAULT_INTERVAL;
        remainingTicks = interval * 20;
        onRecipeCheck = saved.getBoolean("onRecipeCheck");
        lastPullTick = Long.MIN_VALUE;
        exposed = false;
        stock.clear();
        stock = new LongCircuitBuffer(saved, changed, items, fluids);
    }
}
