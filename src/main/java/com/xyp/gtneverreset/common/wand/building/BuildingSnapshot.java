package com.xyp.gtneverreset.common.wand.building;

import net.minecraft.block.Block;

import com.xyp.gtneverreset.common.wand.building.BuildingContext.Position;

/** Shared action snapshot, adapted from ConstructionWand's ISnapshot. */
public interface BuildingSnapshot {

    enum Result {
        SKIPPED,
        CHANGED,
        STOP
    }

    Position getPosition();

    Block getMaterialBlock();

    Result execute(BuildingContext context, BuildingSupplier supplier);

    /** Returns the committed inverse operation, or null when this snapshot did not change the world. */
    BuildingUndoRecord getUndoRecord();
}
