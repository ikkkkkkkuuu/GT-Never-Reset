package com.xyp.gtneverreset.common.machine.hatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtneverreset.common.machine.hatch.MEDataAccessHatch.MEDataStickSnapshot;

public class MEDataAccessContracts {

    private final Item item = new Item().setHasSubtypes(true);
    private final ItemStack dataStick = new ItemStack(item, 1, 7);
    private final MEDataStickSnapshot snapshot = new MEDataStickSnapshot();

    private ItemStack research(String name, int quantity) {
        ItemStack stack = new ItemStack(item, quantity, 7);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("research", name);
        stack.setTagCompound(tag);
        return stack;
    }

    public void filtersNonSticksAndEmptyEntries() {
        assertTrue(
            snapshot.replace(
                Arrays.asList(
                    research("A", 1),
                    research("empty", 0),
                    research("removed", -1),
                    new ItemStack(item, 1, 8),
                    new ItemStack(new Item(), 1, 7),
                    null),
                dataStick));
        assertEquals(1, snapshot.size());
        assertEquals(
            "A",
            snapshot.get(0)
                .getTagCompound()
                .getString("research"));
    }

    public void quantityAndOrderChangesDoNotInvalidateResearch() {
        assertTrue(snapshot.replace(Arrays.asList(research("A", 4), research("B", 1)), dataStick));
        assertFalse(snapshot.replace(Arrays.asList(research("B", 9), research("A", 2), research("A", 1)), dataStick));
        assertEquals(2, snapshot.size());
        assertEquals(1, snapshot.get(0).stackSize);
        assertEquals(1, snapshot.get(1).stackSize);
    }

    public void sameSizedResearchSwapAndRemovalInvalidate() {
        assertTrue(snapshot.replace(Collections.singletonList(research("A", 1)), dataStick));
        assertTrue(snapshot.replace(Collections.singletonList(research("B", 1)), dataStick));
        assertEquals(
            "B",
            snapshot.get(0)
                .getTagCompound()
                .getString("research"));
        assertTrue(snapshot.replace(Collections.emptyList(), dataStick));
        assertEquals(0, snapshot.size());
        assertNull(snapshot.get(0));
    }

    public void callersCannotMutateCachedResearchOrNetworkItems() {
        ItemStack network = research("A", 9);
        snapshot.replace(Collections.singletonList(network), dataStick);
        assertEquals(9, network.stackSize);
        network.getTagCompound()
            .setString("research", "network changed");
        ItemStack returned = snapshot.get(0);
        returned.getTagCompound()
            .setString("research", "caller changed");
        returned.stackSize = 0;
        assertEquals(
            "A",
            snapshot.get(0)
                .getTagCompound()
                .getString("research"));
        assertEquals(1, snapshot.get(0).stackSize);
        assertNull(snapshot.get(-1));
        assertNull(snapshot.get(1));
    }

    public void disconnectClearsResearchAndReconnectRestoresIt() {
        snapshot.replace(Collections.singletonList(research("A", 1)), dataStick);
        assertTrue(snapshot.clear());
        assertFalse(snapshot.clear());
        assertEquals(0, snapshot.size());
        assertTrue(snapshot.replace(Collections.singletonList(research("A", 1)), dataStick));
        assertEquals(1, snapshot.size());
    }

    public void exposesMoreThanTheInheritedSixteenSlots() {
        ArrayList<ItemStack> research = new ArrayList<>();
        for (int i = 0; i < 100; i++) research.add(research("recipe " + i, 1));
        assertTrue(snapshot.replace(research, dataStick));
        assertEquals(100, snapshot.size());
        assertEquals(
            "recipe 99",
            snapshot.get(99)
                .getTagCompound()
                .getString("research"));
    }

    public static void main(String[] args) {
        MEDataAccessContracts checks = new MEDataAccessContracts();
        checks.filtersNonSticksAndEmptyEntries();
        checks = new MEDataAccessContracts();
        checks.quantityAndOrderChangesDoNotInvalidateResearch();
        checks = new MEDataAccessContracts();
        checks.sameSizedResearchSwapAndRemovalInvalidate();
        checks = new MEDataAccessContracts();
        checks.callersCannotMutateCachedResearchOrNetworkItems();
        checks = new MEDataAccessContracts();
        checks.disconnectClearsResearchAndReconnectRestoresIt();
        checks = new MEDataAccessContracts();
        checks.exposesMoreThanTheInheritedSixteenSlots();
        System.out.println("ME data access contracts passed: 6 scenarios");
    }

    private static void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Expected true");
    }

    private static void assertFalse(boolean condition) {
        assertTrue(!condition);
    }

    private static void assertNull(Object value) {
        if (value != null) throw new AssertionError("Expected null, got " + value);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
