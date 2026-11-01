package com.xyp.gtneverreset.common.terminal;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import it.unimi.dsi.fastutil.objects.ObjectLongPair;

public final class TerminalFluidTransactions {

    private final InventoryPlayer playerInventory;
    private final IMEMonitor<IAEItemStack> itemMonitor;
    private final IMEMonitor<IAEFluidStack> fluidMonitor;
    private final IEnergySource powerSource;
    private final BaseActionSource actionSource;

    public TerminalFluidTransactions(InventoryPlayer playerInventory, IMEMonitor<IAEItemStack> itemMonitor,
        IMEMonitor<IAEFluidStack> fluidMonitor, IEnergySource powerSource, BaseActionSource actionSource) {
        this.playerInventory = playerInventory;
        this.itemMonitor = itemMonitor;
        this.fluidMonitor = fluidMonitor;
        this.powerSource = powerSource;
        this.actionSource = actionSource;
    }

    public boolean fill(IAEFluidStack fluid, boolean fullStack) {
        if (fluid == null || itemMonitor == null || fluidMonitor == null || powerSource == null) return false;

        EmptyFluidContainer empty = findEmptyFluidContainer(fluid);
        if (empty == null) return false;

        IAEFluidStack full = fluid.copy();
        full.setStackSize(empty.capacity);
        ObjectLongPair<ItemStack> preview = fluid.getStackType()
            .fillContainer(empty.item.copy(), full);
        if (!isCompletelyFilled(preview, empty.capacity)) return false;
        int count = fullStack ? Math.min(
            64,
            Math.min(
                empty.item.getMaxStackSize(),
                preview.left()
                    .getMaxStackSize()))
            : 1;
        for (int index = 0; index < count; index++) {
            if (!fillFluidContainer(fluid, empty)) break;
        }
        return true;
    }

    /**
     * Fills one container of the type selected at the start of the server-side batch. Each iteration checks available
     * fluid, empty containers and player inventory space before extraction, restoring extracted resources on failure.
     *
     * @param fluid selected fluid type
     * @param empty fixed container type and capacity for this batch
     * @return true if one filled container was delivered; false to stop the batch
     */
    private boolean fillFluidContainer(IAEFluidStack fluid, EmptyFluidContainer empty) {
        IAEFluidStack fluidRequest = fluid.copy();
        fluidRequest.setStackSize(empty.capacity);
        IAEFluidStack availableFluid = fluidMonitor.extractItems(fluidRequest, Actionable.SIMULATE, actionSource);
        if (availableFluid == null || availableFluid.getStackSize() < empty.capacity) return false;

        IAEItemStack emptyRequest = empty.stack.copy();
        emptyRequest.setStackSize(1);
        IAEItemStack availableEmpty = itemMonitor.extractItems(emptyRequest, Actionable.SIMULATE, actionSource);
        if (availableEmpty == null || availableEmpty.getStackSize() < 1) return false;

        ObjectLongPair<ItemStack> preview = fluid.getStackType()
            .fillContainer(empty.item.copy(), fluidRequest);
        if (!isCompletelyFilled(preview, empty.capacity) || !canAcceptFilledContainer(preview.left())) return false;

        IAEItemStack extractedEmpty = Platform.poweredExtraction(powerSource, itemMonitor, emptyRequest, actionSource);
        if (extractedEmpty == null || extractedEmpty.getStackSize() < 1) return false;

        IAEFluidStack extractedFluid = Platform
            .poweredExtraction(powerSource, fluidMonitor, fluidRequest, actionSource);
        if (extractedFluid == null || extractedFluid.getStackSize() < empty.capacity) {
            restoreItem(extractedEmpty);
            restoreFluid(extractedFluid);
            return false;
        }

        ItemStack extractedItem = extractedEmpty.getItemStack();
        extractedItem.stackSize = 1;
        ObjectLongPair<ItemStack> filled = fluid.getStackType()
            .fillContainer(extractedItem, extractedFluid);
        if (!isCompletelyFilled(filled, empty.capacity)) {
            restoreItem(extractedEmpty);
            restoreFluid(extractedFluid);
            return false;
        }

        giveFilledContainer(filled.left());
        return true;
    }

