package com.xyp.gtneverreset.common.crafting.chaos.material;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.FluidStackLong;
import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.ItemStackLong;
import com.xyp.gtneverreset.common.me.ChaosStorageCellInputs.InputSource;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTUtility;

/** Reserves shared physical sources across candidate groups until the scheduler commits a round. */
public final class BottledChaosInputReservation {

    private final Map<Object, Map<Object, InputSource>> sources;
    private final Map<InputSource, Long> reserved = new LinkedHashMap<>();

    public BottledChaosInputReservation() {
        sources = new IdentityHashMap<>();
    }

    private BottledChaosInputReservation(BottledChaosInputReservation original) {
        sources = original.sources;
        reserved.putAll(original.reserved);
    }

    public BottledChaosInputReservation fork() {
        return new BottledChaosInputReservation(this);
    }

    public final class View {

        private final Map<InputSource, Long> limits = new LinkedHashMap<>();

        public void excludeOwner(Object owner) {
            if (owner != null) limits.keySet()
                .removeIf(source -> source.owner == owner);
        }

        public View copy() {
            View copy = new View();
            copy.limits.putAll(limits);
            return copy;
        }

        public void add(ItemStack stack) {
            add(InputSource.of(stack, stack));
        }

        public void add(FluidStack stack) {
            add(InputSource.of(stack, stack));
        }

        public void add(InputSource input) {
            if (input == null) return;
            long limit = input.amount;
            InputSource source = input.register(sources);
            if (source != null) limits.merge(source, limit, Math::max);
        }
    }

    public Snapshot snapshot(View view) {
        return new Snapshot(view, reserved);
    }

    public void accept(Snapshot snapshot) {
        for (int i = 0; i < snapshot.sources.size(); i++) {
            long remaining = i < snapshot.items.length ? snapshot.items[i].stackSize()
                : snapshot.fluids[i - snapshot.items.length].amount();
            long used = snapshot.initial[i] - remaining;
            if (used < 0 || remaining < 0) throw new IllegalStateException("Invalid input reservation");
            if (used > 0) reserved.merge(snapshot.sources.get(i), used, Math::addExact);
        }
    }

    public void accept(BottledChaosInputReservation candidate) {
        reserved.clear();
        reserved.putAll(candidate.reserved);
    }

    public CheckRecipeResult commit() {
        for (var entry : reserved.entrySet()) {
            if (entry.getKey().available.getAsLong() < entry.getValue()) return extractionFailure(entry.getKey());
        }
        // A short real extraction follows GT's non-rollback semantics; never retry an already debited candidate.
        for (var entry : reserved.entrySet()) {
            if (entry.getKey().extract.applyAsLong(entry.getValue()) != entry.getValue()) {
                reserved.clear();
                return extractionFailure(entry.getKey());
            }
        }
        reserved.clear();
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private static CheckRecipeResult extractionFailure(InputSource source) {
        return SimpleCheckRecipeResult.ofFailurePersistOnShutdown(
            source.template instanceof ItemStack ? "stocking_bus_fail_extraction" : "stocking_hatch_fail_extraction");
    }

    public static final class Snapshot {

        private final List<InputSource> sources = new ArrayList<>();
        private final ItemStackLong[] items;
        private final FluidStackLong[] fluids;
        private final long[] initial;

        private Snapshot(View view, Map<InputSource, Long> reserved) {
            for (InputSource source : view.limits.keySet())
                if (source.template instanceof ItemStack) sources.add(source);
            int itemCount = sources.size();
            for (InputSource source : view.limits.keySet())
                if (source.template instanceof FluidStack) sources.add(source);
            items = new ItemStackLong[itemCount];
            fluids = new FluidStackLong[sources.size() - itemCount];
            initial = new long[sources.size()];
            for (int i = 0; i < sources.size(); i++) {
                InputSource source = sources.get(i);
                initial[i] = Math
                    .min(view.limits.get(source), Math.max(0, source.amount - reserved.getOrDefault(source, 0L)));
                if (i < itemCount) items[i] = new ItemStackLong(((ItemStack) source.template).copy(), initial[i]);
                else fluids[i - itemCount] = new FluidStackLong(((FluidStack) source.template).copy(), initial[i]);
            }
        }

        public ItemStackLong[] getInputItems() {
            return items;
        }

        public FluidStackLong[] getInputFluids() {
            return fluids;
        }

        /** Bounded copies for structure parameter checks only, never for recipe allocation. */
        public ItemStack[] itemStacks() {
            ItemStack[] result = new ItemStack[items.length];
            for (int i = 0; i < result.length; i++) result[i] = GTUtility
                .copyAmountUnsafe((int) Math.min(Integer.MAX_VALUE, items[i].stackSize()), items[i].itemStack());
            return result;
        }

        public FluidStack[] fluidStacks() {
            FluidStack[] result = new FluidStack[fluids.length];
            for (int i = 0; i < result.length; i++) result[i] = GTUtility
                .copyAmount((int) Math.min(Integer.MAX_VALUE, fluids[i].amount()), fluids[i].fluidStack());
            return result;
        }
    }
}
