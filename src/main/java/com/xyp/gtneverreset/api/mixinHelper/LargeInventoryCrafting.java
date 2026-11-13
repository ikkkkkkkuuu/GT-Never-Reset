package com.xyp.gtneverreset.api.mixinHelper;

/**
 * Carries an exact long-sized dispatch multiplier alongside Minecraft's int-sized crafting inventory stacks.
 * Namespaced methods keep this metadata separate from other mods' crafting inventory extensions.
 */
public interface LargeInventoryCrafting {

    /**
     * Stores the multiplier selected before a compatible medium receives the crafting inventory.
     *
     * @param value positive dispatch craft count
     */
    void gtnr$setAssemblerSize(long value);

    /**
     * Returns the exact multiplier that medium output logic must use instead of ItemStack.stackSize.
     *
     * @return positive dispatch craft count; ordinary crafting inventories default to one
     */
    long gtnr$getAssemblerSize();
}
