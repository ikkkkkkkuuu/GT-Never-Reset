package com.xyp.gtneverreset.common.crafting.chaos.processing;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;

/** Proportional power allocation; credential frame capacities are independent of tag costs. */
public final class BottledChaosPowerAllocator {

    private final long[] budgetsBySlot;
    private boolean concentratedPower;
    private boolean activeConcentratedPower;
    private int powerRoundMask;
    private boolean allocationPending = true;

    public BottledChaosPowerAllocator(int groups) {
        budgetsBySlot = new long[groups];
    }

    public boolean concentratedRequested() {
        return concentratedPower;
    }

    public boolean concentrated() {
        return activeConcentratedPower;
    }

    public boolean modeChanging() {
        return concentratedPower != activeConcentratedPower;
    }

    public boolean roundActive() {
        return powerRoundMask != 0;
    }

    public boolean needsRound() {
        return activeConcentratedPower || allocationPending;
    }

    public long budget(int slot) {
        return budgetsBySlot[slot];
    }

    public boolean requestConcentrated(boolean enabled) {
        if (enabled == concentratedPower) return false;
        concentratedPower = enabled;
        return true;
    }

    public boolean waitingForRound(int slot) {
        return (needsRound() || modeChanging()) && (powerRoundMask & (1 << slot)) == 0;
    }

    public void updateSupply(boolean wireless, long power, int[] weights) {
        if (wireless) {
            powerRoundMask = 0;
            allocationPending = false;
            Arrays.fill(budgetsBySlot, 0);
        } else if (!activeConcentratedPower) {
            allocationPending = !Arrays.equals(budgetsBySlot, proportionalBudgets(power, weights));
        }
    }

    public boolean retireFinished(int authorized, int[] weights) {
        int finished = powerRoundMask & ~authorized;
        if (finished == 0) return false;
        powerRoundMask &= authorized;
        for (int slot = 0; slot < weights.length; slot++) {
            if ((finished & (1 << slot)) != 0 && (activeConcentratedPower || weights[slot] == 0))
                budgetsBySlot[slot] = 0;
        }
        return true;
    }

    /** Already-paid jobs keep their original EU/t until a compatible round finishes. */
    public boolean refreshRound(int authorized, long power, int[] weights) {
        if (modeChanging() && roundActive()) return false;
        boolean switched = modeChanging();
        if (switched) {
            activeConcentratedPower = concentratedPower;
            Arrays.fill(budgetsBySlot, 0);
            updateSupply(false, power, weights);
        }
        if (activeConcentratedPower) return switched;
        if ((authorized & ~powerRoundMask) != 0) allocationPending = true;
        if (!allocationPending || roundActive() || authorized != 0) return switched;
        startRound(0, proportionalBudgets(power, weights));
        allocationPending = false;
        return true;
    }

    public void joinRound(int slot) {
        powerRoundMask |= 1 << slot;
    }

    public void startRound(int mask, long[] budgets) {
        powerRoundMask = mask;
        System.arraycopy(budgets, 0, budgetsBySlot, 0, budgetsBySlot.length);
    }

    public void save(NBTTagCompound nbt) {
        nbt.setBoolean("bottledChaosConcentratedPower", concentratedPower);
        nbt.setBoolean("bottledChaosActiveConcentratedPower", activeConcentratedPower);
        nbt.setInteger("bottledChaosPowerRound", powerRoundMask);
        NBTTagCompound budgets = new NBTTagCompound();
        for (int slot = 0; slot < budgetsBySlot.length; slot++) {
            budgets.setLong(Integer.toString(slot), budgetsBySlot[slot]);
        }
        nbt.setTag("bottledChaosRoundBudgets", budgets);
    }

    public void load(NBTTagCompound nbt) {
        concentratedPower = nbt.getBoolean("bottledChaosConcentratedPower");
        activeConcentratedPower = nbt.getBoolean("bottledChaosActiveConcentratedPower");
        powerRoundMask = nbt.getInteger("bottledChaosPowerRound") & ((1 << budgetsBySlot.length) - 1);
        NBTTagCompound budgets = nbt.getCompoundTag("bottledChaosRoundBudgets");
        for (int slot = 0; slot < budgetsBySlot.length; slot++) {
            budgetsBySlot[slot] = Math.max(0, budgets.getLong(Integer.toString(slot)));
        }
        allocationPending = true;
    }

    public static long powerForShare(long power, int share, int totalCapacity) {
        if (power <= 0 || share <= 0 || totalCapacity <= 0 || share > totalCapacity) return 0;
        // The remainder and share are bounded by int; their product fits in long even at maximum power.
        return power / totalCapacity * share + (power % totalCapacity) * share / totalCapacity;
    }

    /** Lower hatch numbers receive indivisible EU/t remainders first; zero-weight slots reserve nothing. */
    public static long[] proportionalBudgets(long power, int[] capacities) {
        int total = 0;
        for (int capacity : capacities) {
            if (capacity < 0) throw new IllegalArgumentException("Negative capacity");
            total = Math.addExact(total, capacity);
        }
        long[] budgets = new long[capacities.length];
        if (power <= 0 || total == 0) return budgets;
        long remaining = power;
        for (int i = 0; i < capacities.length; i++) {
            budgets[i] = powerForShare(power, capacities[i], total);
            remaining -= budgets[i];
        }
        for (int slot = 0; slot < capacities.length; slot++) {
            if (remaining == 0) break;
            if (capacities[slot] == 0) continue;
            budgets[slot]++;
            remaining--;
        }
        return budgets;
    }
}
