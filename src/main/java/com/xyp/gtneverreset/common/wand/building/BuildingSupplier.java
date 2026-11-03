package com.xyp.gtneverreset.common.wand.building;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.wand.building.BuildingContext.Position;

/** Material-provider boundary used by the core and the job. */
public interface BuildingSupplier {

    boolean isSupported();

    boolean hasRemaining();

    PlacementSnapshot getPlaceSnapshot(BuildingContext context, Position source, Position destination);

    MaterialLease takeOne(ItemStack required);

    default void onFinished() {}
}
