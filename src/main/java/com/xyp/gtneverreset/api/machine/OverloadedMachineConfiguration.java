package com.xyp.gtneverreset.api.machine;

import net.minecraft.item.ItemStack;

public interface OverloadedMachineConfiguration {

    ItemStack gtnr$getVirtualMold();

    void gtnr$setVirtualMold(ItemStack mold);

    void gtnr$beginConfiguration(ItemStack previousCircuit);

    /** Restores the user's circuit and mold after a delivery stops owning the machine; safe to call when idle. */
    void gtnr$releaseConfiguration();

    void gtnr$setPending(boolean pending);

    boolean gtnr$hasPending();

    void gtnr$setProvider(int x, int y, int z);

    boolean gtnr$isProvider(int x, int y, int z);
}
