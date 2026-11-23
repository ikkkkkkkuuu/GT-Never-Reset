package com.xyp.gtneverreset.common.me.vault;

import java.util.function.Supplier;

import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

public final class VaultInventoryHistory {

    public static final int WINDOW_SECONDS = 300;
    private static final int SAMPLE_TICKS = 20;

    private final IStorageGrid source;
    private final IAEStack<?> target;
    private final long startTick;
    private long nextSample;
    private Sample sample = Sample.WAITING;

    public VaultInventoryHistory(IStorageGrid source, IAEStack<?> target, long startTick) {
        this.source = source;
        this.target = target;
        this.startTick = startTick;
        this.nextSample = startTick;
    }

    public Sample sample(long tick, Supplier<IStorageGrid> authorizedStorage) {
        if (tick < nextSample) return sample;
        nextSample = tick + SAMPLE_TICKS;
        long elapsed = Math.max(0, (tick - startTick) / SAMPLE_TICKS);
        if (authorizedStorage.get() != source) {
            sample = new Sample(elapsed, 0, false);
            return sample;
        }

        IAEStack<?> stock = target instanceof IAEFluidStack fluid ? source.getFluidInventory()
            .getStorageList()
            .findPrecise(fluid)
            : source.getItemInventory()
                .getStorageList()
                .findPrecise((IAEItemStack) target);
        sample = new Sample(elapsed, stock == null ? 0 : Math.max(0, stock.getStackSize()), true);
        return sample;
    }

    public record Sample(long seconds, long amount, boolean available) {

        public static final Sample WAITING = new Sample(-1, 0, false);
    }
}
