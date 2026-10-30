package com.xyp.gtneverreset.api.machine;

import net.minecraft.item.ItemStack;

public interface OverloadedMachineConfiguration {

    ItemStack gtnr$getVirtualMold();

    void gtnr$setVirtualMold(ItemStack mold);

    void gtnr$beginConfiguration(ItemStack previousCircuit);

    void gtnr$setPending(boolean pending);

    boolean gtnr$hasPending();

    void gtnr$setProvider(int x, int y, int z);
}
