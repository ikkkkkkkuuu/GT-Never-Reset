package com.xyp.gtneverreset.utils.crafting.quantum.api.networking.storage;

import java.util.Collection;
import java.util.Set;

import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.KeyCounter;

public interface IStorageService {

    int getInventorySize();

    KeyCounter copyInventory();

    KeyCounter copyInventory(Collection<AEKey> keys, Set<AEKey> fuzzyKeys);
}
