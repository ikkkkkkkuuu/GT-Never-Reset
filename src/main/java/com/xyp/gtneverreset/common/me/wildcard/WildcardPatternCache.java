package com.xyp.gtneverreset.common.me.wildcard;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

/** Each slot owns its cache because AE2 mutates pattern priority. Access stays on the owning thread. */
public final class WildcardPatternCache<T> {

    private ItemStack snapshot;
    private Object context;
    private T result;
    private boolean computed;

    /** Snapshot after building: generation may initialize NBT or write an expanded count. */
    public T get(ItemStack stack, Object context, Supplier<T> build) {
        if (!computed || this.context != context || !matches(stack)) {
            T next = build.get();
            snapshot = stack == null ? null : stack.copy();
            this.context = context;
            result = next;
            computed = true;
        }
        return result;
    }

    private boolean matches(ItemStack stack) {
        if (snapshot == null || stack == null) return snapshot == stack;
        return snapshot.getItem() == stack.getItem() && snapshot.getItemDamage() == stack.getItemDamage()
            && ItemStack.areItemStackTagsEqual(snapshot, stack);
    }
}