    public boolean store(IAEFluidStack fluid, boolean fullStack) {
        if (fluid == null || itemMonitor == null || fluidMonitor == null || powerSource == null) return false;

        FilledFluidContainer filled = findFilledFluidContainer(fluid);
        if (filled == null) return false;

        ItemStack source = playerInventory.mainInventory[filled.inventorySlot];
        int count = fullStack ? Math.min(64, Math.min(source.stackSize, source.getMaxStackSize())) : 1;
        for (int index = 0; index < count; index++) {
            if (!storeFluidContainer(filled)) break;
        }
        return true;
    }

    /**
     * Drains one container from the inventory slot selected at the start of the server-side batch. The empty container
     * and fluid both enter the network; rejected insertions are rolled back before the source stack is consumed.
     *
     * @param filled selected inventory slot, container identity and contents
     * @return true if one container was consumed; false to stop the batch
     */
    private boolean storeFluidContainer(FilledFluidContainer filled) {
        ItemStack current = playerInventory.mainInventory[filled.inventorySlot];
        if (current == null || current.stackSize <= 0 || !Platform.isSameItemPrecise(current, filled.source))
            return false;
        IAEItemStack emptyRequest = AEItemStack.create(filled.empty.copy());
        IAEFluidStack fluidRequest = filled.fluid.copy();
        if (emptyRequest == null
            || !canInsertAll(itemMonitor.injectItems(emptyRequest.copy(), Actionable.SIMULATE, actionSource))
            || !canInsertAll(
                Platform.poweredInsert(
                    powerSource,
                    fluidMonitor,
                    fluidRequest.copy(),
                    actionSource,
                    Actionable.SIMULATE))) {
            return false;
        }

        IAEItemStack emptyLeftover = itemMonitor.injectItems(emptyRequest.copy(), Actionable.MODULATE, actionSource);
        if (!canInsertAll(emptyLeftover)) {
            rollbackInserted(itemMonitor, emptyRequest, emptyLeftover);
            return false;
        }

        IAEFluidStack fluidLeftover = Platform
            .poweredInsert(powerSource, fluidMonitor, fluidRequest.copy(), actionSource, Actionable.MODULATE);
        if (!canInsertAll(fluidLeftover)) {
            rollbackInserted(itemMonitor, emptyRequest, null);
            rollbackInserted(fluidMonitor, fluidRequest, fluidLeftover);
            return false;
        }

        ItemStack source = playerInventory.mainInventory[filled.inventorySlot];
        if (source == null || !Platform.isSameItemPrecise(source, filled.source)) {
            rollbackInserted(itemMonitor, emptyRequest, null);
            rollbackInserted(fluidMonitor, fluidRequest, null);
            return false;
        }
        if (--source.stackSize <= 0) playerInventory.mainInventory[filled.inventorySlot] = null;
        playerInventory.markDirty();
        return true;
    }

    private FilledFluidContainer findFilledFluidContainer(IAEFluidStack target) {
        IAEStackType<IAEFluidStack> type = target.getStackType();
        for (int slot = 0; slot < playerInventory.mainInventory.length; slot++) {
            ItemStack stored = playerInventory.mainInventory[slot];
            if (stored == null || stored.stackSize <= 0 || !type.isContainerItemForType(stored)) continue;
            ItemStack single = stored.copy();
            single.stackSize = 1;
            IAEFluidStack contained = type.getStackFromContainerItem(single);
            if (contained == null || contained.getStackSize() <= 0 || !contained.isSameType(target)) continue;
            ObjectLongPair<ItemStack> drained = type.drainStackFromContainer(single.copy(), contained.copy());
            if (drained.left() == null || drained.rightLong() != contained.getStackSize()) continue;
            IAEFluidStack drainedFluid = contained.copy();
            drainedFluid.setStackSize(drained.rightLong());
            return new FilledFluidContainer(slot, single, drained.left(), drainedFluid);
        }
        return null;
    }

