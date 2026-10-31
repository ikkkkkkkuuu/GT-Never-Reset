package com.xyp.gtneverreset.common.machine.multiblock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

import com.xyp.gtneverreset.utils.DireCraftingPatternDetails;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.tile.inventory.AppEngInternalInventory;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AssemblerMatrixPatternInventory implements IInventory, Iterable<ItemStack> {

    private static final int PAGE_CAPACITY = 72;
    private final AssemblerMatrix machine;
    private int size = -1;
    private AppEngInternalInventory[] combinationInventory = new AppEngInternalInventory[0];
    private int terminalRows = -1;

    public AssemblerMatrixPatternInventory(AssemblerMatrix machine) {
        this.machine = machine;
    }

    /** Matches the super pattern input assembly's empty and full-row expansion rules. */
    public int getTerminalRows() {
        if (terminalRows < 0) {
            int columns = machine.rowSize();
            int lastNonNullIndex = getSizeInventory() - 1;
            while (lastNonNullIndex >= 0 && getStackInSlot(lastNonNullIndex) == null) lastNonNullIndex--;
            int calculatedRows = (lastNonNullIndex + columns) / columns + 1;
            if ((lastNonNullIndex + 1) % columns == 0) calculatedRows++;
            terminalRows = Math.min(calculatedRows, getSizeInventory() / columns);
        }
        return terminalRows;
    }

    @Override
    public int getSizeInventory() {
        return machine.mMaxSlots;
    }

    @Override
    public ItemStack getStackInSlot(int slotIn) {
        if (slotIn < 0 || slotIn >= AssemblerMatrix.PATTERN_CAPACITY) return null;
        size = -1;
        return packItem(getInventory(slotIn / PAGE_CAPACITY).getStackInSlot(slotIn % PAGE_CAPACITY));
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (slot < 0 || slot >= AssemblerMatrix.PATTERN_CAPACITY) return null;
        size = -1;
        return packItem(getInventory(slot / PAGE_CAPACITY).decrStackSize(slot % PAGE_CAPACITY, count));
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot < 0 || slot >= AssemblerMatrix.PATTERN_CAPACITY) return null;
        size = -1;
        return packItem(getInventory(slot / PAGE_CAPACITY).getStackInSlotOnClosing(slot % PAGE_CAPACITY));
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        size = -1;
        getInventory(slot / PAGE_CAPACITY).setInventorySlotContents(slot % PAGE_CAPACITY, stack);
    }

    @Override
    public String getInventoryName() {
        return "patterns";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        terminalRows = -1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= AssemblerMatrix.PATTERN_CAPACITY
            || stack == null
            || !(stack.getItem() instanceof ICraftingPatternItem item)) return false;
        var details = item.getPatternForItem(
            stack,
            machine.getBaseMetaTileEntity()
                .getWorld());
        return details != null && (details.isCraftable() || details instanceof DireCraftingPatternDetails);
    }

    public void saveNBTData(NBTTagCompound aNBT) {
        if (machine.getBaseMetaTileEntity()
            .isServerSide()) {
            var n = new NBTTagCompound();
            for (var i = 0; i < combinationInventory.length; i++) {
                var inv = combinationInventory[i];
                if (inv != null) {
                    inv.writeToNBT(n, Integer.toString(i));
                }
            }
            aNBT.setTag("patterns", n);
        }
    }

    public void loadNBTData(NBTTagCompound aNBT) {
        combinationInventory = new AppEngInternalInventory[0];
        size = -1;
        markDirty();
        var n = aNBT.getCompoundTag("patterns");
        for (var o : n.func_150296_c()) {
            getInventory(Integer.parseInt(o)).readFromNBT(n.getCompoundTag(o));
        }
        machine.upPatterns();
    }

    public int size() {
        if (size < 0) {
            size = 0;
            for (ItemStack ignored : this) {
                ++size;
            }
        }
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    private ItemStack packItem(ItemStack stack) {
        if (stack == null) return null;
        if (stack.stackSize <= 0) return null;
        return stack;
    }

    public List<ItemStack> getAllItemsCopy() {
        List<ItemStack> result = new ObjectArrayList<>();
        for (ItemStack stack : this) {
            result.add(stack);
        }
        return result;
    }

    public int getFirstEmptySlot() {
        for (int slot = 0; slot < getSizeInventory(); slot++) {
            if (getStackInSlot(slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    public boolean insertPattern(ItemStack stack) {
        var slot = getFirstEmptySlot();
        if (slot < 0) return false;
        this.setInventorySlotContents(slot, stack);
        return true;
    }

    @Override
    public @NotNull Iterator<ItemStack> iterator() {
        return new PatternIterator();
    }

    private AppEngInternalInventory getInventory(int ordinal) {
        if (ordinal < 0 || ordinal >= AssemblerMatrix.PATTERN_CAPACITY / PAGE_CAPACITY) {
            throw new IndexOutOfBoundsException("Pattern inventory page: " + ordinal);
        }
        if (ordinal >= combinationInventory.length) {
            combinationInventory = Arrays.copyOf(combinationInventory, ordinal + 1);
        }
        var i = combinationInventory[ordinal];
        if (i == null) {
            combinationInventory[ordinal] = i = new AppEngInternalInventory(machine, PAGE_CAPACITY, 1);
        }
        return i;
    }

    private class PatternIterator implements Iterator<ItemStack> {

        private int invOrdinal = 0;
        private int slotOrdinal = -1;
        private int nowInv = -1;
        private int nowSlot = -1;
        private boolean nowAvailable = false;

        @Override
        public boolean hasNext() {
            upAvailable();
            return nowAvailable;
        }

        @Override
        public ItemStack next() {
            if (hasNext()) {
                nowAvailable = false;
                return AssemblerMatrixPatternInventory.this.combinationInventory[nowInv = invOrdinal]
                    .getStackInSlot(nowSlot = slotOrdinal);
            }
            nowInv = -1;
            nowSlot = -1;
            return null;
        }

        @Override
        public void remove() {
            if (nowInv < 0) return;
            AssemblerMatrixPatternInventory.this.combinationInventory[nowInv].setInventorySlotContents(nowSlot, null);
            nowInv = -1;
            nowSlot = -1;
        }

        private void upAvailable() {
            if (!nowAvailable) {
                while (machine.mMaxSlots >= (invOrdinal * PAGE_CAPACITY + slotOrdinal + 1)) {
                    if (invOrdinal >= combinationInventory.length) {
                        slotOrdinal = PAGE_CAPACITY;
                        break;
                    }
                    var inv = AssemblerMatrixPatternInventory.this.combinationInventory[invOrdinal];
                    if (inv == null) {
                        ++invOrdinal;
                        continue;
                    }
                    while (++slotOrdinal < inv.getSizeInventory()) {
                        var stack = inv.getStackInSlot(slotOrdinal);
                        if (stack != null) {
                            nowAvailable = true;
                            return;
                        }
                    }
                    slotOrdinal = -1;
                    ++invOrdinal;
                }
                nowInv = -1;
                nowSlot = -1;
            }
        }
    }
}
