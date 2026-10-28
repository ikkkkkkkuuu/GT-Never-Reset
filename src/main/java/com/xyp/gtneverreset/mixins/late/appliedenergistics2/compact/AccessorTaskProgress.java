package com.xyp.gtneverreset.mixins.late.appliedenergistics2.compact;

import java.util.LinkedList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance. */
@Mixin(value = CraftingCPUCluster.TaskProgress.class, remap = false)
public interface AccessorTaskProgress {

    @Accessor
    long getValue();

    @Accessor
    void setValue(long value);

    @Accessor("diagnosticSessionCrafts")
    LinkedList<?> getDiagnosticSessionCrafts();
}