    private static boolean canInsertAll(IAEStack<?> leftover) {
        return leftover == null || leftover.getStackSize() <= 0;
    }

    private <T extends IAEStack<T>> void rollbackInserted(IMEMonitor<T> monitor, T requested, T leftover) {
        long leftoverAmount = leftover == null ? 0 : leftover.getStackSize();
        long insertedAmount = requested.getStackSize() - leftoverAmount;
        if (insertedAmount <= 0) return;
        T rollback = requested.copy();
        rollback.setStackSize(insertedAmount);
        monitor.extractItems(rollback, Actionable.MODULATE, actionSource);
    }

    private EmptyFluidContainer findEmptyFluidContainer(IAEFluidStack fluid) {
        IAEStackType<IAEFluidStack> type = fluid.getStackType();
        for (IAEItemStack stored : itemMonitor.getStorageList()) {
            if (stored == null || stored.getStackSize() <= 0) continue;
            ItemStack item = stored.getItemStack();
            if (item == null) continue;
            item.stackSize = 1;
            if (!type.isContainerItemForType(item) || type.getStackFromContainerItem(item) != null) continue;
            long capacity = type.getContainerItemCapacity(item, fluid);
            if (capacity <= 0) continue;
            IAEFluidStack full = fluid.copy();
            full.setStackSize(capacity);
            ObjectLongPair<ItemStack> filled = type.fillContainer(item.copy(), full);
            if (!isCompletelyFilled(filled, capacity)) continue;
            IAEFluidStack available = fluidMonitor.extractItems(full, Actionable.SIMULATE, actionSource);
            if (available != null && available.getStackSize() >= capacity) {
                return new EmptyFluidContainer(stored.copy(), item, capacity);
            }
        }
        return null;
    }

    private static boolean isCompletelyFilled(ObjectLongPair<ItemStack> result, long capacity) {
        return result != null && result.left() != null && result.rightLong() == capacity;
    }

    private boolean canAcceptFilledContainer(ItemStack filled) {
        if (filled == null) return false;
        InventoryAdaptor inventory = InventoryAdaptor.getAdaptor(playerInventory.player, ForgeDirection.UNKNOWN);
        return inventory.simulateAdd(filled.copy()) == null;
    }

    private void giveFilledContainer(ItemStack filled) {
        InventoryAdaptor.getAdaptor(playerInventory.player, ForgeDirection.UNKNOWN)
            .addItems(filled);
    }

    private void restoreItem(IAEItemStack stack) {
        if (stack == null || stack.getStackSize() <= 0) return;
        IAEItemStack leftover = itemMonitor.injectItems(stack, Actionable.MODULATE, actionSource);
        if (leftover != null) Platform.handleLeftover(playerInventory.player, leftover);
    }

    private void restoreFluid(IAEFluidStack stack) {
        if (stack == null || stack.getStackSize() <= 0) return;
        IAEFluidStack leftover = fluidMonitor.injectItems(stack, Actionable.MODULATE, actionSource);
        if (leftover != null) Platform.handleLeftover(playerInventory.player, leftover);
    }

    private static final class EmptyFluidContainer {

        private final IAEItemStack stack;
        private final ItemStack item;
        private final long capacity;

        private EmptyFluidContainer(IAEItemStack stack, ItemStack item, long capacity) {
            this.stack = stack;
            this.item = item;
            this.capacity = capacity;
        }
    }

    private static final class FilledFluidContainer {

        private final int inventorySlot;
        private final ItemStack source;
        private final ItemStack empty;
        private final IAEFluidStack fluid;

        private FilledFluidContainer(int inventorySlot, ItemStack source, ItemStack empty, IAEFluidStack fluid) {
            this.inventorySlot = inventorySlot;
            this.source = source;
            this.empty = empty;
            this.fluid = fluid;
        }
    }

}
