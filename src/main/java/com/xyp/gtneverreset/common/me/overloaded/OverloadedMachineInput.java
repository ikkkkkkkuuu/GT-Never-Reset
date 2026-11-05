package com.xyp.gtneverreset.common.me.overloaded;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration;
import com.xyp.gtneverreset.common.machine.basicMachine.ModernFluidMachine;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.util.GTUtility;

/** Inserts AE deliveries using machine slot/tank rules, applying only explicitly stored tool configuration. */
public final class OverloadedMachineInput {

    private OverloadedMachineInput() {}

    /** Keeps other inventories on the provider's ordinary sided insertion path. */
    public static boolean supports(BaseMetaTileEntity tile) {
        return tile.getMetaTileEntity() instanceof MTEBasicMachine machine && machine.allowSelectCircuit()
            && machine instanceof OverloadedMachineConfiguration
            && !machine.isSteampowered()
            && machine.getRecipeMap() != null;
    }

    /** Accepts the whole delivery into machine inputs and persistent overflow, or leaves all state untouched. */
    public static boolean push(BaseMetaTileEntity tile, ICraftingPatternDetails pattern, InventoryCrafting table,
        ForgeDirection side, OverloadedMachineBuffer overflow) {
        PreparedInput prepared = prepare(tile, pattern, table, side, overflow);
        return prepared != null && commit(List.of(prepared));
    }

    /** No recipe, output, voltage or batch-ratio lookup is involved in preparing a delivery. */
    public static PreparedInput prepare(BaseMetaTileEntity tile, ICraftingPatternDetails pattern,
        InventoryCrafting table, ForgeDirection side, OverloadedMachineBuffer overflow) {
        if (!supports(tile) || tile.getWorldObj() == null
            || tile.getWorldObj().isRemote
            || side == null
            || side == ForgeDirection.UNKNOWN
            || pattern == null
            || pattern.isCraftable()
            || !(table instanceof MEInventoryCrafting aeTable)
            || !overflow.isEmpty()) return null;
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        // A pending delivery owns the provider's overflow until it reaches the machine's physical inputs.
        if (((OverloadedMachineConfiguration) machine).gtnr$hasPending()) return null;
        ItemStack encoded = pattern.getPattern();
        NBTTagCompound tag = encoded == null ? null : encoded.getTagCompound();
        boolean hasCircuit = tag != null && tag.hasKey(CircuitPatternCodec.CIRCUIT);
        boolean hasMold = tag != null && tag.hasKey(CircuitPatternCodec.MOLDS);
        int circuit = CircuitPatternCodec.storedCircuit(encoded);
        ItemStack[] molds = CircuitPatternCodec.storedMolds(encoded);
        if (circuit == -2 || molds == null || hasMold && molds.length != 1) return null;
        ItemStack originalMold = OverloadedMachineMolds.get(machine);
        ItemStack selectedMold = hasMold ? molds[0] : originalMold;
        boolean configured = hasCircuit || hasMold;

        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (int slot = 0; slot < aeTable.getSizeInventory(); slot++) {
            IAEStack<?> stack = aeTable.getAEStackInSlot(slot);
            if (stack == null) continue;
            long amount = stack.getStackSize();
            if (amount <= 0 || amount > Integer.MAX_VALUE) return null;
            if (stack instanceof IAEItemStack item) {
                ItemStack copy = item.getItemStack()
                    .copy();
                copy.stackSize = (int) amount;
                items.add(copy);
            } else if (stack instanceof IAEFluidStack liquid) {
                FluidStack copy = liquid.getFluidStack()
                    .copy();
                copy.amount = (int) amount;
                fluids.add(copy);
            } else return null;
        }
        if (items.isEmpty() && fluids.isEmpty()) return null;

        // GT's insertion filters inspect the current inventory. Stage sequential inserts and restore in finally.
        int first = machine.getInputSlot();
        ItemStack[] original = machine.mInventory.clone();
        ItemStack[] staged = machine.mInventory;
        ItemStack[] committed;
        FluidStack[] oldFluids = copyInputFluids(machine);
        List<ItemStack> pendingItems = new ArrayList<>();
        List<FluidStack> insertedFluids = new ArrayList<>();
        List<FluidStack> pendingFluids = new ArrayList<>();
        if (hasCircuit) staged[machine.getCircuitSlot()] = GTUtility.getIntegratedCircuit(circuit);
        if (hasMold) OverloadedMachineMolds.set(machine, selectedMold);
        try {
            for (FluidStack fluid : fluids) {
                int accepted = tile.fill(side, fluid, false);
                if (accepted <= 0 || accepted > fluid.amount) return null;
                FluidStack inserted = fluid.copy();
                inserted.amount = accepted;
                insertedFluids.add(inserted);
                if (machine instanceof ModernFluidMachine modern) {
                    if (modern.fill(inserted, true) != accepted) return null;
                } else {
                    FluidStack existing = machine.getFillableStack();
                    if (existing == null || existing.amount <= 0) machine.setFillableStack(inserted.copy());
                    else {
                        if (!existing.isFluidEqual(inserted) || (long) existing.amount + accepted > Integer.MAX_VALUE)
                            return null;
                        FluidStack combined = existing.copy();
                        combined.amount += accepted;
                        machine.setFillableStack(combined);
                    }
                }
                if (accepted < fluid.amount) {
                    FluidStack pending = fluid.copy();
                    pending.amount -= accepted;
                    pendingFluids.add(pending);
                }
            }
            for (ItemStack item : items) {
                int remaining = item.stackSize;
                boolean allowedSlot = false;
                for (int slot = first; slot < first + machine.mInputSlotCount && remaining > 0; slot++) {
                    if (!tile.canInsertItem(slot, item, side.ordinal())) continue;
                    allowedSlot = true;
                    ItemStack existing = staged[slot];
                    if (existing != null && existing.stackSize > 0 && !OverloadedProviderInventory.same(existing, item))
                        continue;
                    int present = existing == null ? 0 : Math.max(0, existing.stackSize);
                    int moved = Math
                        .min(remaining, Math.min(item.getMaxStackSize(), tile.getInventoryStackLimit()) - present);
                    if (moved <= 0) continue;
                    ItemStack copy = item.copy();
                    copy.stackSize = present + moved;
                    staged[slot] = copy;
                    remaining -= moved;
                }
                if (remaining != 0) {
                    if (!allowedSlot) return null;
                    ItemStack pending = item.copy();
                    pending.stackSize = remaining;
                    pendingItems.add(pending);
                }
            }
            committed = staged.clone();
        } finally {
            System.arraycopy(original, 0, machine.mInventory, 0, original.length);
            if (hasMold) OverloadedMachineMolds.set(machine, originalMold);
            restoreInputFluids(machine, oldFluids);
        }
        return new PreparedInput(
            tile,
            configured,
            selectedMold,
            original,
            committed,
            oldFluids,
            insertedFluids,
            pendingItems,
            pendingFluids,
            side,
            overflow);
    }

