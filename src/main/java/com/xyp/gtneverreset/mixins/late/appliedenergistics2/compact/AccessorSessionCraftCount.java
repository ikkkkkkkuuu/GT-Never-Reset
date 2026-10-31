package com.xyp.gtneverreset.mixins.late.appliedenergistics2.compact;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.xyp.gtneverreset.common.crafting.batch.CraftingBatchPlanner.SessionSegment;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.diagnostics.CraftingDiagnosticSessionId;

/**
 * Adapts one private AE diagnostics count segment to the planner's bulk-consumption contract without reflection.
 *
 * <p>
 * Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance.
 */
@Mixin(value = CraftingCPUCluster.TaskProgress.SessionCraftCount.class, remap = false)
public abstract class AccessorSessionCraftCount implements SessionSegment<CraftingDiagnosticSessionId> {

    /**
     * Returns the diagnostics session owning this contiguous craft segment.
     *
     * @return owning session identifier
     */
    @Override
    public CraftingDiagnosticSessionId getSessionId() {
        return gtneverreset$getSessionId();
    }

    @Accessor("sessionId")
    public abstract CraftingDiagnosticSessionId gtneverreset$getSessionId();

    /**
     * Returns the unconsumed craft count in this segment.
     *
     * @return positive remaining count
     */
    @Override
    @Accessor("remaining")
    public abstract long getRemaining();

    /**
     * Stores the positive remainder after a batch consumes part of this segment.
     *
     * @param remaining positive unconsumed count
     */
    @Override
    @Accessor("remaining")
    public abstract void setRemaining(long remaining);
}
