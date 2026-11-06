package com.xyp.gtneverreset.api.me;

import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

/** A pattern inventory that opts into per-delivery recipe-map selection. */
public interface RecipeMapPatternInventory extends IDualInputInventoryWithPattern {

    /** Null retains the controller's current mode; an unknown name must not consume the inventory. */
    String getRecipeMapName();
}
