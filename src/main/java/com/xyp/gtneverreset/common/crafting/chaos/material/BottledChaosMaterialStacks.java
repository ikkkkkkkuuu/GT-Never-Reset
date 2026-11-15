package com.xyp.gtneverreset.common.crafting.chaos.material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.FluidStackLong;
import com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.ItemStackLong;

import gregtech.api.util.GTUtility.FluidId;

/** Copied material templates with exact long quantities, independent of recipe timing or batch counts. */
public record BottledChaosMaterialStacks(List<ItemStackLong> items, List<FluidStackLong> fluids) {

    public static BottledChaosMaterialStacks fromAmounts(Map<Object, Long> amounts) {
        List<ItemStackLong> items = new ArrayList<>();
        List<FluidStackLong> fluids = new ArrayList<>();
        amounts.forEach((key, amount) -> appendResourceStack(key, amount, items, fluids));
        return new BottledChaosMaterialStacks(
            Collections.unmodifiableList(items),
            Collections.unmodifiableList(fluids));
    }

    /** Append in source order, including zero-quantity catalysts used by recipe displays. */
    public static void appendResourceStack(Object key, long amount, List<ItemStackLong> items,
        List<FluidStackLong> fluids) {
        // Copy templates so neither runtime outputs nor NEI can mutate cached identities through NBT.
        if (key instanceof ChaosItemKey item) items.add(
            new ItemStackLong(
                item.getItemStack()
                    .copy(),
                amount));
        else fluids.add(
            new FluidStackLong(
                ((FluidId) key).getFluidStack(1)
                    .copy(),
                amount));
    }
}