    /** Commits freshly prepared deliveries on the same server tick, or restores every attempted fluid fill. */
    public static boolean commit(List<PreparedInput> prepared) {
        if (prepared.isEmpty()) return false;
        Set<BaseMetaTileEntity> targets = new HashSet<>();
        for (PreparedInput input : prepared) if (!targets.add(input.tile)) return false;
        int attempted = 0;
        boolean filled = false;
        try {
            for (PreparedInput input : prepared) {
                attempted++;
                if (!input.fill()) return false;
            }
            filled = true;
        } finally {
            if (!filled) for (int i = 0; i < attempted; i++) prepared.get(i)
                .restoreFluid();
        }
        for (PreparedInput input : prepared) input.commit();
        return true;
    }

    /** Moves accepted overflow into physical machine inputs as processing frees capacity. */
    public static void refill(MTEBasicMachine machine, OverloadedMachineBuffer overflow) {
        if (!(machine.getBaseMetaTileEntity() instanceof BaseMetaTileEntity tile)) return;
        if (overflow.isEmpty() || overflow.getSide() == ForgeDirection.UNKNOWN) return;
        ForgeDirection side = overflow.getSide();
        boolean changed = false;
        for (Iterator<FluidStack> iterator = overflow.getFluids()
            .iterator(); iterator.hasNext();) {
            FluidStack remainingFluid = iterator.next();
            int moved = tile.fill(side, remainingFluid, true);
            if (moved > 0) {
                remainingFluid.amount -= moved;
                if (remainingFluid.amount <= 0) iterator.remove();
                changed = true;
            }
        }
        int first = machine.getInputSlot();
        for (Iterator<ItemStack> iterator = overflow.getItems()
            .iterator(); iterator.hasNext();) {
            ItemStack pending = iterator.next();
            for (int slot = first; slot < first + machine.mInputSlotCount && pending.stackSize > 0; slot++) {
                if (!tile.canInsertItem(slot, pending, side.ordinal())) continue;
                ItemStack existing = machine.getStackInSlot(slot);
                if (existing != null && !OverloadedProviderInventory.same(existing, pending)) continue;
                int present = existing == null ? 0 : Math.max(0, existing.stackSize);
                int moved = Math.min(
                    pending.stackSize,
                    Math.min(pending.getMaxStackSize(), tile.getInventoryStackLimit()) - present);
                if (moved <= 0) continue;
                ItemStack inserted = pending.copy();
                inserted.stackSize = present + moved;
                tile.setInventorySlotContents(slot, inserted);
                pending.stackSize -= moved;
                changed = true;
            }
            if (pending.stackSize <= 0) iterator.remove();
        }
        overflow.clearIfEmpty();
        ((OverloadedMachineConfiguration) machine).gtnr$setPending(!overflow.isEmpty());
        if (changed) tile.markDirty();
    }

