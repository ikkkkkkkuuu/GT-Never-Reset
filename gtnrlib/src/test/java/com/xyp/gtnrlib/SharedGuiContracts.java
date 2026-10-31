package com.xyp.gtnrlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnrlib.gui.widget.ItemStackSlot;
import com.xyp.gtnrlib.gui.widget.ScrollingDropdown;

final class SharedGuiContracts {

    static void verify() {
        AtomicInteger changes = new AtomicInteger();
        List<String> options = new ArrayList<>(Arrays.asList("first", "second"));
        ScrollingDropdown selector = new ScrollingDropdown("test", null, options)
            .setOnChanged(value -> changes.incrementAndGet())
            .setValue("second");
        options.clear();
        check(
            selector.getCandidates()
                .size() == 2,
            "Candidates must be copied");
        List<String> view = selector.getCandidates();
        selector.setCandidates(Arrays.asList("replacement"));
        check(view.equals(Arrays.asList("replacement")), "Candidate view remains live");
        check(
            selector.getSelectedValue()
                .equals("second") && changes.get() == 0,
            "Replacing candidates must neither select nor notify");
        try {
            view.clear();
            throw new AssertionError("Mutable candidate view");
        } catch (UnsupportedOperationException expected) {}

        ItemStack source = new ItemStack(new Item(), 37, 5);
        source.setTagCompound(new NBTTagCompound());
        source.getTagCompound()
            .setString("payload", "original");
        AtomicReference<ItemStack> received = new AtomicReference<>();
        ItemStackSlot slot = new ItemStackSlot(() -> source, null).onDrop(stack -> {
            received.set(stack);
            changes.incrementAndGet();
        });
        check(slot.handleDragAndDrop(source, 1), "Item drop accepted");
        ItemStack copy = received.get();
        check(
            changes.get() == 1 && copy != source && copy.stackSize == 1 && copy.getItemDamage() == 5,
            "Exactly one normalized copy per drop");
        check(
            copy.getTagCompound()
                .getString("payload")
                .equals("original"),
            "NBT retained");
        copy.getTagCompound()
            .setString("payload", "changed");
        check(
            source.stackSize == 37 && source.getTagCompound()
                .getString("payload")
                .equals("original"),
            "Source count and NBT remain untouched");
        check(!new ItemStackSlot(() -> source, null).handleDragAndDrop(source, 0), "Preview remains read-only");
        System.out.println("Shared GUI copy and selection contracts passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
