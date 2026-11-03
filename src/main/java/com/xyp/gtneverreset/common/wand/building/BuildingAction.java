package com.xyp.gtneverreset.common.wand.building;

import java.util.List;

/** Core-specific geometry, separate from inventory ownership and world changes. */
public interface BuildingAction {

    List<BuildingSnapshot> getSnapshots(BuildingContext context, BuildingSupplier supplier, int limit);
}