    private static FluidStack[] copyInputFluids(MTEBasicMachine machine) {
        if (machine instanceof ModernFluidMachine modern) return modern.copyInputFluids();
        FluidStack fluid = machine.getFillableStack();
        return new FluidStack[] { fluid == null ? null : fluid.copy() };
    }

    private static void restoreInputFluids(MTEBasicMachine machine, FluidStack[] fluids) {
        if (machine instanceof ModernFluidMachine modern) modern.restoreInputFluids(fluids);
        else machine.setFillableStack(fluids[0] == null ? null : fluids[0].copy());
    }

    public static final class PreparedInput {

        private final BaseMetaTileEntity tile;
        private final MTEBasicMachine machine;
        private final boolean configured;
        private final ItemStack mold;
        private final ItemStack[] original;
        private final ItemStack[] committed;
        private final FluidStack[] oldFluids;
        private final List<FluidStack> insertedFluids;
        private final List<ItemStack> pendingItems;
        private final List<FluidStack> pendingFluids;
        private final ForgeDirection side;
        private final OverloadedMachineBuffer overflow;

        private PreparedInput(BaseMetaTileEntity tile, boolean configured, ItemStack mold, ItemStack[] original,
            ItemStack[] committed, FluidStack[] oldFluids, List<FluidStack> insertedFluids,
            List<ItemStack> pendingItems, List<FluidStack> pendingFluids, ForgeDirection side,
            OverloadedMachineBuffer overflow) {
            this.tile = tile;
            this.machine = (MTEBasicMachine) tile.getMetaTileEntity();
            this.configured = configured;
            this.mold = mold;
            this.original = original;
            this.committed = committed;
            this.oldFluids = oldFluids;
            this.insertedFluids = insertedFluids;
            this.pendingItems = pendingItems;
            this.pendingFluids = pendingFluids;
            this.side = side;
            this.overflow = overflow;
        }

        private boolean fill() {
            if (insertedFluids.isEmpty()) return true;
            ItemStack oldCircuit = machine.mInventory[machine.getCircuitSlot()];
            ItemStack oldMold = OverloadedMachineMolds.get(machine);
            machine.mInventory[machine.getCircuitSlot()] = committed[machine.getCircuitSlot()];
            if (configured) OverloadedMachineMolds.set(machine, mold);
            try {
                for (FluidStack fluid : insertedFluids) {
                    if (tile.fill(side, fluid, true) != fluid.amount) return false;
                }
                return true;
            } finally {
                machine.mInventory[machine.getCircuitSlot()] = oldCircuit;
                if (configured) OverloadedMachineMolds.set(machine, oldMold);
            }
        }

        private void restoreFluid() {
            restoreInputFluids(machine, oldFluids);
        }

        private void commit() {
            OverloadedMachineConfiguration configuration = (OverloadedMachineConfiguration) machine;
            if (configured) {
                configuration.gtnr$beginConfiguration(original[machine.getCircuitSlot()]);
                OverloadedMachineMolds.set(machine, mold);
                tile.setInventorySlotContents(machine.getCircuitSlot(), committed[machine.getCircuitSlot()]);
            }
            int first = machine.getInputSlot();
            for (int slot = first; slot < first + machine.mInputSlotCount; slot++) {
                tile.setInventorySlotContents(slot, committed[slot]);
            }
            overflow.replace(pendingItems, pendingFluids, side);
            configuration.gtnr$setPending(!overflow.isEmpty());
        }
    }
}
